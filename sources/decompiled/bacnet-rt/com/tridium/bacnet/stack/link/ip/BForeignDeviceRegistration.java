package com.tridium.bacnet.stack.link.ip;

import com.tridium.bacnet.stack.network.BNetworkPort;
import java.net.InetAddress;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.log.Log;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "false",
      flags = 64
   ), @NiagaraProperty(
      name = "bbmdAddress",
      type = "String",
      defaultValue = "BForeignDeviceRegistration.BBMD_ADDRESS_DEFAULT"
   ), @NiagaraProperty(
      name = "registrationLifetime",
      type = "BRelTime",
      defaultValue = "BRelTime.make(15 * BRelTime.MILLIS_IN_MINUTE)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(BRelTime.MILLIS_IN_MINUTE)"
      )}
   )})
@NiagaraActions({@NiagaraAction(
      name = "registerWithBBMD"
   ), @NiagaraAction(
      name = "unregisterWithBBMD"
   )})
public class BForeignDeviceRegistration extends BComponent {
   public static final Property enabled = newProperty(64, false, null);
   public static final Property bbmdAddress = newProperty(0, "null", null);
   public static final Property registrationLifetime = newProperty(0, BRelTime.make(900000L), BFacets.make("min", BRelTime.make(60000L)));
   public static final Action registerWithBBMD = newAction(0, null);
   public static final Action unregisterWithBBMD = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BForeignDeviceRegistration.class);
   private String oldAddress = null;
   private Ticket registrationTicket = null;
   private static final int REG_OVERLAP_MS = 30000;
   private static final String BBMD_ADDRESS_DEFAULT = "null";
   private static final Log log = Log.getLog("bacnet.link.ip");
   private static Object FD_TICKET_LOCK = new Object();

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public String getBbmdAddress() {
      return this.getString(bbmdAddress);
   }

   public void setBbmdAddress(String v) {
      this.setString(bbmdAddress, v, null);
   }

   public BRelTime getRegistrationLifetime() {
      return (BRelTime)this.get(registrationLifetime);
   }

   public void setRegistrationLifetime(BRelTime v) {
      this.set(registrationLifetime, v, null);
   }

   public void registerWithBBMD() {
      this.invoke(registerWithBBMD, null, null);
   }

   public void unregisterWithBBMD() {
      this.invoke(unregisterWithBBMD, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BForeignDeviceRegistration() {
   }

   public BForeignDeviceRegistration(String bbmdAddress) {
      this(bbmdAddress, null);
   }

   public BForeignDeviceRegistration(String bbmdAddress, BRelTime registrationLifetime) {
      this.setBbmdAddress(bbmdAddress);
      this.setRegistrationLifetime(registrationLifetime);
   }

   public BBacnetIpLinkLayer linklayer() {
      return (BBacnetIpLinkLayer)this.getParent();
   }

   public BNetworkPort port() {
      return (BNetworkPort)this.linklayer().getParent();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetIpLinkLayer;
   }

   public void started() throws Exception {
      if (this.isRunning() && Sys.atSteadyState()) {
         this.doRegisterWithBBMD();
      }
   }

   public void stopped() throws Exception {
      if (this.isRunning() && this.getEnabled()) {
         this.doUnregisterWithBBMD();
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(enabled) && cx != BacnetConst.noWrite) {
            if (this.getEnabled()) {
               this.doRegisterWithBBMD();
            } else {
               this.doUnregisterWithBBMD();
            }
         } else if (p.equals(bbmdAddress)) {
            if (this.oldAddress != null) {
               this.unregister(this.oldAddress);
               this.doRegisterWithBBMD();
            }
         } else if (p.equals(registrationLifetime) && this.getEnabled()) {
            this.cancelTicket();
            this.startTicket();
            this.doRegisterWithBBMD();
         }
      }
   }

   public void doRegisterWithBBMD() {
      this.register(this.getBbmdAddress());
   }

   public void doUnregisterWithBBMD() {
      this.unregister(this.getBbmdAddress());
   }

   private void startTicket() {
      synchronized (FD_TICKET_LOCK) {
         if (this.registrationTicket != null) {
            this.registrationTicket.cancel();
         }

         this.registrationTicket = Clock.schedulePeriodically(this, this.getOverlappingInterval(), registerWithBBMD, null);
      }
   }

   private void cancelTicket() {
      synchronized (FD_TICKET_LOCK) {
         if (this.registrationTicket != null) {
            this.registrationTicket.cancel();
            this.registrationTicket = null;
         }
      }
   }

   private boolean ticketExpired() {
      return this.registrationTicket == null || this.registrationTicket.isExpired();
   }

   private BRelTime getOverlappingInterval() {
      return BRelTime.make(Math.max(this.getRegistrationLifetime().getMillis() - 30000L, 30000L));
   }

   protected void register(String address) {
      this.oldAddress = address;
      if (BBacnetNetwork.bacnet().getEnabled()) {
         try {
            if (log.isTraceOn()) {
               log.trace("Registering with BBMD at IP:" + address);
            }

            this.sendBvll(address, new RegisterForeignDevice(this.getRegistrationLifetime().getSeconds()));
            this.startTicket();
            this.set(enabled, BBoolean.TRUE, BacnetConst.noWrite);
         } catch (Exception var3) {
            log.warning("Unable to send foreign device registration", var3);
         }
      }
   }

   protected void unregister(String address) {
      this.cancelTicket();
      if (BBacnetNetwork.bacnet().getEnabled()) {
         this.sendBvll(address, new DeleteForeignDeviceTableEntry(this.getLocalMacAddress()));
         this.set(enabled, BBoolean.FALSE, BacnetConst.noWrite);
      }
   }

   protected void sendBvll(BvllMessage message) {
      this.sendBvll(this.getBbmdAddress(), message);
   }

   protected void sendBvll(String address, BvllMessage message) {
      try {
         byte[] bbmdMacAddr = this.getMacBytes(address);
         if (bbmdMacAddr != null && bbmdMacAddr.length == 6) {
            InetAddress addr = this.linklayer().lookupInetAddr(bbmdMacAddr);
            if (addr != null) {
               int port = BBacnetIpLinkLayer.getPort(bbmdMacAddr);
               if (log.isTraceOn()) {
                  log.trace("Sending BVLL message to " + address);
               }

               this.sendBvllMessage(addr, port, message);
            }
         } else if (log.isTraceOn()) {
            log.trace("unable Sending BVLL message to " + address);
         }
      } catch (Exception var6) {
         log.warning("Unable to send bvll message", var6);
      }
   }

   protected void sendBvllMessage(InetAddress inet, int port, BvllMessage msg) {
      this.linklayer().sendBvllMessage(inet, port, msg);
   }

   protected byte[] getMacBytes(String address) {
      return BBacnetIpLinkLayer.getMacBytes(address);
   }

   protected byte[] getLocalMacAddress() {
      return this.linklayer().getMac();
   }
}
