package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONObject;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectDirectoryAttributes;

public final class NSubjectDirectoryAttributes extends NX509Extension {
   private final SubjectDirectoryAttributes subjectDirectoryAttributes;

   NSubjectDirectoryAttributes(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.subjectDirectoryAttributes)) {
         throw new IllegalArgumentException("extension is not a SubjectDirectoryAttributes extension");
      }

      this.subjectDirectoryAttributes = SubjectDirectoryAttributes.getInstance(extension.getParsedValue());
      if (this.subjectDirectoryAttributes == null) {
         throw new IllegalArgumentException("extension is not an SubjectDirectoryAttributes extension");
      }
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      for (Object attribute : this.subjectDirectoryAttributes.getAttributes()) {
         JSONObject attrObj = new JSONObject();
         Attribute asn1Attribute = (Attribute)attribute;
         attrObj.put("oid", OidMap.get(asn1Attribute.getAttrType()));
         this.parsePrimitive(asn1Attribute.getAttrValues().toASN1Primitive(), attrObj);
         obj.append("attributes", attrObj);
      }
   }
}
