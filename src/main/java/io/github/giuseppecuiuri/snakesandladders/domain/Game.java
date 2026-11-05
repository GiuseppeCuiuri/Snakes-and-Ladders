package io.github.giuseppecuiuri.snakesandladders.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

/**
 * One game: a configuration, its players and whose turn it is.
 *
 * <p>Call {@link #playTurn()} until {@link #isOver()}. Everything that happens is reported to the
 * listeners as {@link GameEvent}s; the game itself never prints or draws. All randomness comes from
 * the generator passed in, so a seeded generator replays the same game.
 */
public final class Game {

  /** Fewest players in a game. */
  public static final int MIN_PLAYERS = 1;

  /** Most players in a game. */
  public static final int MAX_PLAYERS = 6;

  private final GameConfig config;
  private final RandomGenerator random;
  private final List<Player> players;
  private final Map<Integer, Jump> jumps = new HashMap<>();
  private final Deque<Card> deck;
  private final List<Consumer<? super GameEvent>> listeners = new ArrayList<>();
  private int current;
  private int turn;
  private Player winner;

  /**
   * Sets up a game with every player off the board, before the first cell. The deck, if any, is
   * shuffled once with {@code random}.
   *
   * @param config the board and its rules
   * @param players how many players, from {@value #MIN_PLAYERS} to {@value #MAX_PLAYERS}
   * @param random the source of every dice roll
   */
  public Game(GameConfig config, int players, RandomGenerator random) {
    if (players < MIN_PLAYERS || players > MAX_PLAYERS) {
      throw new IllegalArgumentException(
          "players must be between " + MIN_PLAYERS + " and " + MAX_PLAYERS + ", not " + players);
    }
    this.config = Objects.requireNonNull(config, "config");
    this.random = Objects.requireNonNull(random, "random");
    this.players = List.copyOf(IntStream.rangeClosed(1, players).mapToObj(Player::new).toList());
    for (Jump jump : config.ladders()) {
      jumps.put(jump.from(), jump);
    }
    for (Jump jump : config.snakes()) {
      jumps.put(jump.from(), jump);
    }
    List<Card> cards = new ArrayList<>(config.deck());
    Collections.shuffle(cards, random);
    this.deck = new ArrayDeque<>(cards);
  }

  /** Registers a listener that receives every event from now on. */
  public void addListener(Consumer<? super GameEvent> listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }

  /**
   * Plays the turn of the current player, then passes the turn to the next player unless the game
   * is over.
   *
   * @throws IllegalStateException if the game is already over
   */
  public void playTurn() {
    if (isOver()) {
      throw new IllegalStateException("The game is over");
    }
    Player player = players.get(current);
    turn++;
    emit(new GameEvent.TurnStarted(turn, player.id()));
    if (player.restTurns() > 0) {
      player.setRestTurns(player.restTurns() - 1);
      emit(new GameEvent.TurnSkipped(player.id(), player.restTurns()));
    } else {
      new Turn(this, player).rollAndMove();
    }
    if (!isOver()) {
      current = (current + 1) % players.size();
    }
  }

  /** True once a player has reached the last cell. */
  public boolean isOver() {
    return winner != null;
  }

  /** The player who reached the last cell, if any. */
  public Optional<Player> winner() {
    return Optional.ofNullable(winner);
  }

  /** The players, in turn order. */
  public List<Player> players() {
    return players;
  }

  /** The player whose turn comes next, or the winner once the game is over. */
  public Player currentPlayer() {
    return players.get(current);
  }

  /** How many turns have been played. */
  public int turnsPlayed() {
    return turn;
  }

  /** The board and its rules. */
  public GameConfig config() {
    return config;
  }

  int rollDie() {
    return random.nextInt(1, 7);
  }

  Card drawCard() {
    return deck.removeFirst();
  }

  void putAtTheBottom(Card card) {
    deck.addLast(card);
  }

  Jump jumpAt(int cell) {
    return jumps.get(cell);
  }

  void win(Player player) {
    winner = player;
    emit(new GameEvent.GameWon(player.id(), turn));
  }

  void emit(GameEvent event) {
    for (Consumer<? super GameEvent> listener : listeners) {
      listener.accept(event);
    }
  }
}
