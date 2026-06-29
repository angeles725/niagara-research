package com.tridium.bacnet.services;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import com.tridium.bacnet.services.unconfirmed.TimeSynchronizationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedCovNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedEventNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedPrivateTransferRequest;
import com.tridium.bacnet.services.unconfirmed.UtcTimeSynchronizationRequest;
import com.tridium.bacnet.services.unconfirmed.WhoHasRequest;
import com.tridium.bacnet.services.unconfirmed.WhoIsRequest;
import com.tridium.bacnet.services.unconfirmed.WriteGroupRequest;
import com.tridium.bacnet.stack.transport.UnconfirmedRequestPdu;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.log.Log;

public abstract class BacnetUnconfirmedRequest extends BacnetServicePrimitive implements BacnetUnconfirmedServiceChoice {
   private static Log log = Log.getLog("bacnet.server");
   private static AsnInputStream staticInputStream = new AsnInputStream();

   protected BacnetUnconfirmedRequest(int serviceChoice) {
      super(1, serviceChoice);
   }

   public static final BacnetUnconfirmedRequest parseAPDU(UnconfirmedRequestPdu pdu) {
      BacnetUnconfirmedRequest request = decodeServiceChoice(pdu.getServiceChoice());
      if (request != null) {
         try {
            synchronized (staticInputStream) {
               staticInputStream.setBuffer(pdu.getServiceRequest());
               request.readEncoded(staticInputStream);
            }
         } catch (AsnException var5) {
            log.message(var5.getMessage());
            return null;
         } catch (RejectException var6) {
            log.message(var6.getMessage());
            return null;
         }
      }

      return request;
   }

   private static BacnetUnconfirmedRequest decodeServiceChoice(int serviceChoice) {
      switch (serviceChoice) {
         case 0:
            return new IAmRequest();
         case 1:
            return new IHaveRequest();
         case 2:
            return new UnconfirmedCovNotificationRequest();
         case 3:
            return new UnconfirmedEventNotificationRequest();
         case 4:
            return new UnconfirmedPrivateTransferRequest();
         case 5:
         default:
            if (log.isTraceOn()) {
               log.trace("Unsupported unconfirmed service " + serviceChoice);
            }

            return null;
         case 6:
            return new TimeSynchronizationRequest();
         case 7:
            return new WhoHasRequest();
         case 8:
            return new WhoIsRequest();
         case 9:
            return new UtcTimeSynchronizationRequest();
         case 10:
            return new WriteGroupRequest();
      }
   }
}
