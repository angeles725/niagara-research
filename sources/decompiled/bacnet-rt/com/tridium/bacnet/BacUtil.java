package com.tridium.bacnet;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import java.io.IOException;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.util.BEnumSet;

public class BacUtil {
   public static final ErrorType setObjectName(BComponent component, Property objectName, byte[] val) throws AsnException {
      String name = AsnUtil.fromAsnCharacterString(val);
      if (name != null && name.length() != 0) {
         component.setString(objectName, name, BLocalBacnetDevice.getBacnetContext());
         return null;
      } else {
         return new NErrorType(2, 37);
      }
   }

   public static Property setOrAdd(BComponent c, String n, BValue v, Context cx) {
      return setOrAdd(c, n, v, 0, null, cx);
   }

   public static Property setOrAdd(BComponent c, String name, BValue value, int flags, BFacets facets, Context context) {
      Property p = c.getProperty(name);
      if (p != null) {
         if (value.isComplex()) {
            c.get(p).asComplex().copyFrom(value.asComplex(), context);
         } else {
            c.set(name, value, context);
         }
      } else {
         p = c.add(name, value, flags, facets, context);
      }

      return p;
   }

   public static void set(BComponent c, Property p, BValue value, Context context) {
      if (value.isComplex()) {
         c.get(p).asComplex().copyFrom(value.asComplex(), context);
      } else {
         c.set(p, value, context);
      }
   }

   public static BFacets makeBacnetNotifyTypeFacets() {
      BString enumSet = BString.make("");

      try {
         enumSet = BString.make(BEnumSet.make(new int[]{0, 1}).encodeToString());
      } catch (IOException var2) {
      }

      return BFacets.make("fieldEditor", BString.make("bacnet:BacnetNotifyTypeFE"), "uxValidOptions", enumSet);
   }
}
