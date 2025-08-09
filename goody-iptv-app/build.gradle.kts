// Top-level Gradle configuration
plugins {
    // Versions managed in module
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
} 