package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class WinkResponse extends LonMessage implements NetMessages {
   private boolean interfaceStatus = false;
   private byte[] neuronID = new byte[6];
   private byte[] idString = new byte[8];
   public static final int WINK = 0;
   public static final int SEND_ID_INFO = 1;
   public static final int UNUSED_SUBCOMMAND = -1;
   public static final int INTERFACE_ACTIVE = 0;
   public static final int INTERFACE_INACTIVE = 1;
   public static final int WINK_RESPONSE_LEN = 15;

   public WinkResponse() {
      this.code = 48;
   }

   public WinkResponse(boolean status, byte[] neuronID, byte[] idString) throws LonException {
      this.code = 48;
      this.interfaceStatus = status;
      this.neuronID = neuronID;
      this.idString = idString;
   }

   public WinkResponse(LonInputStream in) throws LonException {
      this.code = 48;
      this.fromInputStream(in);
   }

   public boolean isInterfaceUp() {
      return this.interfaceStatus;
   }

   public void setInterfaceStatus(boolean interfaceStatus) {
      this.interfaceStatus = interfaceStatus;
   }

   public byte[] getNeuronID() {
      return this.neuronID;
   }

   public void setNeuronID(byte[] neuronID) throws LonException {
      if (neuronID.length != 6) {
         throw new IllegalArgumentException("Invalid neuronID length");
      } else {
         System.arraycopy(neuronID, 0, this.neuronID, 0, 6);
      }
   }

   public byte[] getIDString() {
      return this.idString;
   }

   public void setIDString(byte[] idString) throws LonException {
      if (idString.length != 8) {
         throw new IllegalArgumentException("Invalid idString length.");
      } else {
         System.arraycopy(idString, 0, this.idString, 0, 8);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.code == 48) {
         if (this.interfaceStatus) {
            out.writeUnsigned8(0);
         } else {
            out.writeUnsigned8(1);
         }

         out.writeByteArray(this.neuronID, 6);
         out.writeByteArray(this.idString, 8);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      if (in.read() == 0) {
         this.interfaceStatus = true;
      } else {
         this.interfaceStatus = false;
      }

      this.neuronID = in.readByteArray(6);
      this.idString = in.readByteArray(8);
   }
}
