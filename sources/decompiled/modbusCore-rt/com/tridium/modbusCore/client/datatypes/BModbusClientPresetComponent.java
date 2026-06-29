package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.MessageListener;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "startingAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()"
   ), @NiagaraProperty(
      name = "absoluteStartingAddress",
      type = "BFlexAddress",
      defaultValue = "new BFlexAddress()",
      flags = 3
   ), @NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.down",
      flags = 67
   ), @NiagaraProperty(
      name = "writeOnInputChange",
      type = "boolean",
      defaultValue = "false"
   )})
@NiagaraAction(
   name = "write",
   flags = 24
)
@NiagaraTopic(
   name = "writeSuccessful",
   flags = 8
)
public abstract class BModbusClientPresetComponent extends BComponent {
   public static final Property startingAddress = newProperty(0, new BFlexAddress(), null);
   public static final Property absoluteStartingAddress = newProperty(3, new BFlexAddress(), null);
   public static final Property status = newProperty(67, BStatus.down, null);
   public static final Property writeOnInputChange = newProperty(0, false, null);
   public static final Action write = newAction(24, null);
   public static final Topic writeSuccessful = newTopic(8, null);
   public static final Type TYPE = Sys.loadType(BModbusClientPresetComponent.class);
   private Subscriber subscriber;

   public BFlexAddress getStartingAddress() {
      return (BFlexAddress)this.get(startingAddress);
   }

   public void setStartingAddress(BFlexAddress v) {
      this.set(startingAddress, v, null);
   }

   public BFlexAddress getAbsoluteStartingAddress() {
      return (BFlexAddress)this.get(absoluteStartingAddress);
   }

   public void setAbsoluteStartingAddress(BFlexAddress v) {
      this.set(absoluteStartingAddress, v, null);
   }

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public boolean getWriteOnInputChange() {
      return this.getBoolean(writeOnInputChange);
   }

   public void setWriteOnInputChange(boolean v) {
      this.setBoolean(writeOnInputChange, v, null);
   }

   public void write() {
      this.invoke(write, null, null);
   }

   public void fireWriteSuccessful(BValue event) {
      this.fire(writeSuccessful, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.subscriber = new BModbusClientPresetComponent.BaseAddressSubscriber();
      BModbusClientDevice device = this.getDevice();
      if (device != null) {
         this.subscriber.subscribe(device);
         this.setCurrentAbsoluteAddress();
      }
   }

   public void stopped() throws Exception {
      this.subscriber.unsubscribeAll();
      this.subscriber = null;
      super.stopped();
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(startingAddress) && this.getDevice() != null && context != Context.commit) {
            this.setCurrentAbsoluteAddress();
         }
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      return action.equals(write) ? this.getNetwork().postAsync(new Invocation(this, action, arg, cx)) : super.post(action, arg, cx);
   }

   private void setCurrentAbsoluteAddress() {
      if (!this.isValidAddress(this.getStartingAddress())) {
         this.setStatus(BStatus.make(this.getStatus(), 4, true));
         this.getNetwork()
            .getModbusLog()
            .error("Illegal Modbus address for preset component " + this.getName() + ": Modbus Address does not match preset type.");
      } else {
         this.setStatus(BStatus.make(this.getStatus(), 4, this.getDevice().isDown()));
         int baseAddr = 0;

         try {
            baseAddr = ((BFlexAddress)this.getDevice().get(this.getBaseAddressProperty())).getDataAddress();
         } catch (NullPointerException var6) {
         } catch (NumberFormatException var7) {
            baseAddr = 0;
         }

         try {
            BFlexAddress absAddr = (BFlexAddress)this.getStartingAddress().newCopy();
            int rawAddress;
            if (absAddr.isModbusFormat()) {
               rawAddress = Integer.valueOf(absAddr.getAddress());
            } else {
               rawAddress = absAddr.getDataAddress();
            }

            absAddr.setAddressFromInt(rawAddress + baseAddr);
            this.setAbsoluteStartingAddress(absAddr);
         } catch (NullPointerException var4) {
         } catch (NumberFormatException var5) {
            BFlexAddress absAddr = (BFlexAddress)this.getStartingAddress().newCopy();
            absAddr.setAddressFromInt(baseAddr);
            this.setAbsoluteStartingAddress(absAddr);
         }
      }
   }

   public void doWrite() {
      if (!this.isDown() && !this.isDisabled()) {
         this.writePresetValues();
      }
   }

   protected abstract void writePresetValues();

   protected abstract Property getBaseAddressProperty();

   protected abstract boolean isValidAddress(BFlexAddress var1);

   public void setStatusFault(boolean x) {
      this.setStatus(BStatus.make(this.getStatus(), 2, x));
   }

   public boolean isDown() {
      return this.getStatus().isDown();
   }

   public boolean isDisabled() {
      return this.getStatus().isDisabled();
   }

   public BModbusClientDevice getDevice() {
      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BModbusClientDevice) {
            if (this.isDisabled()) {
               this.setStatus(BStatus.make(this.getStatus(), 1, ((BModbusClientDevice)parent).isDisabled()));
               if (!this.subscriber.isSubscribed((BComponent)parent)) {
                  this.subscriber.subscribe((BComponent)parent);
               }
            }

            return (BModbusClientDevice)parent;
         }
      }

      if (!this.isDisabled()) {
         this.setStatus(BStatus.make(this.getStatus(), 1, true));
      }

      return null;
   }

   public BModbusClientNetwork getNetwork() {
      BModbusClientDevice device = this.getDevice();
      return device == null ? null : (BModbusClientNetwork)device.modbusNet();
   }

   private class BaseAddressSubscriber extends Subscriber {
      public BaseAddressSubscriber() {
      }

      public void event(BComponentEvent event) {
         if (event.getId() == 0) {
            if (event.getSlot().equals(BModbusClientPresetComponent.this.getBaseAddressProperty())) {
               BModbusClientPresetComponent.this.setCurrentAbsoluteAddress();
            } else if (event.getSlot().equals(BModbusClientDevice.status)) {
               if (BModbusClientPresetComponent.this.getDevice().isDown()) {
                  BModbusClientPresetComponent.this.setStatus(BStatus.make(BModbusClientPresetComponent.this.getStatus(), 4, true));
               } else {
                  BModbusClientPresetComponent.this.setStatus(
                     BStatus.make(
                        BModbusClientPresetComponent.this.getStatus(),
                        4,
                        !BModbusClientPresetComponent.this.isValidAddress(BModbusClientPresetComponent.this.getStartingAddress())
                     )
                  );
               }

               BModbusClientPresetComponent.this.setStatus(
                  BStatus.make(BModbusClientPresetComponent.this.getStatus(), 1, BModbusClientPresetComponent.this.getDevice().isDisabled())
               );
            }
         }
      }
   }

   protected class ModbusClientWriteRequest implements Runnable {
      private MessageListener source;
      private ModbusWriteRequest req;

      public ModbusClientWriteRequest(ModbusWriteRequest req, MessageListener source) {
         this.source = source;
         this.req = req;
      }

      @Override
      public void run() {
         this.source.processMessage(BModbusClientPresetComponent.this.getDevice().sendModbusMessage(this.req));
      }
   }
}
