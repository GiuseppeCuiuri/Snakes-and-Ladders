package io.github.giuseppecuiuri.snakesandladders.application;

import io.github.giuseppecuiuri.snakesandladders.domain.Game;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.RandomBoards;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

/**
 * The single entry point the user interfaces use to set up games (a Facade): it owns the seeding of
 * the dice, so the same configuration and seed always replay the same game.
 */
public final class Simulation {

  /**
   * A game is stopped after this many turns. Valid boards can still make winning very unlikely, for
   * example with snake heads on every cell a bounce can reach.
   */
  public static final int MAX_TURNS = 10_000;

  private Simulation() {}

  /** A fresh seed, for a game the user did not ask to replay. */
  public static long newSeed() {
    return RandomGenerator.getDefault().nextLong();
  }

  /**
   * A board with ladders, snakes and special cells on random cells chosen with {@code seed}; dice
   * options and the deck are left to the caller.
   */
  public static GameConfig.Builder randomBoard(
      int rows, int columns, int ladders, int snakes, Map<Special, Integer> specials, long seed) {
    return RandomBoards.builder(
        rows, columns, ladders, snakes, specials, new SplittableRandom(seed));
  }

  /** A new game whose dice are seeded with {@code seed}. */
  public static Game newGame(GameConfig config, int players, long seed) {
    return new Game(config, players, new SplittableRandom(seed));
  }

  /**
   * Plays {@code games} games to the end, each with its own dice derived from {@code seed}, and
   * returns their statistics.
   */
  public static Stats batch(GameConfig config, int players, int games, long seed) {
    return batch(config, players, games, seed, MAX_TURNS);
  }

  static Stats batch(GameConfig config, int players, int games, long seed, int maxTurns) {
    if (games < 1) {
      throw new IllegalArgumentException("games must be at least 1, not " + games);
    }
    Stats stats = new Stats(players);
    SplittableRandom seeds = new SplittableRandom(seed);
    for (int i = 0; i < games; i++) {
      Game game = new Game(config, players, seeds.split());
      game.addListener(stats);
      playToTheEnd(game, maxTurns);
    }
    return stats;
  }

  /** Plays turns until someone wins or {@code maxTurns} turns have been played. */
  public static void playToTheEnd(Game game, int maxTurns) {
    while (!game.isOver() && game.turnsPlayed() < maxTurns) {
      game.playTurn();
    }
  }
}
