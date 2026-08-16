plugins {
    kotlin("jvm") version "2.0.21"
    application
}

dependencies { implementation(project(":shared")) }

application { mainClass.set("com.learncraft.spacephysics.benchmark.SpatialHashBenchmarkKt") }
