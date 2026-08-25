package com.tridium.crypto.core.io;

public interface ICoreStore {
   long getLastModified() throws Exception;

   boolean isReadOnly() throws Exception;
}
