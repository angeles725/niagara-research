package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.VmacUtil;
import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.ByteBuffer;

public abstract class ScBvlcMessage {
   private int messageId;
   private List<HeaderOption> destinationOptions = Collections.emptyList();
   private static final Logger logger = Logger.getLogger("bacnet.sc.linkLayer");

   protected ScBvlcMessage() {
   }

   protected ScBvlcMessage(int messageId) {
      this.messageId = messageId;
   }

   public static ScBvlcMessage make(ByteBuffer in) throws ScReadMessageException {
      try {
         int function = in.readUnsignedByte();
         ScBvlcMessage message;
         switch (function) {
            case 0:
               message = new ScBvlcResult();
               break;
            case 1:
               message = new ScNpdu();
               break;
            case 2:
               message = new AddressResolution();
               break;
            case 3:
               message = new AddressResolutionAck();
               break;
            case 4:
               message = new Advertisement();
               break;
            case 5:
               message = new AdvertisementSolicitation();
               break;
            case 6:
               message = new ConnectRequest();
               break;
            case 7:
               message = new ConnectAccept();
               break;
            case 8:
               message = new DisconnectRequest();
               break;
            case 9:
               message = new DisconnectAck();
               break;
            case 10:
               message = new HeartbeatRequest();
               break;
            case 11:
               message = new HeartbeatAck();
               break;
            case 12:
               message = new ScProprietaryMessage();
               break;
            default:
               logger.fine("BACnet/SC BVLC message function value " + function + " is not valid.");
               throw new ScReadMessageException("Unknown bvlc function: " + function, BBacnetErrorCode.bvlcFunctionUnknown);
         }

         message.decode(in);
         return message;
      } catch (IOException var3) {
         throw new ScReadMessageException("Message header is incomplete", var3, BBacnetErrorCode.messageIncomplete);
      }
   }

   public static boolean isAddressedMessage(int function) {
      switch (function) {
         case 0:
         case 1:
         case 2:
         case 3:
         case 4:
         case 5:
         case 12:
            return true;
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         default:
            return false;
      }
   }

   protected static byte[] getBytes(int function, int messageId) {
      byte[] bytes = new byte[]{(byte)function, 0, 0, 0};
      ByteArrayUtil.writeShort(bytes, 2, messageId);
      return bytes;
   }

   public static int readFunction(byte[] payload, int offset, int len) throws ScReadMessageException {
      checkHasFunctionByte(payload, offset, len);
      int function = ByteArrayUtil.readUnsignedByte(payload, offset);
      ScMessageUtil.checkFunction(function);
      return function;
   }

   private static int readControlFlags(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (payload != null && len >= 2 && offset >= 0 && offset + 2 <= payload.length) {
         int controlFlags = ByteArrayUtil.readUnsignedByte(payload, offset + 1);
         checkControlFlags(controlFlags);
         return controlFlags;
      } else {
         throw new ScReadMessageException("Message length or offset does not allow for control flags", BBacnetErrorCode.messageIncomplete);
      }
   }

   public static int readMessageId(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (payload != null && len >= 4 && offset >= 0 && offset + 4 <= payload.length) {
         return ByteArrayUtil.readUnsignedShort(payload, offset + 2);
      } else {
         throw new ScReadMessageException("Message length or offset does not allow for a message ID", BBacnetErrorCode.messageIncomplete);
      }
   }

   public static long readOriginatingVmac(byte[] payload, int offset, int len) throws ScReadMessageException {
      int controlFlags = readControlFlags(payload, offset, len);
      if ((controlFlags & 8) == 0) {
         return -1L;
      } else {
         int vmacOffset = offset + 4;
         if (len >= 10 && offset + 10 <= payload.length) {
            long originatingVmac = VmacUtil.readVmac(payload, vmacOffset);
            if (!VmacUtil.isDeviceVmac(originatingVmac)) {
               throw new ScReadMessageException(
                  "VMAC [0x" + Long.toHexString(originatingVmac) + "] is not a valid originating VMAC", BBacnetErrorCode.parameterOutOfRange
               );
            } else {
               return originatingVmac;
            }
         } else {
            throw new ScReadMessageException("Message length or offset does not allow for an originating VMAC", BBacnetErrorCode.messageIncomplete);
         }
      }
   }

   public static long readDestinationVmac(byte[] payload, int offset, int len) throws ScReadMessageException {
      int controlFlags = readControlFlags(payload, offset, len);
      if ((controlFlags & 4) == 0) {
         return -1L;
      } else {
         boolean hasOriginatingVmac = (controlFlags & 8) > 0;
         int destinationVmacOffset = offset + 4 + (hasOriginatingVmac ? 6 : 0);
         if ((hasOriginatingVmac ? len >= 16 : len >= 10) && destinationVmacOffset + 6 <= payload.length) {
            long destinationVmac = VmacUtil.readVmac(payload, destinationVmacOffset);
            if (!VmacUtil.isDestinationVmac(destinationVmac)) {
               throw new ScReadMessageException(
                  "VMAC [0x" + Long.toHexString(destinationVmac) + "] is not a valid destination VMAC", BBacnetErrorCode.parameterOutOfRange
               );
            } else {
               return destinationVmac;
            }
         } else {
            throw new ScReadMessageException("Message length or offset does not allow for a destination VMAC", BBacnetErrorCode.messageIncomplete);
         }
      }
   }

   public static int readResultFunction(byte[] payload, int offset, int len) throws ScReadMessageException {
      int controlFlags = readControlFlags(payload, offset, len);
      int messageFunction = ByteArrayUtil.readUnsignedByte(payload, offset);
      if (messageFunction != 0) {
         throw new ScReadMessageException("Result function only available in BVLC-Result messages", BBacnetErrorCode.inconsistentParameters);
      } else if ((controlFlags & 1) > 0) {
         throw new ScReadMessageException("BVLC-Result message cannot have data options", BBacnetErrorCode.inconsistentParameters);
      } else {
         int resultFunctionOffset = 4;
         resultFunctionOffset += (controlFlags & 8) > 0 ? 6 : 0;
         resultFunctionOffset += (controlFlags & 4) > 0 ? 6 : 0;
         if ((controlFlags & 2) > 0) {
            resultFunctionOffset += getDestinationOptionsLength(payload, offset + resultFunctionOffset, len - resultFunctionOffset);
         }

         if (len >= resultFunctionOffset + 1 && offset + resultFunctionOffset + 1 <= payload.length) {
            int resultFunction = ByteArrayUtil.readUnsignedByte(payload, offset + resultFunctionOffset);
            ScMessageUtil.checkFunction(resultFunction);
            return resultFunction;
         } else {
            throw new ScReadMessageException("Message length or offset does not allow for a result function", BBacnetErrorCode.messageIncomplete);
         }
      }
   }

   private static int getDestinationOptionsLength(byte[] payload, int offset, int len) throws ScReadMessageException {
      int optionsLength = 0;

      while (len >= optionsLength + 1 && offset + optionsLength + 1 <= payload.length) {
         int headerMarker = ByteArrayUtil.readUnsignedByte(payload, offset + optionsLength);
         optionsLength++;
         if ((headerMarker & 32) > 0) {
            if (len < optionsLength + 2 || offset + optionsLength + 2 > payload.length) {
               throw new ScReadMessageException("Message length or offset does not allow for a header data length", BBacnetErrorCode.messageIncomplete);
            }

            int headerDataLength = ByteArrayUtil.readUnsignedShort(payload, offset + optionsLength);
            optionsLength += 2;
            if (len < optionsLength + headerDataLength || offset + optionsLength + headerDataLength > payload.length) {
               throw new ScReadMessageException("Message length or offset does not allow for a header data", BBacnetErrorCode.messageIncomplete);
            }

            optionsLength += headerDataLength;
         }

         if ((headerMarker & 128) <= 0) {
            return optionsLength;
         }
      }

      throw new ScReadMessageException("Message length or offset does not allow for a header marker", BBacnetErrorCode.messageIncomplete);
   }

   public static byte[] addOriginatingVmac(long originatingVmac, byte[] payload, int offset, int len) throws ScReadMessageException {
      int controlFlags = readControlFlags(payload, offset, len);
      if ((controlFlags & 8) > 0) {
         if (len >= 10 && offset + 10 <= payload.length) {
            byte[] message = new byte[len];
            System.arraycopy(payload, offset, message, 0, 4);
            VmacUtil.writeVmac(message, 4, originatingVmac);
            System.arraycopy(payload, offset + 10, message, 10, len - 10);
            return message;
         } else {
            throw new ScReadMessageException("Message length or offset not valid for updating the originating VMAC", BBacnetErrorCode.messageIncomplete);
         }
      } else if (len >= 4 && offset + 4 <= payload.length) {
         byte[] message = new byte[len + 6];
         System.arraycopy(payload, offset, message, 0, 4);
         VmacUtil.writeVmac(message, 4, originatingVmac);
         System.arraycopy(payload, offset + 4, message, 10, len - 4);
         message[1] = (byte)(message[1] | 8);
         return message;
      } else {
         throw new ScReadMessageException("Message length or offset not valid for adding an originating VMAC", BBacnetErrorCode.messageIncomplete);
      }
   }

   public static byte[] clearDestinationVmac(long originatingVmac, byte[] payload, int offset, int len) throws ScReadMessageException {
      int controlFlags = readControlFlags(payload, offset, len);
      if ((controlFlags & 4) == 0) {
         throw new ScReadMessageException("Cannot clear non-existent destination VMAC", BBacnetErrorCode.messageIncomplete);
      } else if ((controlFlags & 8) > 0) {
         if (len >= 16 && offset + 16 <= payload.length) {
            byte[] message = new byte[len - 6];
            System.arraycopy(payload, offset, message, 0, 4);
            VmacUtil.writeVmac(message, 4, originatingVmac);
            System.arraycopy(payload, offset + 16, message, 10, len - 16);
            message[1] &= -5;
            return message;
         } else {
            throw new ScReadMessageException("Message length or offset not valid for clearing the destination vmac", BBacnetErrorCode.messageIncomplete);
         }
      } else if (len >= 10 && offset + 10 <= payload.length) {
         byte[] message = new byte[len];
         System.arraycopy(payload, offset, message, 0, len);
         VmacUtil.writeVmac(message, 4, originatingVmac);
         System.arraycopy(payload, offset + 10, message, 10, len - 10);
         message[1] &= -5;
         message[1] = (byte)(message[1] | 8);
         return message;
      } else {
         throw new ScReadMessageException("Message length or offset not valid for replacing the destination vmac", BBacnetErrorCode.messageIncomplete);
      }
   }

   public void encode(ScDataOutputStream out) throws IOException {
      int controlFlags = 0;
      long originatingVmac = this.getOriginatingVmac();
      if (originatingVmac != -1L) {
         controlFlags |= 8;
      }

      long destinationVmac = this.getDestinationVmac();
      if (destinationVmac != -1L) {
         controlFlags |= 4;
      }

      if (!this.destinationOptions.isEmpty()) {
         controlFlags |= 2;
      }

      List<HeaderOption> dataOptions = this.getDataOptions();
      if (!dataOptions.isEmpty()) {
         controlFlags |= 1;
      }

      out.writeByte(this.getFunction());
      out.writeByte(controlFlags);
      out.writeShort(this.messageId);
      if (originatingVmac != -1L) {
         VmacUtil.writeVmac(originatingVmac, out);
      }

      if (destinationVmac != -1L) {
         VmacUtil.writeVmac(destinationVmac, out);
      }

      encodeHeaderOptions(out, this.destinationOptions);
      encodeHeaderOptions(out, dataOptions);
   }

   private void decode(ByteBuffer in) throws IOException, ScReadMessageException {
      int controlFlags = in.readUnsignedByte();
      checkControlFlags(controlFlags);
      this.messageId = in.readUnsignedShort();
      if (this.hasOriginatingVmac(controlFlags)) {
         long vmac = VmacUtil.readVmac(in);

         try {
            this.setOriginatingVmac(vmac);
         } catch (IllegalArgumentException var7) {
            throw new ScReadMessageException(var7.getMessage(), BBacnetErrorCode.parameterOutOfRange);
         }
      }

      if (this.hasDestinationVmac(controlFlags)) {
         long vmac = VmacUtil.readVmac(in);

         try {
            this.setDestinationVmac(vmac);
         } catch (IllegalArgumentException var6) {
            throw new ScReadMessageException(var6.getMessage(), BBacnetErrorCode.parameterOutOfRange);
         }
      }

      if ((controlFlags & 2) > 0) {
         this.destinationOptions = decodeHeaderOptions(in, true);
      }

      if (this.hasDataOptions(controlFlags)) {
         this.decodeDataOptions(in);
      }

      this.decodePayload(in);
      if (in.available() > 0) {
         throw new ScReadMessageException("Should have reached end of message", BBacnetErrorCode.inconsistentParameters);
      }
   }

   protected static List<HeaderOption> decodeHeaderOptions(ByteBuffer in, boolean areDestinationOptions) throws IOException, ScReadMessageException {
      List<HeaderOption> headerOptions = new ArrayList<>();
      boolean hasMore = true;

      while (hasMore) {
         HeaderOption headerOption = HeaderOption.make(in, areDestinationOptions);
         if (areDestinationOptions) {
            if (!(headerOption instanceof UnsupportedHeaderOption)) {
               headerOptions.add(headerOption);
            }
         } else {
            headerOptions.add(headerOption);
         }

         hasMore = headerOption.hasMore();
      }

      return headerOptions;
   }

   private static void encodeHeaderOptions(DataOutput out, List<HeaderOption> listToEncode) throws IOException {
      int size = listToEncode.size();

      for (int i = 0; i < size; i++) {
         listToEncode.get(i).encode(out, i < size - 1);
      }
   }

   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
   }

   protected static void checkHasPayload(ByteBuffer in) throws ScReadMessageException {
      if (in.available() == 0) {
         throw new ScReadMessageException("Payload must be present", BBacnetErrorCode.payloadExpected);
      }
   }

   public static void checkHasFunctionByte(byte[] payload, int offset, int len) throws ScReadMessageException {
      if (payload == null || len < 1 || offset < 0 || offset + 1 > payload.length) {
         throw new ScReadMessageException("Message length or offset does not allow for a function byte", BBacnetErrorCode.messageIncomplete);
      }
   }

   private static void checkControlFlags(int controlFlags) throws ScReadMessageException {
      if (controlFlags < 0 || controlFlags > 15) {
         throw new ScReadMessageException("Control Flags value must be between zero and 0x0F inclusive", BBacnetErrorCode.parameterOutOfRange);
      }
   }

   public abstract int getFunction();

   public final int getMessageId() {
      return this.messageId;
   }

   protected boolean hasOriginatingVmac(int controlFlags) throws ScReadMessageException {
      if ((controlFlags & 8) > 0) {
         throw new ScReadMessageException("Bit 3 (Originating Virtual Address Flag) of the ControlFlag must be zero", BBacnetErrorCode.headerEncodingError);
      } else {
         return false;
      }
   }

   protected void setOriginatingVmac(long vmac) {
      throw new UnsupportedOperationException("Cannot set the originating address");
   }

   public long getOriginatingVmac() {
      return -1L;
   }

   protected boolean hasDestinationVmac(int controlFlags) throws ScReadMessageException {
      if ((controlFlags & 4) > 0) {
         throw new ScReadMessageException("Bit 2 (Destination Virtual Address Flag) of the ControlFlag must be zero", BBacnetErrorCode.headerEncodingError);
      } else {
         return false;
      }
   }

   protected void setDestinationVmac(long vmac) {
      throw new UnsupportedOperationException("Cannot set the destination address");
   }

   public long getDestinationVmac() {
      return -1L;
   }

   public final ScBvlcMessage addDestinationOption(HeaderOption option) {
      if (this.destinationOptions.isEmpty()) {
         this.destinationOptions = new ArrayList<>();
      }

      this.destinationOptions.add(option);
      return this;
   }

   public final List<HeaderOption> getDestinationOptions() {
      return this.destinationOptions;
   }

   protected boolean hasDataOptions(int controlFlags) throws ScReadMessageException {
      if ((controlFlags & 1) > 0) {
         throw new ScReadMessageException("Bit 0 (Data Options Flag) of the ControlFlag must be zero", BBacnetErrorCode.headerEncodingError);
      } else {
         return false;
      }
   }

   protected void decodeDataOptions(ByteBuffer in) throws IOException, ScReadMessageException {
      throw new UnsupportedOperationException("Cannot decode data options for non-ScNpdu messages");
   }

   public List<HeaderOption> getDataOptions() {
      return Collections.emptyList();
   }

   @Override
   public String toString() {
      String string = functionToString(this.getFunction()) + "; ID: " + this.messageId;
      long originatingVmac = this.getOriginatingVmac();
      if (originatingVmac != -1L) {
         string = string + "; originating VMAC: [" + VmacUtil.vmacToString(originatingVmac) + ']';
      }

      long destinationVmac = this.getDestinationVmac();
      if (destinationVmac != -1L) {
         string = string + "; destination VMAC: [" + VmacUtil.vmacToString(destinationVmac) + ']';
      }

      if (!this.destinationOptions.isEmpty()) {
         string = string + "; destination options: " + optionsToString(this.destinationOptions);
      }

      List<HeaderOption> dataOptions = this.getDataOptions();
      if (!dataOptions.isEmpty()) {
         string = string + "; data options: " + optionsToString(dataOptions);
      }

      return string;
   }

   public static String functionToString(int function) {
      switch (function) {
         case 0:
            return "BVLC-Result (0x00)";
         case 1:
            return "Encapsulated-NPDU (0x01)";
         case 2:
            return "Address-Resolution (0x02)";
         case 3:
            return "Address-Resolution-ACK (0x03)";
         case 4:
            return "Advertisement (0x04)";
         case 5:
            return "Advertisement-Solicitation (0x05)";
         case 6:
            return "Connect-Request (0x06)";
         case 7:
            return "Connect-Accept (0x07)";
         case 8:
            return "Disconnect-Request (0x08)";
         case 9:
            return "Disconnect-ACK (0x09)";
         case 10:
            return "Heartbeat-Request (0x0A)";
         case 11:
            return "Heartbeat-ACK (0x0B)";
         case 12:
            return "Proprietary-Message (0x0C)";
         default:
            return String.format("Unknown - 0x%02X", function);
      }
   }

   private static String optionsToString(List<HeaderOption> options) {
      StringJoiner joiner = new StringJoiner("}, {", "{", "}");

      for (HeaderOption option : options) {
         joiner.add(option.toString());
      }

      return joiner.toString();
   }
}
