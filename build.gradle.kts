plugins {
    id("java")
}

group = "net.dv8tion.pokedex"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Note: Remove "-rc.1" when 6.0.0 is released
    implementation("net.dv8tion:JDA:6.0.0-rc.1")
}

tasks.test {
    useJUnitPlatform()
}
