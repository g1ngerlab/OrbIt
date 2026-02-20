plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

application {
    mainClass.set("server.ServerLauncher")
}

dependencies {
    // Kryonet for networking
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")
    // implementation("com.esotericsoftware:kryo:5.4.0") // Removed to avoid version conflict

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
