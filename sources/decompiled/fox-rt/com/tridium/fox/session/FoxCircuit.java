package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.MessageReader;
import com.tridium.fox.message.MessageWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import javax.baja.nre.util.SecurityUtil;

public class FoxCircuit {
   private static final FoxMessage NoMetadata = new FoxMessage();
   private static final boolean TRACE = false;
   public final int id;
   public final String channel;
   public final String command;
   public final FoxMessage metadata;
   boolean open = true;
   FoxSession session;
   FoxCircuit.CircuitOutputStream out;
   FoxCircuit.CircuitInputStream in;
   Object outgoingLock = new Object();
   byte[] outgoing = new byte[Fox.circuitChunkSize];
   int nOutgoing;
   Object incomingLock = new Object();
   FoxCircuit.BufferEntry incomingHead = null;
   FoxCircuit.BufferEntry incomingTail = null;
   int nIncoming;

   FoxCircuit(int id, FoxSession session, String channel, String command, FoxMessage meta) {
      this.id = id;
      this.session = session;
      this.channel = channel;
      this.command = command;
      this.metadata = meta == null ? NoMetadata : meta;
      this.out = new FoxCircuit.CircuitOutputStream();
      this.in = new FoxCircuit.CircuitInputStream();
   }

   public boolean isOpen() {
      return this.open;
   }

   public FoxSession session() {
      return this.session;
   }

   public InputStream getInputStream() {
      return this.in;
   }

   public OutputStream getOutputStream() {
      return this.out;
   }

   public FoxMessage readMessage() throws Exception {
      MessageReader reader = new MessageReader(this.in);
      FoxMessage msg = new FoxMessage();
      msg.readValue(reader);
      return msg;
   }

   public void writeMessage(FoxMessage msg) throws Exception {
      MessageWriter writer = new MessageWriter(this.out);
      msg.writeValue(writer);
      writer.flush();
   }

   public FoxMessage write(byte[] buf, int offset, int length, FoxAsyncCallbacks callbacks) throws Exception {
      if (callbacks == null) {
         this.out.write(buf, offset, length);
         return null;
      } else {
         synchronized (this.outgoingLock) {
            this.flush();
            byte[] outbuf = new byte[length];
            System.arraycopy(buf, offset, outbuf, 0, length);
            FoxMessage msg = null;
            int start = 0;
            int len = length;

            while (start != len) {
               int remainder = len - start;
               int thisChunk = Math.min(Fox.circuitChunkSize, remainder);
               msg = this.sendStream(outbuf, start, thisChunk, thisChunk == remainder ? callbacks : null);
               start += thisChunk;
            }

            return msg;
         }
      }
   }

   public void close() {
      if (this.open) {
         try {
            this.flush();
         } catch (IOException var6) {
         }

         this.session.closeCircuit(this);
         this.open = false;
         synchronized (this.incomingLock) {
            this.incomingLock.notifyAll();
         }

         synchronized (this.outgoingLock) {
            this.outgoingLock.notifyAll();
         }
      }
   }

   void writeOut(byte[] b, int off, int len) throws IOException {
      this.assertOpen();
      synchronized (this.outgoingLock) {
         while (this.open && len > 0) {
            int n = Math.min(this.outgoing.length - this.nOutgoing, len);
            System.arraycopy(b, off, this.outgoing, this.nOutgoing, n);
            this.nOutgoing += n;
            off += n;
            len -= n;
            if (this.nOutgoing >= this.outgoing.length) {
               this.flush();
            }
         }
      }
   }

   public void flush() throws IOException {
      synchronized (this.outgoingLock) {
         if (this.open && this.nOutgoing > 0) {
            try {
               this.sendStream(this.outgoing, this.nOutgoing);
               this.outgoing = new byte[Fox.circuitChunkSize];
               this.nOutgoing = 0;
            } catch (IOException var4) {
               throw var4;
            } catch (Exception var5) {
               var5.printStackTrace();
               throw new IOException(var5.toString());
            }
         }
      }
   }

   void pushIn(byte[] data) throws InterruptedException {
      if (data.length != 0) {
         synchronized (this.incomingLock) {
            while (this.open && this.incomingHead != null && this.nIncoming + data.length > Fox.circuitMaxReceiveBuffer) {
               this.incomingLock.wait();
            }

            FoxCircuit.BufferEntry entry = new FoxCircuit.BufferEntry(data);
            if (this.incomingHead == null) {
               this.incomingHead = entry;
            }

            if (this.incomingTail != null) {
               this.incomingTail.next = entry;
            }

            this.incomingTail = entry;
            this.nIncoming += data.length;
            this.incomingLock.notifyAll();
         }
      }
   }

   int pullIn(byte[] buf, int off, int len) throws IOException {
      synchronized (this.incomingLock) {
         while (this.nIncoming <= 0) {
            if (!this.open) {
               return -1;
            }

            try {
               this.incomingLock.wait();
            } catch (InterruptedException var9) {
               throw new InterruptedIOException();
            }
         }

         int n = 0;

         while (len > 0) {
            FoxCircuit.BufferEntry be = this.incomingHead;
            if (be == null) {
               break;
            }

            int consume = Math.min(be.avail, len);
            System.arraycopy(be.data, be.data.length - be.avail, buf, off, consume);
            n += consume;
            off += consume;
            len -= consume;
            be.avail -= consume;
            if (be.avail <= 0) {
               this.incomingHead = be.next;
               if (this.incomingTail == be) {
                  this.incomingTail = null;
               }

               be.next = null;
            }
         }

         this.nIncoming -= n;
         this.incomingLock.notifyAll();
         return n;
      }
   }

   private void assertOpen() throws IOException {
      if (!this.open) {
         throw new IOException("circuit closed");
      }
   }

   void sendOpen() throws Exception {
      FoxRequest req = new FoxRequest("circuit", "open");
      req.add("id", this.id);
      req.add("channel", this.channel);
      req.add("command", this.command);
      req.add("metadata", this.metadata);
      this.session.sendAsync(req);
   }

   void sendStream(byte[] buffer, int len) throws Exception {
      FoxRequest req = new FoxRequest("circuit", "stream");
      req.add("id", this.id);
      req.add("data", buffer, len);
      this.session.sendAsync(req);
   }

   FoxRequest sendStream(byte[] buffer, int offset, int len, FoxAsyncCallbacks callbacks) throws Exception {
      FoxRequest req = new FoxRequest("circuit", "stream");
      req.add("id", this.id);
      req.add("data", buffer, offset, len);
      this.session.sendAsync(req, callbacks);
      return req;
   }

   void sendClose() throws Exception {
      if (this.open && !this.session.isClosed()) {
         FoxRequest req = new FoxRequest("circuit", "close");
         req.add("id", this.id);
         this.session.sendAsync(req);
      }
   }

   @Override
   public String toString() {
      return "Circuit ["
         + SecurityUtil.calculateSessionIdHash(this.session.getId())
         + "] "
         + this.channel
         + "."
         + this.command
         + " out="
         + this.nOutgoing
         + " in="
         + this.nIncoming
         + " open="
         + this.open;
   }

   public void dumpIncomingList() {
      System.out.println("Incoming List:");
      System.out.println("  head=" + this.incomingHead);
      System.out.println("  tail=" + this.incomingTail);
      System.out.println("  chain=");

      for (FoxCircuit.BufferEntry e = this.incomingHead; e != null; e = e.next) {
         System.out.println("    " + e);
      }
   }

   private static void trace(String s) {
      System.out.println(s);
   }

   static class BufferEntry {
      byte[] data;
      int avail;
      FoxCircuit.BufferEntry next;

      BufferEntry(byte[] b) {
         this.data = b;
         this.avail = b.length;
      }

      @Override
      public String toString() {
         return Integer.toString(System.identityHashCode(this), 36) + " avail=" + this.avail + " length=" + this.data.length;
      }
   }

   class CircuitInputStream extends InputStream {
      byte[] temp = new byte[1];

      @Override
      public int read() throws IOException {
         return this.read(this.temp, 0, 1) <= 0 ? -1 : this.temp[0] & 0xFF;
      }

      @Override
      public int read(byte[] b) throws IOException {
         return this.read(b, 0, b.length);
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
         return FoxCircuit.this.pullIn(b, off, len);
      }

      @Override
      public void close() throws IOException {
         FoxCircuit.this.close();
      }
   }

   class CircuitOutputStream extends OutputStream {
      byte[] temp = new byte[1];

      @Override
      public void write(int b) throws IOException {
         this.temp[0] = (byte)b;
         this.write(this.temp, 0, 1);
      }

      @Override
      public void write(byte[] b) throws IOException {
         this.write(b, 0, b.length);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
         FoxCircuit.this.writeOut(b, off, len);
      }

      @Override
      public void flush() throws IOException {
         FoxCircuit.this.flush();
      }

      @Override
      public void close() {
         FoxCircuit.this.close();
      }
   }
}
