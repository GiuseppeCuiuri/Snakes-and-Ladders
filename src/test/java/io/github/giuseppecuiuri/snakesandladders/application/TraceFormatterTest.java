package io.github.giuseppecuiuri.snakesandladders.application;

import static io.github.giuseppecuiuri.snakesandladders.application.TraceFormatter.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.giuseppecuiuri.snakesandladders.domain.Card;
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
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceFormatterTest {

  @Test
  void everyEventHasItsTraceLine() {
    assertThat(format(new TurnStarted(3, 2))).isEqualTo("Turn 3, player 2");
    assertThat(format(new DiceRolled(2, List.of(4, 5)))).isEqualTo("  rolls 4 + 5 = 9");
    assertThat(format(new DiceRolled(2, List.of(3)))).isEqualTo("  rolls 3");
    assertThat(format(new PlayerMoved(2, 0, 9, false))).isEqualTo("  moves from 0 to 9");
    assertThat(format(new PlayerMoved(2, 97, 98, true)))
        .isEqualTo("  moves from 97, overshoots the last cell and bounces back to 98");
    assertThat(format(new LadderClimbed(2, 9, 31))).isEqualTo("  climbs the ladder from 9 to 31");
    assertThat(format(new SnakeSlid(2, 98, 79))).isEqualTo("  slides down the snake from 98 to 79");
    assertThat(format(new SpecialReached(2, 14, Special.BENCH))).isEqualTo("  lands on a bench");
    assertThat(format(new SpecialReached(2, 24, Special.INN))).isEqualTo("  lands on an inn");
    assertThat(format(new SpecialReached(2, 20, Special.DICE))).isEqualTo("  lands on a dice cell");
    assertThat(format(new SpecialReached(2, 20, Special.SPRING))).isEqualTo("  lands on a spring");
    assertThat(format(new SpecialReached(2, 5, Special.DRAW_CARD)))
        .isEqualTo("  lands on a draw-a-card cell");
    assertThat(format(new CardDrawn(2, Card.BENCH))).isEqualTo("  draws a bench card");
    assertThat(format(new CardDrawn(2, Card.INN))).isEqualTo("  draws an inn card");
    assertThat(format(new CardDrawn(2, Card.DICE))).isEqualTo("  draws a dice card");
    assertThat(format(new CardDrawn(2, Card.SPRING))).isEqualTo("  draws a spring card");
    assertThat(format(new CardDrawn(2, Card.NO_REST)))
        .isEqualTo("  draws a no-rest card and keeps it");
    assertThat(format(new RestAvoided(2))).isEqualTo("  plays the no-rest card and does not rest");
    assertThat(format(new RestStarted(2, 1))).isEqualTo("  has to rest for 1 turn");
    assertThat(format(new RestStarted(2, 3))).isEqualTo("  has to rest for 3 turns");
    assertThat(format(new TurnSkipped(2, 2))).isEqualTo("  rests, 2 more to go");
    assertThat(format(new TurnSkipped(2, 0))).isEqualTo("  rests");
    assertThat(format(new DoubleSix(2))).isEqualTo("  double six: rolls again");
    assertThat(format(new GameWon(2, 40))).isEqualTo("Player 2 wins on turn 40");
  }

  @Test
  void theHeaderNamesBoardPlayersAndSeed() {
    assertThat(TraceFormatter.header("preset compact-48", 3, 42))
        .isEqualTo("preset compact-48, 3 players, seed 42");
    assertThat(TraceFormatter.header("my.json", 1, -7)).isEqualTo("my.json, 1 player, seed -7");
  }
}
