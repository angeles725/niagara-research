package com.tridium.opc.client.point;

import com.tridium.opc.client.BOpcDevice;
import com.tridium.opc.client.BOpcNetwork;
import javax.baja.driver.point.BPointFolder;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "getDeviceOrd",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "submitPointDiscoveryJob",
      parameterType = "BValue",
      defaultValue = "BString.DEFAULT",
      returnType = "BOrd",
      flags = 4
   )})
public class BOpcPointFolder extends BPointFolder {
   public static final Action getDeviceOrd = newAction(4, null);
   public static final Action submitPointDiscoveryJob = newAction(4, BString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BOpcPointFolder.class);

   public BOrd getDeviceOrd() {
      return (BOrd)this.invoke(getDeviceOrd, null, null);
   }

   public BOrd submitPointDiscoveryJob(BValue parameter) {
      return (BOrd)this.invoke(submitPointDiscoveryJob, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BOrd doGetDeviceOrd() {
      return this.getOpcDevice().getSlotPathOrd();
   }

   public final BOrd doSubmitPointDiscoveryJob(BValue args, Context cx) {
      return new BOpcPointDiscoveryJob(this.getOpcPointDeviceExt(), this.get("id")).submit(cx);
   }

   public final BOpcDevice getOpcDevice() {
      return this.getOpcPointDeviceExt().getOpcDevice();
   }

   public final BOpcPointDeviceExt getOpcPointDeviceExt() {
      return (BOpcPointDeviceExt)this.getDeviceExt();
   }

   public final BOpcNetwork getOpcNetwork() {
      return this.getOpcDevice().getOpcNetwork();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcPointDeviceExt || parent instanceof BOpcPointFolder;
   }

   public final IFuture post(Action a, BValue arg, Context cx) {
      this.getOpcNetwork().enqueue(new Invocation(this, a, arg, cx));
      return null;
   }
}
