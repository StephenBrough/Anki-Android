import org.gradle.kotlin.dsl.`kotlin-dsl`

object Versions {
    // Build config
    val buildToolsVersion = "27.0.3"
    val versionCode = 20900105
    val versionName = "2.9alpha5"

    // Plugins
    val fabricPlugin = "1.25.1"

    // Support Lib
    val support = "27.0.2"

    // Architecture
    val architectureComponents = "1.1.0"
    val archCompiler = "1.0.0"
    val archRoom = "1.0.0"

    // SQLite
    val requery = "3.16.0"

    // Material Dialogs
    val materialDialogs = "0.8.6.2@aar"

    val fab = "1.10.1"

    val timber = "4.5.1"

    val crashlytics = "2.6.8@aar"

    val gson = "2.8.0"

    val junit = "4.12"
    val mockito = "1.10.19"
    val powermock = "1.6.5"

    val hamcrest = "1.3"

    val kotlin = "1.2.21"
    val kotlinCoroutines = "0.18"
}

object Dependencies {
    // Support
    val supportAppCompat = "com.android.support:appcompat-v7:${Versions.support}"
    val supportDesign = "com.android.support:design:${Versions.support}"
    val supportCustomTabs = "com.android.support:customtabs:${Versions.support}"
    val supportRecyclerView = "com.android.support:recyclerview-v7:${Versions.support}"

    // SQLite
    val requerySqliteAndroid = "io.requery:sqlite-android:${Versions.requery}"

    // Material Dialogs
    val materialDialogs = "com.afollestad.material-dialogs:core:${Versions.materialDialogs}"

    val fab = "com.getbase:floatingactionbutton:${Versions.fab}"

    // Logging
    val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    val crashlytics = "com.crashlytics.sdk.android:crashlytics:${Versions.crashlytics}"

    // Gson
    val gson = "com.google.code.gson:gson:${Versions.gson}"

    // Testing
    val junit = "junit:junit:${Versions.junit}"
    val mockitoCore = "org.mockito:mockito-core:${Versions.mockito}" // v1 while PowerMock does not fully support v2.
    val powerMockCore = "org.powermock:powermock-core:${Versions.powermock}"
    val powerMockJunit = "org.powermock:powermock-module-junit4:${Versions.powermock}"
    val powerMockApiMockito = "org.powermock:powermock-api-mockito:${Versions.powermock}"
    val hamcrest = "org.hamcrest:hamcrest-all:${Versions.hamcrest}"

    // Kotlin
    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"

    // Architecture
    val archRuntime = "android.arch.lifecycle:runtime:${Versions.architectureComponents}"
    val archExtensions = "android.arch.lifecycle:extensions:${Versions.archCompiler}"
    val archCompiler = "android.arch.lifecycle:compiler:${Versions.archCompiler}"
    val archRoom = "android.arch.persistence.room:runtime:${Versions.archRoom}"
    val archRoomCompiler = "android.arch.persistence.room:compiler:${Versions.archRoom}"
}