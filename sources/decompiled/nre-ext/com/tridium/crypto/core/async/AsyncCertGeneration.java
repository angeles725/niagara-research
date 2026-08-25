package com.tridium.crypto.core.async;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.NKeyPairGenerator;
import com.tridium.crypto.core.cert.NX509CertificateBuilder;
import com.tridium.crypto.core.io.CoreKeyStore;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.SecretChars;
import java.security.AccessController;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.util.IntHashMap;

public class AsyncCertGeneration {
   private BlockingQueue<AsyncCertGeneration.AsyncCertRequest> queue = new LinkedBlockingQueue<>(10);
   private AsyncCertGeneration.CertificateRequestProcessor certGen = null;
   private IntHashMap reqStatus = new IntHashMap();
   private static final Logger LOGGER = Logger.getLogger("crypto");
   public static final int FAILURE = -1;
   public static final int ENQUEUED = 0;
   public static final int PROCESSING = 1;
   public static final int SUCCESS = 2;

   public int enqueue(IAsyncCertRequestEntry entry) {
      AsyncCertGeneration.AsyncCertRequest request = new AsyncCertGeneration.AsyncCertRequest(entry);

      try {
         boolean success = false;
         if (this.queue.remainingCapacity() > 0) {
            synchronized (this) {
               this.reqStatus.put(request.getRequestId(), request);
               success = this.queue.offer(request);
            }
         }

         if (success) {
            this.checkCertGenThread();
            return request.getRequestId();
         } else {
            return -1;
         }
      } catch (Exception e) {
         LOGGER.log(Level.SEVERE, "Exception occurred while enqueuing certificate request", e);
         return -1;
      }
   }

   public int status(int requestId) {
      AsyncCertGeneration.AsyncCertRequest req = (AsyncCertGeneration.AsyncCertRequest)this.reqStatus.get(requestId);
      return req == null ? -1 : req.getStatus();
   }

   private synchronized void checkCertGenThread() {
      if (this.certGen == null || !this.certGen.isAlive()) {
         this.certGen = new AsyncCertGeneration.CertificateRequestProcessor();
         this.certGen.start();
      }
   }

   public void stopCertGenThread() {
      if (this.certGen != null && this.certGen.isAlive()) {
         this.certGen.stopRequested = true;
         this.certGen.interrupt();

         try {
            this.certGen.join(3000L);
         } catch (InterruptedException var2) {
         }
      }
   }

   private void processRequest(AsyncCertGeneration.AsyncCertRequest req) {
      String requestAlias = null;

      try {
         req.setStatus(1);
         if (req.getEntry() != null) {
            IAsyncCertRequestEntry iCertRequestEntry = req.getEntry();
            if (iCertRequestEntry instanceof ResetUserKeyStoreEntry) {
               if (req.getEntry().getKeyStore() != null) {
                  CoreKeyStore coreKeyStore = (CoreKeyStore)req.getEntry().getKeyStore();
                  coreKeyStore.generateDefaultEntry(true);
                  Enumeration<String> aliases = req.getEntry().getKeyStore().aliases();

                  while (aliases.hasMoreElements()) {
                     String alias = aliases.nextElement();
                     if (!"default".equalsIgnoreCase(alias)) {
                        req.getEntry().getKeyStore().deleteEntry(alias);
                     }
                  }

                  coreKeyStore.save();
               }

               req.setStatus(2);
            } else if (iCertRequestEntry instanceof CertGenerationEntry) {
               CertGenerationEntry genEntry = (CertGenerationEntry)req.getEntry();
               requestAlias = genEntry.getCertificateBuilder().getAlias();
               NX509CertificateBuilder builder = genEntry.getCertificateBuilder();
               NKeyPairGenerator generator = genEntry.getGenerator();
               SecretChars password = genEntry.getPassword();
               if (generator == null) {
                  generator = CertUtils.FACTORY_CERT_GENERATOR;
               }

               IX509CertificateEntry entry = builder.generateEntry(generator);
               if (genEntry.getKeyStore() != null) {
                  if (password == null) {
                     genEntry.getKeyStore().setKeyEntry(entry.getAlias(), entry.getPrivateKey(), null, entry.getCertificates());
                  } else {
                     genEntry.getKeyStore().setKeyEntry(entry.getAlias(), entry.getPrivateKey(), password.get(), entry.getCertificates());
                  }

                  genEntry.getKeyStore().save();
               }

               req.setStatus(2);
            } else {
               LOGGER.log(Level.SEVERE, "Invalid async cert request entry: " + req.getEntry().getClass().getSimpleName());
               req.setStatus(-1);
            }
         } else {
            LOGGER.log(Level.WARNING, "cert processing request unsupported from older connections");
         }
      } catch (Exception e) {
         LOGGER.log(Level.SEVERE, "Exception occurred while processing cert request", e);
         req.setStatus(-1);
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("cert processing request failed for " + req.getEntry().getClass().getSimpleName());
         }
      }
   }

   private static class AsyncCertRequest {
      private int requestId;
      private IAsyncCertRequestEntry entry;
      private int status = 0;
      private long completed = 0L;
      private static int nextRequestId = 1;

      public AsyncCertRequest(IAsyncCertRequestEntry entry) {
         this.requestId = nextRequestId++;
         if (this.requestId < 0) {
            nextRequestId = 1;
            this.requestId = nextRequestId++;
         }

         this.entry = entry;
      }

      public int getRequestId() {
         return this.requestId;
      }

      public IAsyncCertRequestEntry getEntry() {
         return this.entry;
      }

      public synchronized void setStatus(int status) {
         this.status = status;
      }

      public synchronized int getStatus() {
         return this.status;
      }

      public synchronized void setCompleted(long completed) {
         this.completed = completed;
      }

      public synchronized long getCompleted() {
         return this.completed;
      }

      @Override
      public boolean equals(Object o) {
         if (o != null && o instanceof AsyncCertGeneration.AsyncCertRequest) {
            AsyncCertGeneration.AsyncCertRequest req = (AsyncCertGeneration.AsyncCertRequest)o;
            return req.requestId == this.requestId;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(this.requestId);
      }
   }

   private class CertificateRequestProcessor extends Thread {
      volatile boolean stopRequested = false;

      public CertificateRequestProcessor() {
         super("AsyncCertGeneration.CertificateRequestProcessor");
         this.setDaemon(true);
      }

      @Override
      public void run() {
         Thread.currentThread().setPriority(1);

         while (!this.stopRequested) {
            try {
               AsyncCertGeneration.AsyncCertRequest req = AsyncCertGeneration.this.queue.poll(600L, TimeUnit.SECONDS);
               if (req == null && !AsyncCertGeneration.this.reqStatus.isEmpty()) {
                  break;
               }

               if (req != null) {
                  AsyncCertGeneration.this.processRequest(req);
                  req.setCompleted(AsyncCertGeneration.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount());
               }

               Iterator<Object> stati = AsyncCertGeneration.this.reqStatus.iterator();
               long now = AsyncCertGeneration.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount();

               while (stati.hasNext()) {
                  AsyncCertGeneration.AsyncCertRequest completed = (AsyncCertGeneration.AsyncCertRequest)stati.next();
                  if (completed.getCompleted() != 0L && now - completed.getCompleted() > 60000L) {
                     AsyncCertGeneration.this.reqStatus.remove(completed.getRequestId());
                  }
               }
            } catch (Exception e) {
               if (!this.stopRequested) {
                  AsyncCertGeneration.LOGGER.log(Level.SEVERE, "Certificate request processing terminated prior to stopRequested", e);
               }
               break;
            }
         }
      }
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
