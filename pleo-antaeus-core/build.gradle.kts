plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    compile("com.github.shyiko.skedule:skedule:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1")
    compile("org.awaitility:awaitility:3.1.6")
}