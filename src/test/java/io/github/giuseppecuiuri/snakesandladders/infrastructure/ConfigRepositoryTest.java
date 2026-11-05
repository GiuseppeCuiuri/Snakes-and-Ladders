package io.github.giuseppecuiuri.snakesandladders.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.giuseppecuiuri.snakesandladders.domain.Card;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import io.github.giuseppecuiuri.snakesandladders.domain.Jump;
import io.github.giuseppecuiuri.snakesandladders.domain.Special;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigRepositoryTest {

  private static final GameConfig EVERYTHING =
      GameConfig.builder(10, 10)
          .singleDieNearEnd(true)
          .doubleSixBonus(true)
          .ladder(9, 31)
          .snake(17, 7)
          .special(20, Special.INN)
          .special(45, Special.DRAW_CARD)
          .deck(Card.standardDeck(true))
          .build();

  private final ConfigRepository repository = new ConfigRepository();

  @Test
  void writesEveryFieldInTheVersionedFormat() {
    assertThat(repository.toJson(EVERYTHING))
        .isEqualTo(
            """
            {
              "schemaVersion" : 1,
              "rows" : 10,
              "columns" : 10,
              "dice" : 2,
              "singleDieNearEnd" : true,
              "doubleSixBonus" : true,
              "ladders" : [ {
                "from" : 9,
                "to" : 31
              } ],
              "snakes" : [ {
                "from" : 17,
                "to" : 7
              } ],
              "specials" : [ {
                "cell" : 20,
                "type" : "INN"
              }, {
                "cell" : 45,
                "type" : "DRAW_CARD"
              } ],
              "deck" : [ "BENCH", "INN", "DICE", "SPRING", "NO_REST" ]
            }
            """);
  }

  @Test
  void savedFilesLoadBackUnchanged(@TempDir Path folder) throws Exception {
    Path file = folder.resolve("everything.json");

    repository.save(file, EVERYTHING);
    assertThat(repository.load(file)).isEqualTo(EVERYTHING);
  }

  @Test
  void ladderAndSnakePositionsAreSavedNotJustTheirNumber() {
    GameConfig loaded = repository.parse(repository.toJson(EVERYTHING));

    assertThat(loaded.ladders()).isEqualTo(EVERYTHING.ladders());
    assertThat(loaded.snakes()).isEqualTo(EVERYTHING.snakes());
    assertThat(loaded.specials()).isEqualTo(EVERYTHING.specials());
  }

  @Test
  void theSchemaVersionComesFirst() {
    assertInvalid("[]", "Invalid configuration file: the file must contain a JSON object");
    assertInvalid("{\"rows\": 10}", "Invalid configuration file: schemaVersion is missing");
    assertInvalid(
        json().replace("\"schemaVersion\" : 1", "\"schemaVersion\" : 2"),
        "Invalid configuration file: schemaVersion 2 is not supported;"
            + " this version reads schemaVersion 1");
  }

  @Test
  void unknownMissingAndNullFieldsAreErrors() {
    assertInvalid(
        json().replace("\"rows\" : 10,", "\"rows\" : 10, \"extra\" : 1,"),
        "Invalid configuration file: at extra, unknown field \"extra\"");
    assertInvalid(
        json().replace("  \"dice\" : 2,\n", ""),
        "Invalid configuration file: at dice, the field is missing");
    assertInvalid(
        json().replace("\"ladders\" : [ {", "\"ladders\" : null, \"unused\" : [ {"),
        "Invalid configuration file: at ladders, ");
    assertInvalid(
        json().replace("\"deck\" : [ \"BENCH\"", "\"deck\" : [ null"),
        "Invalid configuration file: at deck[0], ");
  }

  @Test
  void valuesMustHaveTheRightType() {
    assertInvalid(
        json().replace("\"type\" : \"INN\"", "\"type\" : \"HOTEL\""),
        "Invalid configuration file: at specials[0].type, \"HOTEL\" is not one of"
            + " [BENCH, INN, DICE, SPRING, DRAW_CARD]");
    assertInvalid(
        json().replace("\"rows\" : 10", "\"rows\" : 10.5"),
        "Invalid configuration file: at rows, Cannot coerce Floating-point value (10.5) to \"int\""
            + " value");
    assertInvalid(
        json().replace("\"rows\" : 10", "\"rows\" : null"),
        "Invalid configuration file: at rows, ");
    assertInvalid(
        "{ \"schemaVersion\" : 1,\n  \"rows\" : ", "Invalid configuration file: line 2, column ");
  }

  @Test
  void theDomainRulesStillApply() {
    assertInvalid(
        json().replace("\"from\" : 9", "\"from\" : 50"),
        "Invalid configuration:\n- ladder 50 -> 31 must go up");
    assertInvalid(
        json().replace("\"cell\" : 45", "\"cell\" : 20"),
        "Invalid configuration:\n- cell 20 has more than one special cell");
  }

  @Test
  void thePresetsAreTheFilesInConfigsAndLoadBackToTheSameText() throws Exception {
    for (String name : ConfigRepository.PRESETS) {
      Path file = Path.of("configs", name + ".json");
      GameConfig preset = repository.preset(name);

      assertThat(repository.load(file)).isEqualTo(preset);
      assertThat(repository.toJson(preset)).isEqualTo(Files.readString(file));
    }
  }

  @Test
  void eachPresetShowsWhatItIsNamedFor() {
    GameConfig classic = repository.preset("classic-100");
    assertThat(classic.size()).isEqualTo(100);
    assertThat(classic.ladders()).contains(new Jump(9, 31));
    assertThat(classic.snakes()).contains(new Jump(17, 7), new Jump(98, 79));

    GameConfig compact = repository.preset("compact-48"); // 8 rows of 6
    assertThat(compact.rows()).isEqualTo(8);
    assertThat(compact.columns()).isEqualTo(6);

    GameConfig everything = repository.preset("everything-on");
    assertThat(everything.singleDieNearEnd()).isTrue();
    assertThat(everything.doubleSixBonus()).isTrue();
    assertThat(everything.specials().values()).containsAll(List.of(Special.values()));
    assertThat(everything.deck()).contains(Card.NO_REST);
  }

  @Test
  void unknownPresetsAreRejected() {
    assertThatThrownBy(() -> repository.preset("giant-400"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "There is no preset called \"giant-400\"; the presets are"
                + " [classic-100, compact-48, everything-on]");
  }

  @Test
  void whatPlainJsonWouldAllowIsStillAnError() {
    assertInvalid(
        json().replace("\"rows\" : 10,", "\"rows\" : 10, \"rows\" : 3,"),
        "Invalid configuration file: line 3, column 22: Duplicate field 'rows'");
    assertInvalid(
        json() + "{}", "Invalid configuration file: there is more text after the JSON object");
    assertInvalid(
        json().replace("\"schemaVersion\" : 1", "\"schemaVersion\" : \"1\""),
        "Invalid configuration file: schemaVersion \"1\" is not supported");
  }

  @Test
  void filesThatCannotBeReadSayWhy(@TempDir Path folder) throws Exception {
    Path latin1 = Files.write(folder.resolve("latin1.json"), new byte[] {'{', (byte) 0xE8, '}'});

    assertThatThrownBy(() -> repository.load(folder.resolve("missing.json")))
        .hasMessage("Cannot read " + folder.resolve("missing.json") + ": there is no such file");
    assertThatThrownBy(() -> repository.load(folder))
        .hasMessage("Cannot read " + folder + ": it is a folder, not a file");
    assertThatThrownBy(() -> repository.load(latin1))
        .hasMessage("Cannot read " + latin1 + ": it is not UTF-8 text");
  }

  private String json() {
    return repository.toJson(EVERYTHING);
  }

  private void assertInvalid(String json, String message) {
    assertThatThrownBy(() -> repository.parse(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith(message);
  }
}
