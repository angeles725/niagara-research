package com.tridium.crypto.core.cert.ext;

import com.tridium.json.JSONObject;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.PolicyConstraints;

public final class NPolicyConstraints extends NX509Extension {
   private final PolicyConstraints policyConstraints;

   NPolicyConstraints(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.policyConstraints)) {
         throw new IllegalArgumentException("extension is not a PolicyConstraints extension");
      }

      this.policyConstraints = PolicyConstraints.fromExtensions(new Extensions(extension));
      if (this.policyConstraints == null) {
         throw new IllegalArgumentException("extension is not an PolicyConstraints extension");
      }
   }

   public int getInhibitPolicyMapping() {
      return this.policyConstraints.getInhibitPolicyMapping().intValue();
   }

   public int getRequireExplicitPolicyMapping() {
      return this.policyConstraints.getRequireExplicitPolicyMapping().intValue();
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      obj.put("inhibitPolicyMapping", this.getInhibitPolicyMapping());
      obj.put("requireExplicitPolicyMapping", this.getRequireExplicitPolicyMapping());
   }
}
