package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.List;
import java.util.Optional;

/**
 * A card of the deck used by the draw-a-card cells.
 *
 * <p>Drawing an effect card has the same effect as landing on the matching special cell: the card
 * runs the very same {@link Special} command.
 */
public enum Card {

  /** Same effect as a bench cell. */
  BENCH(Special.BENCH),

  /** Same effect as an inn cell. */
  INN(Special.INN),

  /** Same effect as a dice cell. */
  DICE(Special.DICE),

  /** Same effect as a spring cell. */
  SPRING(Special.SPRING),

  /**
   * Kept by the player instead of being played, and used to avoid the next rest; it goes back to
   * the bottom of the deck once used.
   */
  NO_REST(null);

  private final Special effect;

  Card(Special effect) {
    this.effect = effect;
  }

  /** The special cell whose effect this card has; empty for the no-rest card. */
  public Optional<Special> effect() {
    return Optional.ofNullable(effect);
  }

  /** One card of each effect, plus a no-rest card if asked for: the standard deck. */
  public static List<Card> standardDeck(boolean withNoRestCard) {
    return withNoRestCard
        ? List.of(BENCH, INN, DICE, SPRING, NO_REST)
        : List.of(BENCH, INN, DICE, SPRING);
  }
}
