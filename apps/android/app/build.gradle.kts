plugins {
    id("com.android.application")
}

android {
    namespace = "com.linjian.tongpin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.linjian.tongpin"
        minSdk = 26
        targetSdk = 34
        versionCode = 21
        versionName = "1.3.1-tong"
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res/drawable-nodpi/apps/android/app/src/main/res/drawable-nodpi"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
}
