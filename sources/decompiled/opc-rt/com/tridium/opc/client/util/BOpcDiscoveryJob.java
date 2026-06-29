package com.tridium.opc.client.util;

import com.tridium.opc.client.BOpcNetwork;
import javax.baja.job.BJob;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraAction(
   name = "discover",
   flags = 16
)
public abstract class BOpcDiscoveryJob extends BJob {
   public static final Action discover = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcDiscoveryJob.class);
   private boolean canceled = false;
   private BOpcNetwork network;

   public void discover() {
      this.invoke(discover, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcDiscoveryJob() {
   }

   public BOpcDiscoveryJob(BOpcNetwork network) {
      this.network = network;
   }

   public void addResult(String name, BValue result) {
      this.add(SlotPath.escape(name) + "?", result, 2);
   }

   public abstract void doDiscover();

   public void doCancel(Context cx) {
      this.canceled = true;
   }

   public void doRun(Context cx) {
      this.canceled = false;
      this.discover();
   }

   public BOpcNetwork getNetwork() {
      return this.network;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      if (action == discover) {
         this.network.enqueue(new Invocation(this, discover, arg, cx));
         return null;
      } else {
         return super.post(action, arg, cx);
      }
   }
}
