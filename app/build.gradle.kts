import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// IDs do Appwrite vêm de local.properties (não versionado). Ver docs/appwrite-setup.md.
val appwriteProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun appwriteProp(key: String): String = appwriteProps.getProperty(key, "")

android {
    namespace = "dev.paraizo.cost"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.paraizo.cost"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "APPWRITE_PROJECT_ID", "\"${appwriteProp("appwrite.projectId")}\"")
        buildConfigField("String", "APPWRITE_DATABASE_ID", "\"${appwriteProp("appwrite.databaseId")}\"")
        buildConfigField("String", "APPWRITE_COLLECTION_GRUPOS", "\"${appwriteProp("appwrite.collectionGrupos")}\"")
        buildConfigField("String", "APPWRITE_COLLECTION_PESSOAS", "\"${appwriteProp("appwrite.collectionPessoas")}\"")
        buildConfigField("String", "APPWRITE_COLLECTION_GASTOS", "\"${appwriteProp("appwrite.collectionGastos")}\"")
        buildConfigField("String", "APPWRITE_COLLECTION_RENDAS", "\"${appwriteProp("appwrite.collectionRendas")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.appwrite.sdk)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
