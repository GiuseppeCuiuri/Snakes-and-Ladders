package io.github.giuseppecuiuri.snakesandladders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import org.junit.jupiter.api.Test;

class GameConfigTest {

  @Test
  void classicBoardHasOneHundredCellsAndTwoDice() {
    GameConfig config = GameConfig.builder(10, 10).ladder(9, 31).snake(17, 7).build();

    assertThat(config.size()).isEqualTo(100);
    assertThat(config.dice()).isEqualTo(2);
    assertThat(config.singleDieNearEnd()).isFalse();
    assertThat(config.doubleSixBonus()).isFalse();
    assertThat(config.ladders()).containsExactly(new Jump(9, 31));
    assertThat(config.snakes()).containsExactly(new Jump(17, 7));
  }

  @Test
  void rowsAndColumnsCanChange() {
    GameConfig config = GameConfig.builder(8, 6).build();

    assertThat(config.size()).isEqualTo(48);
    assertThat(config.rowOf(1)).isZero();
    assertThat(config.rowOf(6)).isZero();
    assertThat(config.rowOf(7)).isOne();
    assertThat(config.rowOf(48)).isEqualTo(7);
    assertThatThrownBy(() -> config.rowOf(0)) // the start area is not a row of the board
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("cell 0 is not on the board: cells go from 1 to 48");
  }

  @Test
  void theConfigurationIsImmutable() {
    GameConfig config = GameConfig.builder(10, 10).ladder(9, 31).build();

    assertThatThrownBy(() -> config.ladders().add(new Jump(2, 40)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void theBoardMustHaveSensibleSides() {
    assertRejected(GameConfig.builder(0, 10), "rows must be between 1 and 30, not 0");
    assertRejected(GameConfig.builder(10, 31), "columns must be between 1 and 30, not 31");
    assertRejected(GameConfig.builder(3, 3), "the board needs at least 12 cells, not 9");
  }

  @Test
  void oneOrTwoDiceAndTheOptionalRulesNeedTwo() {
    assertRejected(GameConfig.builder(10, 10).dice(3), "dice must be 1 or 2, not 3");
    assertRejected(
        GameConfig.builder(10, 10).dice(1).singleDieNearEnd(true),
        "the single-die-near-the-end rule needs two dice");
    assertRejected(
        GameConfig.builder(10, 10).dice(1).doubleSixBonus(true),
        "the double-six rule needs two dice");
    assertThat(GameConfig.builder(10, 10).dice(1).build().dice()).isOne();
  }

  @Test
  void laddersGoUpAndSnakesGoDown() {
    assertRejected(GameConfig.builder(10, 10).ladder(31, 9), "ladder 31 -> 9 must go up");
    assertRejected(GameConfig.builder(10, 10).snake(7, 17), "snake 7 -> 17 must go down");
  }

  @Test
  void bothEndsMustBeOnTheBoardBeforeTheLastCell() {
    assertRejected(
        GameConfig.builder(10, 10).ladder(90, 100),
        "ladder 90 -> 100: both ends must be cells from 1 to 99");
    assertRejected(
        GameConfig.builder(10, 10).snake(100, 80),
        "snake 100 -> 80: both ends must be cells from 1 to 99");
    assertRejected(
        GameConfig.builder(10, 10).snake(20, 0),
        "snake 20 -> 0: both ends must be cells from 1 to 99");
  }

  @Test
  void ladderAndSnakeEndsAreOnDifferentRows() {
    assertRejected(
        GameConfig.builder(10, 10).ladder(2, 5), "ladder 2 -> 5 must connect two different rows");
    assertThat(GameConfig.builder(10, 10).ladder(10, 11).build().ladders()).hasSize(1);
  }

  @Test
  void noCellIsAnEndOfTwoLaddersOrSnakes() {
    assertRejected(
        GameConfig.builder(10, 10).ladder(9, 31).ladder(9, 40),
        "cell 9 is an end of more than one ladder or snake");
    assertRejected(
        GameConfig.builder(10, 10).ladder(9, 31).snake(31, 12),
        "cell 31 is an end of more than one ladder or snake");
  }

  @Test
  void specialCellsAreKeptInCellOrder() {
    GameConfig config =
        GameConfig.builder(10, 10).special(24, Special.INN).special(14, Special.BENCH).build();

    assertThat(config.specials()).containsExactly(entry(14, Special.BENCH), entry(24, Special.INN));
    assertThatThrownBy(() -> config.specials().put(30, Special.BENCH))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void specialCellsAreNeverOnLadderOrSnakeEndsNorOutsideTheBoard() {
    assertRejected(
        GameConfig.builder(10, 10).ladder(9, 31).special(31, Special.BENCH),
        "special cell 31 (BENCH) cannot be on an end of a ladder or snake");
    assertRejected(
        GameConfig.builder(10, 10).special(100, Special.INN),
        "special cell 100 (INN) must be a cell from 1 to 99");
    assertRejected(
        GameConfig.builder(10, 10).special(0, Special.INN),
        "special cell 0 (INN) must be a cell from 1 to 99");
    assertRejected(GameConfig.builder(10, 10).special(5, null), "special cell 5 has no type");
  }

  @Test
  void drawCardCellsAndTheDeckComeTogether() {
    GameConfig config =
        GameConfig.builder(10, 10)
            .special(5, Special.DRAW_CARD)
            .deck(List.of(Card.SPRING, Card.BENCH))
            .build();

    assertThat(config.deck()).containsExactly(Card.SPRING, Card.BENCH);
    assertRejected(
        GameConfig.builder(10, 10).special(5, Special.DRAW_CARD),
        "draw-a-card cells need a deck with at least one card");
    assertRejected(
        GameConfig.builder(10, 10).deck(Card.standardDeck(false)),
        "a deck needs at least one draw-a-card cell");
  }

  @Test
  void cellCannotHoldTwoSpecialCells() {
    GameConfig.Builder builder = GameConfig.builder(10, 10).special(20, Special.BENCH);

    assertThatThrownBy(() -> builder.special(20, Special.INN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cell 20 has more than one special cell");
  }

  @Test
  void everyProblemIsReportedAtOnce() {
    assertThatThrownBy(
            () -> GameConfig.builder(10, 10).dice(1).doubleSixBonus(true).ladder(31, 9).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            """
            Invalid configuration:
            - the double-six rule needs two dice
            - ladder 31 -> 9 must go up\
            """);
  }

  private static void assertRejected(GameConfig.Builder builder, String problem) {
    assertThatThrownBy(builder::build)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(problem);
  }
}
