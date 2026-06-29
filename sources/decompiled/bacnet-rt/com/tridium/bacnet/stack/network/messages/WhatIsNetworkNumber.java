package com.tridium.bacnet.stack.network.messages;

public class WhatIsNetworkNumber extends NetworkLayerMsg {
   private int networkNumber;

   public WhatIsNetworkNumber() {
      super(18);
   }

   public WhatIsNetworkNumber(int networkNumber) {
      super(18);
      this.networkNumber = networkNumber;
   }

   public int getNetworkNumber() {
      return this.networkNumber;
   }
}
