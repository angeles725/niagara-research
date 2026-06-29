package com.tridium.opc.client.point;

import com.tridium.opc.client.BOpcDaClient;
import com.tridium.opc.client.util.BOpcDataType;
import com.tridium.opc.client.util.BOpcDiscoveryJob;
import com.tridium.opc.jni.client.da.BrowseResult;
import com.tridium.opc.jni.client.da.OpcBrowseServerAddressSpace;
import com.tridium.opc.jni.client.da.OpcItem;
import com.tridium.opc.jni.client.da.OpcItemMgt;
import com.tridium.opc.jni.client.da.OpcItemProperties;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcPointDiscoveryJob extends BOpcDiscoveryJob {
   public static final Type TYPE = Sys.loadType(BOpcPointDiscoveryJob.class);
   protected BValue args;
   protected BOpcPointDeviceExt ext;
   private static int OPC_NS_FLAT = 2;
   private static int OPC_NS_HIERARCHIAL = 1;
   Log opcLog = Log.getLog("OpcDaLog");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BOpcPointDiscoveryJob() {
   }

   public BOpcPointDiscoveryJob(BOpcPointDeviceExt ext, BValue args) {
      super(ext.getOpcNetwork());
      this.ext = ext;
      this.args = args;
   }

   @Override
   public void doDiscover() {
      this.opcLog.trace("Point discovery job - do discover");
      this.removeAll();
      BOpcDaClient server = this.ext.getDaClient();
      BrowseResult[] res = null;
      boolean bFlatDiscovery = false;

      try {
         if (server.getState().isDisengaged()) {
            this.opcLog.trace("Point discovery ::" + server.getDisplayName(null) + " is down");
            throw new IllegalStateException(server.getDisplayName(null) + " is down.");
         }

         if (res == null || res.length == 0) {
            try {
               OpcBrowseServerAddressSpace opc2 = server.getPeer().getBrowseServerAddressSpace();
               if (opc2 != null) {
                  if (OPC_NS_FLAT == opc2.queryOrganization()) {
                     this.opcLog.trace("Server supports flat discovery");
                     bFlatDiscovery = true;
                  } else {
                     this.opcLog.trace("Server supports hierarchical discovery");
                  }

                  if (!bFlatDiscovery && !this.ext.getFlatDiscovery()) {
                     OpcItemMgt mgt = this.ext.getPeer().getItemMgt();
                     res = opc2.browse(null, mgt);
                     opc2.release();
                     mgt.release();
                  } else {
                     OpcItemMgt mgt = this.ext.getPeer().getItemMgt();
                     this.opcLog.trace("Server supports hierarchical discovery");
                     res = opc2.browseAllItems(mgt);
                     opc2.release();
                     mgt.release();
                  }
               } else {
                  this.opcLog.trace("Server doesnot support Browse Server Address space interface");
               }
            } catch (Exception var8) {
               this.opcLog.trace("IOPCBrowseServerAddressSpace browse_down failed", var8);
            }
         }

         this.progress(50);
         if (this.isCanceled()) {
            return;
         }

         OpcItemProperties props = server.getPeer().getItemProperties();
         int len = 0;
         if (res != null) {
            len = res.length;
         }

         for (int i = 0; i < len; i++) {
            BOpcPointDiscoveryResult br = new BOpcPointDiscoveryResult();
            br.setFolder(!res[i].isItem());
            br.ext = this.ext;
            if (res[i].isItem()) {
               if (!BOpcDataType.isSupported(res[i].getDataType()) && res[i].getDataType() != 0) {
                  this.opcLog.message(res[i].getId() + " is an unsupported datatype: " + res[i].getDataType());
               } else if (res[i].getId() != null) {
                  br.setId(res[i].getId());
                  prepare(br, res[i], props);
                  this.add(SlotPath.escape(res[i].getName()), br);
               }

               if (this.isCanceled()) {
                  return;
               }
            } else {
               br.setId(res[i].getName());
               this.add(SlotPath.escape(res[i].getName()), br);
            }
         }

         props.release();
         this.progress(100);
         this.success();
      } catch (Exception var9) {
         this.progress(100);
         this.failed(var9);
      }
   }

   protected static void prepare(BOpcPointDiscoveryResult dr, BrowseResult br, OpcItemProperties props) throws Exception {
      try {
         dr.setDataType(BOpcDataType.make(br.getDataType()));
      } catch (Exception var8) {
         dr.setDataType(BOpcDataType.vtEmpty);
      }

      try {
         dr.setMode(br.isReadable(), br.isWritable());
         if (props != null) {
            OpcItem it = br.getItem(props);
            BFacets oldFacets = dr.getFacets();
            BOpcDataType dataType = dr.getDataType();
            BFacets facets = setItemPropertiesAsFacets(oldFacets, dataType, it, props);
            if (!facets.isNull()) {
               dr.setFacets(facets);
            }
         }
      } catch (Exception var7) {
         System.out.println("Error reading properties of " + br.getId());
      }
   }

   public static BFacets setItemPropertiesAsFacets(BFacets oldFacets, BOpcDataType dataType, OpcItem it, OpcItemProperties props) {
      OpcItem.Property[] ary = it.getProperties();
      int len = ary.length;
      BFacets facets = oldFacets;

      for (int j = 0; j < len; j++) {
         try {
            if (ary[j].pid == 104) {
               facets = BFacets.make(facets, "max", BDouble.make(props.getNumericProperty(it, ary[j])));
            } else if (ary[j].pid == 105) {
               facets = BFacets.make(facets, "min", BDouble.make(props.getNumericProperty(it, ary[j])));
            } else if (ary[j].pid == 106) {
               if (props.getStringProperty(it, ary[j]) != null) {
                  facets = BFacets.make(facets, "trueText", BString.make(props.getStringProperty(it, ary[j])));
               }
            } else if (ary[j].pid == 107) {
               facets = BFacets.make(facets, "falseText", BString.make(props.getStringProperty(it, ary[j])));
            } else if (ary[j].pid >= 9) {
               if (BOpcDataType.isNumeric(ary[j].dataType)) {
                  facets = BFacets.make(facets, SlotPath.escape(ary[j].description), BDouble.make(props.getNumericProperty(it, ary[j])));
               } else if (BOpcDataType.isBoolean(ary[j].dataType)) {
                  facets = BFacets.make(facets, SlotPath.escape(ary[j].description), BBoolean.make(props.getBooleanProperty(it, ary[j])));
               } else if (BOpcDataType.isString(ary[j].dataType)) {
                  facets = BFacets.make(facets, SlotPath.escape(ary[j].description), BString.make(props.getStringProperty(it, ary[j])));
               }
            }
         } catch (Exception var9) {
            System.out.println("Ignoring " + it + ", " + ary[j]);
            var9.printStackTrace();
         }
      }

      if (dataType.isNumeric()) {
         if (facets.get("max") == null) {
            facets = BFacets.make(facets, "max", BDouble.make(BOpcDataType.getMax(dataType.getOrdinal())));
         }

         if (facets.get("min") == null) {
            facets = BFacets.make(facets, "min", BDouble.make(BOpcDataType.getMin(dataType.getOrdinal())));
         }
      }

      return facets;
   }

   public BOpcDaClient getOpcClient() {
      return this.ext != null ? this.ext.getDaClient() : null;
   }
}
