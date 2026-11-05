package io.github.giuseppecuiuri.snakesandladders.ui;

import io.github.giuseppecuiuri.snakesandladders.ui.cli.Cli;
import io.github.giuseppecuiuri.snakesandladders.ui.swing.MainWindow;
import java.awt.GraphicsEnvironment;

/** Starts the window, or the command line when there are arguments or no screen. */
public final class Main {

  private Main() {}

  /** The entry point of the jar. */
  public static void main(String[] args) {
    if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
      MainWindow.open();
    } else {
      Cli.runAndExit(args.length == 0 ? new String[] {"--headless"} : args);
    }
  }
}
