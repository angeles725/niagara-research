package com.honeywell.easybinding.util;

public class FileByteData {
   private byte[] a;
   private boolean b;

   public FileByteData() {
      this.a = new byte[0];
      this.b = false;
   }

   public FileByteData(byte[] var1, boolean var2) {
      this.a = var1;
      this.b = var2;
   }

   public byte[] getFileBytes() {
      return this.a;
   }

   public boolean getIsEncrypted() {
      return this.b;
   }

   public void setEncrypted(boolean var1) {
      this.b = var1;
   }

   public void setFileBytes(byte[] var1) {
      this.a = var1;
   }
}
