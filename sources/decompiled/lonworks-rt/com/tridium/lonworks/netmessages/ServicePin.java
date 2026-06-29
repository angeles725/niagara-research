package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class ServicePin extends LonMessage implements NetMessages {
   private byte[] neuronId = new byte[6];
   private byte[] idString = new byte[8];
   public static final int SERVICE_PIN_LEN = 14;

   public ServicePin() {
      this.code = 127;
   }

   public ServicePin(BNeuronId neuronId, BProgramId idString) throws LonException {
      this.code = 127;
      this.setNeuronId(neuronId.getByteArray());
      this.setIdString(idString.getByteArray());
   }

   public BNeuronId getNeuronId() {
      return BNeuronId.make(this.neuronId);
   }

   public void setNeuronId(byte[] neuronId) throws LonException {
      if (neuronId.length != 6) {
         throw new IllegalArgumentException("Invalid neuronId length");
      } else {
         System.arraycopy(neuronId, 0, this.neuronId, 0, 6);
      }
   }

   public BProgramId getIdString() {
      return BProgramId.make(this.idString);
   }

   public void setIdString(byte[] idString) throws LonException {
      if (idString.length != 8) {
         throw new IllegalArgumentException("Invalid idString length");
      } else {
         System.arraycopy(idString, 0, this.idString, 0, 8);
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.writeByteArray(this.neuronId, 6);
      out.writeByteArray(this.idString, 8);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 127) {
         throw new InvalidResponseException(code);
      } else {
         this.neuronId = in.readByteArray(6);
         this.idString = in.readByteArray(8);
      }
   }

   @Override
   public String toString() {
      return "\nneuronId " + LonByteArrayUtil.toString(this.neuronId, ':') + "\nidString " + LonByteArrayUtil.toString(this.idString, ':');
   }
}
