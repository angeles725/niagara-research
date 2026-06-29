package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.AsnUtil;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE",
      flags = 1
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED",
      flags = 1
   ), @NiagaraProperty(
      name = "value",
      type = "BValue",
      defaultValue = "BBacnetNull.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "error",
      type = "BErrorType",
      defaultValue = "new BErrorType()",
      flags = 1
   )})
public final class BReadPropertyResult extends BComponent implements PropertyValue, BIBacnetDataType {
   public static final Property propertyId = newProperty(1, 85, null);
   public static final Property propertyArrayIndex = newProperty(1, -1, null);
   public static final Property value = newProperty(1, BBacnetNull.DEFAULT, null);
   public static final Property error = newProperty(1, new BErrorType(), null);
   public static final Type TYPE = Sys.loadType(BReadPropertyResult.class);
   public static final int PROPERTY_ID_TAG = 2;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 3;
   public static final int PROPERTY_VALUE_TAG = 4;
   public static final int PROPERTY_ACCESS_ERROR_TAG = 5;
   private boolean isError = false;
   private byte[] propertyValue = null;

   @Override
   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.getInt(propertyArrayIndex);
   }

   public void setPropertyArrayIndex(int v) {
      this.setInt(propertyArrayIndex, v, null);
   }

   public BValue getValue() {
      return this.get(value);
   }

   public void setValue(BValue v) {
      this.set(value, v, null);
   }

   public BErrorType getError() {
      return (BErrorType)this.get(error);
   }

   public void setError(BErrorType v) {
      this.set(error, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BReadPropertyResult() {
   }

   public BReadPropertyResult(int propertyId) {
      this.setPropertyId(propertyId);
   }

   public BReadPropertyResult(int propertyId, int propertyArrayIndex) {
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
   }

   public void changed(Property p, Context cx) {
      if (p.equals(value)) {
         this.propertyValue = null;
      } else if (p.equals(error)) {
         if (this.getError().isDefault()) {
            this.isError = false;
         } else {
            this.isError = true;
         }

         this.propertyValue = null;
      }
   }

   @Override
   public byte[] getPropertyValue() {
      if (this.propertyValue == null && !this.isError) {
         this.propertyValue = AsnUtil.toAsn(this.getValue());
      }

      return this.propertyValue;
   }

   @Override
   public int getPriority() {
      return -1;
   }

   @Override
   public ErrorType getPropertyAccessError() {
      return this.getError();
   }

   @Override
   public int getErrorClass() {
      return this.getPropertyAccessError().getErrorClass();
   }

   @Override
   public int getErrorCode() {
      return this.getPropertyAccessError().getErrorCode();
   }

   @Override
   public boolean isError() {
      return this.isError;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(2, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(3, this.getPropertyArrayIndex());
      }

      if (this.isError) {
         out.writeOpeningTag(5);
         this.getError().writeAsn(out);
         out.writeClosingTag(5);
      } else {
         BValue v = this.getValue();
         if (!(v instanceof BIBacnetDataType)) {
            throw new IllegalStateException("propertyValue type " + v.getType() + " is not a BIBacnetDataType!");
         }

         out.writeOpeningTag(4);
         ((BIBacnetDataType)v).writeAsn(out);
         out.writeClosingTag(4);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int propertyId = in.readEnumerated(2);
      in.peekTag();
      int propertyArrayIndex = in.isValueTag(3) ? in.readUnsignedInt(3) : -1;
      int readResultChoice = in.peekTag();
      switch (readResultChoice) {
         case 4:
            byte[] propertyValue = in.readEncodedValue(4);
            BValue value = AsnUtil.asnToValue(propertyValue);
            this.isError = false;
            this.propertyValue = propertyValue;
            this.set(BReadPropertyResult.value, value, noWrite);
            this.getError().setToDefault(noWrite);
            break;
         case 5:
            in.skipOpeningTag(5);
            BErrorType error = new BErrorType();
            error.readAsn(in);
            in.skipClosingTag(5);
            this.isError = true;
            this.propertyValue = null;
            this.set(BReadPropertyResult.error, error, noWrite);
            break;
         default:
            throw new AsnException("Invalid tag: " + readResultChoice);
      }

      this.setInt(BReadPropertyResult.propertyId, propertyId, noWrite);
      this.setInt(BReadPropertyResult.propertyArrayIndex, propertyArrayIndex, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append(" [").append(this.getPropertyArrayIndex()).append("] ");
      }

      sb.append(":");
      if (this.isError) {
         sb.append(this.getError().toString(cx));
      } else {
         sb.append(this.getValue().toString(cx));
      }

      return sb.toString();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append("[" + this.getPropertyArrayIndex() + "]");
      }

      if (this.isError) {
         sb.append("\n  err:" + this.getError().toString());
      } else {
         sb.append("\n  val:" + this.getValue().toString());
      }

      return sb.toString();
   }
}
