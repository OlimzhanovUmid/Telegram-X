plugins {
  id(libs.plugins.android.library.get().pluginId)
  id("tgx-module")
}

dependencies {
  implementation(libs.androidx.core.ktx)
  api(libs.kotlinx.coroutines.core)
  implementation(project(":tdlib"))
}

android {
  namespace = "tgx.bridge"
}