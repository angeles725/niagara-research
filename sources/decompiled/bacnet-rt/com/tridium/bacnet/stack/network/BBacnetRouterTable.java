package com.tridium.bacnet.stack.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "addRouter",
      parameterType = "BBacnetRouterEntry",
      defaultValue = "new BBacnetRouterEntry()"
   ), @NiagaraAction(
      name = "removeRouter",
      parameterType = "BInteger",
      defaultValue = "BInteger.make(-1)"
   ), @NiagaraAction(
      name = "purgeExpiredEntries",
      flags = 4
   ), @NiagaraAction(
      name = "purgeAllEntries",
      flags = 128
   )})
public class BBacnetRouterTable extends BComponent {
   public static final Action addRouter = newAction(0, new BBacnetRouterEntry(), null);
   public static final Action removeRouter = newAction(0, BInteger.make(-1), null);
   public static final Action purgeExpiredEntries = newAction(4, null);
   public static final Action purgeAllEntries = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BBacnetRouterTable.class);
   Ticket routerEntryRemovalTicket = null;
   private static final Log logger = Log.getLog("bacnet.network");

   public void addRouter(BBacnetRouterEntry parameter) {
      this.invoke(addRouter, parameter, null);
   }

   public void removeRouter(BInteger parameter) {
      this.invoke(removeRouter, parameter, null);
   }

   public void purgeExpiredEntries() {
      this.invoke(purgeExpiredEntries, null, null);
   }

   public void purgeAllEntries() {
      this.invoke(purgeAllEntries, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.routerEntryRemovalTicket = Clock.schedulePeriodically(this, BRelTime.makeHours(1), purgeExpiredEntries, null);
   }

   public void added(Property p, Context cx) {
      if (this.isRunning() && cx != Context.decoding) {
         this.validate();
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && cx != Context.decoding) {
         this.validate();
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetworkLayer;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      long ticks = Clock.ticks();
      BAbsTime now = BAbsTime.make(ticks);
      out.prop("ticks", "" + ticks);
      out.prop("->now", now);
      out.prop("routerEntryRemovalTicket", this.routerEntryRemovalTicket);
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BBacnetRouterEntry.class)) {
         BBacnetRouterEntry e = (BBacnetRouterEntry)sc.get();
         out.prop(e.getName(), "lastUpdateTicks=" + e.lastUpdateTicks);
      }

      out.endProps();
   }

   public void doAddRouter(BBacnetRouterEntry e) {
      BNetworkPort port = this.net().getPortByNetwork(e.getRouterAddress().getNetworkNumber());
      if (port != null) {
         e.setPortId(port.getPortId());
      }

      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetRouterEntry.class)) {
         BBacnetRouterEntry cur = (BBacnetRouterEntry)c.get();
         if (e.getDnet() == cur.getDnet()) {
            cur.copyFrom(e);
            long now = Clock.ticks();
            cur.lastUpdateTicks = now;
            return;
         }
      }

      this.add("dnet" + e.getDnet(), e);
   }

   public void doRemoveRouter(BInteger dnet) {
      BBacnetRouterEntry e = this.getRouterByDnet(dnet.getInt());
      if (e != null) {
         this.remove(e);
      }
   }

   public void doPurgeExpiredEntries() {
      long now = Clock.ticks();
      long lifetime = this.net().getRouterEntryLifetime().getMillis();
      long tooOldTime = now - lifetime;
      if (tooOldTime < 0L) {
         tooOldTime = 0L;
      }

      if (BBacnetNetworkLayer.logger.isLoggable(Level.FINE)) {
         BBacnetNetworkLayer.logger.fine("Purging expired router entries: now=" + now + " lifetime=" + lifetime + " tooOldTime=" + tooOldTime);
      }

      BBacnetRouterEntry[] entries = (BBacnetRouterEntry[])this.getChildren(BBacnetRouterEntry.class);

      for (int i = 0; i < entries.length; i++) {
         BBacnetRouterEntry e = entries[i];
         if (!this.net().isDirectlyConnectedNetwork(e.getDnet()) && e.lastUpdateTicks < tooOldTime) {
            this.remove(e);
         }
      }
   }

   public void doPurgeAllEntries() {
      BBacnetNetworkLayer.logger.info("Purging all router table entries (except local ports)...");
      BBacnetRouterEntry[] entries = (BBacnetRouterEntry[])this.getChildren(BBacnetRouterEntry.class);

      for (int i = 0; i < entries.length; i++) {
         BBacnetRouterEntry e = entries[i];
         if (!this.net().isDirectlyConnectedNetwork(e.getDnet())) {
            this.remove(e);
         }
      }
   }

   public void removeRouterByDnet(int dnet) {
      this.removeRouter(BInteger.make(dnet));
   }

   public void removeRouterByPortId(int portId) {
      BBacnetRouterEntry[] list = (BBacnetRouterEntry[])this.getChildren(BBacnetRouterEntry.class);

      for (int i = 0; i < list.length; i++) {
         if (list[i].getPortId() == portId) {
            this.remove(list[i]);
         }
      }
   }

   private BBacnetNetworkLayer net() {
      return (BBacnetNetworkLayer)this.getParent();
   }

   public BBacnetRouterEntry getRouterByDnet(int dnet) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetRouterEntry.class)) {
         BBacnetRouterEntry e = (BBacnetRouterEntry)c.get();
         if (e.getDnet() == dnet) {
            return e;
         }
      }

      return null;
   }

   public Vector<BBacnetRouterEntry> getRoutersByAddress(int networkNumber, byte[] macAddress) {
      Vector<BBacnetRouterEntry> v = new Vector<>();
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetRouterEntry.class)) {
         BBacnetRouterEntry e = (BBacnetRouterEntry)c.get();
         if (e.getRouterAddress().equals(networkNumber, macAddress)) {
            v.addElement(e);
         }
      }

      return v;
   }

   public int[] getDnets(int excludedPortId) {
      this.loadSlots();
      ArrayList<Integer> v = new ArrayList<>();
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetRouterEntry.class)) {
         BBacnetRouterEntry e = (BBacnetRouterEntry)c.get();
         e.loadSlots();
         if (e.getPortId() != excludedPortId) {
            v.add(e.getDnet());
         }
      }

      int[] dnets = new int[v.size()];

      for (int i = 0; i < dnets.length; i++) {
         dnets[i] = v.get(i);
      }

      return dnets;
   }

   public void updateRouter(int dnet, int routerNetworkNumber, byte[] routerMacAddress, int portId, String portInfo, BRouterStatus routerStatus) {
      BBacnetRouterEntry e = this.getRouterByDnet(dnet);
      if (e == null) {
         e = new BBacnetRouterEntry(dnet, new BBacnetAddress(routerNetworkNumber, routerMacAddress), portId, portInfo, routerStatus);
         if (logger.isLoggable(0)) {
            logger.trace("Adding new router entry: " + e);
         }

         this.addRouter(e);
      } else {
         if (logger.isLoggable(0)
            && (
               e.getRouterAddress().getNetworkNumber() != routerNetworkNumber
                  || !Arrays.equals(e.getRouterAddress().getMacAddress().getBytes(), routerMacAddress)
                  || e.getPortId() != portId
                  || !e.getRouterStatus().equals(routerStatus)
            )) {
            logger.trace(
               "Updating router entry: "
                  + e
                  + "; new networkNumber: "
                  + routerNetworkNumber
                  + "; new address: "
                  + ByteArrayUtil.toHexString(routerMacAddress)
                  + "; new port ID: "
                  + portId
                  + "; new port info: "
                  + portInfo
                  + "; new router status: "
                  + routerStatus
            );
         }

         e.getRouterAddress().setNetworkNumber(routerNetworkNumber);
         e.getRouterAddress().setMac(routerMacAddress, null);
         e.setPortId(portId);
         if (portInfo != null) {
            e.setPortInfo(portInfo);
         }

         e.setRouterStatus(routerStatus);
      }

      if (!this.net().isDirectlyConnectedNetwork(dnet)) {
         e.lastUpdateTicks = Clock.ticks();
      }
   }

   public synchronized void validate() {
      if (logger.isTraceOn()) {
         logger.trace("Validating router table (sc=" + this.getSlotCount() + ")");
      }

      boolean tableOk = true;
      int numPasses = 0;
      if (this.getSlotCount() >= 6) {
         do {
            BBacnetRouterEntry[] rt = (BBacnetRouterEntry[])this.getChildren(BBacnetRouterEntry.class);

            for (int i = 0; i < rt.length; i++) {
               for (int j = i + 1; j < rt.length; j++) {
                  if (rt[i].getDnet() == rt[j].getRouterAddress().getNetworkNumber() && rt[i].getRouterAddress().getNetworkNumber() == rt[j].getDnet()) {
                     logger.error("Conflict detected between router table entries " + i + " and " + j + "- removing entry " + j + "...");
                     tableOk = false;
                     this.remove(rt[j]);
                     break;
                  }
               }

               if (!tableOk) {
                  break;
               }
            }
         } while (!tableOk && ++numPasses < this.getSlotCount());
      }
   }
}
