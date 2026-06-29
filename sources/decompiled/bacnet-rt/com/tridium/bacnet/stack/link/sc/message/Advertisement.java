package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.BScHubConnectorState;
import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.IOException;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.sys.InvalidEnumException;

public final class Advertisement extends AddressedMessage {
   private BScHubConnectorState hubConnectionStatus;
   private boolean acceptsDirectConnections;
   private int maxBvlcLength;
   private int maxNpduLength;
   private static final int CAN_ACCEPT_DIRECT_CONNECTIONS = 1;
   private static final int CANNOT_ACCEPT_DIRECT_CONNECTIONS = 0;

   Advertisement() {
   }

   private Advertisement(int messageId) {
      super(messageId);
   }

   public static Advertisement make(
      int messageId, BScHubConnectorState hubConnectionStatus, boolean acceptsDirectConnections, int maxBvlcLength, int maxNpduLength
   ) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      ScMessageUtil.checkNpduLength(maxNpduLength);
      ScMessageUtil.checkBvlcLength(maxBvlcLength, maxNpduLength);
      Advertisement message = new Advertisement(messageId);
      message.hubConnectionStatus = hubConnectionStatus;
      message.acceptsDirectConnections = acceptsDirectConnections;
      message.maxBvlcLength = maxBvlcLength;
      message.maxNpduLength = maxNpduLength;
      return message;
   }

   @Override
   public void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      out.writeByte(this.hubConnectionStatus.getOrdinal());
      out.writeBoolean(this.acceptsDirectConnections);
      out.writeShort(this.maxBvlcLength);
      out.writeShort(this.maxNpduLength);
   }

   @Override
   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
      checkHasPayload(in);

      try {
         int hubConnectionStatusOrdinal = in.readUnsignedByte();

         try {
            this.hubConnectionStatus = BScHubConnectorState.make(hubConnectionStatusOrdinal);
         } catch (InvalidEnumException var6) {
            throw new ScReadMessageException(
               "Hub Connection Status must be 0, 1, or 2 and not " + hubConnectionStatusOrdinal, BBacnetErrorCode.parameterOutOfRange
            );
         }

         int result = in.readUnsignedByte();
         switch (result) {
            case 0:
               this.acceptsDirectConnections = false;
               break;
            case 1:
               this.acceptsDirectConnections = true;
               break;
            default:
               throw new ScReadMessageException("Accept direct connection must be 0 or 1 and not " + result, BBacnetErrorCode.parameterOutOfRange);
         }

         this.maxBvlcLength = in.readUnsignedShort();
         this.maxNpduLength = in.readUnsignedShort();

         try {
            ScMessageUtil.checkNpduLength(this.maxNpduLength);
            ScMessageUtil.checkBvlcLength(this.maxBvlcLength, this.maxNpduLength);
         } catch (IllegalArgumentException var5) {
            throw new ScReadMessageException(var5.getMessage(), BBacnetErrorCode.parameterOutOfRange);
         }
      } catch (IOException var7) {
         throw new ScReadMessageException("Message payload is incomplete", BBacnetErrorCode.messageIncomplete);
      }
   }

   public BScHubConnectorState getHubConnectionStatus() {
      return this.hubConnectionStatus;
   }

   public boolean acceptsDirectConnections() {
      return this.acceptsDirectConnections;
   }

   public int getMaxBvlcLength() {
      return this.maxBvlcLength;
   }

   public int getMaxNpduLength() {
      return this.maxNpduLength;
   }

   @Override
   public int getFunction() {
      return 4;
   }

   @Override
   public String toString() {
      return super.toString()
         + "; hub connection status: "
         + this.hubConnectionStatus
         + "; accepts direct connections? "
         + this.acceptsDirectConnections
         + "; max BVLC length: "
         + this.maxBvlcLength
         + "; max NPDU length: "
         + this.maxNpduLength;
   }
}
