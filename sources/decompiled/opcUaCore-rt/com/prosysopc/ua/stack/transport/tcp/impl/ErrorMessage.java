package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.encoding.IEncodeable;

public final class ErrorMessage extends AbstractUaTcpCommMessage implements IEncodeable {
   public static boolean disableReasonField = true;
   private UnsignedInteger xW;
   private String reason;

   public ErrorMessage() {
   }

   public ErrorMessage(StatusCode var1, String var2) {
      this.xW = var1.getValue();
      this.setReason(var2);
   }

   public ErrorMessage(UnsignedInteger var1, String var2) {
      this.xW = var1;
      this.setReason(var2);
   }

   public UnsignedInteger getError() {
      return this.xW;
   }

   public String getReason() {
      return this.reason;
   }

   public void setError(UnsignedInteger var1) {
      this.xW = var1;
   }

   public void setReason(String var1) {
      this.reason = var1;
      if (disableReasonField) {
         this.reason = null;
      } else {
         this.reason = var1;
      }
   }
}
