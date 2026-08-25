package com.tridium.niagarad.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.WebSocketSession;

public class TimeoutInputStream extends FilterInputStream {
   private long readTimeout;
   private WebSocketClient client;
   private boolean EOFonTimeout;
   private Logger log;
   private ExecutorService executor;
   AtomicBoolean closing = new AtomicBoolean(false);
   AtomicBoolean closed = new AtomicBoolean(false);

   protected TimeoutInputStream(WebSocketClient client, InputStream in, long readTimeout, Logger log, boolean EOFonTimeout) {
      super(in);
      this.readTimeout = readTimeout;
      this.client = client;
      this.log = log;
      this.EOFonTimeout = EOFonTimeout;
      this.executor = Executors.newSingleThreadExecutor(new TimeoutInputStream.TimeoutInputStreamThreadFactory());
   }

   @Override
   public int read(byte[] b) throws IOException {
      return this.read(b, 0, b.length);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      if (this.in == null) {
         return -1;
      }

      Callable<Integer> readBufferTask = () -> this.in.read(b, off, len);
      Future<Integer> future = null;

      try {
         future = this.executor.submit(readBufferTask);
         return future.get(this.readTimeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | TimeoutException e) {
         future.cancel(true);
         if (e instanceof TimeoutException && this.EOFonTimeout) {
            return -1;
         } else {
            throw new InterruptedIOException();
         }
      } catch (ExecutionException e) {
         Throwable cause = e.getCause();
         this.handleWriteEndDeadException(cause);
         if (cause instanceof IOException) {
            throw (IOException)cause;
         } else {
            throw new IOException(cause);
         }
      }
   }

   @Override
   public int read() throws IOException {
      if (this.in == null) {
         return -1;
      }

      Callable<Integer> readTask = this.in::read;
      Future<Integer> future = null;

      try {
         future = this.executor.submit(readTask);
         return future.get(this.readTimeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException | TimeoutException e) {
         future.cancel(true);
         if (e instanceof TimeoutException && this.EOFonTimeout) {
            return -1;
         } else {
            throw new InterruptedIOException();
         }
      } catch (ExecutionException e) {
         Throwable cause = e.getCause();
         this.handleWriteEndDeadException(cause);
         if (cause instanceof IOException) {
            throw (IOException)cause;
         } else {
            throw new IOException(cause);
         }
      }
   }

   @Override
   public void close() throws IOException {
      if (!this.closed.get() && this.closing.compareAndSet(false, true)) {
         try {
            AccessController.doPrivileged(() -> {
               if (this.client != null) {
                  Set<WebSocketSession> openSessions = this.client.getOpenSessions();
                  if (openSessions != null) {
                     for (WebSocketSession webSocketSession : openSessions) {
                        webSocketSession.close(1001, "Disconnected");
                     }
                  }

                  try {
                     this.client.stop();
                  } catch (Throwable var6) {
                  }

                  try {
                     this.client.getHttpClient().stop();
                  } catch (Throwable var5) {
                  }
               }

               if (this.executor != null) {
                  try {
                     this.executor.shutdownNow();
                  } catch (Throwable var4) {
                  }
               }

               this.client = null;
               this.executor = null;
               return null;
            });
         } catch (PrivilegedActionException var2) {
         }

         super.close();
         this.closed.set(true);
         this.closing.set(false);
      }
   }

   private void handleWriteEndDeadException(Throwable cause) throws InterruptedIOException {
      if (this.in instanceof PipedInputStream && cause.getMessage().equals("Write end dead")) {
         try {
            Thread.sleep(this.readTimeout);
         } catch (InterruptedException var3) {
         }

         throw new InterruptedIOException("Write End Dead");
      }
   }

   private static class TimeoutInputStreamThreadFactory implements ThreadFactory {
      private final ThreadGroup group;
      private final AtomicInteger threadNumber = new AtomicInteger(1);
      private final String namePrefix;

      private TimeoutInputStreamThreadFactory() {
         SecurityManager securityManager = System.getSecurityManager();
         this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
         this.namePrefix = "TimeoutInputStream-";
      }

      @Override
      public Thread newThread(Runnable job) {
         Thread thread = new Thread(this.group, job, this.namePrefix + this.threadNumber.getAndIncrement(), 0L);
         if (thread.isDaemon()) {
            thread.setDaemon(false);
         }

         if (thread.getPriority() != 5) {
            thread.setPriority(5);
         }

         return thread;
      }
   }
}
