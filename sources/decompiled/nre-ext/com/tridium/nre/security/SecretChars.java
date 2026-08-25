package com.tridium.nre.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public final class SecretChars implements AutoCloseable, Supplier<char[]> {
   private final char[] value;
   private boolean isClosed = false;
   private static final char FILL = '\u0000';

   public SecretChars(char[] value, boolean clearValueParameter) {
      Objects.requireNonNull(value);
      this.value = Arrays.copyOf(value, value.length);
      if (clearValueParameter) {
         Arrays.fill(value, '\u0000');
      }
   }

   @Override
   public void close() {
      Arrays.fill(this.value, '\u0000');
      this.isClosed = true;
   }

   public char[] get() {
      this.checkClosed();
      return this.value;
   }

   public char get(int i) {
      this.checkClosed();
      return this.value[i];
   }

   public int size() {
      this.checkClosed();
      return this.value.length;
   }

   public SecretChars newCopy() {
      this.checkClosed();
      return new SecretChars(this.value, false);
   }

   public static SecretChars fromString(String value) {
      return value == null ? null : new SecretChars(value.toCharArray(), true);
   }

   public static SecretChars fromSecretBytes(SecretBytes value) {
      return value == null ? null : fromSecretBytes(value, StandardCharsets.UTF_8, false);
   }

   public static SecretChars fromSecretBytes(SecretBytes value, Charset charset, boolean closeSecretBytes) {
      if (value == null) {
         return null;
      }

      CharBuffer charBuffer = null;

      try {
         ByteBuffer byteBuffer = ByteBuffer.wrap(value.get());
         charBuffer = charset.decode(byteBuffer);
         return new SecretChars(Arrays.copyOfRange(charBuffer.array(), charBuffer.position(), charBuffer.limit()), true);
      } finally {
         if (closeSecretBytes) {
            value.close();
         }

         if (charBuffer != null) {
            Arrays.fill(charBuffer.array(), '\u0000');
         }
      }
   }

   public SecretBytes asSecretBytes() {
      return this.asSecretBytes(StandardCharsets.UTF_8, false);
   }

   public SecretBytes asSecretBytes(Charset charset, boolean closeThisSecretChars) {
      this.checkClosed();
      ByteBuffer byteBuffer = null;

      try {
         CharBuffer charBuffer = CharBuffer.wrap(this.get());
         byteBuffer = charset.encode(charBuffer);
         return new SecretBytes(Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()), true);
      } finally {
         if (closeThisSecretChars) {
            this.close();
         }

         if (byteBuffer != null) {
            Arrays.fill(byteBuffer.array(), (byte)0);
         }
      }
   }

   public String asString(boolean closeThisSecretChars) {
      try {
         return new String(this.get());
      } finally {
         if (closeThisSecretChars) {
            this.close();
         }
      }
   }

   public boolean isClosed() {
      return this.isClosed;
   }

   private void checkClosed() {
      if (this.isClosed) {
         throw new IllegalStateException("Cannot perform operation on a SecretChars that has been closed");
      }
   }
}
