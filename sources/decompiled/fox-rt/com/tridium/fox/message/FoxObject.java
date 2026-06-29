package com.tridium.fox.message;

import java.io.IOException;

public class FoxObject extends FoxTuple {
   public String encoding;
   public byte[] data;
   public int length;

   public FoxObject(String name, String encoding, byte[] data, int length) {
      this.name = name;
      this.encoding = encoding;
      this.data = data;
      this.length = length;
   }

   public FoxObject() {
   }

   @Override
   public final int getType() {
      return 111;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      out.writeName(this.encoding).write(32).writeInt(this.length).write(91).write(this.data, 0, this.length).write(93);
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      this.encoding = in.readName();
      in.consume(32);
      this.length = in.readInt();
      in.consume(91);
      this.data = in.readFully(this.length);
      in.consume(93);
   }
}
