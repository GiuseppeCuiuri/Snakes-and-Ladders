# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x | yes |

## Reporting a vulnerability

Please report vulnerabilities privately through GitHub: open the **Security** tab of the
repository and choose **Report a vulnerability**. If that is not possible, write to
<giuseppecuiuri@gmail.com>. Do not open a public issue for them.

You can expect a first answer within a week. Fixed issues are listed in the release notes.

## Scope

Snakes and Ladders is a desktop and command-line application: it opens no network connection and
runs no external process. The only input that may come from someone else is a configuration
file, which is read as strict JSON into plain records (no polymorphic typing) and then validated
by the game rules. Java serialization is not used, for that reason.
