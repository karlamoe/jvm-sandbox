rootProject.name = "jvm-sandbox"

fun submodule(module: String) {
    include(":$module")

    project(":$module").projectDir = file("src/$module")
}

submodule("jvm-sandbox-transformer")

