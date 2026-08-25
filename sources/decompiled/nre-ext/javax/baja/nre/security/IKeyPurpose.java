package javax.baja.nre.security;

import com.tridium.crypto.core.cert.KeyPurpose;

public interface IKeyPurpose {
   IKeyPurpose CLIENT_CERT = KeyPurpose.CLIENT_CERT;
   IKeyPurpose SERVER_CERT = KeyPurpose.SERVER_CERT;
   IKeyPurpose SIGNING_CERT = KeyPurpose.CODE_SIGNING_CERT;
   IKeyPurpose CA_CERT = KeyPurpose.CA_CERT;

   int getValue();
}
