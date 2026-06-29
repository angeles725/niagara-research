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
      value = "scfSave",
      ordinal = 0
   ), @Range(
      value = "scfClear",
      ordinal = 1
   ), @Range(
      value = "scfReport",
      ordinal = 2
   ), @Range(
      value = "scfSize",
      ordinal = 3
   ), @Range(
      value = "scfFree",
      ordinal = 4
   ), @Range(
      value = "scfNul",
      ordinal = -1
   )}
)
public final class BLonSceneConfigEnum extends BFrozenEnum {
   public static final int SCF_SAVE = 0;
   public static final int SCF_CLEAR = 1;
   public static final int SCF_REPORT = 2;
   public static final int SCF_SIZE = 3;
   public static final int SCF_FREE = 4;
   public static final int SCF_NUL = -1;
   public static final BLonSceneConfigEnum scfSave = new BLonSceneConfigEnum(0);
   public static final BLonSceneConfigEnum scfClear = new BLonSceneConfigEnum(1);
   public static final BLonSceneConfigEnum scfReport = new BLonSceneConfigEnum(2);
   public static final BLonSceneConfigEnum scfSize = new BLonSceneConfigEnum(3);
   public static final BLonSceneConfigEnum scfFree = new BLonSceneConfigEnum(4);
   public static final BLonSceneConfigEnum scfNul = new BLonSceneConfigEnum(-1);
   public static final BLonSceneConfigEnum DEFAULT = scfSave;
   public static final Type TYPE = Sys.loadType(BLonSceneConfigEnum.class);

   public static BLonSceneConfigEnum make(int ordinal) {
      return (BLonSceneConfigEnum)scfSave.getRange().get(ordinal, false);
   }

   public static BLonSceneConfigEnum make(String tag) {
      return (BLonSceneConfigEnum)scfSave.getRange().get(tag);
   }

   private BLonSceneConfigEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
