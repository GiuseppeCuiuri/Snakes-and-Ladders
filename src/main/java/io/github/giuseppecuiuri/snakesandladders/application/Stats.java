package io.github.giuseppecuiuri.snakesandladders.application;

import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.CardDrawn;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DiceRolled;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DoubleSix;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.GameWon;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.LadderClimbed;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.PlayerMoved;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestAvoided;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestStarted;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SnakeSlid;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SpecialReached;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnSkipped;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnStarted;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Statistics over any number of games, collected from their events (an Observer): wins per player,
 * game length and the cells players land on most. Attach one instance to every game.
 */
public final class Stats implements Consumer<GameEvent> {

  private final int players;
  private final Map<Integer, Integer> wins = new HashMap<>();
  private final IntSummaryStatistics turnsPerWin = new IntSummaryStatistics();
  private final Map<Integer, Integer> landings = new HashMap<>();
  private int started;

  /** Statistics for games with the given number of players. */
  public Stats(int players) {
    this.players = players;
  }

  @Override
  public void accept(GameEvent event) {
    switch (event) {
      case TurnStarted(int turn, int player) -> {
        if (turn == 1) {
          started++;
        }
      }
      case PlayerMoved(int player, int from, int to, boolean bounced) ->
          landings.merge(to, 1, Integer::sum);
      case GameWon(int player, int turn) -> {
        wins.merge(player, 1, Integer::sum);
        turnsPerWin.accept(turn);
      }
      case DiceRolled d -> {}
      case LadderClimbed l -> {}
      case SnakeSlid s -> {}
      case DoubleSix d -> {}
      case CardDrawn c -> {}
      case SpecialReached s -> {}
      case RestStarted r -> {}
      case RestAvoided r -> {}
      case TurnSkipped t -> {}
    }
  }

  /** Games that started. */
  public int games() {
    return started;
  }

  /** Games stopped without a winner. */
  public int unfinished() {
    return started - (int) turnsPerWin.getCount();
  }

  /** Games won by a player, identified by id (1 for the first). */
  public int wins(int player) {
    return wins.getOrDefault(player, 0);
  }

  /** Average number of turns of the games that have a winner, or 0 if none has. */
  public double averageTurns() {
    return turnsPerWin.getAverage();
  }

  /** The cells players landed on most often after a roll, most frequent first. */
  public List<Map.Entry<Integer, Integer>> mostLandedOn(int limit) {
    return landings.entrySet().stream()
        .sorted(
            Map.Entry.<Integer, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
        .limit(limit)
        .map(Map.Entry::copyOf)
        .toList();
  }

  /** A short plain-text report of all the statistics. */
  public String report() {
    StringBuilder report = new StringBuilder();
    report.append(
        String.format(Locale.ROOT, "Games: %d, without a winner: %d%n", games(), unfinished()));
    for (int player = 1; player <= players; player++) {
      report.append(
          String.format(
              Locale.ROOT,
              "Player %d won %d %s (%.1f%%)%n",
              player,
              wins(player),
              wins(player) == 1 ? "game" : "games",
              100.0 * wins(player) / Math.max(1, games())));
    }
    if (turnsPerWin.getCount() > 0) {
      report.append(
          String.format(
              Locale.ROOT,
              "Turns per game: %.1f on average, %d at least, %d at most%n",
              averageTurns(),
              turnsPerWin.getMin(),
              turnsPerWin.getMax()));
    }
    report.append("Most landed-on cells:");
    for (Map.Entry<Integer, Integer> cell : mostLandedOn(5)) {
      report.append(
          String.format(
              Locale.ROOT,
              " %d (%d %s)",
              cell.getKey(),
              cell.getValue(),
              cell.getValue() == 1 ? "time" : "times"));
    }
    return report.append(String.format(Locale.ROOT, "%n")).toString();
  }
}
