package com.tridium.fox.message;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class FoxTime extends FoxTuple {
   private static DateFormat format = new SimpleDateFormat("HH:mm:ss dd-MMM-yy");
   public long millis;

   public FoxTime(String name, long millis) {
      this.name = name;
      this.millis = millis;
   }

   public FoxTime() {
   }

   @Override
   public final int getType() {
      return 116;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      out.writeHexLong(this.millis);
      if (out.isDebug) {
         out.writeSafe(" // ").writeSafe(format.format(new Date(this.millis)));
      }
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      this.millis = in.readHexLong();
   }
}
