package com.tridium.lonworks.util;

import java.util.NoSuchElementException;

public class ByteArrayTokenizer {
   private byte[] data;
   private byte token;
   private int offset = 0;

   public ByteArrayTokenizer(byte[] data, byte token) {
      this.data = data;
      this.token = token;
   }

   public ByteArrayTokenizer(byte[] data, char token) {
      this.data = data;
      this.token = (byte)token;
   }

   public int countTokens() {
      int cnt = 0;

      for (int off = this.offset; off < this.data.length; cnt++) {
         off = this.offsetToNextToken(off) + 1;
      }

      return cnt;
   }

   public boolean hasMoreTokens() {
      return this.offset < this.data.length;
   }

   public byte[] nextToken() {
      int end = this.offsetToNextToken(this.offset);
      if (!this.hasMoreTokens()) {
         throw new NoSuchElementException();
      } else {
         byte[] a = new byte[end - this.offset];
         System.arraycopy(this.data, this.offset, a, 0, end - this.offset);
         this.offset = end + 1;
         return a;
      }
   }

   private int offsetToNextToken(int begin) {
      if (begin >= this.data.length) {
         return this.data.length;
      } else {
         int i = begin;

         while (i < this.data.length && this.data[i] != this.token) {
            i++;
         }

         return i;
      }
   }
}
