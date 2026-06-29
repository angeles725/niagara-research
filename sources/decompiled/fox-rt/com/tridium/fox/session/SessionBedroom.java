package com.tridium.fox.session;

import java.io.InterruptedIOException;

public class SessionBedroom {
   private SessionBedroom.Bed[] bedTable = new SessionBedroom.Bed[4];

   synchronized SessionBedroom.Bed getBed(Thread thread) {
      int i = 0;

      while (i < this.bedTable.length && this.bedTable[i] != null) {
         i++;
      }

      if (i == this.bedTable.length) {
         SessionBedroom.Bed[] temp = new SessionBedroom.Bed[this.bedTable.length * 2];
         System.arraycopy(this.bedTable, 0, temp, 0, this.bedTable.length);
         this.bedTable = temp;
      }

      SessionBedroom.Bed bed = new SessionBedroom.Bed();
      bed.replyNumber = i;
      bed.thread = thread;
      return this.bedTable[i] = bed;
   }

   void sleep(SessionBedroom.Bed bed) throws InterruptedIOException {
      try {
         synchronized (bed) {
            long sleepTime = Fox.clock.ticks();

            while (!bed.haveReply && Fox.clock.ticks() - sleepTime < Fox.requestTimeout) {
               long waitTime = Fox.requestTimeout - (Fox.clock.ticks() - sleepTime);
               if (waitTime < 50L) {
                  waitTime = 50L;
               }

               bed.wait(waitTime);
            }
         }
      } catch (InterruptedException var19) {
         throw new InterruptedIOException();
      } finally {
         synchronized (this) {
            bed.thread = null;
            this.bedTable[bed.replyNumber] = null;
         }
      }
   }

   void wake(FoxFrame reply) throws Exception {
      SessionBedroom.Bed bed = null;
      synchronized (this) {
         bed = this.bedTable[reply.replyNumber];
      }

      if (bed != null) {
         synchronized (bed) {
            bed.haveReply = true;
            bed.reply = reply;
            bed.notify();
         }
      }
   }

   synchronized void wakeAll() {
      for (int i = 0; i < this.bedTable.length; i++) {
         SessionBedroom.Bed bed = this.bedTable[i];
         if (bed != null) {
            bed.thread.interrupt();
            bed.thread = null;
         }
      }
   }

   @Override
   public synchronized String toString() {
      StringBuilder s = new StringBuilder();

      for (int i = 0; i < this.bedTable.length; i++) {
         SessionBedroom.Bed bed = this.bedTable[i];
         if (bed != null) {
            s.append(bed.replyNumber).append(": ").append(bed.thread).append('\n');
         }
      }

      if (s.length() == 0) {
         return "empty";
      } else {
         s.setLength(s.length() - 1);
         return s.toString();
      }
   }

   static class Bed {
      int replyNumber;
      FoxFrame reply;
      boolean haveReply = false;
      Thread thread;
   }
}
