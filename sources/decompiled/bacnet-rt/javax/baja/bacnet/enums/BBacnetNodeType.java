package javax.baja.bacnet.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unknown"), @Range("system"), @Range("network"), @Range("device"), @Range("organizational"), @Range("area"), @Range("equipment"), @Range("point"), @Range("collection"), @Range("property"), @Range("functional"), @Range("other")}
)
public final class BBacnetNodeType extends BFrozenEnum {
   public static final int UNKNOWN = 0;
   public static final int SYSTEM = 1;
   public static final int NETWORK = 2;
   public static final int DEVICE = 3;
   public static final int ORGANIZATIONAL = 4;
   public static final int AREA = 5;
   public static final int EQUIPMENT = 6;
   public static final int POINT = 7;
   public static final int COLLECTION = 8;
   public static final int PROPERTY = 9;
   public static final int FUNCTIONAL = 10;
   public static final int OTHER = 11;
   public static final BBacnetNodeType unknown = new BBacnetNodeType(0);
   public static final BBacnetNodeType system = new BBacnetNodeType(1);
   public static final BBacnetNodeType network = new BBacnetNodeType(2);
   public static final BBacnetNodeType device = new BBacnetNodeType(3);
   public static final BBacnetNodeType organizational = new BBacnetNodeType(4);
   public static final BBacnetNodeType area = new BBacnetNodeType(5);
   public static final BBacnetNodeType equipment = new BBacnetNodeType(6);
   public static final BBacnetNodeType point = new BBacnetNodeType(7);
   public static final BBacnetNodeType collection = new BBacnetNodeType(8);
   public static final BBacnetNodeType property = new BBacnetNodeType(9);
   public static final BBacnetNodeType functional = new BBacnetNodeType(10);
   public static final BBacnetNodeType other = new BBacnetNodeType(11);
   public static final BBacnetNodeType DEFAULT = unknown;
   public static final Type TYPE = Sys.loadType(BBacnetNodeType.class);

   public static BBacnetNodeType make(int ordinal) {
      return (BBacnetNodeType)unknown.getRange().get(ordinal, false);
   }

   public static BBacnetNodeType make(String tag) {
      return (BBacnetNodeType)unknown.getRange().get(tag);
   }

   private BBacnetNodeType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
