package io.github.giuseppecuiuri.snakesandladders.ui.cli;

import io.github.giuseppecuiuri.snakesandladders.application.Simulation;
import io.github.giuseppecuiuri.snakesandladders.application.TraceFormatter;
import io.github.giuseppecuiuri.snakesandladders.domain.Game;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * The command-line interface: an observer who picks the number of players and watches a text trace
 * of the simulated game, plus batches of games with statistics.
 */
public final class Cli {

  static final String USAGE =
      """
      Usage: java -jar snakes-and-ladders-<version>.jar [options]
        (no options opens the window)

        --headless           play one game in the terminal with a text trace
        --players N          1 to 6 players (default 2)
        --preset NAME        classic-100, compact-48 or everything-on (default classic-100)
        --config FILE        a configuration file saved by the application
        --seed N             replay a game: the same seed gives the same game
        --games N            play N games without a trace and print statistics
        --help               show this help
      """;

  private Cli() {}

  /** Runs the command line with the process's console and exits with its status. */
  public static void runAndExit(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  /**
   * Runs the command line.
   *
   * @return 0 on success, 2 if the arguments or the configuration are invalid
   */
  static int run(String[] args, PrintStream out, PrintStream err) {
    Options options;
    try {
      options = Options.parse(args);
    } catch (IllegalArgumentException e) {
      err.println(e.getMessage());
      err.print(USAGE);
      return 2;
    }
    if (options.help()) {
      out.print(USAGE);
      return 0;
    }
    GameConfig config;
    try {
      config = load(options);
    } catch (IllegalArgumentException | IOException e) {
      err.println(e.getMessage()); // the options were fine, so the usage would only hide this
      return 2;
    }

    long seed = options.seed() != null ? options.seed() : Simulation.newSeed();
    out.println(TraceFormatter.header(options.source(), options.players(), seed));
    if (options.games() != null) {
      out.print(Simulation.batch(config, options.players(), options.games(), seed).report());
      return 0;
    }
    Game game = Simulation.newGame(config, options.players(), seed);
    game.addListener(event -> out.println(TraceFormatter.format(event)));
    Simulation.playToTheEnd(game, Simulation.MAX_TURNS);
    if (!game.isOver()) {
      out.println("No winner after " + Simulation.MAX_TURNS + " turns");
    }
    return 0;
  }

  private static GameConfig load(Options options) throws IOException {
    ConfigRepository repository = new ConfigRepository();
    return options.configFile() != null
        ? repository.load(Path.of(options.configFile()))
        : repository.preset(options.preset());
  }

  /** The parsed command-line options. */
  record Options(
      boolean help, int players, String preset, String configFile, Long seed, Integer games) {

    static Options parse(String[] args) {
      boolean help = false;
      int players = 2;
      String preset = "classic-100";
      String configFile = null;
      Long seed = null;
      Integer games = null;
      for (int i = 0; i < args.length; i++) {
        String option = args[i];
        switch (option) {
          case "--help", "-h" -> help = true;
          case "--headless" -> {
            // the default once any option is given
          }
          case "--players" -> players = number(option, value(args, ++i), 1, 6);
          case "--preset" -> preset = value(args, ++i);
          case "--config" -> configFile = value(args, ++i);
          case "--seed" -> seed = longNumber(option, value(args, ++i));
          case "--games" -> games = number(option, value(args, ++i), 1, 1_000_000);
          default -> throw new IllegalArgumentException("Unknown option " + option);
        }
      }
      if (configFile != null && List.of(args).contains("--preset")) {
        throw new IllegalArgumentException("Use either --preset or --config, not both");
      }
      return new Options(help, players, preset, configFile, seed, games);
    }

    String source() {
      return configFile != null ? configFile : "preset " + preset;
    }

    private static String value(String[] args, int index) {
      if (index >= args.length) {
        throw new IllegalArgumentException(args[index - 1] + " needs a value");
      }
      return args[index];
    }

    private static int number(String option, String text, int min, int max) {
      long value = longNumber(option, text);
      if (value < min || value > max) {
        throw new IllegalArgumentException(
            option + " must be between " + min + " and " + max + ", not " + text);
      }
      return (int) value;
    }

    private static long longNumber(String option, String text) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(option + " needs a whole number, not " + text, e);
      }
    }
  }
}
