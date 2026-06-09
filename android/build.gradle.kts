// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  id("com.diffplug.spotless") version "6.25.0"
}

tasks.register<Exec>("buildReleaseApk") {
    group = "build"
    description = "Runs the build.sh script to compile and sign the release APK."
    workingDir = project.rootDir
    commandLine = listOf("./build.sh")
}