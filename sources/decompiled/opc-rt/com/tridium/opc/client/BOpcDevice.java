package com.tridium.opc.client;

import com.tridium.opc.client.util.BOpcPollScheduler;
import com.tridium.opc.client.util.BOpcState;
import javax.baja.driver.BDevice;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "address",
      type = "String",
      defaultValue = "localhost",
      flags = 72
   ), @NiagaraProperty(
      name = "classId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "local",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "useVersionIndependentProgId",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "programId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "versionIndependentProgramId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "pollScheduler",
      type = "BOpcPollScheduler",
      defaultValue = "new BOpcPollScheduler()"
   ), @NiagaraProperty(
      name = "state",
      type = "BOpcState",
      defaultValue = "BOpcState.detached",
      flags = 3
   )})
@NiagaraActions({@NiagaraAction(
      name = "attach",
      flags = 20
   ), @NiagaraAction(
      name = "detach",
      flags = 20
   )})
public class BOpcDevice extends BDevice {
   public static final Property address = newProperty(72, "localhost", null);
   public static final Property classId = newProperty(0, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property local = newProperty(0, false, null);
   public static final Property useVersionIndependentProgId = newProperty(0, true, null);
   public static final Property programId = newProperty(0, "", null);
   public static final Property versionIndependentProgramId = newProperty(0, "", null);
   public static final Property pollScheduler = newProperty(0, new BOpcPollScheduler(), null);
   public static final Property state = newProperty(3, BOpcState.detached, null);
   public static final Action attach = newAction(20, null);
   public static final Action detach = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BOpcDevice.class);
   private BOpcNetwork network;
   private Thread hook;
   Log opcLog = Log.getLog("OpcDaLog");
   Log opcPingLog = Log.getLog("OpcDaPingLog");

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public String getClassId() {
      return this.getString(classId);
   }

   public void setClassId(String v) {
      this.setString(classId, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public boolean getLocal() {
      return this.getBoolean(local);
   }

   public void setLocal(boolean v) {
      this.setBoolean(local, v, null);
   }

   public boolean getUseVersionIndependentProgId() {
      return this.getBoolean(useVersionIndependentProgId);
   }

   public void setUseVersionIndependentProgId(boolean v) {
      this.setBoolean(useVersionIndependentProgId, v, null);
   }

   public String getProgramId() {
      return this.getString(programId);
   }

   public void setProgramId(String v) {
      this.setString(programId, v, null);
   }

   public String getVersionIndependentProgramId() {
      return this.getString(versionIndependentProgramId);
   }

   public void setVersionIndependentProgramId(String v) {
      this.setString(versionIndependentProgramId, v, null);
   }

   public BOpcPollScheduler getPollScheduler() {
      return (BOpcPollScheduler)this.get(pollScheduler);
   }

   public void setPollScheduler(BOpcPollScheduler v) {
      this.set(pollScheduler, v, null);
   }

   public BOpcState getState() {
      return (BOpcState)this.get(state);
   }

   public void setState(BOpcState v) {
      this.set(state, v, null);
   }

   public void attach() {
      this.invoke(attach, null, null);
   }

   public void detach() {
      this.invoke(detach, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void atSteadyState() throws Exception {
      super.atSteadyState();
      this.attach();
   }

   public final void changed(Property p, Context c) {
      super.changed(p, c);
      if (c != Context.decoding && p == status && this.isRunning()) {
         if (!this.isDown() && !this.isDisabled() && !this.isFault()) {
            if (this.getState().isDisengaged()) {
               this.opcLog.trace("changed()::Attaching..");
               this.attach();
            }
         } else if (this.getState().isEngaged()) {
            this.opcLog.trace("Changed()::Device is down/disabled/fatalfault. Detaching..");
            this.detach();
         }
      }
   }

   public void doAttach() {
   }

   public void doDetach() {
   }

   public void doPing() {
   }

   public final Type getNetworkType() {
      return BOpcNetwork.TYPE;
   }

   public final BOpcNetwork getOpcNetwork() {
      if (this.network == null) {
         for (BComplex cur = this.getParent(); cur != null; cur = cur.getParent()) {
            if (cur instanceof BOpcNetwork) {
               this.network = (BOpcNetwork)cur;
               break;
            }
         }
      }

      return this.network;
   }

   public final IFuture post(Action a, BValue arg, Context cx) {
      this.getOpcNetwork().enqueue(new Invocation(this, a, arg, cx));
      return null;
   }

   public final IFuture postPing() {
      this.getOpcNetwork().enqueue(new Invocation(this, ping, null, null));
      this.opcPingLog.trace("BOpcDevice::postping invoked");
      return null;
   }

   public final void started() throws Exception {
      this.network = null;
      if (this.hook == null) {
         try {
            BOpcDevice.OpcShutdownHook h = new BOpcDevice.OpcShutdownHook();
            Runtime.getRuntime().addShutdownHook(h);
            this.hook = h;
         } catch (Throwable var2) {
         }
      }

      super.started();
   }

   public final void stopped() throws Exception {
      try {
         if (this.getState().isEngaged()) {
            this.opcLog.trace("Stopped():: Device is stopping. Calling detach()");
            this.doDetach();
         }

         if (this.hook != null) {
            Runtime.getRuntime().removeShutdownHook(this.hook);
            this.opcLog.trace("stopped()::Removing shutdown hook");
            this.hook = null;
         }
      } catch (Exception var2) {
         this.opcLog.trace("Exception occurred in BOpcDevice.Stopped.Exception:: " + var2.toString());
      }

      super.stopped();
   }

   protected void setAttached() {
      this.setState(BOpcState.attached);
   }

   protected void setAttaching() {
      this.setState(BOpcState.attaching);
   }

   protected void setDetached() {
      this.setState(BOpcState.detached);
   }

   protected void setDetaching() {
      this.setState(BOpcState.detaching);
   }

   private class OpcShutdownHook extends Thread {
      OpcShutdownHook() {
         super("OpcShutdownHook: " + BOpcDevice.this.toPathString());
      }

      @Override
      public void run() {
         try {
            if (BOpcDevice.this.getState().isEngaged()) {
               BOpcDevice.this.doDetach();
            }
         } catch (Throwable var2) {
         }
      }
   }
}
