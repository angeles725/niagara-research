package com.tridium.opc.jni.client.da;

import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcItemProperties extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a72-011e-11d0-9675-0020afd8adb3}", OpcItemProperties.class);

   public double getNumericProperty(OpcItem item, OpcItem.Property prop) throws IllegalArgumentException {
      return this.getNumericProperty(this.getPeer(), item.id, prop.pid);
   }

   public boolean getBooleanProperty(OpcItem item, OpcItem.Property prop) throws IllegalArgumentException {
      return this.getBooleanProperty(this.getPeer(), item.id, prop.pid);
   }

   public String getStringProperty(OpcItem item, OpcItem.Property prop) throws IllegalArgumentException {
      return this.getStringProperty(this.getPeer(), item.id, prop.pid);
   }

   public void queryAvailableProperties(OpcItem item) {
      this.queryAvailableProperties(this.getPeer(), item.id, item);
   }

   private native double getNumericProperty(long var1, String var3, int var4);

   private native boolean getBooleanProperty(long var1, String var3, int var4);

   private native String getStringProperty(long var1, String var3, int var4);

   private native void queryAvailableProperties(long var1, String var3, OpcItem var4);
}
