package com.tridium.nre.security.io;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.security.AESEncryptFunction;
import com.tridium.nre.security.AesAlgorithmBundle;
import com.tridium.nre.security.SecretBytes;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import javax.baja.nre.util.ByteArrayUtil;

public abstract class AESEncryptingOutputStream extends OutputStream {
   protected final DataOutputStream dataOutput;
   protected final AESEncryptFunction encryptFunction;
   protected final byte[] ivBytes = new byte[16];
   private final byte[] workingBuffer = new byte[4096];
   private final byte[] byteBuffer = new byte[1];
   private int nextWrite = 0;
   private final CryptographicAlgorithmBundle algorithmBundle;

   protected AESEncryptingOutputStream(OutputStream outputStream, AESEncryptFunction encryptFunction) throws IOException {
      this(outputStream, encryptFunction, AesAlgorithmBundle.getInstance());
   }

   protected AESEncryptingOutputStream(OutputStream outputStream, AESEncryptFunction encryptFunction, CryptographicAlgorithmBundle algorithmBundle) throws IOException {
      this.algorithmBundle = algorithmBundle;
      Objects.requireNonNull(outputStream);
      Objects.requireNonNull(encryptFunction);
      this.dataOutput = new DataOutputStream(new BufferedOutputStream(outputStream));
      this.encryptFunction = encryptFunction;
      new SecureRandom().nextBytes(this.ivBytes);
      String[] data = new String[algorithmBundle.getDataElementCount()];
      data[0] = ByteArrayUtil.toHexString(this.ivBytes);
      data[1] = "0";
      String encodedHeader = algorithmBundle.encode(data);
      this.dataOutput.writeUTF(encodedHeader);
      this.dataOutput.flush();
   }

   @Override
   public final void write(byte[] bytes, int off, int len) throws IOException {
      int remaining = len;

      for (int toCopy = Math.min(remaining, this.workingBuffer.length - this.nextWrite); toCopy > 0; toCopy = Math.min(remaining, this.workingBuffer.length)) {
         System.arraycopy(bytes, off, this.workingBuffer, this.nextWrite, toCopy);
         this.nextWrite += toCopy;
         if (this.nextWrite >= this.workingBuffer.length) {
            this.flush();
         }

         remaining -= toCopy;
         off += toCopy;
      }
   }

   @Override
   public void flush() throws IOException {
      if (this.nextWrite > 0) {
         try (SecretBytes toEncrypt = new SecretBytes(this.workingBuffer, 0, this.nextWrite)) {
            byte[] encryptedData = this.encryptFunction.encrypt(toEncrypt, this.ivBytes);
            this.dataOutput.write(ByteBuffer.allocate(4).putInt(encryptedData.length).array());
            this.dataOutput.write(encryptedData);
         } catch (IOException ioe) {
            throw ioe;
         } catch (Exception e) {
            throw new SecurityException();
         } finally {
            Arrays.fill(this.workingBuffer, (byte)0);
            this.nextWrite = 0;
         }
      }

      this.dataOutput.flush();
   }

   @Override
   public final void write(int b) throws IOException {
      try {
         this.byteBuffer[0] = (byte)b;
         this.write(this.byteBuffer, 0, 1);
      } finally {
         this.byteBuffer[0] = 0;
      }
   }

   @Override
   public final void write(byte[] bytes) throws IOException {
      this.write(bytes, 0, bytes.length);
   }

   @Override
   public void close() throws IOException {
      this.flush();
      this.dataOutput.close();
      Arrays.fill(this.workingBuffer, (byte)0);
   }
}
