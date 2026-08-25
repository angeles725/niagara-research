package com.tridium.crypto.core.cert;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.io.AESStreamEncryption;
import com.tridium.nre.security.io.KeyRingDecryptingInputStream;
import com.tridium.nre.security.io.KeyRingEncryptingOutputStream;
import com.tridium.nre.util.FileLock;
import com.tridium.nre.util.NiagaraFiles;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JarSignatureRegistry {
   private Map<CertPath, CertPath> certPaths = new HashMap<>();
   private Map<String, JarSignatureRegistry.JarSignatureEntry> entries = new HashMap<>();
   private boolean hasChanges = false;
   private final File file;
   private long lastModified = 0L;
   private static final Map<File, JarSignatureRegistry> instances = new HashMap<>();
   private static long jarSignatureBuildMillis = 0L;
   private static final Logger LOG = Logger.getLogger("crypto.registry");
   private static final int FILE_LOCK_TIMEOUT = 10000;
   private static final int VERSION = 1;
   private static final String MAIN_SIGNATURE_REGISTRY_NAME = "signers";

   public static JarSignatureRegistry getInstance(File file) {
      synchronized (instances) {
         JarSignatureRegistry registry = instances.get(file);
         if (registry == null) {
            registry = new JarSignatureRegistry(file);
            instances.put(file, registry);
         }

         return registry;
      }
   }

   public static JarSignatureRegistry buildSignatureRegistry() {
      long beforeBuildMillis = System.currentTimeMillis();
      if (LOG.isLoggable(Level.FINE)) {
         LOG.fine("loading module signature registry...");
      }

      File registryFile = new File(NiagaraFiles.getSecuritySigningPath(), "signers");
      if (!registryFile.exists()) {
         File oldRegistryDirectory = new File(NiagaraFiles.getNiagaraHome(), "security" + File.separator + "signing");
         File oldRegistryFile = new File(oldRegistryDirectory, "signers");
         if (oldRegistryFile.exists() && !oldRegistryFile.delete()) {
            LOG.warning("error removing old module signature registry file");
         }

         if (oldRegistryDirectory.exists() && !oldRegistryDirectory.delete()) {
            LOG.warning("error removing old module signature registry directory");
         }
      }

      JarSignatureRegistry signatureRegistry = getInstance(registryFile);
      JarSignatureRegistry.LoadReport loadReport = signatureRegistry.loadDir(NiagaraFiles.getModulesPath());
      boolean needToCheckRemoved = loadReport.anyOutOfDate
         || loadReport.fileCheckFailureCount != 0
         || loadReport.fileCheckSuccessCount != signatureRegistry.entries.size();
      if (!loadReport.anyOutOfDate) {
         LOG.info("module signature registry up-to-date");
      }

      if (!needToCheckRemoved) {
         LOG.fine("skipping save remove operation, all signature entries accounted for during load");
      }

      signatureRegistry.save(needToCheckRemoved);
      jarSignatureBuildMillis = System.currentTimeMillis() - beforeBuildMillis;
      if (loadReport.anyOutOfDate) {
         LOG.info("module signature registry build complete (" + jarSignatureBuildMillis + "ms)");
      } else {
         LOG.info("module signature registry load complete (" + jarSignatureBuildMillis + "ms)");
      }

      return signatureRegistry;
   }

   private JarSignatureRegistry(File file) {
      this.file = file;
      this.load();
   }

   public void checkReload() {
      long fileLastModified = AccessController.doPrivileged(this.file::lastModified);
      if (fileLastModified != this.lastModified) {
         try {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine("detected module signature registry change, reloading...");
            }

            this.load();
         } catch (Exception e) {
            LOG.warning("error reloading signature registry: " + e.getMessage());
            if (LOG.isLoggable(Level.FINE)) {
               LOG.log(Level.FINE, "Caused by", e);
            }
         }
      }
   }

   private synchronized void load() {
      AccessController.doPrivileged(
         () -> {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.fine("loading " + this.file);
            }

            if (this.file.exists()) {
               long loadStarted = JarSignatureRegistry.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount();
               FileLock lock = null;

               try {
                  lock = FileLock.lock(this.file, 10000);
                  if (!AESStreamEncryption.isEncrypted(this.file)) {
                     if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(String.format("JarSignatureRegistry file %s not encrypted. Creating new one.", this.file));
                     }

                     return null;
                  }

                  ISecurityInfoProvider provider = SecurityInitializer.getInstance().getSecurityInfoProvider();

                  try (
                     FileInputStream fin = new FileInputStream(this.file);
                     KeyRingDecryptingInputStream kin = new KeyRingDecryptingInputStream(fin, provider);
                     DataInputStream in = new DataInputStream(kin);
                  ) {
                     this.entries = new HashMap<>();
                     this.certPaths = new HashMap<>();
                     this.read(in);
                  }

                  this.lastModified = this.file.lastModified();
                  if (LOG.isLoggable(Level.FINE)) {
                     LOG.fine(
                        String.format(
                           "%s loaded (%dms)", this.file, JarSignatureRegistry.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount() - loadStarted
                        )
                     );
                  }
               } catch (Exception e) {
                  String causeMessage = e.getMessage();
                  String errorSuffix = causeMessage != null && !causeMessage.isEmpty() ? " (" + causeMessage + ")" : "";
                  LOG.warning("error loading JarSignatureRegistry from " + this.file + ". Creating new one." + errorSuffix);
               } finally {
                  if (lock != null) {
                     lock.unlock();
                  }
               }
            } else if (LOG.isLoggable(Level.FINE)) {
               LOG.fine(this.file + " not found. Creating new one.");
            }

            return null;
         }
      );
   }

   public synchronized void save() {
      this.save(true);
   }

   public synchronized void save(boolean checkRemoved) {
      if (checkRemoved) {
         this.removeMissing();
      }

      if (this.hasChanges) {
         AccessController.doPrivileged(
            () -> {
               if (LOG.isLoggable(Level.FINE)) {
                  LOG.fine("saving " + this.file);
               }

               FileLock lock = null;

               try {
                  long saveStarted = JarSignatureRegistry.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount();
                  File parentDirectory = this.file.getParentFile();
                  if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
                     LOG.warning("error creating module signature registry directory");
                  }

                  if (!this.file.exists() && !this.file.createNewFile()) {
                     LOG.warning("error creating module signature registry file");
                  }

                  lock = FileLock.lock(this.file, 10000);
                  ISecurityInfoProvider provider = SecurityInitializer.getInstance().getSecurityInfoProvider();

                  try (
                     FileOutputStream fileOutputStream = new FileOutputStream(this.file);
                     KeyRingEncryptingOutputStream keyRingEncryptingOutputStream = new KeyRingEncryptingOutputStream(fileOutputStream, provider);
                     DataOutputStream out = new DataOutputStream(keyRingEncryptingOutputStream);
                  ) {
                     this.store(out);
                     this.hasChanges = false;
                  }

                  if (LOG.isLoggable(Level.FINE)) {
                     LOG.fine(
                        String.format(
                           "%s saved (%dms)", this.file, JarSignatureRegistry.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.getTickCount() - saveStarted
                        )
                     );
                  }

                  this.lastModified = this.file.lastModified();
               } catch (Exception e) {
                  LOG.warning("error saving JarSignatureRegistry to " + this.file + ". (" + e.getMessage() + ')');
               } finally {
                  if (lock != null) {
                     lock.unlock();
                  }
               }

               return null;
            }
         );
      }
   }

   private void store(DataOutputStream out) throws IOException {
      List<CertPath> certPaths = new ArrayList<>(this.certPaths.keySet());
      out.writeInt(1);
      out.writeInt(certPaths.size());

      try (ObjectOutputStream objOut = new ObjectOutputStream(out)) {
         for (CertPath certPath : certPaths) {
            objOut.writeObject(certPath);
         }

         out.writeInt(this.entries.size());

         for (Entry<String, JarSignatureRegistry.JarSignatureEntry> entry : this.entries.entrySet()) {
            out.writeUTF(entry.getKey());
            JarSignatureRegistry.JarSignatureEntry signatureEntry = entry.getValue();
            out.writeLong(signatureEntry.lastModified);
            out.writeUTF(signatureEntry.signingError);
            List<CodeSigner> signers = signatureEntry.codeSigners;
            out.writeInt(signers.size());

            for (CodeSigner signer : signatureEntry.codeSigners) {
               out.writeInt(certPaths.indexOf(signer.getSignerCertPath()));
               Timestamp timestamp = signer.getTimestamp();
               if (timestamp != null) {
                  out.writeBoolean(true);
                  out.writeInt(certPaths.indexOf(signer.getTimestamp().getSignerCertPath()));
                  out.writeLong(signer.getTimestamp().getTimestamp().getTime());
               } else {
                  out.writeBoolean(false);
               }
            }
         }
      }
   }

   private void read(DataInputStream in) throws IOException, ClassNotFoundException {
      int version = in.readInt();
      if (version == 1) {
         int certPathSize = in.readInt();
         List<CertPath> certPaths = new ArrayList<>();

         try (ObjectInputStream objIn = new ObjectInputStream(in)) {
            for (int i = 0; i < certPathSize; i++) {
               CertPath certPath = (CertPath)objIn.readObject();
               certPaths.add(certPath);
               this.certPaths.put(certPath, certPath);
            }

            int numEntries = in.readInt();

            for (int i = 0; i < numEntries; i++) {
               String filePath = in.readUTF();
               long lastModified = in.readLong();
               String signatureError = in.readUTF();
               int numCodeSigners = in.readInt();
               List<CodeSigner> codeSigners = new ArrayList<>();

               for (int j = 0; j < numCodeSigners; j++) {
                  CertPath signerCertPath = certPaths.get(in.readInt());
                  Timestamp timestamp = null;
                  boolean timestamped = in.readBoolean();
                  if (timestamped) {
                     CertPath timestampCertPath = certPaths.get(in.readInt());
                     Date timestampDate = new Date(in.readLong());
                     timestamp = new Timestamp(timestampDate, timestampCertPath);
                  }

                  codeSigners.add(new CodeSigner(signerCertPath, timestamp));
               }

               this.entries.put(filePath, new JarSignatureRegistry.JarSignatureEntry(codeSigners, signatureError, lastModified));
            }
         }
      }
   }

   private void removeMissing() {
      Set<String> keys = this.entries.keySet();
      Iterator<String> iterator = keys.iterator();
      AccessController.doPrivileged(() -> {
         while (iterator.hasNext()) {
            String key = iterator.next();
            File file = new File(key);
            if (!file.exists()) {
               iterator.remove();
               this.hasChanges = true;
            }
         }

         return null;
      });
   }

   public JarSignatureRegistry.LoadReport loadDir(File dir) {
      JarSignatureRegistry.LoadReport loadReport = new JarSignatureRegistry.LoadReport();
      if (dir.exists()) {
         File[] list = dir.listFiles();
         if (list != null) {
            for (File current : list) {
               String filename = current.getName().toLowerCase(Locale.ENGLISH);
               if (filename.endsWith(".jar") || filename.endsWith(".sjar")) {
                  try {
                     loadReport.anyOutOfDate = loadReport.anyOutOfDate | this.loadIfNecessary(current);
                     if (loadReport.anyOutOfDate && !loadReport.outOfDateMessagePrinted) {
                        if (LOG.isLoggable(Level.FINE)) {
                           LOG.fine("out-of-date: module signature changed \"" + current + "\"");
                        }

                        LOG.info("rebuilding module signature registry...");
                        loadReport.outOfDateMessagePrinted = true;
                     }

                     loadReport.fileCheckSuccessCount++;
                  } catch (Exception e) {
                     LOG.warning("error loading signatures for jar file " + current.getAbsolutePath() + ": " + e.getMessage());
                     loadReport.fileCheckFailureCount++;
                  }
               }
            }
         }
      }

      return loadReport;
   }

   public List<CodeSigner> getCodeSigners(File file) throws Exception {
      this.loadIfNecessary(file);
      return Collections.unmodifiableList(this.entries.get(file.getAbsolutePath()).codeSigners);
   }

   public String getSignatureFailureCause(File file) throws Exception {
      this.loadIfNecessary(file);
      return this.entries.get(file.getAbsolutePath()).signingError;
   }

   public List<CodeSigner> getCachedCodeSigners(File file) {
      return this.isUpToDate(file) ? Collections.unmodifiableList(this.entries.get(file.getAbsolutePath()).codeSigners) : null;
   }

   public String getCachedSignatureFailureCause(File file) {
      return this.isUpToDate(file) ? this.entries.get(file.getAbsolutePath()).signingError : null;
   }

   private boolean loadIfNecessary(File file) throws Exception {
      if (!this.isUpToDate(file)) {
         AccessController.doPrivileged(() -> {
            String signingError = "";
            List<CodeSigner> codeSigners = null;

            try {
               JarVerifier verifier = new JarVerifier(file);
               codeSigners = verifier.verify();
            } catch (Exception e) {
               signingError = e.getMessage();
               signingError = signingError != null ? signingError : "null";
            }

            this.hasChanges = true;
            if (codeSigners != null) {
               List<CodeSigner> internedCodeSigners = new ArrayList<>();

               for (CodeSigner signer : codeSigners) {
                  CertPath signerCertPath = this.certPaths.computeIfAbsent(signer.getSignerCertPath(), k -> (CertPath)k);
                  Timestamp timestamp = signer.getTimestamp();
                  if (timestamp != null) {
                     CertPath timestampCertPath = this.certPaths.computeIfAbsent(timestamp.getSignerCertPath(), k -> (CertPath)k);
                     Date timestampDate = timestamp.getTimestamp();
                     timestamp = new Timestamp(timestampDate, timestampCertPath);
                  }

                  internedCodeSigners.add(new CodeSigner(signerCertPath, timestamp));
               }

               codeSigners = internedCodeSigners;
            } else {
               codeSigners = new ArrayList<>();
            }

            JarSignatureRegistry.JarSignatureEntry entry = new JarSignatureRegistry.JarSignatureEntry(codeSigners, signingError, file.lastModified());
            this.entries.put(file.getAbsolutePath(), entry);
            return null;
         });
         return true;
      } else {
         return false;
      }
   }

   private boolean isUpToDate(File file) {
      String filePath = file.getAbsolutePath();
      JarSignatureRegistry.JarSignatureEntry entry = this.entries.get(filePath);
      return entry != null && entry.lastModified == file.lastModified();
   }

   public static long getJarSignatureBuildMillis() {
      return jarSignatureBuildMillis;
   }

   private static final class JarSignatureEntry {
      private final List<CodeSigner> codeSigners;
      private final String signingError;
      private final long lastModified;

      public JarSignatureEntry(List<CodeSigner> codeSigners, String signingError, long lastModified) {
         this.codeSigners = codeSigners;
         this.signingError = signingError;
         this.lastModified = lastModified;
      }
   }

   public static class LoadReport {
      public boolean anyOutOfDate = false;
      public boolean outOfDateMessagePrinted = false;
      public int fileCheckSuccessCount = 0;
      public int fileCheckFailureCount = 0;
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }
}
