import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":sharedUI"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        //javaHome = "C:\\Program Files\\Zulu\\zulu-21"

        nativeDistributions {
            appResourcesRootDir.set(rootProject.layout.projectDirectory.dir("appResources"))
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "vRain"
            packageVersion = "1.0.1"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "icu.hearme.vrain.desktopApp"
            }
        }
        buildTypes.release {
            proguard {
                version.set("7.8.0")
                isEnabled.set(false)
            }
        }
    }
}