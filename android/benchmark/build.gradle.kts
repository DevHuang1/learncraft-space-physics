plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

dependencies { implementation(project(":shared")) }

application { mainClass.set("com.learncraft.spacephysics.benchmark.SpatialHashBenchmarkKt") }
