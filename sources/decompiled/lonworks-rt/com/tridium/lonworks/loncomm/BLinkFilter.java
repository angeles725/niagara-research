package com.tridium.lonworks.loncomm;

import com.tridium.lonworks.datatypes.BLinkFilterEntry;
import com.tridium.lonworks.datatypes.BLinkFilterTable;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.io.LonLinkLayer;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enableSubnetNode",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "subnetNode",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT"
   ), @NiagaraProperty(
      name = "enableDevice",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "deviceName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "enableSelector",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "selector",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.make(BFacets.MIN,BInteger.make(0),BFacets.MAX,BInteger.make(0x3fff))")}
   ), @NiagaraProperty(
      name = "maxEntries",
      type = "int",
      defaultValue = "1000"
   ), @NiagaraProperty(
      name = "entries",
      type = "BLinkFilterTable",
      defaultValue = "new BLinkFilterTable()",
      flags = 7
   ), @NiagaraProperty(
      name = "toStandardOut",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "includeCompletionEvents",
      type = "boolean",
      defaultValue = "false"
   )})
@NiagaraAction(
   name = "clearTable"
)
public class BLinkFilter extends BComponent implements LonLinkListenerRegistry.LinkListener {
   public static final Property enableSubnetNode = newProperty(0, false, null);
   public static final Property subnetNode = newProperty(0, BSubnetNode.DEFAULT, null);
   public static final Property enableDevice = newProperty(0, false, null);
   public static final Property deviceName = newProperty(0, "", null);
   public static final Property enableSelector = newProperty(0, false, null);
   public static final Property selector = newProperty(0, 0, BFacets.make("min", BInteger.make(0), "max", BInteger.make(16383)));
   public static final Property maxEntries = newProperty(0, 1000, null);
   public static final Property entries = newProperty(7, new BLinkFilterTable(), null);
   public static final Property toStandardOut = newProperty(0, false, null);
   public static final Property includeCompletionEvents = newProperty(0, false, null);
   public static final Action clearTable = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BLinkFilter.class);
   LonLinkListenerRegistry llink = null;
   BLonDevice dev = null;
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/linkFilter.png");

   public boolean getEnableSubnetNode() {
      return this.getBoolean(enableSubnetNode);
   }

   public void setEnableSubnetNode(boolean v) {
      this.setBoolean(enableSubnetNode, v, null);
   }

   public BSubnetNode getSubnetNode() {
      return (BSubnetNode)this.get(subnetNode);
   }

   public void setSubnetNode(BSubnetNode v) {
      this.set(subnetNode, v, null);
   }

   public boolean getEnableDevice() {
      return this.getBoolean(enableDevice);
   }

   public void setEnableDevice(boolean v) {
      this.setBoolean(enableDevice, v, null);
   }

   public String getDeviceName() {
      return this.getString(deviceName);
   }

   public void setDeviceName(String v) {
      this.setString(deviceName, v, null);
   }

   public boolean getEnableSelector() {
      return this.getBoolean(enableSelector);
   }

   public void setEnableSelector(boolean v) {
      this.setBoolean(enableSelector, v, null);
   }

   public int getSelector() {
      return this.getInt(selector);
   }

   public void setSelector(int v) {
      this.setInt(selector, v, null);
   }

   public int getMaxEntries() {
      return this.getInt(maxEntries);
   }

   public void setMaxEntries(int v) {
      this.setInt(maxEntries, v, null);
   }

   public BLinkFilterTable getEntries() {
      return (BLinkFilterTable)this.get(entries);
   }

   public void setEntries(BLinkFilterTable v) {
      this.set(entries, v, null);
   }

   public boolean getToStandardOut() {
      return this.getBoolean(toStandardOut);
   }

   public void setToStandardOut(boolean v) {
      this.setBoolean(toStandardOut, v, null);
   }

   public boolean getIncludeCompletionEvents() {
      return this.getBoolean(includeCompletionEvents);
   }

   public void setIncludeCompletionEvents(boolean v) {
      this.setBoolean(includeCompletionEvents, v, null);
   }

   public void clearTable() {
      this.invoke(clearTable, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.getLinkLayer().registerLinkListener(this);
   }

   public void stopped() throws Exception {
      super.stopped();
      this.getLinkLayer().unregisterLinkListener(this);
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      this.dev = null;
   }

   public void doClearTable() {
      this.getEntries().clearEntries();
   }

   @Override
   public void receive(NAppBuffer msg) {
      if (this.passFilter(msg, true)) {
         this.display(msg, true);
      }
   }

   @Override
   public void send(NAppBuffer msg) {
      if (this.passFilter(msg, false)) {
         this.display(msg, false);
      }
   }

   private boolean passFilter(NAppBuffer msg, boolean rcv) {
      if (this.isCompletionEvent(msg) && !this.getIncludeCompletionEvents()) {
         return false;
      } else {
         boolean pass = false;
         if (this.getEnableSubnetNode()) {
            if (this.dev == null) {
               this.dev = NmUtil.getLonNetwork(this).addressManager().getDeviceByAddress(this.getSubnetNode());
            }

            if (!this.passDeviceFilter(this.dev, msg, rcv)) {
               return false;
            }

            pass = true;
         }

         if (this.getEnableDevice()) {
            if (this.dev == null) {
               this.dev = NmUtil.getLonNetwork(this).addressManager().getDeviceByName(this.getDeviceName());
            }

            if (!this.passDeviceFilter(this.dev, msg, rcv)) {
               return false;
            }

            pass = true;
         }

         if (this.getEnableSelector()) {
            if (msg.getMessageCode() < 128) {
               return false;
            }

            byte[] a = msg.getWriteBuffer();
            int sel = (a[16] & 63) << 8 | a[17] & 255;
            if (sel != this.getSelector()) {
               return false;
            }

            pass = true;
         }

         return pass;
      }
   }

   private boolean isCompletionEvent(NAppBuffer msg) {
      return msg.isCompletionEvent() && !msg.isResp();
   }

   private boolean passDeviceFilter(BLonDevice dev, NAppBuffer msg, boolean rcv) {
      if (dev == null) {
         return false;
      } else {
         if (rcv && !this.isCompletionEvent(msg)) {
            BSubnetNode sn = msg.getSourceAddress();
            if (!sn.equals(dev.getSubnetNodeAddress())) {
               return false;
            }
         } else {
            byte[] buf = msg.getReadBuffer();
            int typ = buf[5];
            if (typ == 1) {
               BSubnetNode sn = BSubnetNode.make(buf[9], buf[6] & 127);
               if (!sn.equals(dev.getSubnetNodeAddress())) {
                  return false;
               }
            } else {
               if (typ != 2) {
                  return false;
               }

               byte[] na = new byte[6];
               System.arraycopy(buf, 10, na, 0, 6);
               BNeuronId nid = BNeuronId.make(na);
               if (!nid.equals(dev.getNeuronIdAddress())) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void display(NAppBuffer msg, boolean rcv) {
      if (this.getToStandardOut()) {
         this.toStandardOut(msg, rcv);
      } else {
         BLinkFilterTable et = this.getEntries();
         if (et.getMaxIndex() < this.getMaxEntries()) {
            BAbsTime ts = BAbsTime.make(System.currentTimeMillis());
            BBlob bl = BBlob.make(msg.getReadBuffer(), 0, msg.getWriteBufferLen());
            this.getEntries().addEntry(new BLinkFilterEntry(rcv, ts, bl));
         }
      }
   }

   private void toStandardOut(NAppBuffer msg, boolean rcv) {
      byte[] a = msg.getWriteBuffer();
      if (rcv) {
         this.getLinkLayer().writeLinkDebug("recv: ", a, a[1] + 2);
      } else {
         this.getLinkLayer().writeLinkDebug("send: ", a, a[1] + 2);
      }
   }

   LonLinkListenerRegistry getLinkLayer() {
      if (this.llink == null) {
         LonLinkLayer lnk = ((NLonComm)NmUtil.getLonNetwork(this).lonComm()).linkLayer;
         if (lnk instanceof ListenerSupport) {
            this.llink = ((ListenerSupport)lnk).getLonLinkListenerRegistry();
         }
      }

      return this.llink;
   }

   public BIcon getIcon() {
      return icon;
   }
}
