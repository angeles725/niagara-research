package com.tridium.bacnet.asn;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;

public class NWriteAccessSpec implements AsnConst {
   public static final int OBJECT_ID_TAG = 0;
   public static final int LIST_OF_PROPERTY_VALUES_TAG = 1;
   private BBacnetObjectIdentifier objectId;
   private ArrayList listOfPropertyValues;

   public NWriteAccessSpec() {
      this.listOfPropertyValues = new ArrayList();
   }

   public NWriteAccessSpec(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
      this.listOfPropertyValues = new ArrayList();
   }

   public NWriteAccessSpec(BBacnetObjectIdentifier objectId, int propertyId, byte[] propertyValue) {
      this.objectId = objectId;
      this.listOfPropertyValues = new ArrayList();
      this.addPropertyValue(propertyId, propertyValue);
   }

   public NWriteAccessSpec(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] propertyValue) {
      this.objectId = objectId;
      this.listOfPropertyValues = new ArrayList();
      this.addPropertyValue(propertyId, propertyArrayIndex, propertyValue);
   }

   public NWriteAccessSpec(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, byte[] propertyValue, int priority) {
      this.objectId = objectId;
      this.listOfPropertyValues = new ArrayList();
      this.addPropertyValue(propertyId, propertyArrayIndex, propertyValue, priority);
   }

   public NWriteAccessSpec(BBacnetObjectIdentifier objectId, Vector listOfPropertyValues) {
      this.objectId = objectId;
      this.listOfPropertyValues = new ArrayList();

      for (int i = 0; i < listOfPropertyValues.size(); i++) {
         this.addPropertyValue((NBacnetPropertyValue)listOfPropertyValues.elementAt(i));
      }
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public int getSize() {
      return this.listOfPropertyValues.size();
   }

   public void addPropertyValue(NBacnetPropertyValue propVal) {
      this.listOfPropertyValues.add(propVal);
   }

   public void addPropertyValue(int propId, byte[] propertyValue) {
      this.listOfPropertyValues.add(new NBacnetPropertyValue(propId, propertyValue));
   }

   public void addPropertyValue(int propId, int propertyArrayIndex, byte[] propertyValue) {
      this.listOfPropertyValues.add(new NBacnetPropertyValue(propId, propertyArrayIndex, propertyValue));
   }

   public void addPropertyValue(int propId, int propertyArrayIndex, byte[] propertyValue, int priority) {
      this.listOfPropertyValues.add(new NBacnetPropertyValue(propId, propertyArrayIndex, propertyValue, priority));
   }

   public void removePropertyValue(NBacnetPropertyValue propVal) {
      this.listOfPropertyValues.remove(propVal);
   }

   public void removePropertyValue(int propertyId, int propertyArrayIndex) {
      for (int i = 0; i < this.listOfPropertyValues.size(); i++) {
         if (((NBacnetPropertyValue)this.listOfPropertyValues.get(i)).getPropertyId() == propertyId
            && ((NBacnetPropertyValue)this.listOfPropertyValues.get(i)).getPropertyArrayIndex() == propertyArrayIndex) {
            this.listOfPropertyValues.remove(i);
         }
      }
   }

   public ListIterator getPropertyValues() {
      return this.listOfPropertyValues.listIterator();
   }

   public PropertyValue[] getListOfPropertyValues() {
      return this.listOfPropertyValues.toArray(new PropertyValue[0]);
   }

   public boolean isEmpty() {
      return this.listOfPropertyValues.isEmpty();
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeOpeningTag(1);

      for (NBacnetPropertyValue propVal : this.listOfPropertyValues) {
         propVal.writeAsn(out);
      }

      out.writeClosingTag(1);
   }

   public void readAsn(AsnInput in) throws AsnException, RejectException {
      this.objectId = in.readObjectIdentifier(0);
      in.peekTag();
      if (!in.isOpeningTag(1)) {
         throw new RejectException(4);
      } else {
         in.skipTag();

         do {
            NBacnetPropertyValue propVal = new NBacnetPropertyValue();
            propVal.readAsn(in);
            this.listOfPropertyValues.add(propVal);
            if (in.peekTag() == -1) {
               throw new AsnException("No closing tag");
            }
         } while (!in.isClosingTag(1));

         in.skipTag();
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NWriteAccessSpec: ");
      sb.append("\n  " + this.objectId);

      for (NBacnetPropertyValue propVal : this.listOfPropertyValues) {
         sb.append(propVal.toString());
      }

      sb.append("\n");
      return sb.toString();
   }
}
