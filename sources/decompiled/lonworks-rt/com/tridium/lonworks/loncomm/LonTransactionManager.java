package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.util.LonByteArrayUtil;
import java.util.Vector;
import javax.baja.lonworks.LonException;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Clock;

public final class LonTransactionManager {
   long tryCnt = 0L;
   private static final int MAX_TAG = 14;
   private static final int NUM_OF_LON_TRANSACTIONS = 16;
   private LonTransaction[] lonTransactionBuffers = new LonTransaction[16];
   private boolean transactionInProgress = false;
   private int nextTag = 0;
   private NLonComm lonComm = null;
   private LonTransactionManager.TransactionTimer transactionTimer;
   private Thread timerThread = null;
   private boolean done = false;

   LonTransactionManager(NLonComm lc) {
      this.lonComm = lc;

      for (int index = 0; index < 16; index++) {
         LonTransaction lonTransaction = new LonTransaction(index);
         this.lonTransactionBuffers[index] = lonTransaction;
      }
   }

   void start() {
      this.done = false;
      this.transactionTimer = new LonTransactionManager.TransactionTimer();
      this.timerThread = new Thread(this.transactionTimer, this.lonComm.lonNetwork().getLogName() + ".TransactionTimer");
      this.timerThread.start();
      this.timerThread.setPriority(5);
   }

   synchronized void stop() {
      this.done = true;
      if (this.timerThread != null) {
         this.timerThread.interrupt();
         this.timerThread = null;
      }

      this.notifyAll();

      for (int index = 0; index < 16; index++) {
         LonTransaction lt = this.lonTransactionBuffers[index];
         this.freeLonTransaction(lt);
         synchronized (lt) {
            lt.notify();
         }
      }
   }

   protected synchronized LonTransaction getLonTransaction(NAppBuffer appBuffer) {
      int index = 0;
      if (appBuffer.isImplicitAddress()) {
         index = appBuffer.getTag();
      } else {
         index = this.nextTag++;
         if (this.nextTag > 14) {
            this.nextTag = 0;
         }
      }

      while (!this.done && (this.transactionInProgress || this.lonTransactionBuffers[index].isUsed())) {
         this.tryCnt++;

         try {
            this.wait(2000L);
         } catch (InterruptedException var4) {
         }
      }

      if (this.done) {
         return null;
      } else {
         this.initTransaction(index, appBuffer);
         return this.lonTransactionBuffers[index];
      }
   }

   protected LonTransaction getLonTransactionTag15(NAppBuffer appBuffer) {
      int index = 15;

      while (!this.done) {
         synchronized (this) {
            if (!this.transactionInProgress && !this.lonTransactionBuffers[index].isUsed()) {
               this.initTransaction(index, appBuffer);
               return this.lonTransactionBuffers[index];
            }

            try {
               this.wait(2000L);
            } catch (InterruptedException var6) {
            }
         }
      }

      return null;
   }

   private void initTransaction(int index, NAppBuffer appBuffer) {
      LonTransaction transaction = this.lonTransactionBuffers[index];
      transaction.setUsed(true);
      transaction.setLocal(appBuffer.isLocalAddress());
      transaction.setOutgoingMessage(appBuffer);
      appBuffer.setTransactionTag(index);
      this.transactionTimer.start(transaction);
      this.transactionInProgress = true;
   }

   protected void freeLonTransaction(LonTransaction transaction) {
      synchronized (this) {
         if (this.transactionTimer != null) {
            this.transactionTimer.cancel(transaction);
         }

         transaction.setOutgoingMessage(null);
         transaction.setResponseMessage(null);
         transaction.setLocal(false);
         transaction.setUsed(false);
         transaction.setComplete(false);
         transaction.setException(null);
         this.transactionInProgress = false;
         this.notify();
      }

      Thread.yield();
   }

   protected LonTransaction getLonTransactionMatch(int tag) {
      return this.lonTransactionBuffers[tag];
   }

   protected void timeOut(LonTransaction transaction) {
      String err = "LonTransaction timed out " + transaction.getTag() + " :";
      NAppBuffer msg = transaction.getOutgoingMessage();
      if (msg != null) {
         err = err.concat(LonByteArrayUtil.toString(msg.getReadBuffer(), msg.getWriteBufferLen()));
      }

      this.lonComm.lonworks.log().warning(err);
      synchronized (transaction) {
         if (transaction.isUsed() && !transaction.isComplete()) {
            transaction.setResponseMessage(null);
            transaction.setException(new LonException("Timed out waiting for neuron to respond."));
            transaction.notify();
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      out.startProps("Transaction Manager");
      out.prop("transactionInProgress", this.transactionInProgress);
      out.prop("lastTagUsed", this.nextTag);
      if (this.timerThread != null) {
         out.prop("timerThread alive", this.timerThread.isAlive());
      }

      out.prop("done", this.done);
      out.prop("tryCnt", Long.toString(this.tryCnt));
      out.endProps();
      out.startTable(true);
      out.trTitle("transactions", 4);
      out.w("<tr>").th("tag").th("local").th("cmpl").th("end").th("msg").w("</tr>\n");
      synchronized (this) {
         long currTime = Clock.ticks();

         for (int i = 0; i < this.lonTransactionBuffers.length; i++) {
            LonTransaction lt = this.lonTransactionBuffers[i];
            if (lt.isUsed()) {
               out.tr(
                  Integer.toString(lt.getTag()),
                  Boolean.toString(lt.isLocal()),
                  Boolean.toString(lt.isComplete()),
                  Long.toString(lt.getEndTime() - currTime),
                  Integer.toString(lt.getOutgoingMessage().getMessageCode())
               );
            }
         }
      }

      out.endTable();
   }

   private class TransactionTimer implements Runnable {
      private Vector<LonTransaction> v = new Vector<>(16);

      private TransactionTimer() {
      }

      @Override
      public void run() {
         while (!LonTransactionManager.this.done) {
            synchronized (this) {
               long currTime = Clock.ticks();
               int timeToWait = 10000;
               if (!this.v.isEmpty()) {
                  LonTransaction curTrans = this.v.firstElement();
                  long toTime = curTrans.getEndTime();
                  if (toTime <= currTime) {
                     this.v.removeElement(curTrans);
                     LonTransactionManager.this.timeOut(curTrans);
                     timeToWait = 0;
                  } else {
                     timeToWait = (int)(toTime - currTime);
                  }
               }

               try {
                  this.wait(timeToWait);
               } catch (Exception var9) {
               }
            }
         }
      }

      synchronized void start(LonTransaction trans) {
         long transTime = trans.getOutgoingMessage().getMaxTransactionTime();
         if (transTime > 8000L) {
            transTime = 8000L;
         }

         long endTime = Clock.ticks() + transTime + 5000L;
         trans.setEndTime(endTime);
         int i = 0;

         while (i < this.v.size() && this.v.elementAt(i).getEndTime() <= endTime) {
            i++;
         }

         this.v.insertElementAt(trans, i);
         if (i == 0) {
            this.notifyAll();
         }
      }

      synchronized void cancel(LonTransaction trans) {
         trans.setEndTime(0L);
         this.v.removeElement(trans);
         this.notifyAll();
      }
   }
}
