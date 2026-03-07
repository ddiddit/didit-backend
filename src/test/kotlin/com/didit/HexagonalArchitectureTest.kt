package com.didit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.library.Architectures

@AnalyzeClasses(
    packages = ["com.didit"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class HexagonalArchitectureTest {
    @ArchTest
    fun hexagonalArchitecture(classes: JavaClasses) {
        Architectures
            .layeredArchitecture()
            .consideringAllDependencies()
            .layer("domain")
            .definedBy("com.didit.domain..")
            .layer("application")
            .definedBy("com.didit.application..")
            .layer("adapter")
            .definedBy("com.didit.adapter..")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("application", "adapter")
            .whereLayer("application")
            .mayOnlyBeAccessedByLayers("adapter")
            .whereLayer("adapter")
            .mayNotBeAccessedByAnyLayer()
            .check(classes)
    }
}
