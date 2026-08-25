package com.tridium.nre.util;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.security.AbstractAesAlgorithmBundle;
import com.tridium.nre.security.Aes256PasswordManager;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.AliasedAesAlgorithmBundle;
import com.tridium.nre.security.EncryptionKeySource;
import com.tridium.nre.security.KeyRing;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.PBEValidator;
import com.tridium.nre.security.PasswordStrength;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.io.BogPasswordObjectEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XText;
import javax.baja.xml.XWriter;

public class BogTranscoderInputStream extends FilterInputStream {
   private static final String PASSWORD_TYPE_NAME = "Password";
   public static final String UNKNOWN_ENCODING_TYPE = "unknown";
   private static byte[] ZIP_HEADER = new byte[]{80, 75, 3, 4};
   private static final AesAlgorithmBundle ALGORITHM_BUNDLE = AesAlgorithmBundle.getInstance();
   private static final AliasedAesAlgorithmBundle ALIASED_ALGORITHM_BUNDLE = AliasedAesAlgorithmBundle.getInstance();
   private static final boolean DEBUG = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.BogTranscodingInputStream.debug"));
   private static final boolean ECHO = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.BogTranscodingInputStream.echo"));
   private IOException toThrow = null;
   private KeyRing kr;
   private String bajaCode = null;
   private PipedInputStream pin;
   private PipedOutputStream pout;
   private ZipOutputStream zout;
   private XParser parser;
   private XWriter writer;
   private boolean isZip = false;
   private boolean quietMode = false;
   private final PBEEncodingKey keyInfo;
   private EncryptionKeySource targetKeySource;
   private final String path;
   private CompletableFuture<Void> whenHeaderParsed = new CompletableFuture<>();

   public BogTranscoderInputStream(KeyRing kr, InputStream in, PBEEncodingKey keyInfo, EncryptionKeySource targetKeySource) throws IOException {
      this(kr, in, false, keyInfo, targetKeySource, null);
   }

   public BogTranscoderInputStream(KeyRing kr, InputStream in, boolean quietMode, PBEEncodingKey keyInfo, EncryptionKeySource targetKeySource) throws IOException {
      this(kr, in, quietMode, keyInfo, targetKeySource, null);
   }

   public BogTranscoderInputStream(KeyRing kr, InputStream in, boolean quietMode, PBEEncodingKey keyInfo, EncryptionKeySource targetKeySource, String path) throws IOException {
      super(in instanceof BufferedInputStream ? in : new BufferedInputStream(in));
      this.kr = kr;
      this.quietMode = quietMode;
      this.keyInfo = keyInfo;
      this.targetKeySource = targetKeySource;
      this.path = path;
      this.in.mark(10);
      byte[] zipHeader = new byte[4];

      for (int i = 0; i < 4; i++) {
         zipHeader[i] = (byte)this.in.read();
      }

      this.isZip = Arrays.equals(zipHeader, ZIP_HEADER);
      this.in.reset();
      this.pin = new PipedInputStream(8192);
      this.pout = new PipedOutputStream(this.pin);

      try {
         if (this.isZip) {
            ZipInputStream zin = new ZipInputStream(this.in);
            ZipEntry entry = zin.getNextEntry();
            if (!entry.getName().equals("file.xml")) {
               throw new Exception();
            }

            this.parser = XParser.make(zin);
            this.zout = new ZipOutputStream(this.pout);
            this.zout.putNextEntry(new ZipEntry("file.xml"));
            this.writer = new XWriter(new BufferedOutputStream(this.zout));
         } else {
            this.parser = XParser.make(this.in);
            this.writer = new XWriter(this.pout);
         }
      } catch (Exception e) {
         throw new IOException("invalid bog file format");
      }

      new Thread(() -> {
         try {
            this.decode();
         } catch (Exception e) {
            if (!quietMode) {
               System.err.println("SEVERE [" + new Date() + "][nre] error decoding bog " + path == null ? "" : path);
               ex.printStackTrace();
            }

            if (path == null) {
               this.toThrow = new IOException("error decoding bog");
            } else {
               this.toThrow = new IOException("error decoding bog " + path);
            }

            try {
               this.writer.close();
            } catch (Throwable var14) {
            }
         } finally {
            try {
               this.pout.close();
            } catch (Throwable var13x) {
            }
         }
      }, "BogTranscoderInputStream-" + path).start();

      try {
         this.whenHeaderParsed.get(20L, TimeUnit.SECONDS);
      } catch (ExecutionException e) {
         if (e.getCause() instanceof IOException) {
            throw (IOException)e.getCause();
         } else if (e.getCause() instanceof SecurityException) {
            throw (SecurityException)e.getCause();
         } else {
            throw new IOException(e.getCause());
         }
      } catch (InterruptedException | TimeoutException e) {
         throw new IOException(e);
      }
   }

   @Override
   public boolean markSupported() {
      return false;
   }

   @Override
   public void mark(int readlimit) {
   }

   @Override
   public void reset() throws IOException {
      throw new IOException("mark is not supported");
   }

   @Override
   public int read() throws IOException {
      if (this.toThrow != null) {
         throw this.toThrow;
      } else {
         int result = this.pin.read();
         if (this.toThrow != null) {
            throw this.toThrow;
         } else {
            return result;
         }
      }
   }

   @Override
   public int read(byte[] b) throws IOException {
      if (this.toThrow != null) {
         throw this.toThrow;
      } else {
         int result = this.pin.read(b);
         if (this.toThrow != null) {
            throw this.toThrow;
         } else {
            return result;
         }
      }
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      if (this.toThrow != null) {
         throw this.toThrow;
      } else {
         int result = this.pin.read(b, off, len);
         if (this.toThrow != null) {
            throw this.toThrow;
         } else {
            return result;
         }
      }
   }

   public void decode() throws Exception {
      long decodeStart = System.currentTimeMillis();
      if (DEBUG) {
         System.err.println("DEBUG [" + new Date() + "][nre] entered BogTranscoderInputStream::decode(), starting performance timer");
      }

      try {
         Array<String> path = new Array<>(String.class);
         XWriter debugWriter = null;
         if (ECHO) {
            debugWriter = new XWriter(System.err);
         }

         this.writer.prolog();
         if (ECHO) {
            debugWriter.prolog();
            debugWriter.flush();
         }

         int curType = this.parser.next();
         XElem curElem = this.parser.elem().copy();
         if (!curElem.name().equalsIgnoreCase("bajaObjectGraph")) {
            this.writer.close();
            this.decodeFailed("invalid bog file format");
         }

         try (BogPasswordObjectEncoder originalBogEncoder = BogPasswordObjectEncoder.parseBogHeader(curElem, EncryptionKeySource.undefined)) {
            if (originalBogEncoder.getKeySource().equals(EncryptionKeySource.none)) {
               this.targetKeySource = EncryptionKeySource.none;
            } else if (this.targetKeySource.equals(EncryptionKeySource.external)) {
               if (originalBogEncoder.getKeySource() != EncryptionKeySource.keyring) {
                  this.writer.close();
                  this.decodeFailed("Expected bog with key source == keyring, found " + originalBogEncoder.getKeySource().name());
               }
            } else if (this.targetKeySource.equals(EncryptionKeySource.keyring)) {
               if (originalBogEncoder.getKeySource().equals(EncryptionKeySource.external)) {
                  if (!originalBogEncoder.getPbeEncodingInfo().test(this.keyInfo.getPassPhrase())) {
                     this.writer.close();
                     EnumSet<PBEValidator.ValidationFault> validationFaults = PBEValidator.checkPassPhraseValidity(this.keyInfo.getPassPhrase());
                     if (validationFaults.contains(PBEValidator.ValidationFault.FIPS_MIN_LENGTH)) {
                        this.decodeFailed(
                           String.format(
                              "Cannot decode bog file, pass phrases in FIPS 140-%d mode must be at least %d characters long",
                              SecurityInitializer.getInstance().getFipsInformation().getFipsVersion(),
                              PasswordStrength.MINIMUM_ALLOWED_LENGTH
                           )
                        );
                     } else {
                        this.decodeFailed("Cannot decode bog file, incorrect pass phrase");
                     }
                  }
               } else {
                  this.writer.close();
                  this.decodeFailed("Expected bog with key source of external or none, found " + originalBogEncoder.getKeySource().name());
               }
            } else {
               this.writer.close();
               this.decodeFailed("invalid targetKeySource " + this.targetKeySource.name());
            }
         }

         this.whenHeaderParsed.complete(null);
         BogPasswordObjectEncoder passwordObjectEncoder;
         switch (this.targetKeySource) {
            case external:
               passwordObjectEncoder = BogPasswordObjectEncoder.makeExternal(this.keyInfo);
               break;
            case keyring:
               passwordObjectEncoder = BogPasswordObjectEncoder.makeKeyring(this.kr);
               break;
            default:
               passwordObjectEncoder = BogPasswordObjectEncoder.makeNone();
         }

         passwordObjectEncoder.populateBogHeaderElement(curElem);
         int depth = 0;
         XText curText = null;

         while (curElem != null && curType != -1) {
            int nextType = this.parser.next();
            XElem nextElem = this.parser.elem();
            if (nextElem != null) {
               nextElem = nextElem.copy();
            }

            XText nextText = this.parser.text();
            switch (curType) {
               case 1:
                  String module = curElem.get("m", null);
                  if (module != null && module.endsWith("=baja")) {
                     int equals = module.indexOf("=");
                     if (equals > 0) {
                        this.bajaCode = module.substring(0, equals);
                     }
                  }

                  if (this.bajaCode != null) {
                     String passwordType = this.bajaCode + ":" + "Password";
                     String type = curElem.get("t", null);
                     if (type != null && type.equals(passwordType)) {
                        String eValue = curElem.get("v", null);
                        if (eValue != null) {
                           try {
                              eValue = this.processPasswordValue(eValue, passwordObjectEncoder);
                              curElem.removeAttr("v");
                              curElem.addAttr("v", eValue);
                           } catch (Exception e) {
                              if (this.targetKeySource == EncryptionKeySource.keyring) {
                                 throw e;
                              }

                              if (!this.quietMode) {
                                 System.err
                                    .println(
                                       "SEVERE ["
                                          + new Date()
                                          + "][nre] problem encountered while trying to parse a password in the bog at "
                                          + TextUtil.join(path.trim(), '/')
                                    );
                              }
                           }
                        }
                     }
                  }

                  if (nextType == 2) {
                     curElem.write(this.writer, depth);
                     if (ECHO) {
                        curElem.write(debugWriter, depth);
                     }

                     nextType = this.parser.next();
                     nextElem = this.parser.elem().copy();
                     nextText = this.parser.text();
                  } else {
                     this.writer.indent(depth);
                     if (ECHO) {
                        debugWriter.indent(depth);
                     }

                     this.writer.write(curElem.toString() + "\r\n");
                     if (ECHO) {
                        debugWriter.write(curElem.toString() + "\r\n");
                     }

                     depth++;
                     path.push(curElem.get("n", ""));
                  }
                  break;
               case 2:
                  depth--;
                  path.pop();
                  this.writer.indent(depth);
                  if (ECHO) {
                     debugWriter.indent(depth);
                  }

                  this.writer.write("</" + curElem.name() + ">\r\n");
                  if (ECHO) {
                     debugWriter.write("</" + curElem.name() + ">\r\n");
                  }
                  break;
               case 3:
                  String text = curText.string().trim();
                  this.writer.write(text + "\r\n");
                  if (ECHO) {
                     debugWriter.write(text + "\r\n");
                  }
            }

            curType = nextType;
            curElem = nextElem;
            curText = nextText;
         }

         this.writer.flush();
         if (ECHO) {
            debugWriter.flush();
         }

         this.writer.close();
      } catch (IOException ioe) {
         this.decodeFailed(ioe);
      } catch (Exception e) {
         this.decodeFailed(new IOException(e));
      }

      if (DEBUG) {
         System.err
            .println("DEBUG [" + new Date() + "][nre] BogTranscoderInputStream::decode() complete (" + (System.currentTimeMillis() - decodeStart) + "ms)");
      }
   }

   private void decodeFailed(String message) throws IOException {
      this.toThrow = new IOException(message);
      if (!this.whenHeaderParsed.isDone()) {
         this.whenHeaderParsed.completeExceptionally(this.toThrow);
      }

      try {
         this.pout.close();
      } catch (Exception var3) {
      }

      throw this.toThrow;
   }

   private void decodeFailed(IOException e) throws IOException {
      this.toThrow = e;
      if (!this.whenHeaderParsed.isDone()) {
         this.whenHeaderParsed.completeExceptionally(e);
      }

      try {
         this.pout.close();
      } catch (Exception var3) {
      }

      throw this.toThrow;
   }

   private String processPasswordValue(String encodedPassword, BogPasswordObjectEncoder bogPasswordObjectEncoder) throws Exception {
      CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstanceFor(encodedPassword);
      if (!(bundle instanceof AbstractAesAlgorithmBundle)) {
         return encodedPassword;
      }

      if (this.targetKeySource.equals(EncryptionKeySource.none)) {
         throw new IOException("Unexpected AES password in BOG file with keySource==none");
      }

      try (SecretBytes decoded = this.targetKeySource.equals(EncryptionKeySource.external)
            ? this.decodeAesPasswordWithNiagaraKeyRing(encodedPassword)
            : this.decodeAesPasswordWithExternalKey(encodedPassword)) {
         String alias = null;
         if (bundle instanceof AliasedAesAlgorithmBundle) {
            alias = bundle.decode(encodedPassword)[0];
         }

         return bogPasswordObjectEncoder.encodePassword(decoded, (AbstractAesAlgorithmBundle)bundle, alias);
      }
   }

   private SecretBytes decodeAesPasswordWithExternalKey(String encodedPassword) throws Exception {
      CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstanceFor(encodedPassword);
      String[] data = bundle.decode(encodedPassword);
      String ivString = null;
      String cipherString = null;
      String aesTransformation = null;
      if (bundle instanceof AbstractAesAlgorithmBundle) {
         ivString = data[((AbstractAesAlgorithmBundle)bundle).getIvIndex()];
         cipherString = data[((AbstractAesAlgorithmBundle)bundle).getCipherIndex()];
         aesTransformation = ((AbstractAesAlgorithmBundle)bundle).getAesTransformation();
      }

      byte[] iv = ByteArrayUtil.hexStringToBytes(ivString);
      byte[] cipher = ByteArrayUtil.hexStringToBytes(cipherString);
      return new SecretBytes(Aes256PasswordManager.decrypt(this.keyInfo.get().get(), cipher, iv, aesTransformation), true);
   }

   private SecretBytes decodeAesPasswordWithNiagaraKeyRing(String encodedPassword) throws Exception {
      CryptographicAlgorithmBundle bundle = CryptographicAlgorithmBundle.getInstanceFor(encodedPassword);
      String[] data = bundle.decode(encodedPassword);
      Aes256PasswordManager manager = null;
      String iv = null;
      String cipher = null;
      String aesTransformation = null;
      if (bundle instanceof AbstractAesAlgorithmBundle) {
         iv = data[((AbstractAesAlgorithmBundle)bundle).getIvIndex()];
         cipher = data[((AbstractAesAlgorithmBundle)bundle).getCipherIndex()];
         aesTransformation = ((AbstractAesAlgorithmBundle)bundle).getAesTransformation();
      }

      if (bundle instanceof AliasedAesAlgorithmBundle) {
         String alias = data[0];
         manager = Aes256PasswordManager.getManager(this.kr, alias);
      } else {
         manager = Aes256PasswordManager.getManager(this.kr);
      }

      return new SecretBytes(manager.decrypt(cipher, iv, aesTransformation).getBytes(StandardCharsets.UTF_8), true);
   }

   @Override
   public void close() throws IOException {
      this.parser.close();
      this.writer.close();
      this.pin.close();
      this.pout.flush();
      this.pout.close();
      this.in.close();
   }
}
