plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// 개인 콘솔 키는 커밋하지 않는 app/keys.json 한곳에 둔다 (Dart는 --dart-define-from-file로 같은 파일을 읽는다)
val appKeys: Map<*, *> = rootProject.file("../keys.json")
    .takeIf { it.exists() }
    ?.let { groovy.json.JsonSlurper().parse(it) as Map<*, *> }
    ?: emptyMap<String, String>()

android {
    namespace = "com.fitness.traffic_gym_app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.fitness.traffic_gym_app"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        // 카카오 로그인 리다이렉트 스킴(kakao{네이티브 앱 키}). 키가 없으면 카카오 버튼이 꺼져 있어 쓰이지 않는다
        manifestPlaceholders["kakaoNativeAppKey"] = (appKeys["KAKAO_NATIVE_APP_KEY"] as String?)
            ?.takeIf { it.isNotEmpty() } ?: "none"
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}
