package com.tridium.crypto.core.cert.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.Extension;

public final class NSubjectInfoAccess extends NBaseInfoAccess {
   NSubjectInfoAccess(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.subjectInfoAccess)) {
         throw new IllegalArgumentException("extension is not an SubjectInfoAccess extension");
      }

      ASN1Sequence seq = ASN1Sequence.getInstance(extension.getParsedValue());
      if (seq != null && seq.size() >= 1) {
         List<AccessDescription> tdescs = new ArrayList<>();

         for (Object desc : seq) {
            tdescs.add(AccessDescription.getInstance(desc));
         }

         this.accessDescriptions = Collections.unmodifiableList(tdescs);
      } else {
         throw new IllegalArgumentException("extension is not an SubjectInfoAccess extension");
      }
   }
}
