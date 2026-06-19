@file:Suppress("UnstableApiUsage")

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.impl.VariantOutputImpl
import tgx.gradle.*
import tgx.gradle.task.*
import java.util.*

plugins {
  id(libs.plugins.android.application.get().pluginId)
  id("tgx-config")
  id("tgx-module")
}

val generateResourcesAndThemes by tasks.registering(GenerateResourcesAndThemesTask::class) {
  group = "Setup"
  description = "Generates fresh strings, ids, theme resources and utility methods based on current static files"
}
val updateLanguages by tasks.registering(FetchLanguagesTask::class) {
  group = "Setup"
  description = "Generates and updates all strings.xml resources based on translations.telegram.org"
}
val validateApiTokens by tasks.registering(ValidateApiTokensTask::class) {
  group = "Setup"
  description = "Validates some API tokens to make sure they work properly and won't cause problems"
}
val updateExceptions by tasks.registering(UpdateExceptionsTask::class) {
  group = "Setup"
  description = "Updates exception class names with the app or TDLib version number in order to have separate group on Google Play Developer Console"
}
val generatePhoneFormat by tasks.registering(GeneratePhoneFormatTask::class) {
  group = "Setup"
  description = "Generates utility methods for phone formatting, e.g. +12345678901 -> +1 (234) 567 89-01"
}
val checkEmojiKeyboard by tasks.registering(CheckEmojiKeyboardTask::class) {
  group = "Setup"
  description = "Checks that all supported emoji can be entered from the keyboard"
}

val config = extra["config"] as ApplicationConfig

//noinspection WrongGradleMethod
android {
  namespace = "org.thunderdog.challegram"

  lint {
    disable += "MissingTranslation"
    checkDependencies = true
  }

  externalNativeBuild {
    cmake {
      path("jni/CMakeLists.txt")
    }
  }

  defaultConfig {
    applicationId = config.applicationId
    targetSdk = config.targetSdkVersion
    multiDexEnabled = true

    buildConfigString("PROJECT_NAME", config.applicationName)
    buildConfigBool("SHARED_STL", Config.SHARED_STL)
    buildConfigString("SAFETYNET_API_KEY", config.safetyNetToken)

    buildConfigString("DOWNLOAD_URL", config.appDownloadUrl)
    buildConfigString("GOOGLE_PLAY_URL", config.googlePlayUrl)
    buildConfigString("GALAXY_STORE_URL", config.galaxyStoreUrl)
    buildConfigString("HUAWEI_APPGALLERY_URL", config.huaweiAppGalleryUrl)
    buildConfigString("AMAZON_APPSTORE_URL", config.amazonAppStoreUrl)

    buildConfigString("TGX_EXTENSION", config.extension)

    buildConfigString("JNI_VERSION", config.nativeLibraryVersion)
    buildConfigString("LEVELDB_VERSION", config.leveldbVersion)

    buildConfigString("TDLIB_REMOTE_URL", "https://github.com/tdlib/td")

    buildConfigField("boolean", "EXPERIMENTAL", config.isExperimentalBuild.toString())

    buildConfigInt("TARGET_SDK_INT", config.targetSdkVersion)

    buildConfigInt("TELEGRAM_API_ID", config.telegramApiId)
    buildConfigString("TELEGRAM_API_HASH", config.telegramApiHash)

    buildConfigString("TELEGRAM_RESOURCES_CHANNEL", Telegram.RESOURCES_CHANNEL)
    buildConfigString("TELEGRAM_UPDATES_CHANNEL", Telegram.UPDATES_CHANNEL)

    buildConfigInt("EMOJI_VERSION", config.emojiVersion)
    buildConfigString("EMOJI_BUILTIN_ID", Emoji.BUILTIN_ID)

    buildConfigString("LANGUAGE_PACK", Telegram.LANGUAGE_PACK)

    buildConfigString("THEME_FILE_EXTENSION", App.THEME_EXTENSION)

    // Library versions in BuildConfig.java

    var openSslVersion = ""
    var openSslVersionFull = ""
    val openSslVersionFile = File(project.rootDir.absoluteFile, "tdlib/source/openssl/include/openssl/opensslv.h")
    openSslVersionFile.bufferedReader().use { reader ->
      val regex = Regex("^#\\s*define OPENSSL_VERSION_NUMBER\\s*((?:0x)[0-9a-fAF]+)L?\$")
      while (true) {
        val line = reader.readLine() ?: break
        val result = regex.find(line)
        if (result != null) {
          val rawVersion = result.groupValues[1]
          val version = if (rawVersion.startsWith("0x")) {
            rawVersion.substring(2).toLong(16)
          } else {
            rawVersion.toLong()
          }
          // MNNFFPPS: major minor fix patch status
          val major = ((version shr 28) and 0xf).toInt()
          val minor = ((version shr 20) and 0xff).toInt()
          val fix = ((version shr 12) and 0xff).toInt()
          val patch = ((version shr 4) and 0xff).toInt()
          val status = (version and 0xf).toInt()
          if (status != 0xf) {
            fatal("Using non-stable OpenSSL version: $rawVersion (status = ${status.toString(16)})")
          }
          openSslVersion = "${major}.${minor}"
          openSslVersionFull = "${major}.${minor}.${fix}${('a'.code - 1 + patch).toChar()}"
          break
        }
      }
    }
    if (openSslVersion.isEmpty()) {
      fatal("OpenSSL not found!")
    }

    var tdlibVersion = ""
    val tdlibCommit = File(project.rootDir.absoluteFile, "tdlib/version.txt").bufferedReader().readLine().take(7)
    val tdlibVersionFile = File(project.rootDir.absoluteFile, "tdlib/source/td/CMakeLists.txt")
    tdlibVersionFile.bufferedReader().use { reader ->
      val regex = Regex("^project\\(TDLib VERSION (\\d+\\.\\d+\\.\\d+) LANGUAGES CXX C\\)$")
      while (true) {
        val line = reader.readLine() ?: break
        val result = regex.find(line)
        if (result != null) {
          tdlibVersion = "${result.groupValues[1]}-${tdlibCommit}"
          break
        }
      }
    }
    if (tdlibVersion.isEmpty()) {
      fatal("TDLib not found!")
    }

    buildConfigString("OPENSSL_VERSION", openSslVersion)
    buildConfigString("OPENSSL_VERSION_FULL", openSslVersionFull)
    buildConfigString("TDLIB_VERSION", tdlibVersion)

    val tgxGitVersionProvider = providers.of(GitVersionValueSource::class) {
      parameters.module = layout.projectDirectory
    }
    val tgxGit = tgxGitVersionProvider.get()

    val sourcesUrl = config.sourceCodeUrl.takeIf {
      it.isNotEmpty()
    } ?: tgxGit.remoteUrl
    buildConfigString("REMOTE_URL", tgxGit.remoteUrl)
    buildConfigString("COMMIT_URL", tgxGit.commitUrl)
    buildConfigString("COMMIT", tgxGit.commitHashShort)
    buildConfigString("COMMIT_FULL", tgxGit.commitHashLong)
    buildConfigLong("COMMIT_DATE", tgxGit.commitDate)
    buildConfigString("SOURCES_URL", sourcesUrl)

    buildConfigField("long[]", "PULL_REQUEST_ID", "{${
      config.pullRequests.joinToString(", ") { it.id.toString() }
    }}")
    buildConfigField("long[]", "PULL_REQUEST_COMMIT_DATE", "{${
      config.pullRequests.joinToString(", ") { it.commitDate.toString() }
    }}")
    buildConfigField("String[]", "PULL_REQUEST_COMMIT", "{${
      config.pullRequests.joinToString(", ") { "\"${it.commitShort}\"" }
    }}")
    buildConfigField("String[]", "PULL_REQUEST_COMMIT_FULL", "{${
      config.pullRequests.joinToString(", ") { "\"${it.commitLong}\"" }
    }}")
    buildConfigField("String[]", "PULL_REQUEST_URL", "{${
      config.pullRequests.joinToString(", ") { "\"${tgxGit.remoteUrl}/pull/${it.id}/files/${it.commitLong}\"" }
    }}")
    buildConfigField("String[]", "PULL_REQUEST_AUTHOR", "{${
      config.pullRequests.joinToString(", ") { "\"${it.author}\"" }
    }}")

    // WebRTC version

    val webrtcGit = providers.of(GitVersionValueSource::class) {
      parameters.module = layout.projectDirectory.dir("jni/third_party/webrtc")
    }.get()
    buildConfigString("WEBRTC_COMMIT", webrtcGit.commitHashShort)
    buildConfigString("WEBRTC_COMMIT_URL", webrtcGit.commitUrl)

    // tgcalls version

    val tgcallsGit = providers.of(GitVersionValueSource::class) {
      parameters.module = layout.projectDirectory.dir("jni/third_party/tgcalls")
    }.get()
    buildConfigString("TGCALLS_COMMIT", tgcallsGit.commitHashShort)
    buildConfigString("TGCALLS_COMMIT_URL", tgcallsGit.commitUrl)

    // FFmpeg version

    val ffmpegGit = providers.of(GitVersionValueSource::class) {
      parameters.module = layout.projectDirectory.dir("jni/third_party/ffmpeg")
    }.get()
    buildConfigString("FFMPEG_COMMIT", ffmpegGit.commitHashShort)
    buildConfigString("FFMPEG_COMMIT_URL", ffmpegGit.commitUrl)

    // WebP version

    val webpGit = providers.of(GitVersionValueSource::class) {
      parameters.module = layout.projectDirectory.dir("jni/third_party/webp")
    }.get()
    buildConfigString("WEBP_COMMIT", webpGit.commitHashShort)
    buildConfigString("WEBP_COMMIT_URL", webpGit.commitUrl)

    // Set application version

    val timeZone = TimeZone.getTimeZone("UTC")
    val then = Calendar.getInstance(timeZone)
    then.timeInMillis = config.creationDateMillis
    val now = Calendar.getInstance(timeZone)
    now.timeInMillis = tgxGit.commitDate * 1000L
    if (now.timeInMillis < then.timeInMillis)
      fatal("Invalid commit time!")
    val minorVersion = monthYears(now, then)

    versionCode = config.applicationVersion
    versionName = "${config.majorVersion}.${minorVersion}"

    // Native build (formerly per-SDK-flavor; now a single minSdk 26 configuration)
    val nativeFlags = listOf(
      "-w",
      "-Werror=return-type",
      "-ferror-limit=0",
      "-fno-exceptions",

      "-O3",
      "-finline-functions"
    )
    externalNativeBuild.cmake {
      arguments(
        "-DANDROID_PLATFORM=android-${Config.MIN_SDK_VERSION}",
        "-DTGX_FLAVOR=latest",
        "-DANDROID_STL=${if (Config.SHARED_STL) "c++_shared" else "c++_static"}",
        "-DCMAKE_BUILD_WITH_INSTALL_RPATH=ON",
        "-DCMAKE_SKIP_RPATH=ON",
        "-DCMAKE_C_VISIBILITY_PRESET=hidden",
        "-DCMAKE_CXX_VISIBILITY_PRESET=hidden",
        "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections,--icf=safe -Wl,--build-id=sha1",
        "-DCMAKE_C_FLAGS=-D_LARGEFILE_SOURCE=1 ${nativeFlags.joinToString(" ")}",
        "-DCMAKE_CXX_FLAGS=-std=c++17 ${nativeFlags.joinToString(" ")}"
      )
    }

    // Compatibility BuildConfig flags: the SDK flavor dimension was removed, so these
    // are no longer auto-generated. "latest" is now the only configuration.
    buildConfigBool("LEGACY_FLAVOR", false)
    buildConfigBool("LOLLIPOP_FLAVOR", false)
    buildConfigBool("LATEST_FLAVOR", true)
    buildConfigString("FLAVOR_SDK", "latest")

    var extraProguardFileCount = 0
    arrayOf(
      "exoplayer",
      "common",
      "transformer",
      "extractor",
      "muxer",
      "decoder",
      "container",
      "datasource",
      "database",
      "effect"
    ).plus(Config.ANDROIDX_MEDIA_EXTENSIONS).forEach { extension ->
      val proguardRules = file(
        "../thirdparty/androidx-media/latest/libraries/${extension}/proguard-rules.txt"
      )
      if (proguardRules.exists()) {
        extraProguardFileCount++
        proguardFile(proguardRules)
      }
    }
    if (extraProguardFileCount > 0) {
      project.logger.lifecycle("[proguard]: Applied $extraProguardFileCount extra proguard files")
    }
  }

  sourceSets.getByName("main") {
    // TODO: Exclude in FOSS variant
    kotlin.directories += "src/google/java"
    java.directories += "src/google/java"
    // Native media decoders, formerly wired per-SDK-flavor (now single minSdk 26 config)
    Config.ANDROIDX_MEDIA_EXTENSIONS.forEach { extension ->
      java.directories += "../thirdparty/androidx-media/latest/libraries/${extension}/src/main/java"
    }
  }

  lint {
    disable += "MissingTranslation"
    checkDependencies = true
  }

  buildFeatures {
    buildConfig = true
  }

  flavorDimensions += "ABI"
  androidComponents.beforeVariants { variantBuilder ->
    val abiFlavor = variantBuilder.productFlavors.first { it.first == "ABI" }.second
    val abiVariant = Abi.VARIANTS.values.first { it.flavor == abiFlavor }
    // Debug builds are only produced for the cheap-to-build ABIs.
    variantBuilder.enable = variantBuilder.buildType != "debug" ||
      abiVariant.flavor == "x86" || abiVariant.flavor == "x64" || abiVariant.flavor == "universal"
  }
  productFlavors {
    Abi.VARIANTS.forEach { (abiIndex, variant) ->
      create(variant.flavor) {
        dimension = "ABI"
        isDefault = abiIndex == 0
        ndkVersion = if (variant.is64Bit) {
          config.primaryNdkVersion
        } else {
          config.legacyNdkVersion
        }
        // ndkPath = File(sdkDirectory, "ndk/$ndkVersion").absolutePath
        buildConfigString("NDK_VERSION", ndkVersion)
        // With a single flavor dimension AGP no longer auto-generates FLAVOR_<dimension>,
        // so re-emit FLAVOR_ABI (still read in U.java) per ABI flavor.
        buildConfigString("FLAVOR_ABI", variant.flavor)
        buildConfigBool("WEBP_ENABLED", true) // variant.minSdk < 19
        if (ndk.abiFilters.isNotEmpty())
          error(ndk.abiFilters.joinToString())
        ndk.abiFilters.addAll(variant.filters)
        externalNativeBuild.ndkBuild.abiFilters(*variant.filters)
        externalNativeBuild.cmake.abiFilters(*variant.filters)
      }
    }
  }

  androidComponents {
    onVariants { variant ->
      val abiFlavor = variant.productFlavors.first { it.first == "ABI" }.second

      val (abi, abiVariant) = Abi.VARIANTS.entries.first { it.value.flavor == abiFlavor }

      val flavorVersionCode = if (variant.debuggable) 0 else {
        abi
      }
      val flavorVersionNameSuffix = StringBuilder().apply {
        if (extra.has("app_version_suffix")) {
          append(extra["app_version_suffix"])
        }
        if (config.extension != "none") {
          append("-${config.extension}")
        }
        if (abiVariant.displayName != "universal" || config.extension == "none") {
          append("-${abiVariant.displayName}")
        }
        if (extra.has("app_name_suffix")) {
          append("-${extra["app_name_suffix"]}")
        }
        if (variant.debuggable) {
          append("-debug")
        }
      }.toString()

      var baseVersionCode: Int? = null
      var baseVersionName: String? = null
      var fileName: String? = null

      variant.outputs.forEach { output ->
        baseVersionCode = output.versionCode.get()
        val modifiedVersionCode = baseVersionCode * 1000 + flavorVersionCode
        output.versionCode.set(modifiedVersionCode)

        baseVersionName = output.versionName.get()
        val modifiedVersionName = "$baseVersionName.$baseVersionCode$flavorVersionNameSuffix"
        output.versionName.set(modifiedVersionName)

        fileName = "${config.outputFileNamePrefix}-${modifiedVersionName.replace(Regex("-universal(?=-|$)"), "")}"
        if (output is VariantOutputImpl) {
          output.outputFileName.set("$fileName.apk")
        }
      }
      require(baseVersionCode != null && baseVersionName != null && fileName != null)

      val recaptchaVersion = libs.google.recaptcha.get().version!!

      variant.buildConfigFields!!.apply {
        put("ABI", BuildConfigField(
          "int", abi, null
        ))
        put("RECAPTCHA_VERSION", BuildConfigField(
          "String", "\"$recaptchaVersion\"", null
        ))
        put("ORIGINAL_VERSION_CODE", BuildConfigField(
          "int", baseVersionCode, null
        ))
        put("ORIGINAL_VERSION_NAME", BuildConfigField(
          "String", "\"$baseVersionName.$baseVersionCode\"", null
        ))
      }

      if (variant.isMinifyEnabled) {
        val copyTask = project.tasks.register<Copy>("copy${variant.name.replaceFirstChar { it.uppercase() }}MappingFile") {
          from(variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE))
          into(project.layout.buildDirectory.dir("outputs/mapping/${variant.name}"))
          rename("mapping.txt", "$fileName.txt")
        }
        tasks.named {
          it.startsWith("assemble") && it.endsWith("Release")
        }.configureEach {
          finalizedBy(copyTask)
        }
      }
    }
  }

  // Packaging

  packaging {
    Config.SUPPORTED_ABI.forEach { abi ->
      jniLibs.pickFirsts.let { set ->
        if (Config.SHARED_STL) {
          set.add("lib/$abi/libc++_shared.so")
        }
        set.add("tdlib/openssl/$abi/lib/libcryptox.so")
        set.add("tdlib/openssl/$abi/lib/libsslx.so")
        set.add("tdlib/src/main/libs/$abi/libtdjni.so")
      }
    }
  }
}

gradle.projectsEvaluated {
  tasks.preBuild.configure {
    dependsOn(
      generateResourcesAndThemes,
      checkEmojiKeyboard,
      generatePhoneFormat,
      updateExceptions,
    )
  }
  tasks.named {
    it.startsWith("pre") && it.endsWith("ReleaseBuild")
  }.configureEach {
    dependsOn(updateLanguages)
    if (!config.isExperimentalBuild) {
      dependsOn(validateApiTokens)
    }
  }
}

dependencies {
  implementation(project(":extension:${config.extension}"))
  // TDLib: https://github.com/tdlib/td/blob/master/CHANGELOG.md
  implementation(project(":tdlib"))
  implementation(project(":tgcalls"))
  implementation(project(":vkryl:core"))
  implementation(project(":vkryl:leveldb"))
  implementation(project(":vkryl:android"))
  implementation(project(":vkryl:td"))
  // AndroidX: https://developer.android.com/jetpack/androidx/versions
  implementation(libs.androidx.activity)
  implementation(libs.androidx.gridlayout)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.viewpager)
  implementation(libs.androidx.browser)
  implementation(libs.androidx.work.runtime)
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.palette)
  implementation(libs.androidx.collection)
  implementation(libs.androidx.interpolator)
  // CameraX: https://developer.android.com/jetpack/androidx/releases/camera
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.video)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  // Google Play Services: https://developers.google.com/android/guides/releases
  implementation(libs.google.play.services.base)
  implementation(libs.google.play.services.basement)
  implementation(libs.google.play.services.maps)
  implementation(libs.google.play.services.location)
  implementation(libs.google.play.services.safetynet)
  // ML Kit: https://developers.google.com/ml-kit/release-notes
  implementation(libs.google.play.services.mlkit.barcode.scanning)
  implementation(libs.google.mlkit.language.id)
  // Firebase: https://firebase.google.com/support/release-notes/android
  implementation(libs.google.firebase.messaging) {
    exclude(group = "com.google.firebase", module = "firebase-core")
    exclude(group = "com.google.firebase", module = "firebase-analytics")
    exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
  }
  // Play Integrity: https://developer.android.com/google/play/integrity/reference/com/google/android/play/core/release-notes
  implementation(libs.google.play.integrity)
  // ReCaptcha: https://cloud.google.com/recaptcha/docs/release-notes
  implementation(libs.google.recaptcha)
  // AndroidX/media: https://github.com/androidx/media/blob/release/RELEASENOTES.md
  implementation(libs.androidx.media.common)
  implementation(libs.androidx.media.transformer)
  implementation(libs.androidx.media.effect)
  implementation(libs.androidx.media.exoplayer)
  implementation(libs.androidx.media.exoplayer.hls)
  implementation(libs.androidx.media.inspector)
  // Play In-App Updates: https://developer.android.com/reference/com/google/android/play/core/release-notes-in_app_updates
  implementation(libs.google.play.app.update)
  // The Checker Framework: https://checkerframework.org/CHANGELOG.md
  compileOnly(libs.annotations.checkerframework)
  // OkHttp: https://github.com/square/okhttp/blob/master/CHANGELOG.md
  implementation(libs.okhttp)
  // ShortcutBadger: https://github.com/leolin310148/ShortcutBadger
  implementation(libs.shortcutbadger) {
    artifact { type = "aar" }
  }
  // Konfetti: https://github.com/DanielMartinus/Konfetti/blob/main/README.md
  implementation(libs.konfetti)
  // https://github.com/mikereedell/sunrisesunsetlib-java
  implementation(libs.sunriseSunsetCalculator)

  // ZXing: https://github.com/zxing/zxing/blob/master/CHANGES
  implementation(libs.google.zxing.core)

  // subsampling-scale-image-view: https://github.com/davemorrissey/subsampling-scale-image-view
  implementation(libs.subsamplingScaleImageView)

  // mp4parser: https://github.com/sannies/mp4parser/releases
  implementation(libs.mp4parser.isoparser)

  // Compiler warnings
  compileOnly(libs.annotations.errorprone)
  compileOnly(libs.annotations.j2objc)
  compileOnly(libs.androidx.room)
  compileOnly(libs.annotations.jsr305)
  compileOnly(libs.annotations.kotlin)
}

if (!config.isExperimentalBuild) {
  apply(plugin = libs.plugins.google.services.get().pluginId)
  if (config.isHuaweiBuild) {
    apply(plugin = libs.huawei.agconnect.get().group)
  }
}