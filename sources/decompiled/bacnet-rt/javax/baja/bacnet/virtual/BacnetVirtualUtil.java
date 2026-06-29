package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.virtual.BVirtualComponent;

public class BacnetVirtualUtil implements BacnetConst {
   private static int DATE_TIME_TAG = 1;

   static BValue readValue(AsnInputStream asnIn, BValue val) throws AsnException {
      synchronized (asnIn) {
         BValue v = val;
         if (val instanceof BIBacnetDataType) {
            BIBacnetDataType obj = (BIBacnetDataType)val;
            obj.readAsn(asnIn);
         } else {
            int tag = asnIn.peekApplicationTag();
            switch (tag) {
               case 0:
                  v = BBacnetNull.DEFAULT;
                  break;
               case 1:
                  v = BBoolean.make(asnIn.readBoolean());
                  break;
               case 2:
                  v = asnIn.readUnsigned();
                  break;
               case 3:
                  v = asnIn.readSigned();
                  break;
               case 4:
                  v = asnIn.readFloat();
                  break;
               case 5:
                  v = BDouble.make(asnIn.readDouble());
                  break;
               case 6:
                  v = BBacnetOctetString.make(asnIn.readOctetString());
                  break;
               case 7:
                  v = BString.make(asnIn.readCharacterString());
                  break;
               case 8:
                  v = asnIn.readBitString();
                  break;
               case 9:
                  if (val instanceof BEnum) {
                     v = ((BEnum)val).getRange().get(asnIn.readEnumerated());
                  } else {
                     v = BInteger.make(asnIn.readEnumerated());
                  }
                  break;
               case 10:
                  v = asnIn.readDate();
                  break;
               case 11:
                  v = asnIn.readTime();
                  break;
               case 12:
                  v = asnIn.readObjectIdentifier();
               case 13:
               case 14:
               case 15:
                  break;
               default:
                  if (asnIn.isOpeningTag(DATE_TIME_TAG)) {
                     asnIn.skipOpeningTag(DATE_TIME_TAG);
                     BBacnetDateTime dateTime = new BBacnetDateTime();
                     dateTime.readAsn(asnIn);
                     asnIn.skipClosingTag(DATE_TIME_TAG);
                     v = dateTime;
                  } else {
                     v = AsnUtil.asnToValue(asnIn, -1);
                  }
            }
         }

         return v;
      }
   }

   public static boolean isVirtual(BComponent c) {
      if (c.getComponentSpace() == null) {
         return false;
      } else {
         BComponent rc = c.getComponentSpace().getRootComponent();
         return rc instanceof BVirtualComponent;
      }
   }

   public static BBacnetVirtualProperty getVirtualProperty(BComponent c) {
      if (!isVirtual(c)) {
         return null;
      } else {
         for (BComplex p = c.getParent(); p != null; p = p.getParent()) {
            if (p instanceof BBacnetVirtualProperty) {
               return (BBacnetVirtualProperty)p;
            }
         }

         return null;
      }
   }
}
