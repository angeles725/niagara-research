package com.tridium.excel;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public interface Encryptor {
   void confirmPassword(String var1);

   OutputStream getDataStream(ExcelFileSystem var1) throws IOException, GeneralSecurityException;
}
