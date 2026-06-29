package com.tridium.fox.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class FoxMessage extends FoxTuple {
   public static final FoxTuple[] noTuples = new FoxTuple[0];
   public FoxTuple[] tuples = noTuples;
   public int count;

   public FoxMessage(String name) {
      this.name = name;
   }

   public FoxMessage() {
   }

   public FoxMessage(FoxMessage messsage) {
      this.name = messsage.name;
      this.count = messsage.count;
      this.tuples = Arrays.copyOf(messsage.tuples, messsage.tuples.length);
   }

   @Override
   public final int getType() {
      return 109;
   }

   @Override
   public final void writeValue(MessageWriter out) throws IOException {
      boolean isDebug = out.isDebug;
      out.write(123).write(10);
      out.indent++;

      for (int i = 0; i < this.count; i++) {
         if (isDebug) {
            out.writeIndent();
         }

         this.tuples[i].write(out);
         out.write(10);
      }

      out.indent--;
      if (isDebug) {
         out.writeIndent();
      }

      out.write(125);
   }

   @Override
   public void readValue(MessageReader in) throws IOException {
      in.consume(123);
      in.consume(10);

      while (true) {
         int c = in.read();
         if (c == 125) {
            return;
         }

         in.pushBack(c);
         this.add(FoxTuple.read(in));
      }
   }

   public final FoxTuple get(String key) throws IOException {
      for (int i = 0; i < this.count; i++) {
         if (key == this.tuples[i].name) {
            return this.tuples[i];
         }
      }

      throw new IOException("Missing key \"" + key + "\"");
   }

   public final FoxTuple[] list(String key) {
      Vector<FoxTuple> list = new Vector<>();

      for (int i = 0; i < this.count; i++) {
         if (key == this.tuples[i].name) {
            list.addElement(this.tuples[i]);
         }
      }

      FoxTuple[] tuples = new FoxTuple[list.size()];
      list.copyInto(tuples);
      return tuples;
   }

   public final String[] listStrings(String key) {
      Vector<String> list = new Vector<>();

      for (int i = 0; i < this.count; i++) {
         if (key == this.tuples[i].name) {
            list.addElement(((FoxString)this.tuples[i]).value);
         }
      }

      String[] tuples = new String[list.size()];
      list.copyInto(tuples);
      return tuples;
   }

   public final FoxTuple getOptional(String key) {
      for (int i = 0; i < this.count; i++) {
         if (key == this.tuples[i].name) {
            return this.tuples[i];
         }
      }

      return null;
   }

   public final FoxMessage getMessage(String key) throws IOException {
      return (FoxMessage)this.get(key);
   }

   public final boolean getBoolean(String key) throws IOException {
      return ((FoxBoolean)this.get(key)).value;
   }

   public final int getInt(String key) throws IOException {
      return ((FoxInteger)this.get(key)).value;
   }

   public final double getFloat(String key) throws IOException {
      return ((FoxFloat)this.get(key)).value;
   }

   public final String getString(String key) throws IOException {
      return ((FoxString)this.get(key)).value;
   }

   public final long getTime(String key) throws IOException {
      return ((FoxTime)this.get(key)).millis;
   }

   public final byte[] getBlob(String key) throws IOException {
      return ((FoxBlob)this.get(key)).data;
   }

   public final boolean getBoolean(String key, boolean def) {
      FoxBoolean x = (FoxBoolean)this.getOptional(key);
      return x == null ? def : x.value;
   }

   public final int getInt(String key, int def) {
      FoxInteger x = (FoxInteger)this.getOptional(key);
      return x == null ? def : x.value;
   }

   public final double getFloat(String key, double def) {
      FoxFloat x = (FoxFloat)this.getOptional(key);
      return x == null ? def : x.value;
   }

   public final String getString(String key, String def) {
      FoxString x = (FoxString)this.getOptional(key);
      return x == null ? def : x.value;
   }

   public final long getTime(String key, long def) {
      FoxTime x = (FoxTime)this.getOptional(key);
      return x == null ? def : x.millis;
   }

   public final byte[] getBlob(String key, byte[] def) {
      FoxBlob x = (FoxBlob)this.getOptional(key);
      return x == null ? def : x.data;
   }

   public final void add(FoxTuple tuple) {
      if (this.count >= this.tuples.length) {
         if (this.count == 0) {
            this.tuples = new FoxTuple[8];
         } else {
            FoxTuple[] temp = new FoxTuple[this.count * 2];
            System.arraycopy(this.tuples, 0, temp, 0, this.count);
            this.tuples = temp;
         }
      }

      this.tuples[this.count++] = tuple;
   }

   public final void add(String name, boolean v) {
      this.add(new FoxBoolean(name, v));
   }

   public final void add(String name, int v) {
      this.add(new FoxInteger(name, v));
   }

   public final void add(String name, double v) {
      this.add(new FoxFloat(name, v));
   }

   public final void add(String name, String v) {
      this.add(new FoxString(name, v));
   }

   public final void add(String name, long millis) {
      this.add(new FoxTime(name, millis));
   }

   public final void add(String name, byte[] blob) {
      this.add(new FoxBlob(name, blob));
   }

   public final void add(String name, byte[] blob, int length) {
      this.add(new FoxBlob(name, blob, length));
   }

   public final void add(String name, byte[] blob, int offset, int length) {
      this.add(new FoxBlob(name, blob, offset, length));
   }

   public final void add(String name, String encoding, byte[] object, int length) {
      this.add(new FoxObject(name, encoding, object, length));
   }

   public final FoxMessage add(String name, FoxMessage msg) {
      msg.name = name;
      this.add(msg);
      return msg;
   }

   public final void remove(String key) {
      ArrayList<FoxTuple> list = new ArrayList<>(Arrays.asList(this.tuples));
      boolean anyRemoved = list.removeIf(t -> {
         boolean result = t != null && key == t.name;
         if (result) {
            this.count--;
         }

         return result;
      });
      if (anyRemoved) {
         this.tuples = list.toArray(new FoxTuple[0]);
      }
   }
}
