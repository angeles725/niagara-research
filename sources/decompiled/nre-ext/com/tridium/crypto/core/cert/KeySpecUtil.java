package com.tridium.crypto.core.cert;

import com.tridium.nre.security.SecurityInitializer;
import java.lang.reflect.Method;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.crypto.asymmetric.ECDomainParameters;
import org.bouncycastle.crypto.asymmetric.NamedECDomainParameters;
import org.bouncycastle.jcajce.spec.ECDomainParameterSpec;

public class KeySpecUtil {
   private static final Logger log = Logger.getLogger("crypto.keySpecUtil");

   public static String getSpec(PublicKey publicKey) {
      if (!(publicKey instanceof ECPublicKey)) {
         return "";
      }

      ECPublicKey ecPublicKey = (ECPublicKey)publicKey;
      ECParameterSpec ecParameterSpec = ecPublicKey.getParams();
      if (SecurityInitializer.getInstance().isFips()) {
         try {
            if (ecParameterSpec instanceof ECDomainParameterSpec) {
               ECDomainParameterSpec ecDomainParameterSpec = (ECDomainParameterSpec)ecParameterSpec;
               ECDomainParameters domainParameters = ecDomainParameterSpec.getDomainParameters();
               if (domainParameters instanceof NamedECDomainParameters) {
                  NamedECDomainParameters namedECDomainParameters = (NamedECDomainParameters)domainParameters;
                  return namedECDomainParameters.getID().getId();
               }
            }
         } catch (NoClassDefFoundError | Exception e) {
            log.log(Level.FINE, "ECDomainParameterSpec.class unavailable", e);
         }
      } else {
         try {
            Class<?> specClass = Class.forName("org.bouncycastle.jce.spec.ECNamedCurveSpec");
            if (specClass.isAssignableFrom(ecParameterSpec.getClass())) {
               Method getNameMethod = specClass.getMethod("getName");
               return getNameMethod.invoke(ecParameterSpec).toString();
            }
         } catch (NoClassDefFoundError | Exception e) {
            log.log(Level.FINE, "ECNamedCurveSpec.class unavailable", e);
         }
      }

      return "";
   }
}
