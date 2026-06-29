package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range(
      value = "cuArgentinaPeso",
      ordinal = 0
   ), @Range(
      value = "cuAustraliaDollar",
      ordinal = 1
   ), @Range(
      value = "cuAustriaSchilling",
      ordinal = 2
   ), @Range(
      value = "cuBahrainDinar",
      ordinal = 3
   ), @Range(
      value = "cuBelgiumFranc",
      ordinal = 4
   ), @Range(
      value = "cuBrazilCruzeiroReal",
      ordinal = 5
   ), @Range(
      value = "cuBritainPound",
      ordinal = 6
   ), @Range(
      value = "cuCanadaDollar",
      ordinal = 7
   ), @Range(
      value = "cuCzechKoruna",
      ordinal = 8
   ), @Range(
      value = "cuChilePeso",
      ordinal = 9
   ), @Range(
      value = "cuChinaRenminbi",
      ordinal = 10
   ), @Range(
      value = "cuColombiaPeso",
      ordinal = 11
   ), @Range(
      value = "cuDenmarkKrone",
      ordinal = 12
   ), @Range(
      value = "cuEcuadorSucre",
      ordinal = 13
   ), @Range(
      value = "cuEuropeanCurrencyUnit",
      ordinal = 14
   ), @Range(
      value = "cuFinlandMarkka",
      ordinal = 15
   ), @Range(
      value = "cuFranceFranc",
      ordinal = 16
   ), @Range(
      value = "cuGermanyMark",
      ordinal = 17
   ), @Range(
      value = "cuGreeceDrachma",
      ordinal = 18
   ), @Range(
      value = "cuHongKongDollar",
      ordinal = 19
   ), @Range(
      value = "cuHungaryForint",
      ordinal = 20
   ), @Range(
      value = "cuIndiaRupee",
      ordinal = 21
   ), @Range(
      value = "cuIndonesiaRupiah",
      ordinal = 22
   ), @Range(
      value = "cuIrelandPunt",
      ordinal = 23
   ), @Range(
      value = "cuIsraelShekel",
      ordinal = 24
   ), @Range(
      value = "cuItalyLira",
      ordinal = 25
   ), @Range(
      value = "cuJapanYen",
      ordinal = 26
   ), @Range(
      value = "cuJordanDinar",
      ordinal = 27
   ), @Range(
      value = "cuKuwaitDinar",
      ordinal = 28
   ), @Range(
      value = "cuLebanonPound",
      ordinal = 29
   ), @Range(
      value = "cuMalaysiaRinggit",
      ordinal = 30
   ), @Range(
      value = "cuMaltaLira",
      ordinal = 31
   ), @Range(
      value = "cuMexicoPeso",
      ordinal = 32
   ), @Range(
      value = "cuNetherlandsGuilder",
      ordinal = 33
   ), @Range(
      value = "cuNewZealandDollar",
      ordinal = 34
   ), @Range(
      value = "cuNorwayKrone",
      ordinal = 35
   ), @Range(
      value = "cuPakistanRupee",
      ordinal = 36
   ), @Range(
      value = "cuPeruNewSol",
      ordinal = 37
   ), @Range(
      value = "cuPhilippinesPeso",
      ordinal = 38
   ), @Range(
      value = "cuPolandZloty",
      ordinal = 39
   ), @Range(
      value = "cuPortugalEscudo",
      ordinal = 40
   ), @Range(
      value = "cuSaudiArabiaRiyal",
      ordinal = 41
   ), @Range(
      value = "cuSingaporeDollar",
      ordinal = 42
   ), @Range(
      value = "cuSlovakKoruna",
      ordinal = 43
   ), @Range(
      value = "cuSouthAfricaRand",
      ordinal = 44
   ), @Range(
      value = "cuSouthKoreaWon",
      ordinal = 45
   ), @Range(
      value = "cuSpainPeseta",
      ordinal = 46
   ), @Range(
      value = "cuSpecialDrawingRights",
      ordinal = 47
   ), @Range(
      value = "cuSwedenKrona",
      ordinal = 48
   ), @Range(
      value = "cuSwitzerlandFranc",
      ordinal = 49
   ), @Range(
      value = "cuTaiwanDollar",
      ordinal = 50
   ), @Range(
      value = "cuThailandBaht",
      ordinal = 51
   ), @Range(
      value = "cuTurkeyLira",
      ordinal = 52
   ), @Range(
      value = "cuUnitedArabDirham",
      ordinal = 53
   ), @Range(
      value = "cuUnitedStatesDollar",
      ordinal = 54
   ), @Range(
      value = "cuUruguayNewPeso",
      ordinal = 55
   ), @Range(
      value = "cuVenezuelaBolivar",
      ordinal = 56
   ), @Range(
      value = "cuNul",
      ordinal = -1
   )}
)
public final class BLonCurrencyEnum extends BFrozenEnum {
   public static final int CU_ARGENTINA_PESO = 0;
   public static final int CU_AUSTRALIA_DOLLAR = 1;
   public static final int CU_AUSTRIA_SCHILLING = 2;
   public static final int CU_BAHRAIN_DINAR = 3;
   public static final int CU_BELGIUM_FRANC = 4;
   public static final int CU_BRAZIL_CRUZEIRO_REAL = 5;
   public static final int CU_BRITAIN_POUND = 6;
   public static final int CU_CANADA_DOLLAR = 7;
   public static final int CU_CZECH_KORUNA = 8;
   public static final int CU_CHILE_PESO = 9;
   public static final int CU_CHINA_RENMINBI = 10;
   public static final int CU_COLOMBIA_PESO = 11;
   public static final int CU_DENMARK_KRONE = 12;
   public static final int CU_ECUADOR_SUCRE = 13;
   public static final int CU_EUROPEAN_CURRENCY_UNIT = 14;
   public static final int CU_FINLAND_MARKKA = 15;
   public static final int CU_FRANCE_FRANC = 16;
   public static final int CU_GERMANY_MARK = 17;
   public static final int CU_GREECE_DRACHMA = 18;
   public static final int CU_HONG_KONG_DOLLAR = 19;
   public static final int CU_HUNGARY_FORINT = 20;
   public static final int CU_INDIA_RUPEE = 21;
   public static final int CU_INDONESIA_RUPIAH = 22;
   public static final int CU_IRELAND_PUNT = 23;
   public static final int CU_ISRAEL_SHEKEL = 24;
   public static final int CU_ITALY_LIRA = 25;
   public static final int CU_JAPAN_YEN = 26;
   public static final int CU_JORDAN_DINAR = 27;
   public static final int CU_KUWAIT_DINAR = 28;
   public static final int CU_LEBANON_POUND = 29;
   public static final int CU_MALAYSIA_RINGGIT = 30;
   public static final int CU_MALTA_LIRA = 31;
   public static final int CU_MEXICO_PESO = 32;
   public static final int CU_NETHERLANDS_GUILDER = 33;
   public static final int CU_NEW_ZEALAND_DOLLAR = 34;
   public static final int CU_NORWAY_KRONE = 35;
   public static final int CU_PAKISTAN_RUPEE = 36;
   public static final int CU_PERU_NEW_SOL = 37;
   public static final int CU_PHILIPPINES_PESO = 38;
   public static final int CU_POLAND_ZLOTY = 39;
   public static final int CU_PORTUGAL_ESCUDO = 40;
   public static final int CU_SAUDI_ARABIA_RIYAL = 41;
   public static final int CU_SINGAPORE_DOLLAR = 42;
   public static final int CU_SLOVAK_KORUNA = 43;
   public static final int CU_SOUTH_AFRICA_RAND = 44;
   public static final int CU_SOUTH_KOREA_WON = 45;
   public static final int CU_SPAIN_PESETA = 46;
   public static final int CU_SPECIAL_DRAWING_RIGHTS = 47;
   public static final int CU_SWEDEN_KRONA = 48;
   public static final int CU_SWITZERLAND_FRANC = 49;
   public static final int CU_TAIWAN_DOLLAR = 50;
   public static final int CU_THAILAND_BAHT = 51;
   public static final int CU_TURKEY_LIRA = 52;
   public static final int CU_UNITED_ARAB_DIRHAM = 53;
   public static final int CU_UNITED_STATES_DOLLAR = 54;
   public static final int CU_URUGUAY_NEW_PESO = 55;
   public static final int CU_VENEZUELA_BOLIVAR = 56;
   public static final int CU_NUL = -1;
   public static final BLonCurrencyEnum cuArgentinaPeso = new BLonCurrencyEnum(0);
   public static final BLonCurrencyEnum cuAustraliaDollar = new BLonCurrencyEnum(1);
   public static final BLonCurrencyEnum cuAustriaSchilling = new BLonCurrencyEnum(2);
   public static final BLonCurrencyEnum cuBahrainDinar = new BLonCurrencyEnum(3);
   public static final BLonCurrencyEnum cuBelgiumFranc = new BLonCurrencyEnum(4);
   public static final BLonCurrencyEnum cuBrazilCruzeiroReal = new BLonCurrencyEnum(5);
   public static final BLonCurrencyEnum cuBritainPound = new BLonCurrencyEnum(6);
   public static final BLonCurrencyEnum cuCanadaDollar = new BLonCurrencyEnum(7);
   public static final BLonCurrencyEnum cuCzechKoruna = new BLonCurrencyEnum(8);
   public static final BLonCurrencyEnum cuChilePeso = new BLonCurrencyEnum(9);
   public static final BLonCurrencyEnum cuChinaRenminbi = new BLonCurrencyEnum(10);
   public static final BLonCurrencyEnum cuColombiaPeso = new BLonCurrencyEnum(11);
   public static final BLonCurrencyEnum cuDenmarkKrone = new BLonCurrencyEnum(12);
   public static final BLonCurrencyEnum cuEcuadorSucre = new BLonCurrencyEnum(13);
   public static final BLonCurrencyEnum cuEuropeanCurrencyUnit = new BLonCurrencyEnum(14);
   public static final BLonCurrencyEnum cuFinlandMarkka = new BLonCurrencyEnum(15);
   public static final BLonCurrencyEnum cuFranceFranc = new BLonCurrencyEnum(16);
   public static final BLonCurrencyEnum cuGermanyMark = new BLonCurrencyEnum(17);
   public static final BLonCurrencyEnum cuGreeceDrachma = new BLonCurrencyEnum(18);
   public static final BLonCurrencyEnum cuHongKongDollar = new BLonCurrencyEnum(19);
   public static final BLonCurrencyEnum cuHungaryForint = new BLonCurrencyEnum(20);
   public static final BLonCurrencyEnum cuIndiaRupee = new BLonCurrencyEnum(21);
   public static final BLonCurrencyEnum cuIndonesiaRupiah = new BLonCurrencyEnum(22);
   public static final BLonCurrencyEnum cuIrelandPunt = new BLonCurrencyEnum(23);
   public static final BLonCurrencyEnum cuIsraelShekel = new BLonCurrencyEnum(24);
   public static final BLonCurrencyEnum cuItalyLira = new BLonCurrencyEnum(25);
   public static final BLonCurrencyEnum cuJapanYen = new BLonCurrencyEnum(26);
   public static final BLonCurrencyEnum cuJordanDinar = new BLonCurrencyEnum(27);
   public static final BLonCurrencyEnum cuKuwaitDinar = new BLonCurrencyEnum(28);
   public static final BLonCurrencyEnum cuLebanonPound = new BLonCurrencyEnum(29);
   public static final BLonCurrencyEnum cuMalaysiaRinggit = new BLonCurrencyEnum(30);
   public static final BLonCurrencyEnum cuMaltaLira = new BLonCurrencyEnum(31);
   public static final BLonCurrencyEnum cuMexicoPeso = new BLonCurrencyEnum(32);
   public static final BLonCurrencyEnum cuNetherlandsGuilder = new BLonCurrencyEnum(33);
   public static final BLonCurrencyEnum cuNewZealandDollar = new BLonCurrencyEnum(34);
   public static final BLonCurrencyEnum cuNorwayKrone = new BLonCurrencyEnum(35);
   public static final BLonCurrencyEnum cuPakistanRupee = new BLonCurrencyEnum(36);
   public static final BLonCurrencyEnum cuPeruNewSol = new BLonCurrencyEnum(37);
   public static final BLonCurrencyEnum cuPhilippinesPeso = new BLonCurrencyEnum(38);
   public static final BLonCurrencyEnum cuPolandZloty = new BLonCurrencyEnum(39);
   public static final BLonCurrencyEnum cuPortugalEscudo = new BLonCurrencyEnum(40);
   public static final BLonCurrencyEnum cuSaudiArabiaRiyal = new BLonCurrencyEnum(41);
   public static final BLonCurrencyEnum cuSingaporeDollar = new BLonCurrencyEnum(42);
   public static final BLonCurrencyEnum cuSlovakKoruna = new BLonCurrencyEnum(43);
   public static final BLonCurrencyEnum cuSouthAfricaRand = new BLonCurrencyEnum(44);
   public static final BLonCurrencyEnum cuSouthKoreaWon = new BLonCurrencyEnum(45);
   public static final BLonCurrencyEnum cuSpainPeseta = new BLonCurrencyEnum(46);
   public static final BLonCurrencyEnum cuSpecialDrawingRights = new BLonCurrencyEnum(47);
   public static final BLonCurrencyEnum cuSwedenKrona = new BLonCurrencyEnum(48);
   public static final BLonCurrencyEnum cuSwitzerlandFranc = new BLonCurrencyEnum(49);
   public static final BLonCurrencyEnum cuTaiwanDollar = new BLonCurrencyEnum(50);
   public static final BLonCurrencyEnum cuThailandBaht = new BLonCurrencyEnum(51);
   public static final BLonCurrencyEnum cuTurkeyLira = new BLonCurrencyEnum(52);
   public static final BLonCurrencyEnum cuUnitedArabDirham = new BLonCurrencyEnum(53);
   public static final BLonCurrencyEnum cuUnitedStatesDollar = new BLonCurrencyEnum(54);
   public static final BLonCurrencyEnum cuUruguayNewPeso = new BLonCurrencyEnum(55);
   public static final BLonCurrencyEnum cuVenezuelaBolivar = new BLonCurrencyEnum(56);
   public static final BLonCurrencyEnum cuNul = new BLonCurrencyEnum(-1);
   public static final BLonCurrencyEnum DEFAULT = cuArgentinaPeso;
   public static final Type TYPE = Sys.loadType(BLonCurrencyEnum.class);

   public static BLonCurrencyEnum make(int ordinal) {
      return (BLonCurrencyEnum)cuArgentinaPeso.getRange().get(ordinal, false);
   }

   public static BLonCurrencyEnum make(String tag) {
      return (BLonCurrencyEnum)cuArgentinaPeso.getRange().get(tag);
   }

   private BLonCurrencyEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
