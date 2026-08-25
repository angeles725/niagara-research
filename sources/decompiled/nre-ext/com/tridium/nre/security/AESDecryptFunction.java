package com.tridium.nre.security;

public interface AESDecryptFunction extends AutoCloseable {
   SecretBytes decrypt(byte[] var1, byte[] var2, String var3) throws Exception;

   @Override
   default void close() {
   }
}
