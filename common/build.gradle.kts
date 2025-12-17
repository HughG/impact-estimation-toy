plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "org.tameter"
version = "1.0-SNAPSHOT"

kotlin {
    // Android temporarily disabled; only desktop JVM target is active
    jvm("desktop") {
        jvmToolchain(21)
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