package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.crypto.core.util.OidMap;
import com.tridium.json.JSONObject;
import java.util.List;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.Extension;

public abstract class NBaseInfoAccess extends NX509Extension {
   protected List<AccessDescription> accessDescriptions = null;

   NBaseInfoAccess(Extension extension) {
      super(extension);
   }

   @Override
   protected final void appendJSON(JSONObject obj) {
      for (AccessDescription accessDescription : this.accessDescriptions) {
         JSONObject adObj = new JSONObject();
         adObj.put("accessMethod", OidMap.get(accessDescription.getAccessMethod()));
         NGeneralName name = NGeneralName.make(accessDescription.getAccessLocation());
         adObj.put("accessLocation", name.getJSON());
         obj.append("accessDescriptions", adObj);
      }
   }
}
