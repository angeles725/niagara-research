package com.prosysopc.ua.stack.encoding.json;

import com.prosysopc.ua.stack.encoding.DecodingException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

class b {
   private final Map<String, a> map;

   public b(Map<String, a> var1) {
      this.map = var1;
   }

   public Set<String> getFieldNames() {
      return this.map.keySet();
   }

   public a M(String var1) {
      return var1 == null ? null : this.map.get(var1);
   }

   public String esC() throws DecodingException {
      StringBuilder var1 = new StringBuilder();
      var1.append("{");
      Boolean var2 = false;

      for (Entry var4 : this.map.entrySet()) {
         if (var2) {
            var1.append(", ");
         }

         var1.append('"');
         var1.append((String)var4.getKey());
         var1.append("\" : ");
         var1.append(((a)var4.getValue()).esC());
         var2 = true;
      }

      var1.append("}");
      return var1.toString();
   }
}
