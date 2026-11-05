package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One player's turn: rolls, moves, and whatever the cells reached make happen. */
final class Turn {

  private final Game game;
  private final GameConfig config;
  private final Player player;
  private int lastRoll;
  private final Set<Integer> firedSpecials = new HashSet<>();

  Turn(Game game, Player player) {
    this.game = game;
    this.config = game.config();
    this.player = player;
  }

  /**
   * Rolls and moves. With the double-six bonus, rolls again after a double six, unless the game is
   * over or the player has just been told to rest.
   */
  void rollAndMove() {
    boolean rollAgain;
    do {
      GameEvent.DiceRolled roll = rollDice();
      lastRoll = roll.total();
      moveBy(lastRoll);
      rollAgain =
          config.doubleSixBonus()
              && roll.faces().equals(List.of(6, 6))
              && !game.isOver()
              && player.restTurns() == 0;
      if (rollAgain) {
        game.emit(new GameEvent.DoubleSix(player.id()));
      }
    } while (rollAgain);
  }

  /** Moves forward, bouncing back from the last cell, and resolves the cell reached. */
  void moveBy(int steps) {
    int from = player.position();
    int to = from + steps;
    boolean bounced = to > config.size();
    if (bounced) {
      to = 2 * config.size() - to;
    }
    player.moveTo(to);
    game.emit(new GameEvent.PlayerMoved(player.id(), from, to, bounced));
    resolve();
  }

  /** The total of the latest roll of this turn. */
  int lastRoll() {
    return lastRoll;
  }

  /**
   * Draws the top card. The player keeps a no-rest card; any other card goes back to the bottom of
   * the deck and then takes effect.
   */
  void drawCard() {
    Card card = game.drawCard();
    if (card == Card.NO_REST) {
      player.keepNoRestCard();
    } else {
      game.putAtTheBottom(card);
    }
    game.emit(new GameEvent.CardDrawn(player.id(), card));
    card.effect().ifPresent(effect -> effect.applyTo(this));
  }

  /**
   * Makes the player skip their next {@code turns} turns, unless they have a no-rest card: then the
   * card is used and goes back to the bottom of the deck.
   */
  void rest(int turns) {
    if (player.noRestCards() > 0) {
      player.useNoRestCard();
      game.putAtTheBottom(Card.NO_REST);
      game.emit(new GameEvent.RestAvoided(player.id()));
      return;
    }
    player.setRestTurns(turns);
    game.emit(new GameEvent.RestStarted(player.id(), turns));
  }

  private GameEvent.DiceRolled rollDice() {
    boolean nearEnd = player.position() >= config.size() - 6;
    boolean singleDie = config.dice() == 1 || config.singleDieNearEnd() && nearEnd;
    GameEvent.DiceRolled roll =
        new GameEvent.DiceRolled(
            player.id(),
            singleDie ? List.of(game.rollDie()) : List.of(game.rollDie(), game.rollDie()));
    game.emit(roll);
    return roll;
  }

  private void resolve() {
    int cell = player.position();
    if (cell == config.size()) {
      game.win(player);
      return;
    }
    Jump jump = game.jumpAt(cell);
    if (jump != null) {
      // Ladder tops and snake tails are never special cells, so there is nothing more to resolve.
      player.moveTo(jump.to());
      game.emit(
          jump.isLadder()
              ? new GameEvent.LadderClimbed(player.id(), jump.from(), jump.to())
              : new GameEvent.SnakeSlid(player.id(), jump.from(), jump.to()));
      return;
    }
    Special special = config.specials().get(cell);
    // A special cell takes effect at most once per turn: a spring near the end could otherwise
    // bounce the player back onto itself forever.
    if (special != null && firedSpecials.add(cell)) {
      game.emit(new GameEvent.SpecialReached(player.id(), cell, special));
      special.applyTo(this);
    }
  }
}
