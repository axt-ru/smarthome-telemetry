plugins {
    id("java-library")
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
    api("com.fasterxml.jackson.core:jackson-annotations")
}
