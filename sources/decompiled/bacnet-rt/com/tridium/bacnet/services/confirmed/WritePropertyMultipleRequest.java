package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NWriteAccessSpec;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.error.WritePropertyMultipleError;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class WritePropertyMultipleRequest extends BacnetConfirmedRequest {
   private Vector<NWriteAccessSpec> listOfWriteAccessSpecs;

   public WritePropertyMultipleRequest() {
      super(16);
      this.listOfWriteAccessSpecs = new Vector<>();
   }

   public WritePropertyMultipleRequest(Vector<NWriteAccessSpec> listOfWriteAccessSpecs) {
      super(16);
      this.listOfWriteAccessSpecs = listOfWriteAccessSpecs;
   }

   public void addWriteAccessSpec(NWriteAccessSpec was) {
      this.listOfWriteAccessSpecs.addElement(was);
   }

   public void addWriteAccessSpec(BBacnetObjectIdentifier objectId, int[] propertyIds, Vector propertyValues) {
      Vector listOfPropRefs = new Vector();

      for (int i = 0; i < propertyIds.length; i++) {
         listOfPropRefs.addElement(new NBacnetPropertyValue(propertyIds[i], (byte[])propertyValues.elementAt(i)));
      }

      this.listOfWriteAccessSpecs.addElement(new NWriteAccessSpec(objectId, listOfPropRefs));
   }

   public void addWriteAccessSpec(BBacnetObjectIdentifier objectId, int[] propertyIds, Vector propertyValues, int[] priorities) {
      Vector listOfPropRefs = new Vector();

      for (int i = 0; i < propertyIds.length; i++) {
         listOfPropRefs.addElement(new NBacnetPropertyValue(propertyIds[i], (byte[])propertyValues.elementAt(i), priorities[i]));
      }

      this.listOfWriteAccessSpecs.addElement(new NWriteAccessSpec(objectId, listOfPropRefs));
   }

   public ListIterator<NWriteAccessSpec> getWriteAccessSpecs() {
      return this.listOfWriteAccessSpecs.listIterator();
   }

   public Vector<NWriteAccessSpec> getListOfWriteAccessSpecs() {
      return this.listOfWriteAccessSpecs;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      for (NWriteAccessSpec writeAccessSpec : this.listOfWriteAccessSpecs) {
         writeAccessSpec.writeAsn(outputStream);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      while (inputStream.peekTag() != -1) {
         NWriteAccessSpec writeAccessSpec = new NWriteAccessSpec();
         writeAccessSpec.readAsn(inputStream);
         this.listOfWriteAccessSpecs.addElement(writeAccessSpec);
      }
   }

   @Override
   public BacnetError doParseError(int errorChoice, byte[] encodedError) throws AsnException {
      return new WritePropertyMultipleError(errorChoice, encodedError);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("WritePropertyMultipleRequest: ");

      for (NWriteAccessSpec writeAccessSpec : this.listOfWriteAccessSpecs) {
         sb.append(writeAccessSpec.toString());
      }

      return sb.toString();
   }
}
