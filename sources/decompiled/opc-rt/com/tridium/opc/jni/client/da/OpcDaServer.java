package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.client.common.OpcServer;
import java.util.TreeMap;

public class OpcDaServer extends OpcServer {
   private TreeMap<String, OpcGroup> groups = new TreeMap<>();

   public synchronized OpcGroup addGroup(String name, boolean active, int updateRate, int clientHandle, int timebias, float percentDeadband, int localeId) {
      OpcGroup ret = this.groups.get(name);
      if (ret != null) {
         return ret;
      } else {
         long[] result = this.addGroup(this.peer, name, active, updateRate, clientHandle, timebias, percentDeadband, localeId);
         if (result != null && result.length >= 3) {
            ret = new OpcGroup(name, result[0]);
            ret.serverHandle = (int)result[1];
            ret.revisedUpdateRate = (int)result[2];
            this.groups.put(name, ret);
         }

         return ret;
      }
   }

   public OpcBrowse getBrowse() {
      return (OpcBrowse)this.query(OpcBrowse.IID);
   }

   public OpcBrowseServerAddressSpace getBrowseServerAddressSpace() {
      return (OpcBrowseServerAddressSpace)this.query(OpcBrowseServerAddressSpace.IID);
   }

   public OpcItemProperties getItemProperties() {
      return (OpcItemProperties)this.query(OpcItemProperties.IID);
   }

   public OpcGroup getGroup(String name) {
      return this.groups.get(name);
   }

   public OpcDaServer.Status getStatus() {
      OpcDaServer.Status ret = new OpcDaServer.Status();
      this.getStatus(this.peer, ret);
      return ret;
   }

   public static OpcDaServer newServer(String PROGID) throws OpcException {
      OpcDaServer ret = new OpcDaServer();
      ret.peer = ret.createLocalServer(PROGID);
      return ret;
   }

   public static OpcDaServer newServer(String CLSID, String host) throws OpcException {
      OpcDaServer ret = new OpcDaServer();
      ret.peer = ret.createRemoteServer(CLSID, host);
      return ret;
   }

   @Override
   public void release() {
      try {
         TreeMap<String, OpcGroup> tmp = new TreeMap<>();
         tmp.putAll(tmp);

         for (OpcGroup g : tmp.values()) {
            this.removeGroup(g);
         }
      } catch (Exception var4) {
      }

      this.groups.clear();
      super.release();
   }

   public void removeGroup(OpcGroup group) {
      this.groups.remove(group.getName());
      group.realRelease();
      this.removeGroup(this.peer, group.serverHandle, true);
   }

   public OpcNTSecurity getNTSecurity() {
      return (OpcNTSecurity)this.query(OpcNTSecurity.IID);
   }

   public OpcPrivateSecurity getPrivateSecurity() {
      return (OpcPrivateSecurity)this.query(OpcPrivateSecurity.IID);
   }

   public long getPeerObject() {
      return this.getPeer();
   }

   private native long[] addGroup(long var1, String var3, boolean var4, int var5, int var6, int var7, float var8, int var9);

   private native long createLocalServer(String var1);

   private native long createRemoteServer(String var1, String var2);

   private native void getStatus(long var1, OpcDaServer.Status var3);

   private native void removeGroup(long var1, int var3, boolean var4);

   public static class Status {
      public static final int STATE_RUNNING = 1;
      public static final int STATE_FAILED = 2;
      public static final int STATE_NOCONFIG = 3;
      public static final int STATE_SUSPENDED = 4;
      public static final int STATE_TEST = 5;
      public static final int STATE_COMM_FAULT = 6;
      public long startTime;
      public long currentTime;
      public long lastUpdateTime;
      public int state;
      public int groupCount;
      public int bandwidth;
      public int majorVersion;
      public int minorVersion;
      public int buildNumber;
      public String vendorInfo;

      public String getStatusString() {
         switch (this.state) {
            case 1:
               return "Running";
            case 2:
               return "Failed";
            case 3:
               return "No Config";
            case 4:
               return "Suspended";
            case 5:
               return "Test";
            case 6:
               return "Comm fault";
            default:
               return "Unknown: " + this.state;
         }
      }

      public void init(
         long startTime,
         long currentTime,
         long lastUpdateTime,
         int state,
         int groupCount,
         int bandwidth,
         int majorVersion,
         int minorVersion,
         int buildNumber,
         String vendorInfo
      ) {
         this.startTime = startTime;
         this.currentTime = currentTime;
         this.lastUpdateTime = lastUpdateTime;
         this.state = state;
         this.groupCount = groupCount;
         this.bandwidth = bandwidth;
         this.majorVersion = majorVersion;
         this.minorVersion = minorVersion;
         this.buildNumber = buildNumber;
         this.vendorInfo = vendorInfo;
      }
   }
}
