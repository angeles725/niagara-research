package com.tridium.fox.message;

import java.io.IOException;
import java.io.OutputStream;

public abstract class FoxTuple {
   public String name;

   public abstract int getType();

   public final void write(MessageWriter out) throws IOException {
      out.writeName(this.name).write(61).write(this.getType()).write(58);
      this.writeValue(out);
   }

   protected abstract void writeValue(MessageWriter var1) throws IOException;

   public static FoxTuple read(MessageReader in) throws IOException {
      String name = in.readName();
      in.consume(61);
      int type = in.read();
      FoxTuple tuple = null;
      switch (type) {
         case 98:
            tuple = new FoxBlob();
         case 99:
         case 100:
         case 101:
         case 103:
         case 104:
         case 106:
         case 107:
         case 108:
         case 110:
         case 112:
         case 113:
         case 114:
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
         default:
            break;
         case 102:
            tuple = new FoxFloat();
            break;
         case 105:
            tuple = new FoxInteger();
            break;
         case 109:
            tuple = new FoxMessage();
            break;
         case 111:
            tuple = new FoxObject();
            break;
         case 115:
            tuple = new FoxString();
            break;
         case 116:
            tuple = new FoxTime();
            break;
         case 122:
            tuple = new FoxBoolean();
      }

      in.consume(58);
      tuple.name = name.intern();
      tuple.readValue(in);
      in.consume(10);
      return tuple;
   }

   protected void readValue(MessageReader in) throws IOException {
      throw in.error("not implemented");
   }

   public void dump(OutputStream out) {
      try {
         MessageWriter msgOut = new MessageWriter(out, true);
         this.write(msgOut);
         msgOut.flush();
      } catch (IOException var3) {
         var3.printStackTrace();
         throw new RuntimeException(var3.toString());
      }
   }

   public void dump() {
      this.dump(System.out);
   }
}
