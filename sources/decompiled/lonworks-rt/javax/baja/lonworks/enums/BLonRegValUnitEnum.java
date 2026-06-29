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
      value = "rvuNone",
      ordinal = 0
   ), @Range(
      value = "rvuW",
      ordinal = 1
   ), @Range(
      value = "rvuKw",
      ordinal = 2
   ), @Range(
      value = "rvuMw",
      ordinal = 3
   ), @Range(
      value = "rvuGw",
      ordinal = 4
   ), @Range(
      value = "rvuVar",
      ordinal = 5
   ), @Range(
      value = "rvuKvar",
      ordinal = 6
   ), @Range(
      value = "rvuMvar",
      ordinal = 7
   ), @Range(
      value = "rvuGvar",
      ordinal = 8
   ), @Range(
      value = "rvuWh",
      ordinal = 9
   ), @Range(
      value = "rvuKwh",
      ordinal = 10
   ), @Range(
      value = "rvuMwh",
      ordinal = 11
   ), @Range(
      value = "rvuGwn",
      ordinal = 12
   ), @Range(
      value = "rvuVarh",
      ordinal = 13
   ), @Range(
      value = "rvuKvarh",
      ordinal = 14
   ), @Range(
      value = "rvuMvarh",
      ordinal = 15
   ), @Range(
      value = "rvuGvarh",
      ordinal = 16
   ), @Range(
      value = "rvuV",
      ordinal = 17
   ), @Range(
      value = "rvuA",
      ordinal = 18
   ), @Range(
      value = "rvuCosf",
      ordinal = 19
   ), @Range(
      value = "rvuM3",
      ordinal = 20
   ), @Range(
      value = "rvuL",
      ordinal = 21
   ), @Range(
      value = "rvuMl",
      ordinal = 22
   ), @Range(
      value = "rvuUsgal",
      ordinal = 23
   ), @Range(
      value = "rvuGj",
      ordinal = 24
   ), @Range(
      value = "rvuMj",
      ordinal = 25
   ), @Range(
      value = "rvuMcal",
      ordinal = 26
   ), @Range(
      value = "rvuKcal",
      ordinal = 27
   ), @Range(
      value = "rvuMbtu",
      ordinal = 28
   ), @Range(
      value = "rvuKbtu",
      ordinal = 29
   ), @Range(
      value = "rvuMjh",
      ordinal = 30
   ), @Range(
      value = "rvuMls",
      ordinal = 31
   ), @Range(
      value = "rvuLs",
      ordinal = 32
   ), @Range(
      value = "rvuM3s",
      ordinal = 33
   ), @Range(
      value = "rvuC",
      ordinal = 34
   ), @Range(
      value = "rvuLh",
      ordinal = 35
   ), @Range(
      value = "rvuVa",
      ordinal = 36
   ), @Range(
      value = "rvuKva",
      ordinal = 37
   ), @Range(
      value = "rvuMva",
      ordinal = 38
   ), @Range(
      value = "rvuGva",
      ordinal = 39
   ), @Range(
      value = "rvuVah",
      ordinal = 40
   ), @Range(
      value = "rvuKvah",
      ordinal = 41
   ), @Range(
      value = "rvuMvah",
      ordinal = 42
   ), @Range(
      value = "rvuGvah",
      ordinal = 43
   ), @Range(
      value = "rvuNul",
      ordinal = -1
   )}
)
public final class BLonRegValUnitEnum extends BFrozenEnum {
   public static final int RVU_NONE = 0;
   public static final int RVU_W = 1;
   public static final int RVU_KW = 2;
   public static final int RVU_MW = 3;
   public static final int RVU_GW = 4;
   public static final int RVU_VAR = 5;
   public static final int RVU_KVAR = 6;
   public static final int RVU_MVAR = 7;
   public static final int RVU_GVAR = 8;
   public static final int RVU_WH = 9;
   public static final int RVU_KWH = 10;
   public static final int RVU_MWH = 11;
   public static final int RVU_GWN = 12;
   public static final int RVU_VARH = 13;
   public static final int RVU_KVARH = 14;
   public static final int RVU_MVARH = 15;
   public static final int RVU_GVARH = 16;
   public static final int RVU_V = 17;
   public static final int RVU_A = 18;
   public static final int RVU_COSF = 19;
   public static final int RVU_M3 = 20;
   public static final int RVU_L = 21;
   public static final int RVU_ML = 22;
   public static final int RVU_USGAL = 23;
   public static final int RVU_GJ = 24;
   public static final int RVU_MJ = 25;
   public static final int RVU_MCAL = 26;
   public static final int RVU_KCAL = 27;
   public static final int RVU_MBTU = 28;
   public static final int RVU_KBTU = 29;
   public static final int RVU_MJH = 30;
   public static final int RVU_MLS = 31;
   public static final int RVU_LS = 32;
   public static final int RVU_M3S = 33;
   public static final int RVU_C = 34;
   public static final int RVU_LH = 35;
   public static final int RVU_VA = 36;
   public static final int RVU_KVA = 37;
   public static final int RVU_MVA = 38;
   public static final int RVU_GVA = 39;
   public static final int RVU_VAH = 40;
   public static final int RVU_KVAH = 41;
   public static final int RVU_MVAH = 42;
   public static final int RVU_GVAH = 43;
   public static final int RVU_NUL = -1;
   public static final BLonRegValUnitEnum rvuNone = new BLonRegValUnitEnum(0);
   public static final BLonRegValUnitEnum rvuW = new BLonRegValUnitEnum(1);
   public static final BLonRegValUnitEnum rvuKw = new BLonRegValUnitEnum(2);
   public static final BLonRegValUnitEnum rvuMw = new BLonRegValUnitEnum(3);
   public static final BLonRegValUnitEnum rvuGw = new BLonRegValUnitEnum(4);
   public static final BLonRegValUnitEnum rvuVar = new BLonRegValUnitEnum(5);
   public static final BLonRegValUnitEnum rvuKvar = new BLonRegValUnitEnum(6);
   public static final BLonRegValUnitEnum rvuMvar = new BLonRegValUnitEnum(7);
   public static final BLonRegValUnitEnum rvuGvar = new BLonRegValUnitEnum(8);
   public static final BLonRegValUnitEnum rvuWh = new BLonRegValUnitEnum(9);
   public static final BLonRegValUnitEnum rvuKwh = new BLonRegValUnitEnum(10);
   public static final BLonRegValUnitEnum rvuMwh = new BLonRegValUnitEnum(11);
   public static final BLonRegValUnitEnum rvuGwn = new BLonRegValUnitEnum(12);
   public static final BLonRegValUnitEnum rvuVarh = new BLonRegValUnitEnum(13);
   public static final BLonRegValUnitEnum rvuKvarh = new BLonRegValUnitEnum(14);
   public static final BLonRegValUnitEnum rvuMvarh = new BLonRegValUnitEnum(15);
   public static final BLonRegValUnitEnum rvuGvarh = new BLonRegValUnitEnum(16);
   public static final BLonRegValUnitEnum rvuV = new BLonRegValUnitEnum(17);
   public static final BLonRegValUnitEnum rvuA = new BLonRegValUnitEnum(18);
   public static final BLonRegValUnitEnum rvuCosf = new BLonRegValUnitEnum(19);
   public static final BLonRegValUnitEnum rvuM3 = new BLonRegValUnitEnum(20);
   public static final BLonRegValUnitEnum rvuL = new BLonRegValUnitEnum(21);
   public static final BLonRegValUnitEnum rvuMl = new BLonRegValUnitEnum(22);
   public static final BLonRegValUnitEnum rvuUsgal = new BLonRegValUnitEnum(23);
   public static final BLonRegValUnitEnum rvuGj = new BLonRegValUnitEnum(24);
   public static final BLonRegValUnitEnum rvuMj = new BLonRegValUnitEnum(25);
   public static final BLonRegValUnitEnum rvuMcal = new BLonRegValUnitEnum(26);
   public static final BLonRegValUnitEnum rvuKcal = new BLonRegValUnitEnum(27);
   public static final BLonRegValUnitEnum rvuMbtu = new BLonRegValUnitEnum(28);
   public static final BLonRegValUnitEnum rvuKbtu = new BLonRegValUnitEnum(29);
   public static final BLonRegValUnitEnum rvuMjh = new BLonRegValUnitEnum(30);
   public static final BLonRegValUnitEnum rvuMls = new BLonRegValUnitEnum(31);
   public static final BLonRegValUnitEnum rvuLs = new BLonRegValUnitEnum(32);
   public static final BLonRegValUnitEnum rvuM3s = new BLonRegValUnitEnum(33);
   public static final BLonRegValUnitEnum rvuC = new BLonRegValUnitEnum(34);
   public static final BLonRegValUnitEnum rvuLh = new BLonRegValUnitEnum(35);
   public static final BLonRegValUnitEnum rvuVa = new BLonRegValUnitEnum(36);
   public static final BLonRegValUnitEnum rvuKva = new BLonRegValUnitEnum(37);
   public static final BLonRegValUnitEnum rvuMva = new BLonRegValUnitEnum(38);
   public static final BLonRegValUnitEnum rvuGva = new BLonRegValUnitEnum(39);
   public static final BLonRegValUnitEnum rvuVah = new BLonRegValUnitEnum(40);
   public static final BLonRegValUnitEnum rvuKvah = new BLonRegValUnitEnum(41);
   public static final BLonRegValUnitEnum rvuMvah = new BLonRegValUnitEnum(42);
   public static final BLonRegValUnitEnum rvuGvah = new BLonRegValUnitEnum(43);
   public static final BLonRegValUnitEnum rvuNul = new BLonRegValUnitEnum(-1);
   public static final BLonRegValUnitEnum DEFAULT = rvuNone;
   public static final Type TYPE = Sys.loadType(BLonRegValUnitEnum.class);

   public static BLonRegValUnitEnum make(int ordinal) {
      return (BLonRegValUnitEnum)rvuNone.getRange().get(ordinal, false);
   }

   public static BLonRegValUnitEnum make(String tag) {
      return (BLonRegValUnitEnum)rvuNone.getRange().get(tag);
   }

   private BLonRegValUnitEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
