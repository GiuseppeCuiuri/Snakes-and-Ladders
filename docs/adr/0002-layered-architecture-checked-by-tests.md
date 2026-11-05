# ADR-0002: Layered architecture, checked by tests

- Status: accepted
- Date: 2026-11-02

## Context

The game has two interfaces, a window and a command line, and many rules to test. Game logic that
depends on Swing, on the console or on global state could not be tested without them, and the
second interface would have to duplicate it.

## Decision

Four layers under `io.github.giuseppecuiuri.snakesandladders`:

| Layer | Contents | May use |
|---|---|---|
| `domain` | rules, configuration, events | only `java.lang` and `java.util` |
| `application` | game setup with a seed, trace text, statistics | `domain` |
| `infrastructure` | JSON configuration files | `domain` |
| `ui` | Swing window, command-line interface | all of the above |

ArchUnit tests enforce the dependencies, keep `System.out` inside the command-line interface, and
require every static field to be final. The domain receives its random generator; only the application
layer creates one.

## Consequences

- The rules can be tested without a screen, a console or a file system.
- The GUI and the command line see the game only through its public API and its events.
