package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.error.CreateObjectError;
import java.util.Iterator;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.util.Array;

public class CreateObjectRequest extends BacnetConfirmedRequest {
   int objectType = -1;
   BBacnetObjectIdentifier objectId = null;
   private Array<PropertyValue> listOfInitialValues = null;
   public static final int OBJECT_SPECIFIER_TAG = 0;
   public static final int OBJECT_TYPE_TAG = 0;
   public static final int OBJECT_ID_TAG = 1;
   public static final int LIST_OF_INITIAL_VALUES_TAG = 1;
   public static final int OBJECT_TYPE_NOT_USED = -1;

   public CreateObjectRequest() {
      super(10);
      this.listOfInitialValues = new Array(PropertyValue.class);
   }

   public CreateObjectRequest(int objectType) {
      super(10);
      this.objectType = objectType;
      this.listOfInitialValues = null;
   }

   public CreateObjectRequest(int objectType, Array<PropertyValue> ivs) {
      super(10);
      this.objectType = objectType;
      this.listOfInitialValues = ivs;
   }

   public CreateObjectRequest(BBacnetObjectIdentifier objectId) {
      super(10);
      this.objectId = objectId;
      this.listOfInitialValues = null;
   }

   public CreateObjectRequest(BBacnetObjectIdentifier objectId, Array<PropertyValue> ivs) {
      super(10);
      this.objectId = objectId;
      this.listOfInitialValues = ivs;
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException, RejectException {
      int tag = in.peekTag();
      if (in.isOpeningTag(0)) {
         in.skipTag();
         tag = in.peekTag();
         if (tag == 0) {
            this.objectType = in.readEnumerated(0);
         } else {
            if (tag != 1) {
               throw new RejectException(4);
            }

            this.objectId = in.readObjectIdentifier(1);
         }

         in.skipTag();
         tag = in.peekTag();
         if (tag != -1) {
            if (in.isOpeningTag(1)) {
               in.skipTag();

               for (int var6 = in.peekTag(); !in.isClosingTag(1); var6 = in.peekTag()) {
                  if (var6 == -1) {
                     throw new RejectException(4);
                  }

                  NBacnetPropertyValue pv = new NBacnetPropertyValue();
                  pv.readAsn(in);
                  this.listOfInitialValues.add(pv);
               }
            } else {
               throw new RejectException(4);
            }
         }
      } else {
         throw new RejectException(4);
      }
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(0);
      if (this.objectId == null) {
         out.writeEnumerated(0, this.objectType);
      } else {
         out.writeObjectIdentifier(1, this.objectId);
      }

      out.writeClosingTag(0);
      if (this.listOfInitialValues != null && this.listOfInitialValues.size() > 0) {
         out.writeOpeningTag(1);

         for (PropertyValue pv : this.listOfInitialValues) {
            pv.writeAsn(out);
         }

         out.writeClosingTag(1);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("CreateObjectRequest: ");
      if (this.objectId == null) {
         sb.append("\n  " + BBacnetObjectType.tag(this.objectType));
      } else {
         sb.append("\n  " + this.objectId);
      }

      if (this.listOfInitialValues != null) {
         Iterator<PropertyValue> it = this.listOfInitialValues.iterator();

         while (it.hasNext()) {
            sb.append("\n  " + it.next().toString());
         }
      }

      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new CreateObjectAck(serviceChoice, inputStream);
   }

   @Override
   protected BacnetError doParseError(int errorChoice, byte[] encodedError) throws AsnException {
      return new CreateObjectError(errorChoice, encodedError);
   }

   public int getObjectType() {
      return this.objectType;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public Array<PropertyValue> getListOfInitialValues() {
      return this.listOfInitialValues;
   }
}
