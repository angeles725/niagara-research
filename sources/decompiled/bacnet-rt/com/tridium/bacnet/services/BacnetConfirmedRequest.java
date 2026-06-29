package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.services.confirmed.AcknowledgeAlarmRequest;
import com.tridium.bacnet.services.confirmed.AddListElementRequest;
import com.tridium.bacnet.services.confirmed.AtomicReadFileRequest;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedCovNotificationRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedEventNotificationRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedPrivateTransferRequest;
import com.tridium.bacnet.services.confirmed.CreateObjectRequest;
import com.tridium.bacnet.services.confirmed.DeleteObjectRequest;
import com.tridium.bacnet.services.confirmed.DeviceCommunicationControlRequest;
import com.tridium.bacnet.services.confirmed.GetAlarmSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEnrollmentSummaryRequest;
import com.tridium.bacnet.services.confirmed.GetEventInformationRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeRequest;
import com.tridium.bacnet.services.confirmed.ReinitializeDeviceRequest;
import com.tridium.bacnet.services.confirmed.RemoveListElementRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovPropertyRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.stack.transport.ConfirmedRequestPdu;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.log.Log;
import javax.baja.sys.InvalidEnumException;

public abstract class BacnetConfirmedRequest extends BacnetServicePrimitive implements BacnetConfirmedServiceChoice {
   protected int maxDataLength = -1;
   protected int maxDataLengthNoSeg = -1;
   public static final int RESPONSE_MAX_TRANSPORT_BYTES = 4;
   public static final int MAX_TRANSPORT_BYTES = 6;
   private static final Log logger = Log.getLog("bacnet.asn");

   protected BacnetConfirmedRequest(int serviceChoice) {
      super(0, serviceChoice);
   }

   public static final BacnetConfirmedRequest parseAPDU(ConfirmedRequestPdu pdu) throws RejectException {
      BacnetConfirmedRequest request = decodeServiceChoice(pdu.getServiceChoice());

      try {
         AsnInputStream asnIn = AsnInputStream.make(pdu.getServiceRequest());

         try {
            request.readEncoded(asnIn);
            determineMaxDataLength(pdu, request);
         } finally {
            asnIn.release();
         }

         return request;
      } catch (AsnException var8) {
         if (logger.isTraceOn()) {
            logger.trace("parseAPDU AsnException failure", var8);
         }

         throw new RejectException(4);
      } catch (InvalidEnumException var9) {
         if (logger.isTraceOn()) {
            logger.trace("parseAPDU InvalidEnumException failure", var9);
         }

         throw new RejectException(8);
      }
   }

   private static BacnetConfirmedRequest decodeServiceChoice(int serviceChoice) throws RejectException {
      switch (serviceChoice) {
         case 0:
            return new AcknowledgeAlarmRequest();
         case 1:
            return new ConfirmedCovNotificationRequest();
         case 2:
            return new ConfirmedEventNotificationRequest();
         case 3:
            return new GetAlarmSummaryRequest();
         case 4:
            return new GetEnrollmentSummaryRequest();
         case 5:
            return new SubscribeCovRequest();
         case 6:
            return new AtomicReadFileRequest();
         case 7:
            return new AtomicWriteFileRequest();
         case 8:
            return new AddListElementRequest();
         case 9:
            return new RemoveListElementRequest();
         case 10:
            return new CreateObjectRequest();
         case 11:
            return new DeleteObjectRequest();
         case 12:
            return new ReadPropertyRequest();
         case 13:
         case 19:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 27:
         default:
            throw new RejectException(9);
         case 14:
            return new ReadPropertyMultipleRequest();
         case 15:
            return new WritePropertyRequest();
         case 16:
            return new WritePropertyMultipleRequest();
         case 17:
            return new DeviceCommunicationControlRequest();
         case 18:
            return new ConfirmedPrivateTransferRequest();
         case 20:
            return new ReinitializeDeviceRequest();
         case 26:
            return new ReadRangeRequest();
         case 28:
            return new SubscribeCovPropertyRequest();
         case 29:
            return new GetEventInformationRequest();
      }
   }

   public final BacnetServicePrimitive parseAck(int serviceChoice, byte[] encodedAck) throws AsnException {
      if (encodedAck == null) {
         return new BacnetSimpleAck(serviceChoice);
      } else {
         AsnInputStream in = AsnInputStream.make(encodedAck);

         BacnetServicePrimitive var4;
         try {
            var4 = this.doParseAck(serviceChoice, in);
         } finally {
            in.release();
         }

         return var4;
      }
   }

   protected BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new BacnetSimpleAck(serviceChoice);
   }

   public final BacnetError parseError(int errorChoice, byte[] encodedError) throws AsnException {
      return this.doParseError(errorChoice, encodedError);
   }

   protected BacnetError doParseError(int errorChoice, byte[] encodedError) throws AsnException {
      return new SimpleError(errorChoice, encodedError);
   }

   protected static void determineMaxDataLength(ConfirmedRequestPdu pdu, BacnetConfirmedRequest req) {
      int maxApduLength = Math.min(pdu.getMaxAPDULengthAccepted(), BBacnetNetwork.localDevice().getMaxAPDULengthAccepted());
      req.maxDataLengthNoSeg = maxApduLength - 4;
      req.maxDataLength = maxApduLength - 4;
      if (pdu.isSegmentedResponseAccepted()) {
         int maxSegments = pdu.getMaxSegmentsAccepted();
         if (maxSegments < 0) {
            req.maxDataLength = -1;
         } else {
            if (maxSegments == 0) {
               return;
            }

            req.maxDataLength = maxSegments * req.maxDataLengthNoSeg;
         }
      }
   }

   public int getMaxDataLength() {
      return this.maxDataLength;
   }
}
