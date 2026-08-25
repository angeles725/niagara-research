package com.tridium.nre.security.io;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.security.AESEncryptFunction;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.SecretBytes;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.SecurityUtil;

public abstract class AESEncryptingInputStream extends InputStream {
   private final BufferedInputStream bufferedIn;
   protected final AESEncryptFunction encryptFunction;
   protected final byte[] ivBytes = new byte[16];
   protected byte[] encryptedBuffer = null;
   private final byte[] workingBuffer = new byte[4096];
   private final byte[] byteBuffer = new byte[1];
   private int nextRead = 0;
   private final CryptographicAlgorithmBundle algorithmBundle;

   protected AESEncryptingInputStream(InputStream unencryptedContentsIn, AESEncryptFunction encryptFunction) throws IOException {
      this(unencryptedContentsIn, encryptFunction, AesAlgorithmBundle.getInstance());
   }

   protected AESEncryptingInputStream(InputStream unencryptedContentsIn, AESEncryptFunction encryptFunction, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      this.algorithmBundle = algorithmBundle;
      Objects.requireNonNull(unencryptedContentsIn);
      this.bufferedIn = new BufferedInputStream(unencryptedContentsIn, 4096);
      this.encryptFunction = encryptFunction;
      new SecureRandom().nextBytes(this.ivBytes);
   }

   protected final void initHeader(DataOutputStream out) throws IOException {
      String[] data = new String[this.algorithmBundle.getDataElementCount()];
      data[0] = ByteArrayUtil.toHexString(this.ivBytes);
      data[1] = "0";
      String encodedHeader = this.algorithmBundle.encode(data);
      out.writeUTF(encodedHeader);
      out.flush();
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
         if (this.encryptedBuffer == null) {
            int nBufferedRead = this.bufferedIn.read(this.workingBuffer, 0, this.workingBuffer.length);
            if (nBufferedRead < 0) {
               return nBufferedRead;
            }

            try (SecretBytes toEncrypt = new SecretBytes(this.workingBuffer, 0, nBufferedRead)) {
               byte[] encryptedData = this.encryptFunction.encrypt(toEncrypt, this.ivBytes);
               this.encryptedBuffer = Arrays.copyOf(ByteBuffer.allocate(4).putInt(encryptedData.length).array(), encryptedData.length + 4);
               System.arraycopy(encryptedData, 0, this.encryptedBuffer, 4, encryptedData.length);
               this.nextRead = 0;
            }
         }

         int result = Math.min(len, this.encryptedBuffer.length - this.nextRead);
         System.arraycopy(this.encryptedBuffer, this.nextRead, b, off, result);
         this.nextRead += result;
         if (this.nextRead >= this.encryptedBuffer.length) {
            this.encryptedBuffer = null;
            this.nextRead = -1;
         }

         return result;
      } catch (IOException | RuntimeException rethrow) {
         throw rethrow;
      } catch (Exception e) {
         throw new IOException(e);
      } finally {
         SecurityUtil.zeroByteArray(this.workingBuffer);
      }
   }

   @Override
   public void close() throws IOException {
      this.bufferedIn.close();
      if (this.encryptFunction != null) {
         this.encryptFunction.close();
      }

      super.close();
   }
}
