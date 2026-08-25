package com.tridium.nre.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public final class SecretBytes implements AutoCloseable, Supplier<byte[]> {
   private final byte[] value;
   private boolean isClosed;

   public SecretBytes(byte[] value, boolean clearValueParameter) {
      Objects.requireNonNull(value);
      this.value = Arrays.copyOf(value, value.length);
      if (clearValueParameter) {
         Arrays.fill(value, (byte)0);
      }
   }

   public SecretBytes(byte[] value, int start, int len) {
      Objects.requireNonNull(value);
      this.value = Arrays.copyOfRange(value, start, len);
   }

   public SecretBytes(int capacity) {
      this.value = new byte[capacity];
   }

   @Override
   public void close() {
      Arrays.fill(this.value, (byte)0);
      this.isClosed = true;
   }

   public byte[] get() {
      this.checkClosed();
      return this.value;
   }

   public int size() {
      this.checkClosed();
      return this.value.length;
   }

   public SecretBytes newCopy() {
      this.checkClosed();
      return new SecretBytes(this.value, false);
   }

   public static SecretBytes fromString(String value) {
      return value == null ? null : fromString(value, StandardCharsets.UTF_8);
   }

   public String asString(boolean closeThisSecretBytes, Charset charset) {
      try {
         return new String(this.get(), charset);
      } finally {
         if (closeThisSecretBytes) {
            this.close();
         }
      }
   }

   public static SecretBytes random(int size) {
      SecretBytes result = new SecretBytes(size);
      new SecureRandom().nextBytes(result.get());
      return result;
   }

   public static SecretBytes fromString(String value, Charset charset) {
      Objects.requireNonNull(charset);
      if (value == null) {
         return null;
      }

      ByteBuffer byteBuffer = null;

      try {
         CharBuffer charBuffer = CharBuffer.wrap(value.toCharArray());
         byteBuffer = charset.encode(charBuffer);
         return new SecretBytes(Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()), true);
      } finally {
         if (byteBuffer != null) {
            Arrays.fill(byteBuffer.array(), (byte)0);
         }
      }
   }

   private void checkClosed() {
      if (this.isClosed) {
         throw new IllegalStateException("Cannot perform operation on a SecretBytes that has been closed");
      }
   }

   public boolean isClosed() {
      return this.isClosed;
   }
}
