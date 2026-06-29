package javax.baja.bacnet.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BIComparable;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BBacnetObjectIdentifier extends BSimple implements BIComparable {
   public static final int OBJECT_TYPE_MASK = -4194304;
   public static final int OBJECT_TYPE_MASK_SHIFTED = 1023;
   public static final int INSTANCE_NUMBER_MASK = 4194303;
   public static final int OBJECT_TYPE_SHIFT = 22;
   public static final int MIN_INSTANCE_NUMBER = 0;
   public static final int MAX_INSTANCE_NUMBER = 4194302;
   public static final int UNCONFIGURED_INSTANCE_NUMBER = 4194303;
   private static final char SEP = ':';
   private static final char NAME_SEP = '_';
   public static final BBacnetObjectIdentifier DEFAULT = new BBacnetObjectIdentifier(0, -1);
   public static final BBacnetObjectIdentifier DEFAULT_DEVICE = new BBacnetObjectIdentifier(8, -1);
   public static final Type TYPE = Sys.loadType(BBacnetObjectIdentifier.class);
   private final int objectType;
   private final int instanceNumber;
   private final int hashCode;

   private BBacnetObjectIdentifier(int objectType, int instanceNumber) {
      this.objectType = objectType;
      this.instanceNumber = instanceNumber;
      this.hashCode = objectType << 22 & -4194304 | instanceNumber & 4194303;
   }

   public static BBacnetObjectIdentifier make(int objectType) {
      return new BBacnetObjectIdentifier(objectType, -1);
   }

   public static BBacnetObjectIdentifier make(int objectType, int instanceNumber) {
      return new BBacnetObjectIdentifier(objectType, instanceNumber);
   }

   public static BBacnetObjectIdentifier make(BBacnetObjectType objectType, int instanceNumber) {
      return new BBacnetObjectIdentifier(objectType.getOrdinal(), instanceNumber);
   }

   public static BBacnetObjectIdentifier makeId(int objectId) {
      int objectType = objectId >> 22 & 1023;
      int instanceNumber = objectId & 4194303;
      return new BBacnetObjectIdentifier(objectType, instanceNumber);
   }

   public boolean equals(Object obj) {
      return !(obj instanceof BBacnetObjectIdentifier)
         ? false
         : ((BBacnetObjectIdentifier)obj).objectType == this.objectType && ((BBacnetObjectIdentifier)obj).instanceNumber == this.instanceNumber;
   }

   public String toString(Context context) {
      char sep = ':';
      StringBuilder sb = new StringBuilder();

      try {
         if (context == null) {
            return this.encodeToString();
         } else {
            if (context.equals(BacnetConst.nameContext) || context.equals(BacnetConst.facetsContext)) {
               sep = '_';
            }

            BEnumRange r = (BEnumRange)context.getFacet("range");
            if (r != null) {
               sb.append(r.getTag(this.objectType)).append(sep).append(this.instanceNumber);
            } else {
               sb.append(BBacnetObjectType.tag(this.objectType)).append(sep).append(this.instanceNumber);
            }

            return !context.equals(BacnetConst.nameContext) && !context.equals(BacnetConst.facetsContext) ? sb.toString() : SlotPath.escape(sb.toString());
         }
      } catch (IOException var5) {
         return var5.toString();
      }
   }

   public String toShortString() {
      return this.instanceNumber == 4194303 ? "" : BBacnetObjectType.getShortTag(this.objectType) + this.instanceNumber;
   }

   public int hashCode() {
      return this.hashCode;
   }

   public void encode(DataOutput out) throws IOException {
      out.writeInt(this.objectType);
      out.writeInt(this.instanceNumber);
   }

   public BObject decode(DataInput in) throws IOException {
      return new BBacnetObjectIdentifier(in.readInt(), in.readInt());
   }

   public String encodeToString() throws IOException {
      return BBacnetObjectType.tag(this.objectType) + ':' + this.instanceNumber;
   }

   public BObject decodeFromString(String s) throws IOException {
      try {
         StringTokenizer st = new StringTokenizer(s, ":_ ");
         int objectType = BBacnetObjectType.ordinal(st.nextToken());
         int instanceNumber = Integer.parseInt(st.nextToken());
         return new BBacnetObjectIdentifier(objectType, instanceNumber);
      } catch (Exception var5) {
         throw new IOException("Error decoding BBacnetObjectIdentifier " + s);
      }
   }

   public boolean isValid() {
      return this.objectType >= 0 && this.instanceNumber >= 0 && this.instanceNumber < 4194303;
   }

   public int getObjectType() {
      return this.objectType;
   }

   public int getInstanceNumber() {
      return this.instanceNumber;
   }

   public int getId() {
      return this.hashCode();
   }

   public BBacnetObjectIdentifier newId(int newInstanceNumber) {
      return new BBacnetObjectIdentifier(this.objectType, newInstanceNumber);
   }

   public int compareTo(Object o) {
      if (!(o instanceof BBacnetObjectIdentifier)) {
         throw new ClassCastException();
      } else {
         BBacnetObjectIdentifier id = (BBacnetObjectIdentifier)o;
         long my = this.hashCode();
         long his = id.hashCode();
         return (int)(my - his);
      }
   }

   public Type getType() {
      return TYPE;
   }
}
