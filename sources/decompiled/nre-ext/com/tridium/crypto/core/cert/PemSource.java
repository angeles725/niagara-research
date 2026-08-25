package com.tridium.crypto.core.cert;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1String;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public abstract class PemSource {
   private static final Logger LOGGER = Logger.getLogger("crypto");

   protected static String getPEMString(Object obj) throws IOException {
      try (StringWriter sout = new StringWriter()) {
         JcaPEMWriter out = new JcaPEMWriter(sout);
         Throwable var4 = null;

         try {
            try {
               AccessController.doPrivileged(() -> {
                  out.writeObject(obj);
                  out.flush();
                  return null;
               });
            } catch (PrivilegedActionException e) {
               throw (IOException)e.getException();
            }
         } catch (Throwable var30) {
            var4 = var30;
            throw var30;
         } finally {
            if (out != null) {
               if (var4 != null) {
                  try {
                     out.close();
                  } catch (Throwable var28) {
                     var4.addSuppressed(var28);
                  }
               } else {
                  out.close();
               }
            }
         }

         sout.flush();
         return sout.toString();
      }
   }

   protected static Object getFromPEM(String pemString) throws IOException {
      try (StringReader sin = new StringReader(pemString)) {
         PEMParser in = new PEMParser(sin);
         Throwable var4 = null;

         try {
            return in.readObject();
         } catch (Throwable var29) {
            var4 = var29;
            throw var29;
         } finally {
            if (in != null) {
               if (var4 != null) {
                  try {
                     in.close();
                  } catch (Throwable var28) {
                     var4.addSuppressed(var28);
                  }
               } else {
                  in.close();
               }
            }
         }
      }
   }

   public static String extractFriendlyName(X500Name x500name) {
      try {
         RDN[] issuer = x500name.getRDNs(BCStyle.CN);
         if (issuer != null && issuer.length > 0) {
            return ((ASN1String)issuer[0].getFirst().getValue()).getString();
         }

         issuer = x500name.getRDNs(BCStyle.OU);
         if (issuer != null && issuer.length > 0) {
            return ((ASN1String)issuer[0].getFirst().getValue()).getString();
         }
      } catch (Exception e) {
         LOGGER.log(Level.SEVERE, "Exception while extracting friendly name", e);
      }

      return x500name.toString();
   }

   public static String extractFriendlyName(String dn) {
      try {
         X500Name x500name = new X500Name(dn);
         RDN[] issuer = x500name.getRDNs(BCStyle.CN);
         if (issuer != null && issuer.length > 0) {
            return IETFUtils.valueToString(issuer[0].getFirst().getValue());
         }

         issuer = x500name.getRDNs(BCStyle.OU);
         if (issuer != null && issuer.length > 0) {
            return IETFUtils.valueToString(issuer[0].getFirst().getValue());
         }
      } catch (Exception e) {
         LOGGER.log(Level.SEVERE, "Exception while extracting friendly name", e);
      }

      return dn;
   }

   public static String extractCommonName(X500Name x500name) {
      return extractOptionalCommonName(x500name).orElse(x500name.toString());
   }

   public static String extractCommonName(X500Principal principal) {
      X500Name x500name = new X500Name(principal.getName());
      return extractOptionalCommonName(x500name).orElse(null);
   }

   public static Optional<String> extractOptionalCommonName(X500Name x500name) {
      try {
         RDN[] subject = x500name.getRDNs(BCStyle.CN);
         if (subject != null && subject.length > 0) {
            return Optional.of(IETFUtils.valueToString(subject[0].getFirst().getValue()));
         }
      } catch (Exception var2) {
      }

      return Optional.empty();
   }

   public static int getPublicKeyLength(PublicKey key) {
      int length = -1;
      if (key instanceof RSAPublicKey) {
         length = ((RSAPublicKey)key).getModulus().bitLength();
      } else if (key instanceof DSAPublicKey) {
         length = ((DSAPublicKey)key).getParams().getP().bitLength();
      } else if (key instanceof ECPublicKey) {
         length = ((ECPublicKey)key).getParams().getOrder().bitLength();
      }

      return length;
   }
}
