package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.Jump;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a board with plain Java2D, so the same code paints the window and the screenshots made
 * without a screen. Cells follow the traditional serpentine path: cell 1 is bottom left and every
 * other row runs right to left.
 */
final class BoardRenderer {

  /**
   * Where a token is drawn: on cell {@code from} when {@code progress} is 0, on cell {@code to}
   * when it is 1. In between it walks the path cell by cell, or flies straight for a ladder or a
   * snake. Cell 0 is the start area below the board.
   */
  record Token(int player, int from, int to, double progress, boolean straight) {

    static Token at(int player, int cell) {
      return new Token(player, cell, cell, 0, false);
    }

    boolean resting() {
      return from == to || progress >= 1;
    }

    int cell() {
      return progress >= 1 ? to : from;
    }
  }

  private final GameConfig config;
  private final Theme theme;
  private final double cellSize;
  private final double left;
  private final double top;

  BoardRenderer(GameConfig config, Theme theme, int width, int height) {
    this.config = config;
    this.theme = theme;
    double margin = 12;
    double startArea = 0.9;
    this.cellSize =
        Math.max(
            4,
            Math.min(
                (width - 2 * margin) / config.columns(),
                (height - 2 * margin) / (config.rows() + startArea)));
    this.left = (width - cellSize * config.columns()) / 2;
    this.top = (height - cellSize * (config.rows() + startArea)) / 2;
  }

  /** The centre of a cell on screen; cell 0 is the start area below cell 1. */
  Point2D center(int cell) {
    if (cell <= 0) {
      Point2D first = center(1);
      return new Point2D.Double(first.getX(), first.getY() + cellSize * 0.95);
    }
    int index = cell - 1;
    int rowFromBottom = index / config.columns();
    int column = index % config.columns();
    if (rowFromBottom % 2 != 0) {
      column = config.columns() - 1 - column;
    }
    return new Point2D.Double(
        left + (column + 0.5) * cellSize,
        top + (config.rows() - 1 - rowFromBottom + 0.5) * cellSize);
  }

  double cellSize() {
    return cellSize;
  }

  /** Tokens are small enough for six of them to sit side by side in one cell. */
  double tokenRadius() {
    return cellSize * 0.15;
  }

  /** Paints the whole board with the tokens on top. */
  void paint(Graphics2D g, List<Token> tokens) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    paintCells(g);
    config.ladders().forEach(ladder -> paintLadder(g, ladder));
    config.snakes().forEach(snake -> paintSnake(g, snake));
    paintStartArea(g);
    paintTokens(g, tokens);
  }

  private void paintCells(Graphics2D g) {
    Font numberFont = new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(8, cellSize * 0.22));
    g.setFont(numberFont);
    for (int cell = 1; cell <= config.size(); cell++) {
      Rectangle2D square = square(cell);
      Special special = config.specials().get(cell);
      Color fill;
      if (cell == config.size()) {
        fill = theme.finish();
      } else if (special != null) {
        fill = theme.special(special);
      } else {
        fill = config.rowOf(cell) % 2 == cell % 2 ? theme.cell() : theme.alternateCell();
      }
      g.setColor(fill);
      g.fill(square);
      g.setColor(theme.grid());
      g.setStroke(new BasicStroke(1f));
      g.draw(square);
      if (special != null) {
        paintIcon(g, special, iconBox(square));
      } else if (cell == config.size()) {
        paintFlag(g, iconBox(square));
      }
      g.setColor(special != null || cell == config.size() ? Color.DARK_GRAY : theme.number());
      g.drawString(
          String.valueOf(cell),
          (float) (square.getX() + cellSize * 0.08),
          (float) (square.getMaxY() - cellSize * 0.08));
    }
  }

  private Rectangle2D square(int cell) {
    Point2D center = center(cell);
    return new Rectangle2D.Double(
        center.getX() - cellSize / 2, center.getY() - cellSize / 2, cellSize, cellSize);
  }

  private Rectangle2D iconBox(Rectangle2D square) {
    double size = cellSize * 0.42;
    return new Rectangle2D.Double(
        square.getMaxX() - size - cellSize * 0.1, square.getY() + cellSize * 0.1, size, size);
  }

  /** Draws the symbol of a special cell in a box; also used by the legend. */
  static void paintIcon(Graphics2D g, Special special, Rectangle2D box) {
    double x = box.getX();
    double y = box.getY();
    double w = box.getWidth();
    double h = box.getHeight();
    g.setColor(new Color(0x3A3A3A));
    g.setStroke(new BasicStroke((float) Math.max(1, w / 9), BasicStroke.CAP_ROUND, 1));
    switch (special) {
      case BENCH -> {
        g.draw(new Line2D.Double(x, y + h * 0.45, x + w, y + h * 0.45));
        g.draw(new Line2D.Double(x + w * 0.1, y + h * 0.15, x + w * 0.9, y + h * 0.15));
        g.draw(new Line2D.Double(x + w * 0.2, y + h * 0.45, x + w * 0.2, y + h));
        g.draw(new Line2D.Double(x + w * 0.8, y + h * 0.45, x + w * 0.8, y + h));
      }
      case INN -> {
        GeneralPath house = new GeneralPath();
        house.moveTo(x, y + h * 0.45);
        house.lineTo(x + w / 2, y);
        house.lineTo(x + w, y + h * 0.45);
        house.lineTo(x + w * 0.85, y + h * 0.45);
        house.lineTo(x + w * 0.85, y + h);
        house.lineTo(x + w * 0.15, y + h);
        house.lineTo(x + w * 0.15, y + h * 0.45);
        house.closePath();
        g.draw(house);
      }
      case DICE -> {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, w / 3, h / 3));
        double pip = w / 6;
        for (double t : new double[] {0.25, 0.5, 0.75}) {
          g.fill(new Ellipse2D.Double(x + w * t - pip / 2, y + h * t - pip / 2, pip, pip));
        }
      }
      case SPRING -> {
        GeneralPath coil = new GeneralPath();
        coil.moveTo(x + w * 0.2, y + h);
        for (int i = 1; i <= 6; i++) {
          coil.lineTo(x + (i % 2 == 0 ? w * 0.2 : w * 0.8), y + h - i * h / 6);
        }
        g.draw(coil);
      }
      default -> { // DRAW_CARD
        g.draw(new RoundRectangle2D.Double(x + w * 0.25, y, w * 0.7, h * 0.85, w / 5, h / 5));
        g.draw(new RoundRectangle2D.Double(x, y + h * 0.15, w * 0.7, h * 0.85, w / 5, h / 5));
      }
    }
  }

  private void paintFlag(Graphics2D g, Rectangle2D box) {
    g.setColor(new Color(0x3A3A3A));
    g.setStroke(new BasicStroke((float) Math.max(1, box.getWidth() / 9)));
    g.draw(
        new Line2D.Double(
            box.getX() + box.getWidth() * 0.2,
            box.getY(),
            box.getX() + box.getWidth() * 0.2,
            box.getMaxY()));
    GeneralPath flag = new GeneralPath();
    flag.moveTo(box.getX() + box.getWidth() * 0.2, box.getY());
    flag.lineTo(box.getMaxX(), box.getY() + box.getHeight() * 0.25);
    flag.lineTo(box.getX() + box.getWidth() * 0.2, box.getY() + box.getHeight() * 0.5);
    flag.closePath();
    g.fill(flag);
  }

  private void paintLadder(Graphics2D g, Jump ladder) {
    Point2D foot = center(ladder.from());
    Point2D top = center(ladder.to());
    double dx = top.getX() - foot.getX();
    double dy = top.getY() - foot.getY();
    double length = Math.hypot(dx, dy);
    double nx = -dy / length * cellSize * 0.16;
    double ny = dx / length * cellSize * 0.16;
    g.setColor(theme.ladder());
    g.setStroke(
        new BasicStroke((float) (cellSize * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.draw(new Line2D.Double(foot.getX() + nx, foot.getY() + ny, top.getX() + nx, top.getY() + ny));
    g.draw(new Line2D.Double(foot.getX() - nx, foot.getY() - ny, top.getX() - nx, top.getY() - ny));
    int rungs = (int) Math.max(2, length / (cellSize * 0.35));
    for (int i = 1; i < rungs; i++) {
      double t = (double) i / rungs;
      double px = foot.getX() + dx * t;
      double py = foot.getY() + dy * t;
      g.draw(new Line2D.Double(px + nx, py + ny, px - nx, py - ny));
    }
  }

  private void paintSnake(Graphics2D g, Jump snake) {
    Point2D head = center(snake.from());
    Point2D tail = center(snake.to());
    double dx = tail.getX() - head.getX();
    double dy = tail.getY() - head.getY();
    double length = Math.hypot(dx, dy);
    double nx = -dy / length * Math.min(length * 0.25, cellSize * 1.1);
    double ny = dx / length * Math.min(length * 0.25, cellSize * 1.1);
    CubicCurve2D body =
        new CubicCurve2D.Double(
            head.getX(),
            head.getY(),
            head.getX() + dx / 3 + nx,
            head.getY() + dy / 3 + ny,
            head.getX() + 2 * dx / 3 - nx,
            head.getY() + 2 * dy / 3 - ny,
            tail.getX(),
            tail.getY());
    g.setStroke(
        new BasicStroke((float) (cellSize * 0.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.setColor(theme.snake());
    g.draw(body);
    double headSize = cellSize * 0.34;
    g.setColor(theme.snakeHead());
    g.fill(
        new Ellipse2D.Double(
            head.getX() - headSize / 2, head.getY() - headSize / 2, headSize, headSize));
    double eye = headSize * 0.22;
    g.setColor(Color.WHITE);
    g.fill(
        new Ellipse2D.Double(
            head.getX() - headSize * 0.28, head.getY() - headSize * 0.22, eye, eye));
    g.fill(
        new Ellipse2D.Double(
            head.getX() + headSize * 0.06, head.getY() - headSize * 0.22, eye, eye));
  }

  private void paintStartArea(Graphics2D g) {
    Point2D start = center(0);
    g.setColor(theme.number());
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) Math.max(8, cellSize * 0.24)));
    FontMetrics metrics = g.getFontMetrics();
    g.drawString(
        "Start",
        (float) (start.getX() + cellSize * 0.55),
        (float) (start.getY() + metrics.getAscent() / 2.0));
  }

  private void paintTokens(Graphics2D g, List<Token> tokens) {
    Map<Integer, Integer> onCell = new HashMap<>();
    double radius = tokenRadius();
    Font font = new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.max(7, radius * 1.2));
    g.setFont(font);
    for (Token token : tokens) {
      Point2D position;
      if (token.resting()) {
        int cell = token.cell();
        int slot = onCell.merge(cell, 1, Integer::sum) - 1;
        position = offset(center(cell), slot);
      } else {
        position = moving(token);
      }
      double x = position.getX() - radius;
      double y = position.getY() - radius;
      g.setColor(new Color(0, 0, 0, 70));
      g.fill(new Ellipse2D.Double(x + radius * 0.15, y + radius * 0.2, radius * 2, radius * 2));
      g.setColor(theme.player(token.player()));
      g.fill(new Ellipse2D.Double(x, y, radius * 2, radius * 2));
      g.setColor(Color.WHITE);
      g.setStroke(new BasicStroke((float) Math.max(1, radius * 0.18)));
      g.draw(new Ellipse2D.Double(x, y, radius * 2, radius * 2));
      String label = String.valueOf(token.player());
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(
          label,
          (float) (position.getX() - metrics.stringWidth(label) / 2.0),
          (float) (position.getY() + metrics.getAscent() / 2.0 - metrics.getDescent() / 2.0));
    }
  }

  /**
   * Where the {@code slot}-th token on a cell goes, so tokens sharing a cell sit side by side:
   * three per row, two rows.
   */
  Point2D offset(Point2D center, int slot) {
    int column = slot % 3;
    int row = (slot / 3) % 2;
    return new Point2D.Double(
        center.getX() + (column - 1) * cellSize * 0.31,
        center.getY() + (row == 0 ? -0.12 : 0.2) * cellSize);
  }

  private Point2D moving(Token token) {
    if (token.straight()) {
      return lerp(center(token.from()), center(token.to()), token.progress());
    }
    double along = token.from() + (token.to() - token.from()) * token.progress();
    int from = (int) Math.floor(along);
    int to = (int) Math.ceil(along);
    return lerp(center(from), center(to), along - from);
  }

  private static Point2D lerp(Point2D a, Point2D b, double t) {
    return new Point2D.Double(
        a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t);
  }
}
