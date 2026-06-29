package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.stack.core.ResponseHeader;
import com.prosysopc.ua.stack.core.ServiceFault;
import com.prosysopc.ua.stack.encoding.DecodingException;

public class InternalClientSideDecodingServiceFault extends ServiceFault {
   private DecodingException xY;

   public InternalClientSideDecodingServiceFault(ResponseHeader var1, DecodingException var2) {
      super(var1);
      this.xY = var2;
   }

   public DecodingException getDecodingException() {
      return this.xY;
   }

   public void setDecodingException(DecodingException var1) {
      this.xY = var1;
   }
}
