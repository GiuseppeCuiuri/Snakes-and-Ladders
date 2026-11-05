package io.github.giuseppecuiuri.snakesandladders.infrastructure;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.giuseppecuiuri.snakesandladders.domain.GameConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Saves and loads game configurations as JSON files (a Repository).
 *
 * <p>The format is versioned and read strictly: unknown, missing, null or repeated fields are
 * errors, as are text after the JSON object and anything the domain rejects. Every error message
 * says what is wrong and where.
 */
public final class ConfigRepository {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
          .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .defaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.FAIL))
          .build();

  private static final String LF = "\n";

  /** Two-space indentation and LF line ends on every platform, so saved files diff cleanly. */
  private static final ObjectWriter WRITER =
      MAPPER.writer(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", LF)));

  /** The configurations shipped inside the application, the same files as in {@code configs/}. */
  public static final List<String> PRESETS = List.of("classic-100", "compact-48", "everything-on");

  /** Reads a configuration file. */
  public GameConfig load(Path file) throws IOException {
    String json;
    try {
      json = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IOException("Cannot read " + file + ": " + reason(file, e), e);
    }
    return parse(json);
  }

  /** Writes a configuration file, replacing any file already there. */
  public void save(Path file, GameConfig config) throws IOException {
    Files.writeString(file, toJson(config), StandardCharsets.UTF_8);
  }

  /**
   * Reads a configuration from JSON text.
   *
   * @throws IllegalArgumentException if the text is not a valid configuration, with the reason
   */
  public GameConfig parse(String json) {
    try {
      JsonNode root = MAPPER.readTree(json);
      if (root == null || !root.isObject()) {
        throw invalid("the file must contain a JSON object");
      }
      JsonNode version = root.get("schemaVersion");
      if (version == null) {
        throw invalid("schemaVersion is missing");
      }
      if (!version.isInt() || version.intValue() != ConfigFileV1.VERSION) {
        throw invalid(
            "schemaVersion "
                + version
                + " is not supported; this version reads schemaVersion "
                + ConfigFileV1.VERSION);
      }
      return MAPPER.treeToValue(root, ConfigFileV1.class).toGameConfig();
    } catch (JsonProcessingException e) {
      throw invalid(location(e) + describe(e));
    }
  }

  /**
   * Reads one of the {@link #PRESETS}.
   *
   * @throws IllegalArgumentException if there is no preset with that name
   */
  public GameConfig preset(String name) {
    if (!PRESETS.contains(name)) {
      throw new IllegalArgumentException(
          "There is no preset called \"" + name + "\"; the presets are " + PRESETS);
    }
    try (InputStream in =
        ConfigRepository.class.getResourceAsStream("/configs/" + name + ".json")) {
      if (in == null) {
        throw new IllegalStateException("The preset " + name + " is missing from the build");
      }
      return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Writes a configuration as JSON text, in the current file format. */
  public String toJson(GameConfig config) {
    try {
      return WRITER.writeValueAsString(ConfigFileV1.of(config)) + LF;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("A configuration could not be written as JSON", e);
    }
  }

  private static IllegalArgumentException invalid(String reason) {
    return new IllegalArgumentException("Invalid configuration file: " + reason);
  }

  /** The JDK's own message is often just the path, which says nothing to the user. */
  private static String reason(Path file, IOException e) {
    if (Files.isDirectory(file)) {
      return "it is a folder, not a file";
    }
    if (e instanceof NoSuchFileException) {
      return "there is no such file";
    }
    if (e instanceof AccessDeniedException) {
      return "access denied";
    }
    if (e instanceof CharacterCodingException) {
      return "it is not UTF-8 text";
    }
    return e.getMessage();
  }

  private static String location(JsonProcessingException e) {
    if (!(e instanceof JsonMappingException mapping) || mapping.getPath().isEmpty()) {
      return "";
    }
    String path =
        mapping.getPath().stream()
            .map(
                reference ->
                    reference.getFieldName() != null
                        ? "." + reference.getFieldName()
                        : "[" + reference.getIndex() + "]")
            .collect(Collectors.joining())
            .substring(1);
    return "at " + path + ", ";
  }

  private static String describe(JsonProcessingException e) {
    if (e instanceof UnrecognizedPropertyException unknown) {
      return "unknown field \"" + unknown.getPropertyName() + "\"";
    }
    if (e instanceof InvalidFormatException format
        && format.getTargetType() != null
        && format.getTargetType().isEnum()) {
      return "\""
          + format.getValue()
          + "\" is not one of "
          + Arrays.toString(format.getTargetType().getEnumConstants());
    }
    String message = e.getOriginalMessage();
    if (message.startsWith("Missing creator property")) {
      return "the field is missing";
    }
    if (message.startsWith("Trailing token")) {
      return "there is more text after the JSON object";
    }
    if (!(e instanceof JsonMappingException) && e.getLocation() != null) {
      // A syntax error: point at the place in the text rather than at a field.
      return "line "
          + e.getLocation().getLineNr()
          + ", column "
          + e.getLocation().getColumnNr()
          + ": "
          + message.replaceAll(" \\(start marker at .*", "");
    }
    // Drop Jackson's hints about its own settings, which mean nothing to someone editing the file.
    return message
        .replaceAll("; `DeserializationFeature.*| \\(but could if coercion.*", "")
        .replace('`', '"');
  }
}
