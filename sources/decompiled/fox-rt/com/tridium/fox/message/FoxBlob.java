package com.tridium.fox.message;

import java.io.IOException;

public final class FoxBlob extends FoxTuple {
   public byte[] data;
   public int offset;
   public int length;

   public FoxBlob(String name, byte[] data, int length) {
      this.name = name;
      this.data = data;
      this.offset = 0;
      this.length = length;
   }

   public FoxBlob(String name, byte[] data, int offset, int length) {
      this.name = name;
      this.data = data;
      this.offset = offset;
      this.length = length;
   }

   public FoxBlob(String name, byte[] data) {
      this.name = name;
      this.data = data;
      this.offset = 0;
      this.length = data.length;
   }

   public FoxBlob() {
   }

   @Override
   public final int getType() {
      return 98;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      out.writeInt(this.length).write(91).write(this.data, this.offset, this.length).write(93);
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      this.length = in.readInt();
      in.consume(91);
      this.data = in.readFully(this.length);
      in.consume(93);
   }
}
