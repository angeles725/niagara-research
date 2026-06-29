package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.data.BIDataValue;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class ComplexEventType extends BacnetNotificationParameters {
   private Vector<PropertyValue> listOfPropertyValues;

   public ComplexEventType() {
      this.listOfPropertyValues = new Vector<>();
   }

   public ComplexEventType(Vector<PropertyValue> list) {
      for (int i = 0; i < list.size(); i++) {
         if (!(list.get(i) instanceof NBacnetPropertyValue)) {
            throw new IllegalArgumentException();
         }
      }

      this.listOfPropertyValues = list;
   }

   @Override
   public int getChoiceType() {
      return 6;
   }

   public void addPropertyValue(PropertyValue propVal) {
      this.listOfPropertyValues.add(propVal);
   }

   public Vector<PropertyValue> getListOfPropertyValues() {
      return this.listOfPropertyValues;
   }

   public Iterator<PropertyValue> getPropertyValues() {
      return this.listOfPropertyValues.iterator();
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(6);

      for (PropertyValue propVal : this.listOfPropertyValues) {
         propVal.writeAsn(outputStream);
      }

      outputStream.writeClosingTag(6);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      try {
         int tag = inputStream.peekTag();
         if (inputStream.isOpeningTag(6)) {
            inputStream.skipTag();

            for (int var6 = inputStream.peekTag(); !inputStream.isClosingTag(6); var6 = inputStream.peekTag()) {
               if (var6 == -1) {
                  throw new AsnException("No closing tag");
               }

               NBacnetPropertyValue propVal = new NBacnetPropertyValue();
               propVal.readAsn(inputStream);
               this.listOfPropertyValues.add(propVal);
            }

            inputStream.skipTag();
         } else {
            throw new AsnException("Invalid tag: " + tag);
         }
      } catch (RejectException var5) {
         throw new AsnException(var5.toString());
      }
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  complex-event-type\n");

      for (PropertyValue propVal : this.listOfPropertyValues) {
         sb.append("    ").append(propVal.toString()).append("\n");
      }

      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");

      for (PropertyValue v : this.listOfPropertyValues) {
         sb.append("..val.");
         valueToFacetString(v, sb);
      }

      return sb.toString();
   }

   private static void valueToFacetString(PropertyValue v, StringBuilder sb) {
      sb.append(v.getPropertyId()).append('.');
      if (v.getPropertyArrayIndex() != -1) {
         sb.append(v.getPropertyArrayIndex()).append('.');
      }

      sb.append(ByteArrayUtil.toHexString(v.getPropertyValue()));
      if (v.getPriority() != -1) {
         sb.append('.').append(v.getPriority());
      }
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      Iterator<PropertyValue> i = this.listOfPropertyValues.iterator();
      int count = 0;

      while (i.hasNext()) {
         map.put("complexEventValue" + count++, BString.make(i.next().toString()));
      }
   }
}
