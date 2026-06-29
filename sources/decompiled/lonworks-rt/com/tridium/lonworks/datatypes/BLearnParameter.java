package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.util.LonStringUtil;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "unmanagedNetwork",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "subnetNode",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT",
      flags = 4
   ), @NiagaraProperty(
      name = "learnLinks",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "subnetNodeArray",
      type = "String",
      defaultValue = "",
      flags = 4
   ), @NiagaraProperty(
      name = "uploadConfigData",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "container",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 4
   )})
public class BLearnParameter extends BStruct {
   public static final Property unmanagedNetwork = newProperty(0, false, null);
   public static final Property subnetNode = newProperty(4, BSubnetNode.DEFAULT, null);
   public static final Property learnLinks = newProperty(0, false, null);
   public static final Property subnetNodeArray = newProperty(4, "", null);
   public static final Property uploadConfigData = newProperty(0, true, null);
   public static final Property container = newProperty(4, BOrd.NULL, null);
   public static final Type TYPE = Sys.loadType(BLearnParameter.class);

   public boolean getUnmanagedNetwork() {
      return this.getBoolean(unmanagedNetwork);
   }

   public void setUnmanagedNetwork(boolean v) {
      this.setBoolean(unmanagedNetwork, v, null);
   }

   public BSubnetNode getSubnetNode() {
      return (BSubnetNode)this.get(subnetNode);
   }

   public void setSubnetNode(BSubnetNode v) {
      this.set(subnetNode, v, null);
   }

   public boolean getLearnLinks() {
      return this.getBoolean(learnLinks);
   }

   public void setLearnLinks(boolean v) {
      this.setBoolean(learnLinks, v, null);
   }

   public String getSubnetNodeArray() {
      return this.getString(subnetNodeArray);
   }

   public void setSubnetNodeArray(String v) {
      this.setString(subnetNodeArray, v, null);
   }

   public boolean getUploadConfigData() {
      return this.getBoolean(uploadConfigData);
   }

   public void setUploadConfigData(boolean v) {
      this.setBoolean(uploadConfigData, v, null);
   }

   public BOrd getContainer() {
      return (BOrd)this.get(container);
   }

   public void setContainer(BOrd v) {
      this.set(container, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isSelectedDevices() {
      return !this.getSubnetNode().equals(BSubnetNode.DEFAULT) || this.getSubnetNodeArray().length() > 0;
   }

   public BLonDevice[] getDeviceList(BLonNetwork lon) {
      Array<BLonDevice> a = new Array(BLonDevice.class);
      BSubnetNode sn = this.getSubnetNode();
      if (!sn.equals(BSubnetNode.DEFAULT)) {
         a.add(lon.addressManager().getDeviceByAddress(sn));
      } else {
         BSubnetNode[] sns = (BSubnetNode[])LonStringUtil.getSimpleArray(this.getSubnetNodeArray(), BSubnetNode.DEFAULT);

         for (int i = 0; i < sns.length; i++) {
            a.add(lon.addressManager().getDeviceByAddress(sns[i]));
         }
      }

      return (BLonDevice[])a.trim();
   }
}
