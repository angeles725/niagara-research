package com.tridium.opc.client.point;

import com.tridium.opc.client.BOpcDaClient;
import com.tridium.opc.client.util.BOpcDataType;
import com.tridium.opc.jni.client.da.BrowseResult;
import com.tridium.opc.jni.client.da.OpcBrowseServerAddressSpace;
import com.tridium.opc.jni.client.da.OpcItemMgt;
import com.tridium.opc.jni.client.da.OpcItemProperties;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "id",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "folder",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "dataType",
      type = "BOpcDataType",
      defaultValue = "BOpcDataType.vtInt4"
   ), @NiagaraProperty(
      name = "mode",
      type = "BReadWriteMode",
      defaultValue = "BReadWriteMode.readWrite"
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.NULL"
   ), @NiagaraProperty(
      name = "kidCheck",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "actualdataType",
      type = "BOpcDataType",
      defaultValue = "BOpcDataType.vtInt4"
   )})
@NiagaraAction(
   name = "discover"
)
public class BOpcPointDiscoveryResult extends BComponent {
   public static final Property id = newProperty(0, "", null);
   public static final Property folder = newProperty(0, false, null);
   public static final Property dataType = newProperty(0, BOpcDataType.vtInt4, null);
   public static final Property mode = newProperty(0, BReadWriteMode.readWrite, null);
   public static final Property facets = newProperty(0, BFacets.NULL, null);
   public static final Property kidCheck = newProperty(0, false, null);
   public static final Property actualdataType = newProperty(0, BOpcDataType.vtInt4, null);
   public static final Action discover = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BOpcPointDiscoveryResult.class);
   protected BOpcPointDeviceExt ext;
   Log opcLog = Log.getLog("OpcDaLog");

   public String getId() {
      return this.getString(id);
   }

   public void setId(String v) {
      this.setString(id, v, null);
   }

   public boolean getFolder() {
      return this.getBoolean(folder);
   }

   public void setFolder(boolean v) {
      this.setBoolean(folder, v, null);
   }

   public BOpcDataType getDataType() {
      return (BOpcDataType)this.get(dataType);
   }

   public void setDataType(BOpcDataType v) {
      this.set(dataType, v, null);
   }

   public BReadWriteMode getMode() {
      return (BReadWriteMode)this.get(mode);
   }

   public void setMode(BReadWriteMode v) {
      this.set(mode, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public boolean getKidCheck() {
      return this.getBoolean(kidCheck);
   }

   public void setKidCheck(boolean v) {
      this.setBoolean(kidCheck, v, null);
   }

   public BOpcDataType getActualdataType() {
      return (BOpcDataType)this.get(actualdataType);
   }

   public void setActualdataType(BOpcDataType v) {
      this.set(actualdataType, v, null);
   }

   public void discover() {
      this.invoke(discover, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void doDiscover() {
      this.setKidCheck(true);
      if (this.ext != null) {
         BOpcDaClient server = this.ext.getDaClient();
         BrowseResult[] res = null;

         try {
            if (server.getState().isDisengaged()) {
               return;
            }

            this.opcLog.trace("Browsing - " + this.getId());
            if (res == null || res.length == 0) {
               try {
                  OpcBrowseServerAddressSpace opc2 = server.getPeer().getBrowseServerAddressSpace();
                  if (opc2 != null) {
                     OpcItemMgt mgt = this.ext.getPeer().getItemMgt();
                     res = opc2.browse(this.getPath(), mgt);
                     opc2.release();
                     mgt.release();
                  }
               } catch (Exception var7) {
                  this.opcLog.trace("IOPCBrowseServerAddressSpace browse_down failed", var7);
               }
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
                  } else {
                     br.setId(res[i].getId());
                     BOpcPointDiscoveryJob.prepare(br, res[i], props);
                     br.setActualdataType(BOpcDataType.make(res[i].getDataType()));
                     this.add(SlotPath.escape(res[i].getName()) + "?", br);
                  }
               } else {
                  br.setId(res[i].getName());
                  this.add(SlotPath.escape(res[i].getName()) + "?", br);
               }
            }

            props.release();
         } catch (Exception var8) {
            this.opcLog.message("Discovery failure", var8);
         }
      }
   }

   protected String[] getPath() {
      int len = 0;

      for (BComplex cur = this; cur instanceof BOpcPointDiscoveryResult; cur = cur.getParent()) {
         len++;
      }

      String[] ret = new String[len];

      for (BComplex var4 = this; --len >= 0; var4 = var4.getParent()) {
         ret[len] = ((BOpcPointDiscoveryResult)var4).getId();
      }

      return ret;
   }

   protected void setMode(boolean read, boolean write) {
      if (read && write) {
         this.setMode(BReadWriteMode.readWrite);
      } else if (write) {
         this.setMode(BReadWriteMode.writeonly);
      } else {
         this.setMode(BReadWriteMode.readonly);
      }
   }
}
