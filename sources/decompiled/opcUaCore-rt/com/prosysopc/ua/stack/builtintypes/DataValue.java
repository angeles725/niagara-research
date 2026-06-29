package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.StructureUtils;
import com.prosysopc.ua.stack.core.Identifiers;
import java.util.Objects;

public class DataValue implements Cloneable {
   @Deprecated
   public static final NodeId ID = Identifiers.DataValue;
   public static final DataValue[] EMPTY_ARRAY = new DataValue[0];
   Variant iu;
   StatusCode statusCode;
   DateTime qR;
   UnsignedShort qX;
   DateTime qW;
   UnsignedShort rL;

   public DataValue() {
      this(StatusCode.GOOD);
   }

   public DataValue(StatusCode var1) {
      this(Variant.NULL, var1);
   }

   public DataValue(Variant var1) {
      this(var1, StatusCode.GOOD);
   }

   public DataValue(Variant var1, StatusCode var2) {
      this(var1, var2, null, null, null, null);
   }

   public DataValue(Variant var1, StatusCode var2, DateTime var3, DateTime var4) {
      this(var1, var2, var3, null, var4, null);
   }

   public DataValue(Variant var1, StatusCode var2, DateTime var3, UnsignedShort var4, DateTime var5, UnsignedShort var6) {
      this.statusCode = var2 == null ? StatusCode.GOOD : var2;
      this.qR = var3;
      this.qW = var5;
      this.qX = var4 == null ? UnsignedShort.ZERO : var4;
      this.rL = var6 == null ? UnsignedShort.ZERO : var6;
      this.setValue(var1);
   }

   public DataValue clone() {
      try {
         DataValue var1 = (DataValue)super.clone();
         var1.iu = var1.iu == null ? null : new Variant(StructureUtils.clone(var1.iu.getValue()));
         return var1;
      } catch (CloneNotSupportedException var2) {
         throw new Error("Could not call super.clone on DataValue even when it implements Cloneable");
      }
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof DataValue)) {
         return false;
      } else {
         DataValue var2 = (DataValue)var1;
         return Objects.equals(var2.iu, this.iu)
            && Objects.equals(var2.statusCode, this.statusCode)
            && Objects.equals(var2.qR, this.qR)
            && Objects.equals(var2.qW, this.qW)
            && Objects.equals(var2.qX, this.qX)
            && Objects.equals(var2.rL, this.rL);
      }
   }

   public UnsignedShort getServerPicoseconds() {
      return this.rL;
   }

   public DateTime getServerTimestamp() {
      return this.qW;
   }

   public UnsignedShort getSourcePicoseconds() {
      return this.qX;
   }

   public DateTime getSourceTimestamp() {
      return this.qR;
   }

   public StatusCode getStatusCode() {
      return this.statusCode;
   }

   public Variant getValue() {
      return this.iu;
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.iu, this.statusCode, this.qR, this.qX, this.qW, this.rL);
   }

   public boolean isNull() {
      return this.iu.getValue() == null;
   }

   public void setServerPicoseconds(UnsignedShort var1) {
      this.rL = var1;
   }

   public void setServerTimestamp(DateTime var1) {
      this.qW = var1;
   }

   public void setSourcePicoseconds(UnsignedShort var1) {
      this.qX = var1;
   }

   public void setSourceTimestamp(DateTime var1) {
      this.qR = var1;
   }

   public void setStatusCode(StatusCode var1) {
      this.statusCode = var1 == null ? StatusCode.GOOD : var1;
   }

   public void setStatusCode(UnsignedInteger var1) {
      if (var1 == null) {
         this.setStatusCode(StatusCode.GOOD);
      } else {
         this.setStatusCode(StatusCode.valueOf(var1));
      }
   }

   public void setValue(Variant var1) {
      if (var1 == null) {
         this.iu = Variant.NULL;
      } else {
         this.iu = var1;
      }
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      var1.append("DataValue(");
      var1.append("value=" + this.iu);
      var1.append(", statusCode=" + this.statusCode);
      var1.append(", sourceTimestamp=" + this.qR);
      var1.append(", sourcePicoseconds=" + this.qX);
      var1.append(", serverTimestamp=" + this.qW);
      var1.append(", serverPicoseconds=" + this.rL);
      var1.append(")");
      return var1.toString();
   }
}
