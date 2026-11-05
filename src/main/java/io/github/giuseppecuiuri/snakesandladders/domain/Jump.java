package io.github.giuseppecuiuri.snakesandladders.domain;

/**
 * A ladder or a snake: a player who lands exactly on {@code from} is moved to {@code to}.
 *
 * @param from the foot of a ladder or the head of a snake
 * @param to the top of a ladder or the tail of a snake
 */
public record Jump(int from, int to) {

  /** True for a ladder (it goes up), false for a snake. */
  public boolean isLadder() {
    return to > from;
  }
}
