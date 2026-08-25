package javax.baja.nre.util;

import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x500.X500Name;

public final class CertificateUtils {
   public static boolean checkDnEquality(X500Principal dn1, X500Principal dn2) {
      return dn1 != null && dn2 != null ? checkDnEquality(dn1.getName(), dn2.getName()) : dn1 == dn2;
   }

   public static boolean checkDnEquality(String dn1, String dn2) {
      return dn1 != null && dn2 != null ? new X500Name(dn1).equals(new X500Name(dn2)) : dn1 == dn2;
   }
}
