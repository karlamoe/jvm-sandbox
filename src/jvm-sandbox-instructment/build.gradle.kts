plugins {
    `java-library`
}


dependencies {
    api(project(":jvm-sandbox-transformer"))
    api(project(":jvm-sandbox-runtime"))

    testImplementation(project(":test-helper"))

}
