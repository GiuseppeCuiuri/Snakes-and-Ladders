# Contributing

Thanks for taking an interest. The bar here is "clear and tested" rather than "feature-rich".

## Build

- JDK 21 or newer. Maven is not needed: the wrapper downloads it.
- `./mvnw verify` (on Windows `mvnw.cmd verify`) compiles, runs every test and checks:
  - test coverage of the `domain` package (at least 80% of lines and branches),
  - formatting (Spotless with google-java-format),
  - Checkstyle (Google checks) and SpotBugs, both blocking.
- `./mvnw spotless:apply` formats the code.
- `java -jar target/snakes-and-ladders-1.0.0.jar` opens the window; add `--help` for the
  command line.
- The images in `docs/assets` are drawn by the test-scope `Screenshots` tool; its javadoc has the
  command that regenerates them after a visual change.

## Ground rules

- **Layers**: `domain` knows nothing about files, screens or consoles; `application`,
  `infrastructure` and `ui` build on it. `ArchitectureTest` checks this, along with final static
  fields only and console output only in `ui.cli`.
- **Tests first** for behaviour: a new rule gets a test in `GameTest`; a bug gets a test that
  fails before the fix.
- **Randomness** only through the `RandomGenerator` passed to `Game`, so that a seed replays a
  game exactly.
- **Commits** follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `build:`, `ci:`.
- Decisions with lasting consequences get a short ADR in `docs/adr/`.

## Adding a special cell

The compiler finds most of the places to touch, because events and special cells are handled
with exhaustive `switch` statements:

1. a constant in `domain/Special` with its `applyTo(Turn)` effect, and a test in `GameTest`;
2. a matching `Card` constant if a card should have the same effect;
3. its text in `application/TraceFormatter` and its colour in `ui/swing/Theme` (the compiler
   asks for both);
4. its icon in `ui/swing/BoardRenderer.paintIcon`, its row in the legend of `GamePanel` and its
   spinner in `ConfigDialog` (these three are not checked by the compiler);
5. a line in `docs/REQUIREMENTS.md` and, if it changes the file format, in ADR-0004.

The JSON configuration format needs no change: special cells are stored by name.
