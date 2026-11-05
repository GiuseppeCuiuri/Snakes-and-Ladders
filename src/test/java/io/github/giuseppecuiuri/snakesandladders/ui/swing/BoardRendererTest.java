package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import io.github.giuseppecuiuri.snakesandladders.ui.swing.BoardRenderer.Token;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BoardRendererTest {

  private static final GameConfig CLASSIC = new ConfigRepository().preset("classic-100");

  private final BoardRenderer renderer = new BoardRenderer(CLASSIC, Theme.LIGHT, 600, 650);

  @Test
  void cellsFollowSerpentinePathFromTheBottomLeft() {
    Point2D one = renderer.center(1);
    Point2D ten = renderer.center(10);
    Point2D eleven = renderer.center(11);
    Point2D hundred = renderer.center(100);

    assertThat(ten.getY()).isEqualTo(one.getY());
    assertThat(ten.getX()).isGreaterThan(one.getX());
    assertThat(eleven.getX()).isEqualTo(ten.getX()); // the second row starts above cell 10
    assertThat(eleven.getY()).isLessThan(ten.getY());
    assertThat(hundred.getX()).isEqualTo(one.getX()); // ten rows: the last one runs right to left
    assertThat(hundred.getY()).isLessThan(eleven.getY());
  }

  @Test
  void playersWaitingToStartAreDrawnBelowTheBoard() {
    assertThat(renderer.center(0).getY()).isGreaterThan(renderer.center(1).getY());
    assertThat(renderer.center(0).getY()).isLessThan(650);
  }

  @Test
  void tokensOnTheSameCellSitSideBySide() {
    List<Point2D> spots = new ArrayList<>();
    for (int slot = 0; slot < 6; slot++) {
      spots.add(renderer.offset(renderer.center(50), slot));
    }
    for (Point2D spot : spots) {
      for (Point2D other : spots) {
        if (spot != other) {
          assertThat(spot.distance(other)).isGreaterThanOrEqualTo(2 * renderer.tokenRadius());
        }
      }
    }
  }

  @Test
  void ladderTopsLookLikeOrdinaryCellsNotLikeRestCells() {
    BufferedImage image = new BufferedImage(600, 650, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    renderer.paint(g, List.of(Token.at(1, 0), Token.at(2, 0)));
    g.dispose();

    Color top = cornerColour(image, 31); // the top of the ladder 9 -> 31
    assertThat(top).isIn(Theme.LIGHT.cell(), Theme.LIGHT.alternateCell());
    assertThat(top).isNotEqualTo(Theme.LIGHT.special(Special.BENCH));
  }

  @Test
  void specialCellsGetTheirOwnColour() {
    GameConfig everything = new ConfigRepository().preset("everything-on");
    BoardRenderer withSpecials = new BoardRenderer(everything, Theme.LIGHT, 600, 650);
    BufferedImage image = new BufferedImage(600, 650, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    withSpecials.paint(g, List.of());
    g.dispose();

    everything
        .specials()
        .forEach(
            (cell, special) ->
                assertThat(cornerColour(image, withSpecials, cell))
                    .isEqualTo(Theme.LIGHT.special(special)));
  }

  private Color cornerColour(BufferedImage image, int cell) {
    return cornerColour(image, renderer, cell);
  }

  private static Color cornerColour(BufferedImage image, BoardRenderer renderer, int cell) {
    Point2D center = renderer.center(cell);
    double half = renderer.cellSize() / 2;
    return new Color(
        image.getRGB((int) (center.getX() - half + 4), (int) (center.getY() - half + 4)));
  }
}
