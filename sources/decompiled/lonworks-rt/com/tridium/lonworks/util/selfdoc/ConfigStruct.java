package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.util.Neuron;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.LonAddress;

public class ConfigStruct {
   byte[] config;

   public static ConfigStruct make(LonAddress devAdr, LonComm lonComm, boolean auth, boolean farSide) throws LonException {
      return new ConfigStruct(devAdr, lonComm, auth, farSide);
   }

   public static boolean toBool(byte b, int offset) {
      return (b >> offset & 1) > 0;
   }

   private ConfigStruct(LonAddress devAdr, LonComm lonComm, boolean auth, boolean farSide) throws LonException {
      this.config = Neuron.getConfigStruct(lonComm, devAdr, auth, farSide);
   }

   public int getChannelId() {
      return this.config[0] << 8 | this.config[1] & 0xFF;
   }

   public byte[] getLocation() {
      return this.getBytes(2, 6);
   }

   public String getLocationS() {
      return this.getString(this.config, 2, 6);
   }

   public int getCommClock() {
      return (this.config[8] & 248) >> 3;
   }

   public int getInputClock() {
      return this.config[8] & 7;
   }

   public int getCommType() {
      return (this.config[9] & 224) >> 5;
   }

   public int getCommPinDirection() {
      return this.config[9] & 31;
   }

   public int getPreambleLength() {
      return this.config[10];
   }

   public int getPacketCycle() {
      return this.config[11];
   }

   public int getBeta2Control() {
      return this.config[12];
   }

   public int getXmitInterpacket() {
      return this.config[13];
   }

   public int getRecvInterpacket() {
      return this.config[14];
   }

   public int getNodePriority() {
      return this.config[15];
   }

   public int getChannelPriorities() {
      return this.config[16];
   }

   public boolean hasDirectModeParams() {
      int com_pin = this.getCommPinDirection();
      return com_pin == 12 || com_pin == 14;
   }

   public boolean getCollisionDetect() {
      return toBool(this.config[17], 7);
   }

   public int getBitSyncThreshhold() {
      return (this.config[17] & 96) >> 5;
   }

   public int getFilter() {
      return (this.config[17] & 24) >> 3;
   }

   public int getHysteresis() {
      return this.config[17] & 7;
   }

   public int getColDetToEnd() {
      return (this.config[18] & 252) >> 3;
   }

   public boolean getColDetToTail() {
      return toBool(this.config[18], 1);
   }

   public boolean getColDetPreamble() {
      return toBool(this.config[18], 0);
   }

   public byte[] getTransceiverParams() {
      return this.hasDirectModeParams() ? this.getBytes(19, 5) : this.getBytes(17, 7);
   }

   public int getNongroupTimer() {
      return (this.config[24] & 240) >> 4;
   }

   public boolean getAuthentication() {
      return toBool(this.config[24], 3);
   }

   public int getPreemptionTimeout() {
      return this.config[24] & 7;
   }

   @Deprecated
   public int getPreambleTimeout() {
      return this.getPreemptionTimeout();
   }

   public int getInputClockFreq() {
      switch (this.getInputClock()) {
         case 0:
            return 0;
         case 1:
            return 625000000;
         case 2:
            return 1250000;
         case 3:
            return 2500000;
         case 4:
            return 5000000;
         case 5:
            return 10000000;
         case 6:
            return 20000000;
         case 7:
            return 40000000;
         default:
            return -1;
      }
   }

   public int getCommClockRatio() {
      return (int)Math.pow(2.0, this.getCommClock() + 3);
   }

   private String getString(byte[] a, int offset, int len) {
      StringBuilder sb = new StringBuilder();
      int cnt = 0;

      while (cnt < len) {
         byte b = a[offset + cnt++];
         sb.append(b >= 48 && b <= 122 ? (char)b : '.');
      }

      return sb.toString();
   }

   private byte[] getBytes(int offset, int len) {
      byte[] b = new byte[len];
      System.arraycopy(this.config, offset, b, 0, len);
      return b;
   }
}
