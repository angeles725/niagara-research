package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import javax.baja.bacnet.enums.BBacnetCommControl;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BRelTime;

public class DeviceCommunicationControlRequest extends BacnetConfirmedRequest {
   public static final int DURATION_TAG = 0;
   public static final int ENABLE_DISABLE_TAG = 1;
   public static final int PASSWORD_TAG = 2;
   private BRelTime duration = null;
   private BBacnetCommControl enableDisable;
   private String password = null;
   private BCharacterSetEncoding encoding = null;

   public DeviceCommunicationControlRequest() {
      super(17);
   }

   public DeviceCommunicationControlRequest(BBacnetCommControl enableDisable) {
      this(null, enableDisable, null, null);
   }

   public DeviceCommunicationControlRequest(BRelTime duration, BBacnetCommControl enableDisable) {
      this(duration, enableDisable, null, null);
   }

   public DeviceCommunicationControlRequest(BRelTime duration, BBacnetCommControl enableDisable, String password, BCharacterSetEncoding encoding) {
      super(17);
      this.duration = duration;
      this.enableDisable = enableDisable;
      this.password = password;
      this.encoding = encoding;
   }

   public BRelTime getDuration() {
      return this.duration;
   }

   public void setDuration(BRelTime duration) {
      this.duration = duration;
   }

   public boolean isDurationUsed() {
      return this.duration != null;
   }

   public boolean isIndefinite() {
      return this.duration.getMillis() == 0L;
   }

   public BBacnetCommControl getEnableDisable() {
      return this.enableDisable;
   }

   public void setEnableDisable(BBacnetCommControl enableDisable) {
      this.enableDisable = enableDisable;
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
      if (this.isDurationUsed()) {
         out.writeUnsignedInteger(0, this.duration.getMinutes());
      }

      out.writeEnumerated(1, this.enableDisable);
      if (this.isPasswordUsed()) {
         out.writeCharacterString(2, this.password, this.encoding);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      in.peekTag();
      if (in.isValueTag(0)) {
         this.duration = BRelTime.make(in.readUnsignedInteger(0) * 60000L);
      }

      this.enableDisable = BBacnetCommControl.make(in.readEnumerated(1));
      if (in.peekTag() == 2) {
         this.encoding = in.peekEncoding(2);
         this.password = in.readCharacterString(2);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("DeviceCommunicationControlRequest: ");
      sb.append("\n duration: ");
      if (this.isDurationUsed()) {
         if (this.isIndefinite()) {
            sb.append("indefinite");
         } else {
            sb.append(this.duration.toString());
         }
      } else {
         sb.append("not used");
      }

      sb.append("\n enableDisable: " + this.enableDisable);
      if (this.isPasswordUsed()) {
         sb.append("\n password: " + this.password);
         sb.append("\n encoding: " + this.encoding.getTag());
      }

      return sb.toString();
   }
}
