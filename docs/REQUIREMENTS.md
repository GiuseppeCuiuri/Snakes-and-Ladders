# Requirements

This page states the rules of the game, refines them into services and quality requirements, and
traces every requirement to the code and to the tests that prove it.

## The game

The application simulates the board game Snakes and Ladders. A person, the **observer**, chooses how many players
to simulate and then watches the game unfold until someone wins, with a **text trace** of what
happens in each turn.

**Base rules.**
- The board is a path of N cells on a grid of R rows and C columns; the classic board is 10 x 10.
- Players roll two six-sided dice in turn and move forward.
- Landing exactly on the foot of a ladder moves the player to its top; landing on the head of a
  snake moves the player to its tail. Passing over them, or landing on a top or a tail, does
  nothing.
- No cell is an end of more than one ladder or snake.
- The last cell must be reached with an exact roll: extra points bounce back.
- The first player to reach the last cell wins.

**Optional rules, independent unless stated otherwise.**
- **Board size**: any number of cells, rows and columns, for example 48 cells in 8 rows of 6.
- **Single die**: one die instead of two for the whole game.
- **Single die near the end**: one die instead of two on cells N-6 to N-1. Not compatible with a
  single-die game.
- **Double-six bonus**: a double six earns another roll, after the move and any ladder or snake.
  Not compatible with a single-die game.
- **Rest cells**: a bench means skipping 1 turn, an inn 3 turns.
- **Bonus cells**: a dice cell means rolling again; a spring means moving again by the last roll,
  without rolling.
- **Draw-a-card cells**: bench, inn, dice and spring cards act like the matching cells; a card is
  used at once and goes back to the bottom of the deck.
- **No-rest card** (needs draw-a-card cells): it is kept and cancels a later rest, from a cell or
  from a card; once used, it goes back to the bottom of the deck.
- Special cells are never on an end of a ladder or snake.

**The application must** have a graphical interface, save and load the configuration, and let the
game advance one step at a time or automatically.

## Services, by priority

| # | Service | Priority | Status |
|---|---|---|---|
| S1 | Move the players by the rules, including every special effect | high | done: `domain` |
| S2 | Ladders, snakes, rest, bonus and draw-a-card cells, no-rest card | high | done: `domain` |
| S3 | Save and load the configuration, positions included | medium | done: versioned JSON files |
| S4 | Graphical interface with the board and the trace | high | done: Swing + FlatLaf |
| S5 | One to six players on the same computer | medium | done |
| S6 | A customisable board: size, rules, ladders, snakes, special cells | medium | done: dialog and files |
| S7 | Command line with the text trace, replay by seed, statistics over many games | medium | done |

## Quality requirements

| Requirement | How it is met |
|---|---|
| **Maintainability** | Four layers checked by ArchUnit; design patterns only where they solve a problem (see `PATTERNS.md`); coverage gate on the domain; Checkstyle and SpotBugs blocking. |
| **Reliability** | Every configuration is validated before use and every problem is reported at once; invalid files are rejected with the place of the error (JSON path, or line and column); random board generation always ends. |
| **Reproducibility** | All randomness comes from one seeded generator, so a seed replays a game exactly. |
| **Performance** | A turn is computed in microseconds on the Swing event thread; 10 000 games run in about a second from the command line. |
| **Portability** | One executable jar on Java 21 or newer; CI builds on Linux, Windows and macOS with Java 21 and 25. |

## Scenarios

1. **First game.** The observer starts the application. The classic board appears with two
   players, and *Step* (or Space) plays one turn at a time while the trace lists every roll and
   move.
2. **A custom game.** *Game → New game* opens a dialog. The observer picks a random board of
   8 x 6 with rests, bonus and draw-a-card cells and three players. If the choice is invalid, for
   instance a single die with the double-six rule, the dialog explains what is wrong. *Play* then
   runs the game automatically, at the chosen speed, until the winner banner.
3. **Saving and replaying.** The observer saves the configuration as JSON, reopens it later,
   possibly on another computer, and gets the same board. From the command line,
   `--config file.json --seed 42` replays the same game turn by turn.

## Assumptions

- The person watching knows the rules; *Help → How to play* summarises them.
- A Java 21 or newer runtime is installed.
- When the rules leave a case open, the game follows the interpretations in
  [ADR-0003](adr/0003-rule-interpretations.md), for example when a double-six bonus is lost, or
  when the no-rest card is used.

## Excluded

- **Online multiplayer**: it would need a server and accounts.
- **Undo**: a game can be replayed from its seed instead.

## Traceability: requirement → code → tests

Paths are relative to `src/main/java/io/github/giuseppecuiuri/snakesandladders` (code) and
`src/test/java/io/github/giuseppecuiuri/snakesandladders` (tests).

| Req. | Code | Tests |
|---|---|---|
| Board of R x C cells | `domain/GameConfig` (`size`, `rowOf`), `ui/swing/BoardRenderer` | [`GameConfigTest.rowsAndColumnsCanChange`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L25), [`BoardRendererTest.cellsFollowSerpentinePathFromTheBottomLeft`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/swing/BoardRendererTest.java#L24) |
| Two six-sided dice | `domain/Turn.rollAndMove` | [`GameTest.eachTurnReportsWhatHappenedInOrder`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L74), `GamePropertiesTest` |
| Ladders and snakes move a player only on landing | `domain/Turn.resolve` | [`GameTest.passingOverLadderFootOrSnakeHeadDoesNotMovePlayer`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L87), [`landingOnLadderTopOrSnakeTailDoesNotMovePlayer`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L98) |
| No shared ends, ends on different rows, ladders up, snakes down | `domain/GameConfig` (validation) | [`GameConfigTest`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L66), lines 66-100 |
| Exact landing with bounce | `domain/Turn.moveBy` | [`GameTest.excessPointsBounceBackFromTheLastCell`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L109), [`bounceOnSmallBoardResolvesTheCellReached`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L119) |
| First to the last cell wins | `domain/Game`, `domain/Turn.resolve` | [`GameTest.anExactRollToTheLastCellWinsAndEndsTheGame`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L128) |
| Worked examples of the rules | the whole engine | [`GameTest.GoldenExamples`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L45) |
| Observer picks players and reads a trace | `ui/cli/Cli`, `ui/swing/GamePanel`, `application/TraceFormatter` | [`CliTest.headlessGamePrintsTraceUntilSomeoneWins`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/cli/CliTest.java#L30), `TraceFormatterTest`, [`GameTest.oneToSixPlayersCanPlay`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L157) |
| Any number of cells, rows and columns | `domain/GameConfig`, preset `configs/compact-48.json` | [`GameConfigTest.rowsAndColumnsCanChange`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L25), [`ConfigRepositoryTest.eachPresetShowsWhatItIsNamedFor`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/infrastructure/ConfigRepositoryTest.java#L145) |
| Single die | `GameConfig.dice`, `Turn.rollAndMove` | [`GameTest.withOneDieEveryRollUsesOneDie`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L173) |
| One die near the end, not in a single-die game | `GameConfig.singleDieNearEnd` | [`GameTest.singleDieNearTheEndStartsSixCellsBeforeTheLast`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L184), [`bonusRollNearTheEndAlsoUsesOneDie`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L220), [`GameConfigTest.oneOrTwoDiceAndTheOptionalRulesNeedTwo`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L54) |
| Double-six bonus, not in a single-die game | `GameConfig.doubleSixBonus`, `Turn.rollAndMove` | [`GameTest.doubleSixRollsAgainAndMovesByTheNewRollOnly`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L206), [`doubleSixThatWinsEndsTheTurnWithoutBonusRoll`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L231) |
| Bench and inn | `Special.BENCH`, `Special.INN`, `Turn.rest` | [`GameTest.RestCells`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L253) |
| Dice cell and spring | `Special.DICE`, `Special.SPRING` | [`GameTest.BonusCells`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L313) |
| Draw-a-card cells and deck | `Special.DRAW_CARD`, `Card`, `Turn.drawCard`, `GameConfig.deck` | [`GameTest.DrawCards`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L370), [`GameConfigTest.drawCardCellsAndTheDeckComeTogether`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L126) |
| No-rest card | `Card.NO_REST`, `Turn.rest`, `Player.noRestCards` | [`GameTest.NoRestCard`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameTest.java#L446) |
| Special cells never on ladder or snake ends | `GameConfig` (validation) | [`GameConfigTest.specialCellsAreNeverOnLadderOrSnakeEndsNorOutsideTheBoard`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L112) |
| Save and load, positions included | `infrastructure/ConfigRepository`, `ConfigFileV1`, `ui/swing/MainWindow` | [`ConfigRepositoryTest.savedFilesLoadBackUnchanged`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/infrastructure/ConfigRepositoryTest.java#L64), [`ladderAndSnakePositionsAreSavedNotJustTheirNumber`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/infrastructure/ConfigRepositoryTest.java#L72) |
| Step-by-step and automatic advance | `ui/swing/GamePanel` (`step`, `togglePlay`) | [`GamePanelTest.stepAnimatesTheMoveAndThenRests`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/swing/GamePanelTest.java#L36), [`wholeGameCanBePlayedAndDrawnWithoutScreen`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/swing/GamePanelTest.java#L20) |
| Graphical interface | `ui/swing/*` | `BoardRendererTest`, `GamePanelTest` |
| Clear errors on invalid configurations | `GameConfig`, `ConfigRepository`, `ConfigDialog` | [`GameConfigTest.everyProblemIsReportedAtOnce`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/GameConfigTest.java#L152), [`ConfigRepositoryTest.unknownMissingAndNullFieldsAreErrors`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/infrastructure/ConfigRepositoryTest.java#L91) |
| Replay by seed | `application/Simulation` | [`SimulationTest.theSameSeedReplaysTheSameGame`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/application/SimulationTest.java#L19), [`CliTest.theSameSeedPrintsTheSameGame`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/cli/CliTest.java#L40), `GamePropertiesTest` |
| Statistics over many games | `application/Stats`, `Simulation.batch`, `Cli --games` | `StatsTest`, [`CliTest.gamesOptionPrintsStatistics`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/ui/cli/CliTest.java#L59) |
| Valid random boards | `domain/RandomBoards`, `ConfigDialog` | [`RandomBoardsTest.everyGeneratedBoardIsValid`](../src/test/java/io/github/giuseppecuiuri/snakesandladders/domain/RandomBoardsTest.java#L38) |
| Layering and no global state | the whole code base | `ArchitectureTest` |

### Edge cases

| Case | Covered by |
|---|---|
| Several players on the same cell | `GameTest.DrawCards` (five players on one cell), `BoardRendererTest.tokensOnTheSameCellSitSideBySide` |
| A ladder or snake leading to a special cell | impossible by rule: `GameConfigTest.specialCellsAreNeverOnLadderOrSnakeEndsNorOutsideTheBoard` |
| A spring after a move, chained into a snake | `GameTest.BonusCells.theSpringCanLeadOntoSnakeHead` |
| A no-rest card on an inn | `GameTest.NoRestCard.keptCardCancelsAnInnStayAndGoesBackToTheBottom` |
| A cyclic deck that can never run empty | `GameTest.DrawCards.cardsAreDealtFromTheTopAndGoBackToTheBottom`, `GameTest.NoRestCard.deckNeedsOneCardOtherThanTheNoRestCard` |
| A spring that would bounce onto itself forever | `GameTest.BonusCells.springThatBouncesBackOntoItselfEndsTheTurn` |
| Invalid configurations: overlaps, ladders going down, snakes going up, special cells on ends | `GameConfigTest` |
