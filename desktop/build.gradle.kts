import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "org.tameter.impactestimation"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(21)
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
