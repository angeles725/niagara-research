package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class UnprocessedNV extends LonMessage implements NetMessages {
   private static final int DIRECTION_BIT_MASK = 64;
   private static final int UPPER_SEL_MASK = 16128;
   private static final int LOWER_SEL_MASK = 255;
   private static final int HDR_SIZE = 2;
   private static final int DIRECTION_INDEX = 0;
   private static final int UPPER_SEL_INDEX = 0;
   private static final int LOWER_SEL_INDEX = 1;
   private static final int DATA_INDEX = 2;
   public static final int MAX_NETVAR_DATA = 31;
   private int length = 0;
   private byte[] msgData = new byte[228];

   public UnprocessedNV() {
   }

   public UnprocessedNV(BLonNvDirection nvDirection, int nvSelector, byte[] nvData) throws LonException {
      if (nvData.length > 226) {
         throw new IllegalArgumentException("Invalid nvData length " + nvData.length + " exceeds max " + 228);
      } else {
         this.msgData[0] = (byte)(this.msgData[0] | 128);
         if (nvDirection == BLonNvDirection.input) {
            this.msgData[0] = (byte)(this.msgData[0] & -65);
         } else if (nvDirection == BLonNvDirection.output) {
            this.msgData[0] = (byte)(this.msgData[0] | 64);
         }

         this.msgData[0] = (byte)(this.msgData[0] | (byte)((nvSelector & 16128) >> 8));
         this.msgData[1] = (byte)(nvSelector & 0xFF);
         System.arraycopy(nvData, 0, this.msgData, 2, nvData.length);
         this.length = nvData.length + 2;
      }
   }

   public BLonNvDirection getDirection() {
      return (this.msgData[0] & 64) == 0 ? BLonNvDirection.input : BLonNvDirection.output;
   }

   public void setDirection(BLonNvDirection direction) {
      if (direction == BLonNvDirection.input) {
         this.msgData[0] = (byte)(this.msgData[0] & -65);
      } else if (direction == BLonNvDirection.output) {
         this.msgData[0] = (byte)(this.msgData[0] | 64);
      }
   }

   public int getNvSelector() {
      int workingInt = this.msgData[0] << 8 & 16128;
      return workingInt | this.msgData[1] & 0xFF;
   }

   public void setNvSelector(int nvSelector) {
      this.msgData[0] = (byte)(this.msgData[0] | (byte)((nvSelector & 16128) >> 8));
      this.msgData[1] = (byte)(nvSelector & 0xFF);
   }

   public byte[] getData() {
      byte[] tempData = new byte[this.length - 2];
      System.arraycopy(this.msgData, 2, tempData, 0, this.length - 2);
      return tempData;
   }

   public void setData(byte[] data) {
      System.arraycopy(data, 0, this.msgData, 2, data.length);
   }

   @Override
   public String toString() {
      return LonByteArrayUtil.toString(this.msgData, this.length);
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.write(this.msgData, 0, this.length);
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.msgData = in.readByteArray();
      this.length = this.msgData.length;
   }
}
