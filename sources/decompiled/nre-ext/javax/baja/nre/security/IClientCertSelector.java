package javax.baja.nre.security;

import com.tridium.nre.security.SecretChars;

public interface IClientCertSelector {
   IX509CertificateEntry selectClientCertificate(String var1, SecretChars var2);

   default IX509CertificateEntry selectClientCertificate() {
      return this.selectClientCertificate(null, null);
   }
}
