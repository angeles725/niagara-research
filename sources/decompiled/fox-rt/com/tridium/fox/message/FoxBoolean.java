package com.tridium.fox.message;

import java.io.IOException;

public final class FoxBoolean extends FoxTuple {
   public boolean value;

   public FoxBoolean(String name, boolean value) {
      this.name = name;
      this.value = value;
   }

   public FoxBoolean() {
   }

   @Override
   public final int getType() {
      return 122;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      if (this.value) {
         out.write(116);
      } else {
         out.write(102);
      }
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      int c = in.read();
      if (c == 116) {
         this.value = true;
      } else {
         if (c != 102) {
            throw in.error("Expecting 't' or 'f', not '" + in.toString(c) + "'");
         }

         this.value = false;
      }
   }
}
