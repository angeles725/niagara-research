package com.tridium.excel.impl;

import com.tridium.excel.Encryptor;
import com.tridium.excel.ExcelFileSystem;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public class EncryptorImpl implements Encryptor {
   final org.apache.poi.poifs.crypt.Encryptor encryptor;

   EncryptorImpl(org.apache.poi.poifs.crypt.Encryptor encryptor) {
      this.encryptor = encryptor;
   }

   public void confirmPassword(String password) {
      this.encryptor.confirmPassword(password);
   }

   public OutputStream getDataStream(ExcelFileSystem fs) throws IOException, GeneralSecurityException {
      return this.encryptor.getDataStream(((ExcelFileSystemImpl)fs).fileSystem);
   }

   @Override
   public String toString() {
      return this.encryptor.toString();
   }
}
