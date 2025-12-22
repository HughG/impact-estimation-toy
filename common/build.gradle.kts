plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

group = "org.tameter"
version = "1.0-SNAPSHOT"

kotlin {
    // Configure JVM toolchain for all JVM targets (Kotlin 2.0+ requirement)
    jvmToolchain(21)
    // Android temporarily disabled; only desktop JVM target is active
    jvm("desktop") {
    }
    @Suppress("unused")
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                // SharedFlow for Stage 2 observability
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                // Stage 3 Storage: kotlinx.serialization for JSON in common
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // Coroutine test utilities for deterministic Flow testing in commonTest
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting
    }
}

// Android Gradle configuration removed while Android is disabled