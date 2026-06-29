package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import java.util.Iterator;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.io.AsnException;
import javax.baja.data.BIDataValue;
import javax.baja.nre.util.Array;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class Extended extends BacnetNotificationParameters {
   public static final int VENDOR_ID_TAG = 0;
   public static final int EXTENDED_EVENT_TYPE_TAG = 1;
   public static final int PARAMETERS_TAG = 2;
   public static final int REFERENCE_TAG = 0;
   int vendorId;
   long extendedEventType;
   Array<Extended.Param> parameters;
   private static final int NUL = 0;
   private static final int REAL = 1;
   private static final int UNS = 2;
   private static final int BOOL = 3;
   private static final int DUBL = 4;
   private static final int OCT = 5;
   private static final int BITS = 6;
   private static final int ENUM = 7;
   private static final int REF = 8;
   private static final int INT = 9;
   private static final int CHARS = 10;
   private static final int DATE = 11;
   private static final int TIME = 12;
   private static final int OID = 13;

   public Extended() {
   }

   public Extended(int vId, long extEvType, Array<Extended.Param> params) {
      this.vendorId = vId;
      this.extendedEventType = extEvType;
      this.parameters = params;
      this.validate();
   }

   @Override
   public int getChoiceType() {
      return 9;
   }

   public int getVendorId() {
      return this.vendorId;
   }

   public void setVendorId(int vendorId) {
      this.vendorId = vendorId;
   }

   public long getExtendedEventType() {
      return this.extendedEventType;
   }

   public void setExtendedEventType(long extendedEventType) {
      this.extendedEventType = extendedEventType;
   }

   public Array<Extended.Param> getParameters() {
      return this.parameters;
   }

   public void setParameters(Array<Extended.Param> parameters) {
      this.parameters = parameters;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(9);
      outputStream.writeUnsignedInteger(0, this.vendorId);
      outputStream.writeUnsignedInteger(1, this.extendedEventType);
      outputStream.writeOpeningTag(2);
      Iterator<Extended.Param> it = this.parameters.iterator();

      while (it.hasNext()) {
         this.writeParameter(outputStream, it.next());
      }

      outputStream.writeClosingTag(2);
      outputStream.writeClosingTag(9);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      inputStream.skipOpeningTag(9);
      int vendorId = inputStream.readUnsignedInt(0);
      long extendedEventType = inputStream.readUnsignedInteger(1);
      inputStream.skipOpeningTag(2);
      Array<Extended.Param> parameters = new Array(Extended.Param.class);

      for (Extended.Param p = this.readParameter(inputStream); p != null; p = this.readParameter(inputStream)) {
         parameters.add(p);
      }

      inputStream.skipClosingTag(2);
      inputStream.skipClosingTag(9);
      this.vendorId = vendorId;
      this.extendedEventType = extendedEventType;
      this.parameters = parameters;
   }

   private void validate() {
      if (this.parameters == null) {
         this.parameters = new Array(Extended.Param.class);
      }
   }

   private Extended.Param readParameter(AsnInputStream in) throws AsnException {
      int tag = in.peekApplicationTag();
      switch (tag) {
         case 0:
            in.readNull();
            return new Extended.Param();
         case 1:
            return new Extended.Param(in.readBoolean());
         case 2:
            return new Extended.Param(in.readUnsigned());
         case 3:
            return new Extended.Param(in.readSigned());
         case 4:
            return new Extended.Param(in.readReal());
         case 5:
            return new Extended.Param(in.readDouble());
         case 6:
            return new Extended.Param(in.readOctetString());
         case 7:
            return new Extended.Param(in.readCharacterString());
         case 8:
            return new Extended.Param(in.readBitString());
         case 9:
            return new Extended.Param(in.readEnumerated());
         case 10:
            return new Extended.Param(in.readDate());
         case 11:
            return new Extended.Param(in.readTime());
         case 12:
            return new Extended.Param(in.readObjectIdentifier());
         default:
            if (in.isOpeningTag(0)) {
               in.skipTag();
               BBacnetDeviceObjectPropertyReference r = new BBacnetDeviceObjectPropertyReference();
               r.readAsn(in);
               Extended.Param p = new Extended.Param(r);
               in.skipClosingTag(0);
               return p;
            } else if (in.isClosingTag(2)) {
               return null;
            } else {
               throw new AsnException("Invalid tag: " + tag);
            }
      }
   }

   private void writeParameter(AsnOutputStream out, Extended.Param p) {
      switch (p.typ) {
         case 0:
            out.writeNull();
            break;
         case 1:
            out.writeReal(((Float)p.obj).floatValue());
            break;
         case 2:
            out.writeUnsigned((BBacnetUnsigned)p.obj);
            break;
         case 3:
            out.writeBoolean((Boolean)p.obj);
            break;
         case 4:
            out.writeDouble((Double)p.obj);
            break;
         case 5:
            out.writeOctetString((BBacnetOctetString)p.obj);
            break;
         case 6:
            out.writeBitString((BBacnetBitString)p.obj);
            break;
         case 7:
            out.writeEnumerated((Integer)p.obj);
            break;
         case 8:
            out.writeOpeningTag(0);
            ((BBacnetDeviceObjectPropertyReference)p.obj).writeAsn(out);
            out.writeClosingTag(0);
            break;
         case 9:
            out.writeSignedInteger((BInteger)p.obj);
            break;
         case 10:
            out.writeCharacterString((String)p.obj);
            break;
         case 11:
            out.writeDate((BBacnetDate)p.obj);
            break;
         case 12:
            out.writeTime((BBacnetTime)p.obj);
            break;
         case 13:
            out.writeObjectIdentifier((BBacnetObjectIdentifier)p.obj);
      }
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  extended\n");
      sb.append("    " + this.vendorId);
      sb.append("    " + this.extendedEventType);
      sb.append("    " + this.parameters);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".vId." + this.vendorId);
      sb.append(".eetyp." + this.extendedEventType);
      sb.append(".params.");
      toFacetString(this.parameters, sb);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("vendorId", BString.make(String.valueOf(this.vendorId)));
      map.put("extendedEventType", BString.make(String.valueOf(this.extendedEventType)));
      map.put("parameters", BString.make(toFacetString(this.parameters)));
   }

   private static String toFacetString(Array<Extended.Param> a) {
      StringBuilder sb = new StringBuilder();
      toFacetString(a, sb);
      return sb.toString();
   }

   private static void toFacetString(Array<Extended.Param> a, StringBuilder sb) {
      Iterator<Extended.Param> it = a.iterator();

      while (it.hasNext()) {
         sb.append(toFacetString(it.next())).append('_');
      }

      if (sb.length() > 1) {
         sb.deleteCharAt(sb.length() - 1);
      }
   }

   private static String toFacetString(Extended.Param p) {
      switch (p.typ) {
         case 0:
            return "n";
         case 1:
         case 2:
         case 3:
         case 4:
         case 7:
         case 9:
         case 10:
            return String.valueOf(p.obj);
         case 5:
            return ((BBacnetOctetString)p.obj).toString(facetsContext);
         case 6:
            return ((BBacnetBitString)p.obj).toString(facetsContext);
         case 8:
            return ((BBacnetDeviceObjectPropertyReference)p.obj).toString(facetsContext);
         case 11:
            return ((BBacnetDate)p.obj).toString(facetsContext);
         case 12:
            return ((BBacnetTime)p.obj).toString(facetsContext);
         case 13:
            return ((BBacnetObjectIdentifier)p.obj).toString(facetsContext);
         default:
            return "err";
      }
   }

   public static class Param {
      private final int typ;
      private final Object obj;

      public Param() {
         this.typ = 0;
         this.obj = null;
      }

      public Param(float f) {
         this.typ = 1;
         this.obj = f;
      }

      public Param(BBacnetUnsigned u) {
         this.typ = 2;
         this.obj = u;
      }

      public Param(boolean b) {
         this.typ = 3;
         this.obj = b ? Boolean.TRUE : Boolean.FALSE;
      }

      public Param(BInteger i) {
         this.typ = 9;
         this.obj = i;
      }

      public Param(double d) {
         this.typ = 4;
         this.obj = d;
      }

      public Param(byte[] b) {
         this.typ = 5;
         this.obj = BBacnetOctetString.make(b);
      }

      public Param(String s) {
         this.typ = 10;
         this.obj = s;
      }

      public Param(BBacnetBitString b) {
         this.typ = 6;
         this.obj = b;
      }

      public Param(int ord) {
         this.typ = 7;
         this.obj = ord;
      }

      public Param(BBacnetDate date) {
         this.typ = 11;
         this.obj = date;
      }

      public Param(BBacnetTime time) {
         this.typ = 12;
         this.obj = time;
      }

      public Param(BBacnetObjectIdentifier objectId) {
         this.typ = 13;
         this.obj = objectId;
      }

      public Param(BBacnetDeviceObjectPropertyReference r) {
         this.typ = 8;
         this.obj = r;
      }

      public Object getObj() {
         return this.obj;
      }

      @Override
      public String toString() {
         return String.valueOf(this.obj);
      }
   }
}
