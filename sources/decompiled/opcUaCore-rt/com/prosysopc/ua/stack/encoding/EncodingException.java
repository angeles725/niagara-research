package com.prosysopc.ua.stack.encoding;

import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.StatusCodes;

public class EncodingException extends ServiceResultException {
   private static final long serialVersionUID = 1L;

   public EncodingException() {
      super(StatusCodes.Bad_EncodingError);
   }

   public EncodingException(Exception var1) {
      super(StatusCodes.Bad_EncodingError, var1, var1.getMessage());
   }

   public EncodingException(int var1) {
      super(var1);
   }

   public EncodingException(int var1, String var2) {
      super(var1, var2);
   }

   public EncodingException(int var1, Throwable var2, String var3) {
      super(var1, var2, var3);
   }

   public EncodingException(StatusCode var1) {
      super(var1);
   }

   public EncodingException(StatusCode var1, String var2) {
      super(var1, var2);
   }

   public EncodingException(StatusCode var1, Throwable var2, String var3) {
      super(var1, var2, var3);
   }

   public EncodingException(String var1) {
      super(StatusCodes.Bad_EncodingError, var1);
   }

   public EncodingException(String var1, Exception var2) {
      super(StatusCodes.Bad_EncodingError, var2, var1);
   }

   public EncodingException(Throwable var1) {
      super(var1);
   }

   public EncodingException(UnsignedInteger var1) {
      super(var1);
   }

   public EncodingException(UnsignedInteger var1, String var2) {
      super(var1, var2);
   }

   public EncodingException(UnsignedInteger var1, Throwable var2) {
      super(var1, var2);
   }

   public EncodingException(UnsignedInteger var1, Throwable var2, String var3) {
      super(var1, var2, var3);
   }
}
