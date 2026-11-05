package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Runs the playing area without a window, as the screenshot tool does. */
class GamePanelTest {

  private static Setup everything(int players) {
    return new Setup(
        "preset everything-on", new ConfigRepository().preset("everything-on"), players, 42);
  }

  @Test
  void wholeGameCanBePlayedAndDrawnWithoutScreen() {
    GamePanel panel = new GamePanel(everything(4));

    panel.fastForward(10_000);
    BufferedImage image = panel.snapshot(1100, 760);

    Set<Integer> colours = new HashSet<>();
    for (int x = 0; x < image.getWidth(); x += 7) {
      for (int y = 0; y < image.getHeight(); y += 7) {
        colours.add(image.getRGB(x, y));
      }
    }
    assertThat(colours).hasSizeGreaterThan(20);
  }

  @Test
  void stepAnimatesTheMoveAndThenRests() {
    GamePanel panel = new GamePanel(everything(2));
    panel.advanceAnimation(0); // drive the animation by hand, as the screenshot tool does

    panel.step();
    assertThat(panel.isAnimating()).isTrue();
    for (int i = 0; i < 200 && panel.isAnimating(); i++) {
      panel.advanceAnimation(1_000);
    }
    assertThat(panel.isAnimating()).isFalse();
  }

  @Test
  void restartingReplaysTheSameGame() {
    GamePanel panel = new GamePanel(everything(3));
    panel.fastForward(30);
    BufferedImage first = panel.boardSnapshot(500, 540);

    panel.start(panel.setup());
    panel.fastForward(30);
    BufferedImage second = panel.boardSnapshot(500, 540);

    assertThat(pixels(second)).isEqualTo(pixels(first));
  }

  private static int[] pixels(BufferedImage image) {
    return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
  }
}
