import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellij)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    maven("https://repo.denwav.dev/repository/maven-public/")
    mavenCentral()
}

intellij {
    version.set(libs.versions.idea)

    plugins.addAll("java", "gradle", "org.jetbrains.kotlin")

    updateSinceUntilBuild.set(false)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}

tasks.runPluginVerifier {
    ideVersions.addAll("IC-2021.1", "IC-2021.2", "IC-2021.3", "IC-2022.1")
}
