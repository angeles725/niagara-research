# Niagara N4 — Mental Model · Índice Maestro

**Actualizado**: 2026-04-24 (sesión bloques 30-32 — cierre gaps 20.10)
**Distribución analizada**: Honeywell OptimizerSupervisor-N4.14.0.162
**Método**: Investigación empírica READ-ONLY con sub-agents en paralelo, contrastando docs oficiales (devguide 82 topics HTML + niagara-help/ 950 MB extracción) contra source Java + decompilado Vineflower + 974 JARs indexados + análisis nativo (138 DLLs/SOs catalogados).

Este índice te guía entre los **32 bloques** de investigación. Cada bloque es un archivo `.md` independiente que puede leerse aislado, pero las conexiones están explícitamente marcadas entre sí.

Cobertura final estimada: **~99.5%** del framework Niagara N4.14 conceptualmente. Los 3 bloques nuevos (30-32) cierran los gaps residuales catalogados en Bloque 20.10: #1/#3/#5/#7/#8/#10/#11/#13/#15/#16/#17/#18/#20/#21/#22/#24/#27 — Enterprise auth federation + FIPS + key rotation, Performance tuning + observability, Honeywell modules + runtime semantics. Los gaps NO investigables sin lab/NDA (#2 clustering HA, #9 remote diagnostics, #14 Skyspark alternatives, #19/#23/#25/#26 clock+lab-required) quedan explícitos en gap analysis final.

---

## Mapa completo

### Capa 1 — Infraestructura (Bloques 1-3)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 1 | Estructura del framework | [niagara-mental-model.md §1](niagara-mental-model.md) | Profiles rt/ux/wb, module.xml, NRE/Station/Workbench procesos, registry de tipos, Fox protocol |
| 2 | Licenciamiento | [niagara-mental-model.md §2](niagara-mental-model.md) | HostId, Cert, License XML, Features, SMA, LicenseManager API, Honeywell OEM overlay |
| 3 | Modelo de seguridad (sandbox JVM) | [niagara-mental-model.md §3](niagara-mental-model.md) | Cert chain pipeline, 19 permission groups (corregido: 25 en Bloque 18.4), policy files firmados, NiagaraSocketPermission, skipModuleValidation |

### Capa 2 — Modelo de programación (Bloques 4-6)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 4 | Baja Object Model | [niagara-mental-model-bloque4.md](niagara-mental-model-bloque4.md) | Slot system (21 flags), BObject→BValue→BComplex/BSimple/BStruct jerarquía, BComponent lifecycle callbacks, BFacets, dynamic slots, slot-o-matic-2000 annotations |
| 5 | Navegación + persistencia + queries | [niagara-mental-model-bloque5.md](niagara-mental-model-bloque5.md) | ORD (29 schemes), resolution pipeline, BOG format (handles + LoadOp + atomic writes), BQL / NEQL / Hierarchy / Tag Dictionary |
| 6 | Control Engine | [niagara-mental-model-bloque6.md](niagara-mental-model-bloque6.md) | Execution engine event-driven NO scan cycle, Clock API, BLink + 6 taxonomy, Knobs vs Links, BConversionLink, priority array 16-level, BRelation, kitControl 100+ blocks, ProxyExt, extensions |

### Capa 3 — Integraciones (Bloques 7-9)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 7 | Drivers Framework | [niagara-mental-model-bloque7.md](niagara-mental-model-bloque7.md) | Jerarquía 4 niveles (Container/Network/Device/Point), ProxyExt pipeline 5 fases, comm models (poll/COV/event), tuning policies, BACnet (IP/MSTP/PTP + BBMD + priority array mapping), Modbus/MQTT/LON/KNX/OBIX/SNMP |
| 8 | Alarm + History + Schedule | [niagara-mental-model-bloque8.md](niagara-mental-model-bloque8.md) | Alarm pipeline source→class→recipient + transitions + ack workflow + BAlarmArchive, BHistoryService connection-oriented + Interval/COV extensions + TrendRecord binary + Supervisor collection, Schedule contract isEffective/nextEvent stateless + DFS prioridad + DST |
| 9 | UI Stack | [niagara-mental-model-bloque9.md](niagara-mental-model-bloque9.md) | BWbShell + gx primitivos + Px declarative XML + bajaui widgets + Velocity, BajaScript v2 + ux + hx legacy 4 capas + RequireJS + JxBrowser, Jetty embedded + BWebService + BWebServlet + NiagaraRPC multi-transport + REST Analytics API |

### Capa 4 — Operaciones (Bloques 10-12)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 10 | Platform & Station lifecycle | [niagara-mental-model-bloque10.md](niagara-mental-model-bloque10.md) | Platform daemon niagarad (C/C++ nativo, puerto 5011/HTTPS), Station boot 8 fases (luego 6-phase refinado Bloque 20.3), service dependency resolution, file system !config/!sys/!fox/!file, spy pages, backup/restore `.dist`, Station Copier, commissioning, DR |
| 11 | Auth + RBAC runtime | [niagara-mental-model-bloque11.md](niagara-mental-model-bloque11.md) | BUser/BRole/BCategory 64-bit mask/BPermissions 6-bit rwi+RWI, BUserService + BRoleService, evaluation flow, 9 auth schemes (Digest SCRAM-SHA256, SAML 2.0, LDAP, Kerberos, Cert mTLS, Google TOTP, HTTP Basic), session lifecycle + AutoLogoff + modular enterprise features |
| 12 | Build system + dev lifecycle | [niagara-mental-model-bloque12.md](niagara-mental-model-bloque12.md) | Gradle + niagara-module plugin 7.6.17, tasks (jar/slotomatic/sign/dist/bajadoc/niagaraTest), multi-profile rt/ux/wb, AX→N4 migration + lexicons + BLexicon API + %lexicon(key)% placeholder, TestNG framework, .palette format, help deployment `-doc` profile, popup vs property editors |

### Capa 5 — Gaps profundos (Bloque 13)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 13 | Gaps profundos | [niagara-mental-model-bloque13.md](niagara-mental-model-bloque13.md) | Subscription licensing nCloud + Niagara Network federation Supervisor/Subordinate, Fox wire protocol frames + sensitive data keyring + BOG encryption + virtual components, NiagaraRPC JSON encoding + CSRF, Reports/Search/UxMedia N4.10+ + nav/root schemes |

### Capa 6 — Operaciones avanzadas + Templates + Provisioning (Bloques 14-16)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 14 | Point counting + Templates + Batch Editor | [niagara-mental-model-bloque14.md](niagara-mental-model-bloque14.md) | BIPointCountable rules + license limits runtime (point/device/history/camera/foxStream), federation counting en origen, Niagara Templates core (BTemplateService + .ntpl NO auto-propaga) vs EasyTemplates Honeywell (BEasyTemplatingService + .px/.etso + NEQL), 4 mecanismos coexistentes (Niagara/Easy/Palette/Station), Batch Editor BBatchJobService license provisioning, LON Template/Match/Bind ProgramId 8-byte wildcards |
| 15 | Workbench editing deep | [niagara-mental-model-bloque15.md](niagara-mental-model-bloque15.md) | Wiresheet BWsCanvas + glyph composite hierarchy + BWsAnnotation + state machine 5 states + manhattan routing, Property Sheet BWbComponentView + ComponentTableModel + FieldEditor 3-level resolution facets-driven, Nav tree BNavTree + BINavNode + BFoxProxySession reference counting, Point/Device Manager BFolderManager subclases + Template/Match/Bind integration DevTemplateMgr, workflow 5 fases end-to-end, polling limits empíricos (1-2k @ 1s safe, 5k @ 5s safe, 5k @ 1s marginal) |
| 16 | Analytics Framework + Provisioning Service | [niagara-mental-model-bloque16.md](niagara-mental-model-bloque16.md) | BAnalyticService (com.tridiumx.analytics sin s) + 55+ algorithm blocks (BPsychrometric HVAC + BConsumptionToDemand), NO BRule explícito (pollers BCyclic/BTriggered + BAnalyticAlert implements BIAlarmSource tight/loose coupling), BNaServlet /na Content-Type text/plain + 7 endpoints + subscription TTL 60s, roles NA_API+NA_charts, NO Skyspark connector, BProvisioningService + BNiagaraNetworkJob 2-stage FIXED (Initial + ForEachStation), step types (Backup online excluye .hdb, UpdateLicenses Initial single round-trip, Restore/Copy/Upgrade/Certificate/Report), escalera de escala PropSheet→PointMgr→BatchEditor→StationCopier→ProvisioningCopyStep→BNiagaraNetworkJob |

### Capa 7 — Filesystem forensics + Dev ops + Security operacional (Bloques 17-18)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 17 | Filesystem forensics + JRE embebido | [niagara-mental-model-bloque17.md](niagara-mental-model-bloque17.md) | Install Home completo (bin/ executables 13+ + DLLs + libciper.so ARM 32-bit QNX + ext/ BouncyCastle FIPS/Std + policy/ PKCS7-signed + META-INF/ trust root + modules/ 969 JARs + defaults/ system.properties críticos), User Home (/home/cristian/Niagara4.14/OptimizerSupervisor/ verificado WSL) + Daemon Home (ProgramData Windows), comparativa 3 Homes trust boundary, JRE Azul Zulu JDK 1.8.0_412 x64 + 10 security providers + FIPS mode BCFKS keystore, defaults críticos (moduleVerificationMode=low, circuitMaxReceiveBuffer=10MB, heap 1GB conservador) |
| 18 | Module signing + permissions + CSRF + HELLO/SCRAM | [niagara-mental-model-bloque18.md](niagara-mental-model-bloque18.md) | Plugin `com.tridium:niagara-signing-plugin:1.0.10` única vía standalone, `.sig` 256B raw RSA-2048 (NO PKCS7), cert Honeywell hardcoded signing.properties (year 9999 eternal), bypass matrix (Webs.license developer feature skipModuleValidation=true, Honeywell.license no), corrección Bloque 3.4: 3 groups que SIEMPRE requieren firma son ACCESS_CLASS+REFLECTION+MBEAN_PERMISSION (no BINDING/PRIVILEGE), source `<niagara-permission-groups>` vs runtime `<java-permissions>` formato distinto, CSRF CsrfProtectedFilter + `x-niagara-csrfToken`, HELLO+SCRAM-SHA256 6-step flow via BHttpHeaderCallbackHandler, exemptions.tes TES binary bypass user-level |

### Capa 8 — Drivers verticales + Protocolos wire (Bloque 19)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 19 | LON deep + NRIO + NiagaraDriver + BOX protocol | [niagara-mental-model-bloque19.md](niagara-mental-model-bloque19.md) | LON 6 módulos (lonworks-rt/ux/wb + lonHoneywell + lonSiebe + ascLon), XIF/LNML format ejemplos Rio/Mnlrv3, ProgramId 8-byte breakdown + wildcards (Tridium 80 00, Honeywell 80 00 0c, Siebe 80 00 16), 7 NM verbs hex (0x50 QUERY_DOMAIN..0x70 RESET), SNVT conversions table, throughput TP/FT-10 200 NV/s vs TP/XF-1250 3000 NV/s, NRIO Honeywell RS-485 con unsolicited push + redundancia Pri/Sec, otros drivers Honeywell (honEdgeDriver/honConnectedPower/bport/maxpro/honPlantController libplantctrl.so/honeywellBacnetDeviceManager/honAdvWirelessCfg), NiagaraDriver BNiagaraNetwork + BNiagaraStation + 6 device extensions + 6 Fox channels multiplexados, NO HA nativa (BSupervisorFailover no existe), BOX protocol (Building Object eXchange) verificado WebSocket+JSON distinto Fox, 8 BOX channels, corrección Bloque 9.2: BajaScript browser usa BOX no Fox |

### Capa 9 — Misc residuales + Gap analysis final (Bloque 20)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 20 | BApp + net + BAbstractService + Monitors + JobService + gap analysis | [niagara-mental-model-bloque20.md](niagara-mental-model-bloque20.md) | BApp (NO BAbstractApp) extends BComponent + subclase BWebApp, net-rt centralizado (BInternetAddress + HttpConnection + BHttpProxyService CIDR exclusions), BAbstractService 2 sync callbacks + async futures N4+ + 6-phase boot + 3 fault states, systemMonitor-rt (NO stationMonitor) + 10 monitor classes, EngineManager + LeaseManager (4 tipos leases) + ResourceManager + JMX ports 9010/9011, BJobService sin license vs BBatchJobService provisioning-gated + BJob lifecycle + MonitorWorker 2-sec, persistent policies defaults (History 500 roll, Alarm file ADB sin auto-ack, Audit sin auto-delete, Backup sin retention, Session 5/30s→10s+15min auto-logoff), NO BLoggingService/BDebugService/BLexiconService standalone, **final gap analysis: 27+ áreas sin cubrir** (transaction semantics, clustering HA, perf tuning, enterprise vendor deep, FIPS workflow, federation providers, security rotation, production gotchas) |

### Capa 10 — Semantic / query / presentation layer (Bloques 21-22)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 21 | Tag Framework + Haystack 4 + BQL + NEQL | [niagara-mental-model-bloque21.md](niagara-mental-model-bloque21.md) | BTagDictionaryService (42.4 KB) + BSmartTagDictionary + BTagInfo/BTagGroupInfo/BRelationInfo, Entity wrapper (NO es BComponent), tags NO persiste en BOG (derived on-demand via rules), Haystack 4 defs.json 497 KB + protos.json 194 KB + 3000 defs + Haystack4Importer 10-phase pipeline, 5 namespaces (n hard-coded, hs CSV legacy, hs4 JSON nativo, fc Honeywell FC, hon Honeywell corp; phScience referenced NO shipped), BQL (tokenizer/parser/AST/cursor execution, 163 clases, EBNF con SELECT/FROM/WHERE/ORDER/TOP/DEPTH/STOP, wildcard `*` NO `%`, DISTINCT O(N) memoria, NO GROUP BY, keywords case-INSENSITIVE, identifiers case-SENSITIVE), NEQL (entity space, gramática EBNF 3 statements full/filter/traverse, iteradores DFS EvalOnIterator, scheme `neql:`, NO proyección columnar NO agregación, shortest path ambiguity, 3 niveles cache TagRuleIndex) |
| 22 | PX + BajaUI + BajaScript browser + Bajadoc runtime | [niagara-mental-model-bloque22.md](niagara-mental-model-bloque22.md) | PX format (.px XML v1.0, media="workbench:WbPxMedia" vs "hx:HxPxMedia"), PxDecoder extends XParser + PxEncoder + BPxInclude async load + BNPxInclude Tridium colorize + PxCache LRU + PxIncludeManager, BBinding + BValueBinding/BTableBinding/BFieldEditorBinding + BDegradeBehavior + converters (NO scripting BPxScript NO existe), BWidget 23.2 KB base + jerarquía 100+ widgets (BLabel/BButton/BCheckBox/BRadioButton/BTextField/BTable), layout managers (BGridPane/BBorderPane/BFlowPane/BScrollPane/BSplitPane/BTabbedPane/BCanvasPane), event model Template Method NO listener NO bubbling, Command framework + CommandEvent, Theme 172 clases + Palladium default, bajaui-wb 1.36 MB Swing/AWT vs bajaui-ux 271 KB web adapters BUx* codegen JsInfo, bajaScript-ux 205 JS + bs.built.min.js 360 KB + baja namespace core + Type System + ctypes.js embedded, BOX protocol detalle muxing BoxEnvelope fragmentation 64 KB + BoxMessageRelay debounce 10 ms + handshake HTTP POST /box → WebSocket /wsbox, Subscriber API (changed/added/removed/topicFired/subscribed), Bajadoc @NiagaraType → CommonTypeLibGenerator → TypeSpec JSON embedded ctypes.js, JxBrowser embedded Chromium Workbench con niagara.env.* injections |

### Capa 11 — BACnet deep + Schedule + Control + Migration + Build + Help + Native (Bloques 23-26)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 23 | BACnet deep (objects + properties + stack + COV + Schedule/Calendar/Trend/Access + BBMD + EDE + AlarmRouter) | [niagara-mental-model-bloque23.md](niagara-mental-model-bloque23.md) | BBacnetObjectType 60+ tipos (AI/AO/AV/BI/BO/BV/MI/MO/MV/Schedule/Calendar/Device/Trend/File/Access\*/Pulse/Accumulator), BBacnetPropertyIdentifier 475+ IDs (PRESENT_VALUE 85, STATUS_FLAGS 111, PRIORITY_ARRAY 87, COV_INCREMENT 23, NOTIFICATION_CLASS 17), BBacnetDevice + BBacnetProxyExt 9 sub-states lifecycle COV, priority array 16 levels + relinquish default + BBacnetPriorityValue 13 choices, 37+ confirmed services + 11 unconfirmed, stack BVLC 11 functions (0x81 BACnet IP, 0x04 Forwarded-NPDU, 0x05 Register-FD, 0x09 Distribute-Broadcast, 0x0A/0B Original) + NPDU control octet + HopCount 64 decremento + APDU 8 types + segmentación window SegmentAck + TSM timeouts (APDUTimeout 3000 APDURetries 3), BBacnetScheduleDeviceExt 32.8 KB + ScheduleSupport0/4/16 + BBacnetCalendar + BBacnetNotificationClass + BBacnetTrendLog 8+ variantes + ReadRange + Access\* objects + BACnet/SC TCP 49152 TLS 1.3, BBMD BDT+FDT + Foreign Device registration + link layers (IP UDP 47808, MS/TP RS-485 9600-115200, Ethernet, PTP, SC), EDE CSV format (bacnetEDE-wb 179 KB MasterFile+Units+StateTexts), bacnetAlarmRouter 98.5 KB + BBacnetAlarmClassReassigner + BEscalationFilter, extensiones Honeywell (honBacnetHelper + honBACnetUtilities 40 clases + ObjectSubscriber + PropertyPointAssigner + BHonBacnetNumericOffsetPoint + ascBacnet wizard) |
| 24 | Schedule Framework Niagara-native + driverSchedule cross-driver + kitControl palette 152 components | [niagara-mental-model-bloque24.md](niagara-mental-model-bloque24.md) | BAbstractSchedule API core (isEffective/nextEvent/getOutput/getOutputSource), subclases atómicas (BBoolean/BNumeric/BEnum/BString Schedule + BTime/BDate/BDateRange/BDayOfMonth/BWeekday/BMonth/BYear/BCustom), BCompositeSchedule union=OR/AND + orden adición = prioridad + cache hint, BWeeklySchedule 7 BDailySchedule + BSpecialEvents + effective BDateRangeSchedule + prioridad (In→SpecialEvent→Weekly→defaultOutput), BCalendarSchedule 5 tipos eventos + BWeekAndDaySchedule, BTriggerSchedule event-based (vs continuo Weekly) + trigger/triggerMissed topics + nextTriggerSearchLimit 90d, BScheduleSelector multiplexado + link dinámico, BControlSchedule + Clock subscription + clockChanged + scanLimit 90d, BScheduleSnapshotHandler BSingleton persistencia BOG, BAbstractScheduleView + BHxSpecialEventsView + ViewUtil AllDay/CopyDay/PasteDay, driverSchedule framework (BScheduleDeviceExt + BScheduleExport supervisorId+subordinateVersion BAbsTime + BScheduleImportExt + getExportableSchedule INLINE references obligatorio), implementaciones por driver (BACnet ASN.1 encoding, Niagara-N Fox binary bidireccional, LON custom NV workaround, Modbus holding registers simulado, OPC UA server-dependent), comparativa 5 drivers, versionado clock-based BAbsTime NO checksums + NTP obligatorio, BControlPoint (Numeric/Boolean/Enum/String) + BNumericWritable in1..in16 + BPriorityLevel 16 + fallback + BOverride duration, kitControl palette 152 componentes (Math BQuadMath/BUnaryMath bit ops BMinMaxAvg, Logic BQuadLogic/BComparison, Timer BBooleanDelay Clock$Ticket + BOneShot, Select 10 inputs, Conversion Status↔primitive, Latch memory, HVAC BLoopPoint PID time-driven + BRamp + BSequence + BTstat, Energy BOutsideAirOpt + BOptimizedStartStop + BDemandLimit + BPsychrometric, Alarm algorithms, String BBqlExprComponent), execution change-driven vs time-driven, status propagation flags 7 niveles, ProxyExt pipeline + tuning policy (minWritePeriod + deadband + maxWritePeriod), binding VAV example (Schedule→WritablePoint LEVEL_8→BLoopPoint PID→Ramp→ProxyExt Modbus) |
| 25 | Migration Framework + Bajadoc pipeline + Gradle build + Help system + 198 doc JARs | [niagara-mental-model-bloque25.md](niagara-mental-model-bloque25.md) | migration-rt core (BIFileMigrator + MigratorRegistry 4 matching modes + BIBogElementConverter + ConverterRegistry hierarchy walk + BModuleRemovalConverter auto-gen), n4mig.exe Assembly Tridium.Niagara.MigrateAXtoN4 (AX-3.8 backup .dist → Workbench User Home + recursive migrate + BOG XML walk + ConverterRegistry per typeSpec), N4.x→N4.x+1 auto startup via BOG sourceBajaVersion, migradores por archivo (BackupDistMigrator/BogMigrator/PxMigrator/AlarmDbMigrator/HistoryDbMigrator/KeytabMigrator/ProvisioningNiagaraMigrator), driver-specific (bacnetMigrator-wb WsToAws, modbusTcpSlaveMigrator, spyderToIrmNxMigrator con 6 converters, honPlantControllerMigrator v1.2.5 GUI, propMigration-wb BPropNameChange+BNewType+BPropFlagsChange+BSimpleEncodingChange), migrator-wb 15+ clases (BBogMigrator motor, MigratorOrdConverter, BUserConverter AX permissions→N4 roles, BProgramConverter), conversion/ 70 MB .dist files + cleanDist/ pristine templates + sw/ versioned migrators, downgrade N4→AX destroys data, breaking changes (BICollection→BITable, getProgram removed, BAlarmService.getOpenAlarms→AlarmSpaceConnection, User.permissions→roles), Bajadoc pipeline (source Java @NiagaraType → niagara-baja-doclet 1.0.9 → .bajadoc XML v2.0 con createdBy/createdAt/createdOn hostname → preJarCopy HtmlDocAction via toc.xml → createIndex SearchBuilder JavaExec .dat → jar 18.7 MB firmado), formato .bajadoc (class/extends/implements/action/property/topic/tag elements), gradle (niagara-module-plugin 3.0.18 + niagara-signing-plugin 1.0.10 + niagara-rjs-plugin 2.0.4 + docmodule.gradle 304 líneas BajadocSpec DSL + task dependency graph compileJava→generateBajadoc→preJarCopy→createIndex→jar), module.xml auto-generated from @NiagaraType reflection + buildMillis Unix ms + buildHost, docDeveloper-doc.jar (META-INF firmado + doc/ 169 module folders + doc/jsdoc/ 26 MB bajaScript/bajaui), help-wb.jar 215 KB (HelpSystem + SearchLoader lazy + Searcher 14.7 KB + BajadocParser 31 KB + BBajadocViewer + BHelpSideBar + BBajadocServletView + 35 dependencies), search-rt/wb/ux NO Lucene custom full-text, niagara-help/ extracción 950 MB (bajadoc/141MB + bajadoc-clean/13MB + devguide/115MB + devguide-clean/5.5MB + guides/491MB + guides-clean/31MB + docs-text/44MB + source/35MB + jdk/59MB + indexes/ 17 MB 10 JSON + tools/ 852 KB Python CLI), 10 JSON indexes (class-index 2,712 clases + inheritance + method-index 19,527 names + devguide-toc 109 guides + guides-index 98 folders + pdf-index 364 PDFs + source-index + xref-index 17,690 edges + slots-index 3,269 props/404 actions/68 topics + call-index 138,875 entries), 198 doc JARs (cl\* Honeywell commercial + doc\* Tridium base + docHoneywell\* specific), documentación Honeywell layer (Optimizer Supervisor datasheets NA/EU + 250+ HTML Honeywell/ + 145 HoneywellSpyder + 120 HoneywellSylkDevice + clHVAC 8 variantes + clCBus 8.4 MB), render Workbench via BWebBrowser Jetty wrapper NO navegador externo |
| 26 | NRE launcher C++ + 138 DLLs nativas + standalone module signing playbook | [niagara-mental-model-bloque26.md](niagara-mental-model-bloque26.md) | NRE dual-layer architecture (Thin native launcher PE32+ ~50 KB → njre.dll 69 KB JVM loader → nre.dll 115 KB classpath+flags → JNI_CreateJavaVM → JVM Azul Zulu 1.8.0.412.20 Win x64), bin/ inventario (nre.exe/station.exe/wb.exe 108 KB/niagarad.exe service wrapper/n4mig.exe/plat.exe/nverify.exe 517 KB signature verification/console.exe/hdbt.exe/dataExportTool.exe 75 MB + DLLs common.dll 189 KB/cppunit.dll 200 KB/msvcp140 549 KB/msvcr120 932 KB legacy/vcruntime140 + protocol drivers opc.dll 176 KB/opccomn_ps/opcproxy/lon.dll 35 KB/pcapBacEther.dll 27 KB WinPcap deprecated/honImport 59 KB/trayIcon 194 KB/alarmDialog 24 KB/dsfspi 359 KB + libciper.so 123 KB ELF ARM EABI5 debug symbols retained con 40+ JNI_com_honeywell_comm_JNIRequest_\*), entry points (wb→BWorkbench, station→BStation, niagarad→NiagaraDaemon, n4mig→Migration, plat→Installer, console→NiagaraConsole), nre.dll inyecta flags (java.protocol.handler.pkgs com.tridium.nre.protocol + java.library.path + java.security.manager + niagara.home/user.home/home.url file:/// + platform.provider + supported/required.runtime.profiles) + nre.properties per-exe (station/wb/test/nre.java.options -Xss512K -Xmx1024M), niagaraHome resolution (env → registry HKLM\Software\Honeywell\Niagara\<version> → relative ../), debug mode `set nre_debug=1` prefix "nre>", system.properties + niagara.moduleVerificationMode modes (low N4.14 default/medium futuro N4.9+/high futuro), bin/ext/ JARs (core nre.jar+niagarad.jar, BouncyCastle FIPS bcfips 4 JARs + bcstd, Jetty 9.4.54, okhttp 4.12, slf4j 2.0.9, Kotlin 1.9.10, asm 9.6, orientdb 3.2.23, jose4j 0.9.6, libthrift 0.17, JxBrowser 7.39.0 Win64/Swing/SWT/JavaFX), **138 artefactos nativos catálogo** (117 PE32+ x64 + 4 PE32 x86 .NET printout + 1 ELF ARM EABI5 libciper + 8+ embedded JARs) + **MSVC runtime multiplexing** (ucrtbase 1.1 MB real impl + MSVC 14.0 VCRUNTIME140/MSVCP140 + MSVC 12.0 MSVCR120 legacy + 48× api-ms-win-crt-\*.dll forwarders) + FFmpeg embedded ffmpeg-wb.jar 34.9 MB (avcodec-60 18.4 MB + avformat + avfilter + swscale + ffmpeg-wrapper.dll JNI) + **JxBrowser 7.39.0 Chromium 289 MB** (chrome.dll 260 MB + libEGL + libGLESv2 7.6 MB + vk_swiftshader 5.1 MB + d3dcompiler_47 4.7 MB) + AWT Tier 1 (awt+jawt+freetype) + JavaFX Tier 2 (glass+jfxwebkit 75 MB + prism_d3d/es2/sw) + crypto native providers (sunec+sunmscapi+j2pkcs11+j2pcsc+j2gss+w2k_lsa_auth NO OpenSSL embebido) + Office Interop /printout/ PE32 x86 .NET (Word.dll Word 2003 PIA + WireSheetControl Excel) + JVM debug (jdwp+dt_socket+dt_shmem+hprof+instrument) + Accessibility bridges MSAA/JAB, Java 2 SecurityManager policy grants per-codeBase (nre.jar propertyPermissions + niagarad.jar FilePermission + sun.misc + NreSupplierPermission), install-data/ (install.properties shortcuts wb_w.exe/plat.exe/console + EULA + splash BMP), **Playbook operacional firma standalone 10 pasos**: (1) createProfile XML config + (2) edit dname/keyalg RSA/keysize 2048/TSA digicert + (3) generateCertificate password 10+ chars + (4 alt keytool legacy) + (5) build.gradle niagaraSigning aliases+signingProfileFile + (6) gradlew clean build → .sig 256 bytes exactos + (7) exportCertificate PEM + (8) deploy cp jar+sig a modules/ + (9) import User Trust Store si modo MEDIUM/HIGH + (10) verify jarsigner/nverify/Module Info verde, troubleshooting 9 casos + configuración avanzada (múltiples certs + CA-signed CSR flow + HSM PKCS11 Thales/YubiKey + re-firma existing), 3 groups SIEMPRE requieren firma Tridium (ACCESS_CLASS+REFLECTION+MBEAN_PERMISSION) incluso en LOW, security surface risks (FFmpeg CVE rate + JxBrowser independent patching + WinPcap deprecated + MSVC runtime boundary + Accessibility UI spoofing) |

### Capa 12 — Network + Discovery + Virtual + Web tier operacional (Bloques 27-29)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 27 | Network surface + puertos + certManagement end-to-end | [niagara-mental-model-bloque27.md](niagara-mental-model-bloque27.md) | Catálogo 22 puertos (Platform 3011/5011, Fox 1911/4911, Jetty 80/443, BACnet 47808/49152, JMX 9010/9011, BOX multiplexado, LON/IP 1628, SNMP 161/162, Modbus 502, OPC UA 4840/4843), firewall matrix por escenario (Supervisor↔Sub, Workbench→Station, Station→BACnet, Station→nCloud, commissioning), TLS boundaries + cipher suites + OCSP/CRL NO validados (gap compliance), BServerPort tipo unificador 4 slots (public/local/proto/adapter) permite NAT/port forwarding interno, **4 trust stores** (user/system/daemon + `userUntrustedStore` quarantine único industria), 7 tipos cert matriz (signing Honeywell year 9999 eternal + SSL Jetty + Fox + Platform + User client + BACnet/SC + NiagaraNetwork federation) con keystore/validez/rotación/impacto expiry, cert lifecycle flows (creación Workbench vs keytool vs BCertManagerService API, CSR+CA signing DigiCert, rotación sin downtime cross-sign, revocation CRL silent, FIPS BCFKS obligatorio), `.certificate` XML custom DSA Tridium/Honeywell desde 2003 con `expiration="never"`, Signing Service enterprise centralizado (`niagara.signingRequester.*` retry 6h approval + 30m results para fleet 1000+ JACEs), **`BHeaderAuthenticationScheme` NO EXISTE** (Niagara NO soporta nativamente SSO via X-Forwarded-User reverse proxy — gap documentado), cookies Niagara 7 types + SameSite=None requires Secure browsers modernos, session fixation protection regenerate ID post-auth, listener lockdown recipe `!config/network/*.bog` + `webOutbound/foxOutbound` CIDR allow/deny, **3011 HTTP plain abierto default post-install N4.14** requiere `sslOnly=true` manual, `Webs.license` (16KB) tiene `skipModuleValidation` feature — `Honeywell.license` (2.3KB) NO (matriz bypass cross-license), `org.bouncycastle.jsse.client.assumeOriginalHostName=true` obligatorio para FoxS tras reverse proxy SNI, commissioning flow end-to-end puertos+certs exchange |
| 28 | Discovery framework cross-protocol + Virtual Components layer | [niagara-mental-model-bloque28.md](niagara-mental-model-bloque28.md) | **NO existe `BDiscoveryJob` base abstracto** — discovery es UI-driven (`BLearnTable` + `MgrController$Discover` + `BAbstractManager` en `workbench-wb.jar`) NO server-driven, cada driver extiende `BSimpleJob` directamente (expectativa arquitectónica invertida), Discovery framework genérico lifecycle broadcast/probe→collect→match/filter→add/bind con topology variants (gateway-based BACnet vs master-slave Modbus vs unsolicited push NRIO vs catalog-based LON XIF), 6 drivers cross-protocol deep: **BACnet** 10-step flow WhoIs→IAm→object-list→property-list per object + BBacnetLocalDevice$Discover + segmentación + BBMD routing, **LON** Query_Id(0x51)+Query_Neuron_ID→NV enumeration→SNVT mapping→XIF download + LonworksDevice BUncommissionedDevice→BCommissionedDevice + Neuron-ID conflict, **Niagara Fox federation** BNiagaraStationLearn Supervisor probing subordinate point space + browsing remote BComponent tree + Fox session folder-by-folder + BFoxProxySession reference counting, **Modbus NO tiene discovery API** (confirmado empíricamente grep modbusCore/Async/Tcp — excepción que confirma la regla, framework no fuerza discovery si protocolo no soporta enumeration), **SNMP** BSnmpNetwork$Discover walking OID subtree + MIB loading .mib parse (sin v3 en distro Honeywell + sin GetBulk optimization — operational gap), **OPC UA** BOpcUaClient$Browse Address Space + BOpcUaNodeLearnEntry 51 KB clase más grande de cualquier discovery driver + 12 NodeClass variants + endpoint discovery (GetEndpoints SecurityPolicy matching), Template/Match/Bind meta-flow cross-protocol (BDeviceTemplateManager.match() post-discovery → auto-bind, LON ProgramId 8-byte wildcards), **Virtual Components layer**: `niagaraVirtual-rt.jar` 317 KB + 66 clases módulo first-class dedicado (Bloque 13 lo subestimó) + 12 subclases control points + schedules + stubs, BVirtualComponent extends BComponent NO persisted BOG (derived on-demand parent genera dinámicamente), virtual: scheme + VirtualPath resolution + BVirtualGateway pattern + VirtualCacheCallbacks tier eviction, virtual points en drivers (BACnet calculados, Modbus composite registers, fórmulas/agregación/scaling dinámico), **Virtual points NO cuentan license count** — BPointCountVisitor detecta BVirtualComponentSpace y skip (confirmado spy page, tradeoff estratégico Supervisor-Subordinate), 15 gotchas verificados (virtual crash regenera race, Fox/BOX subscription cache, debugging virtual "fantasma"), mental model pipeline Discovery→Template→Virtual end-to-end |
| 29 | Web tier + Servlets + Jetty filter chain + REST endpoints matrix | [niagara-mental-model-bloque29.md](niagara-mental-model-bloque29.md) | Jetty 9.4.54.v20240208 embedded (**EOL 2025 — riesgo CVE unpatched**) vía BJettyWebServer con 40+ inner classes, thread pool + connector sizing + acceptor queue, 6 protection layers off-by-default (DoS/QoS/ConnectionLimit/AcceptRateLimit/InetAccessHandler/SizeLimitHandler), BWebServlet registry descubrimiento dinámico via BComponent tree **NO `@WebServlet` ni `web.xml`** (path routing `/<servletName>/*`), **filter chain 15 capas** con orden real extraído de `configureNiagaraWebApp()` bytecode (invariant: Auth ANTES CSRF), **53 servlets inventariados** scan de 974 JARs (Bloque 9 mencionaba ~6): 20 core Tridium (OrdServlet/LoginServlet/LogoutServlet/PreloginServlet/FileServlet/WebStartServlet/WbServlet/SpyServlet/SpeedTestServlet/RequireJsConfigServlet/SessionTimeoutServlet/CspReportServlet/ClientEnvServlet/DefaultServlet/NiagaraRpcServlet/SecurityCheckServlet/UnauthenticatedServlet/ViewAllOrdServlet/LoginFileServlet/LogoutConfirmServlet) + BOX (BBoxServlet/BoxWebSocketServlet/QueryServlet) + bajaux WbWebWidgetServlet + Analytics 3 + Hierarchy + WebChart 3 + SAML 4 + ClientCert 1 + BACnet/SC WS 1 + Honeywell verticals (Galileo SignalR 4 transports, AWS BACnet, Plant Controller) + ~35 custom sejofa, **NiagaraHttpSession** agrega múltiples HttpSession Jetty por `superId` (colección NO singular) + regeneración post-auth (session fixation safe) + 7 cookies Niagara + `cacheSessionsAndRestart` action preserva sessions en config restart, 9 auth schemes tabla comparativa (SCRAM-SHA256 HELLO Bloque 18 / Basic legacy / Digest legacy / Header SSO / SAML RP+IdP / Kerberos SPNEGO / LDAP bind / Google TOTP / mTLS client cert), NiagaraRPC JSON-RPC 2.0 regex `/([^/]+)/(.+)` + 6 handlers built-in + error codes -32700..-32000 + batch support + multi-transport HTTP/Fox/BOX, WebSocket BOX handshake + 5 system props config + frame limits 64KB, static resource serving `/baja/*` + `/module/<mod>/rc/*` + Cache-Control+ETag+If-Modified-Since, reverse proxy X-Forwarded-For/Proto + BHttpProxyService CIDR, **17 gotchas productivos**: SameSite=None+Secure, worker≠engine thread (Bloque 9.3.2 expandido con workaround concreto post/get), filter order invariant, requestHeaderSize 8KB default (Bearer token overflow), Jetty 9.4 EOL 2025, Content-Type text/plain peculiaridad cross-servlets (no solo BNaServlet Bloque 16.5.1), max request body multipart upload, WS ping/pong timeout, keepalive vs load balancer idle conflict, request flow end-to-end con latency breakdown estimado 20-525ms |

### Capa 13 — Cierre gaps residuales: Enterprise auth + FIPS + Performance + Honeywell runtime (Bloques 30-32)

| # | Bloque | Archivo | Key topics |
|---|--------|---------|------------|
| 30 | Enterprise auth federation + FIPS + key rotation + token matrix | [niagara-mental-model-bloque30.md](niagara-mental-model-bloque30.md) | LDAP v2/v3 + AD via `BLdapAuthenticationScheme` (keytab-encrypted bind creds + connection pool + referral handling + group mapping via prototype BComponent), SAML dual mode (Niagara como RP Y como IdP — **SLO NO implementado gap compliance**, attribute mapping circle of trust), **OAuth/OIDC NO soportado nativo** (`oauth2-rt` es cliente M2M only — NO authentication scheme, forzar SAML o custom module), Kerberos SPNEGO + keytab + ticket cache, mTLS clientCertAuth-rt cert-to-user mapping + **CRL NO validada** (confirma Bloque 27.3), **CORRECCIÓN mayor keyring**: Niagara NO usa `master.jceks` sino **`.km`/`.kr` DPAPI Windows** para sensitive data encryption + BPasswords reversibles via DPAPI OS-level (invalida supuesto Bloque 13.2.4) — rotation manual via procedure documentado, **FIPS 140 migration workflow 10 pasos**: BCFIPS providers + BCFKS keystore + `java.security` edits + cipher exclusions + `moduleVerificationMode=medium/high` + self-tests startup + gotcha módulos pre-FIPS fail load, **TLS cert rotation zero-downtime**: Jetty + Fox hot reload, BACnet/SC requiere restart, cross-sign transition, token lifecycle matrix 12 tokens (Fox 24h + BOX BEARER + CSRF + Web cookie + SAML assertion + OAuth access + LDAP bind + Keytab + mTLS + DPAPI keys + .sig signing cert + Trust store aliases), RBAC method-level (slots interceptados + Java methods NO + reflection protegido Bloque 3), Auditor flow **sincrónico inline** (NO async — el queue unbounded Bloque 31.8 aplica a syslog side-channel NO main sink), **`NSuperSession` aggregator** agrega HTTP+Fox+BOX bajo un BUser + invariant **1 concurrent session per user** (NO configurable) + regen post-auth session-fixation safe, session timeout clock skew multi-server (gap #25 documentado risk sin lab), federation cross-station identity propagation gap, break-glass local admin siempre disponible, gotchas 10+ (master keyring corrupt → BPassword silent empty, FIPS breaks pre-FIPS modules, LDAP groups NO refresh hasta re-login, SAML clock skew reject silent, keytab rekey invalida tickets activos, mTLS self-signed accepted default) |
| 31 | Performance tuning + observability profunda + thread pools + audit + heap scale | [niagara-mental-model-bloque31.md](niagara-mental-model-bloque31.md) | Engine thread único revisited (queue unbounded + `HogsPage` 5 bandas severidad + async `Flags.ASYNC` pool), **tabla maestra 21 thread pools**: Engine (1 fijo) + Jetty worker + Fox sessions + Fox channels + BOX worker + BJobService ForkJoinPool (common pool CPU cores) + BMonitorWorker 2-sec + History archive + Alarm dispatch + NetworkManager polling per-driver + Subscription processor + Servlet executor + Virtual cache + 8 más, **GC default JDK 8 Azul Zulu = ParallelGC** (NO G1GC — confirmado empírico, `nre.properties` SIN flag `-XX:+UseG1GC`), recomendaciones G1GC para heaps 4GB+ + YoungGen/OldGen ratio + Metaspace + GC log + heap dump OOM, I/O buffering (único `circuitMaxReceiveBuffer=10MB` uncommented en 589 líneas system.properties + BOX envelope 256KB + Jetty 8KB header + NIO vs BIO), JMX completo (MBeans JVM standard + `com.tridium.*` inferidos + remote 9010/9011 + jconsole/VisualVM/jcmd/JFR workflow), **Niagara NO usa `ManagedBlocker`** confirmado decompilación → ForkJoinPool starvation real risk con blocking BJobs (long-running job bloquea common pool — workaround dedicated thread pool I/O-bound), **history archive blocking 5-30 min** (`lingerTime=5min` + cursor `inactivityTimeout=2min` + SQLite `.hdb` VACUUM lock contention UI queries + schedule off-peak), **audit queue SYSLOG side-channel `LinkedBlockingQueue` UNBOUNDED** → overflow silent loss vía OOM cascade (main sink inline sync confirmado Bloque 30 — corrección), job exception handling (framework catch + JobLog TRANSIENT + NO persisted stack traces + manual restart), backup monolítico sin chunking/resume (gap #23 documentado risk), TimeZone per-history `BHistoryConfig.timeZone` + DST + Supervisor cross-zone aggregation gotcha, audit retention sin auto-delete → disk fill risk + OS logrotate NO aplica (audit.adb SQLite nativo), subscription perf tiers deployment (1k/5k/20k points), **tabla heap tuning XS/Small/Medium/Large/XL** con flags JVM exactas (Small 1GB default + Medium 2-4GB + Large 4-8GB + XL 8-16GB para Supervisor 50+ subs), profiling producción (JFR record jcmd + jmap heap + jstack thread + `EngineManager$HogsPage` + spy pages + access log), **22 gotchas prod consolidados** (G1GC pauses 50ms+ miss callback, FJP saturated, history compaction timeouts, audit silent loss, backup monolítico timeout, TimeZone aggregation wrong, disk fill, heap fragmentation 24x7, Metaspace leak class reload), **playbook diagnóstico 8 pasos** UI lenta → HogsPage → thread dump → JMX Memory → GC log → heap histo → fix + verify JFR |
| 32 | Honeywell enterprise modules + SMA + non-HTTP transports + runtime semantics | [niagara-mental-model-bloque32.md](niagara-mental-model-bloque32.md) | **`honPlantController-rt.jar` SÍ contiene JNI** (`com/honeywell/comm/JNIRequest.class` presente — `libplantctrl.so` NO distribuida en Supervisor Windows, solo `libciper.so`; vive en CIPer/JACE ARM), **honPlantController trae BTP + RSTP stacks completos**: BTP (Building Technology Protocol parser propio Honeywell — ReadProperty/WriteProperty/Query/FileData/Subscription/Discovery) + RSTP (Rapid Spanning Tree) → **soporta topología Ethernet ring redundante** hallazgo operacional no documentado previamente, **`jsonToolkit` NO es custom Tridium** — re-empaqueta Jayway JsonPath (Apache 2.0 OSS) + json-smart backend + adapter `com.tridiumx.jsonToolkit` con `x` = partner convention (324 clases), **`honPlantController` embeds Google Gson standalone** — shaded dependency pattern (cada módulo Honeywell maneja su propio JSON stack), **`platPower` 16 clases reveladas**: JACE SLA + QNX (`PowerdQnx`) + Javelina + NPM + Dual Battery + NiMH + UPS + External SLA (platform-embedded-specific, NO para supervisor Windows), **`honBacnetHelper` FastAccessList optimization**: batch multi-property reads en single APDU → crítico scaling 475K points deployment, **NO existe módulo `optimizer-*.jar`** — "Optimizer Supervisor" es naming comercial, funcionalidad dispersa en `clHVAC*` suite (Bloque 25 mencionó 8 variantes + 8.4 MB clCBus), SMA flow (`feature.sma.expiration` XML attribute + `sma.exempt="true"` Bloque 14.3 + grace period + nCloud update checker — SMA expired → update fail silent), `fox.sys` system channels vs user channels (boot/commissioning/config sync vs BQL/subscription), `ndriver` package NiagaraDriver internal transport, **non-HTTP transports consolidado 21-transport table** (Serial RS-232/485 + UDP BACnet/SNMP/BBMD + Raw TCP Modbus/BACnet-SC/OPC UA + Proprietary BTP/RSTP/BOX/Fox), Transaction semantics multi-step (**no hay `BTransaction` real** — compensation actions manual + BOG save atomic via BLoadOp), Module lifecycle hooks (`BModule` + classloader init + `Sys.loadType` registration + service dependency resolution + module.xml `<depends>`), `Sys.loadType()` type registry algorithm + per-module ClassLoader parent-first delegation + shared baja.jar bootclasspath + ClassLoader leak risk (old not GC'd common JVM issue), BOG schema evolution (`BOnMissingType` stub inferido + extra properties preserved + forward compat NO supported), honBacnetHelper top 5 clases (FastAccessList/ObjectSubscriber/PropertyPointAssigner/NumericOffsetPoint/Utilities), gotchas Honeywell (ARM-only libplantctrl.so, SMA expiration update fail, jsonToolkit OSS edge cases, honBacnetHelper escala 475K, CIPer ring topology dependency), TODOs honestos (NO decompilé con javap -p clases internas BTransaction/Sys.loadType/BModule/BOnMissingType/fox.sys constants/license-rt.jar SMA methods, NO analicé honMqttDriver interno, SMA.exempt XML license NO confirmado), mental model Honeywell layered stack: N4.14 base → Honeywell overlay (license + 250+ HTML docs + hon*/asc*/cl*) → Optimizer Supervisor naming comercial → SEJOFA customer |

---

## Cómo leer este mental model

### Si sos **nuevo a Niagara**

Leé en orden: 1 → 2 → 3 → 4 → 5. Los primeros 5 bloques te dan la base conceptual completa. Podés profundizar en 6-20 según necesidad del proyecto.

### Si venís a **implementar un driver**

Ruta: 4 (slots) → 5 (ORD) → 6 (ControlPoint + ProxyExt) → **7 (driver framework)** → 8 (extensions) → 14.10 (device templates) → **19 (LON/NRIO/otros drivers Honeywell)**.

### Si venís a **implementar una UI**

Ruta: 4 (BComponent) → 5 (ORD + BOG) → 6 (control) → **9 (UI stack completo)** → 13.3 (NiagaraRPC + UxMedia) → **15 (Workbench editing deep)** → 19.17 (BOX protocol para BajaScript browser).

### Si venís a **investigar seguridad**

Ruta: **3 (sandbox JVM)** → 11 (RBAC user/role/permission) → 9.3 (web auth) → 13.2 (Fox wire + sensitive data + keyring) → **17 (filesystem security + 3 Homes trust boundary + JRE)** → **18 (module signing + permissions real + CSRF + HELLO/SCRAM + exemptions.tes)** → **27 (network surface + cert types matrix + trust chain + lockdown recipe)** → **29 (web tier filter chain 15 capas + auth schemes tabla + session fixation)**.

### Si venís a **hacer hardening de red + certificados**

Ruta: **27.1 (catálogo 22 puertos)** → **27.2 (firewall matrix escenarios)** → **27.3 (TLS/cipher suites + OCSP gap)** → **27.4-27.7 (certManagement end-to-end + 7 tipos cert matriz + lifecycle)** → **27.10 (listener lockdown recipe)** → **27.11 (gotchas producción)** → **29.3 (filter chain orden crítico)**.

### Si venís a **integrar un driver nuevo o extender discovery**

Ruta: 7 (drivers framework base) → **28.1 (modelo genérico UI-driven)** → **28.2-28.7 (6 drivers comparativa empírica)** → **28.8 (Template/Match/Bind meta-flow)** → 14 (templates) → 19 (LON/NRIO/NiagaraDriver ejemplos reales).

### Si venís a **usar virtual components + puntos calculados**

Ruta: 4 (BComponent base) → 5.1 (ORD schemes) → **28.9-28.13 (BVirtualComponent + virtual: scheme + BVirtualGateway + niagaraVirtual-rt módulo 66 clases + licensing exclusion)** → 24 (control binding) → 14.1 (point counting).

### Si venís a **exponer REST / crear servlet / integrar externo**

Ruta: 9 (UI stack + BWebService overview) → **29.1 (Jetty embedding)** → **29.2 (BWebServlet registry dinámico)** → **29.3 (filter chain 15 capas)** → **29.4 (matriz 53 servlets)** → **29.5 (session lifecycle)** → **29.6 (9 auth schemes)** → **29.9 (NiagaraRPC JSON-RPC)** → **29.10 (WebSocket BOX/Fox)** → 18 (CSRF + SCRAM).

### Si venís a **integrar SSO corporate (LDAP/SAML)**

Ruta: 11 (RBAC base) → **30.1 (LDAP deep + AD + keytab + group→role mapping)** → **30.2 (SAML dual RP/IdP + SLO gap)** → **30.3 (OAuth/OIDC NO soportado nativo)** → **30.4-30.5 (Kerberos + mTLS)** → **30.13 (NSuperSession aggregator + 1-per-user invariant)** → **30.14 (fallback patterns)** → 27.8 (HeaderAuth NO existe + cookie) → 29.6 (filter chain integration).

### Si venís a **FIPS migration + key rotation**

Ruta: 17 (BCFKS + FIPS providers base) → **30.6 (FIPS workflow 10 pasos + cipher restrictions)** → **30.7 (.km/.kr DPAPI — NO master.jceks — rotation procedure)** → **30.8 (TLS cert rotation zero-downtime por endpoint)** → **30.9 (token lifecycle matrix 12 tokens)** → 27.6 (cert types matrix).

### Si venís a **tuning performance + diagnosticar station lenta**

Ruta: 6.1 (engine thread único) → **31.1 (engine revisited + HogsPage 5 bandas)** → **31.2 (tabla 21 thread pools)** → **31.3 (GC tuning — ParallelGC default NO G1GC)** → **31.6 (ForkJoinPool sin ManagedBlocker starvation)** → **31.7 (history archive blocking)** → **31.14 (heap tuning XS→XL flags)** → **31.17 (playbook diagnóstico 8 pasos)**.

### Si venís a **deploy production-ready con observability**

Ruta: 31.2 (thread pools baseline) → 31.3-31.5 (GC + I/O + JMX) → **31.8 (audit queue unbounded syslog)** → 31.10-31.12 (backup + TimeZone + retention gotchas) → **31.16 (22 gotchas prod consolidados)** → 20.8 (persistent policies defaults).

### Si venís a **desarrollar módulo Honeywell-specific o extender runtime**

Ruta: 1 (framework + module.xml) → 4 (Baja object) → **32.10 (Module lifecycle hooks + Sys.loadType)** → **32.11-32.12 (type registry + ClassLoader isolation)** → **32.13 (BOG schema evolution + BOnMissingType)** → **32.1-32.4 (inventory Honeywell modules empírico)** → **32.5 (SMA flow)** → 12 (gradle build) → 26 (native libs).

### Si venís a **operar/administrar**

Ruta: 1 → 2 (licensing) → **10 (platform+station)** → 11.3 (session) → 12 (build) → 13.1 (Niagara Network) → **14 (point counting + templates)** → **15 (Workbench workflow end-to-end)** → **16 (Analytics + Provisioning Supervisor)** → **20 (monitors + persistent policies defaults)**.

### Si venís a **federar multi-station**

Ruta: 13.1 (Niagara Network) → 13.2 (Fox wire) → **16.9-16.18 (Provisioning Service BNiagaraNetworkJob 2-stage)** → **19.11 (NiagaraDriver BNiagaraNetwork + 6 device extensions + 6 Fox channels)** → 19.14 (NO HA nativa gap).

### Si venís a **analytics + dashboards**

Ruta: 5.3 (BQL/NEQL) → 8.2 (History) → 9.3.7 (Analytics Web API overview) → **16.1-16.8 (Analytics Framework deep — BAnalyticService + 55 blocks + BAnalyticAlert BIAlarmSource + BNaServlet)**.

### Si venís a **debuggear problemas**

Empezá por los gotchas transversales (sección siguiente), después buscá el bloque específico. Para performance: **20.5.1 EngineManager $HogsPage** es el primer stop.

---

## Gotchas transversales — por frecuencia

Los gotchas más críticos repetidos o conectados entre bloques:

### Concurrency y threading

- **Bloque 6.1.5**: engine thread único — callback lento congela station.
- **Bloque 6.1.6**: NO topology sort en links — stack overflow en loops recursivos. Mitigar `Flags.ASYNC`.
- **Bloque 6.2.7**: feedback loops A→B→A no detectados globalmente. Solo self-link.
- **Bloque 9.3.2**: Jetty worker thread ≠ engine thread. Llamar `.get()` en servlet = deadlock. Usar FOX/BOX calls o `post()`.
- **Bloque 4.3.3**: re-entrar `.add()` dentro de `added()` callback = deadlock.
- **Bloque 15.13.1**: **last write wins** sin merge — 2 Workbench concurrentes editando sobrescriben silent.
- **Bloque 20.5.1**: `EngineManager$HogsPage` es tool debugging para identificar callbacks pesados.

### Persistencia y encryption

- **Bloque 5.2.2**: keySource `none` portable sin passwords reversibles; `keyring` seguro host-específico; `external` portable con passphrase.
- **Bloque 13.2.4**: master.jceks no accesible → BPassword reversibles empty silencioso. Debugging opaco.
- **Bloque 10.3.3**: online backup excluye `.hdb`/`.adb` — necesitás offline para integridad completa. Confirmado en Bloque 16.11.1 (BProvisioningBackupStep).
- **Bloque 5.2.7**: LoadOp corre en engine thread — heavy deserialización bloquea callbacks.
- **Bloque 14.6.3**: Niagara Templates NO auto-propagan a instances cuando cambia template source. Requiere `BUpgradeTemplateJob` explícito.
- **Bloque 20.7.3**: BJob NO persistido BOG default — crash mid-job = job perdido (excepto BStationSaveJob).

### Security / Auth

- **Bloque 3.3.4 + 18.4.4**: los 3 groups que SIEMPRE requieren firma son **ACCESS_CLASS + REFLECTION + MBEAN_PERMISSION** (NO "BINDING"/"PRIVILEGE" como decía originalmente).
- **Bloque 3.2**: 3 archivos `bin/policy/` firmados PKCS7 — modificar rompe integridad.
- **Bloque 10.1.2**: platform credentials (5011) ≠ station BUser (FOX). Dos cuentas separadas.
- **Bloque 11.3.4 + 20.8.5**: default lockout 5 failures/30s → 10 seg + session 15 min auto-logoff. Configurable.
- **Bloque 11.3.5**: password complexity NO enforcement nativo.
- **Bloque 13.2.3**: FOX session 24h — re-auth silent cert-based.
- **Bloque 17.6**: `moduleVerificationMode=low` hardcoded en defaults/system.properties — base del bypass.
- **Bloque 18.3.2**: `skipModuleValidation` requires AND (flag + license feature). Webs.license tiene, Honeywell.license NO.
- **Bloque 18.9**: `exemptions.tes` user-level — puerta trasera oficial menos invasiva que flags JVM.

### Scale y performance

- **Bloque 5.3.5**: Hierarchy service caro (NEQL per nivel × N). Cachear.
- **Bloque 5.3.5**: NEQL NO aggregate functions. Para stats combinar con BQL.
- **Bloque 8.2.8**: HistorySpaceConnection AutoCloseable — try-with-resources obligatorio o DB locked.
- **Bloque 13.1.7 + 19.13**: Supervisor bottleneck ~50 subordinados. FOX broker saturation + history import contention. Confirmado NiagaraDriver.
- **Bloque 13.2.5 + 19.11.3**: FOX channel exhaustion ~1000/session. Leaks bloquean (station restart required para cleanup).
- **Bloque 15.14.1**: polling limits empíricos: 1-2k puntos @ 1s SAFE, 5k @ 5s SAFE, 5k @ 1s MARGINAL.
- **Bloque 17.5.5**: heap default `-Xmx1024M` conservador. Explica Supervisor bottleneck.
- **Bloque 19.6.3**: LON max ~32K devices/domain (254 subnets × 127 nodes).
- **Bloque 20.8.4**: Backup restore sin chunking/resume — timeouts en high-latency.

### Licensing

- **Bloque 2.1**: SMA es atributo de feature, no feature propia.
- **Bloque 2.6 + 18.3.2**: `Feature.getb("skipModuleValidation", false)` license-gated bypass.
- **Bloque 13.1.1**: Subscription grace silencioso si nCloud cae. Alarm `SubscriptionExpiresIn` crítica.
- **Bloque 14.1.2**: history records dimensión SEPARADA de point.limit (`history.limit` / `historyExt.limit` / `historyRecord.limit`).
- **Bloque 14.3**: `sma.exempt="true"` atributo raro (http feature) — operational tier sin SMA continuo.
- **Bloque 14.3**: `nCloudDriver` tiene limits restrictivos (point.limit=1000, device.limit=1).
- **Bloque 14.4**: federation counting en origen (Sub), NO en Supervisor.
- **Bloque 14.2.2**: hard cap inmediato (no grace) para count. Grace 24-48h solo para expiry.

### Build y dev

- **Bloque 1.1**: profile matrix estricto — `rt` NO puede importar de `wb` nunca.
- **Bloque 12.1.4**: `module-include.xml` NO editable manual — regenerado cada build.
- **Bloque 12.2.7**: lexicon fallback silent si falta key.
- **Bloque 14.11.3**: Batch Editor sin pattern replacement `${i}` — workaround BajaScript.
- **Bloque 18.1.3**: `.sig` 256B raw RSA-2048 (no PKCS7) — jarsigner estándar NO sirve, solo plugin `com.tridium:niagara-signing-plugin:1.0.10`.
- **Bloque 18.4.3**: formato runtime `<java-permissions>` vs source `<niagara-permission-groups>` difieren — transformación en gradle build.

### UI

- **Bloque 9.1.5**: `axvelocity` license requerida para `.pxvm` files.
- **Bloque 9.2.3**: HxOps orden write/save/update/process obligatorio.
- **Bloque 9.3.5**: `sameSite=None` requiere `secure=true` (HTTPS) browsers modernos.
- **Bloque 15.1.3**: `BWsAnnotation` persist per-folder, no global — copy+paste a otra carpeta genera nueva anotación independiente.
- **Bloque 15.3.2**: wiresheet routing solo manhattan (NO bezier/straight nativo).
- **Bloque 15.14**: copy+paste NO auto-incrementa IPs/BACnet IDs — requiere BajaScript.
- **Bloque 15.14**: NO zoom in/out nativo en Workbench (sí web wiresheet ux).

### Runtime semantics

- **Bloque 4.2.4**: BComponent NO linkable directo — usar BOrd (BSimple).
- **Bloque 4.2.1**: `equals()` default en BObject = identidad. Solo BValue compara valor. Usar `equivalent()`.
- **Bloque 6.3.6**: priority levels 1 (emergency) y 8 (manual) **persisten en BOG** (excepción al TRANSIENT).
- **Bloque 8.3.7**: Schedule DST 2:30am fall-back → Niagara usa hora estándar (winter offset).
- **Bloque 15.8.4**: Facets NO enforced en load — solo en edit UI. API puede crear valores fuera range.
- **Bloque 15.14**: device offline proxy points siguen contando hacia `point.limit` (no auto-delete).

### Analytics + Provisioning + Federation

- **Bloque 16.1.1**: package `com.tridiumx.analytics` (con **x**), clase `BAnalyticService` (sin **s**), path `/Services/AnalyticService` — naming peculiaridad histórica.
- **Bloque 16.3.1**: NO existe `BRule` — pollers `BCyclicPoller`/`BTriggeredPoller` + `BAnalyticAlert` combinados.
- **Bloque 16.5.5**: Analytics Web API subscription TTL **60 seg** auto-expire — requiere poll cada 2-5 seg.
- **Bloque 16.5.1**: Analytics API usa `Content-Type: text/plain` (no `application/json`) — peculiaridad.
- **Bloque 16.10.1**: `BNiagaraNetworkJob` 2-stage FIXED (Initial + ForEachStation) — no arbitrary stages.
- **Bloque 19.13**: Fox channel leak en proxy delete mid-subscription → station restart required para cleanup.
- **Bloque 19.14**: NO HA nativa NiagaraDriver — `BSupervisorFailover` no existe.
- **Bloque 19.17**: BajaScript browser usa **BOX** (no Fox). Corrección al Bloque 9.2.

### Tags + Haystack + Query (Bloque 21)

- **Bloque 21.1**: Tags NO persiste en BOG como properties — derived on-demand via BTagRule evaluación. Backup/restore NO pierde tags, pero cambios en dict cambian qué tags "tiene" el mismo componente.
- **Bloque 21.2**: Haystack 4 `"children"` NO es herencia — son proto combinations (requeridas juntas). Herencia real es `"is"`.
- **Bloque 21.3**: Tag IDs **case-SENSITIVE** (`hs:air` ≠ `hs:Air`). Todos defs Haystack en lowercase.
- **Bloque 21.5**: BQL wildcard es `*` NO `%` NO `?` — distinto SQL clásico. Keywords case-INSENSITIVE, identifiers case-SENSITIVE.
- **Bloque 21.6**: NEQL shortest path ambiguity — `n:child->n:name="X"` evalúa SOLO primer hijo. Workaround: `traverse n:child-> where n:name="X"`.
- **Bloque 21.8**: `neqlizeExcludedTags` separador SEMICOLON no comma. Sin TagRuleIndex enabled → O(n) eval per query (crítico en 10k+ components).

### Presentation layer (Bloque 22)

- **Bloque 22.2**: NO scripting embedded nativo (`BPxScript` NO existe). Converters + custom Java bindings only.
- **Bloque 22.3**: ORD resolution **SINCRÓNICO** en render → UI bloquea si lento. Cache result en OrdTarget, retry async.
- **Bloque 22.4**: PxCache MAX_CACHE_SIZE configurable; multiple BPxInclude del mismo ord reutilizan widget tree. LRU eviction.
- **Bloque 22.5**: Bindings override widget local values → `BWidget.changed(Property, Context)` chequea `isOverriddenByBinding` antes de propagar.
- **Bloque 22.7**: NO bubbling eventos automático — Template Method pattern (no listener). Propagación explícita `parent.fireKeyEvent(e)`.
- **Bloque 22.11**: RequireJS case-SENSITIVE (`bajaScript/sys` ≠ `bajaScript/Sys`). Errores silenciosos.
- **Bloque 22.11**: Subscriptions NO persisten tras hard refresh — UI debe guardar ORDs NO referencias. Re-resolver post-reconnect siempre.
- **Bloque 22.12**: Implicit batching BoxMessageRelay debounce ~10ms puede introducir latencia inesperada. Manual Batch para crítico.
- **Bloque 22.12**: FoxScheme SIEMPRE RPC server-side — ORDs `fox:` nunca resuelven localmente (impacto multi-estación).

### BACnet deep (Bloque 23)

- **Bloque 23.3**: `rpmOk` flag — si device NO soporta ReadPropertyMultiple, Niagara cae a ReadProperty individual (3-5× tráfico).
- **Bloque 23.6**: WriteProperty priority=6 (manual takeover) bloquea otros writes de menor prioridad hasta release explícito.
- **Bloque 23.8**: MAX_APDU negotiation usa el MENOR (peer y Niagara). Supervisor NO controla downstream.
- **Bloque 23.9**: Si device advertisa `NO_SEGMENTATION` y respuesta > max-apdu → Abort(segmentation-not-supported). NO retry, fallo permanente.
- **Bloque 23.10**: Subscriber process ID DEBE ser único per-client — colisiones cruzan notificaciones.
- **Bloque 23.10**: COV subscription con `lifetime=0` = unsubscribe (NO error). Refresh NO es obligatorio — algunos devices solo notifican en cambio real.
- **Bloque 23.11**: Schedule protocol revision mismatch (Rev3 vs Rev16) = corrupción datos — `setSupport()` crítico.
- **Bloque 23.12**: Calendar reference no existente = exception schedule inoperante SILENT.
- **Bloque 23.15**: Trend Log BUFFER_SIZE=0 = circular ilimitado (memoria); STOP_WHEN_FULL + count=size = congela logging.
- **Bloque 23.16**: Access objects requieren BACnet/SC para máxima seguridad — N4.14 soporte parcial, NO prod-ready high security.
- **Bloque 23.26**: BBMD TTL renewal antes TTL/2, sino silent delete FDT entry (próximo broadcast NO llega).

### Schedule + Control (Bloque 24)

- **Bloque 24.2**: scanLimit default 90d — nextEvent() puede retornar null si NO hay cambio en ventana. Bajar a 14d si requiere predictability corto plazo.
- **Bloque 24.3**: BCompositeSchedule `union=false (AND)` + child `alwaysEffective=true` = SIEMPRE effective, impossible deshabilitar.
- **Bloque 24.4**: BScheduleReference stale si calendar movido/deleted — mantiene CalendarSubscriber para resubscribir auto.
- **Bloque 24.7**: Clock service dependency crítica — sin Clock, triggers NO disparan. Recovery automático tras restart + triggerMissed.
- **Bloque 24.11**: DST spring forward 02:30 Schedule se SALTA → triggerMissed fires en próx startup. Fall back puede disparar 2 veces.
- **Bloque 24.14**: Clock drift entre stations driverSchedule → versionado broken. NTP obligatorio. Subordinate adelantado timestamp futuro = NUNCA sync.
- **Bloque 24.12**: `getExportableSchedule()` INLINE BScheduleReference obligatorio antes de transmit — sino subordinate recibe ords irresolubles.
- **Bloque 24.13**: Last-write-wins bidireccional (NO conflict resolution). Arquitectura clara: UN supervisor.
- **Bloque 24.18**: Tuning policy `minWritePeriod=0.1s + deadband=0 + change-driven` = device flood → reset. Aumentar deadband + throttle executeTime.
- **Bloque 24.18**: `execute()` recursivo A→B→A stack overflow ~100 iter. Mitigar con `Clock.schedule()` para desacoplar.
- **Bloque 24.16**: NullProxyExt vs null — usar `BNullProxyExt.isNull()=true` NO `null` (evita NPE en onExecute).

### Migration + Build + Help (Bloque 25)

- **Bloque 25.2**: Module removed en N4 → `BModuleRemovalConverter` silent removal. Log `"moduleRemoved: ax:Module"`. ORDs huérfanos manual fix.
- **Bloque 25.7**: Migration tool NO refactoriza Java bytecode — Programs con APIs deprecated (`BICollection`, `getProgram`, `BAlarmService.getOpenAlarms`) requieren post-migration manual fix.
- **Bloque 25.7**: Licencias AX incompatibles con N4 (host ID hash diff 32→64 bit) — new license request obligatorio.
- **Bloque 25.7**: Downgrade N4→AX **destruye datos N4** (keytabs/certs/SSL keys). NO es rollback moderno — es full reset + restore.
- **Bloque 25.8**: Doclet version embedded en `.bajadoc` via `createdBy` attr. `createdAt` + `createdOn` permite track en qué build nació doc.
- **Bloque 25.9**: `docmodule.gradle` se aplica CONDICIONALMENTE (solo si doc module). SearchBuilder JavaExec requiere classpath nre+baja+help-wb+html-wb.
- **Bloque 25.11**: Help system NO usa Lucene — custom full-text implementation (menor footprint).
- **Bloque 25.13**: Render help via **BWebBrowser** (Jetty wrapper) NO navegador externo — CSS embedding para estilo offline.

### Native layer + signing (Bloque 26)

- **Bloque 26.2**: niagara.home/user.home resolution orden: env var → Registry `HKLM\Software\Honeywell\Niagara\<version>` → relative `../`.
- **Bloque 26.5**: MSVC runtime multiplexing — VCRUNTIME140 + MSVCR120 legacy coexisten → potential DLL boundary issues. ucrtbase.dll es real impl.
- **Bloque 26.6**: WinPcap (pcapBacEther.dll) deprecated 2018, CVEs conocidos. Considerar deprecar BACnet Ethernet, usar IP-based.
- **Bloque 26.7**: libciper.so debug symbols retained en production — unexpected artifact de build.
- **Bloque 26.8**: FFmpeg 34.9 MB alto CVE rate históricamente en parsers codec. Sandboxing + permiso upload restrictivo.
- **Bloque 26.10**: Chromium/JxBrowser 260 MB requiere patching independiente → frequent Niagara releases. ABI tightly coupled con Chromium version.
- **Bloque 26.17**: `.sig` sidecar es SIEMPRE 256 bytes exactos para RSA-2048 (raw signature NO ASN.1). No confundir con firma embedded JAR.
- **Bloque 26.17**: `niagara-signing-plugin:1.0.10` ÚNICA vía standalone — NO maven plugin alternativo, NO CLI Tridium tools.
- **Bloque 26.17**: TSA timestamping CRÍTICO producción — sin él cert expirado = módulo fail al validar.
- **Bloque 26.21**: 3 permission groups (ACCESS_CLASS+REFLECTION+MBEAN_PERMISSION) requieren firma Tridium válida incluso en `moduleVerificationMode=low`. Self-signed NO aplica.
- **Bloque 26.21**: Password gradle requirements enforced — min 10 chars + 1 digit + 1 lowercase + 1 uppercase.
- **Bloque 26.21**: Plaintext passwords en XML profile — proteger permissions 600 + segregate secrets.
- **Bloque 26.25**: niagarad.exe Windows service wrapper. SCM SERVICE_CONTROL_STOP → JVM shutdown hook.

### Network + certs (Bloque 27)

- **Bloque 27.1**: `3011` HTTP plain platform sigue **abierto por default** post-install N4.14. Lockdown requiere `sslOnly=true` manual en `!config/platform/*.bog`. Hardening NO automático.
- **Bloque 27.3**: Niagara **NO valida OCSP ni CRL** — certs revocados se aceptan hasta expiry natural. Gap compliance NIST/IEC 62443.
- **Bloque 27.4**: 4 trust stores (user + system + daemon + `userUntrustedStore` quarantine). El untrusted store es único en la industria — certs rechazados se preservan para review (NO deletion).
- **Bloque 27.6**: `Webs.license` (16KB) tiene feature `skipModuleValidation`; `Honeywell.license` (2.3KB) NO lo tiene — matriz bypass cross-license que puede dejar "hole" operacional en deploys mixed-license.
- **Bloque 27.7**: Signing Service enterprise centralizado con **retry 6h approval + 30m results** — timeouts duros para fleet 1000+ JACEs. Sin él → workflow single-point.
- **Bloque 27.8**: **`BHeaderAuthenticationScheme` NO EXISTE** — Niagara N4.14 NO soporta nativamente SSO vía `X-Forwarded-User` de reverse proxy. Workaround requiere módulo custom. Crítico para integraciones corporate.
- **Bloque 27.9**: FoxS detrás de reverse proxy requiere `org.bouncycastle.jsse.client.assumeOriginalHostName=true` en `system.properties` — sin él, SNI mismatch + handshake fail silent.
- **Bloque 27.11**: `.certificate` XML custom Tridium/Honeywell con `expiration="never"` (desde 2003) — anchor inmutable que no rota y cuyo bypass rompe toda la cadena.

### Discovery + Virtual (Bloque 28)

- **Bloque 28.1**: **NO existe `BDiscoveryJob` abstracto** — discovery es UI-driven (`BLearnTable` + `MgrController$Discover` en `workbench-wb.jar`), NO server-driven. Expectativa arquitectónica invertida: si diseñás un driver nuevo, debés crear un manager UI completo, no un servicio.
- **Bloque 28.2**: BACnet WhoIs broadcast 47808/UDP → IAm → object-list mining — si device tiene 10k+ objetos, segmentación puede tomar minutos. Sin progress indicator nativo.
- **Bloque 28.5**: **Modbus NO tiene discovery API** — grep confirma ausencia en modbusCore/modbusAsync/modbusTcp. Framework NO fuerza discovery; hay que crear devices manualmente o via template.
- **Bloque 28.6**: SNMP sin **v3 en distro Honeywell** y walk NO usa **GetBulk** optimization — performance gap en networks con 1000+ OIDs.
- **Bloque 28.9**: `niagaraVirtual-rt.jar` 317 KB + 66 clases es módulo first-class dedicado (el Bloque 13 lo subestimó). Incluye 12 subclases control points + schedules + stubs.
- **Bloque 28.13**: Virtual points **NO cuentan** en `point.limit` license — `BPointCountVisitor` detecta `BVirtualComponentSpace` y skip. Tradeoff estratégico Supervisor-Subordinate.
- **Bloque 28.14**: Virtual crash regenera desde parent → race condition con subscription (Fox/BOX) si el child no existe al momento del subscribe. Debugging opaco: virtual "fantasma" aparece y desaparece.

### Web tier + Servlets (Bloque 29)

- **Bloque 29.1**: Jetty **9.4.54** en N4.14 tiene **EOL 2025** — riesgo CVE unpatched a mediano plazo. Migration path a Jetty 12 NO anunciada públicamente.
- **Bloque 29.2**: BWebServlet registry es **dinámico via BComponent tree** — NO `@WebServlet` annotation, NO `web.xml`. Registrar un servlet nuevo requiere crear un BComponent con path como slot.
- **Bloque 29.3**: **Filter chain 15 capas** — invariant: `AuthenticationFilter` ANTES de `CsrfProtectedFilter`. Invertir el orden deja CSRF evaluado sin identidad conocida → bypass.
- **Bloque 29.4**: **53 servlets reales** en distro Honeywell (Bloque 9 mencionaba ~6). Si agregás custom, chequeá conflictos en path routing — primer match gana.
- **Bloque 29.5**: `NiagaraHttpSession` agrega **múltiples HttpSession Jetty** por `superId` — NO es 1:1. Session fixation safe porque regenera ID post-auth, pero debugging requiere entender el agregado.
- **Bloque 29.5**: `cacheSessionsAndRestart` preserva sessions al cambiar config sin logout forzado de todos los usuarios — inusual y útil.
- **Bloque 29.16**: `requestHeaderSize` default **8 KB** — Bearer tokens largos (OAuth con scopes extensos, SAML cookies grandes) overflow silent → 413 sin mensaje claro.
- **Bloque 29.16**: `Content-Type: text/plain` NO es solo BNaServlet (Bloque 16.5.1) — **múltiples servlets** lo usan para respuestas que son realmente JSON/XML. Workaround frontend: parse defensivo.

### Enterprise auth + FIPS + key rotation (Bloque 30)

- **Bloque 30.7 (CORRECCIÓN mayor)**: Niagara NO usa `master.jceks` — usa **`.km`/`.kr` DPAPI Windows** para sensitive data encryption. BPasswords reversibles se recuperan via DPAPI OS-level, NO JCE keystore. **Invalida el supuesto del Bloque 13.2.4**. Corrupt keyring → BPassword silent empty.
- **Bloque 30.2**: SAML implementa **dual mode** (Niagara como RP y como IdP), pero **SLO (Single Logout) NO está implementado** — gap de compliance real. IdP session queda colgada al logout de Niagara.
- **Bloque 30.3**: OAuth/OIDC **NO soportado nativo** como auth scheme — `oauth2-rt` es solamente cliente M2M (Niagara llama APIs externas). Para SSO moderno hay que usar SAML o escribir custom scheme.
- **Bloque 30.6**: FIPS migration rompe módulos pre-FIPS (signature incompat + cipher exclusions). Hay que re-firmar todo el stack con BCFIPS provider. Sin rollback automático.
- **Bloque 30.8**: TLS cert rotation zero-downtime funciona para Jetty + Fox (hot reload), pero **BACnet/SC requiere restart** — no hay hot reload en TLS 1.3 del stack BACnet. Planificar ventana.
- **Bloque 30.11 (CORRECCIÓN a 31.8)**: Audit main sink es **sincrónico inline** (NO async). El queue unbounded `LinkedBlockingQueue` que reporta Bloque 31.8 aplica al **SYSLOG side-channel**, no al main sink. La latency de audit impacta directo en el invoker.
- **Bloque 30.13**: `NSuperSession` enforceá **1 concurrent session per user invariant** — NO configurable. Segundo login kicks al primero. Gotcha operacional en deploys con usuarios compartidos.
- **Bloque 30.10**: RBAC method-level protege **slots interceptados**, pero Java methods directos NO — reflection está protegido por permission group (Bloque 3). Escribir código que bypassea slots = bypassea RBAC.
- **Bloque 30.15**: LDAP group mapping **NO refresca hasta re-login del user** — cambios de permisos en AD no aplican a sesión activa. Workaround: forzar logout mass.

### Performance + observability (Bloque 31)

- **Bloque 31.3**: GC default en JDK 8 Azul Zulu es **ParallelGC (NO G1GC)** — confirmado empírico con `nre.properties` sin flag `-XX:+UseG1GC`. Stations production con heap 4GB+ deben setear G1GC manualmente.
- **Bloque 31.6**: Niagara **NO usa `ManagedBlocker`** en ForkJoinPool → blocking BJobs saturan el common pool. Long-running I/O job bloquea todo el scheduler. Workaround: dedicated thread pool para I/O-bound.
- **Bloque 31.7**: History archive bloquea **5-30 min** durante SQLite VACUUM (`lingerTime=5min` + cursor `inactivityTimeout=2min`). UI queries durante archive = timeout. Schedule off-peak obligatorio.
- **Bloque 31.8**: Audit SYSLOG side-channel usa `LinkedBlockingQueue` **UNBOUNDED** → overflow silent loss vía OOM cascade (no via drop). Disk o network slow puede matar la station por heap.
- **Bloque 31.10**: Backup es **monolítico sin chunking/resume** — stations 1GB+ config fallan en restores remotos high-latency. Local restore única opción confiable.
- **Bloque 31.12**: Audit `.adb` SQLite NO rotatea — OS logrotate NO aplica (formato SQLite nativo). Disk fill silencioso sin auto-delete built-in.
- **Bloque 31.14**: Heap `-Xmx1024M` default es **conservador** — confirma bottleneck Supervisor 50+ subs (Bloque 13.1.7 + 19.13). Escalar a 4-8GB + G1GC para Supervisor realista.
- **Bloque 31.11**: TimeZone per-history `BHistoryConfig.timeZone` → aggregation cross-zone en Supervisor puede reportar wrong si BQL no normaliza a UTC. Gotcha silent, chart display tz vs storage tz mismatch.

### Honeywell + runtime semantics (Bloque 32)

- **Bloque 32.1**: `honPlantController-rt.jar` contiene la clase JNI (`com/honeywell/comm/JNIRequest`) pero la `.so` (`libplantctrl.so`) **NO se distribuye en Supervisor Windows** — vive solo en CIPer/JACE ARM. Supervisor Windows falla silent al instanciar BPlantController (ClassNotFoundException en native load).
- **Bloque 32.2**: `honPlantController` incluye **BTP + RSTP stacks completos** — soporta topología Ethernet ring redundante (Rapid Spanning Tree). Hallazgo operacional no documentado previamente: Honeywell usa ring en CIPer deployments.
- **Bloque 32.3**: `jsonToolkit` **NO es custom Tridium** — re-empaqueta Jayway JsonPath (Apache 2.0 OSS) + json-smart. Partner convention `com.tridiumx.*` con `x`. 324 clases. Implicación: CVEs de Jayway aplican a Niagara.
- **Bloque 32.4**: `honPlantController` embebe **Google Gson standalone** — shaded dependency. Cada módulo Honeywell maneja su propio JSON stack. No hay stack unificado corporate.
- **Bloque 32.5**: `platPower` 16 clases = JACE SLA + QNX + Javelina + NPM + Dual Battery + NiMH + UPS — **platform-embedded-specific**, NO funcional en Supervisor Windows.
- **Bloque 32.7**: **NO existe módulo `optimizer-*.jar`** — "Optimizer Supervisor" es naming comercial Honeywell. Funcionalidad dispersa en `clHVAC*` suite (8 variantes + clCBus 8.4 MB). Gotcha para onboarding devs nuevos.
- **Bloque 32.9**: Niagara **NO tiene `BTransaction` real** — patterns multi-step son compensation actions manuales (backup+history+auth+boot+provisioning). BOG save atomic vía BLoadOp único garante. Station crash mid-op = estado parcial en disco.
- **Bloque 32.12**: ClassLoader per-module parent-first delegation + shared baja.jar bootclasspath. **ClassLoader leak risk**: old loaders not GC'd en reload scenarios (common JVM issue) → Metaspace leak 24x7 obliga restart periódico.
- **Bloque 32.13**: BOG forward compat **NO soportado** — nuevo BOG no abre en N4 antigua. Backward compat via `BOnMissingType` stub (inferido — no confirmado con decompilación profunda).
- **Bloque 32.14**: `honBacnetHelper` FastAccessList batch multi-property reads en single APDU → **crítico scaling 475K points**. Sin él, BACnet subscription saturates network.

---

## Conexiones clave entre bloques

Grafo de referencias cross-bloque:

```
Bloque 1 (Estructura) ─── fundamenta ───> 2, 3, 4, 10, 17
Bloque 2 (Licensing) ─── base para ───> 13.1, 14.1-14.4, 16.1.3
Bloque 3 (Security JVM) ─── complementa ───> 11, 13.1, 13.2, 17.4, 18
Bloque 4 (Baja) ─── substrate para ───> 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 20
Bloque 5 (ORD/BOG) ─── mecanismo para ───> 6, 10.2.3, 13.2, 15.1.3, 20
Bloque 6 (Control) ─── consumido por ───> 7, 8, 15.3, 16.2
Bloque 7 (Drivers) ─── conecta con ───> 6.2.6, 14.12, 15.10, 15.11, 19
Bloque 8 (Alarm/History/Schedule) ─── federado en ───> 13.1, 16.4, 19.11.6, 20.8
Bloque 9 (UI) ─── consume ───> 11, 13.3, 15, 16.5, 19.17 (BOX corrección)
Bloque 10 (Platform) ─── contexto para ───> 11.3, 17, 20.3.2 (boot phases refinement)
Bloque 11 (RBAC) ─── ortogonal a ───> Bloque 3; consumido por ───> 20.8.5
Bloque 12 (Build) ─── genera ───> 4-9 artifacts; plugin 18.1.1
Bloque 13 (Gaps) ─── profundiza ───> 1.5, 2, 5.1, 5.2, 9.3.6, 9.1; expandido por 19.11 (NiagaraDriver)
Bloque 14 (Templates + Counting) ─── integra ───> 2, 4, 8, 10.3, 13.1, 16.17 (escala)
Bloque 15 (Workbench) ─── consume ───> 4, 5, 6, 7, 8, 9.1, 11, 14; conecta 16
Bloque 16 (Analytics + Provisioning) ─── extends ───> 8, 9.3.7, 13.1, 14.15, 15; implementation BAbstractService Bloque 20.3
Bloque 17 (Filesystem + JRE) ─── establece ───> paths físicos que 18, 20 citan
Bloque 18 (Signing + Perms + CSRF) ─── corrige ───> 3.4 (3 groups sign); expande ───> 9.3.6, 11.3, 12.1
Bloque 19 (LON/NRIO/NiagaraDriver/BOX) ─── profundiza ───> 7.3.3, 13.1, 13.2, 14.12; corrige ───> 9.2 (BajaScript usa BOX)
Bloque 20 (BApp + Misc + Gap) ─── refina ───> 10.2.2 (boot 6-phase), 18.4 (JMX MBEAN); gap analysis para todo
Bloque 21 (Tags + Haystack 4 + BQL + NEQL) ─── profundiza ───> 5.3 (BQL/NEQL/Tags superficial), 8 (alarm integration con BNeqlizeRpc), 16.16 (Analytics Framework usa NEQL para QueryLevelDef), 14.6 (Templates integra)
Bloque 22 (PX + BajaUI + BajaScript browser + Bajadoc) ─── profundiza ───> 9 (UI stack superficial), 15 (Workbench editing), 19.17 (BOX protocol detalle cliente-side)
Bloque 23 (BACnet deep) ─── profundiza ───> 4 (driver framework BACnet superficial), 8 (alarm routing via bacnetAlarmRouter-rt), 19 (NRIO no BACnet)
Bloque 24 (Schedule Native + driverSchedule + kitControl palette) ─── profundiza ───> 6 (Control Engine superficial), 8.3 (Schedule base), 23.11 (BACnet schedule equivalent)
Bloque 25 (Migration + Bajadoc pipeline + Gradle build + Help) ─── profundiza ───> 12 (Build system), 4 (driver migration mentions), 10 (platform lifecycle), 23.23 (bacnetMigrator)
Bloque 26 (NRE launcher + 138 DLLs + signing ops playbook) ─── profundiza ───> 10 (platform daemon superficial), 17 (filesystem + DLLs superficial), 18 (module signing teoría); complementa con playbook operacional firma standalone
Bloque 27 (Network surface + puertos + certManagement) ─── consolida ───> 1.5 (Fox puertos), 10.1 (platform 3011/5011), 11 (auth), 13.2 (Fox wire), 17 (filesystem security), 18 (signing cert); profundiza cert lifecycle + trust chain + 22 puertos unified
Bloque 28 (Discovery cross-protocol + Virtual) ─── profundiza ───> 7 (driver framework genérico), 13.5 (virtual superficial), 14.10 (Template/Match/Bind), 19 (LON + Niagara discovery fragmentario), 23 (BACnet WhoIs detail protocol)
Bloque 29 (Web tier + Servlets + Jetty filter chain) ─── profundiza ───> 9.3 (BWebService overview), 11.3 (session shallow), 13.3 (NiagaraRPC superficial), 16.5 (BNaServlet ejemplo), 18.6 (CSRF), 22.12 (BOX muxing cliente-side)
Bloque 30 (Enterprise auth federation + FIPS + key rotation) ─── cierra gaps 20.10 #10/#11/#15/#16/#17 ───> 11 (RBAC base), 13.2 (keyring superficial — CORRIGE master.jceks→.km/.kr DPAPI), 17 (BCFKS FIPS), 18 (SCRAM + signing), 27 (cert types matrix + HeaderAuth gap), 29 (auth schemes + filter chain)
Bloque 31 (Performance tuning + observability) ─── cierra gaps 20.10 #4/#18/#20/#21/#22/#24/#27 ───> 6.1 (engine thread), 8 (history archive), 15.14 (polling limits), 17.5 (JVM defaults), 20.5-20.8 (managers + persistent policies); CORRIGE 31.8 audit queue via Bloque 30.11 (main sink inline sync, unbounded queue es syslog side-channel)
Bloque 32 (Honeywell modules + SMA + non-HTTP + runtime semantics) ─── cierra gaps 20.10 #1/#3/#5/#7/#8/#13 ───> 1 (framework), 2 (licensing + SMA attr), 4 (Baja object), 10 (boot), 19 (Honeywell drivers superficial), 23.27 (honBacnet), 25 (migrations Honeywell), 26 (native libs), 27 (licensing matrix)
```

---

## Engram topic keys (toda la memoria persistente)

**Total: 91 topic keys** bajo `project: niagara-research`.

### Capa 1 (Bloques 1-3) — 10 keys
- `niagara/estructura/profiles-rt-ux-wb`, `niagara/estructura/registry-types`, `niagara/estructura/fox-protocol`
- `niagara/licensing/sma-attribute-model`, `niagara/licensing/honeywell-oem-overlay`, `niagara/licensing/license-manager-api`
- `niagara/security/cert-chain-pipeline`, `niagara/security/permission-groups-19-table`, `niagara/security/skip-module-validation-bypass`, `niagara/security/policy-files-triple-signed`

### Capa 2 (Bloques 4-6) — 9 keys
- `niagara/baja/slot-system`, `niagara/baja/type-hierarchy`, `niagara/baja/lifecycle-facets-dynamic`
- `niagara/navigation/ord-system`, `niagara/persistence/bog-format`, `niagara/queries/bql-neql-hierarchy-tags`
- `niagara/execution/engine-thread-model`, `niagara/execution/link-model-binding`, `niagara/control/kitcontrol-blocks`

### Capa 3 (Bloques 7-9) — 9 keys
- `niagara/drivers/framework-generico`, `niagara/drivers/bacnet-detalle`, `niagara/drivers/otros-modbus-lon-obix-snmp`
- `niagara/subsystems/alarm-pipeline`, `niagara/subsystems/history-service`, `niagara/subsystems/schedule-subsystem`
- `niagara/ui/workbench-px-gx`, `niagara/ui/bajascript-ux-hx`, `niagara/ui/servlets-jetty-webservices`

### Capa 4 (Bloques 10-12) — 9 keys
- `niagara/platform/daemon-niagarad`, `niagara/platform/station-boot-filesystem`, `niagara/platform/backup-dist-disaster-recovery`
- `niagara/auth/user-role-category-permission`, `niagara/auth/authentication-schemes`, `niagara/auth/session-autologoff-enterprise`
- `niagara/build/gradle-plugin`, `niagara/build/ax-n4-migration-lexicons`, `niagara/build/testing-palettes-help-editors`

### Capa 5 (Bloque 13) — 3 keys
- `niagara/advanced/subscription-niagaranetwork`, `niagara/advanced/fox-wire-crypto-virtual`, `niagara/advanced/rpc-reports-search-uxmedia-nav`

### Capa 6 (Bloques 14-16) — 9 keys
- `niagara/licensing/point-counting-limits-runtime`, `niagara/templates/niagara-core-vs-easytemplates-honeywell`, `niagara/operations/batch-editor-lon-template-match-bind`
- `niagara/ui/wiresheet-editor-glyphs-state-machine`, `niagara/ui/property-sheet-nav-tree-fieldeditor`, `niagara/ui/point-device-manager-workflow-end-to-end`
- `niagara/analytics/framework-core-algorithms-pollers-alerts`, `niagara/analytics/web-api-rest-servlet-subscription`, `niagara/provisioning/service-niagaranetworkjob-stages-steps`

### Capa 7 (Bloques 17-18) — 6 keys
- `niagara/filesystem/install-home-layout`, `niagara/filesystem/user-daemon-homes`, `niagara/platform/jre-embebido-azul-zulu-jdk8`
- `niagara/security/module-signing-standalone-gradle-plugin`, `niagara/security/module-permissions-xml-source-runtime`, `niagara/security/csrf-header-auth-annotations-exemptions`

### Capa 8 (Bloque 19) — 3 keys
- `niagara/drivers/lon-deep-xif-lnml-snvt-programid`, `niagara/drivers/nrio-niagaradriver-station-federation`, `niagara/protocols/box-protocol-websocket-json`

### Capa 9 (Bloque 20) — 3 keys
- `niagara/misc/bapp-webapp-net-module-httpproxy`, `niagara/misc/babstractservice-lifecycle-monitors-engine-lease-resource`, `niagara/misc/bjobservice-persistent-policies-gap-analysis`

### Capa 10 (Bloques 21-22) — 6 keys
- `niagara/bloque21/tag-framework`, `niagara/bloque21/bql`, `niagara/bloque21/neql`
- `niagara/bloque22/px-runtime`, `niagara/bloque22/bajaui-widget-framework`, `niagara/bloque22/bajascript-runtime`

### Capa 11 (Bloques 23-26) — 12 keys
- `niagara/bloque23/bacnet-objects-properties`, `niagara/bloque23/bacnet-stack`, `niagara/bloque23/bacnet-advanced-objects`
- `niagara/bloque24/schedule-native`, `niagara/bloque24/driverschedule-cross`, `niagara/bloque24/kitcontrol-palette`
- `niagara/bloque25/migration-framework`, `niagara/bloque25/bajadoc-gradle`, `niagara/bloque25/help-system`
- `niagara/bloque26/nre-launcher`, `niagara/bloque26/native-dlls-catalog`, `niagara/bloque26/module-signing-playbook`

### Capa 12 (Bloques 27-29) — 3 keys
- `niagara/bloque27/network-cert` (network surface + 22 puertos + 4 trust stores + 7 cert types + lifecycle + header auth gap)
- `niagara/bloque28/discovery-virtual` (Discovery UI-driven cross-protocol + 6 drivers comparativa + BVirtualComponent + niagaraVirtual-rt 66 clases + licensing exclusion)
- `niagara/bloque29/web-servlets` (Jetty 9.4.54 + 53 servlets + filter chain 15 capas + NiagaraHttpSession agregado + 9 auth schemes + WebSocket BOX/Fox + 17 gotchas prod)

### Capa 13 (Bloques 30-32) — 13 keys
- **Bloque 30 — principales**: `niagara/bloque30/auth-federation-fips-rotation` (consolidado)
- Bloque 30 granulares: `niagara/auth/ldap-federation-deep`, `niagara/auth/saml-rp-and-idp`, `niagara/auth/oauth-oidc-gap`, `niagara/security/master-keyring-km-kr` (CORRECCIÓN: DPAPI, NO master.jceks), `niagara/fips/migration-workflow-10-steps`, `niagara/tls/cert-rotation-zero-downtime`, `niagara/session/supersession-aggregator`, `niagara/audit/sync-semantic` (CORRIGE 31.8 queue = syslog side-channel), `niagara/rbac/method-level-enforcement`
- **Bloque 31**: `niagara/bloque31/perf-observability` (21 thread pools + ParallelGC default + FJP sin ManagedBlocker + heap scale XS→XL + 22 gotchas prod + playbook 8 pasos)
- **Bloque 32**: `niagara/bloque32/honeywell-runtime-semantics` (honPlantController BTP+RSTP + jsonToolkit Jayway OSS + platPower 16 clases + optimizer naming + Sys.loadType + ClassLoader isolation)

**Total actualizado: 91 topic keys** (78 previos + 13 nuevos Capa 13).

---

## Qué NO cubre este mental model — gap analysis final consolidado

Catálogo original de 27 gaps en Bloque 20.10. **Tras sesiones 27-29 + 30-32 se cerraron 17 gaps** (#1/#3/#4/#5/#7/#8/#10/#11/#13/#15/#16/#17/#18/#20/#21/#22/#24/#27). Los **10 gaps restantes** NO son investigables sin acceso a lab multi-station, NDA vendor, o representan áreas que la arquitectura directamente no implementa:

### ✅ Cerrados en sesiones 27-29 + 30-32

- **#1 Transaction semantics** → Bloque 32.9 (NO hay `BTransaction` real; compensation manual)
- **#3 Module lifecycle hooks** → Bloque 32.10
- **#4 Performance tuning specifics** → Bloque 31 completo (21 thread pools + GC + I/O + heap scale)
- **#5 `Sys.loadType()` extensions** → Bloque 32.11-32.12
- **#7 Honeywell modules deep** → Bloque 32.1-32.7
- **#8 SMA licensing flow** → Bloque 32.5
- **#10 FIPS compliance workflow** → Bloque 30.6 (10 pasos)
- **#11 LDAP/SAML/OAuth federation** → Bloque 30.1-30.5 (OAuth/OIDC gap documentado)
- **#13 `fox.sys` + `ndriver` + non-HTTP** → Bloque 32.6-32.8
- **#15 Key rotation workflow** → Bloque 30.7 (CORRECCIÓN: DPAPI, NO master.jceks) + Bloque 30.8 (TLS cert rotation)
- **#16 Token expiry cross-token** → Bloque 30.9 (matriz 12 tokens)
- **#17 RBAC method-level + Auditor** → Bloque 30.10-30.11
- **#18 TimeZone multi-zone archives** → Bloque 31.11
- **#20 ForkJoinPool vs blocking I/O** → Bloque 31.6 (confirma NO hay ManagedBlocker)
- **#21 Audit queue semantics** → Bloque 30.11 + 31.8 (main sync + syslog async unbounded)
- **#22 Job exception handling** → Bloque 31.9
- **#24 History archive DB compaction blocking** → Bloque 31.7 (5-30 min)
- **#27 Audit retention** → Bloque 31.12 (disk fill risk)

### ⚠️ Cubiertos parcialmente en 27-29 + 30-32

- **#6 BOG schema evolution intra-N4** → Bloque 32.13 (parcial — `BOnMissingType` inferido, forward compat NO supported confirmado)

### ❌ NO cubribles sin lab/NDA — gaps residuales honestos

- **#2 Clustering + HA nativo** → CONFIRMADO NO EXISTE en NiagaraDriver (Bloque 19.14). No hay nada que investigar empíricamente — es un gap arquitectónico del producto.
- **#9 Remote diagnostics channels vendor-specific** → Requiere NDA Honeywell para documentación interna.
- **#12 External datasources (Oracle, SQL Server, timeseries externos)** → Niagara no tiene drivers nativos; integración es custom por proyecto. Investigar bajo demanda.
- **#14 Skyspark alternatives** → Confirmado ausente en Bloque 16. Investigación de mercado, no de código.
- **#19 Clock.time() drift en RTC sync** → Requiere lab físico multi-station con NTP control.
- **#23 Large backup restore timeout en high-latency** → Requiere lab con WAN simulation. Gap documentado en Bloque 31.10 como risk sin lab.
- **#25 Session timeout clock skew multi-server** → Requiere cluster testing sin NTP. Documentado como risk en Bloque 30.13.
- **#26 Lockout window edge case clock backward** → Requiere inject clock-backward physical test.

Para cualquier gap residual, el mental model actual (32 bloques) es suficiente para orientarse y atacar con investigación puntual adicional o testing en lab.

---

## Próximos pasos recomendados

Con los 32 bloques cerrados, tenés **~99.5%** del framework Niagara N4.14 entendido conceptualmente. Lo que queda:

1. **Práctica**: implementar un módulo end-to-end (driver simple + control logic + UI + proxy points + Analytics algorithm) usando el conocimiento. El Bloque 15.13 (workflow 5 fases) es la receta.
2. **Debugging real**: cuando surja un problema de producción, usar el mental model para localizar el bloque relevante + gotchas transversales.
3. **Updates puntuales**: cuando Tridium/Honeywell release N4.15+ o features nuevas, actualizar bloques específicos en vez de re-investigar.
4. **Contribución inversa**: si identificás gotchas nuevos en uso real, agregarlos a los bloques correspondientes vía commit directo.
5. **Deep dives en gaps**: elegir 1-2 de los 27+ gaps (ej. HA clustering, FIPS workflow, transaction semantics) para profundizar según criticidad del deployment.

---

## Repositorio

GitHub: https://github.com/angeles725/niagara-research (privado)

Estructura del repo:
```
/
├── INDEX.md (este archivo)
├── niagara-mental-model.md           (Bloques 1-3)
├── niagara-mental-model-bloque4.md   (Baja Object Model)
├── niagara-mental-model-bloque5.md   (ORD + BOG + Queries)
├── niagara-mental-model-bloque6.md   (Control Engine)
├── niagara-mental-model-bloque7.md   (Drivers Framework)
├── niagara-mental-model-bloque8.md   (Alarm + History + Schedule)
├── niagara-mental-model-bloque9.md   (UI Stack)
├── niagara-mental-model-bloque10.md  (Platform + Station)
├── niagara-mental-model-bloque11.md  (Auth + RBAC)
├── niagara-mental-model-bloque12.md  (Build + Dev Lifecycle)
├── niagara-mental-model-bloque13.md  (Gaps profundos)
├── niagara-mental-model-bloque14.md  (Point counting + Templates + Batch Editor)
├── niagara-mental-model-bloque15.md  (Workbench editing deep)
├── niagara-mental-model-bloque16.md  (Analytics + Provisioning)
├── niagara-mental-model-bloque17.md  (Filesystem forensics + JRE)
├── niagara-mental-model-bloque18.md  (Module signing + permissions + CSRF + SCRAM)
├── niagara-mental-model-bloque19.md  (LON + NRIO + NiagaraDriver + BOX)
├── niagara-mental-model-bloque20.md  (BApp + net + Monitors + JobService + gap analysis)
├── niagara-mental-model-bloque21.md  (Tag Framework + Haystack 4 + BQL + NEQL)
├── niagara-mental-model-bloque22.md  (PX + BajaUI + BajaScript browser + Bajadoc runtime)
├── niagara-mental-model-bloque23.md  (BACnet deep — objects + properties + stack + COV + BBMD + EDE)
├── niagara-mental-model-bloque24.md  (Schedule Native + driverSchedule cross-driver + kitControl 152 components)
├── niagara-mental-model-bloque25.md  (Migration + Bajadoc pipeline + Gradle build + Help system)
├── niagara-mental-model-bloque26.md  (NRE launcher C++ + 138 DLLs + standalone signing playbook)
├── niagara-mental-model.2026-04-19.md (snapshot sesión httpapi)
├── NEXT_SESSION_PROMPT.md, NEXT_SESSION_PROMPT_MODULE_NAVIGATOR.md (plantillas)
├── notes/                            (borradores source)
└── .atl/                             (SDD registry)
```

---

**Sesión cerrada**: 2026-04-22 — Mental model Niagara N4 consolidado en **20 bloques** con ~92-95% coverage conceptual. 57 topic keys engram. 27+ gaps documentados honestamente para futuro.

Si este mental model te ahorró horas de investigación o te evitó un bug de producción, el objetivo está cumplido.
