package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * Creates boards with ladders, snakes and special cells on random cells (a Factory).
 *
 * <p>Placement always terminates: the request is checked against the free cells first, and every
 * pick is made from a finite list of cells that are still valid, so an impossible request fails
 * with an explanation instead of looping. Placement is greedy: on a very crowded board it can give
 * up even though some arrangement would fit, and another seed usually succeeds.
 */
public final class RandomBoards {

  private RandomBoards() {}

  /**
   * A builder for a {@code rows} x {@code columns} board with ladders, snakes and special cells on
   * random cells. Dice options and the deck are left to the caller.
   *
   * @param specials how many special cells of each type to place
   * @throws IllegalArgumentException if the board is invalid or cannot fit the request
   */
  public static GameConfig.Builder builder(
      int rows,
      int columns,
      int ladders,
      int snakes,
      Map<Special, Integer> specials,
      RandomGenerator random) {
    GameConfig.builder(rows, columns).build(); // rejects an invalid board with the usual messages
    if (ladders < 0
        || snakes < 0
        || specials.values().stream().anyMatch(count -> count == null || count < 0)) {
      throw new IllegalArgumentException("counts cannot be negative");
    }
    int specialCount = specials.values().stream().mapToInt(Integer::intValue).sum();
    int freeCells = rows * columns - 1;
    int needed = 2 * (ladders + snakes) + specialCount;
    if (needed > freeCells) {
      throw new IllegalArgumentException(
          "a "
              + rows
              + " x "
              + columns
              + " board has "
              + freeCells
              + " free cells, but "
              + ladders
              + " ladders, "
              + snakes
              + " snakes and "
              + specialCount
              + " special cells need "
              + needed);
    }

    List<Integer> free = new ArrayList<>(IntStream.range(1, rows * columns).boxed().toList());
    GameConfig.Builder builder = GameConfig.builder(rows, columns);
    for (int i = 1; i <= ladders; i++) {
      int[] ladder = pickJump(free, columns, true, random);
      if (ladder.length == 0) {
        throw new IllegalArgumentException(cannotPlace("ladder", i, ladders));
      }
      builder.ladder(ladder[0], ladder[1]);
    }
    for (int i = 1; i <= snakes; i++) {
      int[] snake = pickJump(free, columns, false, random);
      if (snake.length == 0) {
        throw new IllegalArgumentException(cannotPlace("snake", i, snakes));
      }
      builder.snake(snake[0], snake[1]);
    }
    for (Special special : Special.values()) {
      for (int i = 0; i < specials.getOrDefault(special, 0); i++) {
        builder.special(free.remove(random.nextInt(free.size())), special);
      }
    }
    return builder;
  }

  /** Picks a start and an end on different rows, upwards or downwards; empty if none is left. */
  private static int[] pickJump(
      List<Integer> free, int columns, boolean upwards, RandomGenerator random) {
    // A start needs a free cell on a row beyond its own: the lowest and highest free rows, found in
    // one pass, answer that for every candidate, so each pick stays linear in the free cells.
    IntSummaryStatistics freeRows =
        free.stream().mapToInt(cell -> (cell - 1) / columns).summaryStatistics();
    List<Integer> starts =
        free.stream()
            .filter(
                start ->
                    upwards
                        ? (start - 1) / columns < freeRows.getMax()
                        : (start - 1) / columns > freeRows.getMin())
            .toList();
    if (starts.isEmpty()) {
      return new int[0];
    }
    int start = starts.get(random.nextInt(starts.size()));
    List<Integer> ends = free.stream().filter(end(start, columns, upwards)::test).toList();
    int end = ends.get(random.nextInt(ends.size()));
    free.remove(Integer.valueOf(start));
    free.remove(Integer.valueOf(end));
    return new int[] {start, end};
  }

  /** Accepts the cells on a higher row than {@code start}, or on a lower row if downwards. */
  private static IntPredicate end(int start, int columns, boolean upwards) {
    int startRow = (start - 1) / columns;
    return cell -> upwards ? (cell - 1) / columns > startRow : (cell - 1) / columns < startRow;
  }

  private static String cannotPlace(String kind, int index, int count) {
    return "cannot place "
        + kind
        + " "
        + index
        + " of "
        + count
        + ": no two free cells on different rows are left";
  }
}
