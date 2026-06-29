package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.sys.BEnum;

public class AcknowledgeAlarmRequest extends BacnetConfirmedRequest {
   public static final int ACKNOWLEDGING_PROCESS_ID_TAG = 0;
   public static final int EVENT_OBJECT_ID_TAG = 1;
   public static final int EVENT_STATE_ACKNOWLEDGED_TAG = 2;
   public static final int TIMESTAMP_TAG = 3;
   public static final int ACKNOWLEDGEMENT_SOURCE_TAG = 4;
   public static final int TIME_OF_ACKNOWLEDGEMENT_TAG = 5;
   private long acknowledgingProcessId;
   private BBacnetObjectIdentifier eventObjectId;
   private BEnum eventStateAcknowledged;
   private BBacnetTimeStamp timeStamp;
   private String acknowledgementSource;
   private BBacnetTimeStamp timeOfAcknowledgement;
   private BCharacterSetEncoding encoding;
   private static final Logger logger = Logger.getLogger("bacnet.alarm");

   public AcknowledgeAlarmRequest() {
      super(0);
   }

   public AcknowledgeAlarmRequest(
      long acknowledgingProcessId,
      BBacnetObjectIdentifier eventObjectId,
      BEnum eventStateAcknowledged,
      BBacnetTimeStamp timeStamp,
      String acknowledgementSource,
      BBacnetTimeStamp timeOfAcknowledgement,
      BCharacterSetEncoding encoding
   ) {
      super(0);
      this.acknowledgingProcessId = acknowledgingProcessId;
      this.eventObjectId = eventObjectId;
      this.eventStateAcknowledged = eventStateAcknowledged;
      this.timeStamp = timeStamp;
      this.acknowledgementSource = acknowledgementSource;
      this.timeOfAcknowledgement = timeOfAcknowledgement;
      this.encoding = encoding;
   }

   public long getAcknowledgingProcessId() {
      return this.acknowledgingProcessId;
   }

   public BBacnetObjectIdentifier getEventObjectId() {
      return this.eventObjectId;
   }

   public BEnum getEventStateAcknowledged() {
      return this.eventStateAcknowledged;
   }

   public BBacnetTimeStamp getTimeStamp() {
      return this.timeStamp;
   }

   public String getAcknowledgementSource() {
      return this.acknowledgementSource;
   }

   public BBacnetTimeStamp getTimeOfAcknowledgement() {
      return this.timeOfAcknowledgement;
   }

   public BCharacterSetEncoding getEncoding() {
      return this.encoding;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      outputStream.writeUnsignedInteger(0, this.acknowledgingProcessId);
      outputStream.writeObjectIdentifier(1, this.eventObjectId);
      outputStream.writeEnumerated(2, this.eventStateAcknowledged.getOrdinal());
      outputStream.writeOpeningTag(3);
      this.timeStamp.writeAsn(outputStream);
      outputStream.writeClosingTag(3);
      outputStream.writeCharacterString(4, this.acknowledgementSource, this.encoding);
      outputStream.writeOpeningTag(5);
      this.timeOfAcknowledgement.writeAsn(outputStream);
      outputStream.writeClosingTag(5);
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException, RejectException {
      this.acknowledgingProcessId = inputStream.readUnsignedInteger(0);
      this.eventObjectId = inputStream.readObjectIdentifier(1);
      this.eventStateAcknowledged = BBacnetEventState.make(inputStream.readEnumerated(2));
      inputStream.skipTag();
      this.timeStamp = new BBacnetTimeStamp();
      this.timeStamp.readAsn(inputStream);
      inputStream.skipTag();
      this.encoding = inputStream.peekEncoding(4);
      this.acknowledgementSource = readAcknowledgementSource(inputStream);
      inputStream.skipTag();
      this.timeOfAcknowledgement = new BBacnetTimeStamp();
      this.timeOfAcknowledgement.readAsn(inputStream);
      inputStream.skipTag();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("AcknowledgeAlarmRequest: ");
      sb.append("\n  " + this.acknowledgingProcessId);
      sb.append("\n  " + this.eventObjectId);
      sb.append("\n  " + this.eventStateAcknowledged);
      sb.append("\n  " + this.timeStamp);
      sb.append("\n  " + this.acknowledgementSource);
      sb.append("\n  " + this.timeOfAcknowledgement);
      sb.append("\n  " + this.encoding);
      return sb.toString();
   }

   private static String readAcknowledgementSource(AsnInputStream inputStream) throws AsnException {
      try {
         inputStream.mark(-1);
         return inputStream.readCharacterString(4);
      } catch (Exception var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Exception occurred during decoding of acknowledgementSource", (Throwable)var2);
         }

         inputStream.reset();
         inputStream.skipCharacterString(4);
         return "";
      }
   }
}
