package com.prosysopc.ua.stack.encoding.json;

import com.prosysopc.ua.stack.encoding.DecodingException;
import java.util.ArrayList;
import java.util.Locale;

class a {
   private final Object value;
   private boolean tN = false;

   public a() {
      this.value = null;
   }

   public a(ArrayList<a> var1) {
      this.value = var1;
   }

   public a(b var1) {
      this.value = var1;
   }

   public a(String var1) {
      this(var1, false);
   }

   public a(String var1, boolean var2) {
      this.value = var1;
      this.tN = var2;
   }

   public ArrayList<a> esw() throws DecodingException {
      if (this.esy()) {
         return (ArrayList<a>)this.value;
      } else {
         throw new DecodingException("JsonElement is not Json array");
      }
   }

   public b esx() throws DecodingException {
      if (this.esz()) {
         return (b)this.value;
      } else {
         throw new DecodingException("JsonElement is not Json object");
      }
   }

   public String getString() throws DecodingException {
      if (this.esA()) {
         return (String)this.value;
      } else {
         throw new DecodingException("JsonElement is not string");
      }
   }

   public Object getValue() {
      return this.value;
   }

   public boolean isBoolean() {
      return this.esB() && ("true".equals(this.value) || "false".equals(this.value));
   }

   public boolean esy() {
      return this.value instanceof ArrayList;
   }

   public boolean esz() {
      return this.value instanceof b;
   }

   public boolean isNull() {
      return this.value == null;
   }

   public boolean esA() {
      return this.value instanceof String;
   }

   public boolean esB() {
      return this.esA() && this.tN;
   }

   public String esC() throws DecodingException {
      if (this.isNull()) {
         return "null";
      } else if (this.esy()) {
         StringBuilder var1 = new StringBuilder();
         var1.append("[");
         ArrayList var2 = this.esw();
         Boolean var3 = false;

         for (a var5 : var2) {
            if (var3) {
               var1.append(", ");
            }

            var1.append(var5.esC());
            var3 = true;
         }

         var1.append("]");
         return var1.toString();
      } else if (this.esz()) {
         return ((b)this.value).esC();
      } else {
         return this.esB() ? this.value.toString() : String.format(Locale.ROOT, "\"%s\"", this.value.toString());
      }
   }
}
