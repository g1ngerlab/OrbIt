plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

application {
    mainClass.set("server.ServerLauncher")
}

dependencies {
    // Same kryonet fork as core module to ensure protocol compatibility
    implementation("com.github.crykn:kryonet:2.22.9")

    // Core module for shared classes
    implementation(project(":core"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    main {
        java.srcDirs("src/main/java")
    }
}

tasks.test {
    useJUnitPlatform()
}
