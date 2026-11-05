package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import io.github.giuseppecuiuri.snakesandladders.ui.swing.BoardRenderer.Token;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Writes the README images from the real application, drawn without a screen: two screenshots of
 * the playing area, an animated GIF of a whole game, the README banner and the 1280 x 640 social
 * preview for GitHub. Run it after {@code ./mvnw verify}:
 *
 * <pre>
 * ./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
 * java -Djava.awt.headless=true -cp "target/classes:target/test-classes:$(cat target/cp.txt)" \
 *   io.github.giuseppecuiuri.snakesandladders.ui.swing.Screenshots docs/assets
 * </pre>
 *
 * <p>On Windows the classpath separator is {@code ;} instead of {@code :}.
 */
final class Screenshots {

  private static final int GIF_WIDTH = 420;
  private static final int GIF_HEIGHT = 470;

  private Screenshots() {}

  public static void main(String[] args) throws IOException {
    Path folder = Path.of(args.length > 0 ? args[0] : "docs/assets");
    Files.createDirectories(folder);
    ConfigRepository repository = new ConfigRepository();
    Setup everything =
        new Setup("preset everything-on", repository.preset("everything-on"), 4, 2024);

    FlatLightLaf.setup();
    GamePanel light = new GamePanel(everything);
    light.fastForward(28);
    ImageIO.write(
        light.snapshot(1180, 780), "png", folder.resolve("screenshot-light.png").toFile());

    FlatDarkLaf.setup();
    GamePanel dark = new GamePanel(everything);
    dark.setTheme(Theme.DARK);
    dark.fastForward(10_000);
    ImageIO.write(dark.snapshot(1180, 780), "png", folder.resolve("screenshot-dark.png").toFile());

    FlatLightLaf.setup();
    Setup compact = new Setup("preset compact-48", repository.preset("compact-48"), 2, 11);
    GamePanel recorded = new GamePanel(compact);
    recorded.advanceAnimation(0); // time moves only when the tool says so
    writeGif(recorded, folder.resolve("demo.gif"));

    GameConfig classic = repository.preset("classic-100");
    ImageIO.write(poster(classic, 1280, 320, 52), "png", folder.resolve("banner.png").toFile());
    ImageIO.write(
        poster(classic, 1280, 640, 72), "png", folder.resolve("social-preview.png").toFile());
  }

  /** The title and a few facts on the left, a board with some tokens on the right. */
  private static BufferedImage poster(GameConfig config, int width, int height, int titleSize) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setColor(Theme.LIGHT.background());
    g.fillRect(0, 0, width, height);

    int boardWidth = Math.min(width * 2 / 5, (int) (height * 0.9));
    int boardLeft = width - boardWidth - height / 20;
    Graphics2D board = (Graphics2D) g.create(boardLeft, 0, boardWidth, height);
    new BoardRenderer(config, Theme.LIGHT, boardWidth, height)
        .paint(board, List.of(Token.at(1, 23), Token.at(2, 57), Token.at(3, 57), Token.at(4, 88)));
    board.dispose();

    int left = height / 8;
    String title = "Snakes and Ladders";
    // Shrink the title until it fits between the left margin and the board.
    Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, titleSize);
    while (g.getFontMetrics(titleFont).stringWidth(title) > boardLeft - left - height / 16) {
      titleSize--;
      titleFont = titleFont.deriveFont((float) titleSize);
    }
    int titleY = height / 2 - titleSize / 4;
    g.setColor(new Color(0x2E3440));
    g.setFont(titleFont);
    g.drawString(title, left, titleY);
    g.setColor(new Color(0x2E8B57));
    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, titleSize * 2 / 5));
    g.drawString("Design patterns in practice", left, titleY + titleSize * 3 / 4);
    g.setColor(new Color(0x6B6253));
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, titleSize * 3 / 10));
    g.drawString("Java 21 · Swing · every rule tested", left, titleY + titleSize * 3 / 2);
    g.dispose();
    return image;
  }

  /**
   * A whole game, one frame per 160 ms of animation. After the first frame only the rectangle that
   * changed is stored, which keeps the file small.
   */
  private static void writeGif(GamePanel panel, Path file) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
    // Through an OutputStream, so an older and longer file is truncated rather than overwritten.
    try (OutputStream stream = Files.newOutputStream(file);
        ImageOutputStream out = ImageIO.createImageOutputStream(stream)) {
      writer.setOutput(out);
      writer.prepareWriteSequence(null);
      BufferedImage previous = null;
      while (!panel.isOver()) {
        panel.step();
        while (panel.isAnimating()) {
          panel.advanceAnimation(160);
          previous = writeFrame(writer, previous, panel.boardSnapshot(GIF_WIDTH, GIF_HEIGHT), 12);
        }
      }
      writeFrame(writer, previous, panel.boardSnapshot(GIF_WIDTH, GIF_HEIGHT), 400);
      writer.endWriteSequence();
    } finally {
      writer.dispose();
    }
  }

  /** Writes the part of {@code frame} that differs from {@code previous}, and returns frame. */
  private static BufferedImage writeFrame(
      ImageWriter writer, BufferedImage previous, BufferedImage frame, int delayCentiseconds)
      throws IOException {
    Rectangle changed =
        previous == null
            ? new Rectangle(0, 0, frame.getWidth(), frame.getHeight())
            : changedArea(previous, frame);
    BufferedImage part =
        new BufferedImage(changed.width, changed.height, BufferedImage.TYPE_INT_RGB);
    part.getGraphics()
        .drawImage(
            frame.getSubimage(changed.x, changed.y, changed.width, changed.height), 0, 0, null);

    ImageWriteParam param = writer.getDefaultWriteParam();
    IIOMetadata metadata =
        writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(part), param);
    String format = metadata.getNativeMetadataFormatName();
    IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
    IIOMetadataNode descriptor =
        (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
    descriptor.setAttribute("imageLeftPosition", String.valueOf(changed.x));
    descriptor.setAttribute("imageTopPosition", String.valueOf(changed.y));
    root.appendChild(timing(delayCentiseconds));
    if (previous == null) {
      root.appendChild(loopForever());
    }
    metadata.setFromTree(format, root);
    writer.writeToSequence(new IIOImage(part, null, metadata), param);
    return frame;
  }

  /** The smallest rectangle containing every pixel that changed; one pixel if none did. */
  private static Rectangle changedArea(BufferedImage before, BufferedImage after) {
    int minX = after.getWidth();
    int minY = after.getHeight();
    int maxX = -1;
    int maxY = -1;
    for (int y = 0; y < after.getHeight(); y++) {
      for (int x = 0; x < after.getWidth(); x++) {
        if (before.getRGB(x, y) != after.getRGB(x, y)) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }
    return maxX < 0
        ? new Rectangle(0, 0, 1, 1)
        : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
  }

  private static IIOMetadataNode timing(int delayCentiseconds) {
    IIOMetadataNode control = new IIOMetadataNode("GraphicControlExtension");
    control.setAttribute("disposalMethod", "doNotDispose");
    control.setAttribute("userInputFlag", "FALSE");
    control.setAttribute("transparentColorFlag", "FALSE");
    control.setAttribute("delayTime", String.valueOf(delayCentiseconds));
    control.setAttribute("transparentColorIndex", "0");
    return control;
  }

  private static IIOMetadataNode loopForever() {
    IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
    loop.setAttribute("applicationID", "NETSCAPE");
    loop.setAttribute("authenticationCode", "2.0");
    loop.setUserObject(new byte[] {1, 0, 0});
    IIOMetadataNode extensions = new IIOMetadataNode("ApplicationExtensions");
    extensions.appendChild(loop);
    return extensions;
  }
}
