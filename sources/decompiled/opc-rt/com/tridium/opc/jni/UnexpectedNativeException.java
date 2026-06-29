package com.tridium.opc.jni;

import com.tridium.opc.OpcException;

public class UnexpectedNativeException extends OpcException {
   public UnexpectedNativeException() {
   }

   public UnexpectedNativeException(String msg) {
      super(msg);
   }
}
