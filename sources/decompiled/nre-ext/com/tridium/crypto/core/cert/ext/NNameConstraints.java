package com.tridium.crypto.core.cert.ext;

import com.tridium.crypto.core.cert.NGeneralName;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.NameConstraints;

public final class NNameConstraints extends NX509Extension {
   private final NameConstraints nameConstraints;

   NNameConstraints(Extension extension) {
      super(extension);
      if (!extension.getExtnId().equals(Extension.nameConstraints)) {
         throw new IllegalArgumentException("extension is not a NameConstraints extension");
      }

      this.nameConstraints = NameConstraints.getInstance(extension.getParsedValue());
      if (this.nameConstraints == null) {
         throw new IllegalArgumentException("extension is not a NameConstraints extension");
      }
   }

   public Set<GeneralSubtree> getExcludedSubtrees() {
      Set<GeneralSubtree> subtreeSet = new HashSet<>(Arrays.asList(this.nameConstraints.getExcludedSubtrees()));
      return Collections.unmodifiableSet(subtreeSet);
   }

   public Set<GeneralSubtree> getPermittedSubtrees() {
      Set<GeneralSubtree> subtreeSet = new HashSet<>(Arrays.asList(this.nameConstraints.getPermittedSubtrees()));
      return Collections.unmodifiableSet(subtreeSet);
   }

   @Override
   protected void appendJSON(JSONObject obj) {
      GeneralSubtree[] permittedSubtrees = this.nameConstraints.getPermittedSubtrees();
      if (permittedSubtrees != null) {
         JSONArray permittedArr = new JSONArray();

         for (GeneralSubtree permittedSubtree : permittedSubtrees) {
            appendSubtree(permittedArr, permittedSubtree);
         }

         obj.put("permittedSubtrees", permittedArr);
      }

      GeneralSubtree[] excludedSubtrees = this.nameConstraints.getExcludedSubtrees();
      if (excludedSubtrees != null) {
         JSONArray excludedArr = new JSONArray();

         for (GeneralSubtree excludedSubtree : excludedSubtrees) {
            appendSubtree(excludedArr, excludedSubtree);
         }

         obj.put("excludedSubtrees", excludedArr);
      }
   }

   private static void appendSubtree(JSONArray arr, GeneralSubtree subtree) {
      JSONObject obj = new JSONObject();
      NGeneralName name = NGeneralName.make(subtree.getBase());
      obj.put("base", name.getJSON());
      obj.putOpt("minimum", subtree.getMinimum());
      obj.putOpt("maximum", subtree.getMaximum());
      arr.put(obj);
   }
}
