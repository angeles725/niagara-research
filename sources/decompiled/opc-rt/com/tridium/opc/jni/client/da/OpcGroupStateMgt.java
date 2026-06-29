package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;

public class OpcGroupStateMgt extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a50-011e-11d0-9675-0020afd8adb3}", OpcGroupStateMgt.class);

   public OpcGroupStateMgt.GroupState getState() throws OpcException {
      OpcGroupStateMgt.GroupState ret = new OpcGroupStateMgt.GroupState();
      this.getState(this.getPeer(), ret);
      return ret;
   }

   public int setState(int updateRate, boolean active, int timeBias, float percentDeadband, int localeId, int clientHandle) throws OpcException {
      return this.setState(this.getPeer(), updateRate, active, timeBias, percentDeadband, localeId, clientHandle);
   }

   private native void getState(long var1, OpcGroupStateMgt.GroupState var3);

   private native int setState(long var1, int var3, boolean var4, int var5, float var6, int var7, int var8);

   public static class GroupState {
      public int updateRate;
      public boolean active;
      public String name;
      public int timeBias;
      public float percentDeadband;
      public int localeId;
      public int clientGroup;
      public int serverGroup;

      private void set(int updateRate, boolean active, String name, int timeBias, float percentDeadband, int localeId, int clientGroup, int serverGroup) {
         this.updateRate = updateRate;
         this.active = active;
         this.name = name;
         this.timeBias = timeBias;
         this.percentDeadband = percentDeadband;
         this.localeId = localeId;
         this.clientGroup = clientGroup;
         this.serverGroup = serverGroup;
      }
   }
}
