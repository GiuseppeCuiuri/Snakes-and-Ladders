package io.github.giuseppecuiuri.snakesandladders.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DiceRolled;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.LadderClimbed;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.PlayerMoved;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SnakeSlid;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/** Properties that hold on any valid board, checked on randomly generated games. */
class GamePropertiesTest {

  @Property(tries = 300)
  void tokensStayOnTheBoardAndDiceShowOneToSix(
      @ForAll @IntRange(min = 2, max = 12) int rows,
      @ForAll @IntRange(min = 6, max = 12) int columns,
      @ForAll @IntRange(min = 0, max = 6) int ladders,
      @ForAll @IntRange(min = 0, max = 6) int snakes,
      @ForAll @IntRange(min = 0, max = 2) int specialsOfEachType,
      @ForAll boolean twoDice,
      @ForAll boolean singleDieNearEnd,
      @ForAll boolean doubleSixBonus,
      @ForAll boolean noRestCard,
      @ForAll @IntRange(min = 1, max = 6) int players,
      @ForAll long seed) {
    Assume.that(2 * (ladders + snakes) + 5 * specialsOfEachType < rows * columns);
    SplittableRandom random = new SplittableRandom(seed);
    Map<Special, Integer> specials = new EnumMap<>(Special.class);
    for (Special special : Special.values()) {
      specials.put(special, specialsOfEachType);
    }
    GameConfig.Builder builder;
    try {
      builder = RandomBoards.builder(rows, columns, ladders, snakes, specials, random);
    } catch (IllegalArgumentException crowded) {
      Assume.that(false); // the greedy placement gave up on a crowded board
      return;
    }
    builder
        .dice(twoDice ? 2 : 1)
        .singleDieNearEnd(twoDice && singleDieNearEnd)
        .doubleSixBonus(twoDice && doubleSixBonus);
    if (specialsOfEachType > 0) {
      builder.deck(Card.standardDeck(noRestCard));
    }
    GameConfig config = builder.build();
    Game game = new Game(config, players, random);
    List<GameEvent> events = new ArrayList<>();
    game.addListener(events::add);

    for (int turn = 0; turn < 300 && !game.isOver(); turn++) {
      game.playTurn(); // every turn returns: no special cell can make a turn loop forever
    }

    for (GameEvent event : events) {
      switch (event) {
        case PlayerMoved moved -> assertThat(moved.to()).isBetween(1, config.size());
        case LadderClimbed ladder -> assertThat(ladder.to()).isBetween(1, config.size() - 1);
        case SnakeSlid snake -> assertThat(snake.to()).isBetween(1, config.size() - 1);
        case DiceRolled roll -> {
          assertThat(roll.faces()).allSatisfy(face -> assertThat(face).isBetween(1, 6));
          assertThat(roll.faces().size()).isLessThanOrEqualTo(config.dice());
        }
        default -> {
          // the other events carry no position or die face
        }
      }
    }
    assertThat(game.players())
        .allSatisfy(p -> assertThat(p.position()).isBetween(0, config.size()));
  }

  @Property(tries = 50)
  void theSameSeedReplaysTheSameGame(@ForAll long seed) {
    assertThat(playWithEveryRule(seed)).isEqualTo(playWithEveryRule(seed));
  }

  private static List<GameEvent> playWithEveryRule(long seed) {
    SplittableRandom random = new SplittableRandom(seed);
    Map<Special, Integer> specials = new EnumMap<>(Special.class);
    for (Special special : Special.values()) {
      specials.put(special, 2);
    }
    GameConfig config =
        RandomBoards.builder(10, 10, 8, 8, specials, random)
            .singleDieNearEnd(true)
            .doubleSixBonus(true)
            .deck(Card.standardDeck(true))
            .build();
    Game game = new Game(config, 4, random);
    List<GameEvent> events = new ArrayList<>();
    game.addListener(events::add);
    for (int turn = 0; turn < 500 && !game.isOver(); turn++) {
      game.playTurn();
    }
    return events;
  }
}
