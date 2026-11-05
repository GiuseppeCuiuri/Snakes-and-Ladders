package io.github.giuseppecuiuri.snakesandladders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Map;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class RandomBoardsTest {

  private static final Map<Special, Integer> SOME_SPECIALS =
      Map.of(Special.BENCH, 2, Special.INN, 1, Special.DICE, 1, Special.SPRING, 1);

  private static GameConfig board(long seed) {
    return RandomBoards.builder(10, 10, 8, 8, SOME_SPECIALS, new SplittableRandom(seed)).build();
  }

  @Test
  void placesExactlyWhatWasAskedFor() {
    GameConfig config = board(1);

    assertThat(config.ladders()).hasSize(8);
    assertThat(config.snakes()).hasSize(8);
    assertThat(config.specials().values())
        .containsExactlyInAnyOrder(
            Special.BENCH, Special.BENCH, Special.INN, Special.DICE, Special.SPRING);
  }

  @Test
  void theSameSeedGivesTheSameBoard() {
    assertThat(board(7)).isEqualTo(board(7)).isNotEqualTo(board(8));
  }

  @Test
  void everyGeneratedBoardIsValid() {
    for (long seed = 0; seed < 300; seed++) {
      GameConfig config =
          RandomBoards.builder(8, 6, 6, 6, SOME_SPECIALS, new SplittableRandom(seed)).build();
      for (Jump jump : config.ladders()) {
        assertThat(config.rowOf(jump.to())).isGreaterThan(config.rowOf(jump.from()));
      }
      for (Jump jump : config.snakes()) {
        assertThat(config.rowOf(jump.to())).isLessThan(config.rowOf(jump.from()));
      }
    }
  }

  @Test
  void requestThatCannotFitFailsAtOnce() {
    assertTimeoutPreemptively(
        Duration.ofSeconds(1),
        () ->
            assertThatThrownBy(
                    () -> RandomBoards.builder(8, 6, 20, 20, Map.of(), new SplittableRandom(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                    "a 8 x 6 board has 47 free cells, but 20 ladders, 20 snakes and 0 special"
                        + " cells need 80"));
  }

  @Test
  void runningOutOfRowsIsReported() {
    assertThatThrownBy(() -> RandomBoards.builder(1, 12, 1, 0, Map.of(), new SplittableRandom(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cannot place ladder 1 of 1: no two free cells on different rows are left");
  }

  @Test
  void invalidBoardsAndNegativeCountsAreRejected() {
    assertThatThrownBy(() -> RandomBoards.builder(3, 3, 0, 0, Map.of(), new SplittableRandom(1)))
        .hasMessageContaining("the board needs at least 12 cells");
    assertThatThrownBy(() -> RandomBoards.builder(10, 10, -1, 0, Map.of(), new SplittableRandom(1)))
        .hasMessage("counts cannot be negative");
  }
}
