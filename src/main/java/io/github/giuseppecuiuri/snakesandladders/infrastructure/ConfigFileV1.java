package io.github.giuseppecuiuri.snakesandladders.infrastructure;

import io.github.giuseppecuiuri.snakesandladders.domain.Card;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.Jump;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.util.List;

/**
 * Version 1 of the configuration file (a Data Transfer Object): the JSON document mirrors this
 * record field by field, which keeps the file format apart from the domain classes.
 */
record ConfigFileV1(
    int schemaVersion,
    int rows,
    int columns,
    int dice,
    boolean singleDieNearEnd,
    boolean doubleSixBonus,
    List<JumpEntry> ladders,
    List<JumpEntry> snakes,
    List<SpecialEntry> specials,
    List<Card> deck) {

  static final int VERSION = 1;

  /** A ladder or a snake in the file. */
  record JumpEntry(int from, int to) {}

  /** A special cell in the file. */
  record SpecialEntry(int cell, Special type) {}

  static ConfigFileV1 of(GameConfig config) {
    return new ConfigFileV1(
        VERSION,
        config.rows(),
        config.columns(),
        config.dice(),
        config.singleDieNearEnd(),
        config.doubleSixBonus(),
        entries(config.ladders()),
        entries(config.snakes()),
        config.specials().entrySet().stream()
            .map(special -> new SpecialEntry(special.getKey(), special.getValue()))
            .toList(),
        config.deck());
  }

  /** The domain configuration, which checks every rule of the game. */
  GameConfig toGameConfig() {
    GameConfig.Builder builder =
        GameConfig.builder(rows, columns)
            .dice(dice)
            .singleDieNearEnd(singleDieNearEnd)
            .doubleSixBonus(doubleSixBonus)
            .deck(deck);
    ladders.forEach(ladder -> builder.ladder(ladder.from(), ladder.to()));
    snakes.forEach(snake -> builder.snake(snake.from(), snake.to()));
    specials.forEach(special -> builder.special(special.cell(), special.type()));
    return builder.build();
  }

  private static List<JumpEntry> entries(List<Jump> jumps) {
    return jumps.stream().map(jump -> new JumpEntry(jump.from(), jump.to())).toList();
  }
}
