package com.tridium.nre.auth;

public interface KeyDeriver {
   byte[] deriveKey(byte[] var1, long var2, String var4, int var5) throws Exception;
}
