package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import io.github.giuseppecuiuri.snakesandladders.application.Simulation;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/** The application window: the game panel, the menus and the file dialogs. */
public final class MainWindow extends JFrame {

  private static final int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

  private final ConfigRepository repository = new ConfigRepository();
  private final GamePanel panel;
  private final JFileChooser chooser = new JFileChooser();

  private MainWindow(Setup setup) {
    super("Snakes and Ladders");
    panel = new GamePanel(setup);
    setContentPane(panel);
    setJMenuBar(menus());
    chooser.setFileFilter(new FileNameExtensionFilter("Game configurations (*.json)", "json"));
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    pack();
    setMinimumSize(new Dimension(760, 560));
    setLocationRelativeTo(null);
  }

  /** Opens the window on the classic board with two players. */
  public static void open() {
    SwingUtilities.invokeLater(
        () -> {
          FlatLightLaf.setup();
          GameConfig classic = new ConfigRepository().preset("classic-100");
          new MainWindow(new Setup("preset classic-100", classic, 2, Simulation.newSeed()))
              .setVisible(true);
        });
  }

  private JMenuBar menus() {
    JMenu game = new JMenu("Game");
    game.setMnemonic(KeyEvent.VK_G);
    game.add(item("New game...", KeyEvent.VK_N, this::newGame));
    game.add(item("Restart", KeyEvent.VK_R, () -> panel.start(panel.setup())));
    game.addSeparator();
    game.add(item("Open configuration...", KeyEvent.VK_O, this::openConfiguration));
    game.add(item("Save configuration...", KeyEvent.VK_S, this::saveConfiguration));
    game.addSeparator();
    // Like the window's close button, so the program ends even during automatic play.
    game.add(
        item(
            "Quit",
            KeyEvent.VK_Q,
            () -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING))));

    JMenu view = new JMenu("View");
    view.setMnemonic(KeyEvent.VK_V);
    JCheckBoxMenuItem dark = new JCheckBoxMenuItem("Dark theme");
    dark.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT));
    dark.addActionListener(event -> setDark(dark.isSelected()));
    view.add(dark);

    JMenu help = new JMenu("Help");
    help.setMnemonic(KeyEvent.VK_H);
    help.add(item("How to play", 0, this::showRules));

    JMenuBar bar = new JMenuBar();
    bar.add(game);
    bar.add(view);
    bar.add(help);
    return bar;
  }

  private static JMenuItem item(String name, int key, Runnable action) {
    JMenuItem item =
        new JMenuItem(
            new AbstractAction(name) {
              @Override
              public void actionPerformed(ActionEvent event) {
                action.run();
              }
            });
    if (key != 0) {
      item.setAccelerator(KeyStroke.getKeyStroke(key, SHORTCUT));
    }
    return item;
  }

  private void newGame() {
    panel.pause();
    ConfigDialog.ask(this, panel.setup()).ifPresent(panel::start);
  }

  private void openConfiguration() {
    panel.pause();
    if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = chooser.getSelectedFile();
    try {
      GameConfig config = repository.load(file.toPath());
      panel.start(new Setup(file.getName(), config, panel.setup().players(), Simulation.newSeed()));
    } catch (IOException | IllegalArgumentException e) {
      JOptionPane.showMessageDialog(
          this, e.getMessage(), "Cannot open " + file.getName(), JOptionPane.ERROR_MESSAGE);
    }
  }

  private void saveConfiguration() {
    panel.pause();
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    File file = chooser.getSelectedFile();
    if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
      file = new File(file.getParentFile(), file.getName() + ".json");
    }
    if (file.exists()
        && JOptionPane.showConfirmDialog(
                this,
                file.getName() + " already exists. Replace it?",
                "Save configuration",
                JOptionPane.YES_NO_OPTION)
            != JOptionPane.YES_OPTION) {
      return;
    }
    try {
      repository.save(file.toPath(), panel.setup().config());
    } catch (IOException e) {
      JOptionPane.showMessageDialog(
          this, e.getMessage(), "Cannot save " + file.getName(), JOptionPane.ERROR_MESSAGE);
    }
  }

  private void setDark(boolean dark) {
    if (dark) {
      FlatDarkLaf.setup();
    } else {
      FlatLightLaf.setup();
    }
    FlatLaf.updateUI();
    SwingUtilities.updateComponentTreeUI(chooser); // not on screen, so updateUI() misses it
    panel.setTheme(dark ? Theme.DARK : Theme.LIGHT);
  }

  private void showRules() {
    JOptionPane.showMessageDialog(
        this,
        """
        Players take turns rolling the dice and moving along the numbered path.
        Landing on the foot of a ladder climbs it; landing on a snake's head slides down it.
        The last cell must be reached with an exact roll: extra points bounce back.

        Bench: skip 1 turn.  Inn: skip 3 turns.
        Dice cell: roll again.  Spring: move again by the last roll.
        Draw-a-card cell: the card acts like the matching cell;
        a no-rest card is kept and cancels the next rest.

        Space plays one turn, P plays or pauses automatically.
        """,
        "How to play",
        JOptionPane.INFORMATION_MESSAGE);
  }
}
