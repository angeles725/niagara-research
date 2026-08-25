package com.tridium.crypto.core.cert;

public class CertificateParseException extends Exception {
   private final String fieldName;
   private final Object fieldValue;

   public CertificateParseException(String fieldName, Object fieldValue) {
      this.fieldName = fieldName;
      this.fieldValue = fieldValue;
   }

   public String getFieldName() {
      return this.fieldName;
   }

   public Object getFieldValue() {
      return this.fieldValue;
   }
}
