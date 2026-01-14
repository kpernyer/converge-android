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
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.1.1")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "max_line_length" to "120",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                )
            )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint("1.1.1")
    }
}

tasks.register("checkQuality") {
    group = "verification"
    description = "Run all quality checks"
    dependsOn("detekt", "spotlessCheck")
}
