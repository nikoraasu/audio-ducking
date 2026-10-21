import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "io.github.nikoraasu.audioducking.shared"
        compileSdk = 36
        minSdk = 30
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopMain by getting {
            resources.srcDir(rootProject.file("models"))
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("com.k2fsa.sherpa.onnx:sherpa-onnx-jvm:1.13.8")
                // shortcut: x64 only; add the linux-aarch64 / win-arm64 jars if ARM desktops matter.
                runtimeOnly("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-linux-x64:1.13.8")
                runtimeOnly("com.k2fsa.sherpa.onnx:sherpa-onnx-native-lib-win-x64:1.13.8")
                implementation("net.java.dev.jna:jna-platform:5.19.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.nikoraasu.audioducking.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Audio Ducking"
            packageVersion = "1.0.0"
            linux { iconFile.set(project.file("icons/duck.png")) }
            windows { iconFile.set(project.file("icons/duck.ico")) }
        }
    }
}

// Windows bundle built on Linux (packaging/build-windows.sh): compose.desktop can only package for the host OS,
// so collect the Windows runtime classpath here and let jpackage (run under wine) wrap it.
val windowsRuntime by configurations.creating {
    isCanBeConsumed = false
    attributes {
        attribute(org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE, objects.named(org.gradle.api.attributes.Usage.JAVA_RUNTIME))
        attribute(org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM))
        attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
    }
    exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-linux-x64")
    exclude(group = "com.k2fsa.sherpa.onnx", module = "sherpa-onnx-native-lib-linux-x64")
}
dependencies {
    windowsRuntime(project(":shared"))
    windowsRuntime(compose.desktop.windows_x64)
}
tasks.register<Sync>("collectWindowsApp") {
    from(windowsRuntime)
    from(tasks.named("desktopJar"))
    into(layout.buildDirectory.dir("windows/app"))
}
