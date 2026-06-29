package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("SnvtXxx"), @Range("SnvtAmp"), @Range("SnvtAmpMil"), @Range("SnvtAngle"), @Range("SnvtAngleVel"), @Range("SnvtBtuKilo"), @Range("SnvtBtuMega"), @Range("SnvtCharAscii"), @Range("SnvtCount"), @Range("SnvtCountInc"), @Range("SnvtDateCal"), @Range("SnvtDateDay"), @Range("SnvtDateTime"), @Range("SnvtElecKwh"), @Range("SnvtElecWhr"), @Range("SnvtFlow"), @Range("SnvtFlowMil"), @Range("SnvtLength"), @Range("SnvtLengthKilo"), @Range("SnvtLengthMicr"), @Range("SnvtLengthMil"), @Range("SnvtLevCont"), @Range("SnvtLevDisc"), @Range("SnvtMass"), @Range("SnvtMassKilo"), @Range("SnvtMassMega"), @Range("SnvtMassMil"), @Range("SnvtPower"), @Range("SnvtPowerKilo"), @Range("SnvtPpm"), @Range("SnvtPress"), @Range("SnvtRes"), @Range("SnvtResKilo"), @Range("SnvtSoundDb"), @Range("SnvtSpeed"), @Range("SnvtSpeedMil"), @Range("SnvtStrAsc"), @Range("SnvtStrInt"), @Range("SnvtTelcom"), @Range("SnvtTemp"), @Range("SnvtTimePassed"), @Range("SnvtVol"), @Range("SnvtVolKilo"), @Range("SnvtVolMil"), @Range("SnvtVolt"), @Range("SnvtVoltDbmv"), @Range("SnvtVoltKilo"), @Range("SnvtVoltMil"), @Range("SnvtAmpF"), @Range("SnvtAngleF"), @Range("SnvtAngleVelF"), @Range("SnvtCountF"), @Range("SnvtCountIncF"), @Range("SnvtFlowF"), @Range("SnvtLengthF"), @Range("SnvtLevContF"), @Range("SnvtMassF"), @Range("SnvtPowerF"), @Range("SnvtPpmF"), @Range("SnvtPressF"), @Range("SnvtResF"), @Range("SnvtSoundDbF"), @Range("SnvtSpeedF"), @Range("SnvtTempF"), @Range("SnvtTimeF"), @Range("SnvtVolF"), @Range("SnvtVoltF"), @Range("SnvtBtuF"), @Range("SnvtElecWhrF"), @Range("SnvtConfigSrc"), @Range("SnvtColor"), @Range("SnvtGrammage"), @Range("SnvtGrammageF"), @Range("SnvtFileReq"), @Range("SnvtFileStatus"), @Range("SnvtFreqF"), @Range("SnvtFreqHz"), @Range("SnvtFreqKilohz"), @Range("SnvtFreqMilhz"), @Range("SnvtLux"), @Range("SnvtIso7811"), @Range("SnvtLevPercent"), @Range("SnvtMultiplier"), @Range("SnvtState"), @Range("SnvtTimeStamp"), @Range("SnvtZerospan"), @Range("SnvtMagcard"), @Range("SnvtElapsedTm"), @Range("SnvtAlarm"), @Range("SnvtCurrency"), @Range("SnvtFilePos"), @Range("SnvtMuldiv"), @Range("SnvtObjRequest"), @Range("SnvtObjStatus"), @Range("SnvtPreset"), @Range("SnvtSwitch"), @Range("SnvtTransTable"), @Range("SnvtOverride"), @Range("SnvtPwrFact"), @Range("SnvtPwrFactF"), @Range("SnvtDensity"), @Range("SnvtDensityF"), @Range("SnvtRpm"), @Range("SnvtHvacEmerg"), @Range("SnvtAngleDeg"), @Range("SnvtTempP"), @Range("SnvtTempSetpt"), @Range("SnvtTimeSec"), @Range("SnvtHvacMode"), @Range("SnvtOccupancy"), @Range("SnvtArea"), @Range("SnvtHvacOverid"), @Range("SnvtHvacStatus"), @Range("SnvtPressP"), @Range("SnvtAddress"), @Range("SnvtScene"), @Range("SnvtSceneCfg"), @Range("SnvtSetting"), @Range("SnvtEvapState"), @Range("SnvtThermMode"), @Range("SnvtDefrMode"), @Range("SnvtDefrTerm"), @Range("SnvtDefrState"), @Range("SnvtTimeMin"), @Range("SnvtTimeHour"), @Range("SnvtPh"), @Range("SnvtPhF"), @Range("SnvtChlrStatus"), @Range("SnvtTodEvent"), @Range("SnvtSmoObscur"), @Range("SnvtFireTest"), @Range("SnvtTempRor"), @Range("SnvtFireInit"), @Range("SnvtFireIndcte"), @Range("SnvtTimeZone"), @Range("SnvtEarthPos"), @Range("SnvtRegVal"), @Range("SnvtRegValTs"), @Range("SnvtVoltAc"), @Range("SnvtAmpAc"), @Range(
      value = "SnvtTurbidity",
      ordinal = 143
   ), @Range(
      value = "SnvtTurbidityF",
      ordinal = 144
   ), @Range(
      value = "SnvtHvacType",
      ordinal = 145
   ), @Range(
      value = "SnvtElecKwhL",
      ordinal = 146
   ), @Range(
      value = "SnvtTempDiffP",
      ordinal = 147
   ), @Range(
      value = "SnvtCtrlReq",
      ordinal = 148
   ), @Range(
      value = "SnvtCtrlResp",
      ordinal = 149
   ), @Range(
      value = "SnvtPtz",
      ordinal = 150
   ), @Range(
      value = "SnvtPrivacyzone",
      ordinal = 151
   ), @Range(
      value = "SnvtPosCtrl",
      ordinal = 152
   ), @Range(
      value = "SnvtEnthalpy",
      ordinal = 153
   ), @Range(
      value = "SnvtGfciStatus",
      ordinal = 154
   ), @Range(
      value = "SnvtMotorState",
      ordinal = 155
   ), @Range(
      value = "SnvtPumpsetMn",
      ordinal = 156
   ), @Range(
      value = "SnvtExControl",
      ordinal = 157
   ), @Range(
      value = "SnvtPumpsetSn",
      ordinal = 158
   ), @Range(
      value = "SnvtPumpSensor",
      ordinal = 159
   ), @Range(
      value = "SnvtAbsHumid",
      ordinal = 160
   ), @Range(
      value = "SnvtFlowP",
      ordinal = 161
   ), @Range(
      value = "SnvtDevCMode",
      ordinal = 162
   ), @Range(
      value = "SnvtValveMode",
      ordinal = 163
   ), @Range(
      value = "SnvtAlarm2",
      ordinal = 164
   ), @Range(
      value = "SnvtState64",
      ordinal = 165
   ), @Range(
      value = "SnvtNvType",
      ordinal = 166
   ), @Range(
      value = "SnvtEntOpmode",
      ordinal = 168
   ), @Range(
      value = "SnvtEntState",
      ordinal = 169
   ), @Range(
      value = "SnvtEntStatus",
      ordinal = 170
   ), @Range(
      value = "SnvtFlowDir",
      ordinal = 171
   ), @Range(
      value = "SnvtHvacSatsts",
      ordinal = 172
   ), @Range(
      value = "SnvtDevStatus",
      ordinal = 173
   ), @Range(
      value = "SnvtDevFault",
      ordinal = 174
   ), @Range(
      value = "SnvtDevMaint",
      ordinal = 175
   ), @Range(
      value = "SnvtDateEvent",
      ordinal = 176
   ), @Range(
      value = "SnvtSchedVal",
      ordinal = 177
   ), @Range(
      value = "SnvtSecState",
      ordinal = 178
   ), @Range(
      value = "SnvtSecStatus",
      ordinal = 179
   ), @Range(
      value = "SnvtSblndState",
      ordinal = 180
   ), @Range(
      value = "SnvtRacCtrl",
      ordinal = 181
   ), @Range(
      value = "SnvtRacReq",
      ordinal = 182
   ), @Range(
      value = "SnvtCount32",
      ordinal = 183
   ), @Range(
      value = "SnvtClothesWC",
      ordinal = 184
   ), @Range(
      value = "SnvtClothesWM",
      ordinal = 185
   ), @Range(
      value = "SnvtClothesWS",
      ordinal = 186
   ), @Range(
      value = "SnvtClothesWA",
      ordinal = 187
   ), @Range(
      value = "SnvtMultiplierS",
      ordinal = 188
   ), @Range(
      value = "SnvtSwitch2",
      ordinal = 189
   ), @Range(
      value = "SnvtColor2",
      ordinal = 190
   ), @Range(
      value = "SnvtLogStatus",
      ordinal = 191
   ), @Range(
      value = "SnvtTimeStampP",
      ordinal = 192
   ), @Range(
      value = "SnvtLogFxRequest",
      ordinal = 193
   ), @Range(
      value = "SnvtLogFxStatus",
      ordinal = 194
   ), @Range(
      value = "SnvtLogRequest",
      ordinal = 195
   ), @Range(
      value = "SnvtEnthalpyD",
      ordinal = 196
   ), @Range(
      value = "SnvtAmpAcMil",
      ordinal = 197
   ), @Range(
      value = "SnvtTimeHourP",
      ordinal = 198
   ), @Range(
      value = "SnvtLampStatus",
      ordinal = 199
   ), @Range(
      value = "SnvtEnvironment",
      ordinal = 200
   ), @Range(
      value = "SnvtGeoLoc",
      ordinal = 201
   ), @Range(
      value = "SnvtProgramStatus",
      ordinal = 202
   ), @Range(
      value = "SnvtLoadOffsets",
      ordinal = 203
   ), @Range(
      value = "SnvtWm2P",
      ordinal = 204
   ), @Range(
      value = "SnvtSafe1",
      ordinal = 205
   ), @Range(
      value = "SnvtSafe2",
      ordinal = 206
   ), @Range(
      value = "SnvtSafe4",
      ordinal = 207
   ), @Range(
      value = "SnvtSafe8",
      ordinal = 208
   ), @Range(
      value = "SnvtTimeVal2",
      ordinal = 209
   ), @Range(
      value = "SnvtTimeOffset",
      ordinal = 210
   ), @Range(
      value = "SnvtSchedExc",
      ordinal = 211
   ), @Range(
      value = "SnvtSchedStatus",
      ordinal = 212
   ), @Range(
      value = "SnvtMassFlow",
      ordinal = 213
   ), @Range(
      value = "SnvtMassFlowF",
      ordinal = 214
   )}
)
public final class BLonSnvtType extends BFrozenEnum {
   public static final int SNVT_XXX = 0;
   public static final int SNVT_AMP = 1;
   public static final int SNVT_AMP_MIL = 2;
   public static final int SNVT_ANGLE = 3;
   public static final int SNVT_ANGLE_VEL = 4;
   public static final int SNVT_BTU_KILO = 5;
   public static final int SNVT_BTU_MEGA = 6;
   public static final int SNVT_CHAR_ASCII = 7;
   public static final int SNVT_COUNT = 8;
   public static final int SNVT_COUNT_INC = 9;
   public static final int SNVT_DATE_CAL = 10;
   public static final int SNVT_DATE_DAY = 11;
   public static final int SNVT_DATE_TIME = 12;
   public static final int SNVT_ELEC_KWH = 13;
   public static final int SNVT_ELEC_WHR = 14;
   public static final int SNVT_FLOW = 15;
   public static final int SNVT_FLOW_MIL = 16;
   public static final int SNVT_LENGTH = 17;
   public static final int SNVT_LENGTH_KILO = 18;
   public static final int SNVT_LENGTH_MICR = 19;
   public static final int SNVT_LENGTH_MIL = 20;
   public static final int SNVT_LEV_CONT = 21;
   public static final int SNVT_LEV_DISC = 22;
   public static final int SNVT_MASS = 23;
   public static final int SNVT_MASS_KILO = 24;
   public static final int SNVT_MASS_MEGA = 25;
   public static final int SNVT_MASS_MIL = 26;
   public static final int SNVT_POWER = 27;
   public static final int SNVT_POWER_KILO = 28;
   public static final int SNVT_PPM = 29;
   public static final int SNVT_PRESS = 30;
   public static final int SNVT_RES = 31;
   public static final int SNVT_RES_KILO = 32;
   public static final int SNVT_SOUND_DB = 33;
   public static final int SNVT_SPEED = 34;
   public static final int SNVT_SPEED_MIL = 35;
   public static final int SNVT_STR_ASC = 36;
   public static final int SNVT_STR_INT = 37;
   public static final int SNVT_TELCOM = 38;
   public static final int SNVT_TEMP = 39;
   public static final int SNVT_TIME_PASSED = 40;
   public static final int SNVT_VOL = 41;
   public static final int SNVT_VOL_KILO = 42;
   public static final int SNVT_VOL_MIL = 43;
   public static final int SNVT_VOLT = 44;
   public static final int SNVT_VOLT_DBMV = 45;
   public static final int SNVT_VOLT_KILO = 46;
   public static final int SNVT_VOLT_MIL = 47;
   public static final int SNVT_AMP_F = 48;
   public static final int SNVT_ANGLE_F = 49;
   public static final int SNVT_ANGLE_VEL_F = 50;
   public static final int SNVT_COUNT_F = 51;
   public static final int SNVT_COUNT_INC_F = 52;
   public static final int SNVT_FLOW_F = 53;
   public static final int SNVT_LENGTH_F = 54;
   public static final int SNVT_LEV_CONT_F = 55;
   public static final int SNVT_MASS_F = 56;
   public static final int SNVT_POWER_F = 57;
   public static final int SNVT_PPM_F = 58;
   public static final int SNVT_PRESS_F = 59;
   public static final int SNVT_RES_F = 60;
   public static final int SNVT_SOUND_DB_F = 61;
   public static final int SNVT_SPEED_F = 62;
   public static final int SNVT_TEMP_F = 63;
   public static final int SNVT_TIME_F = 64;
   public static final int SNVT_VOL_F = 65;
   public static final int SNVT_VOLT_F = 66;
   public static final int SNVT_BTU_F = 67;
   public static final int SNVT_ELEC_WHR_F = 68;
   public static final int SNVT_CONFIG_SRC = 69;
   public static final int SNVT_COLOR = 70;
   public static final int SNVT_GRAMMAGE = 71;
   public static final int SNVT_GRAMMAGE_F = 72;
   public static final int SNVT_FILE_REQ = 73;
   public static final int SNVT_FILE_STATUS = 74;
   public static final int SNVT_FREQ_F = 75;
   public static final int SNVT_FREQ_HZ = 76;
   public static final int SNVT_FREQ_KILOHZ = 77;
   public static final int SNVT_FREQ_MILHZ = 78;
   public static final int SNVT_LUX = 79;
   public static final int SNVT_ISO_7811 = 80;
   public static final int SNVT_LEV_PERCENT = 81;
   public static final int SNVT_MULTIPLIER = 82;
   public static final int SNVT_STATE = 83;
   public static final int SNVT_TIME_STAMP = 84;
   public static final int SNVT_ZEROSPAN = 85;
   public static final int SNVT_MAGCARD = 86;
   public static final int SNVT_ELAPSED_TM = 87;
   public static final int SNVT_ALARM = 88;
   public static final int SNVT_CURRENCY = 89;
   public static final int SNVT_FILE_POS = 90;
   public static final int SNVT_MULDIV = 91;
   public static final int SNVT_OBJ_REQUEST = 92;
   public static final int SNVT_OBJ_STATUS = 93;
   public static final int SNVT_PRESET = 94;
   public static final int SNVT_SWITCH = 95;
   public static final int SNVT_TRANS_TABLE = 96;
   public static final int SNVT_OVERRIDE = 97;
   public static final int SNVT_PWR_FACT = 98;
   public static final int SNVT_PWR_FACT_F = 99;
   public static final int SNVT_DENSITY = 100;
   public static final int SNVT_DENSITY_F = 101;
   public static final int SNVT_RPM = 102;
   public static final int SNVT_HVAC_EMERG = 103;
   public static final int SNVT_ANGLE_DEG = 104;
   public static final int SNVT_TEMP_P = 105;
   public static final int SNVT_TEMP_SETPT = 106;
   public static final int SNVT_TIME_SEC = 107;
   public static final int SNVT_HVAC_MODE = 108;
   public static final int SNVT_OCCUPANCY = 109;
   public static final int SNVT_AREA = 110;
   public static final int SNVT_HVAC_OVERID = 111;
   public static final int SNVT_HVAC_STATUS = 112;
   public static final int SNVT_PRESS_P = 113;
   public static final int SNVT_ADDRESS = 114;
   public static final int SNVT_SCENE = 115;
   public static final int SNVT_SCENE_CFG = 116;
   public static final int SNVT_SETTING = 117;
   public static final int SNVT_EVAP_STATE = 118;
   public static final int SNVT_THERM_MODE = 119;
   public static final int SNVT_DEFR_MODE = 120;
   public static final int SNVT_DEFR_TERM = 121;
   public static final int SNVT_DEFR_STATE = 122;
   public static final int SNVT_TIME_MIN = 123;
   public static final int SNVT_TIME_HOUR = 124;
   public static final int SNVT_PH = 125;
   public static final int SNVT_PH_F = 126;
   public static final int SNVT_CHLR_STATUS = 127;
   public static final int SNVT_TOD_EVENT = 128;
   public static final int SNVT_SMO_OBSCUR = 129;
   public static final int SNVT_FIRE_TEST = 130;
   public static final int SNVT_TEMP_ROR = 131;
   public static final int SNVT_FIRE_INIT = 132;
   public static final int SNVT_FIRE_INDCTE = 133;
   public static final int SNVT_TIME_ZONE = 134;
   public static final int SNVT_EARTH_POS = 135;
   public static final int SNVT_REG_VAL = 136;
   public static final int SNVT_REG_VAL_TS = 137;
   public static final int SNVT_VOLT_AC = 138;
   public static final int SNVT_AMP_AC = 139;
   public static final int SNVT_TURBIDITY = 143;
   public static final int SNVT_TURBIDITY_F = 144;
   public static final int SNVT_HVAC_TYPE = 145;
   public static final int SNVT_ELEC_KWH_L = 146;
   public static final int SNVT_TEMP_DIFF_P = 147;
   public static final int SNVT_CTRL_REQ = 148;
   public static final int SNVT_CTRL_RESP = 149;
   public static final int SNVT_PTZ = 150;
   public static final int SNVT_PRIVACYZONE = 151;
   public static final int SNVT_POS_CTRL = 152;
   public static final int SNVT_ENTHALPY = 153;
   public static final int SNVT_GFCI_STATUS = 154;
   public static final int SNVT_MOTOR_STATE = 155;
   public static final int SNVT_PUMPSET_MN = 156;
   public static final int SNVT_EX_CONTROL = 157;
   public static final int SNVT_PUMPSET_SN = 158;
   public static final int SNVT_PUMP_SENSOR = 159;
   public static final int SNVT_ABS_HUMID = 160;
   public static final int SNVT_FLOW_P = 161;
   public static final int SNVT_DEV_CMODE = 162;
   public static final int SNVT_VALVE_MODE = 163;
   public static final int SNVT_ALARM_2 = 164;
   public static final int SNVT_STATE_64 = 165;
   public static final int SNVT_NV_TYPE = 166;
   public static final int SNVT_ENT_OPMODE = 168;
   public static final int SNVT_ENT_STATE = 169;
   public static final int SNVT_ENT_STATUS = 170;
   public static final int SNVT_FLOW_DIR = 171;
   public static final int SNVT_HVAC_SATSTS = 172;
   public static final int SNVT_DEV_STATUS = 173;
   public static final int SNVT_DEV_FAULT = 174;
   public static final int SNVT_DEV_MAINT = 175;
   public static final int SNVT_DATE_EVENT = 176;
   public static final int SNVT_SCHED_VAL = 177;
   public static final int SNVT_SEC_STATE = 178;
   public static final int SNVT_SEC_STATUS = 179;
   public static final int SNVT_SBLND_STATE = 180;
   public static final int SNVT_RAC_CTRL = 181;
   public static final int SNVT_RAC_REQ = 182;
   public static final int SNVT_COUNT_32 = 183;
   public static final int SNVT_CLOTHES_WC = 184;
   public static final int SNVT_CLOTHES_WM = 185;
   public static final int SNVT_CLOTHES_WS = 186;
   public static final int SNVT_CLOTHES_WA = 187;
   public static final int SNVT_MULTIPLIER_S = 188;
   public static final int SNVT_SWITCH_2 = 189;
   public static final int SNVT_COLOR_2 = 190;
   public static final int SNVT_LOG_STATUS = 191;
   public static final int SNVT_TIME_STAMP_P = 192;
   public static final int SNVT_LOG_FX_REQUEST = 193;
   public static final int SNVT_LOG_FX_STATUS = 194;
   public static final int SNVT_LOG_REQUEST = 195;
   public static final int SNVT_ENTHALPY_D = 196;
   public static final int SNVT_AMP_AC_MIL = 197;
   public static final int SNVT_TIME_HOUR_P = 198;
   public static final int SNVT_LAMP_STATUS = 199;
   public static final int SNVT_ENVIRONMENT = 200;
   public static final int SNVT_GEO_LOC = 201;
   public static final int SNVT_PROGRAM_STATUS = 202;
   public static final int SNVT_LOAD_OFFSETS = 203;
   public static final int SNVT_WM_2P = 204;
   public static final int SNVT_SAFE_1 = 205;
   public static final int SNVT_SAFE_2 = 206;
   public static final int SNVT_SAFE_4 = 207;
   public static final int SNVT_SAFE_8 = 208;
   public static final int SNVT_TIME_VAL_2 = 209;
   public static final int SNVT_TIME_OFFSET = 210;
   public static final int SNVT_SCHED_EXC = 211;
   public static final int SNVT_SCHED_STATUS = 212;
   public static final int SNVT_MASS_FLOW = 213;
   public static final int SNVT_MASS_FLOW_F = 214;
   public static final BLonSnvtType SnvtXxx = new BLonSnvtType(0);
   public static final BLonSnvtType SnvtAmp = new BLonSnvtType(1);
   public static final BLonSnvtType SnvtAmpMil = new BLonSnvtType(2);
   public static final BLonSnvtType SnvtAngle = new BLonSnvtType(3);
   public static final BLonSnvtType SnvtAngleVel = new BLonSnvtType(4);
   public static final BLonSnvtType SnvtBtuKilo = new BLonSnvtType(5);
   public static final BLonSnvtType SnvtBtuMega = new BLonSnvtType(6);
   public static final BLonSnvtType SnvtCharAscii = new BLonSnvtType(7);
   public static final BLonSnvtType SnvtCount = new BLonSnvtType(8);
   public static final BLonSnvtType SnvtCountInc = new BLonSnvtType(9);
   public static final BLonSnvtType SnvtDateCal = new BLonSnvtType(10);
   public static final BLonSnvtType SnvtDateDay = new BLonSnvtType(11);
   public static final BLonSnvtType SnvtDateTime = new BLonSnvtType(12);
   public static final BLonSnvtType SnvtElecKwh = new BLonSnvtType(13);
   public static final BLonSnvtType SnvtElecWhr = new BLonSnvtType(14);
   public static final BLonSnvtType SnvtFlow = new BLonSnvtType(15);
   public static final BLonSnvtType SnvtFlowMil = new BLonSnvtType(16);
   public static final BLonSnvtType SnvtLength = new BLonSnvtType(17);
   public static final BLonSnvtType SnvtLengthKilo = new BLonSnvtType(18);
   public static final BLonSnvtType SnvtLengthMicr = new BLonSnvtType(19);
   public static final BLonSnvtType SnvtLengthMil = new BLonSnvtType(20);
   public static final BLonSnvtType SnvtLevCont = new BLonSnvtType(21);
   public static final BLonSnvtType SnvtLevDisc = new BLonSnvtType(22);
   public static final BLonSnvtType SnvtMass = new BLonSnvtType(23);
   public static final BLonSnvtType SnvtMassKilo = new BLonSnvtType(24);
   public static final BLonSnvtType SnvtMassMega = new BLonSnvtType(25);
   public static final BLonSnvtType SnvtMassMil = new BLonSnvtType(26);
   public static final BLonSnvtType SnvtPower = new BLonSnvtType(27);
   public static final BLonSnvtType SnvtPowerKilo = new BLonSnvtType(28);
   public static final BLonSnvtType SnvtPpm = new BLonSnvtType(29);
   public static final BLonSnvtType SnvtPress = new BLonSnvtType(30);
   public static final BLonSnvtType SnvtRes = new BLonSnvtType(31);
   public static final BLonSnvtType SnvtResKilo = new BLonSnvtType(32);
   public static final BLonSnvtType SnvtSoundDb = new BLonSnvtType(33);
   public static final BLonSnvtType SnvtSpeed = new BLonSnvtType(34);
   public static final BLonSnvtType SnvtSpeedMil = new BLonSnvtType(35);
   public static final BLonSnvtType SnvtStrAsc = new BLonSnvtType(36);
   public static final BLonSnvtType SnvtStrInt = new BLonSnvtType(37);
   public static final BLonSnvtType SnvtTelcom = new BLonSnvtType(38);
   public static final BLonSnvtType SnvtTemp = new BLonSnvtType(39);
   public static final BLonSnvtType SnvtTimePassed = new BLonSnvtType(40);
   public static final BLonSnvtType SnvtVol = new BLonSnvtType(41);
   public static final BLonSnvtType SnvtVolKilo = new BLonSnvtType(42);
   public static final BLonSnvtType SnvtVolMil = new BLonSnvtType(43);
   public static final BLonSnvtType SnvtVolt = new BLonSnvtType(44);
   public static final BLonSnvtType SnvtVoltDbmv = new BLonSnvtType(45);
   public static final BLonSnvtType SnvtVoltKilo = new BLonSnvtType(46);
   public static final BLonSnvtType SnvtVoltMil = new BLonSnvtType(47);
   public static final BLonSnvtType SnvtAmpF = new BLonSnvtType(48);
   public static final BLonSnvtType SnvtAngleF = new BLonSnvtType(49);
   public static final BLonSnvtType SnvtAngleVelF = new BLonSnvtType(50);
   public static final BLonSnvtType SnvtCountF = new BLonSnvtType(51);
   public static final BLonSnvtType SnvtCountIncF = new BLonSnvtType(52);
   public static final BLonSnvtType SnvtFlowF = new BLonSnvtType(53);
   public static final BLonSnvtType SnvtLengthF = new BLonSnvtType(54);
   public static final BLonSnvtType SnvtLevContF = new BLonSnvtType(55);
   public static final BLonSnvtType SnvtMassF = new BLonSnvtType(56);
   public static final BLonSnvtType SnvtPowerF = new BLonSnvtType(57);
   public static final BLonSnvtType SnvtPpmF = new BLonSnvtType(58);
   public static final BLonSnvtType SnvtPressF = new BLonSnvtType(59);
   public static final BLonSnvtType SnvtResF = new BLonSnvtType(60);
   public static final BLonSnvtType SnvtSoundDbF = new BLonSnvtType(61);
   public static final BLonSnvtType SnvtSpeedF = new BLonSnvtType(62);
   public static final BLonSnvtType SnvtTempF = new BLonSnvtType(63);
   public static final BLonSnvtType SnvtTimeF = new BLonSnvtType(64);
   public static final BLonSnvtType SnvtVolF = new BLonSnvtType(65);
   public static final BLonSnvtType SnvtVoltF = new BLonSnvtType(66);
   public static final BLonSnvtType SnvtBtuF = new BLonSnvtType(67);
   public static final BLonSnvtType SnvtElecWhrF = new BLonSnvtType(68);
   public static final BLonSnvtType SnvtConfigSrc = new BLonSnvtType(69);
   public static final BLonSnvtType SnvtColor = new BLonSnvtType(70);
   public static final BLonSnvtType SnvtGrammage = new BLonSnvtType(71);
   public static final BLonSnvtType SnvtGrammageF = new BLonSnvtType(72);
   public static final BLonSnvtType SnvtFileReq = new BLonSnvtType(73);
   public static final BLonSnvtType SnvtFileStatus = new BLonSnvtType(74);
   public static final BLonSnvtType SnvtFreqF = new BLonSnvtType(75);
   public static final BLonSnvtType SnvtFreqHz = new BLonSnvtType(76);
   public static final BLonSnvtType SnvtFreqKilohz = new BLonSnvtType(77);
   public static final BLonSnvtType SnvtFreqMilhz = new BLonSnvtType(78);
   public static final BLonSnvtType SnvtLux = new BLonSnvtType(79);
   public static final BLonSnvtType SnvtIso7811 = new BLonSnvtType(80);
   public static final BLonSnvtType SnvtLevPercent = new BLonSnvtType(81);
   public static final BLonSnvtType SnvtMultiplier = new BLonSnvtType(82);
   public static final BLonSnvtType SnvtState = new BLonSnvtType(83);
   public static final BLonSnvtType SnvtTimeStamp = new BLonSnvtType(84);
   public static final BLonSnvtType SnvtZerospan = new BLonSnvtType(85);
   public static final BLonSnvtType SnvtMagcard = new BLonSnvtType(86);
   public static final BLonSnvtType SnvtElapsedTm = new BLonSnvtType(87);
   public static final BLonSnvtType SnvtAlarm = new BLonSnvtType(88);
   public static final BLonSnvtType SnvtCurrency = new BLonSnvtType(89);
   public static final BLonSnvtType SnvtFilePos = new BLonSnvtType(90);
   public static final BLonSnvtType SnvtMuldiv = new BLonSnvtType(91);
   public static final BLonSnvtType SnvtObjRequest = new BLonSnvtType(92);
   public static final BLonSnvtType SnvtObjStatus = new BLonSnvtType(93);
   public static final BLonSnvtType SnvtPreset = new BLonSnvtType(94);
   public static final BLonSnvtType SnvtSwitch = new BLonSnvtType(95);
   public static final BLonSnvtType SnvtTransTable = new BLonSnvtType(96);
   public static final BLonSnvtType SnvtOverride = new BLonSnvtType(97);
   public static final BLonSnvtType SnvtPwrFact = new BLonSnvtType(98);
   public static final BLonSnvtType SnvtPwrFactF = new BLonSnvtType(99);
   public static final BLonSnvtType SnvtDensity = new BLonSnvtType(100);
   public static final BLonSnvtType SnvtDensityF = new BLonSnvtType(101);
   public static final BLonSnvtType SnvtRpm = new BLonSnvtType(102);
   public static final BLonSnvtType SnvtHvacEmerg = new BLonSnvtType(103);
   public static final BLonSnvtType SnvtAngleDeg = new BLonSnvtType(104);
   public static final BLonSnvtType SnvtTempP = new BLonSnvtType(105);
   public static final BLonSnvtType SnvtTempSetpt = new BLonSnvtType(106);
   public static final BLonSnvtType SnvtTimeSec = new BLonSnvtType(107);
   public static final BLonSnvtType SnvtHvacMode = new BLonSnvtType(108);
   public static final BLonSnvtType SnvtOccupancy = new BLonSnvtType(109);
   public static final BLonSnvtType SnvtArea = new BLonSnvtType(110);
   public static final BLonSnvtType SnvtHvacOverid = new BLonSnvtType(111);
   public static final BLonSnvtType SnvtHvacStatus = new BLonSnvtType(112);
   public static final BLonSnvtType SnvtPressP = new BLonSnvtType(113);
   public static final BLonSnvtType SnvtAddress = new BLonSnvtType(114);
   public static final BLonSnvtType SnvtScene = new BLonSnvtType(115);
   public static final BLonSnvtType SnvtSceneCfg = new BLonSnvtType(116);
   public static final BLonSnvtType SnvtSetting = new BLonSnvtType(117);
   public static final BLonSnvtType SnvtEvapState = new BLonSnvtType(118);
   public static final BLonSnvtType SnvtThermMode = new BLonSnvtType(119);
   public static final BLonSnvtType SnvtDefrMode = new BLonSnvtType(120);
   public static final BLonSnvtType SnvtDefrTerm = new BLonSnvtType(121);
   public static final BLonSnvtType SnvtDefrState = new BLonSnvtType(122);
   public static final BLonSnvtType SnvtTimeMin = new BLonSnvtType(123);
   public static final BLonSnvtType SnvtTimeHour = new BLonSnvtType(124);
   public static final BLonSnvtType SnvtPh = new BLonSnvtType(125);
   public static final BLonSnvtType SnvtPhF = new BLonSnvtType(126);
   public static final BLonSnvtType SnvtChlrStatus = new BLonSnvtType(127);
   public static final BLonSnvtType SnvtTodEvent = new BLonSnvtType(128);
   public static final BLonSnvtType SnvtSmoObscur = new BLonSnvtType(129);
   public static final BLonSnvtType SnvtFireTest = new BLonSnvtType(130);
   public static final BLonSnvtType SnvtTempRor = new BLonSnvtType(131);
   public static final BLonSnvtType SnvtFireInit = new BLonSnvtType(132);
   public static final BLonSnvtType SnvtFireIndcte = new BLonSnvtType(133);
   public static final BLonSnvtType SnvtTimeZone = new BLonSnvtType(134);
   public static final BLonSnvtType SnvtEarthPos = new BLonSnvtType(135);
   public static final BLonSnvtType SnvtRegVal = new BLonSnvtType(136);
   public static final BLonSnvtType SnvtRegValTs = new BLonSnvtType(137);
   public static final BLonSnvtType SnvtVoltAc = new BLonSnvtType(138);
   public static final BLonSnvtType SnvtAmpAc = new BLonSnvtType(139);
   public static final BLonSnvtType SnvtTurbidity = new BLonSnvtType(143);
   public static final BLonSnvtType SnvtTurbidityF = new BLonSnvtType(144);
   public static final BLonSnvtType SnvtHvacType = new BLonSnvtType(145);
   public static final BLonSnvtType SnvtElecKwhL = new BLonSnvtType(146);
   public static final BLonSnvtType SnvtTempDiffP = new BLonSnvtType(147);
   public static final BLonSnvtType SnvtCtrlReq = new BLonSnvtType(148);
   public static final BLonSnvtType SnvtCtrlResp = new BLonSnvtType(149);
   public static final BLonSnvtType SnvtPtz = new BLonSnvtType(150);
   public static final BLonSnvtType SnvtPrivacyzone = new BLonSnvtType(151);
   public static final BLonSnvtType SnvtPosCtrl = new BLonSnvtType(152);
   public static final BLonSnvtType SnvtEnthalpy = new BLonSnvtType(153);
   public static final BLonSnvtType SnvtGfciStatus = new BLonSnvtType(154);
   public static final BLonSnvtType SnvtMotorState = new BLonSnvtType(155);
   public static final BLonSnvtType SnvtPumpsetMn = new BLonSnvtType(156);
   public static final BLonSnvtType SnvtExControl = new BLonSnvtType(157);
   public static final BLonSnvtType SnvtPumpsetSn = new BLonSnvtType(158);
   public static final BLonSnvtType SnvtPumpSensor = new BLonSnvtType(159);
   public static final BLonSnvtType SnvtAbsHumid = new BLonSnvtType(160);
   public static final BLonSnvtType SnvtFlowP = new BLonSnvtType(161);
   public static final BLonSnvtType SnvtDevCMode = new BLonSnvtType(162);
   public static final BLonSnvtType SnvtValveMode = new BLonSnvtType(163);
   public static final BLonSnvtType SnvtAlarm2 = new BLonSnvtType(164);
   public static final BLonSnvtType SnvtState64 = new BLonSnvtType(165);
   public static final BLonSnvtType SnvtNvType = new BLonSnvtType(166);
   public static final BLonSnvtType SnvtEntOpmode = new BLonSnvtType(168);
   public static final BLonSnvtType SnvtEntState = new BLonSnvtType(169);
   public static final BLonSnvtType SnvtEntStatus = new BLonSnvtType(170);
   public static final BLonSnvtType SnvtFlowDir = new BLonSnvtType(171);
   public static final BLonSnvtType SnvtHvacSatsts = new BLonSnvtType(172);
   public static final BLonSnvtType SnvtDevStatus = new BLonSnvtType(173);
   public static final BLonSnvtType SnvtDevFault = new BLonSnvtType(174);
   public static final BLonSnvtType SnvtDevMaint = new BLonSnvtType(175);
   public static final BLonSnvtType SnvtDateEvent = new BLonSnvtType(176);
   public static final BLonSnvtType SnvtSchedVal = new BLonSnvtType(177);
   public static final BLonSnvtType SnvtSecState = new BLonSnvtType(178);
   public static final BLonSnvtType SnvtSecStatus = new BLonSnvtType(179);
   public static final BLonSnvtType SnvtSblndState = new BLonSnvtType(180);
   public static final BLonSnvtType SnvtRacCtrl = new BLonSnvtType(181);
   public static final BLonSnvtType SnvtRacReq = new BLonSnvtType(182);
   public static final BLonSnvtType SnvtCount32 = new BLonSnvtType(183);
   public static final BLonSnvtType SnvtClothesWC = new BLonSnvtType(184);
   public static final BLonSnvtType SnvtClothesWM = new BLonSnvtType(185);
   public static final BLonSnvtType SnvtClothesWS = new BLonSnvtType(186);
   public static final BLonSnvtType SnvtClothesWA = new BLonSnvtType(187);
   public static final BLonSnvtType SnvtMultiplierS = new BLonSnvtType(188);
   public static final BLonSnvtType SnvtSwitch2 = new BLonSnvtType(189);
   public static final BLonSnvtType SnvtColor2 = new BLonSnvtType(190);
   public static final BLonSnvtType SnvtLogStatus = new BLonSnvtType(191);
   public static final BLonSnvtType SnvtTimeStampP = new BLonSnvtType(192);
   public static final BLonSnvtType SnvtLogFxRequest = new BLonSnvtType(193);
   public static final BLonSnvtType SnvtLogFxStatus = new BLonSnvtType(194);
   public static final BLonSnvtType SnvtLogRequest = new BLonSnvtType(195);
   public static final BLonSnvtType SnvtEnthalpyD = new BLonSnvtType(196);
   public static final BLonSnvtType SnvtAmpAcMil = new BLonSnvtType(197);
   public static final BLonSnvtType SnvtTimeHourP = new BLonSnvtType(198);
   public static final BLonSnvtType SnvtLampStatus = new BLonSnvtType(199);
   public static final BLonSnvtType SnvtEnvironment = new BLonSnvtType(200);
   public static final BLonSnvtType SnvtGeoLoc = new BLonSnvtType(201);
   public static final BLonSnvtType SnvtProgramStatus = new BLonSnvtType(202);
   public static final BLonSnvtType SnvtLoadOffsets = new BLonSnvtType(203);
   public static final BLonSnvtType SnvtWm2P = new BLonSnvtType(204);
   public static final BLonSnvtType SnvtSafe1 = new BLonSnvtType(205);
   public static final BLonSnvtType SnvtSafe2 = new BLonSnvtType(206);
   public static final BLonSnvtType SnvtSafe4 = new BLonSnvtType(207);
   public static final BLonSnvtType SnvtSafe8 = new BLonSnvtType(208);
   public static final BLonSnvtType SnvtTimeVal2 = new BLonSnvtType(209);
   public static final BLonSnvtType SnvtTimeOffset = new BLonSnvtType(210);
   public static final BLonSnvtType SnvtSchedExc = new BLonSnvtType(211);
   public static final BLonSnvtType SnvtSchedStatus = new BLonSnvtType(212);
   public static final BLonSnvtType SnvtMassFlow = new BLonSnvtType(213);
   public static final BLonSnvtType SnvtMassFlowF = new BLonSnvtType(214);
   public static final BLonSnvtType DEFAULT = SnvtXxx;
   public static final Type TYPE = Sys.loadType(BLonSnvtType.class);
   public static final int LAST_SNVT_ID = 214;
   public static final int SNVT_COUNT32 = 183;
   public static final int SNVT_WM_2_P = 204;

   public static BLonSnvtType make(int ordinal) {
      return (BLonSnvtType)SnvtXxx.getRange().get(ordinal, false);
   }

   public static BLonSnvtType make(String tag) {
      return (BLonSnvtType)SnvtXxx.getRange().get(tag);
   }

   private BLonSnvtType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
