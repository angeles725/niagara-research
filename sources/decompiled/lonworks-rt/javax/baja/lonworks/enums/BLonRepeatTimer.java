package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("milliSec16"), @Range("milliSec24"), @Range("milliSec32"), @Range("milliSec48"), @Range("milliSec64"), @Range("milliSec96"), @Range("milliSec128"), @Range("milliSec192"), @Range("milliSec256"), @Range("milliSec384"), @Range("milliSec512"), @Range("milliSec768"), @Range("milliSec1024"), @Range("milliSec1536"), @Range("milliSec2048"), @Range("milliSec3072")}
)
public final class BLonRepeatTimer extends BFrozenEnum {
   public static final int MILLI_SEC_16 = 0;
   public static final int MILLI_SEC_24 = 1;
   public static final int MILLI_SEC_32 = 2;
   public static final int MILLI_SEC_48 = 3;
   public static final int MILLI_SEC_64 = 4;
   public static final int MILLI_SEC_96 = 5;
   public static final int MILLI_SEC_128 = 6;
   public static final int MILLI_SEC_192 = 7;
   public static final int MILLI_SEC_256 = 8;
   public static final int MILLI_SEC_384 = 9;
   public static final int MILLI_SEC_512 = 10;
   public static final int MILLI_SEC_768 = 11;
   public static final int MILLI_SEC_1024 = 12;
   public static final int MILLI_SEC_1536 = 13;
   public static final int MILLI_SEC_2048 = 14;
   public static final int MILLI_SEC_3072 = 15;
   public static final BLonRepeatTimer milliSec16 = new BLonRepeatTimer(0);
   public static final BLonRepeatTimer milliSec24 = new BLonRepeatTimer(1);
   public static final BLonRepeatTimer milliSec32 = new BLonRepeatTimer(2);
   public static final BLonRepeatTimer milliSec48 = new BLonRepeatTimer(3);
   public static final BLonRepeatTimer milliSec64 = new BLonRepeatTimer(4);
   public static final BLonRepeatTimer milliSec96 = new BLonRepeatTimer(5);
   public static final BLonRepeatTimer milliSec128 = new BLonRepeatTimer(6);
   public static final BLonRepeatTimer milliSec192 = new BLonRepeatTimer(7);
   public static final BLonRepeatTimer milliSec256 = new BLonRepeatTimer(8);
   public static final BLonRepeatTimer milliSec384 = new BLonRepeatTimer(9);
   public static final BLonRepeatTimer milliSec512 = new BLonRepeatTimer(10);
   public static final BLonRepeatTimer milliSec768 = new BLonRepeatTimer(11);
   public static final BLonRepeatTimer milliSec1024 = new BLonRepeatTimer(12);
   public static final BLonRepeatTimer milliSec1536 = new BLonRepeatTimer(13);
   public static final BLonRepeatTimer milliSec2048 = new BLonRepeatTimer(14);
   public static final BLonRepeatTimer milliSec3072 = new BLonRepeatTimer(15);
   public static final BLonRepeatTimer DEFAULT = milliSec16;
   public static final Type TYPE = Sys.loadType(BLonRepeatTimer.class);
   private static final int[] TIMES = new int[]{16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512, 768, 1024, 1536, 2048, 3072};

   public static BLonRepeatTimer make(int ordinal) {
      return (BLonRepeatTimer)milliSec16.getRange().get(ordinal, false);
   }

   public static BLonRepeatTimer make(String tag) {
      return (BLonRepeatTimer)milliSec16.getRange().get(tag);
   }

   private BLonRepeatTimer(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public int getTime() {
      return TIMES[this.getOrdinal()];
   }
}
