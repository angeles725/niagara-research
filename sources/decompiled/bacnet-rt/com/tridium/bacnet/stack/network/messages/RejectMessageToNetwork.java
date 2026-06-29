package com.tridium.bacnet.stack.network.messages;

import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class RejectMessageToNetwork extends NetworkLayerMsg {
   public static final int OTHER = 0;
   public static final int UNKNOWN_DNET = 1;
   public static final int ROUTER_BUSY = 2;
   public static final int UNKNOWN_MESSAGE_TYPE = 3;
   public static final int MESSAGE_TOO_LONG = 4;
   private static final String[] rejectReasons = new String[]{
      "Other", "Unknown DNET - cannot find router", "Router busy", "Unknown network layer message type", "Message too long for DNET"
   };
   private int rejectReason;
   private int rejectedDNET;

   public RejectMessageToNetwork(ByteArrayInputStream is) throws BacnetStackException {
      super(3);
      this.readNetworkBytes(is);
   }

   public RejectMessageToNetwork(int rejectReason, int rejectedDNET) {
      super(3);
      this.rejectReason = rejectReason;
      this.rejectedDNET = rejectedDNET;
   }

   public int getRejectReason() {
      return this.rejectReason;
   }

   public String getReasonString() {
      return this.rejectReason > 0 && this.rejectReason <= 4 ? rejectReasons[this.rejectReason] : "" + this.rejectReason;
   }

   public int getRejectedDNET() {
      return this.rejectedDNET;
   }

   @Override
   public String getMsgString() {
      return "RejectMessageToNetwork; rejectedDNET=" + this.rejectedDNET + ", reason=" + this.getReasonString();
   }

   @Override
   public void readNetworkBytes(ByteArrayInputStream is) throws BacnetStackException {
      if (is.available() < 3) {
         throw new InvalidNetworkMsgException();
      } else {
         this.rejectReason = is.read();
         this.rejectedDNET = is.read() << 8;
         this.rejectedDNET = this.rejectedDNET | is.read();
      }
   }

   @Override
   public void writeNetworkBytes(ByteArrayOutputStream os) {
      super.writeNetworkBytes(os);
      os.write(this.rejectReason);
      os.write(this.rejectedDNET >> 8 & 0xFF);
      os.write(this.rejectedDNET & 0xFF);
   }
}
