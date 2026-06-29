package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.VmacUtil;

public abstract class AddressedMessage extends ScBvlcMessage {
   private long originatingVmac = -1L;
   protected long destinationVmac = -1L;

   protected AddressedMessage() {
   }

   protected AddressedMessage(int messageId) {
      super(messageId);
   }

   @Override
   protected final boolean hasOriginatingVmac(int controlFlags) {
      return (controlFlags & 8) > 0;
   }

   @Override
   public final void setOriginatingVmac(long vmac) {
      VmacUtil.checkIsDeviceVmac(vmac);
      this.originatingVmac = vmac;
   }

   public final void clearOriginatingVmac() {
      this.originatingVmac = -1L;
   }

   @Override
   public final long getOriginatingVmac() {
      return this.originatingVmac;
   }

   @Override
   protected final boolean hasDestinationVmac(int controlFlags) {
      return (controlFlags & 4) > 0;
   }

   @Override
   public void setDestinationVmac(long vmac) {
      VmacUtil.checkIsDeviceVmac(vmac);
      this.destinationVmac = vmac;
   }

   public final void clearDestinationVmac() {
      this.destinationVmac = -1L;
   }

   @Override
   public final long getDestinationVmac() {
      return this.destinationVmac;
   }
}
