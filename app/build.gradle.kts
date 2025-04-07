plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "com.lianxiangdaimaowang.lumina"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lianxiangdaimaowang.lumina"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 多dex支持
        multiDexEnabled = true
        
        // 添加网络权限声明
        manifestPlaceholders["usesCleartextTraffic"] = "true"
    }

    // 添加签名配置
    signingConfigs {
        create("release") {
            storeFile = file("../lumina-release.keystore")
            storePassword = "lumina123"
            keyAlias = "luminakey"
            keyPassword = "lumina123"
        }
        getByName("debug") {
            storeFile = file("../debug.keystore")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 使用debug签名配置代替release配置
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
    }
    
    // 添加Lint配置
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    
    // 解决SO库冲突
    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/versions/21/**"
            )
        }
    }
}

dependencies {
    // Android基础组件
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")
    
    // Preference库 - 用于设置界面
    implementation("androidx.preference:preference:1.2.1")
    
    // Room数据库
    implementation("androidx.room:room-runtime:2.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    
    // WorkManager 任务管理
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Gson - 用于JSON解析
    implementation("com.google.code.gson:gson:2.10")
    
    // Retrofit 网络库
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    
    // Glide 图片加载
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // QQ开放平台SDK - 已禁用
    // implementation("com.tencent.tauth:qqopensdk:3.52.0")
    
    // 微信开放平台SDK - 已禁用
    // implementation("com.tencent.mm.opensdk:wechat-sdk-android-without-mta:6.8.18")
    
    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
