package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.awt.Color;
import java.util.List;

/**
 * The colours of the board, in a light and a dark variant. Player colours come from the Okabe-Ito
 * palette, which stays distinguishable with the common kinds of colour blindness; tokens also carry
 * the player's number.
 */
record Theme(
    boolean dark,
    Color background,
    Color cell,
    Color alternateCell,
    Color grid,
    Color number,
    Color ladder,
    Color snake,
    Color snakeHead,
    Color finish,
    List<Color> players) {

  static final Theme LIGHT =
      new Theme(
          false,
          new Color(0xF7F3EA),
          new Color(0xFFFFFF),
          new Color(0xEFE9DC),
          new Color(0xD3C9B6),
          new Color(0x6B6253),
          new Color(0x8D6E63),
          new Color(0x2E8B57),
          new Color(0x1E5E3B),
          new Color(0xF2C14E),
          okabeIto());

  static final Theme DARK =
      new Theme(
          true,
          new Color(0x1C2026),
          new Color(0x2A2F37),
          new Color(0x242930),
          new Color(0x3B424D),
          new Color(0xA3ACB9),
          new Color(0xC49A6C),
          new Color(0x55B07F),
          new Color(0x2F7A52),
          new Color(0xD9A93C),
          okabeIto());

  /** The fill colour of a special cell. */
  Color special(Special special) {
    Color base =
        switch (special) {
          case BENCH -> new Color(0xF4D35E);
          case INN -> new Color(0xEE8C6E);
          case DICE -> new Color(0x8CC7A1);
          case SPRING -> new Color(0x7DB8EA);
          case DRAW_CARD -> new Color(0xB59BE6);
        };
    return dark ? base.darker() : base;
  }

  /** The colour of a player's token, by id from 1. */
  Color player(int id) {
    return players.get((id - 1) % players.size());
  }

  private static List<Color> okabeIto() {
    return List.of(
        new Color(0x0072B2),
        new Color(0xE69F00),
        new Color(0x009E73),
        new Color(0xCC79A7),
        new Color(0xD55E00),
        new Color(0x56B4E9));
  }
}
