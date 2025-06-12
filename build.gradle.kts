plugins {
    id("java")
}

group = "net.dv8tion.pokedex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // We are getting the pre-release version from jitpack.
    maven("https://jitpack.io")
}

dependencies {
    // We are using a pre-release version of JDA as components haven't merged
    // into main-line JDA yet. PR: https://github.com/discord-jda/JDA/pull/2809
    implementation("io.github.freya022:JDA:c6f764778d")

    // Once the above PR is merged and JDA does a release, you can replace the above implementation(...) with:
    // implementation("net.dv8tion:JDA:$VERSION") where $VERSION is the new release
}

tasks.test {
    useJUnitPlatform()
}
