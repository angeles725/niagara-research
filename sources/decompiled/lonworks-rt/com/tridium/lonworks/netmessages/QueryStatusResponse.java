package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryStatusResponse extends LonMessage implements NetMessages {
   public int xmitErrors;
   public int transactionTimeouts;
   public int rcvTransactionFull;
   public int lostMsgs;
   public int missedMsgs;
   public int resetCause;
   public int nodeState;
   public int versionNumber;
   public int errorLog;
   public int modelNumber;

   public QueryStatusResponse() {
      this.code = 49;
   }

   public QueryStatusResponse(LonInputStream in) throws LonException {
      this.code = 49;
      this.fromInputStream(in);
   }

   public int getXmitErrors() {
      return this.xmitErrors;
   }

   public int getTransactionTimeouts() {
      return this.transactionTimeouts;
   }

   public int getRcvTransactionFull() {
      return this.rcvTransactionFull;
   }

   public int getLostMsgs() {
      return this.lostMsgs;
   }

   public int getMissedMsgs() {
      return this.missedMsgs;
   }

   public int getResetCause() {
      return this.resetCause;
   }

   public int getVersionNumber() {
      return this.versionNumber;
   }

   public int getErrorLog() {
      return this.errorLog;
   }

   public int getModelNumber() {
      return this.modelNumber;
   }

   public int getNodeState() {
      return (this.nodeState & 7) == 4 ? this.nodeState & 143 : this.nodeState & 7;
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 49) {
         throw new InvalidResponseException(code);
      } else {
         this.xmitErrors = in.readUnsigned16();
         this.transactionTimeouts = in.readUnsigned16();
         this.rcvTransactionFull = in.readUnsigned16();
         this.lostMsgs = in.readUnsigned16();
         this.missedMsgs = in.readUnsigned16();
         this.resetCause = in.readUnsigned8();
         this.nodeState = in.readUnsigned8();
         this.versionNumber = in.readUnsigned8();
         this.errorLog = in.readUnsigned8();
         this.modelNumber = in.readUnsigned8();
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeUnsigned16(this.xmitErrors);
      out.writeUnsigned16(this.transactionTimeouts);
      out.writeUnsigned16(this.rcvTransactionFull);
      out.writeUnsigned16(this.lostMsgs);
      out.writeUnsigned16(this.missedMsgs);
      out.writeUnsigned8(this.resetCause);
      out.writeUnsigned8(this.nodeState);
      out.writeUnsigned8(this.versionNumber);
      out.writeUnsigned8(this.errorLog);
      out.writeUnsigned8(this.modelNumber);
   }

   @Override
   public String toString() {
      return "\nxmitErrors = "
         + this.xmitErrors
         + "\ntransactionTimeouts = "
         + this.transactionTimeouts
         + "\nrcvTransactionFull = "
         + this.rcvTransactionFull
         + "\nlostMsgs = "
         + this.lostMsgs
         + "\nmissedMsgs = "
         + this.missedMsgs
         + "\nresetCause = 0x"
         + Integer.toHexString(this.resetCause)
         + "\nnodeState = "
         + this.nodeState
         + "\nversionNumber = "
         + this.versionNumber
         + "\nerrorLog = "
         + this.errorLog
         + "\nmodelNumber = "
         + this.modelNumber;
   }
}
