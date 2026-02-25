plugins {
    java
    `java-library`
    `maven-publish`
}

group = property("group") as String
version = property("version") as String

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    // SessionCast Core — via JitPack
    api("com.github.sessioncast.sessioncast-java:sessioncast-core:${property("sessioncastJavaVersion")}")

    // Spring AI — compileOnly so users manage their own Spring AI BOM
    compileOnly("org.springframework.ai:spring-ai-model:${property("springAiVersion")}")

    // Spring Boot
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:${property("springBootVersion")}")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${property("springBootVersion")}")

    // Test
    testImplementation("org.springframework.ai:spring-ai-model:${property("springAiVersion")}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${property("springBootVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("SessionCast Spring AI")
                description.set("Spring AI ChatModel integration for SessionCast (BYOAI via CLI agent)")
                url.set("https://github.com/sessioncast/sessioncast-spring-ai")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("devload")
                        name.set("devload")
                        email.set("devload@sessioncast.io")
                    }
                }
            }
        }
    }
}
