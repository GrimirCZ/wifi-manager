plugins {
    application
}

dependencies {
    implementation(project(":shared:ui"))
    implementation(project(":shared:security"))

    implementation(project(":admin:application"))
    implementation(project(":admin:persistence"))
    implementation(project(":admin:web"))
    implementation(project(":admin:scheduler"))
    implementation(project(":admin:events:local"))

    implementation(project(":user:application"))
    implementation(project(":user:persistence"))
    implementation(project(":user:events:local"))

    implementation(project(":captive:application"))
    implementation(project(":captive:routeragent"))
    implementation(project(":captive:persistence"))
    implementation(project(":captive:web"))
    implementation(project(":captive:events:local"))
    implementation(project(":captive:auth:google"))
    implementation(project(":captive:auth:keycloak"))

    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.springBootStarter)
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterThymeleaf)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterFlyway)
    implementation(libs.flywayCore)
    implementation(libs.flywayDbPostgresql)

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
}

application {
    mainClass = "cz.grimir.wifimanager.app.WifiManagerAppKt"
}
