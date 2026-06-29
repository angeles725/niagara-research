package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import java.util.StringTokenizer;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.sys.BString;

public class IHaveRequest extends BacnetUnconfirmedRequest {
   private BBacnetObjectIdentifier deviceId;
   private BBacnetObjectIdentifier objectId;
   private String objectName;
   private BCharacterSetEncoding encoding;

   public IHaveRequest() {
      this(null, null, null, null);
   }

   public IHaveRequest(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, String objectName) {
      this(deviceId, objectId, objectName, BBacnetNetwork.localDevice().getCharacterSet());
   }

   public IHaveRequest(BBacnetObjectIdentifier deviceId, BBacnetObjectIdentifier objectId, String objectName, BCharacterSetEncoding encoding) {
      super(1);
      this.deviceId = deviceId;
      this.objectId = objectId;
      this.objectName = objectName;
      this.encoding = encoding;
   }

   public BBacnetObjectIdentifier getDeviceId() {
      return this.deviceId;
   }

   public void setDeviceId(BBacnetObjectIdentifier deviceId) {
      this.deviceId = deviceId;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   public String getObjectName() {
      return this.objectName;
   }

   public BCharacterSetEncoding getEncoding() {
      return this.encoding;
   }

   public void setEncoding(BCharacterSetEncoding encoding) {
      this.encoding = encoding;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeObjectIdentifier(this.deviceId);
      outputStream.writeObjectIdentifier(this.objectId);
      outputStream.writeCharacterString(this.objectName, this.encoding);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.deviceId = inputStream.readObjectIdentifier();
      this.objectId = inputStream.readObjectIdentifier();
      this.encoding = inputStream.peekEncoding();
      this.objectName = inputStream.readCharacterString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(BacnetUnconfirmedServiceChoice.TAGS[1]);
      sb.append("\n  deviceId " + this.deviceId);
      sb.append("\n  objectId " + this.objectId);
      sb.append("\n  objectName " + this.objectName);
      return sb.toString();
   }

   public BString toJob() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.deviceId.toString()).append('|').append(this.objectId.toString()).append('|').append(this.objectName);
      return BString.make(sb.toString());
   }

   public static String fromJob(String jobString) {
      StringTokenizer st = new StringTokenizer(jobString, "|");
      StringBuilder sb = new StringBuilder();
      sb.append(st.nextToken()).append(" has object ").append(st.nextToken()).append(" named ").append(st.nextToken()).append("\n");
      return sb.toString();
   }
}
