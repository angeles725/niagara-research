package com.tridium.excel;

public class EncryptedDocumentException extends IllegalStateException {
   public EncryptedDocumentException(Exception e) {
      super(e);
   }
}
