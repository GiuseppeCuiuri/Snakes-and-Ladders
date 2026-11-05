package io.github.giuseppecuiuri.snakesandladders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.giuseppecuiuri.snakesandladders.domain.Game;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationTest {

  private static final GameConfig CLASSIC =
      GameConfig.builder(10, 10).ladder(9, 31).snake(17, 7).snake(98, 79).build();

  @Test
  void theSameSeedReplaysTheSameGame() {
    assertThat(eventsOfGame(42)).isEqualTo(eventsOfGame(42)).isNotEqualTo(eventsOfGame(43));
  }

  @Test
  void batchPlaysEveryGameToTheEnd() {
    Stats stats = Simulation.batch(CLASSIC, 2, 200, 7);

    assertThat(stats.games()).isEqualTo(200);
    assertThat(stats.unfinished()).isZero();
    assertThat(stats.wins(1) + stats.wins(2)).isEqualTo(200);
    assertThat(stats.averageTurns()).isGreaterThan(1);
  }

  @Test
  void theSameSeedGivesTheSameBatch() {
    assertThat(Simulation.batch(CLASSIC, 3, 50, 1).report())
        .isEqualTo(Simulation.batch(CLASSIC, 3, 50, 1).report());
  }

  @Test
  void gamesStoppedAtTheTurnLimitCountAsUnfinished() {
    Stats stats = Simulation.batch(CLASSIC, 2, 5, 7, 1); // one turn can never reach cell 100

    assertThat(stats.games()).isEqualTo(5);
    assertThat(stats.unfinished()).isEqualTo(5);
  }

  @Test
  void batchNeedsAtLeastOneGame() {
    assertThatThrownBy(() -> Simulation.batch(CLASSIC, 2, 0, 7))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("games must be at least 1, not 0");
  }

  private static List<GameEvent> eventsOfGame(long seed) {
    Game game = Simulation.newGame(CLASSIC, 3, seed);
    List<GameEvent> events = new ArrayList<>();
    game.addListener(events::add);
    Simulation.playToTheEnd(game, Simulation.MAX_TURNS);
    return events;
  }
}
