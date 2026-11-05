# ADR-0004: Versioned JSON configuration files

- Status: accepted
- Date: 2026-11-02

## Context

A saved configuration must load back as the same board, with every ladder, snake and special cell
in place. Java serialization (`ObjectOutputStream`) would be the shortest way, with two problems:

- The file could not be read or edited by a person.
- Reading it back with `ObjectInputStream` deserializes whatever classes the file names, which is a
  well-known security risk for files from untrusted sources.

## Decision

Save configurations as JSON, through a Data Transfer Object (`ConfigFileV1`) and a Repository
(`ConfigRepository`) in the infrastructure layer:

- The DTO mirrors the file field by field: every ladder, snake, special cell and card is stored.
- `schemaVersion` is read first. A future format gets a new DTO and a migration at that point.
- Reading is strict: unknown, missing, null or repeated fields, nulls inside lists, decimals where
  integers are expected, unknown enum names and text after the object are all errors. Each error
  says what is wrong and where: the JSON path, or the line and column when the JSON is broken.
- The domain rules then validate the result, exactly as for a configuration built in code.
- Files are written with two-space indentation and LF line endings on every platform.
- There is only one storage, so `ConfigRepository` is a class, not an interface.

## Consequences

- Saved boards load back exactly, and the example files in `configs/` can be read and edited by
  hand.
- Jackson is the only runtime library of the domain-facing code; the domain itself does not know
  about it.
