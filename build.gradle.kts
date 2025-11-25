allprojects {
    tasks.withType<Test> {
        useJUnitPlatform()
    }

    pluginManager.withPlugin("java") {
        repositories {
            mavenCentral()
        }
        dependencies {
            "testImplementation"(platform("org.junit:junit-bom:6.0.1"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testImplementation"("org.junit.platform:junit-platform-launcher")
        }

        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

    }
}

