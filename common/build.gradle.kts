plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
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