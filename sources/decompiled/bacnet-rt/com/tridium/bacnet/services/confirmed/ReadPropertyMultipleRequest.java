package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.util.Array;

public class ReadPropertyMultipleRequest extends BacnetConfirmedRequest {
   private Vector<NReadAccessSpec> listOfReadAccessSpecs;

   public ReadPropertyMultipleRequest() {
      super(14);
      this.listOfReadAccessSpecs = new Vector<>();
   }

   public ReadPropertyMultipleRequest(NReadAccessSpec readAccessSpec) {
      super(14);
      this.listOfReadAccessSpecs = new Vector<>();
      this.listOfReadAccessSpecs.addElement(readAccessSpec);
   }

   public ReadPropertyMultipleRequest(Vector<NReadAccessSpec> listOfReadAccessSpecs) {
      super(14);
      this.listOfReadAccessSpecs = listOfReadAccessSpecs;
   }

   public ReadPropertyMultipleRequest(Array<NReadAccessSpec> readAccessSpecs) {
      super(14);
      this.listOfReadAccessSpecs = new Vector<>();
      if (readAccessSpecs != null) {
         Iterator<NReadAccessSpec> it = readAccessSpecs.iterator();

         while (it.hasNext()) {
            this.listOfReadAccessSpecs.addElement(it.next());
         }
      }
   }

   public void addReadAccessSpec(NReadAccessSpec ras) {
      this.listOfReadAccessSpecs.addElement(ras);
   }

   public void addReadAccessSpec(BBacnetObjectIdentifier objectId, int[] propertyIds) {
      Vector listOfPropRefs = new Vector();

      for (int i = 0; i < propertyIds.length; i++) {
         listOfPropRefs.addElement(new NBacnetPropertyReference(propertyIds[i]));
      }

      this.listOfReadAccessSpecs.addElement(new NReadAccessSpec(objectId, listOfPropRefs));
   }

   public ListIterator<NReadAccessSpec> getReadAccessSpecs() {
      return this.listOfReadAccessSpecs.listIterator();
   }

   public Vector<NReadAccessSpec> getListOfReadAccessSpecs() {
      return this.listOfReadAccessSpecs;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      for (NReadAccessSpec readAccessSpec : this.listOfReadAccessSpecs) {
         readAccessSpec.writeAsn(outputStream);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      while (inputStream.peekTag() != -1) {
         NReadAccessSpec readAccessSpec = new NReadAccessSpec();
         readAccessSpec.readAsn(inputStream);
         this.listOfReadAccessSpecs.addElement(readAccessSpec);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadPropertyMultipleRequest: ");

      for (NReadAccessSpec readAccessSpec : this.listOfReadAccessSpecs) {
         sb.append(readAccessSpec.toString());
      }

      return sb.toString();
   }

   @Override
   protected BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new ReadPropertyMultipleAck(serviceChoice, inputStream);
   }
}
