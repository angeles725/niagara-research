package javax.baja.nre.security;

import com.tridium.crypto.core.io.CryptoSupport;
import com.tridium.nre.security.SecretChars;

public abstract class TlsParameters {
   protected String certAlias = null;
   protected TlsCipherSuiteGroup group = TlsCipherSuiteGroup.recommended;
   protected String minTlsProtocol = "tlsv1";
   protected SecretChars keyPassphrase = null;

   public String getCertAlias() {
      return this.certAlias;
   }

   protected void setCertAlias(String certAlias) {
      this.certAlias = certAlias;
   }

   public TlsCipherSuiteGroup getTlsCipherSuiteGroup() {
      return this.group;
   }

   protected void setTlsCipherSuiteGroup(TlsCipherSuiteGroup group) {
      if (group == null) {
         this.group = TlsCipherSuiteGroup.recommended;
      } else {
         this.group = group;
      }
   }

   public String getMinTlsProtocol() {
      return this.minTlsProtocol;
   }

   protected void setMinTlsProtocol(String minTlsProtocol) throws IllegalArgumentException {
      if (minTlsProtocol == null) {
         this.minTlsProtocol = "tlsv1.2";
      } else {
         if (!CryptoSupport.TYPES.keySet().contains(minTlsProtocol.toLowerCase())) {
            throw new IllegalArgumentException("unsupported TLS protocol: " + minTlsProtocol);
         }

         this.minTlsProtocol = minTlsProtocol;
      }
   }

   public char[] getKeyPassphrase() {
      return this.keyPassphrase == null ? null : this.keyPassphrase.newCopy().get();
   }

   public void setKeyPassphrase(String passphrase) {
      this.keyPassphrase = SecretChars.fromString(passphrase);
   }

   public void setKeyPassphrase(char[] passphrase) {
      if (passphrase == null) {
         this.keyPassphrase = null;
      } else {
         this.keyPassphrase = new SecretChars(passphrase, false);
      }
   }
}
