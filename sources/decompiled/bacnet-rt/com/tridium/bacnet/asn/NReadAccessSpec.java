package com.tridium.bacnet.asn;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.RejectException;

public class NReadAccessSpec implements AsnConst {
   public static final int OBJECT_ID_TAG = 0;
   public static final int LIST_OF_PROPERTY_REFERENCES_TAG = 1;
   public static final int BASE_DATA_SIZE = 7;
   private BBacnetObjectIdentifier objectId;
   private ArrayList listOfPropertyReferences;
   private int encodedSize = 7;
   private int expectedDataSize = 7;

   public NReadAccessSpec() {
      this.listOfPropertyReferences = new ArrayList();
   }

   public NReadAccessSpec(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
      this.listOfPropertyReferences = new ArrayList();
   }

   public NReadAccessSpec(BBacnetObjectIdentifier objectId, int propertyId) {
      this.objectId = objectId;
      this.listOfPropertyReferences = new ArrayList();
      this.addPropertyReference(propertyId);
   }

   public NReadAccessSpec(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex) {
      this.objectId = objectId;
      this.listOfPropertyReferences = new ArrayList();
      this.addPropertyReference(propertyId, propertyArrayIndex);
   }

   public NReadAccessSpec(BBacnetObjectIdentifier objectId, Vector listOfPropertyReferences) {
      this.objectId = objectId;
      this.listOfPropertyReferences = new ArrayList();

      for (int i = 0; i < listOfPropertyReferences.size(); i++) {
         this.addPropertyReference((NBacnetPropertyReference)listOfPropertyReferences.elementAt(i));
      }
   }

   public NReadAccessSpec(BBacnetObjectIdentifier objectId, int[] propertyIds) {
      this.objectId = objectId;
      this.listOfPropertyReferences = new ArrayList();

      for (int i = 0; i < propertyIds.length; i++) {
         this.addPropertyReference(propertyIds[i]);
      }
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public int getSize() {
      return this.listOfPropertyReferences.size();
   }

   public void addPropertyReference(NBacnetPropertyReference propRef) {
      this.listOfPropertyReferences.add(propRef);
      this.encodedSize += 2;
      this.expectedDataSize += 10;
   }

   public void addPropertyReference(int propId) {
      this.addPropertyReference(propId, -1, 10);
   }

   public void addPropertyReference(int propId, int propertyArrayIndex) {
      this.addPropertyReference(propId, propertyArrayIndex, 10);
   }

   public void addPropertyReference(int propId, int propertyArrayIndex, int dataSize) {
      this.listOfPropertyReferences.add(new NBacnetPropertyReference(propId, propertyArrayIndex));
      this.encodedSize += 2;
      this.expectedDataSize += dataSize;
   }

   public void removePropertyReference(NBacnetPropertyReference propRef) {
      this.listOfPropertyReferences.remove(propRef);
      this.encodedSize -= 2;
      this.expectedDataSize -= 10;
   }

   public void removePropertyReference(int propertyId, int propertyArrayIndex) {
      for (int i = 0; i < this.listOfPropertyReferences.size(); i++) {
         if (((NBacnetPropertyReference)this.listOfPropertyReferences.get(i)).getPropertyId() == propertyId
            && ((NBacnetPropertyReference)this.listOfPropertyReferences.get(i)).getPropertyArrayIndex() == propertyArrayIndex) {
            this.listOfPropertyReferences.remove(i);
         }
      }

      this.encodedSize -= 2;
      this.expectedDataSize -= 10;
   }

   public ListIterator getPropertyReferences() {
      return this.listOfPropertyReferences.listIterator();
   }

   public PropertyReference[] getListOfPropertyReferences() {
      return this.listOfPropertyReferences.toArray(new PropertyReference[0]);
   }

   public boolean isEmpty() {
      return this.listOfPropertyReferences.isEmpty();
   }

   public int getEncodedSize() {
      return this.encodedSize;
   }

   public int getExpectedDataSize() {
      return this.expectedDataSize;
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeOpeningTag(1);

      for (NBacnetPropertyReference propRef : this.listOfPropertyReferences) {
         propRef.writeAsn(out);
      }

      out.writeClosingTag(1);
   }

   public void readAsn(AsnInput in) throws AsnException, RejectException {
      this.objectId = in.readObjectIdentifier(0);
      int tag = in.peekTag();
      if (in.isOpeningTag(1)) {
         in.skipTag();

         for (int var4 = in.peekTag(); !in.isClosingTag(1); var4 = in.peekTag()) {
            if (var4 == -1) {
               throw new RejectException(4);
            }

            NBacnetPropertyReference propRef = new NBacnetPropertyReference();
            propRef.readAsn(in);
            this.listOfPropertyReferences.add(propRef);
         }

         in.skipTag();
      } else {
         throw new RejectException(4);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("  " + this.objectId);

      for (NBacnetPropertyReference propRef : this.listOfPropertyReferences) {
         sb.append(propRef.toString());
      }

      return sb.toString();
   }
}
