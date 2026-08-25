package javax.baja.nre.security;

import com.tridium.crypto.core.cert.ext.NBasicConstraints;
import com.tridium.crypto.core.cert.ext.NExtendedKeyUsage;
import com.tridium.crypto.core.cert.ext.NKeyUsage;
import com.tridium.crypto.core.cert.ext.NSubjectAlternativeName;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;

public interface IX509Certificate {
   X509Certificate getCertificate();

   int getVersion();

   BigInteger getSerialNumber();

   X500Name getIssuerDN();

   X500Name getSubjectDN();

   Date getNotBefore();

   Date getNotAfter();

   PublicKey getPublicKey();

   String getKeyAlgorithm();

   int getKeySize();

   String getSignatureAlgorithm();

   int getSignatureSize();

   String getBasicConstraints();

   String getKeyUsage();

   int getKeyUsageValue();

   String getExtendedKeyUsage();

   String getMD5Fingerprint();

   String getSHA1Fingerprint();

   byte[] getPublicKeyHash();

   boolean checkValidity();

   String getIssuer();

   String getSubject();

   boolean isSelfSigned();

   IX509Extension[] getExtensions() throws Exception;

   IX509Extension getExtension(ASN1ObjectIdentifier var1);

   NBasicConstraints getBasicConstraintsExtension();

   NKeyUsage getKeyUsageExtension();

   NExtendedKeyUsage getExtendedKeyUsageExtension();

   NSubjectAlternativeName getSubjectAlternativeNameExtension();

   String getJSON();

   String encodeToString() throws Exception;

   String getASN1String();
}
