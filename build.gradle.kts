plugins {
    id("java")
    id("org.springframework.boot") version "4.0.7" apply false
}

val springBootVersion: String by project

allprojects {
    group = "ru.otus.smarthome"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.assertj:assertj-core")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<JavaExec> {
        systemProperty("stdout.encoding", "UTF-8")
        systemProperty("stderr.encoding", "UTF-8")
    }
}
