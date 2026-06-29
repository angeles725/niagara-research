package com.tridium.bacnet.asn;

import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.nre.util.TextUtil;

public class NErrorType implements ErrorType {
   private int errorClass;
   private int errorCode;

   public NErrorType() {
   }

   public NErrorType(int errorClass, int errorCode) {
      this.errorClass = errorClass;
      this.errorCode = errorCode;
   }

   public NErrorType(BBacnetErrorClass errorClass, BBacnetErrorCode errorCode) {
      this.errorClass = errorClass.getOrdinal();
      this.errorCode = errorCode.getOrdinal();
   }

   @Override
   public int getErrorClass() {
      return this.errorClass;
   }

   @Override
   public int getErrorCode() {
      return this.errorCode;
   }

   @Override
   public void writeEncoded(AsnOutput out) {
      out.writeEnumerated(this.errorClass);
      out.writeEnumerated(this.errorCode);
   }

   @Override
   public void readEncoded(AsnInput in) throws AsnException {
      this.errorClass = in.readEnumerated();
      this.errorCode = in.readEnumerated();
   }

   @Override
   public String toString() {
      return TextUtil.toFriendly(BBacnetErrorClass.tag(this.errorClass)) + ":" + TextUtil.toFriendly(BBacnetErrorCode.tag(this.errorCode));
   }

   public String debugString() {
      StringBuilder sb = new StringBuilder("Error: ");
      sb.append("\n  error-class = " + BBacnetErrorClass.tag(this.errorClass));
      sb.append("\n  error-code = " + BBacnetErrorCode.tag(this.errorCode));
      return sb.toString();
   }

   public static String toString(int errorClass, int errorCode) {
      return TextUtil.toFriendly(BBacnetErrorClass.tag(errorClass)) + ":" + TextUtil.toFriendly(BBacnetErrorCode.tag(errorCode));
   }
}
