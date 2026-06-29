package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.access.BBacnetAuthenticationFactor;
import javax.baja.bacnet.enums.access.BBacnetAccessEvent;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.data.BIDataValue;
import javax.baja.status.BStatus;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class AccessEvent extends BacnetNotificationParameters {
   public static final int ACCESS_EVENT_TAG = 0;
   public static final int STATUS_FLAGS_TAG = 1;
   public static final int ACCESS_EVENT_TAG_TAG = 2;
   public static final int ACCESS_EVENT_TIME_TAG = 3;
   public static final int ACCESS_CREDENTIAL_TAG = 4;
   public static final int AUTHENTICATION_FACTOR_TAG = 5;
   private BBacnetAccessEvent accessEvent = BBacnetAccessEvent.DEFAULT;
   private BBacnetBitString statusFlags = BacnetBitStringUtil.getBacnetStatusFlags(BStatus.ok);
   private long accessEventTag = 0L;
   private BBacnetTimeStamp accessEventTimeStamp = new BBacnetTimeStamp();
   private BBacnetDeviceObjectReference accessCredential = new BBacnetDeviceObjectReference();
   private BBacnetAuthenticationFactor authenticationFactor = new BBacnetAuthenticationFactor();

   public AccessEvent() {
   }

   public AccessEvent(
      BBacnetAccessEvent accessEvent,
      BBacnetBitString statusFlags,
      long accessEventTag,
      BBacnetTimeStamp accessEventTimeStamp,
      BBacnetDeviceObjectReference accessCredential,
      BBacnetAuthenticationFactor authenticationFactor
   ) {
   }

   @Override
   public int getChoiceType() {
      return 0;
   }

   public BBacnetAccessEvent getAccessEvent() {
      return this.accessEvent;
   }

   public BBacnetBitString getStatusFlags() {
      return this.statusFlags;
   }

   public long getAccessEventTag() {
      return this.accessEventTag;
   }

   public BBacnetTimeStamp getAccessEventTimeStamp() {
      return this.accessEventTimeStamp;
   }

   public BBacnetDeviceObjectReference getAccessCredential() {
      return this.accessCredential;
   }

   public BBacnetAuthenticationFactor getAuthenticationFactor() {
      return this.authenticationFactor;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(13);
      out.writeEnumerated(0, this.accessEvent);
      out.writeBitString(1, this.statusFlags);
      out.writeUnsignedInteger(2, this.accessEventTag);
      out.writeOpeningTag(3);
      this.getAccessEventTimeStamp().writeAsn(out);
      out.writeClosingTag(3);
      out.writeOpeningTag(3);
      this.getAccessCredential().writeAsn(out);
      out.writeClosingTag(3);
      out.writeOpeningTag(5);
      this.getAuthenticationFactor().writeAsn(out);
      out.writeClosingTag(5);
      out.writeClosingTag(13);
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.readEncoded(null, in);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream in) throws AsnException {
      in.skipOpeningTag(13);
      this.accessEvent = BBacnetAccessEvent.make(in.readEnumerated(0));
      this.statusFlags = in.readBitString(1);
      this.accessEventTag = in.readUnsignedInteger(2);
      in.skipOpeningTag(3);
      this.getAccessEventTimeStamp().readAsn(in);
      in.skipClosingTag(3);
      in.skipOpeningTag(4);
      this.getAccessCredential().readAsn(in);
      in.skipClosingTag(4);
      in.skipOpeningTag(5);
      this.getAuthenticationFactor().readAsn(in);
      in.skipClosingTag(5);
      in.skipClosingTag(13);
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  access-event\n");
      sb.append("    ")
         .append(this.accessEvent)
         .append("    ")
         .append(this.statusFlags)
         .append("    ")
         .append(this.accessEventTag)
         .append("    ")
         .append(this.accessEventTimeStamp)
         .append("    ")
         .append(this.accessCredential)
         .append("    ")
         .append(this.authenticationFactor);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".accEvt.")
         .append(this.accessEvent)
         .append(".sFlgs.")
         .append(this.statusFlags)
         .append(".accEvtTag.")
         .append(this.accessEventTag)
         .append(".accEvtTS.")
         .append(this.accessEventTimeStamp)
         .append(".accCred.")
         .append(this.accessCredential)
         .append(".authFact.")
         .append(this.authenticationFactor);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      map.put("accessEvent", BString.make(String.valueOf(this.accessEvent)));
      map.put("statusFlags", BString.make(String.valueOf(this.statusFlags)));
      map.put("accessEventTag", BString.make(String.valueOf(this.accessEventTag)));
      map.put("accessEventTime", BString.make(String.valueOf(this.accessEventTimeStamp)));
      map.put("accessCredential", BString.make(String.valueOf(this.accessCredential)));
      map.put("authFactor", BString.make(String.valueOf(this.authenticationFactor)));
   }
}
