package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.BacnetInputStream;
import java.io.ByteArrayOutputStream;

public abstract class BvllMessage implements BvllConst {
   protected int function;
   protected int len;

   BvllMessage(int function) {
      this.function = function;
   }

   BvllMessage(int function, int len) {
      this.function = function;
      this.len = len;
   }

   BvllMessage() {
   }

   public int getFunction() {
      return this.function;
   }

   public int getLength() {
      return this.len;
   }

   public void setLength(int len) {
      this.len = len;
   }

   public abstract byte[] encode(ByteArrayOutputStream var1);

   public abstract void decode(BacnetInputStream var1);
}
