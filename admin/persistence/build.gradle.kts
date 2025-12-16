plugins {
    kotlin("plugin.jpa")
}

group = "cz.grimir.wifimanager.admin.persistence"

dependencies {
    implementation(project(":admin:core"))
    implementation(project(":admin:application"))

    implementation(project(":shared"))

    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation("io.hypersistence:hypersistence-utils-hibernate-71:3.14.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
