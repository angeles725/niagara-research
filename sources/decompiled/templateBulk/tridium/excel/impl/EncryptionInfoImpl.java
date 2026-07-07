package com.tridium.excel.impl;

import com.tridium.excel.EncryptionInfo;
import com.tridium.excel.Encryptor;

public class EncryptionInfoImpl implements EncryptionInfo {
   final org.apache.poi.poifs.crypt.EncryptionInfo encryptionInfo;

   EncryptionInfoImpl(org.apache.poi.poifs.crypt.EncryptionInfo encryptionInfo) {
      this.encryptionInfo = encryptionInfo;
   }

   public Encryptor getEncryptor() {
      org.apache.poi.poifs.crypt.Encryptor encryptor = this.encryptionInfo.getEncryptor();
      return encryptor == null ? null : new EncryptorImpl(encryptor);
   }

   @Override
   public String toString() {
      return this.encryptionInfo.toString();
   }
}
