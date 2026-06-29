package com.tridium.opc.client;

import com.tridium.opc.client.util.BOpcDiscoveryJob;
import com.tridium.opc.jni.client.common.OpcServerList2;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcDeviceDiscoveryJob extends BOpcDiscoveryJob {
   public static final Type TYPE = Sys.loadType(BOpcDeviceDiscoveryJob.class);
   public static final String CATID_OpcDaServer1_0 = "{63D5F430-CFE4-11D1-B2C8-0060083BA1FB}";
   public static final String CATID_OpcDaServer2_0 = "{63D5F432-CFE4-11D1-B2C8-0060083BA1FB}";
   public static final String CATID_OpcDaServer3_0 = "{CC603642-66D7-48F1-B69A-B625E73652D7}";
   protected BValue args;
   Log opcLog = Log.getLog("OpcDaLog");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BOpcDeviceDiscoveryJob() {
   }

   public BOpcDeviceDiscoveryJob(BOpcNetwork network, BValue args) {
      super(network);
      this.args = args;
   }

   @Override
   public void doDiscover() {
      this.opcLog.trace("Opc Device discovery started");
      this.removeAll();
      String addr = this.args.toString();

      try {
         try {
            this.opcLog.message(addr);
            this.progress(10);
            Hashtable<String, BOpcDeviceDiscoveryResult> tbl = new Hashtable<>();

            try {
               OpcServerList2 obj = new OpcServerList2();
               Object[] arr = obj.discoverServersInNetwork(addr, "{63D5F430-CFE4-11D1-B2C8-0060083BA1FB}");
               if (arr != null) {
                  for (int i = 0; i < arr.length; i++) {
                     BOpcDeviceDiscoveryResult result = (BOpcDeviceDiscoveryResult)arr[i];
                     tbl.put(result.getClassId() + result.getProgId(), result);
                     if (this.isCanceled()) {
                        return;
                     }
                  }
               }

               if (this.isCanceled()) {
                  return;
               }

               this.progress(33);
               this.opcLog.trace("Listing CAT 1.0 OPC servers");
            } catch (Exception var13) {
               this.opcLog.trace("Exception in getting CAT 1.0 servers' list " + var13.toString());
               var13.printStackTrace();
            }

            if (this.isCanceled()) {
               return;
            }

            try {
               OpcServerList2 list2 = new OpcServerList2();
               Object[] arrx = list2.discoverServersInNetwork(addr, "{63D5F432-CFE4-11D1-B2C8-0060083BA1FB}");
               if (arrx != null) {
                  for (int ix = 0; ix < arrx.length; ix++) {
                     BOpcDeviceDiscoveryResult result = (BOpcDeviceDiscoveryResult)arrx[ix];
                     tbl.put(result.getClassId() + result.getProgId(), result);
                     if (this.isCanceled()) {
                        return;
                     }
                  }
               }

               this.progress(66);
               this.opcLog.trace("Listing CAT 2.0 OPC servers");
            } catch (Exception var15) {
               this.opcLog.trace("Exception in getting CAT 2.0 servers' list " + var15.toString());
               var15.printStackTrace();
            }

            if (this.isCanceled()) {
               return;
            }

            try {
               OpcServerList2 list3 = new OpcServerList2();
               Object[] arrx = list3.discoverServersInNetwork(addr, "{CC603642-66D7-48F1-B69A-B625E73652D7}");
               this.opcLog.trace("Listing CAT 3.0 OPC servers");
               if (arrx != null) {
                  for (int ixx = 0; ixx < arrx.length; ixx++) {
                     BOpcDeviceDiscoveryResult result = (BOpcDeviceDiscoveryResult)arrx[ixx];
                     tbl.put(result.getClassId() + result.getProgId(), result);
                     if (this.isCanceled()) {
                        return;
                     }
                  }
               }

               this.progress(90);
            } catch (Exception var14) {
               this.opcLog.trace("Exception in getting CAT 3.0 servers' list " + var14.toString());
               var14.printStackTrace();
            }

            if (this.isCanceled()) {
               return;
            }

            Enumeration<BOpcDeviceDiscoveryResult> e = tbl.elements();

            while (e.hasMoreElements()) {
               BOpcDeviceDiscoveryResult r = e.nextElement();
               this.add(SlotPath.escape(r.getDescription()) + "?", r);
            }

            this.progress(100);
            this.success();
         } catch (Exception var16) {
            String msg = this.getNetwork().lex("opcServerDiscovery.failureGettingServerListFor");
            this.opcLog.message(msg + " " + addr + ": " + var16.toString());
            this.failed(var16);
         }
      } finally {
         if (this.isCanceled()) {
            this.opcLog.trace("Device discovery job::user cancelled");
            this.canceled();
         }
      }
   }
}
