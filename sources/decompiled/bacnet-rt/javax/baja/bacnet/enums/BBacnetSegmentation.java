package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("segmentedBoth"), @Range("segmentedTransmit"), @Range("segmentedReceive"), @Range("noSegmentation")},
   defaultValue = "noSegmentation"
)
public final class BBacnetSegmentation extends BFrozenEnum {
   public static final int SEGMENTED_BOTH = 0;
   public static final int SEGMENTED_TRANSMIT = 1;
   public static final int SEGMENTED_RECEIVE = 2;
   public static final int NO_SEGMENTATION = 3;
   public static final BBacnetSegmentation segmentedBoth = new BBacnetSegmentation(0);
   public static final BBacnetSegmentation segmentedTransmit = new BBacnetSegmentation(1);
   public static final BBacnetSegmentation segmentedReceive = new BBacnetSegmentation(2);
   public static final BBacnetSegmentation noSegmentation = new BBacnetSegmentation(3);
   public static final BBacnetSegmentation DEFAULT = noSegmentation;
   public static final Type TYPE = Sys.loadType(BBacnetSegmentation.class);

   public static BBacnetSegmentation make(int ordinal) {
      return (BBacnetSegmentation)segmentedBoth.getRange().get(ordinal, false);
   }

   public static BBacnetSegmentation make(String tag) {
      return (BBacnetSegmentation)segmentedBoth.getRange().get(tag);
   }

   private BBacnetSegmentation(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isSegmentedTransmit() {
      return this.getOrdinal() < 2;
   }

   public boolean isSegmentedReceive() {
      return (this.getOrdinal() & 1) == 0;
   }

   public static String tag(int id) {
      return DEFAULT.getRange().getTag(id);
   }
}
