/*
 * Copyright 2026 OEM. All Rights Reserved.
 *
 * Authored per the devkit gradle-niagara template
 * (organized/devkit/.../gradle/settings.gradle.kts.vm).
 */

import com.tridium.gradle.plugins.settings.LocalSettingsExtension

pluginManagement {
  // TODO [INFER]: point these at the shop's Niagara plugin repo + versions
  // (settingsRepoUrl / gradlePluginVersion come from local Niagara SDK config).
  val gradlePluginVersion: String by settings
  val settingsPluginVersion: String by settings

  repositories {
    mavenCentral()
  }

  plugins {
    id("com.tridium.settings.multi-project") version (settingsPluginVersion)
    id("com.tridium.settings.local-settings-convention") version (settingsPluginVersion)

    id("com.tridium.niagara") version (gradlePluginVersion)
    id("com.tridium.vendor") version (gradlePluginVersion)
    id("com.tridium.niagara-module") version (gradlePluginVersion)
    id("com.tridium.niagara-signing") version (gradlePluginVersion)

    id("com.tridium.convention.niagara-home-repositories") version (gradlePluginVersion)
  }
}

plugins {
  // Discovers all subprojects (coldRoom-rt) in this build.
  id("com.tridium.settings.multi-project")
  // Applies local/my-niagara.gradle(.kts) overrides if present.
  id("com.tridium.settings.local-settings-convention")
}

rootProject.name = "coldRoom"
