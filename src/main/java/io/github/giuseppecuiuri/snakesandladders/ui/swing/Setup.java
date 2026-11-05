package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;

/** Everything needed to start (or restart) a game: a board, the players and the dice seed. */
record Setup(String name, GameConfig config, int players, long seed) {}
