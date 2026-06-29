package com.tridium.fox.message;

import java.io.IOException;

public final class FoxInteger extends FoxTuple {
   public int value;

   public FoxInteger(String name, int value) {
      this.name = name;
      this.value = value;
   }

   public FoxInteger() {
   }

   @Override
   public final int getType() {
      return 105;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      out.writeInt(this.value);
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      this.value = in.readInt();
   }
}
