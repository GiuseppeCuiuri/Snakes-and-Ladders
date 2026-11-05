# ADR-0003: How the rules interact

- Status: accepted
- Date: 2026-11-02

## Context

The base game and its optional rules are defined independently, which leaves open how they
interact. A simulation has nobody to ask, so every open case needs one fixed answer.

## Decision

1. **Start.** Players start off the board, on "cell 0", and play in a fixed round-robin order.
   Tokens on the same cell do not interact.
2. **Winning.** Landing exactly on the last cell N, by any move, wins and ends both the turn and
   the game.
3. **Bounce.** A move that overshoots N goes back by the excess. Boards have at least 12 cells, so
   a bounce never goes below cell 1.
4. **Ladders and snakes.** A player moves along them only when landing on the foot or the head.
   No cell is an end of two of them, so they never chain.
5. **Single die near the end.** With two dice and this rule on, a single die is rolled exactly when
   the player is on a cell from N-6 to N-1. This applies to every roll, bonus rolls included.
6. **Double six.** After a double six, once the move and any ladder, snake or special cell are
   resolved, the player rolls again. The bonus is lost if the game is over or if the player has
   just been told to rest. A double six obtained on a bonus roll also earns a bonus.
7. **Rest.** A rest of k turns skips the player's next k turns; the turn in which the player
   landed does not count.
8. **Spring.** The spring moves the player again by the total of the latest roll of this turn,
   with the usual bounce, and resolves the cell reached, which can be a ladder, a snake or another
   special cell.
9. **One firing per turn.** Each special cell takes effect at most once per turn. Without this
   rule a spring near the end can send a player back onto itself forever.
10. **Deck.** The deck holds one card of each type unless the configuration says otherwise. It
    is shuffled once at the start with the game's seeded random generator. A drawn card goes back
    to the bottom of the deck before its effect is applied.
11. **No-rest card.** A drawn no-rest card is kept by the player, who may hold several. It is
    used automatically the first time the player would have to rest, and then goes back to the
    bottom of the deck. The deck can never run out: every configuration needs at least one card
    that is not a no-rest card, and those always go back.
12. **Run parameters.** The number of players and the choice between step-by-step and automatic
    play belong to a run, not to the saved configuration.

## Consequences

Each point has at least one dedicated test. The same configuration and seed always replay the same
game.
