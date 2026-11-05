package io.github.giuseppecuiuri.snakesandladders.domain;

/**
 * A special cell and what it does to the player who lands on it.
 *
 * <p>Each constant is a command (Command pattern): the turn runs it through {@link #applyTo(Turn)}
 * without knowing which one it is.
 */
public enum Special {

  /** Rest cell: the player skips the next turn. */
  BENCH {
    @Override
    void applyTo(Turn turn) {
      turn.rest(1);
    }
  },

  /** Rest cell: the player skips the next three turns. */
  INN {
    @Override
    void applyTo(Turn turn) {
      turn.rest(3);
    }
  },

  /** Bonus cell: the player rolls the dice again and moves. */
  DICE {
    @Override
    void applyTo(Turn turn) {
      turn.rollAndMove();
    }
  },

  /** Bonus cell: the player moves again by the latest roll, without rolling. */
  SPRING {
    @Override
    void applyTo(Turn turn) {
      turn.moveBy(turn.lastRoll());
    }
  },

  /** Draw-a-card cell: the player draws the top card of the deck. */
  DRAW_CARD {
    @Override
    void applyTo(Turn turn) {
      turn.drawCard();
    }
  };

  abstract void applyTo(Turn turn);
}
