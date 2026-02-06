import com.google.protobuf.gradle.id

group = "cz.grimir.wifimanager.captive.routeragent"

plugins {
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(project(":shared:application"))
    implementation(project(":captive:application"))
    implementation(libs.springBootStarterWeb)
    implementation(libs.kotlinLogging)
    implementation(libs.bundles.grpc)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                id("grpc")
            }
        }
    }
}
