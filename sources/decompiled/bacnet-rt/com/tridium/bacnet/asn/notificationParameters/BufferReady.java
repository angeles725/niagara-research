package com.tridium.bacnet.asn.notificationParameters;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.BacnetNotificationParameters;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.data.BIDataValue;
import javax.baja.sys.BString;
import javax.baja.sys.Context;

public class BufferReady extends BacnetNotificationParameters {
   public static final int BUFFER_PROPERTY_TAG = 0;
   public static final int PREVIOUS_NOTIFICATION_TAG = 1;
   public static final int CURRENT_NOTIFICATION_TAG = 2;
   BBacnetDeviceObjectPropertyReference bufferProperty;
   long previousNotification;
   long currentNotification;
   private static final Logger logger = Logger.getLogger("bacnet.asn");

   public BufferReady() {
   }

   public BufferReady(BBacnetDeviceObjectPropertyReference bufferProperty, long previousNotification, long currentNotification) {
      this.bufferProperty = bufferProperty;
      this.previousNotification = previousNotification;
      this.currentNotification = currentNotification;
   }

   @Override
   public int getChoiceType() {
      return 10;
   }

   public BBacnetDeviceObjectPropertyReference getBufferProperty() {
      return this.bufferProperty;
   }

   public long getPreviousNotification() {
      return this.previousNotification;
   }

   public long getCurrentNotification() {
      return this.currentNotification;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeOpeningTag(10);
      outputStream.writeOpeningTag(0);
      this.bufferProperty.writeAsn(outputStream);
      outputStream.writeClosingTag(0);
      outputStream.writeUnsignedInteger(1, this.previousNotification);
      outputStream.writeUnsignedInteger(2, this.currentNotification);
      outputStream.writeClosingTag(10);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      this.readEncoded(null, inputStream);
   }

   @Override
   public void readEncoded(BBacnetObjectIdentifier deviceId, AsnInputStream inputStream) throws AsnException {
      int tag = inputStream.peekTag();
      if (inputStream.isOpeningTag(10)) {
         inputStream.skipTag();
         tag = inputStream.peekTag();
         if (inputStream.isOpeningTag(0)) {
            inputStream.skipTag();
            this.bufferProperty = new BBacnetDeviceObjectPropertyReference();
            this.bufferProperty.readAsn(inputStream);
            tag = inputStream.peekTag();
            if (inputStream.isClosingTag(0)) {
               inputStream.skipTag();
               this.previousNotification = inputStream.readUnsignedInteger(1);
               this.currentNotification = inputStream.readUnsignedInteger(2);
               tag = inputStream.peekTag();
               if (inputStream.isClosingTag(10)) {
                  inputStream.skipTag();
               } else {
                  throw new AsnException("Invalid tag: " + tag);
               }
            } else {
               throw new AsnException("Invalid tag: " + tag);
            }
         } else {
            throw new AsnException("Invalid tag: " + tag);
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   @Override
   public String toString(Context cx) {
      return cx != null && cx.equals(BacnetConst.facetsContext) ? this.toFacetString() : this.toString();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("  buffer-ready\n");
      sb.append("    " + this.bufferProperty);
      sb.append("    " + this.previousNotification);
      sb.append("    " + this.currentNotification);
      return sb.toString();
   }

   @Override
   public String toFacetString() {
      StringBuilder sb = new StringBuilder("evtVals");
      sb.append(".bufProp." + this.bufferProperty);
      sb.append(".prevN." + this.previousNotification);
      sb.append(".currN." + this.currentNotification);
      return sb.toString();
   }

   @Override
   public void addAlarmData(HashMap<String, ? super BIDataValue> map) {
      try {
         map.put("bufferProperty", BString.make(this.bufferProperty.encodeToString()));
         map.put("previousNotification", BString.make(String.valueOf(this.previousNotification)));
         map.put("currentNotification", BString.make(String.valueOf(this.currentNotification)));
      } catch (Exception var3) {
         logger.log(Level.SEVERE, "Failed to add alarm data", (Throwable)var3);
      }
   }
}
