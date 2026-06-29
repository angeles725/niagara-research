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
import javax.baja.sys.BSimple;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   ), @NiagaraProperty(
      name = "value",
      type = "BValue",
      defaultValue = "BBacnetNull.DEFAULT"
   ), @NiagaraProperty(
      name = "priority",
      type = "int",
      defaultValue = "0"
   )})
public final class BBacnetPropertyValue extends BComponent implements PropertyValue, BIBacnetDataType {
   public static final Property propertyId = newProperty(0, 85, null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Property value = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Property priority = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetPropertyValue.class);
   public static final int PROPERTY_ID_TAG = 0;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 1;
   public static final int VALUE_TAG = 2;
   public static final int PRIORITY_TAG = 3;

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

   @Override
   public int getPriority() {
      return this.getInt(priority);
   }

   public void setPriority(int v) {
      this.setInt(priority, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetPropertyValue() {
   }

   public BBacnetPropertyValue(int propertyId, BSimple value) {
      this(propertyId, -1, value, 0);
   }

   public BBacnetPropertyValue(int propertyId, int propertyArrayIndex, BSimple value) {
      this(propertyId, propertyArrayIndex, value, 0);
   }

   public BBacnetPropertyValue(int propertyId, BSimple value, int priority) {
      this(propertyId, -1, value, priority);
   }

   public BBacnetPropertyValue(int propertyId, int propertyArrayIndex, BSimple value, int priority) {
      this.setPropertyId(propertyId);
      this.setPropertyArrayIndex(propertyArrayIndex);
      this.setValue(value);
      if (priority >= 0 && priority <= 16) {
         this.setPriority(priority);
      } else {
         throw new IllegalArgumentException("BacnetPropertyValue.priority must be 1-16");
      }
   }

   @Override
   public byte[] getPropertyValue() {
      return AsnUtil.toAsn(this.getValue());
   }

   @Override
   public ErrorType getPropertyAccessError() {
      return null;
   }

   @Override
   public int getErrorClass() {
      throw new IllegalStateException();
   }

   @Override
   public int getErrorCode() {
      throw new IllegalStateException();
   }

   @Override
   public boolean isError() {
      return false;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.getPropertyArrayIndex() != -1;
   }

   public boolean isPriorityUsed() {
      return this.getPriority() > 0;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getPropertyId());
      if (this.isPropertyArrayIndexUsed()) {
         out.writeUnsignedInteger(1, this.getPropertyArrayIndex());
      }

      out.writeEncodedValue(2, AsnUtil.toAsn(this.getValue()));
      if (this.isPriorityUsed()) {
         out.writeUnsignedInteger(3, this.getPriority());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int propertyId = in.readEnumerated(0);
      in.peekTag();
      int propertyArrayIndex = in.isValueTag(1) ? in.readUnsignedInt(1) : -1;
      BValue value = AsnUtil.asnToValue(in.readEncodedValue(2));
      in.peekTag();
      int priority = in.isValueTag(3) ? in.readUnsignedInt(3) : 0;
      this.setInt(BBacnetPropertyValue.propertyId, propertyId, noWrite);
      this.setInt(BBacnetPropertyValue.propertyArrayIndex, propertyArrayIndex, noWrite);
      this.set(BBacnetPropertyValue.value, value, noWrite);
      this.setInt(BBacnetPropertyValue.priority, priority, noWrite);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(BBacnetPropertyIdentifier.tag(this.getPropertyId()));
      if (this.isPropertyArrayIndexUsed()) {
         sb.append('[').append(this.getPropertyArrayIndex()).append("]:");
      } else {
         sb.append(':');
      }

      sb.append(this.getValue().toString(cx));
      if (this.isPriorityUsed()) {
         sb.append(" @").append(this.getPriority());
      }

      return sb.toString();
   }
}
