package io.github.giuseppecuiuri.snakesandladders.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DiceRolled;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.GameWon;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.PlayerMoved;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnStarted;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatsTest {

  @Test
  void countsWinsGameLengthsAndLandings() {
    Stats stats = new Stats(2);
    List<GameEvent> firstGame =
        List.of(
            new TurnStarted(1, 1),
            new DiceRolled(1, List.of(4, 5)),
            new PlayerMoved(1, 0, 9, false),
            new TurnStarted(2, 2),
            new PlayerMoved(2, 0, 9, false),
            new GameWon(2, 2));
    List<GameEvent> secondGame =
        List.of(new TurnStarted(1, 1), new PlayerMoved(1, 0, 12, false), new GameWon(1, 4));
    List<GameEvent> unfinishedGame = List.of(new TurnStarted(1, 1));
    firstGame.forEach(stats);
    secondGame.forEach(stats);
    unfinishedGame.forEach(stats);

    assertThat(stats.games()).isEqualTo(3);
    assertThat(stats.unfinished()).isEqualTo(1);
    assertThat(stats.wins(1)).isEqualTo(1);
    assertThat(stats.wins(2)).isEqualTo(1);
    assertThat(stats.averageTurns()).isEqualTo(3.0);
    assertThat(stats.mostLandedOn(1)).containsExactly(Map.entry(9, 2));
    assertThat(stats.report())
        .contains("Games: 3, without a winner: 1")
        .contains("Player 1 won 1 game (33.3%)")
        .contains("Turns per game: 3.0 on average, 2 at least, 4 at most")
        .contains("Most landed-on cells: 9 (2 times) 12 (1 time)");
  }
}
