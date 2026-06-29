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
      value = "bufferSize255",
      ordinal = 0
   ), @Range(
      value = "bufferSize20",
      ordinal = 2
   ), @Range(
      value = "bufferSize21",
      ordinal = 3
   ), @Range(
      value = "bufferSize22",
      ordinal = 4
   ), @Range(
      value = "bufferSize24",
      ordinal = 5
   ), @Range(
      value = "bufferSize26",
      ordinal = 6
   ), @Range(
      value = "bufferSize30",
      ordinal = 7
   ), @Range(
      value = "bufferSize34",
      ordinal = 8
   ), @Range(
      value = "bufferSize42",
      ordinal = 9
   ), @Range(
      value = "bufferSize50",
      ordinal = 10
   ), @Range(
      value = "bufferSize66",
      ordinal = 11
   ), @Range(
      value = "bufferSize82",
      ordinal = 12
   ), @Range(
      value = "bufferSize114",
      ordinal = 13
   ), @Range(
      value = "bufferSize146",
      ordinal = 14
   ), @Range(
      value = "bufferSize210",
      ordinal = 15
   )},
   defaultValue = "bufferSize255"
)
public final class BBufferSizeEnum extends BFrozenEnum {
   public static final int BUFFER_SIZE_255 = 0;
   public static final int BUFFER_SIZE_20 = 2;
   public static final int BUFFER_SIZE_21 = 3;
   public static final int BUFFER_SIZE_22 = 4;
   public static final int BUFFER_SIZE_24 = 5;
   public static final int BUFFER_SIZE_26 = 6;
   public static final int BUFFER_SIZE_30 = 7;
   public static final int BUFFER_SIZE_34 = 8;
   public static final int BUFFER_SIZE_42 = 9;
   public static final int BUFFER_SIZE_50 = 10;
   public static final int BUFFER_SIZE_66 = 11;
   public static final int BUFFER_SIZE_82 = 12;
   public static final int BUFFER_SIZE_114 = 13;
   public static final int BUFFER_SIZE_146 = 14;
   public static final int BUFFER_SIZE_210 = 15;
   public static final BBufferSizeEnum bufferSize255 = new BBufferSizeEnum(0);
   public static final BBufferSizeEnum bufferSize20 = new BBufferSizeEnum(2);
   public static final BBufferSizeEnum bufferSize21 = new BBufferSizeEnum(3);
   public static final BBufferSizeEnum bufferSize22 = new BBufferSizeEnum(4);
   public static final BBufferSizeEnum bufferSize24 = new BBufferSizeEnum(5);
   public static final BBufferSizeEnum bufferSize26 = new BBufferSizeEnum(6);
   public static final BBufferSizeEnum bufferSize30 = new BBufferSizeEnum(7);
   public static final BBufferSizeEnum bufferSize34 = new BBufferSizeEnum(8);
   public static final BBufferSizeEnum bufferSize42 = new BBufferSizeEnum(9);
   public static final BBufferSizeEnum bufferSize50 = new BBufferSizeEnum(10);
   public static final BBufferSizeEnum bufferSize66 = new BBufferSizeEnum(11);
   public static final BBufferSizeEnum bufferSize82 = new BBufferSizeEnum(12);
   public static final BBufferSizeEnum bufferSize114 = new BBufferSizeEnum(13);
   public static final BBufferSizeEnum bufferSize146 = new BBufferSizeEnum(14);
   public static final BBufferSizeEnum bufferSize210 = new BBufferSizeEnum(15);
   public static final BBufferSizeEnum DEFAULT = bufferSize255;
   public static final Type TYPE = Sys.loadType(BBufferSizeEnum.class);
   private static final int[] SIZES = new int[]{255, -1, 20, 21, 22, 24, 26, 30, 34, 42, 50, 66, 82, 114, 146, 210};

   public static BBufferSizeEnum make(int ordinal) {
      return (BBufferSizeEnum)bufferSize255.getRange().get(ordinal, false);
   }

   public static BBufferSizeEnum make(String tag) {
      return (BBufferSizeEnum)bufferSize255.getRange().get(tag);
   }

   private BBufferSizeEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public int getSize() {
      return SIZES[this.getOrdinal()];
   }
}
