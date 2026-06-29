package com.prosysopc.ua.stack.encoding;

import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.StatusCodes;

public class DecodingException extends ServiceResultException {
   private static final long serialVersionUID = 1L;

   public DecodingException() {
      super(StatusCodes.Bad_DecodingError);
   }

   public DecodingException(Exception var1) {
      super(StatusCodes.Bad_DecodingError, var1, var1.getMessage());
   }

   public DecodingException(Exception var1, String var2) {
      super(StatusCodes.Bad_DecodingError, var1, var2);
   }

   public DecodingException(int var1) {
      super(var1);
   }

   public DecodingException(int var1, String var2) {
      super(var1, var2);
   }

   public DecodingException(StatusCode var1) {
      super(var1);
   }

   public DecodingException(StatusCode var1, String var2) {
      super(var1, var2);
   }

   public DecodingException(StatusCode var1, Throwable var2, String var3) {
      super(var1, var2, var3);
   }

   public DecodingException(String var1) {
      super(StatusCodes.Bad_DecodingError, var1);
   }

   public DecodingException(String var1, Exception var2) {
      super(StatusCodes.Bad_DecodingError, var2, var1);
   }

   public DecodingException(Throwable var1) {
      super(var1);
   }

   public DecodingException(UnsignedInteger var1) {
      super(var1);
   }

   public DecodingException(UnsignedInteger var1, String var2) {
      super(var1, var2);
   }

   public DecodingException(UnsignedInteger var1, Throwable var2) {
      super(var1, var2);
   }
}
