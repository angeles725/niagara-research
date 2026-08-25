package com.tridium.crypto.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.x509.GeneralName;

public final class BouncyCastleHelper {
   private static final Logger LOG = Logger.getLogger("bchelper");

   private BouncyCastleHelper() {
   }

   public static ASN1Primitive getASN1TaggedObjectPrimitive(ASN1TaggedObject taggedObject) {
      return AccessController.doPrivileged(() -> {
         try {
            Class<?> asn1TaggedObjectClass = Class.forName("org.bouncycastle.asn1.ASN1TaggedObject");
            Field objField = asn1TaggedObjectClass.getDeclaredField("obj");
            ASN1Primitive asn1Primitive;
            if (!objField.isAccessible()) {
               objField.setAccessible(true);
               asn1Primitive = (ASN1Primitive)objField.get(taggedObject);
               objField.setAccessible(false);
            } else {
               asn1Primitive = (ASN1Primitive)objField.get(taggedObject);
            }

            return asn1Primitive;
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      });
   }

   public static String getASN1TaggedObjectValue(ASN1TaggedObject taggedObject) {
      ASN1Primitive asn1Primitive = getASN1TaggedObjectPrimitive(taggedObject);
      return asn1Primitive.toString();
   }

   public static String getIA5StringValue(GeneralName generalName) {
      return AccessController.doPrivileged(() -> {
         try {
            Class<?> asn1Class = Class.forName("org.bouncycastle.asn1.ASN1IA5String");
            Method getInstanceMethod = asn1Class.getMethod("getInstance", Object.class);
            return getInstanceMethod.invoke(asn1Class, generalName.getName()).toString();
         } catch (Exception asn1Exception) {
            try {
               Class<?> derClass = Class.forName("org.bouncycastle.asn1.DERIA5String");
               Method getInstanceMethod = derClass.getMethod("getInstance", Object.class);
               return getInstanceMethod.invoke(derClass, generalName.getName()).toString();
            } catch (Exception derException) {
               LOG.log(Level.SEVERE, "Unable to load an appropriate IA5String object.");
               throw new RuntimeException("unable to load an appropriate IA5String object");
            }
         }
      });
   }
}
