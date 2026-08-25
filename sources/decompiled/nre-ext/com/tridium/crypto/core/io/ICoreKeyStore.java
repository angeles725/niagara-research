package com.tridium.crypto.core.io;

import java.security.Key;
import java.security.cert.X509Certificate;

public interface ICoreKeyStore extends ICoreTrustStore {
   Key getKey(String var1, char[] var2) throws Exception;

   void setKeyEntry(String var1, byte[] var2, X509Certificate[] var3) throws Exception;

   void setKeyEntry(String var1, Key var2, char[] var3, X509Certificate[] var4) throws Exception;
}
