package io.github.giuseppecuiuri.snakesandladders.ui.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {

  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  private int run(String... args) {
    return Cli.run(
        args,
        new PrintStream(out, true, StandardCharsets.UTF_8),
        new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  private String out() {
    return out.toString(StandardCharsets.UTF_8);
  }

  @Test
  void headlessGamePrintsTraceUntilSomeoneWins() {
    assertThat(run("--headless", "--players", "3", "--seed", "42")).isZero();

    assertThat(out())
        .startsWith("preset classic-100, 3 players, seed 42")
        .contains("Turn 1, player 1", "  rolls ", "  moves from 0 to ")
        .containsPattern("Player [123] wins on turn \\d+");
  }

  @Test
  void theSameSeedPrintsTheSameGame() {
    run("--headless", "--preset", "everything-on", "--seed", "7");
    String first = out();
    out.reset();
    run("--headless", "--preset", "everything-on", "--seed", "7");

    assertThat(out()).isEqualTo(first);
  }

  @Test
  void configurationFilesCanBePlayed(@TempDir Path folder) throws Exception {
    Path file = folder.resolve("compact.json");
    Files.copy(Path.of("configs", "compact-48.json"), file);

    assertThat(run("--config", file.toString(), "--seed", "1")).isZero();
    assertThat(out()).startsWith(file + ", 2 players, seed 1");
  }

  @Test
  void gamesOptionPrintsStatistics() {
    assertThat(run("--games", "200", "--players", "2", "--seed", "3")).isZero();

    assertThat(out())
        .contains("Games: 200, without a winner: 0")
        .contains("Player 1 won", "Player 2 won", "Turns per game: ", "Most landed-on cells:");
  }

  @Test
  void badArgumentsExplainWhatIsWrong() {
    assertThat(run("--players", "9")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .startsWith("--players must be between 1 and 6, not 9")
        .contains("Usage: java -jar snakes-and-ladders-<version>.jar");

    err.reset();
    assertThat(run("--seed")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).startsWith("--seed needs a value");

    err.reset();
    assertThat(run("--preset", "giant")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).startsWith("There is no preset called");

    err.reset();
    assertThat(run("--preset", "compact-48", "--config", "x.json")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .startsWith("Use either --preset or --config, not both");

    err.reset();
    assertThat(run("--fast")).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8)).startsWith("Unknown option --fast");
  }

  @Test
  void helpShowsTheUsage() {
    assertThat(run("--help")).isZero();
    assertThat(out()).isEqualTo(Cli.USAGE);
  }

  @Test
  void unreadableFileIsNamedWithoutTheUsage(@TempDir Path folder) {
    Path missing = folder.resolve("missing.json");

    assertThat(run("--config", missing.toString())).isEqualTo(2);
    assertThat(err.toString(StandardCharsets.UTF_8))
        .isEqualTo("Cannot read " + missing + ": there is no such file" + System.lineSeparator());
  }
}
