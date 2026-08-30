/*
 * Copyright 2026 OEM. All Rights Reserved.
 *
 * The coldRoom-rt module part. Authored per the devkit gradle-niagara templates
 * (module.gradle.kts.vm + modulePlugins.vm + moduleDependencies.vm).
 */

import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.*

plugins {
  // Configures the "moduleManifest" extension and the "jar" / "moduleTestJar" tasks.
  id("com.tridium.niagara-module")
  // Correct module signing (also applied on the root project).
  id("com.tridium.niagara-signing")
  // Bajadoc API doc generation.
  id("com.tridium.bajadoc")
  // JaCoCo for the niagaraTest task.
  id("com.tridium.niagara-jacoco")
  // Runs Slot-o-Matic via the annotation-processor configuration (":nre").
  id("com.tridium.niagara-annotation-processors")
  // !bin/ext + !modules as flat-file Maven repos.
  id("com.tridium.convention.niagara-home-repositories")
}

description = "OEM Cold-Room control — reusable BColdRoom / BEvaporatorUnit / BDefrostController types"

moduleManifest {
  moduleName.set("coldRoom")
  // runtimeProfile is load-bearing: a station daemon (-rp:rt,se) only loads rt.
  // rt = runtime-only part (module-best-practices 1, module-dev-workflow 4.1).
  runtimeProfile.set(rt)
}

dependencies {
  // Niagara module dependencies (design section 10 / module.xml <dependencies>).
  // Versions come from the target SDK home (build against 4.13/4.14/4.15 deliberately).
  api("Tridium:baja:4.14.0")          // BComponent, BStatus*, BRelTime, Clock, enums
  api("Tridium:control-rt:4.14.0")    // writable/point patterns the outputs link to
  api("Tridium:kitControl-rt:4.14.0") // BTstat/BBooleanDelay/BOr reference primitives
  api("Tridium:alarm-rt:4.14.0")      // BAlarmSourceExt (visual alarms, design section 9)
  api("Tridium:history-rt:4.14.0")    // history + AuditHistoryService (sections 7/8)
  api("Tridium:schedule-rt:4.14.0")   // BBooleanSchedule for defrost schedule mode

  // TODO [INFER]: confirm the exact "vendor:module:version" dependency coordinates
  // your gradle-niagara install resolves (docSource shows the module names; the
  // group/version convention is SDK-home specific). Prune any dependency a final
  // build proves unused (e.g. control-rt if outputs are pure BStatusBoolean slots).
}
