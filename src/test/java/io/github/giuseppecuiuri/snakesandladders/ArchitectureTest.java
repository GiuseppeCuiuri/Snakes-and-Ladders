package io.github.giuseppecuiuri.snakesandladders;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/** The architecture rules of ADR-0002, checked on the production classes. */
@AnalyzeClasses(
    packages = "io.github.giuseppecuiuri.snakesandladders",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule layersOnlyDependDownwards =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .withOptionalLayers(true)
          .layer("Domain")
          .definedBy("..domain..")
          .layer("Application")
          .definedBy("..application..")
          .layer("Infrastructure")
          .definedBy("..infrastructure..")
          .layer("UI")
          .definedBy("..ui..")
          .whereLayer("UI")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Application")
          .mayOnlyBeAccessedByLayers("UI")
          .whereLayer("Infrastructure")
          .mayOnlyBeAccessedByLayers("UI")
          .whereLayer("Domain")
          .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "UI");

  @ArchTest
  static final ArchRule theDomainOnlyUsesTheCoreJdk =
      classes()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage("..domain..", "java.lang..", "java.util..");

  @ArchTest
  static final ArchRule onlyTheCommandLineWritesToTheConsole =
      noClasses()
          .that()
          .resideOutsideOfPackage("..ui.cli..")
          .should()
          .accessField(System.class, "out")
          .orShould()
          .accessField(System.class, "err")
          .orShould()
          .callMethod(Throwable.class, "printStackTrace");

  @ArchTest
  static final ArchRule thereIsNoGlobalMutableState =
      fields().that().areStatic().should().beFinal();

  @ArchTest
  static final ArchRule theCommandLineNeedsNoGraphics =
      noClasses()
          .that()
          .resideInAPackage("..ui.cli..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("java.awt..", "javax.swing..", "..ui.swing..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule onlyTheApplicationLayerCreatesRandomGenerators =
      noClasses()
          .that()
          .resideOutsideOfPackage("..application..")
          .should()
          .dependOnClassesThat()
          .belongToAnyOf(
              Random.class,
              SecureRandom.class,
              SplittableRandom.class,
              ThreadLocalRandom.class,
              RandomGeneratorFactory.class)
          .orShould()
          .callMethod(RandomGenerator.class, "getDefault")
          .orShould()
          .callMethod(RandomGenerator.class, "of", String.class)
          .orShould()
          .callMethod(Collections.class, "shuffle", List.class)
          .orShould()
          .callMethod(Math.class, "random");
}
