plugins { id("com.android.application") }

android {
    namespace = "dev.cheeseowl.gmbg"
    enableKotlin = false
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "dev.cheeseowl.gmbg"
        minSdk = 28
        targetSdk = 35
        versionCode = 40005
        versionName = "1.0.0-rc1"
    }
    buildTypes {
        debug { isMinifyEnabled = false }
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = false }
    packaging { resources.excludes += setOf("META-INF/DEPENDENCIES") }
}

dependencies {
    compileOnly(files("../vendor/libxposed-api-102.0.0.jar"))
    testImplementation("junit:junit:4.13.2")
}

// Optional, build-host-only Windows workaround. Never a runtime APK dependency.
tasks.withType<Test>().configureEach {
    providers.gradleProperty("gmbgHostAgent").orNull?.let { jvmArgs("-javaagent:$it") }
}

// This project has no device tasks in its supported build workflow.
gradle.taskGraph.whenReady {
    require(allTasks.none { it.name.contains("install", true) ||
        it.name.contains("connected", true) || it.name.contains("managedDevice", true) }) {
        "Device operations are permanently outside this project execution scope"
    }
}
