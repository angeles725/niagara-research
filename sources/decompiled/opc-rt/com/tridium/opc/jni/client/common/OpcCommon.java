package com.tridium.opc.jni.client.common;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcCommon extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{F31DFDE2-07B6-11d2-B2D8-0060083BA1FB}", OpcCommon.class);

   public int[] availableLocales() {
      return this.availableLocales(this.getPeer());
   }

   public String getErrorString(int hresult) throws OpcException {
      return this.getErrorString(this.getPeer(), hresult);
   }

   public int getLocale() throws OpcException {
      return this.getLocaleId(this.getPeer());
   }

   public void setClientName(String name) throws OpcException {
      this.setClientName(this.getPeer(), name);
   }

   public void setLocale(int id) throws OpcException {
      this.setLocaleId(this.getPeer(), id);
   }

   private native int[] availableLocales(long var1);

   private native String getErrorString(long var1, int var3);

   private native int getLocaleId(long var1);

   private native void setClientName(long var1, String var3);

   private native void setLocaleId(long var1, int var3);
}
