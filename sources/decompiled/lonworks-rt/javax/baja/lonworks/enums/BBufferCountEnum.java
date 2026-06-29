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
      value = "bufferCnt0",
      ordinal = 0
   ), @Range(
      value = "bufferCnt1",
      ordinal = 2
   ), @Range(
      value = "bufferCnt2",
      ordinal = 3
   ), @Range(
      value = "bufferCnt3",
      ordinal = 4
   ), @Range(
      value = "bufferCnt5",
      ordinal = 5
   ), @Range(
      value = "bufferCnt7",
      ordinal = 6
   ), @Range(
      value = "bufferCnt11",
      ordinal = 7
   ), @Range(
      value = "bufferCnt15",
      ordinal = 8
   ), @Range(
      value = "bufferCnt23",
      ordinal = 9
   ), @Range(
      value = "bufferCnt31",
      ordinal = 10
   ), @Range(
      value = "bufferCnt47",
      ordinal = 11
   ), @Range(
      value = "bufferCnt63",
      ordinal = 12
   ), @Range(
      value = "bufferCnt95",
      ordinal = 13
   ), @Range(
      value = "bufferCnt127",
      ordinal = 14
   ), @Range(
      value = "bufferCnt191",
      ordinal = 15
   )},
   defaultValue = "bufferCnt0"
)
public final class BBufferCountEnum extends BFrozenEnum {
   public static final int BUFFER_CNT_0 = 0;
   public static final int BUFFER_CNT_1 = 2;
   public static final int BUFFER_CNT_2 = 3;
   public static final int BUFFER_CNT_3 = 4;
   public static final int BUFFER_CNT_5 = 5;
   public static final int BUFFER_CNT_7 = 6;
   public static final int BUFFER_CNT_11 = 7;
   public static final int BUFFER_CNT_15 = 8;
   public static final int BUFFER_CNT_23 = 9;
   public static final int BUFFER_CNT_31 = 10;
   public static final int BUFFER_CNT_47 = 11;
   public static final int BUFFER_CNT_63 = 12;
   public static final int BUFFER_CNT_95 = 13;
   public static final int BUFFER_CNT_127 = 14;
   public static final int BUFFER_CNT_191 = 15;
   public static final BBufferCountEnum bufferCnt0 = new BBufferCountEnum(0);
   public static final BBufferCountEnum bufferCnt1 = new BBufferCountEnum(2);
   public static final BBufferCountEnum bufferCnt2 = new BBufferCountEnum(3);
   public static final BBufferCountEnum bufferCnt3 = new BBufferCountEnum(4);
   public static final BBufferCountEnum bufferCnt5 = new BBufferCountEnum(5);
   public static final BBufferCountEnum bufferCnt7 = new BBufferCountEnum(6);
   public static final BBufferCountEnum bufferCnt11 = new BBufferCountEnum(7);
   public static final BBufferCountEnum bufferCnt15 = new BBufferCountEnum(8);
   public static final BBufferCountEnum bufferCnt23 = new BBufferCountEnum(9);
   public static final BBufferCountEnum bufferCnt31 = new BBufferCountEnum(10);
   public static final BBufferCountEnum bufferCnt47 = new BBufferCountEnum(11);
   public static final BBufferCountEnum bufferCnt63 = new BBufferCountEnum(12);
   public static final BBufferCountEnum bufferCnt95 = new BBufferCountEnum(13);
   public static final BBufferCountEnum bufferCnt127 = new BBufferCountEnum(14);
   public static final BBufferCountEnum bufferCnt191 = new BBufferCountEnum(15);
   public static final BBufferCountEnum DEFAULT = bufferCnt0;
   public static final Type TYPE = Sys.loadType(BBufferCountEnum.class);
   private static final int[] COUNTS = new int[]{0, -1, 1, 2, 3, 5, 7, 11, 15, 23, 31, 47, 63, 95, 127, 191};

   public static BBufferCountEnum make(int ordinal) {
      return (BBufferCountEnum)bufferCnt0.getRange().get(ordinal, false);
   }

   public static BBufferCountEnum make(String tag) {
      return (BBufferCountEnum)bufferCnt0.getRange().get(tag);
   }

   private BBufferCountEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public int getCount() {
      return COUNTS[this.getOrdinal()];
   }
}
