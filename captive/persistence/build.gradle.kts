plugins {
    kotlin("plugin.jpa")
}

group = "cz.grimir.wifimanager.captive.persistence"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}