package io.github.giuseppecuiuri.snakesandladders.application;

import io.github.giuseppecuiuri.snakesandladders.domain.Card;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.CardDrawn;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DiceRolled;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.DoubleSix;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.GameWon;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.LadderClimbed;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.PlayerMoved;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestAvoided;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.RestStarted;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SnakeSlid;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.SpecialReached;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnSkipped;
import io.github.giuseppecuiuri.snakesandladders.domain.GameEvent.TurnStarted;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.util.List;

/**
 * Turns game events into the lines of the text trace: one header line per turn, then one indented
 * line per thing that happened in it.
 */
public final class TraceFormatter {

  private TraceFormatter() {}

  /** The trace line for an event. */
  public static String format(GameEvent event) {
    return switch (event) {
      case TurnStarted(int turn, int player) -> "Turn " + turn + ", player " + player;
      case DiceRolled roll -> "  rolls " + describe(roll);
      case PlayerMoved(int player, int from, int to, boolean bounced) ->
          bounced
              ? "  moves from " + from + ", overshoots the last cell and bounces back to " + to
              : "  moves from " + from + " to " + to;
      case LadderClimbed(int player, int from, int to) ->
          "  climbs the ladder from " + from + " to " + to;
      case SnakeSlid(int player, int from, int to) ->
          "  slides down the snake from " + from + " to " + to;
      case SpecialReached(int player, int cell, Special special) ->
          "  lands on " + describe(special);
      case RestAvoided(int player) -> "  plays the no-rest card and does not rest";
      case RestStarted(int player, int turns) ->
          "  has to rest for " + turns + (turns == 1 ? " turn" : " turns");
      case TurnSkipped(int player, int turnsLeft) ->
          turnsLeft == 0 ? "  rests" : "  rests, " + turnsLeft + " more to go";
      case CardDrawn(int player, Card card) -> "  draws " + describe(card);
      case DoubleSix(int player) -> "  double six: rolls again";
      case GameWon(int player, int turn) -> "Player " + player + " wins on turn " + turn;
    };
  }

  /** The first line of a trace: where the board comes from, how many play, and the seed. */
  public static String header(String board, int players, long seed) {
    return board + ", " + players + (players == 1 ? " player" : " players") + ", seed " + seed;
  }

  private static String describe(Special special) {
    return switch (special) {
      case BENCH -> "a bench";
      case INN -> "an inn";
      case DICE -> "a dice cell";
      case SPRING -> "a spring";
      case DRAW_CARD -> "a draw-a-card cell";
    };
  }

  private static String describe(Card card) {
    return switch (card) {
      case BENCH -> "a bench card";
      case INN -> "an inn card";
      case DICE -> "a dice card";
      case SPRING -> "a spring card";
      case NO_REST -> "a no-rest card and keeps it";
    };
  }

  private static String describe(DiceRolled roll) {
    List<Integer> faces = roll.faces();
    if (faces.size() == 1) {
      return String.valueOf(faces.get(0));
    }
    return faces.get(0) + " + " + faces.get(1) + " = " + roll.total();
  }
}
