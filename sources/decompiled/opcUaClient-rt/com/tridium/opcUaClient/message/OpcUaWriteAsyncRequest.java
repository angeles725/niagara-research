package com.tridium.opcUaClient.message;

import com.tridium.opcUaClient.point.BOpcUaClientProxyExt;
import javax.baja.status.BStatusValue;
import javax.baja.util.ICoalesceable;

public class OpcUaWriteAsyncRequest implements Runnable, ICoalesceable {
   protected final int hashCode;
   protected final BOpcUaClientProxyExt proxyExt;
   protected final BStatusValue out;

   public OpcUaWriteAsyncRequest(BOpcUaClientProxyExt proxyExt, BStatusValue out) {
      this.hashCode = proxyExt.hashCode();
      this.proxyExt = proxyExt;
      this.out = out;
   }

   @Override
   public void run() {
      if (this.proxyExt != null) {
         this.proxyExt.doWrite(this.out);
      }
   }

   @Override
   public int hashCode() {
      return this.hashCode;
   }

   @Override
   public boolean equals(Object object) {
      if (object instanceof OpcUaWriteAsyncRequest) {
         OpcUaWriteAsyncRequest o = (OpcUaWriteAsyncRequest)object;
         return this.proxyExt.equals(o.proxyExt);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "OpcWriteAsyncRequest (proxyExt=" + this.proxyExt + ", writeValue=" + this.out + ")";
   }

   public Object getCoalesceKey() {
      return this;
   }

   public ICoalesceable coalesce(ICoalesceable c) {
      return c;
   }
}
