package com.tridium.nre.security.io;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.security.AESDecryptFunction;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.SecretBytes;
import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;

public abstract class AESDecryptingInputStream extends InputStream {
   private final DataInputStream dataIn;
   protected AESDecryptFunction decryptFunction;
   private final byte[] ivBytes = new byte[16];
   private SecretBytes decryptedBuffer = null;
   private final byte[] byteBuffer = new byte[1];
   private int nextRead = -1;
   private final AesAlgorithmBundle algorithmBundle;

   protected AESDecryptingInputStream(InputStream encryptedContentsIn, AESDecryptFunction decryptFunction) throws IOException {
      Objects.requireNonNull(encryptedContentsIn);
      this.dataIn = new DataInputStream(new BufferedInputStream(encryptedContentsIn));
      this.decryptFunction = decryptFunction;
      String encodedHeader = this.dataIn.readUTF();

      try {
         CryptographicAlgorithmBundle algorithmBundle = CryptographicAlgorithmBundle.getInstanceFor(encodedHeader);
         if (!(algorithmBundle instanceof AesAlgorithmBundle)) {
            throw new IOException("Invalid algorithm bundle: " + algorithmBundle.getAlgorithmName());
         }

         this.algorithmBundle = (AesAlgorithmBundle)algorithmBundle;
         String[] data = algorithmBundle.decode(encodedHeader);
         ByteArrayUtil.copy(ByteArrayUtil.hexStringToBytes(data[0]), this.ivBytes);
      } catch (IllegalArgumentException iae) {
         throw new IOException("Invalid encrypted file format", iae);
      }

      this.init(this.dataIn);
   }

   public static boolean isEncryptedFile(File file) {
      try (
         FileInputStream fileInRaw = new FileInputStream(file);
         DataInputStream fileIn = new DataInputStream(fileInRaw);
      ) {
         String encodedHeader = fileIn.readUTF();
         CryptographicAlgorithmBundle algorithmBundle = CryptographicAlgorithmBundle.getInstanceFor(encodedHeader);
         String[] data = algorithmBundle.decode(encodedHeader);
         return algorithmBundle instanceof AesAlgorithmBundle;
      } catch (Exception e) {
         return false;
      }
   }

   protected void init(DataInput in) throws IOException {
   }

   @Override
   public final int read() throws IOException {
      try {
         int result = this.read(this.byteBuffer, 0, 1);
         return result < 0 ? result : this.byteBuffer[0] & 0xFF;
      } finally {
         this.byteBuffer[0] = 0;
      }
   }

   @Override
   public final int read(byte[] b, int off, int len) throws IOException {
      try {
         if (this.decryptedBuffer == null) {
            int bufferSize;
            try {
               bufferSize = this.dataIn.readInt();
            } catch (EOFException eofe) {
               return -1;
            }

            byte[] encryptedBuffer = new byte[bufferSize];
            this.dataIn.readFully(encryptedBuffer);

            try {
               this.decryptedBuffer = this.decryptFunction.decrypt(encryptedBuffer, this.ivBytes, this.algorithmBundle.getAesTransformation());
            } catch (Exception e) {
               throw new SecurityException();
            }

            this.nextRead = 0;
         }

         int result = Math.min(len, this.decryptedBuffer.get().length - this.nextRead);
         System.arraycopy(this.decryptedBuffer.get(), this.nextRead, b, off, result);
         this.nextRead += result;
         if (this.nextRead >= this.decryptedBuffer.get().length) {
            this.decryptedBuffer.close();
            this.decryptedBuffer = null;
            this.nextRead = -1;
         }

         return result;
      } catch (RuntimeException | IOException torethrow) {
         throw torethrow;
      } catch (Exception e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      this.dataIn.close();
      if (this.decryptFunction != null) {
         this.decryptFunction.close();
      }

      if (this.decryptedBuffer != null) {
         this.decryptedBuffer.close();
      }

      super.close();
   }
}
