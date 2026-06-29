package javax.baja.lonworks.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "unknown",
      ordinal = 0
   ), @Range(
      value = "echelon",
      ordinal = 1
   ), @Range(
      value = "motorola",
      ordinal = 2
   ), @Range(
      value = "ibm",
      ordinal = 3
   ), @Range(
      value = "sild",
      ordinal = 4
   ), @Range(
      value = "helvar",
      ordinal = 5
   ), @Range(
      value = "ahlstrom",
      ordinal = 6
   ), @Range(
      value = "tmi",
      ordinal = 7
   ), @Range(
      value = "danfoss",
      ordinal = 8
   ), @Range(
      value = "iec",
      ordinal = 9
   ), @Range(
      value = "kaba",
      ordinal = 10
   ), @Range(
      value = "ish",
      ordinal = 11
   ), @Range(
      value = "honeywell",
      ordinal = 12
   ), @Range(
      value = "leviton",
      ordinal = 13
   ), @Range(
      value = "grayhill",
      ordinal = 14
   ), @Range(
      value = "smartControls",
      ordinal = 15
   ), @Range(
      value = "andover",
      ordinal = 16
   ), @Range(
      value = "johnsonControls",
      ordinal = 17
   ), @Range(
      value = "heatTimer",
      ordinal = 18
   ), @Range(
      value = "taControl",
      ordinal = 19
   ), @Range(
      value = "groupSchneider",
      ordinal = 20
   ), @Range(
      value = "weidmuller",
      ordinal = 21
   ), @Range(
      value = "siebe",
      ordinal = 22
   ), @Range(
      value = "jGordonDesign",
      ordinal = 23
   ), @Range(
      value = "circon",
      ordinal = 24
   ), @Range(
      value = "staefa",
      ordinal = 25
   ), @Range(
      value = "homeAutomation",
      ordinal = 26
   ), @Range(
      value = "comelta",
      ordinal = 27
   ), @Range(
      value = "hycal",
      ordinal = 28
   ), @Range(
      value = "caradonTrend",
      ordinal = 29
   ), @Range(
      value = "powerMeasurement",
      ordinal = 30
   ), @Range(
      value = "csi",
      ordinal = 31
   ), @Range(
      value = "abb",
      ordinal = 32
   ), @Range(
      value = "electronicSystems",
      ordinal = 33
   ), @Range(
      value = "continentalControl",
      ordinal = 34
   ), @Range(
      value = "msrTechnolgien",
      ordinal = 35
   ), @Range(
      value = "hubbell",
      ordinal = 36
   ), @Range(
      value = "mcquay",
      ordinal = 37
   ), @Range(
      value = "vaisala",
      ordinal = 38
   ), @Range(
      value = "svm",
      ordinal = 39
   ), @Range(
      value = "bircherGebaudeAg",
      ordinal = 40
   ), @Range(
      value = "hachCompany",
      ordinal = 41
   ), @Range(
      value = "theTraneCompany",
      ordinal = 42
   ), @Range(
      value = "lintonSystems",
      ordinal = 43
   ), @Range(
      value = "osmonics",
      ordinal = 44
   ), @Range(
      value = "delmatic",
      ordinal = 45
   ), @Range(
      value = "elmLtd",
      ordinal = 46
   ), @Range(
      value = "philipsLighting",
      ordinal = 47
   ), @Range(
      value = "safeguard",
      ordinal = 48
   ), @Range(
      value = "seaboard",
      ordinal = 49
   ), @Range(
      value = "lighthouse",
      ordinal = 50
   ), @Range(
      value = "auslon",
      ordinal = 51
   ), @Range(
      value = "kabaBenzing",
      ordinal = 52
   ), @Range(
      value = "rpRichards",
      ordinal = 53
   ), @Range(
      value = "camilleBauer",
      ordinal = 54
   ), @Range(
      value = "honeywell37",
      ordinal = 55
   ), @Range(
      value = "programmedWater",
      ordinal = 56
   ), @Range(
      value = "magnetek",
      ordinal = 57
   ), @Range(
      value = "mentzelUndKrutmann",
      ordinal = 58
   ), @Range(
      value = "zellwegerAnalytics",
      ordinal = 59
   ), @Range(
      value = "tlon",
      ordinal = 60
   ), @Range(
      value = "enermet",
      ordinal = 61
   ), @Range(
      value = "orasGroup",
      ordinal = 62
   ), @Range(
      value = "mstAnalytics",
      ordinal = 63
   ), @Range(
      value = "dhElektronikAnlagenbau",
      ordinal = 64
   ), @Range(
      value = "alyaInternational",
      ordinal = 65
   ), @Range(
      value = "crystalControls",
      ordinal = 66
   ), @Range(
      value = "yokogawa",
      ordinal = 67
   ), @Range(
      value = "douglasPowerEquip",
      ordinal = 68
   ), @Range(
      value = "develcoElectronik",
      ordinal = 69
   ), @Range(
      value = "gebruderTroxGmb",
      ordinal = 70
   ), @Range(
      value = "tsiInc",
      ordinal = 71
   ), @Range(
      value = "rikenKeikiCo",
      ordinal = 72
   ), @Range(
      value = "gesytecGmbh",
      ordinal = 73
   ), @Range(
      value = "cumminsEngineCo",
      ordinal = 74
   ), @Range(
      value = "landertMotorenAg",
      ordinal = 75
   ), @Range(
      value = "toshibaCorp",
      ordinal = 76
   ), @Range(
      value = "satronInstrumentsInc",
      ordinal = 77
   ), @Range(
      value = "toshibaInfoSystems",
      ordinal = 78
   ), @Range(
      value = "fujiElectricCo",
      ordinal = 80
   ), @Range(
      value = "computerProcessControls",
      ordinal = 81
   ), @Range(
      value = "somfy",
      ordinal = 82
   ), @Range(
      value = "alcoControls",
      ordinal = 83
   ), @Range(
      value = "keleAndAssociates",
      ordinal = 84
   ), @Range(
      value = "grundfosElectronics",
      ordinal = 85
   ), @Range(
      value = "zoneControlsKb",
      ordinal = 86
   ), @Range(
      value = "reko",
      ordinal = 87
   ), @Range(
      value = "coactiveNetworksInc",
      ordinal = 89
   ), @Range(
      value = "nodusGmbh",
      ordinal = 90
   ), @Range(
      value = "acutherm",
      ordinal = 91
   ), @Range(
      value = "sontayOpenSystems",
      ordinal = 92
   ), @Range(
      value = "cAndKSystems",
      ordinal = 93
   ), @Range(
      value = "sysmikGmbh",
      ordinal = 94
   ), @Range(
      value = "yamatakeCorp",
      ordinal = 95
   ), @Range(
      value = "ctiProducts",
      ordinal = 96
   ), @Range(
      value = "belimoAutomation",
      ordinal = 97
   ), @Range(
      value = "neurologicResearch",
      ordinal = 98
   ), @Range(
      value = "cnaEngineers",
      ordinal = 99
   ), @Range(
      value = "energyControlsInternational",
      ordinal = 100
   ), @Range(
      value = "frSauterAg",
      ordinal = 101
   ), @Range(
      value = "teldaElectronics",
      ordinal = 102
   ), @Range(
      value = "comtecTechnologie",
      ordinal = 103
   ), @Range(
      value = "abbGebaudetechnikAg",
      ordinal = 104
   ), @Range(
      value = "siemensStaefaControlsUsa",
      ordinal = 105
   ), @Range(
      value = "luxmateControlsGmbh",
      ordinal = 106
   ), @Range(
      value = "matrixControls",
      ordinal = 107
   ), @Range(
      value = "huppeFormSonnenschutzsysteme",
      ordinal = 108
   ), @Range(
      value = "samsungHeavyIndustries",
      ordinal = 110
   ), @Range(
      value = "kitzCorp",
      ordinal = 111
   ), @Range(
      value = "wago",
      ordinal = 112
   ), @Range(
      value = "matsushitaElectricWorks",
      ordinal = 113
   ), @Range(
      value = "siemensLandisStaefaKorea",
      ordinal = 114
   ), @Range(
      value = "samsonAg",
      ordinal = 115
   ), @Range(
      value = "enelIt",
      ordinal = 116
   ), @Range(
      value = "vapacHumidityControls",
      ordinal = 117
   ), @Range(
      value = "dciCo",
      ordinal = 118
   ), @Range(
      value = "yorkInternationalCorp",
      ordinal = 119
   ), @Range(
      value = "legrand",
      ordinal = 120
   ), @Range(
      value = "wabtecCorp",
      ordinal = 121
   ), @Range(
      value = "reginAb",
      ordinal = 122
   ), @Range(
      value = "watanabeElectricIndustryCo",
      ordinal = 123
   ), @Range(
      value = "firecom",
      ordinal = 124
   ), @Range(
      value = "australonEnterprises",
      ordinal = 125
   ), @Range(
      value = "meikosha",
      ordinal = 126
   ), @Range(
      value = "knorrBrakeCorp",
      ordinal = 127
   ), @Range(
      value = "viessmannWerke",
      ordinal = 128
   ), @Range(
      value = "siemensLandisUsa",
      ordinal = 129
   ), @Range(
      value = "kongsbergAnalogic",
      ordinal = 130
   ), @Range(
      value = "distechControls",
      ordinal = 131
   ), @Range(
      value = "idecIzumiCorp",
      ordinal = 132
   ), @Range(
      value = "toshibaLighting",
      ordinal = 133
   ), @Range(
      value = "reserved",
      ordinal = 134
   ), @Range(
      value = "daikinIndustries",
      ordinal = 135
   ), @Range(
      value = "rockwellAutomation",
      ordinal = 136
   ), @Range(
      value = "alstonTransport",
      ordinal = 137
   ), @Range(
      value = "luminator",
      ordinal = 138
   ), @Range(
      value = "hyundaiAutonetCo",
      ordinal = 139
   ), @Range(
      value = "pdlIndustries",
      ordinal = 140
   ), @Range(
      value = "plexusTechnology",
      ordinal = 141
   ), @Range(
      value = "tridium",
      ordinal = 142
   ), @Range(
      value = "ercoLeuchten",
      ordinal = 143
   ), @Range(
      value = "cetelab",
      ordinal = 144
   ), @Range(
      value = "ciac",
      ordinal = 145
   ), @Range(
      value = "networkControls",
      ordinal = 146
   ), @Range(
      value = "valvconCorp",
      ordinal = 147
   ), @Range(
      value = "carel",
      ordinal = 148
   ), @Range(
      value = "fieldServerTechnologies",
      ordinal = 149
   ), @Range(
      value = "halenSmartCompany",
      ordinal = 150
   ), @Range(
      value = "faiveley",
      ordinal = 151
   ), @Range(
      value = "lonMarkTechnicalStaff",
      ordinal = 159
   ), @Range(
      value = "axsysAutomation",
      ordinal = 160
   ), @Range(
      value = "adicCo",
      ordinal = 161
   ), @Range(
      value = "mitsubishiElectricCorp",
      ordinal = 162
   ), @Range(
      value = "hermos",
      ordinal = 163
   ), @Range(
      value = "kiebackandPeter",
      ordinal = 164
   ), @Range(
      value = "terasakiElectricCo",
      ordinal = 165
   ), @Range(
      value = "microlabSistemiSrl",
      ordinal = 166
   ), @Range(
      value = "wattStopper",
      ordinal = 167
   ), @Range(
      value = "aquametro",
      ordinal = 168
   ), @Range(
      value = "infranetPartners",
      ordinal = 169
   ), @Range(
      value = "stifabFarex",
      ordinal = 170
   ), @Range(
      value = "agtatec",
      ordinal = 171
   ), @Range(
      value = "surfNetworks",
      ordinal = 172
   ), @Range(
      value = "kamstrup",
      ordinal = 173
   ), @Range(
      value = "gentec",
      ordinal = 174
   ), @Range(
      value = "cypressSemiconductor",
      ordinal = 175
   ), @Range(
      value = "intellicomInnovation",
      ordinal = 176
   ), @Range(
      value = "shikokuInstrumentation",
      ordinal = 177
   ), @Range(
      value = "carrierCorporation",
      ordinal = 178
   ), @Range(
      value = "shanghaiChangXiangComputer",
      ordinal = 179
   ), @Range(
      value = "raypak",
      ordinal = 180
   ), @Range(
      value = "nicoTechnology",
      ordinal = 181
   ), @Range(
      value = "lochinvarCorporation",
      ordinal = 182
   ), @Range(
      value = "programmedWaterTech",
      ordinal = 183
   ), @Range(
      value = "kaifaTechnology",
      ordinal = 184
   ), @Range(
      value = "capelon",
      ordinal = 185
   ), @Range(
      value = "oas",
      ordinal = 186
   ), @Range(
      value = "microTask",
      ordinal = 187
   ), @Range(
      value = "pureChoice",
      ordinal = 188
   ), @Range(
      value = "vaconPlc",
      ordinal = 189
   ), @Range(
      value = "orionCI",
      ordinal = 190
   ), @Range(
      value = "samsungElectronics",
      ordinal = 191
   ), @Range(
      value = "drucegrove",
      ordinal = 192
   ), @Range(
      value = "janitzaElectronic",
      ordinal = 193
   ), @Range(
      value = "oilesCorporation",
      ordinal = 194
   ), @Range(
      value = "osakiElectric",
      ordinal = 196
   ), @Range(
      value = "viconicsElectronics",
      ordinal = 197
   ), @Range(
      value = "fujiElectricSystems",
      ordinal = 198
   ), @Range(
      value = "hubbellBuildingAutomation",
      ordinal = 199
   ), @Range(
      value = "zanderFacilityEngineering",
      ordinal = 200
   ), @Range(
      value = "solidyneCorp",
      ordinal = 201
   ), @Range(
      value = "badgerMeter",
      ordinal = 202
   ), @Range(
      value = "draegerSafety",
      ordinal = 203
   ), @Range(
      value = "lgElectronics",
      ordinal = 204
   ), @Range(
      value = "hitachi",
      ordinal = 205
   ), @Range(
      value = "gorenje",
      ordinal = 206
   ), @Range(
      value = "functionalDevices",
      ordinal = 207
   ), @Range(
      value = "onicon",
      ordinal = 208
   ), @Range(
      value = "electronicTheatreControls",
      ordinal = 209
   ), @Range(
      value = "gulfSecurity",
      ordinal = 210
   ), @Range(
      value = "controlTechniques",
      ordinal = 211
   ), @Range(
      value = "phoenixControls",
      ordinal = 212
   ), @Range(
      value = "vaComTechnologies",
      ordinal = 213
   ), @Range(
      value = "buildingAutomation",
      ordinal = 214
   ), @Range(
      value = "loytec",
      ordinal = 215
   ), @Range(
      value = "spiSystems",
      ordinal = 216
   ), @Range(
      value = "quantumAutomation",
      ordinal = 217
   ), @Range(
      value = "lsIndustrialSystems",
      ordinal = 218
   ), @Range(
      value = "nanjingLianhongAutomation",
      ordinal = 219
   ), @Range(
      value = "sitecoControl",
      ordinal = 220
   ), @Range(
      value = "voyantSolutions",
      ordinal = 221
   ), @Range(
      value = "elkaElektronik",
      ordinal = 222
   ), @Range(
      value = "mSystem",
      ordinal = 223
   ), @Range(
      value = "schneiderElectric",
      ordinal = 224
   ), @Range(
      value = "isde",
      ordinal = 225
   ), @Range(
      value = "paragonControls",
      ordinal = 226
   ), @Range(
      value = "schneiderElectricMerten",
      ordinal = 227
   ), @Range(
      value = "picElectronics",
      ordinal = 228
   ), @Range(
      value = "airTestTechnologies",
      ordinal = 229
   ), @Range(
      value = "spega",
      ordinal = 230
   ), @Range(
      value = "hunterDouglas",
      ordinal = 231
   ), @Range(
      value = "lennoxIndustries",
      ordinal = 232
   ), @Range(
      value = "citylone",
      ordinal = 233
   ), @Range(
      value = "samsungSds",
      ordinal = 234
   ), @Range(
      value = "gdMideaHeatingAndVentEquip",
      ordinal = 235
   ), @Range(
      value = "vosslohSchwabeDeutschland",
      ordinal = 236
   ), @Range(
      value = "verisIndustries",
      ordinal = 237
   ), @Range(
      value = "blueEarthInc",
      ordinal = 238
   ), @Range(
      value = "benHtsAg",
      ordinal = 239
   ), @Range(
      value = "hoshizakiAmerica",
      ordinal = 240
   ), @Range(
      value = "honeywellEmon",
      ordinal = 241
   ), @Range(
      value = "simon",
      ordinal = 242
   ), @Range(
      value = "sloanValve",
      ordinal = 243
   ), @Range(
      value = "trustbridge",
      ordinal = 244
   ), @Range(
      value = "mangelberger",
      ordinal = 245
   ), @Range(
      value = "secyourit",
      ordinal = 246
   ), @Range(
      value = "guangdongRongwen",
      ordinal = 247
   ), @Range(
      value = "ecosian",
      ordinal = 248
   ), @Range(
      value = "apanet",
      ordinal = 249
   ), @Range(
      value = "lonMarkAfs1",
      ordinal = 10479
   ), @Range(
      value = "honeywellFieldProgrammed",
      ordinal = 13108
   ), @Range(
      value = "celsiusBeneluxBV",
      ordinal = 1048132
   )}
)
public final class BLonMfgId extends BFrozenEnum {
   public static final int UNKNOWN = 0;
   public static final int ECHELON = 1;
   public static final int MOTOROLA = 2;
   public static final int IBM = 3;
   public static final int SILD = 4;
   public static final int HELVAR = 5;
   public static final int AHLSTROM = 6;
   public static final int TMI = 7;
   public static final int DANFOSS = 8;
   public static final int IEC = 9;
   public static final int KABA = 10;
   public static final int ISH = 11;
   public static final int HONEYWELL = 12;
   public static final int LEVITON = 13;
   public static final int GRAYHILL = 14;
   public static final int SMART_CONTROLS = 15;
   public static final int ANDOVER = 16;
   public static final int JOHNSON_CONTROLS = 17;
   public static final int HEAT_TIMER = 18;
   public static final int TA_CONTROL = 19;
   public static final int GROUP_SCHNEIDER = 20;
   public static final int WEIDMULLER = 21;
   public static final int SIEBE = 22;
   public static final int J_GORDON_DESIGN = 23;
   public static final int CIRCON = 24;
   public static final int STAEFA = 25;
   public static final int HOME_AUTOMATION = 26;
   public static final int COMELTA = 27;
   public static final int HYCAL = 28;
   public static final int CARADON_TREND = 29;
   public static final int POWER_MEASUREMENT = 30;
   public static final int CSI = 31;
   public static final int ABB = 32;
   public static final int ELECTRONIC_SYSTEMS = 33;
   public static final int CONTINENTAL_CONTROL = 34;
   public static final int MSR_TECHNOLGIEN = 35;
   public static final int HUBBELL = 36;
   public static final int MCQUAY = 37;
   public static final int VAISALA = 38;
   public static final int SVM = 39;
   public static final int BIRCHER_GEBAUDE_AG = 40;
   public static final int HACH_COMPANY = 41;
   public static final int THE_TRANE_COMPANY = 42;
   public static final int LINTON_SYSTEMS = 43;
   public static final int OSMONICS = 44;
   public static final int DELMATIC = 45;
   public static final int ELM_LTD = 46;
   public static final int PHILIPS_LIGHTING = 47;
   public static final int SAFEGUARD = 48;
   public static final int SEABOARD = 49;
   public static final int LIGHTHOUSE = 50;
   public static final int AUSLON = 51;
   public static final int KABA_BENZING = 52;
   public static final int RP_RICHARDS = 53;
   public static final int CAMILLE_BAUER = 54;
   public static final int HONEYWELL_37 = 55;
   public static final int PROGRAMMED_WATER = 56;
   public static final int MAGNETEK = 57;
   public static final int MENTZEL_UND_KRUTMANN = 58;
   public static final int ZELLWEGER_ANALYTICS = 59;
   public static final int TLON = 60;
   public static final int ENERMET = 61;
   public static final int ORAS_GROUP = 62;
   public static final int MST_ANALYTICS = 63;
   public static final int DH_ELEKTRONIK_ANLAGENBAU = 64;
   public static final int ALYA_INTERNATIONAL = 65;
   public static final int CRYSTAL_CONTROLS = 66;
   public static final int YOKOGAWA = 67;
   public static final int DOUGLAS_POWER_EQUIP = 68;
   public static final int DEVELCO_ELECTRONIK = 69;
   public static final int GEBRUDER_TROX_GMB = 70;
   public static final int TSI_INC = 71;
   public static final int RIKEN_KEIKI_CO = 72;
   public static final int GESYTEC_GMBH = 73;
   public static final int CUMMINS_ENGINE_CO = 74;
   public static final int LANDERT_MOTOREN_AG = 75;
   public static final int TOSHIBA_CORP = 76;
   public static final int SATRON_INSTRUMENTS_INC = 77;
   public static final int TOSHIBA_INFO_SYSTEMS = 78;
   public static final int FUJI_ELECTRIC_CO = 80;
   public static final int COMPUTER_PROCESS_CONTROLS = 81;
   public static final int SOMFY = 82;
   public static final int ALCO_CONTROLS = 83;
   public static final int KELE_AND_ASSOCIATES = 84;
   public static final int GRUNDFOS_ELECTRONICS = 85;
   public static final int ZONE_CONTROLS_KB = 86;
   public static final int REKO = 87;
   public static final int COACTIVE_NETWORKS_INC = 89;
   public static final int NODUS_GMBH = 90;
   public static final int ACUTHERM = 91;
   public static final int SONTAY_OPEN_SYSTEMS = 92;
   public static final int C_AND_KSYSTEMS = 93;
   public static final int SYSMIK_GMBH = 94;
   public static final int YAMATAKE_CORP = 95;
   public static final int CTI_PRODUCTS = 96;
   public static final int BELIMO_AUTOMATION = 97;
   public static final int NEUROLOGIC_RESEARCH = 98;
   public static final int CNA_ENGINEERS = 99;
   public static final int ENERGY_CONTROLS_INTERNATIONAL = 100;
   public static final int FR_SAUTER_AG = 101;
   public static final int TELDA_ELECTRONICS = 102;
   public static final int COMTEC_TECHNOLOGIE = 103;
   public static final int ABB_GEBAUDETECHNIK_AG = 104;
   public static final int SIEMENS_STAEFA_CONTROLS_USA = 105;
   public static final int LUXMATE_CONTROLS_GMBH = 106;
   public static final int MATRIX_CONTROLS = 107;
   public static final int HUPPE_FORM_SONNENSCHUTZSYSTEME = 108;
   public static final int SAMSUNG_HEAVY_INDUSTRIES = 110;
   public static final int KITZ_CORP = 111;
   public static final int WAGO = 112;
   public static final int MATSUSHITA_ELECTRIC_WORKS = 113;
   public static final int SIEMENS_LANDIS_STAEFA_KOREA = 114;
   public static final int SAMSON_AG = 115;
   public static final int ENEL_IT = 116;
   public static final int VAPAC_HUMIDITY_CONTROLS = 117;
   public static final int DCI_CO = 118;
   public static final int YORK_INTERNATIONAL_CORP = 119;
   public static final int LEGRAND = 120;
   public static final int WABTEC_CORP = 121;
   public static final int REGIN_AB = 122;
   public static final int WATANABE_ELECTRIC_INDUSTRY_CO = 123;
   public static final int FIRECOM = 124;
   public static final int AUSTRALON_ENTERPRISES = 125;
   public static final int MEIKOSHA = 126;
   public static final int KNORR_BRAKE_CORP = 127;
   public static final int VIESSMANN_WERKE = 128;
   public static final int SIEMENS_LANDIS_USA = 129;
   public static final int KONGSBERG_ANALOGIC = 130;
   public static final int DISTECH_CONTROLS = 131;
   public static final int IDEC_IZUMI_CORP = 132;
   public static final int TOSHIBA_LIGHTING = 133;
   public static final int RESERVED = 134;
   public static final int DAIKIN_INDUSTRIES = 135;
   public static final int ROCKWELL_AUTOMATION = 136;
   public static final int ALSTON_TRANSPORT = 137;
   public static final int LUMINATOR = 138;
   public static final int HYUNDAI_AUTONET_CO = 139;
   public static final int PDL_INDUSTRIES = 140;
   public static final int PLEXUS_TECHNOLOGY = 141;
   public static final int TRIDIUM = 142;
   public static final int ERCO_LEUCHTEN = 143;
   public static final int CETELAB = 144;
   public static final int CIAC = 145;
   public static final int NETWORK_CONTROLS = 146;
   public static final int VALVCON_CORP = 147;
   public static final int CAREL = 148;
   public static final int FIELD_SERVER_TECHNOLOGIES = 149;
   public static final int HALEN_SMART_COMPANY = 150;
   public static final int FAIVELEY = 151;
   public static final int LON_MARK_TECHNICAL_STAFF = 159;
   public static final int AXSYS_AUTOMATION = 160;
   public static final int ADIC_CO = 161;
   public static final int MITSUBISHI_ELECTRIC_CORP = 162;
   public static final int HERMOS = 163;
   public static final int KIEBACKAND_PETER = 164;
   public static final int TERASAKI_ELECTRIC_CO = 165;
   public static final int MICROLAB_SISTEMI_SRL = 166;
   public static final int WATT_STOPPER = 167;
   public static final int AQUAMETRO = 168;
   public static final int INFRANET_PARTNERS = 169;
   public static final int STIFAB_FAREX = 170;
   public static final int AGTATEC = 171;
   public static final int SURF_NETWORKS = 172;
   public static final int KAMSTRUP = 173;
   public static final int GENTEC = 174;
   public static final int CYPRESS_SEMICONDUCTOR = 175;
   public static final int INTELLICOM_INNOVATION = 176;
   public static final int SHIKOKU_INSTRUMENTATION = 177;
   public static final int CARRIER_CORPORATION = 178;
   public static final int SHANGHAI_CHANG_XIANG_COMPUTER = 179;
   public static final int RAYPAK = 180;
   public static final int NICO_TECHNOLOGY = 181;
   public static final int LOCHINVAR_CORPORATION = 182;
   public static final int PROGRAMMED_WATER_TECH = 183;
   public static final int KAIFA_TECHNOLOGY = 184;
   public static final int CAPELON = 185;
   public static final int OAS = 186;
   public static final int MICRO_TASK = 187;
   public static final int PURE_CHOICE = 188;
   public static final int VACON_PLC = 189;
   public static final int ORION_CI = 190;
   public static final int SAMSUNG_ELECTRONICS = 191;
   public static final int DRUCEGROVE = 192;
   public static final int JANITZA_ELECTRONIC = 193;
   public static final int OILES_CORPORATION = 194;
   public static final int OSAKI_ELECTRIC = 196;
   public static final int VICONICS_ELECTRONICS = 197;
   public static final int FUJI_ELECTRIC_SYSTEMS = 198;
   public static final int HUBBELL_BUILDING_AUTOMATION = 199;
   public static final int ZANDER_FACILITY_ENGINEERING = 200;
   public static final int SOLIDYNE_CORP = 201;
   public static final int BADGER_METER = 202;
   public static final int DRAEGER_SAFETY = 203;
   public static final int LG_ELECTRONICS = 204;
   public static final int HITACHI = 205;
   public static final int GORENJE = 206;
   public static final int FUNCTIONAL_DEVICES = 207;
   public static final int ONICON = 208;
   public static final int ELECTRONIC_THEATRE_CONTROLS = 209;
   public static final int GULF_SECURITY = 210;
   public static final int CONTROL_TECHNIQUES = 211;
   public static final int PHOENIX_CONTROLS = 212;
   public static final int VA_COM_TECHNOLOGIES = 213;
   public static final int BUILDING_AUTOMATION = 214;
   public static final int LOYTEC = 215;
   public static final int SPI_SYSTEMS = 216;
   public static final int QUANTUM_AUTOMATION = 217;
   public static final int LS_INDUSTRIAL_SYSTEMS = 218;
   public static final int NANJING_LIANHONG_AUTOMATION = 219;
   public static final int SITECO_CONTROL = 220;
   public static final int VOYANT_SOLUTIONS = 221;
   public static final int ELKA_ELEKTRONIK = 222;
   public static final int M_SYSTEM = 223;
   public static final int SCHNEIDER_ELECTRIC = 224;
   public static final int ISDE = 225;
   public static final int PARAGON_CONTROLS = 226;
   public static final int SCHNEIDER_ELECTRIC_MERTEN = 227;
   public static final int PIC_ELECTRONICS = 228;
   public static final int AIR_TEST_TECHNOLOGIES = 229;
   public static final int SPEGA = 230;
   public static final int HUNTER_DOUGLAS = 231;
   public static final int LENNOX_INDUSTRIES = 232;
   public static final int CITYLONE = 233;
   public static final int SAMSUNG_SDS = 234;
   public static final int GD_MIDEA_HEATING_AND_VENT_EQUIP = 235;
   public static final int VOSSLOH_SCHWABE_DEUTSCHLAND = 236;
   public static final int VERIS_INDUSTRIES = 237;
   public static final int BLUE_EARTH_INC = 238;
   public static final int BEN_HTS_AG = 239;
   public static final int HOSHIZAKI_AMERICA = 240;
   public static final int HONEYWELL_EMON = 241;
   public static final int SIMON = 242;
   public static final int SLOAN_VALVE = 243;
   public static final int TRUSTBRIDGE = 244;
   public static final int MANGELBERGER = 245;
   public static final int SECYOURIT = 246;
   public static final int GUANGDONG_RONGWEN = 247;
   public static final int ECOSIAN = 248;
   public static final int APANET = 249;
   public static final int LON_MARK_AFS_1 = 10479;
   public static final int HONEYWELL_FIELD_PROGRAMMED = 13108;
   public static final int CELSIUS_BENELUX_BV = 1048132;
   public static final BLonMfgId unknown = new BLonMfgId(0);
   public static final BLonMfgId echelon = new BLonMfgId(1);
   public static final BLonMfgId motorola = new BLonMfgId(2);
   public static final BLonMfgId ibm = new BLonMfgId(3);
   public static final BLonMfgId sild = new BLonMfgId(4);
   public static final BLonMfgId helvar = new BLonMfgId(5);
   public static final BLonMfgId ahlstrom = new BLonMfgId(6);
   public static final BLonMfgId tmi = new BLonMfgId(7);
   public static final BLonMfgId danfoss = new BLonMfgId(8);
   public static final BLonMfgId iec = new BLonMfgId(9);
   public static final BLonMfgId kaba = new BLonMfgId(10);
   public static final BLonMfgId ish = new BLonMfgId(11);
   public static final BLonMfgId honeywell = new BLonMfgId(12);
   public static final BLonMfgId leviton = new BLonMfgId(13);
   public static final BLonMfgId grayhill = new BLonMfgId(14);
   public static final BLonMfgId smartControls = new BLonMfgId(15);
   public static final BLonMfgId andover = new BLonMfgId(16);
   public static final BLonMfgId johnsonControls = new BLonMfgId(17);
   public static final BLonMfgId heatTimer = new BLonMfgId(18);
   public static final BLonMfgId taControl = new BLonMfgId(19);
   public static final BLonMfgId groupSchneider = new BLonMfgId(20);
   public static final BLonMfgId weidmuller = new BLonMfgId(21);
   public static final BLonMfgId siebe = new BLonMfgId(22);
   public static final BLonMfgId jGordonDesign = new BLonMfgId(23);
   public static final BLonMfgId circon = new BLonMfgId(24);
   public static final BLonMfgId staefa = new BLonMfgId(25);
   public static final BLonMfgId homeAutomation = new BLonMfgId(26);
   public static final BLonMfgId comelta = new BLonMfgId(27);
   public static final BLonMfgId hycal = new BLonMfgId(28);
   public static final BLonMfgId caradonTrend = new BLonMfgId(29);
   public static final BLonMfgId powerMeasurement = new BLonMfgId(30);
   public static final BLonMfgId csi = new BLonMfgId(31);
   public static final BLonMfgId abb = new BLonMfgId(32);
   public static final BLonMfgId electronicSystems = new BLonMfgId(33);
   public static final BLonMfgId continentalControl = new BLonMfgId(34);
   public static final BLonMfgId msrTechnolgien = new BLonMfgId(35);
   public static final BLonMfgId hubbell = new BLonMfgId(36);
   public static final BLonMfgId mcquay = new BLonMfgId(37);
   public static final BLonMfgId vaisala = new BLonMfgId(38);
   public static final BLonMfgId svm = new BLonMfgId(39);
   public static final BLonMfgId bircherGebaudeAg = new BLonMfgId(40);
   public static final BLonMfgId hachCompany = new BLonMfgId(41);
   public static final BLonMfgId theTraneCompany = new BLonMfgId(42);
   public static final BLonMfgId lintonSystems = new BLonMfgId(43);
   public static final BLonMfgId osmonics = new BLonMfgId(44);
   public static final BLonMfgId delmatic = new BLonMfgId(45);
   public static final BLonMfgId elmLtd = new BLonMfgId(46);
   public static final BLonMfgId philipsLighting = new BLonMfgId(47);
   public static final BLonMfgId safeguard = new BLonMfgId(48);
   public static final BLonMfgId seaboard = new BLonMfgId(49);
   public static final BLonMfgId lighthouse = new BLonMfgId(50);
   public static final BLonMfgId auslon = new BLonMfgId(51);
   public static final BLonMfgId kabaBenzing = new BLonMfgId(52);
   public static final BLonMfgId rpRichards = new BLonMfgId(53);
   public static final BLonMfgId camilleBauer = new BLonMfgId(54);
   public static final BLonMfgId honeywell37 = new BLonMfgId(55);
   public static final BLonMfgId programmedWater = new BLonMfgId(56);
   public static final BLonMfgId magnetek = new BLonMfgId(57);
   public static final BLonMfgId mentzelUndKrutmann = new BLonMfgId(58);
   public static final BLonMfgId zellwegerAnalytics = new BLonMfgId(59);
   public static final BLonMfgId tlon = new BLonMfgId(60);
   public static final BLonMfgId enermet = new BLonMfgId(61);
   public static final BLonMfgId orasGroup = new BLonMfgId(62);
   public static final BLonMfgId mstAnalytics = new BLonMfgId(63);
   public static final BLonMfgId dhElektronikAnlagenbau = new BLonMfgId(64);
   public static final BLonMfgId alyaInternational = new BLonMfgId(65);
   public static final BLonMfgId crystalControls = new BLonMfgId(66);
   public static final BLonMfgId yokogawa = new BLonMfgId(67);
   public static final BLonMfgId douglasPowerEquip = new BLonMfgId(68);
   public static final BLonMfgId develcoElectronik = new BLonMfgId(69);
   public static final BLonMfgId gebruderTroxGmb = new BLonMfgId(70);
   public static final BLonMfgId tsiInc = new BLonMfgId(71);
   public static final BLonMfgId rikenKeikiCo = new BLonMfgId(72);
   public static final BLonMfgId gesytecGmbh = new BLonMfgId(73);
   public static final BLonMfgId cumminsEngineCo = new BLonMfgId(74);
   public static final BLonMfgId landertMotorenAg = new BLonMfgId(75);
   public static final BLonMfgId toshibaCorp = new BLonMfgId(76);
   public static final BLonMfgId satronInstrumentsInc = new BLonMfgId(77);
   public static final BLonMfgId toshibaInfoSystems = new BLonMfgId(78);
   public static final BLonMfgId fujiElectricCo = new BLonMfgId(80);
   public static final BLonMfgId computerProcessControls = new BLonMfgId(81);
   public static final BLonMfgId somfy = new BLonMfgId(82);
   public static final BLonMfgId alcoControls = new BLonMfgId(83);
   public static final BLonMfgId keleAndAssociates = new BLonMfgId(84);
   public static final BLonMfgId grundfosElectronics = new BLonMfgId(85);
   public static final BLonMfgId zoneControlsKb = new BLonMfgId(86);
   public static final BLonMfgId reko = new BLonMfgId(87);
   public static final BLonMfgId coactiveNetworksInc = new BLonMfgId(89);
   public static final BLonMfgId nodusGmbh = new BLonMfgId(90);
   public static final BLonMfgId acutherm = new BLonMfgId(91);
   public static final BLonMfgId sontayOpenSystems = new BLonMfgId(92);
   public static final BLonMfgId cAndKSystems = new BLonMfgId(93);
   public static final BLonMfgId sysmikGmbh = new BLonMfgId(94);
   public static final BLonMfgId yamatakeCorp = new BLonMfgId(95);
   public static final BLonMfgId ctiProducts = new BLonMfgId(96);
   public static final BLonMfgId belimoAutomation = new BLonMfgId(97);
   public static final BLonMfgId neurologicResearch = new BLonMfgId(98);
   public static final BLonMfgId cnaEngineers = new BLonMfgId(99);
   public static final BLonMfgId energyControlsInternational = new BLonMfgId(100);
   public static final BLonMfgId frSauterAg = new BLonMfgId(101);
   public static final BLonMfgId teldaElectronics = new BLonMfgId(102);
   public static final BLonMfgId comtecTechnologie = new BLonMfgId(103);
   public static final BLonMfgId abbGebaudetechnikAg = new BLonMfgId(104);
   public static final BLonMfgId siemensStaefaControlsUsa = new BLonMfgId(105);
   public static final BLonMfgId luxmateControlsGmbh = new BLonMfgId(106);
   public static final BLonMfgId matrixControls = new BLonMfgId(107);
   public static final BLonMfgId huppeFormSonnenschutzsysteme = new BLonMfgId(108);
   public static final BLonMfgId samsungHeavyIndustries = new BLonMfgId(110);
   public static final BLonMfgId kitzCorp = new BLonMfgId(111);
   public static final BLonMfgId wago = new BLonMfgId(112);
   public static final BLonMfgId matsushitaElectricWorks = new BLonMfgId(113);
   public static final BLonMfgId siemensLandisStaefaKorea = new BLonMfgId(114);
   public static final BLonMfgId samsonAg = new BLonMfgId(115);
   public static final BLonMfgId enelIt = new BLonMfgId(116);
   public static final BLonMfgId vapacHumidityControls = new BLonMfgId(117);
   public static final BLonMfgId dciCo = new BLonMfgId(118);
   public static final BLonMfgId yorkInternationalCorp = new BLonMfgId(119);
   public static final BLonMfgId legrand = new BLonMfgId(120);
   public static final BLonMfgId wabtecCorp = new BLonMfgId(121);
   public static final BLonMfgId reginAb = new BLonMfgId(122);
   public static final BLonMfgId watanabeElectricIndustryCo = new BLonMfgId(123);
   public static final BLonMfgId firecom = new BLonMfgId(124);
   public static final BLonMfgId australonEnterprises = new BLonMfgId(125);
   public static final BLonMfgId meikosha = new BLonMfgId(126);
   public static final BLonMfgId knorrBrakeCorp = new BLonMfgId(127);
   public static final BLonMfgId viessmannWerke = new BLonMfgId(128);
   public static final BLonMfgId siemensLandisUsa = new BLonMfgId(129);
   public static final BLonMfgId kongsbergAnalogic = new BLonMfgId(130);
   public static final BLonMfgId distechControls = new BLonMfgId(131);
   public static final BLonMfgId idecIzumiCorp = new BLonMfgId(132);
   public static final BLonMfgId toshibaLighting = new BLonMfgId(133);
   public static final BLonMfgId reserved = new BLonMfgId(134);
   public static final BLonMfgId daikinIndustries = new BLonMfgId(135);
   public static final BLonMfgId rockwellAutomation = new BLonMfgId(136);
   public static final BLonMfgId alstonTransport = new BLonMfgId(137);
   public static final BLonMfgId luminator = new BLonMfgId(138);
   public static final BLonMfgId hyundaiAutonetCo = new BLonMfgId(139);
   public static final BLonMfgId pdlIndustries = new BLonMfgId(140);
   public static final BLonMfgId plexusTechnology = new BLonMfgId(141);
   public static final BLonMfgId tridium = new BLonMfgId(142);
   public static final BLonMfgId ercoLeuchten = new BLonMfgId(143);
   public static final BLonMfgId cetelab = new BLonMfgId(144);
   public static final BLonMfgId ciac = new BLonMfgId(145);
   public static final BLonMfgId networkControls = new BLonMfgId(146);
   public static final BLonMfgId valvconCorp = new BLonMfgId(147);
   public static final BLonMfgId carel = new BLonMfgId(148);
   public static final BLonMfgId fieldServerTechnologies = new BLonMfgId(149);
   public static final BLonMfgId halenSmartCompany = new BLonMfgId(150);
   public static final BLonMfgId faiveley = new BLonMfgId(151);
   public static final BLonMfgId lonMarkTechnicalStaff = new BLonMfgId(159);
   public static final BLonMfgId axsysAutomation = new BLonMfgId(160);
   public static final BLonMfgId adicCo = new BLonMfgId(161);
   public static final BLonMfgId mitsubishiElectricCorp = new BLonMfgId(162);
   public static final BLonMfgId hermos = new BLonMfgId(163);
   public static final BLonMfgId kiebackandPeter = new BLonMfgId(164);
   public static final BLonMfgId terasakiElectricCo = new BLonMfgId(165);
   public static final BLonMfgId microlabSistemiSrl = new BLonMfgId(166);
   public static final BLonMfgId wattStopper = new BLonMfgId(167);
   public static final BLonMfgId aquametro = new BLonMfgId(168);
   public static final BLonMfgId infranetPartners = new BLonMfgId(169);
   public static final BLonMfgId stifabFarex = new BLonMfgId(170);
   public static final BLonMfgId agtatec = new BLonMfgId(171);
   public static final BLonMfgId surfNetworks = new BLonMfgId(172);
   public static final BLonMfgId kamstrup = new BLonMfgId(173);
   public static final BLonMfgId gentec = new BLonMfgId(174);
   public static final BLonMfgId cypressSemiconductor = new BLonMfgId(175);
   public static final BLonMfgId intellicomInnovation = new BLonMfgId(176);
   public static final BLonMfgId shikokuInstrumentation = new BLonMfgId(177);
   public static final BLonMfgId carrierCorporation = new BLonMfgId(178);
   public static final BLonMfgId shanghaiChangXiangComputer = new BLonMfgId(179);
   public static final BLonMfgId raypak = new BLonMfgId(180);
   public static final BLonMfgId nicoTechnology = new BLonMfgId(181);
   public static final BLonMfgId lochinvarCorporation = new BLonMfgId(182);
   public static final BLonMfgId programmedWaterTech = new BLonMfgId(183);
   public static final BLonMfgId kaifaTechnology = new BLonMfgId(184);
   public static final BLonMfgId capelon = new BLonMfgId(185);
   public static final BLonMfgId oas = new BLonMfgId(186);
   public static final BLonMfgId microTask = new BLonMfgId(187);
   public static final BLonMfgId pureChoice = new BLonMfgId(188);
   public static final BLonMfgId vaconPlc = new BLonMfgId(189);
   public static final BLonMfgId orionCI = new BLonMfgId(190);
   public static final BLonMfgId samsungElectronics = new BLonMfgId(191);
   public static final BLonMfgId drucegrove = new BLonMfgId(192);
   public static final BLonMfgId janitzaElectronic = new BLonMfgId(193);
   public static final BLonMfgId oilesCorporation = new BLonMfgId(194);
   public static final BLonMfgId osakiElectric = new BLonMfgId(196);
   public static final BLonMfgId viconicsElectronics = new BLonMfgId(197);
   public static final BLonMfgId fujiElectricSystems = new BLonMfgId(198);
   public static final BLonMfgId hubbellBuildingAutomation = new BLonMfgId(199);
   public static final BLonMfgId zanderFacilityEngineering = new BLonMfgId(200);
   public static final BLonMfgId solidyneCorp = new BLonMfgId(201);
   public static final BLonMfgId badgerMeter = new BLonMfgId(202);
   public static final BLonMfgId draegerSafety = new BLonMfgId(203);
   public static final BLonMfgId lgElectronics = new BLonMfgId(204);
   public static final BLonMfgId hitachi = new BLonMfgId(205);
   public static final BLonMfgId gorenje = new BLonMfgId(206);
   public static final BLonMfgId functionalDevices = new BLonMfgId(207);
   public static final BLonMfgId onicon = new BLonMfgId(208);
   public static final BLonMfgId electronicTheatreControls = new BLonMfgId(209);
   public static final BLonMfgId gulfSecurity = new BLonMfgId(210);
   public static final BLonMfgId controlTechniques = new BLonMfgId(211);
   public static final BLonMfgId phoenixControls = new BLonMfgId(212);
   public static final BLonMfgId vaComTechnologies = new BLonMfgId(213);
   public static final BLonMfgId buildingAutomation = new BLonMfgId(214);
   public static final BLonMfgId loytec = new BLonMfgId(215);
   public static final BLonMfgId spiSystems = new BLonMfgId(216);
   public static final BLonMfgId quantumAutomation = new BLonMfgId(217);
   public static final BLonMfgId lsIndustrialSystems = new BLonMfgId(218);
   public static final BLonMfgId nanjingLianhongAutomation = new BLonMfgId(219);
   public static final BLonMfgId sitecoControl = new BLonMfgId(220);
   public static final BLonMfgId voyantSolutions = new BLonMfgId(221);
   public static final BLonMfgId elkaElektronik = new BLonMfgId(222);
   public static final BLonMfgId mSystem = new BLonMfgId(223);
   public static final BLonMfgId schneiderElectric = new BLonMfgId(224);
   public static final BLonMfgId isde = new BLonMfgId(225);
   public static final BLonMfgId paragonControls = new BLonMfgId(226);
   public static final BLonMfgId schneiderElectricMerten = new BLonMfgId(227);
   public static final BLonMfgId picElectronics = new BLonMfgId(228);
   public static final BLonMfgId airTestTechnologies = new BLonMfgId(229);
   public static final BLonMfgId spega = new BLonMfgId(230);
   public static final BLonMfgId hunterDouglas = new BLonMfgId(231);
   public static final BLonMfgId lennoxIndustries = new BLonMfgId(232);
   public static final BLonMfgId citylone = new BLonMfgId(233);
   public static final BLonMfgId samsungSds = new BLonMfgId(234);
   public static final BLonMfgId gdMideaHeatingAndVentEquip = new BLonMfgId(235);
   public static final BLonMfgId vosslohSchwabeDeutschland = new BLonMfgId(236);
   public static final BLonMfgId verisIndustries = new BLonMfgId(237);
   public static final BLonMfgId blueEarthInc = new BLonMfgId(238);
   public static final BLonMfgId benHtsAg = new BLonMfgId(239);
   public static final BLonMfgId hoshizakiAmerica = new BLonMfgId(240);
   public static final BLonMfgId honeywellEmon = new BLonMfgId(241);
   public static final BLonMfgId simon = new BLonMfgId(242);
   public static final BLonMfgId sloanValve = new BLonMfgId(243);
   public static final BLonMfgId trustbridge = new BLonMfgId(244);
   public static final BLonMfgId mangelberger = new BLonMfgId(245);
   public static final BLonMfgId secyourit = new BLonMfgId(246);
   public static final BLonMfgId guangdongRongwen = new BLonMfgId(247);
   public static final BLonMfgId ecosian = new BLonMfgId(248);
   public static final BLonMfgId apanet = new BLonMfgId(249);
   public static final BLonMfgId lonMarkAfs1 = new BLonMfgId(10479);
   public static final BLonMfgId honeywellFieldProgrammed = new BLonMfgId(13108);
   public static final BLonMfgId celsiusBeneluxBV = new BLonMfgId(1048132);
   public static final BLonMfgId DEFAULT = unknown;
   public static final Type TYPE = Sys.loadType(BLonMfgId.class);
   private static final Map<Integer, String> manufacturerOrdnialVsNameMap = new HashMap<>();
   private static final Lexicon LON_LEX = Lexicon.make("lonworks");

   public static BLonMfgId make(int ordinal) {
      return (BLonMfgId)unknown.getRange().get(ordinal, false);
   }

   public static BLonMfgId make(String tag) {
      return (BLonMfgId)unknown.getRange().get(tag);
   }

   private BLonMfgId(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public String getConvertedName() {
      return manufacturerOrdnialVsNameMap.get(this.getOrdinal());
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      )}
   )
   public static Map<Integer, String> getConversionMap(Context cx) {
      return Collections.unmodifiableMap(manufacturerOrdnialVsNameMap);
   }

   static {
      int[] ordinalArray = new int[]{
         10,
         16,
         19,
         20,
         21,
         22,
         23,
         24,
         25,
         26,
         28,
         29,
         31,
         32,
         33,
         39,
         40,
         42,
         44,
         52,
         53,
         55,
         57,
         60,
         62,
         77,
         87,
         90,
         103,
         108,
         113,
         114,
         117,
         125,
         129,
         144,
         224,
         227,
         59,
         197
      };

      for (int ordinal : ordinalArray) {
         manufacturerOrdnialVsNameMap.computeIfAbsent(ordinal, o -> LON_LEX.get("LonMfgId." + make(o).getTag()));
      }

      for (int ordinal : unknown.getRange().getOrdinals()) {
         manufacturerOrdnialVsNameMap.computeIfAbsent(ordinal, o -> make(o).getTag());
      }
   }
}
