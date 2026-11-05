package io.github.giuseppecuiuri.snakesandladders.domain;

/** A player's token and what the rules remember about it. Only the game changes it. */
public final class Player {

  private final int id;
  private int position;
  private int restTurns;
  private int noRestCards;

  Player(int id) {
    this.id = id;
  }

  /** 1 for the first player, 2 for the second, and so on. */
  public int id() {
    return id;
  }

  /** 0 before the first move, then the cell the token is on, from 1 to N. */
  public int position() {
    return position;
  }

  /** How many of their next turns the player still has to skip. */
  public int restTurns() {
    return restTurns;
  }

  /** How many no-rest cards the player has kept. */
  public int noRestCards() {
    return noRestCards;
  }

  void moveTo(int cell) {
    position = cell;
  }

  void setRestTurns(int turns) {
    restTurns = turns;
  }

  void keepNoRestCard() {
    noRestCards++;
  }

  void useNoRestCard() {
    noRestCards--;
  }
}
