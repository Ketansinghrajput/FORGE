package com.forge.platform.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.forge", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_not_access_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..")
                    .because("Controllers must go through the service layer");

    @ArchTest
    static final ArchRule engine_should_not_depend_on_spring =
            noClasses().that().resideInAPackage("com.forge.engine..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("The core engine must remain framework-agnostic");

    @ArchTest
    static final ArchRule services_should_not_access_controllers =
            noClasses().that().resideInAPackage("..service..")
                    .should().accessClassesThat().resideInAPackage("..controller..")
                    .because("Services should not depend on web layer routing");

    @ArchTest
    static final ArchRule entities_should_not_depend_on_dtos =
            noClasses().that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..dto..")
                    .because("Entities should not know about data transfer objects");

    @ArchTest
    static final ArchRule no_cycles =
            slices().matching("com.forge.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule strict_layering = layeredArchitecture()
            .consideringAllDependencies()
            // 1. Define Standard Layers
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .layer("Entity").definedBy("..entity..")

            // 🔥 SENSEI FIX: Define Cross-Cutting Layers for Spring Boot
            .layer("Config").definedBy("..config..")         // Security & Setup
            .layer("Scheduler").definedBy("..scheduler..")   // Quartz/Cron Jobs
            .layer("Bridge").definedBy("..bridge..")         // Engine-to-DB event sync
            .layer("Main").definedBy("com.forge.platform")   // ForgePlatformApplication (root)

            // 2. Define Access Rules
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()

            // Services can be used by web (Controller), background jobs (Scheduler), and setups (Config/Main)
            .whereLayer("Service").mayOnlyBeAccessedByLayers(
                    "Controller", "Scheduler", "Config", "Bridge", "Main"
            )

            // Repositories are used by Service, but also allowed for direct DB setups/syncs
            .whereLayer("Repository").mayOnlyBeAccessedByLayers(
                    "Service", "Bridge", "Config", "Main"
            )
            .because("Strict layering must be enforced, but Config, Schedulers, and Bridges are allowed as cross-cutting concerns");
}