package com.tridium.opc.jni;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.OpcException;

public class HresultException extends OpcException {
   protected int hresult;

   public HresultException(String msg) {
      super(msg);
   }

   public HresultException(int hresult, String msg) {
      super(msg);
      this.hresult = hresult;
   }

   public int getHresult() {
      return this.hresult;
   }

   public String toString() {
      return '[' + Integer.toHexString(this.hresult) + " - " + OpcEnv.getDescription(this.hresult) + "] " + this.getMessage();
   }
}
