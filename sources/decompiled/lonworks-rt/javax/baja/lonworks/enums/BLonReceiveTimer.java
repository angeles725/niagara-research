package javax.baja.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("milliSec128"), @Range("milliSec192"), @Range("milliSec256"), @Range("milliSec384"), @Range("milliSec512"), @Range("milliSec768"), @Range("milliSec1024"), @Range("milliSec1536"), @Range("milliSec2048"), @Range("milliSec3072"), @Range("milliSec4096"), @Range("milliSec6144"), @Range("milliSec8192"), @Range("milliSec12288"), @Range("milliSec16384"), @Range("milliSec24576")}
)
public final class BLonReceiveTimer extends BFrozenEnum {
   public static final int MILLI_SEC_128 = 0;
   public static final int MILLI_SEC_192 = 1;
   public static final int MILLI_SEC_256 = 2;
   public static final int MILLI_SEC_384 = 3;
   public static final int MILLI_SEC_512 = 4;
   public static final int MILLI_SEC_768 = 5;
   public static final int MILLI_SEC_1024 = 6;
   public static final int MILLI_SEC_1536 = 7;
   public static final int MILLI_SEC_2048 = 8;
   public static final int MILLI_SEC_3072 = 9;
   public static final int MILLI_SEC_4096 = 10;
   public static final int MILLI_SEC_6144 = 11;
   public static final int MILLI_SEC_8192 = 12;
   public static final int MILLI_SEC_12288 = 13;
   public static final int MILLI_SEC_16384 = 14;
   public static final int MILLI_SEC_24576 = 15;
   public static final BLonReceiveTimer milliSec128 = new BLonReceiveTimer(0);
   public static final BLonReceiveTimer milliSec192 = new BLonReceiveTimer(1);
   public static final BLonReceiveTimer milliSec256 = new BLonReceiveTimer(2);
   public static final BLonReceiveTimer milliSec384 = new BLonReceiveTimer(3);
   public static final BLonReceiveTimer milliSec512 = new BLonReceiveTimer(4);
   public static final BLonReceiveTimer milliSec768 = new BLonReceiveTimer(5);
   public static final BLonReceiveTimer milliSec1024 = new BLonReceiveTimer(6);
   public static final BLonReceiveTimer milliSec1536 = new BLonReceiveTimer(7);
   public static final BLonReceiveTimer milliSec2048 = new BLonReceiveTimer(8);
   public static final BLonReceiveTimer milliSec3072 = new BLonReceiveTimer(9);
   public static final BLonReceiveTimer milliSec4096 = new BLonReceiveTimer(10);
   public static final BLonReceiveTimer milliSec6144 = new BLonReceiveTimer(11);
   public static final BLonReceiveTimer milliSec8192 = new BLonReceiveTimer(12);
   public static final BLonReceiveTimer milliSec12288 = new BLonReceiveTimer(13);
   public static final BLonReceiveTimer milliSec16384 = new BLonReceiveTimer(14);
   public static final BLonReceiveTimer milliSec24576 = new BLonReceiveTimer(15);
   public static final BLonReceiveTimer DEFAULT = milliSec128;
   public static final Type TYPE = Sys.loadType(BLonReceiveTimer.class);

   public static BLonReceiveTimer make(int ordinal) {
      return (BLonReceiveTimer)milliSec128.getRange().get(ordinal, false);
   }

   public static BLonReceiveTimer make(String tag) {
      return (BLonReceiveTimer)milliSec128.getRange().get(tag);
   }

   private BLonReceiveTimer(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
