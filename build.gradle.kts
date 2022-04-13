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

    plugins.addAll("java", "gradle")
    plugins.addProvider(libs.versions.kotlinPlugin.map { "org.jetbrains.kotlin:$it" })

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

// `intellij.plugins` is defined as `ListProperty<Any>` which causes an overload ambiguity error when trying to pass
// something to the `Provider` version of `add`, so this function helps get around that issue.
fun <T> ListProperty<T>.addProvider(provider: Provider<out T>) = add(provider)
