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
      value = "fiUndefined",
      ordinal = 0
   ), @Range(
      value = "fiThermalFixed",
      ordinal = 1
   ), @Range(
      value = "fiSmokeIon",
      ordinal = 2
   ), @Range(
      value = "fiMultiIonThermal",
      ordinal = 3
   ), @Range(
      value = "fiSmokePhoto",
      ordinal = 4
   ), @Range(
      value = "fiMultiPhotoThermal",
      ordinal = 5
   ), @Range(
      value = "fiMultiPhotoIon",
      ordinal = 6
   ), @Range(
      value = "fiMultiPhotoIonThermal",
      ordinal = 7
   ), @Range(
      value = "fiThermalRor",
      ordinal = 8
   ), @Range(
      value = "fiMultiThermalRor",
      ordinal = 9
   ), @Range(
      value = "fiManualPull",
      ordinal = 10
   ), @Range(
      value = "fiWaterFlow",
      ordinal = 11
   ), @Range(
      value = "fiWaterFlowTamper",
      ordinal = 12
   ), @Range(
      value = "fiStatusOnly",
      ordinal = 13
   ), @Range(
      value = "fiManualCall",
      ordinal = 14
   ), @Range(
      value = "fiFiremanCall",
      ordinal = 15
   ), @Range(
      value = "fiUniveral",
      ordinal = 16
   ), @Range(
      value = "fiNul",
      ordinal = -1
   )}
)
public final class BLonFireInitEnum extends BFrozenEnum {
   public static final int FI_UNDEFINED = 0;
   public static final int FI_THERMAL_FIXED = 1;
   public static final int FI_SMOKE_ION = 2;
   public static final int FI_MULTI_ION_THERMAL = 3;
   public static final int FI_SMOKE_PHOTO = 4;
   public static final int FI_MULTI_PHOTO_THERMAL = 5;
   public static final int FI_MULTI_PHOTO_ION = 6;
   public static final int FI_MULTI_PHOTO_ION_THERMAL = 7;
   public static final int FI_THERMAL_ROR = 8;
   public static final int FI_MULTI_THERMAL_ROR = 9;
   public static final int FI_MANUAL_PULL = 10;
   public static final int FI_WATER_FLOW = 11;
   public static final int FI_WATER_FLOW_TAMPER = 12;
   public static final int FI_STATUS_ONLY = 13;
   public static final int FI_MANUAL_CALL = 14;
   public static final int FI_FIREMAN_CALL = 15;
   public static final int FI_UNIVERAL = 16;
   public static final int FI_NUL = -1;
   public static final BLonFireInitEnum fiUndefined = new BLonFireInitEnum(0);
   public static final BLonFireInitEnum fiThermalFixed = new BLonFireInitEnum(1);
   public static final BLonFireInitEnum fiSmokeIon = new BLonFireInitEnum(2);
   public static final BLonFireInitEnum fiMultiIonThermal = new BLonFireInitEnum(3);
   public static final BLonFireInitEnum fiSmokePhoto = new BLonFireInitEnum(4);
   public static final BLonFireInitEnum fiMultiPhotoThermal = new BLonFireInitEnum(5);
   public static final BLonFireInitEnum fiMultiPhotoIon = new BLonFireInitEnum(6);
   public static final BLonFireInitEnum fiMultiPhotoIonThermal = new BLonFireInitEnum(7);
   public static final BLonFireInitEnum fiThermalRor = new BLonFireInitEnum(8);
   public static final BLonFireInitEnum fiMultiThermalRor = new BLonFireInitEnum(9);
   public static final BLonFireInitEnum fiManualPull = new BLonFireInitEnum(10);
   public static final BLonFireInitEnum fiWaterFlow = new BLonFireInitEnum(11);
   public static final BLonFireInitEnum fiWaterFlowTamper = new BLonFireInitEnum(12);
   public static final BLonFireInitEnum fiStatusOnly = new BLonFireInitEnum(13);
   public static final BLonFireInitEnum fiManualCall = new BLonFireInitEnum(14);
   public static final BLonFireInitEnum fiFiremanCall = new BLonFireInitEnum(15);
   public static final BLonFireInitEnum fiUniveral = new BLonFireInitEnum(16);
   public static final BLonFireInitEnum fiNul = new BLonFireInitEnum(-1);
   public static final BLonFireInitEnum DEFAULT = fiUndefined;
   public static final Type TYPE = Sys.loadType(BLonFireInitEnum.class);

   public static BLonFireInitEnum make(int ordinal) {
      return (BLonFireInitEnum)fiUndefined.getRange().get(ordinal, false);
   }

   public static BLonFireInitEnum make(String tag) {
      return (BLonFireInitEnum)fiUndefined.getRange().get(tag);
   }

   private BLonFireInitEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
