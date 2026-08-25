package com.tridium.nre.security;

public interface AESEncryptFunction extends AutoCloseable {
   byte[] encrypt(SecretBytes var1, byte[] var2) throws Exception;

   @Override
   default void close() {
   }
}
