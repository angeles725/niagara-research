package com.tridium.fox.message;

import java.io.IOException;

public final class FoxString extends FoxTuple {
   public String value;

   public FoxString(String name, String value) {
      this.name = name;
      this.value = value;
   }

   public FoxString() {
   }

   @Override
   public final int getType() {
      return 115;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      out.writeSafe(this.value);
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      this.value = in.readSafe();
   }
}
