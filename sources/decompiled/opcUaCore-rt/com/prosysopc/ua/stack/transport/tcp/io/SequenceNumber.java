package com.prosysopc.ua.stack.transport.tcp.io;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class SequenceNumber {
   private AtomicInteger sendSequenceNumber = new AtomicInteger(1);
   private AtomicInteger recvSequenceNumber = null;

   public int getCurrentSendSequenceNumber() {
      return this.sendSequenceNumber.get();
   }

   public Integer getNextRecvSequenceNumber() {
      return this.recvSequenceNumber == null ? null : this.recvSequenceNumber.incrementAndGet();
   }

   public int getNextSendSequencenumber() {
      long var1 = this.sendSequenceNumber.get() & 4294967295L;
      boolean var3 = var1 == 4294967295L;
      boolean var4 = var1 >= 4294966271L;
      boolean var5 = var3 || var4 && ThreadLocalRandom.current().nextBoolean();
      long var6 = var1 + 1L;
      if (var5) {
         var6 = ThreadLocalRandom.current().nextInt(1024);
      }

      this.sendSequenceNumber.set((int)var6);
      return (int)var6;
   }

   public Integer getRecvSequenceNumber() {
      return this.recvSequenceNumber == null ? null : this.recvSequenceNumber.get();
   }

   public boolean hasRecvSequenceNumber() {
      return this.recvSequenceNumber != null;
   }

   public void setCurrentSendSequenceNumber(int var1) {
      this.sendSequenceNumber.set(var1);
   }

   public void setRecvSequenceNumber(int var1) {
      this.recvSequenceNumber = new AtomicInteger(var1);
   }

   public boolean testAndSetRecvSequencenumber(int var1) {
      if (this.recvSequenceNumber == null) {
         this.recvSequenceNumber = new AtomicInteger(var1);
         return true;
      } else {
         int var2 = this.recvSequenceNumber.get();
         boolean var3 = (var2 & 4294967295L) >= 4294966271L;
         boolean var4 = var2 + 1 == var1;
         boolean var5 = var3 & var1 < 1024;
         boolean var6 = var4 | var5;
         if (var6) {
            this.recvSequenceNumber.set(var1);
         }

         return var6;
      }
   }
}
