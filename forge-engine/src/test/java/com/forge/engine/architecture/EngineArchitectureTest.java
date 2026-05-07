package com.forge.engine.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.forge.engine",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class EngineArchitectureTest {

    // Most important rule: engine must have zero Spring dependencies
    @ArchTest
    static final ArchRule engine_should_not_depend_on_spring =
            noClasses()
                    .that().resideInAPackage("com.forge.engine..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("forge-engine is pure Java — no Spring allowed");

    // Engine must not depend on platform layer
    @ArchTest
    static final ArchRule engine_should_not_depend_on_platform =
            noClasses()
                    .that().resideInAPackage("com.forge.engine..")
                    .should().dependOnClassesThat().resideInAPackage("com.forge.platform..")
                    .because("Engine must be framework-agnostic");
}