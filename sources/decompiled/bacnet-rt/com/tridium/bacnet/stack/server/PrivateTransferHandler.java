package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetAbort;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.confirmed.ConfirmedPrivateTransferAck;
import com.tridium.bacnet.services.confirmed.ConfirmedPrivateTransferRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedPrivateTransferRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.io.AbortException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.PrivateTransferListener;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.IntHashMap;

public class PrivateTransferHandler implements ServiceHandler, BacnetUnconfirmedServiceChoice, BacnetConfirmedServiceChoice {
   IntHashMap confirmedListeners = new IntHashMap();
   IntHashMap unconfirmedListeners = new IntHashMap();
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private static final byte[] CANNED_CPT_RESPONSE = new byte[]{117, 9, 0, 67, 80, 84, 32, 82, 69, 83, 80};

   PrivateTransferHandler(BBacnetServerLayer server) {
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 4:
            this.processUnconfirmedPrivateTransferRequest((UnconfirmedPrivateTransferRequest)request, sourceAddress);
            return null;
         case 18:
            return this.processConfirmedPrivateTransferRequest((ConfirmedPrivateTransferRequest)request, sourceAddress);
         default:
            logger.info("PrivateTransferHandler.reqceiveRequest:Unknown request! " + request);
            return null;
      }
   }

   private void processUnconfirmedPrivateTransferRequest(UnconfirmedPrivateTransferRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PrivateTransferHandler: UnconfirmedPrivateTransferRequest received: " + request);
      }

      try {
         this.routeToListener(request, sourceAddress);
      } catch (Exception var4) {
         logger.log(Level.INFO, "Error handling Unconfirmed Private Transfer request:" + var4, (Throwable)var4);
      }
   }

   private BacnetServicePrimitive processConfirmedPrivateTransferRequest(ConfirmedPrivateTransferRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PrivateTransferHandler: ConfirmedPrivateTransferRequest received: " + request);
      }

      try {
         byte[] resultBlock = this.routeToListener(request, sourceAddress);
         return new ConfirmedPrivateTransferAck(request.getVendorId(), request.getServiceNumber(), resultBlock);
      } catch (ErrorException var4) {
         return new SimpleError(18, var4.getErrorType());
      } catch (RejectException var5) {
         return new BacnetReject(var5.getRejectReason());
      } catch (AbortException var6) {
         return new BacnetAbort(var6.getAbortReason());
      } catch (BacnetException var7) {
         return new SimpleError(18, new NErrorType(5, 0));
      }
   }

   void addListener(PrivateTransferListener listener, boolean confirmed) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PTHandler.addListener(" + listener + (confirmed ? ", confirmed)" : ", unconfirmed)"));
      }

      if (confirmed) {
         synchronized (this.confirmedListeners) {
            this.confirmedListeners.put(listener.getVendorId(), listener);
         }
      } else {
         synchronized (this.unconfirmedListeners) {
            this.unconfirmedListeners.put(listener.getVendorId(), listener);
         }
      }
   }

   void removeListener(PrivateTransferListener listener, boolean confirmed) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("PTHandler.removeListener(" + listener + (confirmed ? ", confirmed)" : ", unconfirmed)"));
      }

      int vId = listener.getVendorId();
      if (confirmed) {
         synchronized (this.confirmedListeners) {
            PrivateTransferListener l = (PrivateTransferListener)this.confirmedListeners.get(vId);
            if (l == null || l != listener) {
               throw new IllegalArgumentException("Incorrect listener provided to removeListener: " + listener + " != " + l);
            }

            this.confirmedListeners.remove(vId);
         }
      } else {
         synchronized (this.unconfirmedListeners) {
            PrivateTransferListener l = (PrivateTransferListener)this.unconfirmedListeners.get(vId);
            if (l == null || l != listener) {
               throw new IllegalArgumentException("Incorrect listener provided to removeListener: " + listener + " != " + l);
            }

            this.unconfirmedListeners.remove(vId);
         }
      }
   }

   private byte[] routeToListener(ConfirmedPrivateTransferRequest request, BBacnetAddress sourceAddress) throws BacnetException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("routeToListener:" + request + " from " + sourceAddress);
      }

      PrivateTransferListener listener = null;
      synchronized (this.confirmedListeners) {
         listener = (PrivateTransferListener)this.confirmedListeners.get((int)request.getVendorId());
      }

      if (listener != null) {
         return listener.receiveConfirmedPrivateTransfer(request.getVendorId(), request.getServiceNumber(), request.getServiceParameters(), sourceAddress);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               "Received ConfirmedPrivateTransfer (vID "
                  + request.getVendorId()
                  + ", svcNum "
                  + request.getServiceNumber()
                  + "); no listeners for this vendorId!"
            );
         }

         throw new AbortException(0);
      }
   }

   private void routeToListener(UnconfirmedPrivateTransferRequest request, BBacnetAddress sourceAddress) throws BacnetException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("routeToListener:" + request + " from " + sourceAddress);
      }

      PrivateTransferListener listener = null;
      synchronized (this.unconfirmedListeners) {
         listener = (PrivateTransferListener)this.unconfirmedListeners.get((int)request.getVendorId());
      }

      if (listener != null) {
         listener.receiveUnconfirmedPrivateTransfer(request.getVendorId(), request.getServiceNumber(), request.getServiceParameters(), sourceAddress);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            "Received UnconfirmedPrivateTransfer (vID "
               + request.getVendorId()
               + ", svcNum "
               + request.getServiceNumber()
               + "); no listeners for this vendorId!"
         );
      }
   }

   private static void d(String s) {
      System.out.println(s);
   }

   static class DebugPTListener implements PrivateTransferListener {
      private int vendorId;

      public DebugPTListener(int vId) {
         this.vendorId = vId;
      }

      @Override
      public int getVendorId() {
         return this.vendorId;
      }

      @Override
      public byte[] receiveConfirmedPrivateTransfer(long vendorId, long serviceNumber, byte[] serviceParameters, BBacnetAddress sourceAddress) throws BacnetException {
         PrivateTransferHandler.d(
            "\n\n*****\n\nDebugPTListener: received Confirmed PT Request: vId " + vendorId + ", svcNum " + serviceNumber + " from " + sourceAddress + ":"
         );
         ByteArrayUtil.hexDump(serviceParameters);
         PrivateTransferHandler.d("\n\n*****\n\n");
         PrivateTransferHandler.d("Returning " + ByteArrayUtil.toHexString(PrivateTransferHandler.CANNED_CPT_RESPONSE));
         return PrivateTransferHandler.CANNED_CPT_RESPONSE;
      }

      @Override
      public void receiveUnconfirmedPrivateTransfer(long vendorId, long serviceNumber, byte[] serviceParameters, BBacnetAddress sourceAddress) throws BacnetException {
         PrivateTransferHandler.d(
            "\n\n*****\n\nDebugPTListener: received Unconfirmed PT Request: vId " + vendorId + ", svcNum " + serviceNumber + " from " + sourceAddress + ":"
         );
         ByteArrayUtil.hexDump(serviceParameters);
         PrivateTransferHandler.d("\n\n*****\n\n");
      }
   }
}
