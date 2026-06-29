package com.tridium.fox.message;

import java.io.IOException;

public final class FoxFloat extends FoxTuple {
   public double value;

   public FoxFloat(String name, double value) {
      this.name = name;
      this.value = value;
   }

   public FoxFloat() {
   }

   @Override
   public final int getType() {
      return 102;
   }

   @Override
   protected final void writeValue(MessageWriter out) throws IOException {
      if (Double.isNaN(this.value)) {
         out.writeSafe("nan");
      } else if (this.value == Double.POSITIVE_INFINITY) {
         out.writeSafe("+inf");
      } else if (this.value == Double.NEGATIVE_INFINITY) {
         out.writeSafe("-inf");
      } else {
         out.writeSafe(String.valueOf(this.value));
      }
   }

   @Override
   protected void readValue(MessageReader in) throws IOException {
      String s = in.readSafe();
      if (s.equals("nan")) {
         this.value = Double.NaN;
      } else if (s.equals("+inf")) {
         this.value = Double.POSITIVE_INFINITY;
      } else if (s.equals("-inf")) {
         this.value = Double.NEGATIVE_INFINITY;
      } else {
         this.value = Double.parseDouble(s);
      }
   }
}
