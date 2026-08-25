package com.tridium.nre.security.io;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.ISecurityInfoProvider;
import com.tridium.nre.security.PBEDecryptingInputStream;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.SecretChars;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.baja.nre.function.UnaryOperatorCanThrowException;

public final class AESStreamEncryption {
   public static InputStream keyRingToPBE(InputStream krEncryptedContents, ISecurityInfoProvider provider, SecretChars passPhrase) throws IOException {
      return keyRingToPBE(krEncryptedContents, provider, passPhrase, AesAlgorithmBundle.getInstance());
   }

   public static InputStream keyRingToPBE(
      InputStream krEncryptedContents, ISecurityInfoProvider provider, SecretChars passPhrase, AesAlgorithmBundle algorithmBundle
   ) throws IOException {
      return new PBEEncryptingInputStream(new KeyRingDecryptingInputStream(krEncryptedContents, provider), passPhrase, algorithmBundle);
   }

   public static InputStream keyRingToPBE(InputStream krEncryptedContents, ISecurityInfoProvider provider, PBEEncodingKey encodingKey) throws IOException {
      return keyRingToPBE(krEncryptedContents, provider, encodingKey, AesAlgorithmBundle.getInstance());
   }

   public static InputStream keyRingToPBE(
      InputStream krEncryptedContents, ISecurityInfoProvider provider, PBEEncodingKey encodingKey, AesAlgorithmBundle algorithmBundle
   ) throws IOException {
      return new PBEEncryptingInputStream(new KeyRingDecryptingInputStream(krEncryptedContents, provider), encodingKey, algorithmBundle);
   }

   public static InputStream pbeToKeyRing(InputStream pbeEncryptedContents, SecretChars passPhrase, ISecurityInfoProvider provider) throws IOException {
      return pbeToKeyRing(pbeEncryptedContents, passPhrase, provider, AesAlgorithmBundle.getInstance());
   }

   public static InputStream pbeToKeyRing(
      InputStream pbeEncryptedContents, SecretChars passPhrase, ISecurityInfoProvider provider, AesAlgorithmBundle algorithmBundle
   ) throws IOException {
      return new KeyRingEncryptingInputStream(new PBEDecryptingInputStream(pbeEncryptedContents, passPhrase), provider, algorithmBundle);
   }

   public static InputStream pbeToKeyRing(InputStream pbeEncryptedContents, PBEEncodingKey encodingKey, ISecurityInfoProvider provider) throws IOException {
      return pbeToKeyRing(pbeEncryptedContents, encodingKey, provider, AesAlgorithmBundle.getInstance());
   }

   public static InputStream pbeToKeyRing(
      InputStream pbeEncryptedContents, PBEEncodingKey encodingKey, ISecurityInfoProvider provider, AesAlgorithmBundle algorithmBundle
   ) throws IOException {
      return new KeyRingEncryptingInputStream(new PBEDecryptingInputStream(pbeEncryptedContents, encodingKey), provider, algorithmBundle);
   }

   public static <E extends Exception> InputStream ifEncrypted(
      InputStream originalInputStream, UnaryOperatorCanThrowException<InputStream, E> applyWhenEncrypted
   ) throws E, IOException {
      return ifEncrypted(originalInputStream, applyWhenEncrypted, UnaryOperatorCanThrowException.identity());
   }

   public static <EE extends Exception, UE extends Exception> InputStream ifEncrypted(
      InputStream originalInputStream,
      UnaryOperatorCanThrowException<InputStream, EE> applyWhenEncrypted,
      UnaryOperatorCanThrowException<InputStream, UE> applyWhenNotEncrypted
   ) throws EE, UE, IOException {
      AESStreamEncryption.StreamEncryptionDetails check = new AESStreamEncryption.StreamEncryptionDetails(originalInputStream);
      if (check.isEncrypted) {
         if (applyWhenEncrypted == null) {
            check.stream.close();
            return null;
         }

         InputStream result = applyWhenEncrypted.apply(check.stream);
         if (result == null) {
            check.stream.close();
         }

         return result;
      } else {
         if (applyWhenNotEncrypted == null) {
            check.stream.close();
            return null;
         }

         InputStream result = applyWhenNotEncrypted.apply(check.stream);
         if (result == null) {
            check.stream.close();
         }

         return result;
      }
   }

   public static boolean isEncrypted(File file) throws IOException {
      try (InputStream in = new FileInputStream(file)) {
         AESStreamEncryption.StreamEncryptionDetails check = new AESStreamEncryption.StreamEncryptionDetails(in);

         try {
            return check.isEncrypted();
         } finally {
            try {
               check.stream.close();
            } catch (Exception var24) {
            }
         }
      }
   }

   private static final class StreamEncryptionDetails {
      private final DataInputStream stream;
      private boolean isEncrypted = true;

      private StreamEncryptionDetails(InputStream originalInputStream) throws IOException {
         this.stream = new DataInputStream(new BufferedInputStream(originalInputStream));
         this.stream.mark(1024);

         try {
            String encodedHeader = this.stream.readUTF();
            CryptographicAlgorithmBundle algorithmBundle = CryptographicAlgorithmBundle.getInstanceFor(encodedHeader);
            String[] data = algorithmBundle.decode(encodedHeader);
            this.isEncrypted = algorithmBundle instanceof AesAlgorithmBundle;
         } catch (Exception e) {
            this.isEncrypted = false;
         } finally {
            this.stream.reset();
         }
      }

      public InputStream stream() {
         return this.stream;
      }

      public boolean isEncrypted() {
         return this.isEncrypted;
      }
   }
}
