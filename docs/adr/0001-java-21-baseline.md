# ADR-0001: Java 21 as the baseline

- Status: accepted
- Date: 2026-11-02

## Context

The game reports what happens through a sealed hierarchy of event records (`GameEvent`). Java 17
supports sealed types and records, but `switch` with type and record patterns only became final
in Java 21. On Java 17 every consumer of the events (the text trace, the statistics, the GUI)
would need `instanceof` chains, and the compiler would not notice a forgotten event type.

## Decision

Compile with `--release 21`. Build and test in CI on JDK 21 and JDK 25, the two current LTS
releases.

## Consequences

- Consumers of `GameEvent` use exhaustive `switch` statements: adding an event type breaks the
  build until every consumer handles it.
- Running the application needs Java 21 or newer.
