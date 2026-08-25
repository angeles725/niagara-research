package javax.baja.nre.security;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface IX509CertificateEntry {
   IX509Certificate getCertificate(int var1);

   X509Certificate[] getCertificates();

   String getAlias();

   PrivateKey getPrivateKey();

   String encodeToString() throws Exception;
}
