package com.tridium.crypto.core.cert.ext;

import java.util.Arrays;
import java.util.Collections;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;

public final class NAuthorityInfoAccess extends NBaseInfoAccess {
   NAuthorityInfoAccess(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.authorityInfoAccess)) {
         throw new IllegalArgumentException("extension is not an AuthorityInfoAccess extension");
      }

      AuthorityInformationAccess authorityInformationAccess = AuthorityInformationAccess.fromExtensions(new Extensions(extension));
      if (authorityInformationAccess == null) {
         throw new IllegalArgumentException("extension is not an AuthorityInfoAccess extension");
      }

      this.accessDescriptions = Collections.unmodifiableList(Arrays.asList(authorityInformationAccess.getAccessDescriptions()));
   }
}
