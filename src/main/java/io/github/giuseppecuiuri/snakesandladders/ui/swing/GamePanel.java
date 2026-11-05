package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import io.github.giuseppecuiuri.snakesandladders.application.Simulation;
import io.github.giuseppecuiuri.snakesandladders.application.TraceFormatter;
import io.github.giuseppecuiuri.snakesandladders.domain.Game;
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
import io.github.giuseppecuiuri.snakesandladders.domain.Player;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import io.github.giuseppecuiuri.snakesandladders.ui.swing.BoardRenderer.Token;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * The playing area: the animated board, the dice, the players, the text trace and the controls for
 * stepping through a game or playing it automatically. All of it runs on the Swing event thread: a
 * turn is computed instantly and then shown move by move.
 */
final class GamePanel extends JPanel {

  private static final int TICK_MILLIS = 16;

  private final BoardView board = new BoardView();
  private final DiceView dice = new DiceView();
  private final JLabel status = new JLabel(" ");
  private final JPanel playerList = new JPanel(new GridLayout(0, 1, 0, 4));
  private final JTextArea log = new JTextArea();
  private final JButton stepButton = new JButton("Step");
  private final JButton playButton = new JButton("Play");
  private final JSlider speed = new JSlider(1, 10, 5);
  private final Deque<Token> moves = new ArrayDeque<>();
  private final Timer animation = new Timer(TICK_MILLIS, event -> animate());
  private final Timer autoPlay = new Timer(150, event -> autoStep());

  private Theme theme = Theme.LIGHT;
  private Setup setup;
  private Game game;
  private List<Token> tokens = new ArrayList<>();
  private Token moving;
  private long movingSince;
  private long manualTime = -1;
  private String banner;

  GamePanel(Setup setup) {
    super(new BorderLayout(12, 0));
    setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    add(board, BorderLayout.CENTER);
    add(sidePanel(), BorderLayout.EAST);
    bindKeys();
    start(setup);
  }

  /** Starts a new game from the given setup, discarding the current one. */
  void start(Setup newSetup) {
    pause();
    animation.stop();
    setup = newSetup;
    game = Simulation.newGame(setup.config(), setup.players(), setup.seed());
    game.addListener(this::onEvent);
    tokens = new ArrayList<>();
    for (Player player : game.players()) {
      tokens.add(Token.at(player.id(), 0));
    }
    moves.clear();
    moving = null;
    banner = null;
    dice.show(List.of());
    log.setText(TraceFormatter.header(setup.name(), setup.players(), setup.seed()) + "\n");
    refresh();
  }

  Setup setup() {
    return setup;
  }

  void setTheme(Theme newTheme) {
    theme = newTheme;
    refresh();
  }

  /** Plays one turn, finishing the animation of the previous one first. */
  void step() {
    if (game.isOver()) {
      return;
    }
    finishAnimation();
    game.playTurn();
    if (moves.isEmpty()) {
      refresh();
    } else {
      // The player list and status follow when the tokens have arrived (see animate()).
      board.repaint();
      if (manualTime < 0) {
        animation.start();
      }
    }
  }

  void togglePlay() {
    if (autoPlay.isRunning()) {
      pause();
    } else if (!game.isOver()) {
      autoPlay.start();
      playButton.setText("Pause");
    }
  }

  void pause() {
    autoPlay.stop();
    playButton.setText("Play");
  }

  /** Plays turns without animation until someone wins; used for screenshots. */
  void fastForward(int turns) {
    for (int i = 0; i < turns && !game.isOver(); i++) {
      game.playTurn();
    }
    finishAnimation();
    refresh();
  }

  /** Renders the whole panel into an image, without needing a screen. */
  BufferedImage snapshot(int width, int height) {
    setSize(width, height);
    layoutTree(this);
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    printAll(g);
    g.dispose();
    return image;
  }

  /** Renders only the board, at its current animation state. */
  BufferedImage boardSnapshot(int width, int height) {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    board.paintBoard(g, width, height);
    g.dispose();
    return image;
  }

  /** Advances the running animation by {@code millis}, for screenshots and tests. */
  void advanceAnimation(long millis) {
    if (manualTime < 0) {
      // From now on time only moves here, so the frames do not depend on how fast the machine is.
      manualTime = System.currentTimeMillis();
      animation.stop();
    }
    manualTime += millis;
    animate();
  }

  private long now() {
    return manualTime >= 0 ? manualTime : System.currentTimeMillis();
  }

  boolean isOver() {
    return game.isOver();
  }

  boolean isAnimating() {
    return moving != null || !moves.isEmpty();
  }

  private static void layoutTree(java.awt.Container container) {
    container.doLayout();
    for (java.awt.Component child : container.getComponents()) {
      if (child instanceof java.awt.Container inner) {
        layoutTree(inner);
      }
    }
  }

  private JPanel sidePanel() {
    JPanel side = new JPanel();
    side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
    side.setPreferredSize(new Dimension(330, 600));

    status.setFont(status.getFont().deriveFont(Font.BOLD));
    status.setAlignmentX(LEFT_ALIGNMENT);
    side.add(status);
    side.add(Box.createVerticalStrut(8));

    JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    controls.setAlignmentX(LEFT_ALIGNMENT);
    stepButton.setToolTipText("Play one turn (Space)");
    stepButton.addActionListener(event -> step());
    playButton.setToolTipText("Play or pause automatically (P)");
    playButton.addActionListener(event -> togglePlay());
    // Not focusable, so Space and P always reach the panel's shortcuts instead of a button.
    stepButton.setFocusable(false);
    playButton.setFocusable(false);
    controls.add(stepButton);
    controls.add(playButton);
    controls.add(dice);
    side.add(controls);

    speed.setToolTipText("Speed of automatic play and of the moves");
    speed.setAlignmentX(LEFT_ALIGNMENT);
    JPanel speedRow = new JPanel(new BorderLayout(6, 0));
    speedRow.setAlignmentX(LEFT_ALIGNMENT);
    speedRow.add(new JLabel("Speed"), BorderLayout.WEST);
    speedRow.add(speed, BorderLayout.CENTER);
    speedRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
    side.add(Box.createVerticalStrut(6));
    side.add(speedRow);

    side.add(Box.createVerticalStrut(10));
    side.add(titled("Players", playerList));
    side.add(Box.createVerticalStrut(10));

    log.setEditable(false);
    log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    log.setLineWrap(true);
    log.setWrapStyleWord(true);
    JScrollPane trace = new JScrollPane(log);
    trace.setPreferredSize(new Dimension(320, 260));
    side.add(titled("Trace", trace));
    side.add(Box.createVerticalStrut(10));
    side.add(titled("Legend", legend()));
    return side;
  }

  private static JPanel titled(String title, JComponent content) {
    JPanel box = new JPanel(new BorderLayout());
    box.setAlignmentX(LEFT_ALIGNMENT);
    box.setBorder(BorderFactory.createTitledBorder(title));
    box.add(content, BorderLayout.CENTER);
    return box;
  }

  private JPanel legend() {
    JPanel legend = new JPanel(new GridLayout(0, 2, 8, 2));
    legend.add(new JLabel("Bench: rest 1 turn", new SpecialIcon(Special.BENCH), JLabel.LEFT));
    legend.add(new JLabel("Inn: rest 3 turns", new SpecialIcon(Special.INN), JLabel.LEFT));
    legend.add(new JLabel("Dice: roll again", new SpecialIcon(Special.DICE), JLabel.LEFT));
    legend.add(new JLabel("Spring: move again", new SpecialIcon(Special.SPRING), JLabel.LEFT));
    legend.add(new JLabel("Draw a card", new SpecialIcon(Special.DRAW_CARD), JLabel.LEFT));
    return legend;
  }

  private void bindKeys() {
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "step", this::step);
    bind(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "play", this::togglePlay);
  }

  private void bind(KeyStroke key, String name, Runnable action) {
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(key, name);
    getActionMap()
        .put(
            name,
            new AbstractAction() {
              @Override
              public void actionPerformed(ActionEvent event) {
                action.run();
              }
            });
  }

  private void onEvent(GameEvent event) {
    log.append(TraceFormatter.format(event) + "\n");
    switch (event) {
      case DiceRolled roll -> dice.show(roll.faces());
      case PlayerMoved move -> {
        int last = setup.config().size();
        if (move.bounced()) {
          moves.add(new Token(move.player(), move.from(), last, 0, false));
          moves.add(new Token(move.player(), last, move.to(), 0, false));
        } else {
          moves.add(new Token(move.player(), move.from(), move.to(), 0, false));
        }
      }
      case LadderClimbed ladder ->
          moves.add(new Token(ladder.player(), ladder.from(), ladder.to(), 0, true));
      case SnakeSlid snake ->
          moves.add(new Token(snake.player(), snake.from(), snake.to(), 0, true));
      case GameWon won -> {
        banner = "Player " + won.player() + " wins!";
        pause();
      }
      // The other events only add a line to the trace. They are listed rather than left to a
      // default branch, so that a new kind of event does not compile until it is considered here.
      case TurnStarted t -> {}
      case SpecialReached s -> {}
      case CardDrawn c -> {}
      case RestStarted r -> {}
      case RestAvoided r -> {}
      case TurnSkipped t -> {}
      case DoubleSix d -> {}
    }
  }

  private void autoStep() {
    if (game.isOver()) {
      pause();
    } else if (!isAnimating()) {
      step();
    }
  }

  private void animate() {
    if (moving == null) {
      moving = moves.poll();
      movingSince = now();
      if (moving == null) {
        animation.stop();
        refresh();
        return;
      }
    }
    double duration = durationMillis(moving);
    double progress = Math.min(1, (now() - movingSince) / duration);
    Token current =
        new Token(moving.player(), moving.from(), moving.to(), progress, moving.straight());
    tokens.set(moving.player() - 1, current);
    if (progress >= 1) {
      tokens.set(moving.player() - 1, Token.at(moving.player(), moving.to()));
      moving = null;
    }
    board.repaint();
  }

  private double durationMillis(Token move) {
    double factor = 11.0 - speed.getValue();
    return move.straight()
        ? 90 * factor
        : 22 * factor * Math.max(1, Math.abs(move.to() - move.from()));
  }

  private void finishAnimation() {
    if (moving != null) {
      tokens.set(moving.player() - 1, Token.at(moving.player(), moving.to()));
      moving = null;
    }
    for (Token move : moves) {
      tokens.set(move.player() - 1, Token.at(move.player(), move.to()));
    }
    moves.clear();
    animation.stop();
    refresh();
  }

  private void refresh() {
    Player next = game.currentPlayer();
    status.setText(
        game.isOver()
            ? "Game over after " + game.turnsPlayed() + " turns"
            : "Turn " + (game.turnsPlayed() + 1) + ": player " + next.id() + " to play");
    stepButton.setEnabled(!game.isOver());
    playButton.setEnabled(!game.isOver());
    playerList.removeAll();
    for (Player player : game.players()) {
      playerList.add(playerRow(player));
    }
    playerList.revalidate();
    log.setCaretPosition(log.getDocument().getLength());
    board.repaint();
    repaint();
  }

  private JLabel playerRow(Player player) {
    StringBuilder text = new StringBuilder("Player " + player.id() + ": ");
    text.append(player.position() == 0 ? "start" : "cell " + player.position());
    if (player.restTurns() > 0) {
      text.append(", resting ").append(player.restTurns());
    }
    if (player.noRestCards() > 0) {
      text.append(", no-rest cards ").append(player.noRestCards());
    }
    if (game.winner().filter(winner -> winner == player).isPresent()) {
      text.append(", winner");
    }
    JLabel row = new JLabel(text.toString(), new TokenIcon(player.id()), JLabel.LEFT);
    if (!game.isOver() && game.currentPlayer() == player) {
      row.setFont(row.getFont().deriveFont(Font.BOLD));
    }
    return row;
  }

  /** The board itself, scaled to the space available. */
  private final class BoardView extends JComponent {

    BoardView() {
      setPreferredSize(new Dimension(640, 680));
      setToolTipText("Snakes and ladders board");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      try {
        paintBoard(g, getWidth(), getHeight());
      } finally {
        g.dispose();
      }
    }

    void paintBoard(Graphics2D g, int width, int height) {
      g.setColor(theme.background());
      g.fillRect(0, 0, width, height);
      BoardRenderer renderer = new BoardRenderer(setup.config(), theme, width, height);
      renderer.paint(g, tokens);
      if (banner != null && !isAnimating()) {
        paintBanner(g, width, height);
      }
    }

    private void paintBanner(Graphics2D g, int width, int height) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(18, width / 18)));
      FontMetrics metrics = g.getFontMetrics();
      double boxWidth = metrics.stringWidth(banner) + 48.0;
      double boxHeight = metrics.getHeight() + 28.0;
      RoundRectangle2D box =
          new RoundRectangle2D.Double(
              (width - boxWidth) / 2, (height - boxHeight) / 2, boxWidth, boxHeight, 24, 24);
      g.setColor(new Color(0, 0, 0, 170));
      g.fill(box);
      g.setColor(Color.WHITE);
      g.drawString(
          banner, (float) (box.getX() + 24), (float) (box.getY() + 14 + metrics.getAscent()));
    }
  }

  /** The faces of the last roll. */
  private static final class DiceView extends JComponent {

    private List<Integer> faces = List.of();

    DiceView() {
      setPreferredSize(new Dimension(76, 34));
      setToolTipText("Last roll");
    }

    void show(List<Integer> newFaces) {
      faces = List.copyOf(newFaces);
      setToolTipText(newFaces.isEmpty() ? "No roll yet" : "Last roll: " + newFaces);
      repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int size = 30;
      for (int i = 0; i < faces.size(); i++) {
        paintDie(g, i * (size + 6) + 2, 2, size, faces.get(i));
      }
      g.dispose();
    }

    private static void paintDie(Graphics2D g, int x, int y, int size, int face) {
      g.setColor(Color.WHITE);
      g.fill(new RoundRectangle2D.Double(x, y, size, size, 8, 8));
      g.setColor(Color.DARK_GRAY);
      g.setStroke(new BasicStroke(1.5f));
      g.draw(new RoundRectangle2D.Double(x, y, size, size, 8, 8));
      double pip = size / 6.5;
      for (double[] spot : pips(face)) {
        g.fill(
            new Ellipse2D.Double(
                x + spot[0] * size - pip / 2, y + spot[1] * size - pip / 2, pip, pip));
      }
    }

    private static double[][] pips(int face) {
      double a = 0.27;
      double m = 0.5;
      double b = 0.73;
      return switch (face) {
        case 1 -> new double[][] {{m, m}};
        case 2 -> new double[][] {{a, a}, {b, b}};
        case 3 -> new double[][] {{a, a}, {m, m}, {b, b}};
        case 4 -> new double[][] {{a, a}, {b, a}, {a, b}, {b, b}};
        case 5 -> new double[][] {{a, a}, {b, a}, {m, m}, {a, b}, {b, b}};
        default -> new double[][] {{a, a}, {b, a}, {a, m}, {b, m}, {a, b}, {b, b}};
      };
    }
  }

  /** A player's coloured token, for the list of players. */
  private final class TokenIcon implements Icon {

    private final int player;

    TokenIcon(int player) {
      this.player = player;
    }

    @Override
    public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(theme.player(player));
      g.fill(new Ellipse2D.Double(x, y, 14, 14));
      g.dispose();
    }

    @Override
    public int getIconWidth() {
      return 14;
    }

    @Override
    public int getIconHeight() {
      return 14;
    }
  }

  /** The symbol of a special cell, for the legend. */
  private final class SpecialIcon implements Icon {

    private final Special special;

    SpecialIcon(Special special) {
      this.special = special;
    }

    @Override
    public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(theme.special(special));
      g.fill(new RoundRectangle2D.Double(x, y, 18, 18, 5, 5));
      BoardRenderer.paintIcon(g, special, new Rectangle2D.Double(x + 3, y + 3, 12, 12));
      g.dispose();
    }

    @Override
    public int getIconWidth() {
      return 18;
    }

    @Override
    public int getIconHeight() {
      return 18;
    }
  }
}
