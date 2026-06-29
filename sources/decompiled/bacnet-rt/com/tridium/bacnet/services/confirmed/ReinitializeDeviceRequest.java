package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.enums.BBacnetReinitializedDeviceState;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;

public class ReinitializeDeviceRequest extends BacnetConfirmedRequest {
   public static final int REINITIALIZED_STATE_OF_DEVICE_TAG = 0;
   public static final int PASSWORD_TAG = 1;
   private BBacnetReinitializedDeviceState reinitializedStateOfDevice;
   private String password = null;
   private BCharacterSetEncoding encoding = null;

   public ReinitializeDeviceRequest() {
      super(20);
   }

   public ReinitializeDeviceRequest(BBacnetReinitializedDeviceState reinitializedStateOfDevice) {
      this(reinitializedStateOfDevice, null, null);
   }

   public ReinitializeDeviceRequest(BBacnetReinitializedDeviceState reinitializedStateOfDevice, String password, BCharacterSetEncoding encoding) {
      super(20);
      this.reinitializedStateOfDevice = reinitializedStateOfDevice;
      this.password = password;
      this.encoding = encoding;
   }

   public BBacnetReinitializedDeviceState getReinitializedStateOfDevice() {
      return this.reinitializedStateOfDevice;
   }

   public void setReinitializedStateOfDevice(BBacnetReinitializedDeviceState reinitializedStateOfDevice) {
      this.reinitializedStateOfDevice = reinitializedStateOfDevice;
   }

   public String getPassword() {
      return this.password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean isPasswordUsed() {
      return this.password != null;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeEnumerated(0, this.reinitializedStateOfDevice);
      if (this.isPasswordUsed()) {
         out.writeCharacterString(1, this.password, this.encoding);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.reinitializedStateOfDevice = BBacnetReinitializedDeviceState.make(in.readEnumerated(0));
      if (in.peekTag() == 1) {
         this.encoding = in.peekEncoding(1);
         this.password = in.readCharacterString(1);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReinitializeDeviceRequest: ");
      sb.append("\n reinitializedStateOfDevice: " + this.reinitializedStateOfDevice);
      if (this.isPasswordUsed()) {
         sb.append("\n encoding: " + this.encoding.getTag());
      }

      return sb.toString();
   }
}
