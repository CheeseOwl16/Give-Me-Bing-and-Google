plugins { id("com.android.application") version "9.3.3" apply false }

allprojects {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir(".local/build/$name"))
}
