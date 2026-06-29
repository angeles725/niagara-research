package com.tridium.lonworks.xml;

import java.util.Vector;

public class XLonDevice extends XLonData {
   public XDeviceData deviceData;
   public XDeviceFacets deviceFacets = null;
   public Vector<XNetworkVariable> nvs = new Vector<>();
   public Vector<XNetworkConfig> ncs = new Vector<>();
   public Vector<XMessageTag> mtags = new Vector<>();
   public Vector<XConfigProperty> cfgs = new Vector<>();
   public boolean hasReadOnlyFile = false;
   public boolean useZeroBasedArrays = false;

   @Override
   public void addAttribute(String name, Object obj) {
      if (obj instanceof XNetworkVariable) {
         this.nvs.addElement((XNetworkVariable)obj);
      } else if (obj instanceof XNetworkConfig) {
         this.ncs.addElement((XNetworkConfig)obj);
      } else if (obj instanceof XMessageTag) {
         this.mtags.addElement((XMessageTag)obj);
      } else if (obj instanceof XConfigProperty) {
         this.cfgs.addElement((XConfigProperty)obj);
      }
   }

   public XNetworkVariable[] getNetworkVariables() {
      XNetworkVariable[] a = new XNetworkVariable[this.nvs.size()];
      this.nvs.copyInto(a);
      return a;
   }

   public XNetworkConfig[] getNetworkConfigs() {
      XNetworkConfig[] a = new XNetworkConfig[this.ncs.size()];
      this.ncs.copyInto(a);
      return a;
   }

   public XMessageTag[] getMessageTags() {
      XMessageTag[] a = new XMessageTag[this.mtags.size()];
      this.mtags.copyInto(a);
      return a;
   }

   public XConfigProperty[] getConfigProperties() {
      XConfigProperty[] a = new XConfigProperty[this.cfgs.size()];
      this.cfgs.copyInto(a);
      return a;
   }
}
