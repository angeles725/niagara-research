package com.tridium.niagarad.http;

import com.tridium.niagarad.util.KeyedList;
import java.io.PrintWriter;
import javax.baja.nre.util.TextUtil;

public class HeaderField {
   String name;
   String value;
   KeyedList attrs = null;

   public HeaderField(String name, String value) {
      this.attrs = new KeyedList();
      this.name = name;
      this.value = value;
   }

   public HeaderField(String full) {
      this.attrs = new KeyedList();
      this.name = this.value = null;
      int colonIndex;
      if ((colonIndex = full.indexOf(58)) == -1) {
         this.name = this.value = null;
      } else {
         this.name = full.substring(0, colonIndex);
         String remaining = full.substring(colonIndex + 1).trim();
         if (remaining.length() == 0) {
            this.name = this.value = null;
         } else {
            int spaceIndex;
            if ((spaceIndex = remaining.indexOf(32)) == -1) {
               this.value = remaining;
            } else {
               this.value = remaining.substring(0, spaceIndex);
               remaining = remaining.substring(spaceIndex + 1).trim();
               if (remaining.length() != 0) {
                  String[] attributes = TextUtil.split(remaining, ',');

                  for (String attribute : attributes) {
                     String currentAttribute = attribute.trim();
                     if (currentAttribute.length() >= 2 && currentAttribute.charAt(0) == '"' && currentAttribute.charAt(currentAttribute.length() - 1) == '"') {
                        currentAttribute = currentAttribute.substring(1, currentAttribute.length() - 1);
                     }

                     this.addAttribute(currentAttribute);
                  }
               }
            }
         }
      }
   }

   public void setValue(String newValue) {
      this.value = newValue;
   }

   public boolean write(PrintWriter out) {
      boolean first = true;
      out.write(this.name);
      out.write(": ");
      out.write(this.value);

      for (int i = 0; i < this.attrs.size(); i++) {
         String attrKey = this.attrs.getKey(i);
         String attrValue = this.attrs.getAtIndex(i);
         if (first) {
            first = false;
            out.write(" ");
         } else {
            out.write(", ");
         }

         out.write(attrKey);
         if (attrValue != null && attrValue.length() > 0) {
            out.write("=");
            out.write(attrValue);
         }
      }

      out.write("\r\n");
      return true;
   }

   public void addAttribute(String full) {
      int len = full.length();
      if (len != 0) {
         if (full.charAt(len - 1) == '=') {
            this.attrs.add(String.valueOf(this.attrs.size()), full);
         } else {
            int equalsIndex;
            if ((equalsIndex = full.indexOf(61)) == -1) {
               this.attrs.add(String.valueOf(this.attrs.size()), full);
            } else {
               String key = full.substring(0, equalsIndex);
               String value = full.substring(equalsIndex + 1, full.length());
               this.attrs.add(key, value);
            }
         }
      }
   }
}
