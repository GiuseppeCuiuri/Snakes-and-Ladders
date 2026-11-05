package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Everything that defines a board and its rules: size, dice, optional rules, ladders, snakes and
 * special cells.
 *
 * <p>Instances are immutable and always valid: the constructor checks every rule and reports all
 * the problems it finds in a single {@link IllegalArgumentException}, one per line.
 *
 * @param rows rows of the board grid
 * @param columns columns of the board grid
 * @param dice number of dice rolled: 2, or 1 for the single-die variant
 * @param singleDieNearEnd with two dice, roll a single die on cells N-6 to N-1
 * @param doubleSixBonus after a double six, roll again once the move is resolved
 * @param ladders ladders, each going up to a different row
 * @param snakes snakes, each going down to a different row
 * @param specials special cells by cell number, never on an end of a ladder or snake
 * @param deck the cards of the draw-a-card cells, in order; empty when there are no such cells
 */
public record GameConfig(
    int rows,
    int columns,
    int dice,
    boolean singleDieNearEnd,
    boolean doubleSixBonus,
    List<Jump> ladders,
    List<Jump> snakes,
    Map<Integer, Special> specials,
    List<Card> deck) {

  /** Smallest board: from 12 cells up, no bounce can take a player below cell 1. */
  public static final int MIN_CELLS = 12;

  /** Largest number of rows, and of columns. */
  public static final int MAX_SIDE = 30;

  /** Copies the collections and checks every rule. */
  public GameConfig {
    ladders = List.copyOf(ladders);
    snakes = List.copyOf(snakes);
    specials = Collections.unmodifiableSortedMap(new TreeMap<>(specials));
    deck = List.copyOf(deck);
    List<String> problems = new ArrayList<>();
    checkBoard(rows, columns, problems);
    boolean validBoard = problems.isEmpty();
    checkDice(dice, singleDieNearEnd, doubleSixBonus, problems);
    if (validBoard) {
      Set<Integer> ends = checkJumps(rows * columns, columns, ladders, snakes, problems);
      checkSpecials(rows * columns, specials, ends, problems);
    }
    checkDeck(specials, deck, problems);
    if (!problems.isEmpty()) {
      throw new IllegalArgumentException(
          "Invalid configuration:\n- " + String.join("\n- ", problems));
    }
  }

  /** Starts a configuration for a board of the given size: two dice, no optional rules. */
  public static Builder builder(int rows, int columns) {
    return new Builder(rows, columns);
  }

  /** The number of cells, N; the last cell is N. */
  public int size() {
    return rows * columns;
  }

  /**
   * The row of a cell, counting from 0 for the row of cell 1.
   *
   * @throws IllegalArgumentException for a cell off the board, such as 0, the start area
   */
  public int rowOf(int cell) {
    if (cell < 1 || cell > size()) {
      throw new IllegalArgumentException(
          "cell " + cell + " is not on the board: cells go from 1 to " + size());
    }
    return row(cell, columns);
  }

  private static int row(int cell, int columns) {
    return (cell - 1) / columns;
  }

  private static void checkBoard(int rows, int columns, List<String> problems) {
    if (rows < 1 || rows > MAX_SIDE) {
      problems.add("rows must be between 1 and " + MAX_SIDE + ", not " + rows);
    }
    if (columns < 1 || columns > MAX_SIDE) {
      problems.add("columns must be between 1 and " + MAX_SIDE + ", not " + columns);
    }
    if (problems.isEmpty() && rows * columns < MIN_CELLS) {
      problems.add("the board needs at least " + MIN_CELLS + " cells, not " + rows * columns);
    }
  }

  private static void checkDice(
      int dice, boolean singleDieNearEnd, boolean doubleSixBonus, List<String> problems) {
    if (dice != 1 && dice != 2) {
      problems.add("dice must be 1 or 2, not " + dice);
    }
    if (dice == 1 && singleDieNearEnd) {
      problems.add("the single-die-near-the-end rule needs two dice");
    }
    if (dice == 1 && doubleSixBonus) {
      problems.add("the double-six rule needs two dice");
    }
  }

  private static Set<Integer> checkJumps(
      int size, int columns, List<Jump> ladders, List<Jump> snakes, List<String> problems) {
    Set<Integer> ends = new HashSet<>();
    for (Jump ladder : ladders) {
      checkJump("ladder", ladder, size, columns, ends, problems);
      if (ladder.to() <= ladder.from()) {
        problems.add("ladder " + describe(ladder) + " must go up");
      }
    }
    for (Jump snake : snakes) {
      checkJump("snake", snake, size, columns, ends, problems);
      if (snake.to() >= snake.from()) {
        problems.add("snake " + describe(snake) + " must go down");
      }
    }
    return ends;
  }

  private static void checkSpecials(
      int size, Map<Integer, Special> specials, Set<Integer> ends, List<String> problems) {
    specials.forEach(
        (cell, special) -> {
          String name = "special cell " + cell + " (" + special + ")";
          if (special == null) {
            problems.add("special cell " + cell + " has no type");
          } else if (!onBoardBeforeLastCell(cell, size)) {
            problems.add(name + " must be a cell from 1 to " + (size - 1));
          } else if (ends.contains(cell)) {
            problems.add(name + " cannot be on an end of a ladder or snake");
          }
        });
  }

  private static void checkJump(
      String kind, Jump jump, int size, int columns, Set<Integer> ends, List<String> problems) {
    String name = kind + " " + describe(jump);
    if (!onBoardBeforeLastCell(jump.from(), size) || !onBoardBeforeLastCell(jump.to(), size)) {
      problems.add(name + ": both ends must be cells from 1 to " + (size - 1));
      return;
    }
    if (row(jump.from(), columns) == row(jump.to(), columns)) {
      problems.add(name + " must connect two different rows");
    }
    for (int end : new int[] {jump.from(), jump.to()}) {
      if (!ends.add(end)) {
        problems.add("cell " + end + " is an end of more than one ladder or snake");
      }
    }
  }

  private static void checkDeck(
      Map<Integer, Special> specials, List<Card> deck, List<String> problems) {
    boolean drawCells = specials.containsValue(Special.DRAW_CARD);
    if (drawCells && deck.isEmpty()) {
      problems.add("draw-a-card cells need a deck with at least one card");
    }
    if (!drawCells && !deck.isEmpty()) {
      problems.add("a deck needs at least one draw-a-card cell");
    }
    if (!deck.isEmpty() && deck.stream().allMatch(card -> card == Card.NO_REST)) {
      problems.add("the deck needs at least one card other than the no-rest card");
    }
  }

  private static boolean onBoardBeforeLastCell(int cell, int size) {
    return cell >= 1 && cell < size;
  }

  private static String describe(Jump jump) {
    return jump.from() + " -> " + jump.to();
  }

  /** Builds a {@link GameConfig} step by step. */
  public static final class Builder {

    private final int rows;
    private final int columns;
    private int dice = 2;
    private boolean singleDieNearEnd;
    private boolean doubleSixBonus;
    private final List<Jump> ladders = new ArrayList<>();
    private final List<Jump> snakes = new ArrayList<>();
    private final Map<Integer, Special> specials = new TreeMap<>();
    private final List<Card> deck = new ArrayList<>();

    private Builder(int rows, int columns) {
      this.rows = rows;
      this.columns = columns;
    }

    /** Sets the number of dice, 1 or 2. */
    public Builder dice(int dice) {
      this.dice = dice;
      return this;
    }

    /** Turns the single die near the end on or off. */
    public Builder singleDieNearEnd(boolean on) {
      this.singleDieNearEnd = on;
      return this;
    }

    /** Turns the double-six bonus on or off. */
    public Builder doubleSixBonus(boolean on) {
      this.doubleSixBonus = on;
      return this;
    }

    /** Adds a ladder from its foot up to its top. */
    public Builder ladder(int foot, int top) {
      ladders.add(new Jump(foot, top));
      return this;
    }

    /** Adds a snake from its head down to its tail. */
    public Builder snake(int head, int tail) {
      snakes.add(new Jump(head, tail));
      return this;
    }

    /**
     * Puts a special cell on a cell.
     *
     * @throws IllegalArgumentException if the cell already has a special cell
     */
    public Builder special(int cell, Special special) {
      if (specials.putIfAbsent(cell, special) != null) {
        throw new IllegalArgumentException(
            "Invalid configuration:\n- cell " + cell + " has more than one special cell");
      }
      return this;
    }

    /** Sets the cards of the deck, top card first. */
    public Builder deck(List<Card> cards) {
      deck.clear();
      deck.addAll(cards);
      return this;
    }

    /** Checks every rule and returns the configuration. */
    public GameConfig build() {
      return new GameConfig(
          rows, columns, dice, singleDieNearEnd, doubleSixBonus, ladders, snakes, specials, deck);
    }
  }
}
