package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.List;

/**
 * Something that happened in a game, reported to the listeners in the order it happened. Players
 * are identified by their {@link Player#id() id}.
 *
 * <p>The hierarchy is sealed, so a {@code switch} over a {@code GameEvent} that misses a case does
 * not compile.
 */
public sealed interface GameEvent {

  /**
   * A player's turn begins.
   *
   * @param turn the number of the turn, counting all the turns of the game from 1
   * @param player the player whose turn it is
   */
  record TurnStarted(int turn, int player) implements GameEvent {}

  /**
   * The dice were rolled.
   *
   * @param player the player who rolled
   * @param faces one face with a single die, two otherwise
   */
  record DiceRolled(int player, List<Integer> faces) implements GameEvent {

    /** Copies the faces. */
    public DiceRolled {
      faces = List.copyOf(faces);
    }

    /** The sum of the faces. */
    public int total() {
      return faces.stream().mapToInt(Integer::intValue).sum();
    }
  }

  /**
   * The player's token moved along the path.
   *
   * @param player the player who moved
   * @param from the cell the move started from, 0 for the start area
   * @param to the cell reached
   * @param bounced true when the move overshot the last cell and came back
   */
  record PlayerMoved(int player, int from, int to, boolean bounced) implements GameEvent {}

  /**
   * The player landed on the foot of a ladder and climbed it.
   *
   * @param player the player who climbed
   * @param from the foot of the ladder
   * @param to the top of the ladder
   */
  record LadderClimbed(int player, int from, int to) implements GameEvent {}

  /**
   * The player landed on the head of a snake and slid down to its tail.
   *
   * @param player the player who slid
   * @param from the head of the snake
   * @param to the tail of the snake
   */
  record SnakeSlid(int player, int from, int to) implements GameEvent {}

  /**
   * The player landed on a special cell, which now takes effect.
   *
   * @param player the player who landed
   * @param cell the cell landed on
   * @param special what the cell does
   */
  record SpecialReached(int player, int cell, Special special) implements GameEvent {}

  /**
   * The player drew a card from the deck.
   *
   * @param player the player who drew
   * @param card the card drawn
   */
  record CardDrawn(int player, Card card) implements GameEvent {}

  /**
   * The player has to skip their next turns.
   *
   * @param player the player who rests
   * @param turns how many of their next turns they skip
   */
  record RestStarted(int player, int turns) implements GameEvent {}

  /**
   * The player used a no-rest card instead of resting.
   *
   * @param player the player who used the card
   */
  record RestAvoided(int player) implements GameEvent {}

  /**
   * A resting player skips this turn.
   *
   * @param player the player who skips
   * @param turnsLeft how many more turns they will skip after this one
   */
  record TurnSkipped(int player, int turnsLeft) implements GameEvent {}

  /**
   * The player rolled a double six with the double-six bonus on, and rolls again.
   *
   * @param player the player who rolls again
   */
  record DoubleSix(int player) implements GameEvent {}

  /**
   * The player reached the last cell and won; the game is over.
   *
   * @param player the winner
   * @param turn the number of the winning turn
   */
  record GameWon(int player, int turn) implements GameEvent {}
}
