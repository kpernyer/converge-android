// Top-level build file for Converge Android
// Quality-first: Detekt + Spotless for code hygiene

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.protobuf") version "0.9.4" apply false

    // Code quality
    id("io.gitlab.arturbosch.detekt") version "1.23.5"
    id("com.diffplug.spotless") version "6.25.0"
}

// Detekt - static analysis (richer than ktlint)
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    parallel = true
}

// Spotless - format enforcement (kills bikeshedding)
// Using ktlint with Android-compatible rules
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.1.1")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    // Disabled rules that conflict with Android/Compose conventions:
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_value-argument-comment" to "disabled",
                    "ktlint_standard_value-parameter-comment" to "disabled",
                    "ktlint_standard_function-naming" to "disabled", // Compose uses PascalCase
                    "ktlint_standard_property-naming" to "disabled", // _backing pattern is idiomatic
                ),
            )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint("1.1.1")
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_value-argument-comment" to "disabled",
                ),
            )
    }
}

tasks.register("checkQuality") {
    group = "verification"
    description = "Run all quality checks"
    dependsOn("detekt", "spotlessCheck")
}
