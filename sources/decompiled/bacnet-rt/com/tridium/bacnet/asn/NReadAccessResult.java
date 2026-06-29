package com.tridium.bacnet.asn;

import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyValue;

public class NReadAccessResult implements AsnConst {
   public static final int OBJECT_ID_TAG = 0;
   public static final int LIST_OF_RESULTS_TAG = 1;
   private BBacnetObjectIdentifier objectId;
   private Vector listOfResults;

   public NReadAccessResult() {
      this.listOfResults = new Vector();
   }

   public NReadAccessResult(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
      this.listOfResults = new Vector();
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void addResult(PropertyValue result) {
      this.listOfResults.addElement(result);
   }

   public ListIterator getResults() {
      return this.listOfResults.listIterator();
   }

   public Vector getListOfResults() {
      return this.listOfResults;
   }

   public void writeAsn(AsnOutput out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeOpeningTag(1);

      for (NReadPropertyResult result : this.listOfResults) {
         result.writeAsn(out);
      }

      out.writeClosingTag(1);
   }

   public void readAsn(AsnInput in) throws AsnException {
      this.objectId = in.readObjectIdentifier(0);
      int tag = in.peekTag();
      if (in.isOpeningTag(1)) {
         in.skipTag();

         for (int var4 = in.peekTag(); !in.isClosingTag(1); var4 = in.peekTag()) {
            if (var4 == -1) {
               throw new AsnException("Invalid tag: " + var4);
            }

            NReadPropertyResult result = new NReadPropertyResult(this);
            result.readAsn(in);
            this.listOfResults.addElement(result);
         }

         in.skipTag();
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("NReadAccessResult: ");
      sb.append("\n  " + this.objectId);

      for (PropertyValue result : this.listOfResults) {
         sb.append(result.toString());
      }

      sb.append("\n");
      return sb.toString();
   }
}
