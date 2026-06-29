package com.tridium.bacnet.stack;

import java.io.ByteArrayInputStream;

public class BacnetInputStream extends ByteArrayInputStream {
   public static final int STREAM_POOL_SIZE = 20;
   private boolean freeBuf = false;
   private static int cnt = 0;
   public BacnetInputStream next = null;
   private static BacnetInputStream[] pool = new BacnetInputStream[20];

   private BacnetInputStream() {
      super(new byte[0]);
   }

   public BacnetInputStream(byte[] buf, int offset, int length) {
      super(buf, offset, length);
   }

   public void setBuffer(byte[] buffer) {
      this.buf = buffer;
      this.pos = this.mark = 0;
      this.count = this.buf.length;
   }

   @Override
   public int read(byte[] array) {
      return this.read(array, 0, array.length);
   }

   public int getPos() {
      return this.pos;
   }

   public void setPos(int newPos) {
      this.pos = this.mark = newPos;
   }

   public BacnetInputStream copy() {
      byte[] clone = (byte[])this.buf.clone();
      return new BacnetInputStream(clone, 0, clone.length);
   }

   public static BacnetInputStream make() {
      synchronized (pool) {
         BacnetInputStream strm;
         if (cnt > 0 && cnt <= 20) {
            strm = pool[--cnt];
         } else {
            strm = new BacnetInputStream();
         }

         strm.freeBuf = false;
         return strm;
      }
   }

   public static BacnetInputStream make(byte[] buf, int offset, int length) {
      synchronized (pool) {
         BacnetInputStream strm;
         if (cnt > 0 && cnt <= 20) {
            strm = pool[--cnt];
            strm.pos = offset;
            strm.mark = offset;
            strm.buf = buf;
            strm.count = length;
         } else {
            strm = new BacnetInputStream(buf, offset, length);
         }

         strm.freeBuf = false;
         return strm;
      }
   }

   public void release() {
      synchronized (pool) {
         BacnetInputStream strm = this;

         while (strm != null) {
            if (!strm.freeBuf) {
               if (cnt < pool.length) {
                  pool[cnt++] = strm;
               }

               strm.freeBuf = true;
               strm = strm.next;
            }
         }
      }
   }
}
