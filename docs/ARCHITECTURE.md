# Architecture

## Static view: four layers

```mermaid
flowchart TB
  subgraph ui["ui"]
    main["Main"]
    swing["swing: MainWindow, GamePanel,<br/>BoardRenderer, ConfigDialog"]
    cli["cli: Cli"]
  end
  subgraph application["application"]
    simulation["Simulation (Facade)"]
    trace["TraceFormatter"]
    stats["Stats (Observer)"]
  end
  subgraph infrastructure["infrastructure"]
    repo["ConfigRepository + ConfigFileV1 (DTO)"]
  end
  subgraph domain["domain: java.lang and java.util only"]
    game["Game, Turn, Player"]
    config["GameConfig, Jump, Special, Card"]
    events["GameEvent (sealed)"]
    random["RandomBoards (static factory)"]
  end
  main --> swing & cli
  swing --> simulation & trace & repo
  cli --> simulation & trace & repo
  simulation --> game & random
  trace --> events
  stats --> events
  repo --> config
  game --> config & events
```

| Layer | Responsibility | Depends on |
|---|---|---|
| `domain` | the rules: configuration and its validation, turns, special cells, cards, events | `java.lang`, `java.util` |
| `application` | games seeded from a number, the text trace, statistics | `domain` |
| `infrastructure` | configuration files (JSON, Jackson) | `domain` |
| `ui` | the Swing window, the command line and the launcher | everything above |

The rules are checked by [`ArchitectureTest`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ArchitectureTest.java)
on every build:
- each layer only depends downwards;
- the domain uses nothing but `java.lang` and `java.util`;
- only the command line writes to the console;
- every static field is final;
- the command line needs no graphics;
- only the application layer creates random generators.

Full UML diagrams, with their PlantUML sources:
[components](uml/components.svg), [domain model](uml/domain-classes.svg),
[use cases](uml/use-cases.svg), [one turn](uml/turn-sequence.svg).

## Dynamic view: one turn

```mermaid
sequenceDiagram
  actor Observer
  participant Panel as GamePanel
  participant Game
  participant Turn
  participant Cell as Special (Command)
  participant L as listeners
  Observer->>Panel: Step (Space)
  Panel->>Game: playTurn()
  Game->>L: TurnStarted
  Game->>Turn: rollAndMove()
  Turn->>Game: rollDie() twice
  Turn->>L: DiceRolled
  Turn->>Turn: moveBy(roll), bounce if needed
  Turn->>L: PlayerMoved
  alt foot of a ladder or head of a snake
    Turn->>L: LadderClimbed / SnakeSlid
  else special cell, first time this turn
    Turn->>L: SpecialReached
    Turn->>Cell: applyTo(turn)
    Cell->>Turn: rest / rollAndMove / moveBy / drawCard
  end
  opt double six with the bonus on, game not over, not resting
    Turn->>L: DoubleSix
    Turn->>Turn: rollAndMove() again
  end
  Game-->>Panel: return
  Panel->>Panel: animate the queued moves, then update the player list
```

`Game.playTurn` computes the whole turn at once and reports it as events. The GUI queues the moves
those events describe and animates them afterwards with a Swing `Timer`, so the model never waits
for the screen.

## Threads

Everything runs on the Swing event thread: a turn takes microseconds, so there is no background
worker to synchronise. Automatic play is a `javax.swing.Timer` that plays the next turn once the
previous animation has finished.

## Randomness and replay

The only source of randomness is the `RandomGenerator` given to `Game`. `Simulation` creates it
from a seed, `SplittableRandom(seed)`, and also shuffles the deck with it. The same
configuration and seed therefore replay the same game, in the window ("Restart"), on the command
line (`--seed`) and in tests, which pass a scripted generator instead.

## Configuration files

`ConfigRepository` reads and writes the versioned JSON format through the `ConfigFileV1` DTO
([ADR-0004](adr/0004-versioned-json-configuration-files.md)). The three presets in `configs/`
are packaged into the jar and loaded from the classpath by name.

## Packaging

`./mvnw verify` builds one executable jar with Jackson and FlatLaf inside (Maven Shade).
`ui.Main` opens the window when started without arguments and a screen is available; otherwise
it runs the command line.

## Quality gates

| Gate | Tool | Fails the build when |
|---|---|---|
| Unit, scenario and property tests | JUnit 5, AssertJ, jqwik | any test fails |
| Architecture | ArchUnit | a layering or design rule is broken |
| Coverage | JaCoCo | the `domain` package drops below 80% of lines or branches |
| Formatting | Spotless (google-java-format) | the code is not formatted |
| Style | Checkstyle (Google checks) | any violation |
| Bug patterns | SpotBugs | any bug at its default threshold (medium priority and above) |

## Decisions

| ADR | Decision |
|---|---|
| [0001](adr/0001-java-21-baseline.md) | Java 21, for exhaustive `switch` over sealed events |
| [0002](adr/0002-layered-architecture-checked-by-tests.md) | Four layers, checked by tests |
| [0003](adr/0003-rule-interpretations.md) | One fixed answer for each way the rules interact |
| [0004](adr/0004-versioned-json-configuration-files.md) | Versioned JSON configuration files instead of Java serialization |
