/*
 * Copyright 2026 OEM. All Rights Reserved.
 *
 * Root build for the coldRoom module. Authored per the devkit gradle-niagara
 * template (organized/devkit/.../gradle/build.gradle.kts.vm).
 */

plugins {
  // Base Niagara plugin.
  id("com.tridium.niagara")

  // Provides the vendor {} extension: default Maven group, default vendor
  // attribute for manifests, and default module/dist versions.
  id("com.tridium.vendor")

  // Auto-signs all modules from niagara_user_home/security/keystore.jceks.
  // No explicit config block (module-dev-workflow section 1.2).
  id("com.tridium.niagara-signing")

  // Exposes !bin/ext and !modules as flat-file Maven repos so the module can
  // compile against the installed Niagara SDK.
  id("com.tridium.convention.niagara-home-repositories")
}

vendor {
  // "vendor" attribute shown in Niagara for the module/dist.
  // TODO: replace the OEM placeholder with the real vendor identity; the signing
  // alias must match this vendor (module-best-practices 5.3, e.g. ANGELES).
  defaultVendor("OEM")

  // "vendorVersion" on all modules. Bump per release (best-practices 5.4).
  defaultModuleVersion("1.0.0")
}

subprojects {
  repositories {
    mavenCentral()
  }
}
