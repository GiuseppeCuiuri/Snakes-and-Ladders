package io.github.giuseppecuiuri.snakesandladders.ui.swing;

import io.github.giuseppecuiuri.snakesandladders.application.Simulation;
import io.github.giuseppecuiuri.snakesandladders.domain.Card;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import io.github.giuseppecuiuri.snakesandladders.infrastructure.ConfigRepository;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * The "new game" form: a preset or a random board, the rules, the players and the seed. Invalid
 * choices are explained in the dialog itself; nothing is read from the console any more.
 */
final class ConfigDialog extends JDialog {

  private final ConfigRepository repository = new ConfigRepository();
  private final JRadioButton usePreset = new JRadioButton("Preset", true);
  private final JRadioButton useRandom = new JRadioButton("Random board");
  private final JComboBox<String> preset =
      new JComboBox<>(ConfigRepository.PRESETS.toArray(String[]::new));
  private final JSpinner rows = spinner(10, 2, GameConfig.MAX_SIDE);
  private final JSpinner columns = spinner(10, 2, GameConfig.MAX_SIDE);
  private final JSpinner ladders = spinner(8, 0, 100);
  private final JSpinner snakes = spinner(8, 0, 100);
  private final Map<Special, JSpinner> specials = new EnumMap<>(Special.class);
  private final JCheckBox noRestCard = new JCheckBox("No-rest card in the deck");
  private final JCheckBox oneDie = new JCheckBox("Play with a single die");
  private final JCheckBox singleDieNearEnd = new JCheckBox("Single die on the last six cells");
  private final JCheckBox doubleSixBonus = new JCheckBox("Roll again after a double six");
  private final JSpinner players = spinner(2, 1, 6);
  private final JTextField seed = new JTextField(12);
  private final JTextArea problems = new JTextArea(0, 30);
  private Setup result;

  private ConfigDialog(Window owner, Setup current) {
    super(owner, "New game", ModalityType.APPLICATION_MODAL);
    for (Special special : Special.values()) {
      specials.put(special, spinner(0, 0, 20));
    }
    ButtonGroup source = new ButtonGroup();
    source.add(usePreset);
    source.add(useRandom);
    usePreset.addActionListener(event -> updateEnabled());
    useRandom.addActionListener(event -> updateEnabled());
    players.setValue(current.players());
    seed.setToolTipText("Leave empty for a new random game; the same seed replays the same game");
    problems.setEditable(false);
    problems.setLineWrap(true);
    problems.setWrapStyleWord(true);
    problems.setForeground(new Color(0xC0392B));
    problems.setOpaque(false);

    JPanel form = new JPanel(new GridBagLayout());
    form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
    int row = 0;
    row = add(form, row, usePreset, preset);
    row = add(form, row, useRandom, null);
    row = add(form, row, new JLabel("Rows x columns"), pair(rows, columns));
    row = add(form, row, new JLabel("Ladders, snakes"), pair(ladders, snakes));
    row =
        add(
            form,
            row,
            new JLabel("Benches, inns"),
            pair(specials.get(Special.BENCH), specials.get(Special.INN)));
    row =
        add(
            form,
            row,
            new JLabel("Dice cells, springs"),
            pair(specials.get(Special.DICE), specials.get(Special.SPRING)));
    row = add(form, row, new JLabel("Draw-a-card cells"), specials.get(Special.DRAW_CARD));
    row = add(form, row, null, noRestCard);
    row = add(form, row, null, oneDie);
    row = add(form, row, null, singleDieNearEnd);
    row = add(form, row, null, doubleSixBonus);
    row = add(form, row, new JLabel("Players"), players);
    row = add(form, row, new JLabel("Seed"), seed);
    add(form, row, null, problems);

    JButton start = new JButton("Start");
    start.addActionListener(event -> submit());
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(event -> dispose());
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(cancel);
    buttons.add(start);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(form, BorderLayout.CENTER);
    getContentPane().add(buttons, BorderLayout.SOUTH);
    getRootPane().setDefaultButton(start);
    updateEnabled();
    pack();
    setLocationRelativeTo(owner);
  }

  /** Shows the dialog and returns the chosen setup, or nothing if the user cancelled. */
  static Optional<Setup> ask(Window owner, Setup current) {
    ConfigDialog dialog = new ConfigDialog(owner, current);
    dialog.setVisible(true);
    return Optional.ofNullable(dialog.result);
  }

  private void submit() {
    try {
      long chosenSeed =
          seed.getText().isBlank() ? Simulation.newSeed() : Long.parseLong(seed.getText().trim());
      result = new Setup(name(), config(chosenSeed), (Integer) players.getValue(), chosenSeed);
      dispose();
    } catch (NumberFormatException e) {
      showProblem("The seed must be a whole number.");
    } catch (IllegalArgumentException e) {
      showProblem(e.getMessage());
    }
  }

  private void showProblem(String message) {
    problems.setText(message);
    pack(); // grow so that every line of the message is visible
  }

  private String name() {
    return usePreset.isSelected() ? "preset " + preset.getSelectedItem() : "random board";
  }

  private GameConfig config(long chosenSeed) {
    if (usePreset.isSelected()) {
      return repository.preset((String) preset.getSelectedItem());
    }
    Map<Special, Integer> counts = new EnumMap<>(Special.class);
    specials.forEach((special, spinner) -> counts.put(special, (Integer) spinner.getValue()));
    GameConfig.Builder builder =
        Simulation.randomBoard(
                (Integer) rows.getValue(),
                (Integer) columns.getValue(),
                (Integer) ladders.getValue(),
                (Integer) snakes.getValue(),
                counts,
                chosenSeed)
            .dice(oneDie.isSelected() ? 1 : 2)
            .singleDieNearEnd(singleDieNearEnd.isSelected())
            .doubleSixBonus(doubleSixBonus.isSelected());
    if (counts.get(Special.DRAW_CARD) > 0) {
      builder.deck(Card.standardDeck(noRestCard.isSelected()));
    }
    return builder.build();
  }

  private void updateEnabled() {
    boolean random = useRandom.isSelected();
    preset.setEnabled(!random);
    for (JComponent field :
        new JComponent[] {
          rows, columns, ladders, snakes, noRestCard, oneDie, singleDieNearEnd, doubleSixBonus
        }) {
      field.setEnabled(random);
    }
    specials.values().forEach(spinner -> spinner.setEnabled(random));
  }

  private static JSpinner spinner(int value, int min, int max) {
    return new JSpinner(new SpinnerNumberModel(value, min, max, 1));
  }

  private static JPanel pair(JComponent first, JComponent second) {
    JPanel pair = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    pair.add(first);
    pair.add(second);
    return pair;
  }

  private static int add(JPanel form, int row, JComponent label, JComponent field) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridy = row;
    constraints.insets = new Insets(3, 0, 3, 8);
    constraints.anchor = GridBagConstraints.WEST;
    if (label != null) {
      constraints.gridx = 0;
      form.add(label, constraints);
    }
    if (field != null) {
      constraints.gridx = 1;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 1;
      form.add(field, constraints);
    }
    return row + 1;
  }
}
