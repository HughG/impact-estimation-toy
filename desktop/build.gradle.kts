import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "org.tameter.impactestimation"
version = "1.0-SNAPSHOT"


kotlin {
    // Configure JVM toolchain at the extension level (Kotlin 2.0+)
    jvmToolchain(21)
    jvm {
        withJava()
    }
    @Suppress("unused")
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi /*, TargetFormat.Dmg, TargetFormat.Deb*/)
            packageName = "impact-estimation-toy"
            packageVersion = "1.0.0"
        }
    }
}
