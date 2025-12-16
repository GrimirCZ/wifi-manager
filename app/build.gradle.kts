plugins {
    application
}

dependencies {
    implementation(project(":shared:ui"))

    implementation(project(":admin:application"))
    implementation(project(":admin:persistence"))
    implementation(project(":admin:web"))
    implementation(project(":admin:events:local"))

    implementation(project(":captive:application"))
    implementation(project(":captive:persistence"))
    implementation(project(":captive:web"))
    implementation(project(":captive:events:local"))

    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}

application {
    mainClass = "cz.grimir.wifimanager.app.WifiManagerAppKt"
}
