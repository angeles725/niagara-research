package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class ScBvlcResult extends AddressedMessage {
   public static final int ACK = 0;
   public static final int NAK = 1;
   private int resultFunction;
   private boolean isAck;
   private int errorHeaderMarker;
   private int errorClass;
   private int errorCode;
   private String errorDetails;

   ScBvlcResult() {
   }

   private ScBvlcResult(int messageId, int resultFunction, boolean isAck) {
      super(messageId);
      this.resultFunction = resultFunction;
      this.isAck = isAck;
   }

   public static ScBvlcResult makeAck(int messageId, int resultFunction) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      ScMessageUtil.checkResultFunction(resultFunction);
      return new ScBvlcResult(messageId, resultFunction, true);
   }

   public static ScBvlcResult makeNak(int messageId, int resultFunction, int errorHeaderMarker, int errorClass, int errorCode, String errorDetails) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      ScMessageUtil.checkUnsignedByte(resultFunction, "resultFunction");
      ScMessageUtil.checkUnsignedByte(errorHeaderMarker, "errorHeaderMarker");
      ScMessageUtil.checkUnsignedShort(errorClass, "errorClass");
      ScMessageUtil.checkUnsignedShort(errorCode, "errorCode");
      ScBvlcResult message = new ScBvlcResult(messageId, resultFunction, false);
      message.errorHeaderMarker = errorHeaderMarker;
      message.errorClass = errorClass;
      message.errorCode = errorCode;
      message.errorDetails = errorDetails;
      return message;
   }

   @Override
   public int getFunction() {
      return 0;
   }

   public int getResultFunction() {
      return this.resultFunction;
   }

   public boolean isAck() {
      return this.isAck;
   }

   public boolean isNak() {
      return !this.isAck;
   }

   public int getErrorHeaderMarker() {
      return this.errorHeaderMarker;
   }

   public int getErrorClass() {
      return this.errorClass;
   }

   public int getErrorCode() {
      return this.errorCode;
   }

   public String getErrorDetails() {
      return this.errorDetails;
   }

   @Override
   public void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      out.writeByte(this.resultFunction);
      if (this.isAck) {
         out.writeByte(0);
      } else {
         out.writeByte(1);
         out.writeByte(this.errorHeaderMarker);
         out.writeShort(this.errorClass);
         out.writeShort(this.errorCode);
         if (this.errorDetails != null) {
            out.write(this.errorDetails.getBytes(StandardCharsets.UTF_8));
         }
      }
   }

   @Override
   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
      checkHasPayload(in);

      try {
         this.resultFunction = in.readUnsignedByte();
         int result = in.readUnsignedByte();
         switch (result) {
            case 0:
               this.isAck = true;
               break;
            case 1:
               this.isAck = false;
               this.errorHeaderMarker = in.readUnsignedByte();
               this.errorClass = in.readUnsignedShort();
               this.errorCode = in.readUnsignedShort();
               this.errorDetails = ScMessageUtil.readString(in);
               break;
            default:
               throw new ScReadMessageException("Result code must be 0 or 1 and not " + result, BBacnetErrorCode.parameterOutOfRange);
         }
      } catch (IOException var3) {
         throw new ScReadMessageException("Message payload is incomplete", BBacnetErrorCode.messageIncomplete);
      }
   }

   @Override
   public String toString() {
      String string = super.toString() + (this.isAck ? "; ACK" : "; NAK") + "; result function: " + functionToString(this.resultFunction);
      if (!this.isAck) {
         string = string
            + "; error header marker: "
            + this.errorHeaderMarker
            + "; errorClass: "
            + this.errorClass
            + "; errorCode: "
            + this.errorCode
            + "; errorDetails: "
            + this.errorDetails;
      }

      return string;
   }
}
