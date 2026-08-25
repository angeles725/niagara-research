package javax.baja.nre.security;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.Extension;

public interface IX509Extension {
   String getIdentifier();

   ASN1ObjectIdentifier getOid();

   boolean isCritical();

   Extension getExtension();

   String getJSON();

   String encodeToString();
}
