package javax.baja.bacnet.enums;

import javax.baja.bacnet.BacnetConst;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "ackedTransitions",
      ordinal = 0
   ), @Range(
      value = "ackRequired",
      ordinal = 1
   ), @Range(
      value = "action",
      ordinal = 2
   ), @Range(
      value = "actionText",
      ordinal = 3
   ), @Range(
      value = "activeText",
      ordinal = 4
   ), @Range(
      value = "activeVtSessions",
      ordinal = 5
   ), @Range(
      value = "alarmValue",
      ordinal = 6
   ), @Range(
      value = "alarmValues",
      ordinal = 7
   ), @Range(
      value = "all",
      ordinal = 8
   ), @Range(
      value = "allWritesSuccessful",
      ordinal = 9
   ), @Range(
      value = "apduSegmentTimeout",
      ordinal = 10
   ), @Range(
      value = "apduTimeout",
      ordinal = 11
   ), @Range(
      value = "applicationSoftwareVersion",
      ordinal = 12
   ), @Range(
      value = "archive",
      ordinal = 13
   ), @Range(
      value = "bias",
      ordinal = 14
   ), @Range(
      value = "changeOfStateCount",
      ordinal = 15
   ), @Range(
      value = "changeOfStateTime",
      ordinal = 16
   ), @Range(
      value = "notificationClass",
      ordinal = 17
   ), @Range(
      value = "controlledVariableReference",
      ordinal = 19
   ), @Range(
      value = "controlledVariableUnits",
      ordinal = 20
   ), @Range(
      value = "controlledVariableValue",
      ordinal = 21
   ), @Range(
      value = "covIncrement",
      ordinal = 22
   ), @Range(
      value = "dateList",
      ordinal = 23
   ), @Range(
      value = "daylightSavingsStatus",
      ordinal = 24
   ), @Range(
      value = "deadband",
      ordinal = 25
   ), @Range(
      value = "derivativeConstant",
      ordinal = 26
   ), @Range(
      value = "derivativeConstantUnits",
      ordinal = 27
   ), @Range(
      value = "description",
      ordinal = 28
   ), @Range(
      value = "descriptionOfHalt",
      ordinal = 29
   ), @Range(
      value = "deviceAddressBinding",
      ordinal = 30
   ), @Range(
      value = "deviceType",
      ordinal = 31
   ), @Range(
      value = "effectivePeriod",
      ordinal = 32
   ), @Range(
      value = "elapsedActiveTime",
      ordinal = 33
   ), @Range(
      value = "errorLimit",
      ordinal = 34
   ), @Range(
      value = "eventEnable",
      ordinal = 35
   ), @Range(
      value = "eventState",
      ordinal = 36
   ), @Range(
      value = "eventType",
      ordinal = 37
   ), @Range(
      value = "exceptionSchedule",
      ordinal = 38
   ), @Range(
      value = "faultValues",
      ordinal = 39
   ), @Range(
      value = "feedbackValue",
      ordinal = 40
   ), @Range(
      value = "fileAccessMethod",
      ordinal = 41
   ), @Range(
      value = "fileSize",
      ordinal = 42
   ), @Range(
      value = "fileType",
      ordinal = 43
   ), @Range(
      value = "firmwareRevision",
      ordinal = 44
   ), @Range(
      value = "highLimit",
      ordinal = 45
   ), @Range(
      value = "inactiveText",
      ordinal = 46
   ), @Range(
      value = "inProcess",
      ordinal = 47
   ), @Range(
      value = "instanceOf",
      ordinal = 48
   ), @Range(
      value = "integralConstant",
      ordinal = 49
   ), @Range(
      value = "integralConstantUnits",
      ordinal = 50
   ), @Range(
      value = "limitEnable",
      ordinal = 52
   ), @Range(
      value = "listOfGroupMembers",
      ordinal = 53
   ), @Range(
      value = "listOfObjectPropertyReferences",
      ordinal = 54
   ), @Range(
      value = "localDate",
      ordinal = 56
   ), @Range(
      value = "localTime",
      ordinal = 57
   ), @Range(
      value = "location",
      ordinal = 58
   ), @Range(
      value = "lowLimit",
      ordinal = 59
   ), @Range(
      value = "manipulatedVariableReference",
      ordinal = 60
   ), @Range(
      value = "maximumOutput",
      ordinal = 61
   ), @Range(
      value = "maxApduLengthAccepted",
      ordinal = 62
   ), @Range(
      value = "maxInfoFrames",
      ordinal = 63
   ), @Range(
      value = "maxMaster",
      ordinal = 64
   ), @Range(
      value = "maxPresValue",
      ordinal = 65
   ), @Range(
      value = "minimumOffTime",
      ordinal = 66
   ), @Range(
      value = "minimumOnTime",
      ordinal = 67
   ), @Range(
      value = "minimumOutput",
      ordinal = 68
   ), @Range(
      value = "minPresValue",
      ordinal = 69
   ), @Range(
      value = "modelName",
      ordinal = 70
   ), @Range(
      value = "modificationDate",
      ordinal = 71
   ), @Range(
      value = "notifyType",
      ordinal = 72
   ), @Range(
      value = "numberOfApduRetries",
      ordinal = 73
   ), @Range(
      value = "numberOfStates",
      ordinal = 74
   ), @Range(
      value = "objectIdentifier",
      ordinal = 75
   ), @Range(
      value = "objectList",
      ordinal = 76
   ), @Range(
      value = "objectName",
      ordinal = 77
   ), @Range(
      value = "objectPropertyReference",
      ordinal = 78
   ), @Range(
      value = "objectType",
      ordinal = 79
   ), @Range(
      value = "optional",
      ordinal = 80
   ), @Range(
      value = "outOfService",
      ordinal = 81
   ), @Range(
      value = "outputUnits",
      ordinal = 82
   ), @Range(
      value = "eventParameters",
      ordinal = 83
   ), @Range(
      value = "polarity",
      ordinal = 84
   ), @Range(
      value = "presentValue",
      ordinal = 85
   ), @Range(
      value = "priority",
      ordinal = 86
   ), @Range(
      value = "priorityArray",
      ordinal = 87
   ), @Range(
      value = "priorityForWriting",
      ordinal = 88
   ), @Range(
      value = "processIdentifier",
      ordinal = 89
   ), @Range(
      value = "programChange",
      ordinal = 90
   ), @Range(
      value = "programLocation",
      ordinal = 91
   ), @Range(
      value = "programState",
      ordinal = 92
   ), @Range(
      value = "proportionalConstant",
      ordinal = 93
   ), @Range(
      value = "proportionalConstantUnits",
      ordinal = 94
   ), @Range(
      value = "protocolObjectTypesSupported",
      ordinal = 96
   ), @Range(
      value = "protocolServicesSupported",
      ordinal = 97
   ), @Range(
      value = "protocolVersion",
      ordinal = 98
   ), @Range(
      value = "readOnly",
      ordinal = 99
   ), @Range(
      value = "reasonForHalt",
      ordinal = 100
   ), @Range(
      value = "recipientList",
      ordinal = 102
   ), @Range(
      value = "reliability",
      ordinal = 103
   ), @Range(
      value = "relinquishDefault",
      ordinal = 104
   ), @Range(
      value = "required",
      ordinal = 105
   ), @Range(
      value = "resolution",
      ordinal = 106
   ), @Range(
      value = "segmentationSupported",
      ordinal = 107
   ), @Range(
      value = "setpoint",
      ordinal = 108
   ), @Range(
      value = "setpointReference",
      ordinal = 109
   ), @Range(
      value = "stateText",
      ordinal = 110
   ), @Range(
      value = "statusFlags",
      ordinal = 111
   ), @Range(
      value = "systemStatus",
      ordinal = 112
   ), @Range(
      value = "timeDelay",
      ordinal = 113
   ), @Range(
      value = "timeOfActiveTimeReset",
      ordinal = 114
   ), @Range(
      value = "timeOfStateCountReset",
      ordinal = 115
   ), @Range(
      value = "timeSynchronizationRecipients",
      ordinal = 116
   ), @Range(
      value = "units",
      ordinal = 117
   ), @Range(
      value = "updateInterval",
      ordinal = 118
   ), @Range(
      value = "utcOffset",
      ordinal = 119
   ), @Range(
      value = "vendorIdentifier",
      ordinal = 120
   ), @Range(
      value = "vendorName",
      ordinal = 121
   ), @Range(
      value = "vtClassesSupported",
      ordinal = 122
   ), @Range(
      value = "weeklySchedule",
      ordinal = 123
   ), @Range(
      value = "attemptedSamples",
      ordinal = 124
   ), @Range(
      value = "averageValue",
      ordinal = 125
   ), @Range(
      value = "bufferSize",
      ordinal = 126
   ), @Range(
      value = "clientCovIncrement",
      ordinal = 127
   ), @Range(
      value = "covResubscriptionInterval",
      ordinal = 128
   ), @Range(
      value = "eventTimeStamps",
      ordinal = 130
   ), @Range(
      value = "logBuffer",
      ordinal = 131
   ), @Range(
      value = "logDeviceObjectProperty",
      ordinal = 132
   ), @Range(
      value = "enable",
      ordinal = 133
   ), @Range(
      value = "logInterval",
      ordinal = 134
   ), @Range(
      value = "maximumValue",
      ordinal = 135
   ), @Range(
      value = "minimumValue",
      ordinal = 136
   ), @Range(
      value = "notificationThreshold",
      ordinal = 137
   ), @Range(
      value = "protocolRevision",
      ordinal = 139
   ), @Range(
      value = "recordsSinceNotification",
      ordinal = 140
   ), @Range(
      value = "recordCount",
      ordinal = 141
   ), @Range(
      value = "startTime",
      ordinal = 142
   ), @Range(
      value = "stopTime",
      ordinal = 143
   ), @Range(
      value = "stopWhenFull",
      ordinal = 144
   ), @Range(
      value = "totalRecordCount",
      ordinal = 145
   ), @Range(
      value = "validSamples",
      ordinal = 146
   ), @Range(
      value = "windowInterval",
      ordinal = 147
   ), @Range(
      value = "windowSamples",
      ordinal = 148
   ), @Range(
      value = "maximumValueTimestamp",
      ordinal = 149
   ), @Range(
      value = "minimumValueTimestamp",
      ordinal = 150
   ), @Range(
      value = "varianceValue",
      ordinal = 151
   ), @Range(
      value = "activeCovSubscriptions",
      ordinal = 152
   ), @Range(
      value = "backupFailureTimeout",
      ordinal = 153
   ), @Range(
      value = "configurationFiles",
      ordinal = 154
   ), @Range(
      value = "databaseRevision",
      ordinal = 155
   ), @Range(
      value = "directReading",
      ordinal = 156
   ), @Range(
      value = "lastRestoreTime",
      ordinal = 157
   ), @Range(
      value = "maintenanceRequired",
      ordinal = 158
   ), @Range(
      value = "memberOf",
      ordinal = 159
   ), @Range(
      value = "mode",
      ordinal = 160
   ), @Range(
      value = "operationExpected",
      ordinal = 161
   ), @Range(
      value = "setting",
      ordinal = 162
   ), @Range(
      value = "silenced",
      ordinal = 163
   ), @Range(
      value = "trackingValue",
      ordinal = 164
   ), @Range(
      value = "zoneMembers",
      ordinal = 165
   ), @Range(
      value = "lifeSafetyAlarmValues",
      ordinal = 166
   ), @Range(
      value = "maxSegmentsAccepted",
      ordinal = 167
   ), @Range(
      value = "profileName",
      ordinal = 168
   ), @Range(
      value = "autoSlaveDiscovery",
      ordinal = 169
   ), @Range(
      value = "manualSlaveAddressBinding",
      ordinal = 170
   ), @Range(
      value = "slaveAddressBinding",
      ordinal = 171
   ), @Range(
      value = "slaveProxyEnable",
      ordinal = 172
   ), @Range(
      value = "lastNotifyRecord",
      ordinal = 173
   ), @Range(
      value = "scheduleDefault",
      ordinal = 174
   ), @Range(
      value = "acceptedModes",
      ordinal = 175
   ), @Range(
      value = "adjustValue",
      ordinal = 176
   ), @Range(
      value = "count",
      ordinal = 177
   ), @Range(
      value = "countBeforeChange",
      ordinal = 178
   ), @Range(
      value = "countChangeTime",
      ordinal = 179
   ), @Range(
      value = "covPeriod",
      ordinal = 180
   ), @Range(
      value = "inputReference",
      ordinal = 181
   ), @Range(
      value = "limitMonitoringInterval",
      ordinal = 182
   ), @Range(
      value = "loggingObject",
      ordinal = 183
   ), @Range(
      value = "loggingRecord",
      ordinal = 184
   ), @Range(
      value = "prescale",
      ordinal = 185
   ), @Range(
      value = "pulseRate",
      ordinal = 186
   ), @Range(
      value = "scale",
      ordinal = 187
   ), @Range(
      value = "scaleFactor",
      ordinal = 188
   ), @Range(
      value = "updateTime",
      ordinal = 189
   ), @Range(
      value = "valueBeforeChange",
      ordinal = 190
   ), @Range(
      value = "valueSet",
      ordinal = 191
   ), @Range(
      value = "valueChangeTime",
      ordinal = 192
   ), @Range(
      value = "alignIntervals",
      ordinal = 193
   ), @Range(
      value = "intervalOffset",
      ordinal = 195
   ), @Range(
      value = "lastRestartReason",
      ordinal = 196
   ), @Range(
      value = "loggingType",
      ordinal = 197
   ), @Range(
      value = "restartNotificationRecipients",
      ordinal = 202
   ), @Range(
      value = "timeOfDeviceRestart",
      ordinal = 203
   ), @Range(
      value = "timeSynchronizationInterval",
      ordinal = 204
   ), @Range(
      value = "trigger",
      ordinal = 205
   ), @Range(
      value = "utcTimeSynchronizationRecipients",
      ordinal = 206
   ), @Range(
      value = "nodeSubtype",
      ordinal = 207
   ), @Range(
      value = "nodeType",
      ordinal = 208
   ), @Range(
      value = "structuredObjectList",
      ordinal = 209
   ), @Range(
      value = "subordinateAnnotations",
      ordinal = 210
   ), @Range(
      value = "subordinateList",
      ordinal = 211
   ), @Range(
      value = "actualShedLevel",
      ordinal = 212
   ), @Range(
      value = "dutyWindow",
      ordinal = 213
   ), @Range(
      value = "expectedShedLevel",
      ordinal = 214
   ), @Range(
      value = "fullDutyBaseline",
      ordinal = 215
   ), @Range(
      value = "requestedShedLevel",
      ordinal = 218
   ), @Range(
      value = "shedDuration",
      ordinal = 219
   ), @Range(
      value = "shedLevelDescriptions",
      ordinal = 220
   ), @Range(
      value = "shedLevels",
      ordinal = 221
   ), @Range(
      value = "stateDescription",
      ordinal = 222
   ), @Range(
      value = "doorAlarmState",
      ordinal = 226
   ), @Range(
      value = "doorExtendedPulseTime",
      ordinal = 227
   ), @Range(
      value = "doorMembers",
      ordinal = 228
   ), @Range(
      value = "doorOpenTooLongTime",
      ordinal = 229
   ), @Range(
      value = "doorPulseTime",
      ordinal = 230
   ), @Range(
      value = "doorStatus",
      ordinal = 231
   ), @Range(
      value = "doorUnlockDelayTime",
      ordinal = 232
   ), @Range(
      value = "lockStatus",
      ordinal = 233
   ), @Range(
      value = "maskedAlarmValues",
      ordinal = 234
   ), @Range(
      value = "securedStatus",
      ordinal = 235
   ), @Range(
      value = "absenteeLimit",
      ordinal = 244
   ), @Range(
      value = "accessAlarmEvents",
      ordinal = 245
   ), @Range(
      value = "accessDoors",
      ordinal = 246
   ), @Range(
      value = "accessEvent",
      ordinal = 247
   ), @Range(
      value = "accessEventAuthenticationFactor",
      ordinal = 248
   ), @Range(
      value = "accessEventCredential",
      ordinal = 249
   ), @Range(
      value = "accessEventTime",
      ordinal = 250
   ), @Range(
      value = "accessTransactionEvents",
      ordinal = 251
   ), @Range(
      value = "accompaniment",
      ordinal = 252
   ), @Range(
      value = "accompanimentTime",
      ordinal = 253
   ), @Range(
      value = "activationTime",
      ordinal = 254
   ), @Range(
      value = "activeAuthenticationPolicy",
      ordinal = 255
   ), @Range(
      value = "assignedAccessRights",
      ordinal = 256
   ), @Range(
      value = "authenticationFactors",
      ordinal = 257
   ), @Range(
      value = "authenticationPolicyList",
      ordinal = 258
   ), @Range(
      value = "authenticationPolicyNames",
      ordinal = 259
   ), @Range(
      value = "authenticationStatus",
      ordinal = 260
   ), @Range(
      value = "authorizationMode",
      ordinal = 261
   ), @Range(
      value = "belongsTo",
      ordinal = 262
   ), @Range(
      value = "credentialDisable",
      ordinal = 263
   ), @Range(
      value = "credentialStatus",
      ordinal = 264
   ), @Range(
      value = "credentials",
      ordinal = 265
   ), @Range(
      value = "credentialsInZone",
      ordinal = 266
   ), @Range(
      value = "daysRemaining",
      ordinal = 267
   ), @Range(
      value = "entryPoints",
      ordinal = 268
   ), @Range(
      value = "exitPoints",
      ordinal = 269
   ), @Range(
      value = "expiryTime",
      ordinal = 270
   ), @Range(
      value = "extendedTimeEnable",
      ordinal = 271
   ), @Range(
      value = "failedAttemptEvents",
      ordinal = 272
   ), @Range(
      value = "failedAttempts",
      ordinal = 273
   ), @Range(
      value = "failedAttemptsTime",
      ordinal = 274
   ), @Range(
      value = "lastAccessEvent",
      ordinal = 275
   ), @Range(
      value = "lastAccessPoint",
      ordinal = 276
   ), @Range(
      value = "lastCredentialAdded",
      ordinal = 277
   ), @Range(
      value = "lastCredentialAddedTime",
      ordinal = 278
   ), @Range(
      value = "lastCredentialRemoved",
      ordinal = 279
   ), @Range(
      value = "lastCredentialRemovedTime",
      ordinal = 280
   ), @Range(
      value = "lastUseTime",
      ordinal = 281
   ), @Range(
      value = "lockout",
      ordinal = 282
   ), @Range(
      value = "lockoutRelinquishTime",
      ordinal = 283
   ), @Range(
      value = "maxFailedAttempts",
      ordinal = 285
   ), @Range(
      value = "members",
      ordinal = 286
   ), @Range(
      value = "musterPoint",
      ordinal = 287
   ), @Range(
      value = "negativeAccessRules",
      ordinal = 288
   ), @Range(
      value = "numberOfAuthenticationPolicies",
      ordinal = 289
   ), @Range(
      value = "occupancyCount",
      ordinal = 290
   ), @Range(
      value = "occupancyCountAdjust",
      ordinal = 291
   ), @Range(
      value = "occupancyCountEnable",
      ordinal = 292
   ), @Range(
      value = "occupancyLowerLimit",
      ordinal = 294
   ), @Range(
      value = "occupancyLowerLimitEnforced",
      ordinal = 295
   ), @Range(
      value = "occupancyState",
      ordinal = 296
   ), @Range(
      value = "occupancyUpperLimit",
      ordinal = 297
   ), @Range(
      value = "occupancyUpperLimitEnforced",
      ordinal = 298
   ), @Range(
      value = "passbackMode",
      ordinal = 300
   ), @Range(
      value = "passbackTimeout",
      ordinal = 301
   ), @Range(
      value = "positiveAccessRules",
      ordinal = 302
   ), @Range(
      value = "reasonForDisable",
      ordinal = 303
   ), @Range(
      value = "supportedFormats",
      ordinal = 304
   ), @Range(
      value = "supportedFormatClasses",
      ordinal = 305
   ), @Range(
      value = "threatAuthority",
      ordinal = 306
   ), @Range(
      value = "threatLevel",
      ordinal = 307
   ), @Range(
      value = "traceFlag",
      ordinal = 308
   ), @Range(
      value = "transactionNotificationClass",
      ordinal = 309
   ), @Range(
      value = "userExternalIdentifier",
      ordinal = 310
   ), @Range(
      value = "userInformationReference",
      ordinal = 311
   ), @Range(
      value = "userName",
      ordinal = 317
   ), @Range(
      value = "userType",
      ordinal = 318
   ), @Range(
      value = "usesRemaining",
      ordinal = 319
   ), @Range(
      value = "zoneFrom",
      ordinal = 320
   ), @Range(
      value = "zoneTo",
      ordinal = 321
   ), @Range(
      value = "accessEventTag",
      ordinal = 322
   ), @Range(
      value = "globalIdentifier",
      ordinal = 323
   ), @Range(
      value = "verificationTime",
      ordinal = 326
   ), @Range(
      value = "backupAndRestoreState",
      ordinal = 338
   ), @Range(
      value = "backupPreparationTime",
      ordinal = 339
   ), @Range(
      value = "restoreCompletionTime",
      ordinal = 340
   ), @Range(
      value = "restorePreparationTime",
      ordinal = 341
   ), @Range(
      value = "bitMask",
      ordinal = 342
   ), @Range(
      value = "bitText",
      ordinal = 343
   ), @Range(
      value = "isUtc",
      ordinal = 344
   ), @Range(
      value = "groupMembers",
      ordinal = 345
   ), @Range(
      value = "groupMemberNames",
      ordinal = 346
   ), @Range(
      value = "memberStatusDlags",
      ordinal = 347
   ), @Range(
      value = "requestedUpdateInterval",
      ordinal = 348
   ), @Range(
      value = "covuPeriod",
      ordinal = 349
   ), @Range(
      value = "covuRecipients",
      ordinal = 350
   ), @Range(
      value = "eventMessageTexts",
      ordinal = 351
   ), @Range(
      value = "eventMessageTextsConfig",
      ordinal = 352
   ), @Range(
      value = "eventDetectionEnable",
      ordinal = 353
   ), @Range(
      value = "eventAlgorithmInhibit",
      ordinal = 354
   ), @Range(
      value = "eventAlgorithmInhibitRef",
      ordinal = 355
   ), @Range(
      value = "timeDelayNormal",
      ordinal = 356
   ), @Range(
      value = "reliabilityEvaluationInhibit",
      ordinal = 357
   ), @Range(
      value = "faultParameters",
      ordinal = 358
   ), @Range(
      value = "faultType",
      ordinal = 359
   ), @Range(
      value = "localForwardingOnly",
      ordinal = 360
   ), @Range(
      value = "processIdentifierFilter",
      ordinal = 361
   ), @Range(
      value = "subscribedRecipients",
      ordinal = 362
   ), @Range(
      value = "portFilter",
      ordinal = 363
   ), @Range(
      value = "authorizationExemptions",
      ordinal = 364
   ), @Range(
      value = "allowGroupDelayInhibit",
      ordinal = 365
   ), @Range(
      value = "channelNumber",
      ordinal = 366
   ), @Range(
      value = "controlGroups",
      ordinal = 367
   ), @Range(
      value = "executionDelay",
      ordinal = 368
   ), @Range(
      value = "lastPriority",
      ordinal = 369
   ), @Range(
      value = "writeStatus",
      ordinal = 370
   ), @Range(
      value = "propertyList",
      ordinal = 371
   ), @Range(
      value = "serialNumber",
      ordinal = 372
   ), @Range(
      value = "blinkWarnEnable",
      ordinal = 373
   ), @Range(
      value = "defaultFadetime",
      ordinal = 374
   ), @Range(
      value = "defaultRamprate",
      ordinal = 375
   ), @Range(
      value = "defaultStepIncrement",
      ordinal = 376
   ), @Range(
      value = "egressTime",
      ordinal = 377
   ), @Range(
      value = "inProgress",
      ordinal = 378
   ), @Range(
      value = "instantaneousPower",
      ordinal = 379
   ), @Range(
      value = "lightingCommand",
      ordinal = 380
   ), @Range(
      value = "lightingCommandDefaultPriority",
      ordinal = 381
   ), @Range(
      value = "maxActualValue",
      ordinal = 382
   ), @Range(
      value = "minActualValue",
      ordinal = 383
   ), @Range(
      value = "power",
      ordinal = 384
   ), @Range(
      value = "transition",
      ordinal = 385
   ), @Range(
      value = "egressActive",
      ordinal = 386
   ), @Range(
      value = "interfaceValue",
      ordinal = 387
   ), @Range(
      value = "faultHighLimit",
      ordinal = 388
   ), @Range(
      value = "faultLowLimit",
      ordinal = 389
   ), @Range(
      value = "lowDiffLimit",
      ordinal = 390
   ), @Range(
      value = "strikeCount",
      ordinal = 391
   ), @Range(
      value = "timeOfStrikeCountReset",
      ordinal = 392
   ), @Range(
      value = "defaultTimeout",
      ordinal = 393
   ), @Range(
      value = "initialTimeout",
      ordinal = 394
   ), @Range(
      value = "lastStateChange",
      ordinal = 395
   ), @Range(
      value = "stateChangeValues",
      ordinal = 396
   ), @Range(
      value = "timerRunning",
      ordinal = 397
   ), @Range(
      value = "timerState",
      ordinal = 398
   ), @Range(
      value = "apduLength",
      ordinal = 399
   ), @Range(
      value = "ipAddress",
      ordinal = 400
   ), @Range(
      value = "ipDefaultGateway",
      ordinal = 401
   ), @Range(
      value = "ipDhcpEnable",
      ordinal = 402
   ), @Range(
      value = "ipDhcpLeaseTime",
      ordinal = 403
   ), @Range(
      value = "ipDhcpLeaseTimeRemaining",
      ordinal = 404
   ), @Range(
      value = "ipDhcpServer",
      ordinal = 405
   ), @Range(
      value = "ipDnsServer",
      ordinal = 406
   ), @Range(
      value = "bacnetIpGlobalAddress",
      ordinal = 407
   ), @Range(
      value = "bacnetIpMode",
      ordinal = 408
   ), @Range(
      value = "bacnetIpMulticastAddress",
      ordinal = 409
   ), @Range(
      value = "bacnetIpNatTraversal",
      ordinal = 410
   ), @Range(
      value = "ipSubnetMask",
      ordinal = 411
   ), @Range(
      value = "bacnetIpUdpPort",
      ordinal = 412
   ), @Range(
      value = "bbmdAcceptFdRegistrations",
      ordinal = 413
   ), @Range(
      value = "bbmdBroadcastDistributionTable",
      ordinal = 414
   ), @Range(
      value = "bbmdForeignDeviceTable",
      ordinal = 415
   ), @Range(
      value = "changesPending",
      ordinal = 416
   ), @Range(
      value = "command",
      ordinal = 417
   ), @Range(
      value = "fdBbmdAddress",
      ordinal = 418
   ), @Range(
      value = "fdSubscriptionLifetime",
      ordinal = 419
   ), @Range(
      value = "linkSpeed",
      ordinal = 420
   ), @Range(
      value = "linkSpeeds",
      ordinal = 421
   ), @Range(
      value = "linkSpeedAutonegotiate",
      ordinal = 422
   ), @Range(
      value = "macAddress",
      ordinal = 423
   ), @Range(
      value = "networkInterfaceName",
      ordinal = 424
   ), @Range(
      value = "networkNumber",
      ordinal = 425
   ), @Range(
      value = "networkNumberQuality",
      ordinal = 426
   ), @Range(
      value = "networkType",
      ordinal = 427
   ), @Range(
      value = "routingTable",
      ordinal = 428
   ), @Range(
      value = "virtualMacAddressTable",
      ordinal = 429
   ), @Range(
      value = "commandTimeArray",
      ordinal = 430
   ), @Range(
      value = "currentCommandPriority",
      ordinal = 431
   ), @Range(
      value = "lastCommandTime",
      ordinal = 432
   ), @Range(
      value = "valueSource",
      ordinal = 433
   ), @Range(
      value = "valueSourceArray",
      ordinal = 434
   ), @Range(
      value = "bacnetIpv6Mode",
      ordinal = 435
   ), @Range(
      value = "ipv6Address",
      ordinal = 436
   ), @Range(
      value = "ipv6PrefixLength",
      ordinal = 437
   ), @Range(
      value = "bacnetIpv6UdpPort",
      ordinal = 438
   ), @Range(
      value = "ipv6DefaultGateway",
      ordinal = 439
   ), @Range(
      value = "bacnetIpv6MulticastAddress",
      ordinal = 440
   ), @Range(
      value = "ipv6DnsServer",
      ordinal = 441
   ), @Range(
      value = "ipv6AutoAddressingEnable",
      ordinal = 442
   ), @Range(
      value = "ipv6DhcpLeaseTime",
      ordinal = 443
   ), @Range(
      value = "ipv6DhcpLeaseTimeRemaining",
      ordinal = 444
   ), @Range(
      value = "ipv6DhcpServer",
      ordinal = 445
   ), @Range(
      value = "ipv6ZoneIndex",
      ordinal = 446
   ), @Range(
      value = "assignedLandingCalls",
      ordinal = 447
   ), @Range(
      value = "carAssignedDirection",
      ordinal = 448
   ), @Range(
      value = "carDoorCommand",
      ordinal = 449
   ), @Range(
      value = "carDoorStatus",
      ordinal = 450
   ), @Range(
      value = "carDoorText",
      ordinal = 451
   ), @Range(
      value = "carDoorZone",
      ordinal = 452
   ), @Range(
      value = "carDriveStatus",
      ordinal = 453
   ), @Range(
      value = "carLoad",
      ordinal = 454
   ), @Range(
      value = "carLoadUnits",
      ordinal = 455
   ), @Range(
      value = "carMode",
      ordinal = 456
   ), @Range(
      value = "carMovingDirection",
      ordinal = 457
   ), @Range(
      value = "carPosition",
      ordinal = 458
   ), @Range(
      value = "elevatorGroup",
      ordinal = 459
   ), @Range(
      value = "energyMeter",
      ordinal = 460
   ), @Range(
      value = "energyMeterRef",
      ordinal = 461
   ), @Range(
      value = "escalatorMode",
      ordinal = 462
   ), @Range(
      value = "faultSignals",
      ordinal = 463
   ), @Range(
      value = "floorText",
      ordinal = 464
   ), @Range(
      value = "groupId",
      ordinal = 465
   ), @Range(
      value = "groupMode",
      ordinal = 467
   ), @Range(
      value = "higherDeck",
      ordinal = 468
   ), @Range(
      value = "installationId",
      ordinal = 469
   ), @Range(
      value = "landingCalls",
      ordinal = 470
   ), @Range(
      value = "landingCallControl",
      ordinal = 471
   ), @Range(
      value = "landingDoorStatus",
      ordinal = 472
   ), @Range(
      value = "lowerDeck",
      ordinal = 473
   ), @Range(
      value = "machineRoomId",
      ordinal = 474
   ), @Range(
      value = "makingCarCall",
      ordinal = 475
   ), @Range(
      value = "nextStoppingFloor",
      ordinal = 476
   ), @Range(
      value = "operationDirection",
      ordinal = 477
   ), @Range(
      value = "passengerAlarm",
      ordinal = 478
   ), @Range(
      value = "powerMode",
      ordinal = 479
   ), @Range(
      value = "registeredCarCall",
      ordinal = 480
   ), @Range(
      value = "activeCovMultipleSubscriptions",
      ordinal = 481
   ), @Range(
      value = "protocolLevel",
      ordinal = 482
   ), @Range(
      value = "referencePort",
      ordinal = 483
   ), @Range(
      value = "deployedProfileLocation",
      ordinal = 484
   ), @Range(
      value = "profileLocation",
      ordinal = 485
   ), @Range(
      value = "tags",
      ordinal = 486
   ), @Range(
      value = "subordinateNodeTypes",
      ordinal = 487
   ), @Range(
      value = "subordinateTags",
      ordinal = 488
   ), @Range(
      value = "subordinateRelationships",
      ordinal = 489
   ), @Range(
      value = "defaultSubordinateRelationship",
      ordinal = 490
   ), @Range(
      value = "represents",
      ordinal = 491
   ), @Range(
      value = "defaultPresentValue",
      ordinal = 492
   ), @Range(
      value = "presentStage",
      ordinal = 493
   ), @Range(
      value = "stages",
      ordinal = 494
   ), @Range(
      value = "stageNames",
      ordinal = 495
   ), @Range(
      value = "targetReferences",
      ordinal = 496
   ), @Range(
      value = "auditSourceReporter",
      ordinal = 497
   ), @Range(
      value = "auditLevel",
      ordinal = 498
   ), @Range(
      value = "auditNotificationRecipient",
      ordinal = 499
   ), @Range(
      value = "auditPriorityFilter",
      ordinal = 500
   ), @Range(
      value = "auditableOperations",
      ordinal = 501
   ), @Range(
      value = "deleteOnForward",
      ordinal = 502
   ), @Range(
      value = "maximumSendDelay",
      ordinal = 503
   ), @Range(
      value = "monitoredObjects",
      ordinal = 504
   ), @Range(
      value = "sendNow",
      ordinal = 505
   ), @Range(
      value = "floorNumber",
      ordinal = 506
   ), @Range(
      value = "deviceUuid",
      ordinal = 507
   ), @Range(
      value = "removed1",
      ordinal = 18
   ), @Range(
      value = "issueConfirmedNotifications",
      ordinal = 51
   ), @Range(
      value = "listOfSessionKeys",
      ordinal = 55
   ), @Range(
      value = "protocolConformanceClass",
      ordinal = 95
   ), @Range(
      value = "recipient",
      ordinal = 101
   ), @Range(
      value = "currentNotifyTime",
      ordinal = 129
   ), @Range(
      value = "previousNotifyTime",
      ordinal = 138
   ), @Range(
      value = "masterExemption",
      ordinal = 284
   ), @Range(
      value = "occupancyExemption",
      ordinal = 293
   ), @Range(
      value = "passbackExemption",
      ordinal = 299
   )}
)
public final class BBacnetPropertyIdentifier extends BFrozenEnum implements BacnetConst {
   public static final int ACKED_TRANSITIONS = 0;
   public static final int ACK_REQUIRED = 1;
   public static final int ACTION = 2;
   public static final int ACTION_TEXT = 3;
   public static final int ACTIVE_TEXT = 4;
   public static final int ACTIVE_VT_SESSIONS = 5;
   public static final int ALARM_VALUE = 6;
   public static final int ALARM_VALUES = 7;
   public static final int ALL = 8;
   public static final int ALL_WRITES_SUCCESSFUL = 9;
   public static final int APDU_SEGMENT_TIMEOUT = 10;
   public static final int APDU_TIMEOUT = 11;
   public static final int APPLICATION_SOFTWARE_VERSION = 12;
   public static final int ARCHIVE = 13;
   public static final int BIAS = 14;
   public static final int CHANGE_OF_STATE_COUNT = 15;
   public static final int CHANGE_OF_STATE_TIME = 16;
   public static final int NOTIFICATION_CLASS = 17;
   public static final int CONTROLLED_VARIABLE_REFERENCE = 19;
   public static final int CONTROLLED_VARIABLE_UNITS = 20;
   public static final int CONTROLLED_VARIABLE_VALUE = 21;
   public static final int COV_INCREMENT = 22;
   public static final int DATE_LIST = 23;
   public static final int DAYLIGHT_SAVINGS_STATUS = 24;
   public static final int DEADBAND = 25;
   public static final int DERIVATIVE_CONSTANT = 26;
   public static final int DERIVATIVE_CONSTANT_UNITS = 27;
   public static final int DESCRIPTION = 28;
   public static final int DESCRIPTION_OF_HALT = 29;
   public static final int DEVICE_ADDRESS_BINDING = 30;
   public static final int DEVICE_TYPE = 31;
   public static final int EFFECTIVE_PERIOD = 32;
   public static final int ELAPSED_ACTIVE_TIME = 33;
   public static final int ERROR_LIMIT = 34;
   public static final int EVENT_ENABLE = 35;
   public static final int EVENT_STATE = 36;
   public static final int EVENT_TYPE = 37;
   public static final int EXCEPTION_SCHEDULE = 38;
   public static final int FAULT_VALUES = 39;
   public static final int FEEDBACK_VALUE = 40;
   public static final int FILE_ACCESS_METHOD = 41;
   public static final int FILE_SIZE = 42;
   public static final int FILE_TYPE = 43;
   public static final int FIRMWARE_REVISION = 44;
   public static final int HIGH_LIMIT = 45;
   public static final int INACTIVE_TEXT = 46;
   public static final int IN_PROCESS = 47;
   public static final int INSTANCE_OF = 48;
   public static final int INTEGRAL_CONSTANT = 49;
   public static final int INTEGRAL_CONSTANT_UNITS = 50;
   public static final int LIMIT_ENABLE = 52;
   public static final int LIST_OF_GROUP_MEMBERS = 53;
   public static final int LIST_OF_OBJECT_PROPERTY_REFERENCES = 54;
   public static final int LOCAL_DATE = 56;
   public static final int LOCAL_TIME = 57;
   public static final int LOCATION = 58;
   public static final int LOW_LIMIT = 59;
   public static final int MANIPULATED_VARIABLE_REFERENCE = 60;
   public static final int MAXIMUM_OUTPUT = 61;
   public static final int MAX_APDU_LENGTH_ACCEPTED = 62;
   public static final int MAX_INFO_FRAMES = 63;
   public static final int MAX_MASTER = 64;
   public static final int MAX_PRES_VALUE = 65;
   public static final int MINIMUM_OFF_TIME = 66;
   public static final int MINIMUM_ON_TIME = 67;
   public static final int MINIMUM_OUTPUT = 68;
   public static final int MIN_PRES_VALUE = 69;
   public static final int MODEL_NAME = 70;
   public static final int MODIFICATION_DATE = 71;
   public static final int NOTIFY_TYPE = 72;
   public static final int NUMBER_OF_APDU_RETRIES = 73;
   public static final int NUMBER_OF_STATES = 74;
   public static final int OBJECT_IDENTIFIER = 75;
   public static final int OBJECT_LIST = 76;
   public static final int OBJECT_NAME = 77;
   public static final int OBJECT_PROPERTY_REFERENCE = 78;
   public static final int OBJECT_TYPE = 79;
   public static final int OPTIONAL = 80;
   public static final int OUT_OF_SERVICE = 81;
   public static final int OUTPUT_UNITS = 82;
   public static final int EVENT_PARAMETERS = 83;
   public static final int POLARITY = 84;
   public static final int PRESENT_VALUE = 85;
   public static final int PRIORITY = 86;
   public static final int PRIORITY_ARRAY = 87;
   public static final int PRIORITY_FOR_WRITING = 88;
   public static final int PROCESS_IDENTIFIER = 89;
   public static final int PROGRAM_CHANGE = 90;
   public static final int PROGRAM_LOCATION = 91;
   public static final int PROGRAM_STATE = 92;
   public static final int PROPORTIONAL_CONSTANT = 93;
   public static final int PROPORTIONAL_CONSTANT_UNITS = 94;
   public static final int PROTOCOL_OBJECT_TYPES_SUPPORTED = 96;
   public static final int PROTOCOL_SERVICES_SUPPORTED = 97;
   public static final int PROTOCOL_VERSION = 98;
   public static final int READ_ONLY = 99;
   public static final int REASON_FOR_HALT = 100;
   public static final int RECIPIENT_LIST = 102;
   public static final int RELIABILITY = 103;
   public static final int RELINQUISH_DEFAULT = 104;
   public static final int REQUIRED = 105;
   public static final int RESOLUTION = 106;
   public static final int SEGMENTATION_SUPPORTED = 107;
   public static final int SETPOINT = 108;
   public static final int SETPOINT_REFERENCE = 109;
   public static final int STATE_TEXT = 110;
   public static final int STATUS_FLAGS = 111;
   public static final int SYSTEM_STATUS = 112;
   public static final int TIME_DELAY = 113;
   public static final int TIME_OF_ACTIVE_TIME_RESET = 114;
   public static final int TIME_OF_STATE_COUNT_RESET = 115;
   public static final int TIME_SYNCHRONIZATION_RECIPIENTS = 116;
   public static final int UNITS = 117;
   public static final int UPDATE_INTERVAL = 118;
   public static final int UTC_OFFSET = 119;
   public static final int VENDOR_IDENTIFIER = 120;
   public static final int VENDOR_NAME = 121;
   public static final int VT_CLASSES_SUPPORTED = 122;
   public static final int WEEKLY_SCHEDULE = 123;
   public static final int ATTEMPTED_SAMPLES = 124;
   public static final int AVERAGE_VALUE = 125;
   public static final int BUFFER_SIZE = 126;
   public static final int CLIENT_COV_INCREMENT = 127;
   public static final int COV_RESUBSCRIPTION_INTERVAL = 128;
   public static final int EVENT_TIME_STAMPS = 130;
   public static final int LOG_BUFFER = 131;
   public static final int LOG_DEVICE_OBJECT_PROPERTY = 132;
   public static final int ENABLE = 133;
   public static final int LOG_INTERVAL = 134;
   public static final int MAXIMUM_VALUE = 135;
   public static final int MINIMUM_VALUE = 136;
   public static final int NOTIFICATION_THRESHOLD = 137;
   public static final int PROTOCOL_REVISION = 139;
   public static final int RECORDS_SINCE_NOTIFICATION = 140;
   public static final int RECORD_COUNT = 141;
   public static final int START_TIME = 142;
   public static final int STOP_TIME = 143;
   public static final int STOP_WHEN_FULL = 144;
   public static final int TOTAL_RECORD_COUNT = 145;
   public static final int VALID_SAMPLES = 146;
   public static final int WINDOW_INTERVAL = 147;
   public static final int WINDOW_SAMPLES = 148;
   public static final int MAXIMUM_VALUE_TIMESTAMP = 149;
   public static final int MINIMUM_VALUE_TIMESTAMP = 150;
   public static final int VARIANCE_VALUE = 151;
   public static final int ACTIVE_COV_SUBSCRIPTIONS = 152;
   public static final int BACKUP_FAILURE_TIMEOUT = 153;
   public static final int CONFIGURATION_FILES = 154;
   public static final int DATABASE_REVISION = 155;
   public static final int DIRECT_READING = 156;
   public static final int LAST_RESTORE_TIME = 157;
   public static final int MAINTENANCE_REQUIRED = 158;
   public static final int MEMBER_OF = 159;
   public static final int MODE = 160;
   public static final int OPERATION_EXPECTED = 161;
   public static final int SETTING = 162;
   public static final int SILENCED = 163;
   public static final int TRACKING_VALUE = 164;
   public static final int ZONE_MEMBERS = 165;
   public static final int LIFE_SAFETY_ALARM_VALUES = 166;
   public static final int MAX_SEGMENTS_ACCEPTED = 167;
   public static final int PROFILE_NAME = 168;
   public static final int AUTO_SLAVE_DISCOVERY = 169;
   public static final int MANUAL_SLAVE_ADDRESS_BINDING = 170;
   public static final int SLAVE_ADDRESS_BINDING = 171;
   public static final int SLAVE_PROXY_ENABLE = 172;
   public static final int LAST_NOTIFY_RECORD = 173;
   public static final int SCHEDULE_DEFAULT = 174;
   public static final int ACCEPTED_MODES = 175;
   public static final int ADJUST_VALUE = 176;
   public static final int COUNT = 177;
   public static final int COUNT_BEFORE_CHANGE = 178;
   public static final int COUNT_CHANGE_TIME = 179;
   public static final int COV_PERIOD = 180;
   public static final int INPUT_REFERENCE = 181;
   public static final int LIMIT_MONITORING_INTERVAL = 182;
   public static final int LOGGING_OBJECT = 183;
   public static final int LOGGING_RECORD = 184;
   public static final int PRESCALE = 185;
   public static final int PULSE_RATE = 186;
   public static final int SCALE = 187;
   public static final int SCALE_FACTOR = 188;
   public static final int UPDATE_TIME = 189;
   public static final int VALUE_BEFORE_CHANGE = 190;
   public static final int VALUE_SET = 191;
   public static final int VALUE_CHANGE_TIME = 192;
   public static final int ALIGN_INTERVALS = 193;
   public static final int INTERVAL_OFFSET = 195;
   public static final int LAST_RESTART_REASON = 196;
   public static final int LOGGING_TYPE = 197;
   public static final int RESTART_NOTIFICATION_RECIPIENTS = 202;
   public static final int TIME_OF_DEVICE_RESTART = 203;
   public static final int TIME_SYNCHRONIZATION_INTERVAL = 204;
   public static final int TRIGGER = 205;
   public static final int UTC_TIME_SYNCHRONIZATION_RECIPIENTS = 206;
   public static final int NODE_SUBTYPE = 207;
   public static final int NODE_TYPE = 208;
   public static final int STRUCTURED_OBJECT_LIST = 209;
   public static final int SUBORDINATE_ANNOTATIONS = 210;
   public static final int SUBORDINATE_LIST = 211;
   public static final int ACTUAL_SHED_LEVEL = 212;
   public static final int DUTY_WINDOW = 213;
   public static final int EXPECTED_SHED_LEVEL = 214;
   public static final int FULL_DUTY_BASELINE = 215;
   public static final int REQUESTED_SHED_LEVEL = 218;
   public static final int SHED_DURATION = 219;
   public static final int SHED_LEVEL_DESCRIPTIONS = 220;
   public static final int SHED_LEVELS = 221;
   public static final int STATE_DESCRIPTION = 222;
   public static final int DOOR_ALARM_STATE = 226;
   public static final int DOOR_EXTENDED_PULSE_TIME = 227;
   public static final int DOOR_MEMBERS = 228;
   public static final int DOOR_OPEN_TOO_LONG_TIME = 229;
   public static final int DOOR_PULSE_TIME = 230;
   public static final int DOOR_STATUS = 231;
   public static final int DOOR_UNLOCK_DELAY_TIME = 232;
   public static final int LOCK_STATUS = 233;
   public static final int MASKED_ALARM_VALUES = 234;
   public static final int SECURED_STATUS = 235;
   public static final int ABSENTEE_LIMIT = 244;
   public static final int ACCESS_ALARM_EVENTS = 245;
   public static final int ACCESS_DOORS = 246;
   public static final int ACCESS_EVENT = 247;
   public static final int ACCESS_EVENT_AUTHENTICATION_FACTOR = 248;
   public static final int ACCESS_EVENT_CREDENTIAL = 249;
   public static final int ACCESS_EVENT_TIME = 250;
   public static final int ACCESS_TRANSACTION_EVENTS = 251;
   public static final int ACCOMPANIMENT = 252;
   public static final int ACCOMPANIMENT_TIME = 253;
   public static final int ACTIVATION_TIME = 254;
   public static final int ACTIVE_AUTHENTICATION_POLICY = 255;
   public static final int ASSIGNED_ACCESS_RIGHTS = 256;
   public static final int AUTHENTICATION_FACTORS = 257;
   public static final int AUTHENTICATION_POLICY_LIST = 258;
   public static final int AUTHENTICATION_POLICY_NAMES = 259;
   public static final int AUTHENTICATION_STATUS = 260;
   public static final int AUTHORIZATION_MODE = 261;
   public static final int BELONGS_TO = 262;
   public static final int CREDENTIAL_DISABLE = 263;
   public static final int CREDENTIAL_STATUS = 264;
   public static final int CREDENTIALS = 265;
   public static final int CREDENTIALS_IN_ZONE = 266;
   public static final int DAYS_REMAINING = 267;
   public static final int ENTRY_POINTS = 268;
   public static final int EXIT_POINTS = 269;
   public static final int EXPIRY_TIME = 270;
   public static final int EXTENDED_TIME_ENABLE = 271;
   public static final int FAILED_ATTEMPT_EVENTS = 272;
   public static final int FAILED_ATTEMPTS = 273;
   public static final int FAILED_ATTEMPTS_TIME = 274;
   public static final int LAST_ACCESS_EVENT = 275;
   public static final int LAST_ACCESS_POINT = 276;
   public static final int LAST_CREDENTIAL_ADDED = 277;
   public static final int LAST_CREDENTIAL_ADDED_TIME = 278;
   public static final int LAST_CREDENTIAL_REMOVED = 279;
   public static final int LAST_CREDENTIAL_REMOVED_TIME = 280;
   public static final int LAST_USE_TIME = 281;
   public static final int LOCKOUT = 282;
   public static final int LOCKOUT_RELINQUISH_TIME = 283;
   public static final int MAX_FAILED_ATTEMPTS = 285;
   public static final int MEMBERS = 286;
   public static final int MUSTER_POINT = 287;
   public static final int NEGATIVE_ACCESS_RULES = 288;
   public static final int NUMBER_OF_AUTHENTICATION_POLICIES = 289;
   public static final int OCCUPANCY_COUNT = 290;
   public static final int OCCUPANCY_COUNT_ADJUST = 291;
   public static final int OCCUPANCY_COUNT_ENABLE = 292;
   public static final int OCCUPANCY_LOWER_LIMIT = 294;
   public static final int OCCUPANCY_LOWER_LIMIT_ENFORCED = 295;
   public static final int OCCUPANCY_STATE = 296;
   public static final int OCCUPANCY_UPPER_LIMIT = 297;
   public static final int OCCUPANCY_UPPER_LIMIT_ENFORCED = 298;
   public static final int PASSBACK_MODE = 300;
   public static final int PASSBACK_TIMEOUT = 301;
   public static final int POSITIVE_ACCESS_RULES = 302;
   public static final int REASON_FOR_DISABLE = 303;
   public static final int SUPPORTED_FORMATS = 304;
   public static final int SUPPORTED_FORMAT_CLASSES = 305;
   public static final int THREAT_AUTHORITY = 306;
   public static final int THREAT_LEVEL = 307;
   public static final int TRACE_FLAG = 308;
   public static final int TRANSACTION_NOTIFICATION_CLASS = 309;
   public static final int USER_EXTERNAL_IDENTIFIER = 310;
   public static final int USER_INFORMATION_REFERENCE = 311;
   public static final int USER_NAME = 317;
   public static final int USER_TYPE = 318;
   public static final int USES_REMAINING = 319;
   public static final int ZONE_FROM = 320;
   public static final int ZONE_TO = 321;
   public static final int ACCESS_EVENT_TAG = 322;
   public static final int GLOBAL_IDENTIFIER = 323;
   public static final int VERIFICATION_TIME = 326;
   public static final int BACKUP_AND_RESTORE_STATE = 338;
   public static final int BACKUP_PREPARATION_TIME = 339;
   public static final int RESTORE_COMPLETION_TIME = 340;
   public static final int RESTORE_PREPARATION_TIME = 341;
   public static final int BIT_MASK = 342;
   public static final int BIT_TEXT = 343;
   public static final int IS_UTC = 344;
   public static final int GROUP_MEMBERS = 345;
   public static final int GROUP_MEMBER_NAMES = 346;
   public static final int MEMBER_STATUS_DLAGS = 347;
   public static final int REQUESTED_UPDATE_INTERVAL = 348;
   public static final int COVU_PERIOD = 349;
   public static final int COVU_RECIPIENTS = 350;
   public static final int EVENT_MESSAGE_TEXTS = 351;
   public static final int EVENT_MESSAGE_TEXTS_CONFIG = 352;
   public static final int EVENT_DETECTION_ENABLE = 353;
   public static final int EVENT_ALGORITHM_INHIBIT = 354;
   public static final int EVENT_ALGORITHM_INHIBIT_REF = 355;
   public static final int TIME_DELAY_NORMAL = 356;
   public static final int RELIABILITY_EVALUATION_INHIBIT = 357;
   public static final int FAULT_PARAMETERS = 358;
   public static final int FAULT_TYPE = 359;
   public static final int LOCAL_FORWARDING_ONLY = 360;
   public static final int PROCESS_IDENTIFIER_FILTER = 361;
   public static final int SUBSCRIBED_RECIPIENTS = 362;
   public static final int PORT_FILTER = 363;
   public static final int AUTHORIZATION_EXEMPTIONS = 364;
   public static final int ALLOW_GROUP_DELAY_INHIBIT = 365;
   public static final int CHANNEL_NUMBER = 366;
   public static final int CONTROL_GROUPS = 367;
   public static final int EXECUTION_DELAY = 368;
   public static final int LAST_PRIORITY = 369;
   public static final int WRITE_STATUS = 370;
   public static final int PROPERTY_LIST = 371;
   public static final int SERIAL_NUMBER = 372;
   public static final int BLINK_WARN_ENABLE = 373;
   public static final int DEFAULT_FADETIME = 374;
   public static final int DEFAULT_RAMPRATE = 375;
   public static final int DEFAULT_STEP_INCREMENT = 376;
   public static final int EGRESS_TIME = 377;
   public static final int IN_PROGRESS = 378;
   public static final int INSTANTANEOUS_POWER = 379;
   public static final int LIGHTING_COMMAND = 380;
   public static final int LIGHTING_COMMAND_DEFAULT_PRIORITY = 381;
   public static final int MAX_ACTUAL_VALUE = 382;
   public static final int MIN_ACTUAL_VALUE = 383;
   public static final int POWER = 384;
   public static final int TRANSITION = 385;
   public static final int EGRESS_ACTIVE = 386;
   public static final int INTERFACE_VALUE = 387;
   public static final int FAULT_HIGH_LIMIT = 388;
   public static final int FAULT_LOW_LIMIT = 389;
   public static final int LOW_DIFF_LIMIT = 390;
   public static final int STRIKE_COUNT = 391;
   public static final int TIME_OF_STRIKE_COUNT_RESET = 392;
   public static final int DEFAULT_TIMEOUT = 393;
   public static final int INITIAL_TIMEOUT = 394;
   public static final int LAST_STATE_CHANGE = 395;
   public static final int STATE_CHANGE_VALUES = 396;
   public static final int TIMER_RUNNING = 397;
   public static final int TIMER_STATE = 398;
   public static final int APDU_LENGTH = 399;
   public static final int IP_ADDRESS = 400;
   public static final int IP_DEFAULT_GATEWAY = 401;
   public static final int IP_DHCP_ENABLE = 402;
   public static final int IP_DHCP_LEASE_TIME = 403;
   public static final int IP_DHCP_LEASE_TIME_REMAINING = 404;
   public static final int IP_DHCP_SERVER = 405;
   public static final int IP_DNS_SERVER = 406;
   public static final int BACNET_IP_GLOBAL_ADDRESS = 407;
   public static final int BACNET_IP_MODE = 408;
   public static final int BACNET_IP_MULTICAST_ADDRESS = 409;
   public static final int BACNET_IP_NAT_TRAVERSAL = 410;
   public static final int IP_SUBNET_MASK = 411;
   public static final int BACNET_IP_UDP_PORT = 412;
   public static final int BBMD_ACCEPT_FD_REGISTRATIONS = 413;
   public static final int BBMD_BROADCAST_DISTRIBUTION_TABLE = 414;
   public static final int BBMD_FOREIGN_DEVICE_TABLE = 415;
   public static final int CHANGES_PENDING = 416;
   public static final int COMMAND = 417;
   public static final int FD_BBMD_ADDRESS = 418;
   public static final int FD_SUBSCRIPTION_LIFETIME = 419;
   public static final int LINK_SPEED = 420;
   public static final int LINK_SPEEDS = 421;
   public static final int LINK_SPEED_AUTONEGOTIATE = 422;
   public static final int MAC_ADDRESS = 423;
   public static final int NETWORK_INTERFACE_NAME = 424;
   public static final int NETWORK_NUMBER = 425;
   public static final int NETWORK_NUMBER_QUALITY = 426;
   public static final int NETWORK_TYPE = 427;
   public static final int ROUTING_TABLE = 428;
   public static final int VIRTUAL_MAC_ADDRESS_TABLE = 429;
   public static final int COMMAND_TIME_ARRAY = 430;
   public static final int CURRENT_COMMAND_PRIORITY = 431;
   public static final int LAST_COMMAND_TIME = 432;
   public static final int VALUE_SOURCE = 433;
   public static final int VALUE_SOURCE_ARRAY = 434;
   public static final int BACNET_IPV_6MODE = 435;
   public static final int IPV_6ADDRESS = 436;
   public static final int IPV_6PREFIX_LENGTH = 437;
   public static final int BACNET_IPV_6UDP_PORT = 438;
   public static final int IPV_6DEFAULT_GATEWAY = 439;
   public static final int BACNET_IPV_6MULTICAST_ADDRESS = 440;
   public static final int IPV_6DNS_SERVER = 441;
   public static final int IPV_6AUTO_ADDRESSING_ENABLE = 442;
   public static final int IPV_6DHCP_LEASE_TIME = 443;
   public static final int IPV_6DHCP_LEASE_TIME_REMAINING = 444;
   public static final int IPV_6DHCP_SERVER = 445;
   public static final int IPV_6ZONE_INDEX = 446;
   public static final int ASSIGNED_LANDING_CALLS = 447;
   public static final int CAR_ASSIGNED_DIRECTION = 448;
   public static final int CAR_DOOR_COMMAND = 449;
   public static final int CAR_DOOR_STATUS = 450;
   public static final int CAR_DOOR_TEXT = 451;
   public static final int CAR_DOOR_ZONE = 452;
   public static final int CAR_DRIVE_STATUS = 453;
   public static final int CAR_LOAD = 454;
   public static final int CAR_LOAD_UNITS = 455;
   public static final int CAR_MODE = 456;
   public static final int CAR_MOVING_DIRECTION = 457;
   public static final int CAR_POSITION = 458;
   public static final int ELEVATOR_GROUP = 459;
   public static final int ENERGY_METER = 460;
   public static final int ENERGY_METER_REF = 461;
   public static final int ESCALATOR_MODE = 462;
   public static final int FAULT_SIGNALS = 463;
   public static final int FLOOR_TEXT = 464;
   public static final int GROUP_ID = 465;
   public static final int GROUP_MODE = 467;
   public static final int HIGHER_DECK = 468;
   public static final int INSTALLATION_ID = 469;
   public static final int LANDING_CALLS = 470;
   public static final int LANDING_CALL_CONTROL = 471;
   public static final int LANDING_DOOR_STATUS = 472;
   public static final int LOWER_DECK = 473;
   public static final int MACHINE_ROOM_ID = 474;
   public static final int MAKING_CAR_CALL = 475;
   public static final int NEXT_STOPPING_FLOOR = 476;
   public static final int OPERATION_DIRECTION = 477;
   public static final int PASSENGER_ALARM = 478;
   public static final int POWER_MODE = 479;
   public static final int REGISTERED_CAR_CALL = 480;
   public static final int ACTIVE_COV_MULTIPLE_SUBSCRIPTIONS = 481;
   public static final int PROTOCOL_LEVEL = 482;
   public static final int REFERENCE_PORT = 483;
   public static final int DEPLOYED_PROFILE_LOCATION = 484;
   public static final int PROFILE_LOCATION = 485;
   public static final int TAGS = 486;
   public static final int SUBORDINATE_NODE_TYPES = 487;
   public static final int SUBORDINATE_TAGS = 488;
   public static final int SUBORDINATE_RELATIONSHIPS = 489;
   public static final int DEFAULT_SUBORDINATE_RELATIONSHIP = 490;
   public static final int REPRESENTS = 491;
   public static final int DEFAULT_PRESENT_VALUE = 492;
   public static final int PRESENT_STAGE = 493;
   public static final int STAGES = 494;
   public static final int STAGE_NAMES = 495;
   public static final int TARGET_REFERENCES = 496;
   public static final int AUDIT_SOURCE_REPORTER = 497;
   public static final int AUDIT_LEVEL = 498;
   public static final int AUDIT_NOTIFICATION_RECIPIENT = 499;
   public static final int AUDIT_PRIORITY_FILTER = 500;
   public static final int AUDITABLE_OPERATIONS = 501;
   public static final int DELETE_ON_FORWARD = 502;
   public static final int MAXIMUM_SEND_DELAY = 503;
   public static final int MONITORED_OBJECTS = 504;
   public static final int SEND_NOW = 505;
   public static final int FLOOR_NUMBER = 506;
   public static final int DEVICE_UUID = 507;
   public static final int REMOVED_1 = 18;
   public static final int ISSUE_CONFIRMED_NOTIFICATIONS = 51;
   public static final int LIST_OF_SESSION_KEYS = 55;
   public static final int PROTOCOL_CONFORMANCE_CLASS = 95;
   public static final int RECIPIENT = 101;
   public static final int CURRENT_NOTIFY_TIME = 129;
   public static final int PREVIOUS_NOTIFY_TIME = 138;
   public static final int MASTER_EXEMPTION = 284;
   public static final int OCCUPANCY_EXEMPTION = 293;
   public static final int PASSBACK_EXEMPTION = 299;
   public static final BBacnetPropertyIdentifier ackedTransitions = new BBacnetPropertyIdentifier(0);
   public static final BBacnetPropertyIdentifier ackRequired = new BBacnetPropertyIdentifier(1);
   public static final BBacnetPropertyIdentifier action = new BBacnetPropertyIdentifier(2);
   public static final BBacnetPropertyIdentifier actionText = new BBacnetPropertyIdentifier(3);
   public static final BBacnetPropertyIdentifier activeText = new BBacnetPropertyIdentifier(4);
   public static final BBacnetPropertyIdentifier activeVtSessions = new BBacnetPropertyIdentifier(5);
   public static final BBacnetPropertyIdentifier alarmValue = new BBacnetPropertyIdentifier(6);
   public static final BBacnetPropertyIdentifier alarmValues = new BBacnetPropertyIdentifier(7);
   public static final BBacnetPropertyIdentifier all = new BBacnetPropertyIdentifier(8);
   public static final BBacnetPropertyIdentifier allWritesSuccessful = new BBacnetPropertyIdentifier(9);
   public static final BBacnetPropertyIdentifier apduSegmentTimeout = new BBacnetPropertyIdentifier(10);
   public static final BBacnetPropertyIdentifier apduTimeout = new BBacnetPropertyIdentifier(11);
   public static final BBacnetPropertyIdentifier applicationSoftwareVersion = new BBacnetPropertyIdentifier(12);
   public static final BBacnetPropertyIdentifier archive = new BBacnetPropertyIdentifier(13);
   public static final BBacnetPropertyIdentifier bias = new BBacnetPropertyIdentifier(14);
   public static final BBacnetPropertyIdentifier changeOfStateCount = new BBacnetPropertyIdentifier(15);
   public static final BBacnetPropertyIdentifier changeOfStateTime = new BBacnetPropertyIdentifier(16);
   public static final BBacnetPropertyIdentifier notificationClass = new BBacnetPropertyIdentifier(17);
   public static final BBacnetPropertyIdentifier controlledVariableReference = new BBacnetPropertyIdentifier(19);
   public static final BBacnetPropertyIdentifier controlledVariableUnits = new BBacnetPropertyIdentifier(20);
   public static final BBacnetPropertyIdentifier controlledVariableValue = new BBacnetPropertyIdentifier(21);
   public static final BBacnetPropertyIdentifier covIncrement = new BBacnetPropertyIdentifier(22);
   public static final BBacnetPropertyIdentifier dateList = new BBacnetPropertyIdentifier(23);
   public static final BBacnetPropertyIdentifier daylightSavingsStatus = new BBacnetPropertyIdentifier(24);
   public static final BBacnetPropertyIdentifier deadband = new BBacnetPropertyIdentifier(25);
   public static final BBacnetPropertyIdentifier derivativeConstant = new BBacnetPropertyIdentifier(26);
   public static final BBacnetPropertyIdentifier derivativeConstantUnits = new BBacnetPropertyIdentifier(27);
   public static final BBacnetPropertyIdentifier description = new BBacnetPropertyIdentifier(28);
   public static final BBacnetPropertyIdentifier descriptionOfHalt = new BBacnetPropertyIdentifier(29);
   public static final BBacnetPropertyIdentifier deviceAddressBinding = new BBacnetPropertyIdentifier(30);
   public static final BBacnetPropertyIdentifier deviceType = new BBacnetPropertyIdentifier(31);
   public static final BBacnetPropertyIdentifier effectivePeriod = new BBacnetPropertyIdentifier(32);
   public static final BBacnetPropertyIdentifier elapsedActiveTime = new BBacnetPropertyIdentifier(33);
   public static final BBacnetPropertyIdentifier errorLimit = new BBacnetPropertyIdentifier(34);
   public static final BBacnetPropertyIdentifier eventEnable = new BBacnetPropertyIdentifier(35);
   public static final BBacnetPropertyIdentifier eventState = new BBacnetPropertyIdentifier(36);
   public static final BBacnetPropertyIdentifier eventType = new BBacnetPropertyIdentifier(37);
   public static final BBacnetPropertyIdentifier exceptionSchedule = new BBacnetPropertyIdentifier(38);
   public static final BBacnetPropertyIdentifier faultValues = new BBacnetPropertyIdentifier(39);
   public static final BBacnetPropertyIdentifier feedbackValue = new BBacnetPropertyIdentifier(40);
   public static final BBacnetPropertyIdentifier fileAccessMethod = new BBacnetPropertyIdentifier(41);
   public static final BBacnetPropertyIdentifier fileSize = new BBacnetPropertyIdentifier(42);
   public static final BBacnetPropertyIdentifier fileType = new BBacnetPropertyIdentifier(43);
   public static final BBacnetPropertyIdentifier firmwareRevision = new BBacnetPropertyIdentifier(44);
   public static final BBacnetPropertyIdentifier highLimit = new BBacnetPropertyIdentifier(45);
   public static final BBacnetPropertyIdentifier inactiveText = new BBacnetPropertyIdentifier(46);
   public static final BBacnetPropertyIdentifier inProcess = new BBacnetPropertyIdentifier(47);
   public static final BBacnetPropertyIdentifier instanceOf = new BBacnetPropertyIdentifier(48);
   public static final BBacnetPropertyIdentifier integralConstant = new BBacnetPropertyIdentifier(49);
   public static final BBacnetPropertyIdentifier integralConstantUnits = new BBacnetPropertyIdentifier(50);
   public static final BBacnetPropertyIdentifier limitEnable = new BBacnetPropertyIdentifier(52);
   public static final BBacnetPropertyIdentifier listOfGroupMembers = new BBacnetPropertyIdentifier(53);
   public static final BBacnetPropertyIdentifier listOfObjectPropertyReferences = new BBacnetPropertyIdentifier(54);
   public static final BBacnetPropertyIdentifier localDate = new BBacnetPropertyIdentifier(56);
   public static final BBacnetPropertyIdentifier localTime = new BBacnetPropertyIdentifier(57);
   public static final BBacnetPropertyIdentifier location = new BBacnetPropertyIdentifier(58);
   public static final BBacnetPropertyIdentifier lowLimit = new BBacnetPropertyIdentifier(59);
   public static final BBacnetPropertyIdentifier manipulatedVariableReference = new BBacnetPropertyIdentifier(60);
   public static final BBacnetPropertyIdentifier maximumOutput = new BBacnetPropertyIdentifier(61);
   public static final BBacnetPropertyIdentifier maxApduLengthAccepted = new BBacnetPropertyIdentifier(62);
   public static final BBacnetPropertyIdentifier maxInfoFrames = new BBacnetPropertyIdentifier(63);
   public static final BBacnetPropertyIdentifier maxMaster = new BBacnetPropertyIdentifier(64);
   public static final BBacnetPropertyIdentifier maxPresValue = new BBacnetPropertyIdentifier(65);
   public static final BBacnetPropertyIdentifier minimumOffTime = new BBacnetPropertyIdentifier(66);
   public static final BBacnetPropertyIdentifier minimumOnTime = new BBacnetPropertyIdentifier(67);
   public static final BBacnetPropertyIdentifier minimumOutput = new BBacnetPropertyIdentifier(68);
   public static final BBacnetPropertyIdentifier minPresValue = new BBacnetPropertyIdentifier(69);
   public static final BBacnetPropertyIdentifier modelName = new BBacnetPropertyIdentifier(70);
   public static final BBacnetPropertyIdentifier modificationDate = new BBacnetPropertyIdentifier(71);
   public static final BBacnetPropertyIdentifier notifyType = new BBacnetPropertyIdentifier(72);
   public static final BBacnetPropertyIdentifier numberOfApduRetries = new BBacnetPropertyIdentifier(73);
   public static final BBacnetPropertyIdentifier numberOfStates = new BBacnetPropertyIdentifier(74);
   public static final BBacnetPropertyIdentifier objectIdentifier = new BBacnetPropertyIdentifier(75);
   public static final BBacnetPropertyIdentifier objectList = new BBacnetPropertyIdentifier(76);
   public static final BBacnetPropertyIdentifier objectName = new BBacnetPropertyIdentifier(77);
   public static final BBacnetPropertyIdentifier objectPropertyReference = new BBacnetPropertyIdentifier(78);
   public static final BBacnetPropertyIdentifier objectType = new BBacnetPropertyIdentifier(79);
   public static final BBacnetPropertyIdentifier optional = new BBacnetPropertyIdentifier(80);
   public static final BBacnetPropertyIdentifier outOfService = new BBacnetPropertyIdentifier(81);
   public static final BBacnetPropertyIdentifier outputUnits = new BBacnetPropertyIdentifier(82);
   public static final BBacnetPropertyIdentifier eventParameters = new BBacnetPropertyIdentifier(83);
   public static final BBacnetPropertyIdentifier polarity = new BBacnetPropertyIdentifier(84);
   public static final BBacnetPropertyIdentifier presentValue = new BBacnetPropertyIdentifier(85);
   public static final BBacnetPropertyIdentifier priority = new BBacnetPropertyIdentifier(86);
   public static final BBacnetPropertyIdentifier priorityArray = new BBacnetPropertyIdentifier(87);
   public static final BBacnetPropertyIdentifier priorityForWriting = new BBacnetPropertyIdentifier(88);
   public static final BBacnetPropertyIdentifier processIdentifier = new BBacnetPropertyIdentifier(89);
   public static final BBacnetPropertyIdentifier programChange = new BBacnetPropertyIdentifier(90);
   public static final BBacnetPropertyIdentifier programLocation = new BBacnetPropertyIdentifier(91);
   public static final BBacnetPropertyIdentifier programState = new BBacnetPropertyIdentifier(92);
   public static final BBacnetPropertyIdentifier proportionalConstant = new BBacnetPropertyIdentifier(93);
   public static final BBacnetPropertyIdentifier proportionalConstantUnits = new BBacnetPropertyIdentifier(94);
   public static final BBacnetPropertyIdentifier protocolObjectTypesSupported = new BBacnetPropertyIdentifier(96);
   public static final BBacnetPropertyIdentifier protocolServicesSupported = new BBacnetPropertyIdentifier(97);
   public static final BBacnetPropertyIdentifier protocolVersion = new BBacnetPropertyIdentifier(98);
   public static final BBacnetPropertyIdentifier readOnly = new BBacnetPropertyIdentifier(99);
   public static final BBacnetPropertyIdentifier reasonForHalt = new BBacnetPropertyIdentifier(100);
   public static final BBacnetPropertyIdentifier recipientList = new BBacnetPropertyIdentifier(102);
   public static final BBacnetPropertyIdentifier reliability = new BBacnetPropertyIdentifier(103);
   public static final BBacnetPropertyIdentifier relinquishDefault = new BBacnetPropertyIdentifier(104);
   public static final BBacnetPropertyIdentifier required = new BBacnetPropertyIdentifier(105);
   public static final BBacnetPropertyIdentifier resolution = new BBacnetPropertyIdentifier(106);
   public static final BBacnetPropertyIdentifier segmentationSupported = new BBacnetPropertyIdentifier(107);
   public static final BBacnetPropertyIdentifier setpoint = new BBacnetPropertyIdentifier(108);
   public static final BBacnetPropertyIdentifier setpointReference = new BBacnetPropertyIdentifier(109);
   public static final BBacnetPropertyIdentifier stateText = new BBacnetPropertyIdentifier(110);
   public static final BBacnetPropertyIdentifier statusFlags = new BBacnetPropertyIdentifier(111);
   public static final BBacnetPropertyIdentifier systemStatus = new BBacnetPropertyIdentifier(112);
   public static final BBacnetPropertyIdentifier timeDelay = new BBacnetPropertyIdentifier(113);
   public static final BBacnetPropertyIdentifier timeOfActiveTimeReset = new BBacnetPropertyIdentifier(114);
   public static final BBacnetPropertyIdentifier timeOfStateCountReset = new BBacnetPropertyIdentifier(115);
   public static final BBacnetPropertyIdentifier timeSynchronizationRecipients = new BBacnetPropertyIdentifier(116);
   public static final BBacnetPropertyIdentifier units = new BBacnetPropertyIdentifier(117);
   public static final BBacnetPropertyIdentifier updateInterval = new BBacnetPropertyIdentifier(118);
   public static final BBacnetPropertyIdentifier utcOffset = new BBacnetPropertyIdentifier(119);
   public static final BBacnetPropertyIdentifier vendorIdentifier = new BBacnetPropertyIdentifier(120);
   public static final BBacnetPropertyIdentifier vendorName = new BBacnetPropertyIdentifier(121);
   public static final BBacnetPropertyIdentifier vtClassesSupported = new BBacnetPropertyIdentifier(122);
   public static final BBacnetPropertyIdentifier weeklySchedule = new BBacnetPropertyIdentifier(123);
   public static final BBacnetPropertyIdentifier attemptedSamples = new BBacnetPropertyIdentifier(124);
   public static final BBacnetPropertyIdentifier averageValue = new BBacnetPropertyIdentifier(125);
   public static final BBacnetPropertyIdentifier bufferSize = new BBacnetPropertyIdentifier(126);
   public static final BBacnetPropertyIdentifier clientCovIncrement = new BBacnetPropertyIdentifier(127);
   public static final BBacnetPropertyIdentifier covResubscriptionInterval = new BBacnetPropertyIdentifier(128);
   public static final BBacnetPropertyIdentifier eventTimeStamps = new BBacnetPropertyIdentifier(130);
   public static final BBacnetPropertyIdentifier logBuffer = new BBacnetPropertyIdentifier(131);
   public static final BBacnetPropertyIdentifier logDeviceObjectProperty = new BBacnetPropertyIdentifier(132);
   public static final BBacnetPropertyIdentifier enable = new BBacnetPropertyIdentifier(133);
   public static final BBacnetPropertyIdentifier logInterval = new BBacnetPropertyIdentifier(134);
   public static final BBacnetPropertyIdentifier maximumValue = new BBacnetPropertyIdentifier(135);
   public static final BBacnetPropertyIdentifier minimumValue = new BBacnetPropertyIdentifier(136);
   public static final BBacnetPropertyIdentifier notificationThreshold = new BBacnetPropertyIdentifier(137);
   public static final BBacnetPropertyIdentifier protocolRevision = new BBacnetPropertyIdentifier(139);
   public static final BBacnetPropertyIdentifier recordsSinceNotification = new BBacnetPropertyIdentifier(140);
   public static final BBacnetPropertyIdentifier recordCount = new BBacnetPropertyIdentifier(141);
   public static final BBacnetPropertyIdentifier startTime = new BBacnetPropertyIdentifier(142);
   public static final BBacnetPropertyIdentifier stopTime = new BBacnetPropertyIdentifier(143);
   public static final BBacnetPropertyIdentifier stopWhenFull = new BBacnetPropertyIdentifier(144);
   public static final BBacnetPropertyIdentifier totalRecordCount = new BBacnetPropertyIdentifier(145);
   public static final BBacnetPropertyIdentifier validSamples = new BBacnetPropertyIdentifier(146);
   public static final BBacnetPropertyIdentifier windowInterval = new BBacnetPropertyIdentifier(147);
   public static final BBacnetPropertyIdentifier windowSamples = new BBacnetPropertyIdentifier(148);
   public static final BBacnetPropertyIdentifier maximumValueTimestamp = new BBacnetPropertyIdentifier(149);
   public static final BBacnetPropertyIdentifier minimumValueTimestamp = new BBacnetPropertyIdentifier(150);
   public static final BBacnetPropertyIdentifier varianceValue = new BBacnetPropertyIdentifier(151);
   public static final BBacnetPropertyIdentifier activeCovSubscriptions = new BBacnetPropertyIdentifier(152);
   public static final BBacnetPropertyIdentifier backupFailureTimeout = new BBacnetPropertyIdentifier(153);
   public static final BBacnetPropertyIdentifier configurationFiles = new BBacnetPropertyIdentifier(154);
   public static final BBacnetPropertyIdentifier databaseRevision = new BBacnetPropertyIdentifier(155);
   public static final BBacnetPropertyIdentifier directReading = new BBacnetPropertyIdentifier(156);
   public static final BBacnetPropertyIdentifier lastRestoreTime = new BBacnetPropertyIdentifier(157);
   public static final BBacnetPropertyIdentifier maintenanceRequired = new BBacnetPropertyIdentifier(158);
   public static final BBacnetPropertyIdentifier memberOf = new BBacnetPropertyIdentifier(159);
   public static final BBacnetPropertyIdentifier mode = new BBacnetPropertyIdentifier(160);
   public static final BBacnetPropertyIdentifier operationExpected = new BBacnetPropertyIdentifier(161);
   public static final BBacnetPropertyIdentifier setting = new BBacnetPropertyIdentifier(162);
   public static final BBacnetPropertyIdentifier silenced = new BBacnetPropertyIdentifier(163);
   public static final BBacnetPropertyIdentifier trackingValue = new BBacnetPropertyIdentifier(164);
   public static final BBacnetPropertyIdentifier zoneMembers = new BBacnetPropertyIdentifier(165);
   public static final BBacnetPropertyIdentifier lifeSafetyAlarmValues = new BBacnetPropertyIdentifier(166);
   public static final BBacnetPropertyIdentifier maxSegmentsAccepted = new BBacnetPropertyIdentifier(167);
   public static final BBacnetPropertyIdentifier profileName = new BBacnetPropertyIdentifier(168);
   public static final BBacnetPropertyIdentifier autoSlaveDiscovery = new BBacnetPropertyIdentifier(169);
   public static final BBacnetPropertyIdentifier manualSlaveAddressBinding = new BBacnetPropertyIdentifier(170);
   public static final BBacnetPropertyIdentifier slaveAddressBinding = new BBacnetPropertyIdentifier(171);
   public static final BBacnetPropertyIdentifier slaveProxyEnable = new BBacnetPropertyIdentifier(172);
   public static final BBacnetPropertyIdentifier lastNotifyRecord = new BBacnetPropertyIdentifier(173);
   public static final BBacnetPropertyIdentifier scheduleDefault = new BBacnetPropertyIdentifier(174);
   public static final BBacnetPropertyIdentifier acceptedModes = new BBacnetPropertyIdentifier(175);
   public static final BBacnetPropertyIdentifier adjustValue = new BBacnetPropertyIdentifier(176);
   public static final BBacnetPropertyIdentifier count = new BBacnetPropertyIdentifier(177);
   public static final BBacnetPropertyIdentifier countBeforeChange = new BBacnetPropertyIdentifier(178);
   public static final BBacnetPropertyIdentifier countChangeTime = new BBacnetPropertyIdentifier(179);
   public static final BBacnetPropertyIdentifier covPeriod = new BBacnetPropertyIdentifier(180);
   public static final BBacnetPropertyIdentifier inputReference = new BBacnetPropertyIdentifier(181);
   public static final BBacnetPropertyIdentifier limitMonitoringInterval = new BBacnetPropertyIdentifier(182);
   public static final BBacnetPropertyIdentifier loggingObject = new BBacnetPropertyIdentifier(183);
   public static final BBacnetPropertyIdentifier loggingRecord = new BBacnetPropertyIdentifier(184);
   public static final BBacnetPropertyIdentifier prescale = new BBacnetPropertyIdentifier(185);
   public static final BBacnetPropertyIdentifier pulseRate = new BBacnetPropertyIdentifier(186);
   public static final BBacnetPropertyIdentifier scale = new BBacnetPropertyIdentifier(187);
   public static final BBacnetPropertyIdentifier scaleFactor = new BBacnetPropertyIdentifier(188);
   public static final BBacnetPropertyIdentifier updateTime = new BBacnetPropertyIdentifier(189);
   public static final BBacnetPropertyIdentifier valueBeforeChange = new BBacnetPropertyIdentifier(190);
   public static final BBacnetPropertyIdentifier valueSet = new BBacnetPropertyIdentifier(191);
   public static final BBacnetPropertyIdentifier valueChangeTime = new BBacnetPropertyIdentifier(192);
   public static final BBacnetPropertyIdentifier alignIntervals = new BBacnetPropertyIdentifier(193);
   public static final BBacnetPropertyIdentifier intervalOffset = new BBacnetPropertyIdentifier(195);
   public static final BBacnetPropertyIdentifier lastRestartReason = new BBacnetPropertyIdentifier(196);
   public static final BBacnetPropertyIdentifier loggingType = new BBacnetPropertyIdentifier(197);
   public static final BBacnetPropertyIdentifier restartNotificationRecipients = new BBacnetPropertyIdentifier(202);
   public static final BBacnetPropertyIdentifier timeOfDeviceRestart = new BBacnetPropertyIdentifier(203);
   public static final BBacnetPropertyIdentifier timeSynchronizationInterval = new BBacnetPropertyIdentifier(204);
   public static final BBacnetPropertyIdentifier trigger = new BBacnetPropertyIdentifier(205);
   public static final BBacnetPropertyIdentifier utcTimeSynchronizationRecipients = new BBacnetPropertyIdentifier(206);
   public static final BBacnetPropertyIdentifier nodeSubtype = new BBacnetPropertyIdentifier(207);
   public static final BBacnetPropertyIdentifier nodeType = new BBacnetPropertyIdentifier(208);
   public static final BBacnetPropertyIdentifier structuredObjectList = new BBacnetPropertyIdentifier(209);
   public static final BBacnetPropertyIdentifier subordinateAnnotations = new BBacnetPropertyIdentifier(210);
   public static final BBacnetPropertyIdentifier subordinateList = new BBacnetPropertyIdentifier(211);
   public static final BBacnetPropertyIdentifier actualShedLevel = new BBacnetPropertyIdentifier(212);
   public static final BBacnetPropertyIdentifier dutyWindow = new BBacnetPropertyIdentifier(213);
   public static final BBacnetPropertyIdentifier expectedShedLevel = new BBacnetPropertyIdentifier(214);
   public static final BBacnetPropertyIdentifier fullDutyBaseline = new BBacnetPropertyIdentifier(215);
   public static final BBacnetPropertyIdentifier requestedShedLevel = new BBacnetPropertyIdentifier(218);
   public static final BBacnetPropertyIdentifier shedDuration = new BBacnetPropertyIdentifier(219);
   public static final BBacnetPropertyIdentifier shedLevelDescriptions = new BBacnetPropertyIdentifier(220);
   public static final BBacnetPropertyIdentifier shedLevels = new BBacnetPropertyIdentifier(221);
   public static final BBacnetPropertyIdentifier stateDescription = new BBacnetPropertyIdentifier(222);
   public static final BBacnetPropertyIdentifier doorAlarmState = new BBacnetPropertyIdentifier(226);
   public static final BBacnetPropertyIdentifier doorExtendedPulseTime = new BBacnetPropertyIdentifier(227);
   public static final BBacnetPropertyIdentifier doorMembers = new BBacnetPropertyIdentifier(228);
   public static final BBacnetPropertyIdentifier doorOpenTooLongTime = new BBacnetPropertyIdentifier(229);
   public static final BBacnetPropertyIdentifier doorPulseTime = new BBacnetPropertyIdentifier(230);
   public static final BBacnetPropertyIdentifier doorStatus = new BBacnetPropertyIdentifier(231);
   public static final BBacnetPropertyIdentifier doorUnlockDelayTime = new BBacnetPropertyIdentifier(232);
   public static final BBacnetPropertyIdentifier lockStatus = new BBacnetPropertyIdentifier(233);
   public static final BBacnetPropertyIdentifier maskedAlarmValues = new BBacnetPropertyIdentifier(234);
   public static final BBacnetPropertyIdentifier securedStatus = new BBacnetPropertyIdentifier(235);
   public static final BBacnetPropertyIdentifier absenteeLimit = new BBacnetPropertyIdentifier(244);
   public static final BBacnetPropertyIdentifier accessAlarmEvents = new BBacnetPropertyIdentifier(245);
   public static final BBacnetPropertyIdentifier accessDoors = new BBacnetPropertyIdentifier(246);
   public static final BBacnetPropertyIdentifier accessEvent = new BBacnetPropertyIdentifier(247);
   public static final BBacnetPropertyIdentifier accessEventAuthenticationFactor = new BBacnetPropertyIdentifier(248);
   public static final BBacnetPropertyIdentifier accessEventCredential = new BBacnetPropertyIdentifier(249);
   public static final BBacnetPropertyIdentifier accessEventTime = new BBacnetPropertyIdentifier(250);
   public static final BBacnetPropertyIdentifier accessTransactionEvents = new BBacnetPropertyIdentifier(251);
   public static final BBacnetPropertyIdentifier accompaniment = new BBacnetPropertyIdentifier(252);
   public static final BBacnetPropertyIdentifier accompanimentTime = new BBacnetPropertyIdentifier(253);
   public static final BBacnetPropertyIdentifier activationTime = new BBacnetPropertyIdentifier(254);
   public static final BBacnetPropertyIdentifier activeAuthenticationPolicy = new BBacnetPropertyIdentifier(255);
   public static final BBacnetPropertyIdentifier assignedAccessRights = new BBacnetPropertyIdentifier(256);
   public static final BBacnetPropertyIdentifier authenticationFactors = new BBacnetPropertyIdentifier(257);
   public static final BBacnetPropertyIdentifier authenticationPolicyList = new BBacnetPropertyIdentifier(258);
   public static final BBacnetPropertyIdentifier authenticationPolicyNames = new BBacnetPropertyIdentifier(259);
   public static final BBacnetPropertyIdentifier authenticationStatus = new BBacnetPropertyIdentifier(260);
   public static final BBacnetPropertyIdentifier authorizationMode = new BBacnetPropertyIdentifier(261);
   public static final BBacnetPropertyIdentifier belongsTo = new BBacnetPropertyIdentifier(262);
   public static final BBacnetPropertyIdentifier credentialDisable = new BBacnetPropertyIdentifier(263);
   public static final BBacnetPropertyIdentifier credentialStatus = new BBacnetPropertyIdentifier(264);
   public static final BBacnetPropertyIdentifier credentials = new BBacnetPropertyIdentifier(265);
   public static final BBacnetPropertyIdentifier credentialsInZone = new BBacnetPropertyIdentifier(266);
   public static final BBacnetPropertyIdentifier daysRemaining = new BBacnetPropertyIdentifier(267);
   public static final BBacnetPropertyIdentifier entryPoints = new BBacnetPropertyIdentifier(268);
   public static final BBacnetPropertyIdentifier exitPoints = new BBacnetPropertyIdentifier(269);
   public static final BBacnetPropertyIdentifier expiryTime = new BBacnetPropertyIdentifier(270);
   public static final BBacnetPropertyIdentifier extendedTimeEnable = new BBacnetPropertyIdentifier(271);
   public static final BBacnetPropertyIdentifier failedAttemptEvents = new BBacnetPropertyIdentifier(272);
   public static final BBacnetPropertyIdentifier failedAttempts = new BBacnetPropertyIdentifier(273);
   public static final BBacnetPropertyIdentifier failedAttemptsTime = new BBacnetPropertyIdentifier(274);
   public static final BBacnetPropertyIdentifier lastAccessEvent = new BBacnetPropertyIdentifier(275);
   public static final BBacnetPropertyIdentifier lastAccessPoint = new BBacnetPropertyIdentifier(276);
   public static final BBacnetPropertyIdentifier lastCredentialAdded = new BBacnetPropertyIdentifier(277);
   public static final BBacnetPropertyIdentifier lastCredentialAddedTime = new BBacnetPropertyIdentifier(278);
   public static final BBacnetPropertyIdentifier lastCredentialRemoved = new BBacnetPropertyIdentifier(279);
   public static final BBacnetPropertyIdentifier lastCredentialRemovedTime = new BBacnetPropertyIdentifier(280);
   public static final BBacnetPropertyIdentifier lastUseTime = new BBacnetPropertyIdentifier(281);
   public static final BBacnetPropertyIdentifier lockout = new BBacnetPropertyIdentifier(282);
   public static final BBacnetPropertyIdentifier lockoutRelinquishTime = new BBacnetPropertyIdentifier(283);
   public static final BBacnetPropertyIdentifier maxFailedAttempts = new BBacnetPropertyIdentifier(285);
   public static final BBacnetPropertyIdentifier members = new BBacnetPropertyIdentifier(286);
   public static final BBacnetPropertyIdentifier musterPoint = new BBacnetPropertyIdentifier(287);
   public static final BBacnetPropertyIdentifier negativeAccessRules = new BBacnetPropertyIdentifier(288);
   public static final BBacnetPropertyIdentifier numberOfAuthenticationPolicies = new BBacnetPropertyIdentifier(289);
   public static final BBacnetPropertyIdentifier occupancyCount = new BBacnetPropertyIdentifier(290);
   public static final BBacnetPropertyIdentifier occupancyCountAdjust = new BBacnetPropertyIdentifier(291);
   public static final BBacnetPropertyIdentifier occupancyCountEnable = new BBacnetPropertyIdentifier(292);
   public static final BBacnetPropertyIdentifier occupancyLowerLimit = new BBacnetPropertyIdentifier(294);
   public static final BBacnetPropertyIdentifier occupancyLowerLimitEnforced = new BBacnetPropertyIdentifier(295);
   public static final BBacnetPropertyIdentifier occupancyState = new BBacnetPropertyIdentifier(296);
   public static final BBacnetPropertyIdentifier occupancyUpperLimit = new BBacnetPropertyIdentifier(297);
   public static final BBacnetPropertyIdentifier occupancyUpperLimitEnforced = new BBacnetPropertyIdentifier(298);
   public static final BBacnetPropertyIdentifier passbackMode = new BBacnetPropertyIdentifier(300);
   public static final BBacnetPropertyIdentifier passbackTimeout = new BBacnetPropertyIdentifier(301);
   public static final BBacnetPropertyIdentifier positiveAccessRules = new BBacnetPropertyIdentifier(302);
   public static final BBacnetPropertyIdentifier reasonForDisable = new BBacnetPropertyIdentifier(303);
   public static final BBacnetPropertyIdentifier supportedFormats = new BBacnetPropertyIdentifier(304);
   public static final BBacnetPropertyIdentifier supportedFormatClasses = new BBacnetPropertyIdentifier(305);
   public static final BBacnetPropertyIdentifier threatAuthority = new BBacnetPropertyIdentifier(306);
   public static final BBacnetPropertyIdentifier threatLevel = new BBacnetPropertyIdentifier(307);
   public static final BBacnetPropertyIdentifier traceFlag = new BBacnetPropertyIdentifier(308);
   public static final BBacnetPropertyIdentifier transactionNotificationClass = new BBacnetPropertyIdentifier(309);
   public static final BBacnetPropertyIdentifier userExternalIdentifier = new BBacnetPropertyIdentifier(310);
   public static final BBacnetPropertyIdentifier userInformationReference = new BBacnetPropertyIdentifier(311);
   public static final BBacnetPropertyIdentifier userName = new BBacnetPropertyIdentifier(317);
   public static final BBacnetPropertyIdentifier userType = new BBacnetPropertyIdentifier(318);
   public static final BBacnetPropertyIdentifier usesRemaining = new BBacnetPropertyIdentifier(319);
   public static final BBacnetPropertyIdentifier zoneFrom = new BBacnetPropertyIdentifier(320);
   public static final BBacnetPropertyIdentifier zoneTo = new BBacnetPropertyIdentifier(321);
   public static final BBacnetPropertyIdentifier accessEventTag = new BBacnetPropertyIdentifier(322);
   public static final BBacnetPropertyIdentifier globalIdentifier = new BBacnetPropertyIdentifier(323);
   public static final BBacnetPropertyIdentifier verificationTime = new BBacnetPropertyIdentifier(326);
   public static final BBacnetPropertyIdentifier backupAndRestoreState = new BBacnetPropertyIdentifier(338);
   public static final BBacnetPropertyIdentifier backupPreparationTime = new BBacnetPropertyIdentifier(339);
   public static final BBacnetPropertyIdentifier restoreCompletionTime = new BBacnetPropertyIdentifier(340);
   public static final BBacnetPropertyIdentifier restorePreparationTime = new BBacnetPropertyIdentifier(341);
   public static final BBacnetPropertyIdentifier bitMask = new BBacnetPropertyIdentifier(342);
   public static final BBacnetPropertyIdentifier bitText = new BBacnetPropertyIdentifier(343);
   public static final BBacnetPropertyIdentifier isUtc = new BBacnetPropertyIdentifier(344);
   public static final BBacnetPropertyIdentifier groupMembers = new BBacnetPropertyIdentifier(345);
   public static final BBacnetPropertyIdentifier groupMemberNames = new BBacnetPropertyIdentifier(346);
   public static final BBacnetPropertyIdentifier memberStatusDlags = new BBacnetPropertyIdentifier(347);
   public static final BBacnetPropertyIdentifier requestedUpdateInterval = new BBacnetPropertyIdentifier(348);
   public static final BBacnetPropertyIdentifier covuPeriod = new BBacnetPropertyIdentifier(349);
   public static final BBacnetPropertyIdentifier covuRecipients = new BBacnetPropertyIdentifier(350);
   public static final BBacnetPropertyIdentifier eventMessageTexts = new BBacnetPropertyIdentifier(351);
   public static final BBacnetPropertyIdentifier eventMessageTextsConfig = new BBacnetPropertyIdentifier(352);
   public static final BBacnetPropertyIdentifier eventDetectionEnable = new BBacnetPropertyIdentifier(353);
   public static final BBacnetPropertyIdentifier eventAlgorithmInhibit = new BBacnetPropertyIdentifier(354);
   public static final BBacnetPropertyIdentifier eventAlgorithmInhibitRef = new BBacnetPropertyIdentifier(355);
   public static final BBacnetPropertyIdentifier timeDelayNormal = new BBacnetPropertyIdentifier(356);
   public static final BBacnetPropertyIdentifier reliabilityEvaluationInhibit = new BBacnetPropertyIdentifier(357);
   public static final BBacnetPropertyIdentifier faultParameters = new BBacnetPropertyIdentifier(358);
   public static final BBacnetPropertyIdentifier faultType = new BBacnetPropertyIdentifier(359);
   public static final BBacnetPropertyIdentifier localForwardingOnly = new BBacnetPropertyIdentifier(360);
   public static final BBacnetPropertyIdentifier processIdentifierFilter = new BBacnetPropertyIdentifier(361);
   public static final BBacnetPropertyIdentifier subscribedRecipients = new BBacnetPropertyIdentifier(362);
   public static final BBacnetPropertyIdentifier portFilter = new BBacnetPropertyIdentifier(363);
   public static final BBacnetPropertyIdentifier authorizationExemptions = new BBacnetPropertyIdentifier(364);
   public static final BBacnetPropertyIdentifier allowGroupDelayInhibit = new BBacnetPropertyIdentifier(365);
   public static final BBacnetPropertyIdentifier channelNumber = new BBacnetPropertyIdentifier(366);
   public static final BBacnetPropertyIdentifier controlGroups = new BBacnetPropertyIdentifier(367);
   public static final BBacnetPropertyIdentifier executionDelay = new BBacnetPropertyIdentifier(368);
   public static final BBacnetPropertyIdentifier lastPriority = new BBacnetPropertyIdentifier(369);
   public static final BBacnetPropertyIdentifier writeStatus = new BBacnetPropertyIdentifier(370);
   public static final BBacnetPropertyIdentifier propertyList = new BBacnetPropertyIdentifier(371);
   public static final BBacnetPropertyIdentifier serialNumber = new BBacnetPropertyIdentifier(372);
   public static final BBacnetPropertyIdentifier blinkWarnEnable = new BBacnetPropertyIdentifier(373);
   public static final BBacnetPropertyIdentifier defaultFadetime = new BBacnetPropertyIdentifier(374);
   public static final BBacnetPropertyIdentifier defaultRamprate = new BBacnetPropertyIdentifier(375);
   public static final BBacnetPropertyIdentifier defaultStepIncrement = new BBacnetPropertyIdentifier(376);
   public static final BBacnetPropertyIdentifier egressTime = new BBacnetPropertyIdentifier(377);
   public static final BBacnetPropertyIdentifier inProgress = new BBacnetPropertyIdentifier(378);
   public static final BBacnetPropertyIdentifier instantaneousPower = new BBacnetPropertyIdentifier(379);
   public static final BBacnetPropertyIdentifier lightingCommand = new BBacnetPropertyIdentifier(380);
   public static final BBacnetPropertyIdentifier lightingCommandDefaultPriority = new BBacnetPropertyIdentifier(381);
   public static final BBacnetPropertyIdentifier maxActualValue = new BBacnetPropertyIdentifier(382);
   public static final BBacnetPropertyIdentifier minActualValue = new BBacnetPropertyIdentifier(383);
   public static final BBacnetPropertyIdentifier power = new BBacnetPropertyIdentifier(384);
   public static final BBacnetPropertyIdentifier transition = new BBacnetPropertyIdentifier(385);
   public static final BBacnetPropertyIdentifier egressActive = new BBacnetPropertyIdentifier(386);
   public static final BBacnetPropertyIdentifier interfaceValue = new BBacnetPropertyIdentifier(387);
   public static final BBacnetPropertyIdentifier faultHighLimit = new BBacnetPropertyIdentifier(388);
   public static final BBacnetPropertyIdentifier faultLowLimit = new BBacnetPropertyIdentifier(389);
   public static final BBacnetPropertyIdentifier lowDiffLimit = new BBacnetPropertyIdentifier(390);
   public static final BBacnetPropertyIdentifier strikeCount = new BBacnetPropertyIdentifier(391);
   public static final BBacnetPropertyIdentifier timeOfStrikeCountReset = new BBacnetPropertyIdentifier(392);
   public static final BBacnetPropertyIdentifier defaultTimeout = new BBacnetPropertyIdentifier(393);
   public static final BBacnetPropertyIdentifier initialTimeout = new BBacnetPropertyIdentifier(394);
   public static final BBacnetPropertyIdentifier lastStateChange = new BBacnetPropertyIdentifier(395);
   public static final BBacnetPropertyIdentifier stateChangeValues = new BBacnetPropertyIdentifier(396);
   public static final BBacnetPropertyIdentifier timerRunning = new BBacnetPropertyIdentifier(397);
   public static final BBacnetPropertyIdentifier timerState = new BBacnetPropertyIdentifier(398);
   public static final BBacnetPropertyIdentifier apduLength = new BBacnetPropertyIdentifier(399);
   public static final BBacnetPropertyIdentifier ipAddress = new BBacnetPropertyIdentifier(400);
   public static final BBacnetPropertyIdentifier ipDefaultGateway = new BBacnetPropertyIdentifier(401);
   public static final BBacnetPropertyIdentifier ipDhcpEnable = new BBacnetPropertyIdentifier(402);
   public static final BBacnetPropertyIdentifier ipDhcpLeaseTime = new BBacnetPropertyIdentifier(403);
   public static final BBacnetPropertyIdentifier ipDhcpLeaseTimeRemaining = new BBacnetPropertyIdentifier(404);
   public static final BBacnetPropertyIdentifier ipDhcpServer = new BBacnetPropertyIdentifier(405);
   public static final BBacnetPropertyIdentifier ipDnsServer = new BBacnetPropertyIdentifier(406);
   public static final BBacnetPropertyIdentifier bacnetIpGlobalAddress = new BBacnetPropertyIdentifier(407);
   public static final BBacnetPropertyIdentifier bacnetIpMode = new BBacnetPropertyIdentifier(408);
   public static final BBacnetPropertyIdentifier bacnetIpMulticastAddress = new BBacnetPropertyIdentifier(409);
   public static final BBacnetPropertyIdentifier bacnetIpNatTraversal = new BBacnetPropertyIdentifier(410);
   public static final BBacnetPropertyIdentifier ipSubnetMask = new BBacnetPropertyIdentifier(411);
   public static final BBacnetPropertyIdentifier bacnetIpUdpPort = new BBacnetPropertyIdentifier(412);
   public static final BBacnetPropertyIdentifier bbmdAcceptFdRegistrations = new BBacnetPropertyIdentifier(413);
   public static final BBacnetPropertyIdentifier bbmdBroadcastDistributionTable = new BBacnetPropertyIdentifier(414);
   public static final BBacnetPropertyIdentifier bbmdForeignDeviceTable = new BBacnetPropertyIdentifier(415);
   public static final BBacnetPropertyIdentifier changesPending = new BBacnetPropertyIdentifier(416);
   public static final BBacnetPropertyIdentifier command = new BBacnetPropertyIdentifier(417);
   public static final BBacnetPropertyIdentifier fdBbmdAddress = new BBacnetPropertyIdentifier(418);
   public static final BBacnetPropertyIdentifier fdSubscriptionLifetime = new BBacnetPropertyIdentifier(419);
   public static final BBacnetPropertyIdentifier linkSpeed = new BBacnetPropertyIdentifier(420);
   public static final BBacnetPropertyIdentifier linkSpeeds = new BBacnetPropertyIdentifier(421);
   public static final BBacnetPropertyIdentifier linkSpeedAutonegotiate = new BBacnetPropertyIdentifier(422);
   public static final BBacnetPropertyIdentifier macAddress = new BBacnetPropertyIdentifier(423);
   public static final BBacnetPropertyIdentifier networkInterfaceName = new BBacnetPropertyIdentifier(424);
   public static final BBacnetPropertyIdentifier networkNumber = new BBacnetPropertyIdentifier(425);
   public static final BBacnetPropertyIdentifier networkNumberQuality = new BBacnetPropertyIdentifier(426);
   public static final BBacnetPropertyIdentifier networkType = new BBacnetPropertyIdentifier(427);
   public static final BBacnetPropertyIdentifier routingTable = new BBacnetPropertyIdentifier(428);
   public static final BBacnetPropertyIdentifier virtualMacAddressTable = new BBacnetPropertyIdentifier(429);
   public static final BBacnetPropertyIdentifier commandTimeArray = new BBacnetPropertyIdentifier(430);
   public static final BBacnetPropertyIdentifier currentCommandPriority = new BBacnetPropertyIdentifier(431);
   public static final BBacnetPropertyIdentifier lastCommandTime = new BBacnetPropertyIdentifier(432);
   public static final BBacnetPropertyIdentifier valueSource = new BBacnetPropertyIdentifier(433);
   public static final BBacnetPropertyIdentifier valueSourceArray = new BBacnetPropertyIdentifier(434);
   public static final BBacnetPropertyIdentifier bacnetIpv6Mode = new BBacnetPropertyIdentifier(435);
   public static final BBacnetPropertyIdentifier ipv6Address = new BBacnetPropertyIdentifier(436);
   public static final BBacnetPropertyIdentifier ipv6PrefixLength = new BBacnetPropertyIdentifier(437);
   public static final BBacnetPropertyIdentifier bacnetIpv6UdpPort = new BBacnetPropertyIdentifier(438);
   public static final BBacnetPropertyIdentifier ipv6DefaultGateway = new BBacnetPropertyIdentifier(439);
   public static final BBacnetPropertyIdentifier bacnetIpv6MulticastAddress = new BBacnetPropertyIdentifier(440);
   public static final BBacnetPropertyIdentifier ipv6DnsServer = new BBacnetPropertyIdentifier(441);
   public static final BBacnetPropertyIdentifier ipv6AutoAddressingEnable = new BBacnetPropertyIdentifier(442);
   public static final BBacnetPropertyIdentifier ipv6DhcpLeaseTime = new BBacnetPropertyIdentifier(443);
   public static final BBacnetPropertyIdentifier ipv6DhcpLeaseTimeRemaining = new BBacnetPropertyIdentifier(444);
   public static final BBacnetPropertyIdentifier ipv6DhcpServer = new BBacnetPropertyIdentifier(445);
   public static final BBacnetPropertyIdentifier ipv6ZoneIndex = new BBacnetPropertyIdentifier(446);
   public static final BBacnetPropertyIdentifier assignedLandingCalls = new BBacnetPropertyIdentifier(447);
   public static final BBacnetPropertyIdentifier carAssignedDirection = new BBacnetPropertyIdentifier(448);
   public static final BBacnetPropertyIdentifier carDoorCommand = new BBacnetPropertyIdentifier(449);
   public static final BBacnetPropertyIdentifier carDoorStatus = new BBacnetPropertyIdentifier(450);
   public static final BBacnetPropertyIdentifier carDoorText = new BBacnetPropertyIdentifier(451);
   public static final BBacnetPropertyIdentifier carDoorZone = new BBacnetPropertyIdentifier(452);
   public static final BBacnetPropertyIdentifier carDriveStatus = new BBacnetPropertyIdentifier(453);
   public static final BBacnetPropertyIdentifier carLoad = new BBacnetPropertyIdentifier(454);
   public static final BBacnetPropertyIdentifier carLoadUnits = new BBacnetPropertyIdentifier(455);
   public static final BBacnetPropertyIdentifier carMode = new BBacnetPropertyIdentifier(456);
   public static final BBacnetPropertyIdentifier carMovingDirection = new BBacnetPropertyIdentifier(457);
   public static final BBacnetPropertyIdentifier carPosition = new BBacnetPropertyIdentifier(458);
   public static final BBacnetPropertyIdentifier elevatorGroup = new BBacnetPropertyIdentifier(459);
   public static final BBacnetPropertyIdentifier energyMeter = new BBacnetPropertyIdentifier(460);
   public static final BBacnetPropertyIdentifier energyMeterRef = new BBacnetPropertyIdentifier(461);
   public static final BBacnetPropertyIdentifier escalatorMode = new BBacnetPropertyIdentifier(462);
   public static final BBacnetPropertyIdentifier faultSignals = new BBacnetPropertyIdentifier(463);
   public static final BBacnetPropertyIdentifier floorText = new BBacnetPropertyIdentifier(464);
   public static final BBacnetPropertyIdentifier groupId = new BBacnetPropertyIdentifier(465);
   public static final BBacnetPropertyIdentifier groupMode = new BBacnetPropertyIdentifier(467);
   public static final BBacnetPropertyIdentifier higherDeck = new BBacnetPropertyIdentifier(468);
   public static final BBacnetPropertyIdentifier installationId = new BBacnetPropertyIdentifier(469);
   public static final BBacnetPropertyIdentifier landingCalls = new BBacnetPropertyIdentifier(470);
   public static final BBacnetPropertyIdentifier landingCallControl = new BBacnetPropertyIdentifier(471);
   public static final BBacnetPropertyIdentifier landingDoorStatus = new BBacnetPropertyIdentifier(472);
   public static final BBacnetPropertyIdentifier lowerDeck = new BBacnetPropertyIdentifier(473);
   public static final BBacnetPropertyIdentifier machineRoomId = new BBacnetPropertyIdentifier(474);
   public static final BBacnetPropertyIdentifier makingCarCall = new BBacnetPropertyIdentifier(475);
   public static final BBacnetPropertyIdentifier nextStoppingFloor = new BBacnetPropertyIdentifier(476);
   public static final BBacnetPropertyIdentifier operationDirection = new BBacnetPropertyIdentifier(477);
   public static final BBacnetPropertyIdentifier passengerAlarm = new BBacnetPropertyIdentifier(478);
   public static final BBacnetPropertyIdentifier powerMode = new BBacnetPropertyIdentifier(479);
   public static final BBacnetPropertyIdentifier registeredCarCall = new BBacnetPropertyIdentifier(480);
   public static final BBacnetPropertyIdentifier activeCovMultipleSubscriptions = new BBacnetPropertyIdentifier(481);
   public static final BBacnetPropertyIdentifier protocolLevel = new BBacnetPropertyIdentifier(482);
   public static final BBacnetPropertyIdentifier referencePort = new BBacnetPropertyIdentifier(483);
   public static final BBacnetPropertyIdentifier deployedProfileLocation = new BBacnetPropertyIdentifier(484);
   public static final BBacnetPropertyIdentifier profileLocation = new BBacnetPropertyIdentifier(485);
   public static final BBacnetPropertyIdentifier tags = new BBacnetPropertyIdentifier(486);
   public static final BBacnetPropertyIdentifier subordinateNodeTypes = new BBacnetPropertyIdentifier(487);
   public static final BBacnetPropertyIdentifier subordinateTags = new BBacnetPropertyIdentifier(488);
   public static final BBacnetPropertyIdentifier subordinateRelationships = new BBacnetPropertyIdentifier(489);
   public static final BBacnetPropertyIdentifier defaultSubordinateRelationship = new BBacnetPropertyIdentifier(490);
   public static final BBacnetPropertyIdentifier represents = new BBacnetPropertyIdentifier(491);
   public static final BBacnetPropertyIdentifier defaultPresentValue = new BBacnetPropertyIdentifier(492);
   public static final BBacnetPropertyIdentifier presentStage = new BBacnetPropertyIdentifier(493);
   public static final BBacnetPropertyIdentifier stages = new BBacnetPropertyIdentifier(494);
   public static final BBacnetPropertyIdentifier stageNames = new BBacnetPropertyIdentifier(495);
   public static final BBacnetPropertyIdentifier targetReferences = new BBacnetPropertyIdentifier(496);
   public static final BBacnetPropertyIdentifier auditSourceReporter = new BBacnetPropertyIdentifier(497);
   public static final BBacnetPropertyIdentifier auditLevel = new BBacnetPropertyIdentifier(498);
   public static final BBacnetPropertyIdentifier auditNotificationRecipient = new BBacnetPropertyIdentifier(499);
   public static final BBacnetPropertyIdentifier auditPriorityFilter = new BBacnetPropertyIdentifier(500);
   public static final BBacnetPropertyIdentifier auditableOperations = new BBacnetPropertyIdentifier(501);
   public static final BBacnetPropertyIdentifier deleteOnForward = new BBacnetPropertyIdentifier(502);
   public static final BBacnetPropertyIdentifier maximumSendDelay = new BBacnetPropertyIdentifier(503);
   public static final BBacnetPropertyIdentifier monitoredObjects = new BBacnetPropertyIdentifier(504);
   public static final BBacnetPropertyIdentifier sendNow = new BBacnetPropertyIdentifier(505);
   public static final BBacnetPropertyIdentifier floorNumber = new BBacnetPropertyIdentifier(506);
   public static final BBacnetPropertyIdentifier deviceUuid = new BBacnetPropertyIdentifier(507);
   public static final BBacnetPropertyIdentifier removed1 = new BBacnetPropertyIdentifier(18);
   public static final BBacnetPropertyIdentifier issueConfirmedNotifications = new BBacnetPropertyIdentifier(51);
   public static final BBacnetPropertyIdentifier listOfSessionKeys = new BBacnetPropertyIdentifier(55);
   public static final BBacnetPropertyIdentifier protocolConformanceClass = new BBacnetPropertyIdentifier(95);
   public static final BBacnetPropertyIdentifier recipient = new BBacnetPropertyIdentifier(101);
   public static final BBacnetPropertyIdentifier currentNotifyTime = new BBacnetPropertyIdentifier(129);
   public static final BBacnetPropertyIdentifier previousNotifyTime = new BBacnetPropertyIdentifier(138);
   public static final BBacnetPropertyIdentifier masterExemption = new BBacnetPropertyIdentifier(284);
   public static final BBacnetPropertyIdentifier occupancyExemption = new BBacnetPropertyIdentifier(293);
   public static final BBacnetPropertyIdentifier passbackExemption = new BBacnetPropertyIdentifier(299);
   public static final BBacnetPropertyIdentifier DEFAULT = ackedTransitions;
   public static final Type TYPE = Sys.loadType(BBacnetPropertyIdentifier.class);
   public static final int MAX_ASHRAE_ID = 507;
   public static final int MAX_RESERVED_ID = 511;
   public static final int MAX_ID = 4194303;
   public static final String INVALID_OR_UNSPECIFIED_ID = "Invalid ID";
   @Deprecated
   public static final int LOG_ENABLE = 133;

   public static BBacnetPropertyIdentifier make(int ordinal) {
      return (BBacnetPropertyIdentifier)ackedTransitions.getRange().get(ordinal, false);
   }

   public static BBacnetPropertyIdentifier make(String tag) {
      return (BBacnetPropertyIdentifier)ackedTransitions.getRange().get(tag);
   }

   private BBacnetPropertyIdentifier(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public static String tag(int id) {
      if (DEFAULT.getRange().isOrdinal(id)) {
         return DEFAULT.getRange().getTag(id);
      } else if (isAshrae(id)) {
         return ASHRAE_PREFIX + id;
      } else {
         return isProprietary(id) ? PROPRIETARY_PREFIX + id : "Invalid ID";
      }
   }

   public static int ordinal(String tag) {
      try {
         return DEFAULT.getRange().tagToOrdinal(tag);
      } catch (InvalidEnumException var2) {
         if (tag.startsWith(ASHRAE_PREFIX)) {
            return Integer.parseInt(tag.substring(ASHRAE_PREFIX_LENGTH));
         } else if (tag.startsWith(PROPRIETARY_PREFIX)) {
            return Integer.parseInt(tag.substring(PROPRIETARY_PREFIX_LENGTH));
         } else {
            throw var2;
         }
      }
   }

   public static boolean isProprietary(int id) {
      return id > 511 && id <= 4194303;
   }

   public static boolean isAshrae(int id) {
      return id > 507 && id <= 511;
   }

   public static boolean isValid(int id) {
      return id <= 4194303;
   }

   public static boolean isFixed(int id) {
      return id <= 507;
   }
}
