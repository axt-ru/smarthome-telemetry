plugins {
    id("java-library")
}

dependencies {
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}"))
    compileOnly("org.springframework.boot:spring-boot-starter-amqp")
}
