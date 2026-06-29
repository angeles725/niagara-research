package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryIdResponse extends LonMessage implements NetMessages {
   private byte[] neuronId = new byte[6];
   private byte[] idString = new byte[8];
   public static final int QUERY_ID_SUCCESS_LEN = 14;

   public QueryIdResponse() {
      this.code = 33;
   }

   public QueryIdResponse(byte[] neuronId, byte[] idString) throws LonException {
      this.code = 33;
      this.setNeuronId(neuronId);
      this.setIdString(idString);
   }

   public QueryIdResponse(LonInputStream in) throws LonException {
      this.code = 33;
      this.fromInputStream(in);
   }

   public BNeuronId getNeuronId() {
      return BNeuronId.make(this.neuronId);
   }

   public void setNeuronId(byte[] neuronId) throws LonException {
      if (neuronId.length != 6) {
         throw new IllegalArgumentException("Invalid neuronId length.");
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
      out.write(this.neuronId, 0, 6);
      out.write(this.idString, 0, 8);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 33) {
         throw new InvalidResponseException(code);
      } else {
         in.read(this.neuronId, 0, 6);
         in.read(this.idString, 0, 8);
      }
   }

   @Override
   public String toString() {
      return "\nneuronId " + LonByteArrayUtil.toString(this.neuronId, ':') + "\nidString " + LonByteArrayUtil.toString(this.idString, ':');
   }
}
