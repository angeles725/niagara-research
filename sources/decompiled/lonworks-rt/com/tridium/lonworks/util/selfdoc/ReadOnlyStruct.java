package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.util.Neuron;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.LonAddress;

public class ReadOnlyStruct {
   byte[] rdOnly;
   boolean ro2 = false;
   boolean ro3 = false;
   boolean ro4 = false;

   public static ReadOnlyStruct make(LonAddress devAdr, LonComm lonComm, boolean auth, boolean farSide) throws LonException {
      return new ReadOnlyStruct(devAdr, lonComm, auth, farSide);
   }

   public static byte[] getArray(byte[] src, int offset, int len) {
      byte[] a = new byte[len];
      System.arraycopy(src, offset, a, 0, len);
      return a;
   }

   public static boolean toBool(byte b, int offset) {
      return (b >> offset & 1) > 0;
   }

   private ReadOnlyStruct(LonAddress devAdr, LonComm lonComm, boolean auth, boolean farSide) throws LonException {
      int ver = Neuron.getVersion(lonComm, devAdr, auth);
      ver &= 127;
      this.ro2 = ver >= 6;
      this.ro3 = ver >= 16;
      this.ro4 = ver >= 21;
      int len = 36;
      if (this.ro2) {
         len += 5;
      }

      if (this.ro3) {
         len += 5;
      }

      if (this.ro4) {
         len++;
      }

      this.rdOnly = Neuron.readTable(lonComm, devAdr, 1, len, auth, farSide);
   }

   public BNeuronId getNeuronId() {
      return BNeuronId.make(getArray(this.rdOnly, 0, 6));
   }

   public int getModelNumber() {
      return this.rdOnly[6] & 0xFF;
   }

   public int getMinorModelNumber() {
      return this.rdOnly[7] & 15;
   }

   public int getNvFixedPointer() {
      return (this.rdOnly[8] & 0xFF) << 8 | this.rdOnly[9] & 0xFF;
   }

   public boolean getReadWriteProtect() {
      return toBool(this.rdOnly[10], 7);
   }

   public int getNvCount() {
      return !this.ro3 ? this.rdOnly[10] & 63 : this.rdOnly[41] & 0xFF;
   }

   public int getSnvtStructsPointer() {
      return (this.rdOnly[11] & 0xFF) << 8 | this.rdOnly[12] & 0xFF;
   }

   public BProgramId getProgramId() {
      return BProgramId.make(getArray(this.rdOnly, 13, 8));
   }

   public boolean getNvProcessingOff() {
      return toBool(this.rdOnly[21], 7);
   }

   public boolean getTwoDomains() {
      return toBool(this.rdOnly[21], 6);
   }

   public boolean getExplicitAddressing() {
      return toBool(this.rdOnly[21], 5);
   }

   public int getAddressCount() {
      return !this.ro4 ? (this.rdOnly[22] & 240) >> 4 : this.rdOnly[46];
   }

   public int getRcvTransCount() {
      return this.rdOnly[23] & 15;
   }

   public int getAppBufOutSize() {
      return (this.rdOnly[24] & 240) >> 4;
   }

   public int getAppBufInSize() {
      return this.rdOnly[24] & 15;
   }

   public int getNetBufOutSize() {
      return (this.rdOnly[25] & 240) >> 4;
   }

   public int getNetBufInSize() {
      return this.rdOnly[25] & 15;
   }

   public int getNetBufOutPriorityCount() {
      return (this.rdOnly[26] & 240) >> 4;
   }

   public int getAppBufOutPriorityCount() {
      return this.rdOnly[26] & 15;
   }

   public int getAppBufOutCount() {
      return (this.rdOnly[27] & 240) >> 4;
   }

   public int getAppBufInCount() {
      return this.rdOnly[27] & 15;
   }

   public int getNetBufOutCount() {
      return (this.rdOnly[28] & 240) >> 4;
   }

   public int getNetBufInCount() {
      return this.rdOnly[28] & 15;
   }

   public boolean getTxByAddress() {
      return toBool(this.rdOnly[35], 1);
   }

   public boolean getIdempotentDuplicate() {
      return toBool(this.rdOnly[35], 0);
   }

   public int getAliasCount() {
      if (!this.ro2) {
         return 0;
      } else {
         return this.ro2 && !this.ro3 ? this.rdOnly[36] & 63 : this.rdOnly[42] & 0xFF;
      }
   }

   public int getMessageTagCount() {
      return this.ro2 ? (this.rdOnly[37] & 240) >> 4 : 0;
   }

   public boolean getHasEcs() {
      return this.ro2 ? toBool(this.rdOnly[37], 3) : false;
   }
}
