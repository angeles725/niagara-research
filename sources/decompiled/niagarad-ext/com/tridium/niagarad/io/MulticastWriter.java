package com.tridium.niagarad.io;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

public class MulticastWriter extends Writer {
   private final Object writersMonitor = new Object();
   private LinkedList<Writer> writers = null;

   public MulticastWriter() {
      this.writers = new LinkedList<>();
   }

   public void addWriter(Writer writer) {
      synchronized (this.writersMonitor) {
         this.writers.add(writer);
      }
   }

   @Override
   public void close() throws IOException {
      synchronized (this.writersMonitor) {
         for (Writer writer : this.writers) {
            writer.close();
         }
      }
   }

   @Override
   public void flush() throws IOException {
      synchronized (this.writersMonitor) {
         for (Writer writer : this.writers) {
            writer.flush();
         }
      }
   }

   @Override
   public void write(char[] buf, int offset, int length) throws IOException {
      synchronized (this.writersMonitor) {
         for (Writer writer : this.writers) {
            writer.write(buf, offset, length);
         }
      }
   }

   @Override
   public void write(String message) throws IOException {
      synchronized (this.writersMonitor) {
         for (Writer writer : this.writers) {
            writer.write(message);
         }
      }
   }
}
