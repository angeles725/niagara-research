package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.services.BacnetAbort;
import com.tridium.bacnet.services.BacnetComplexAck;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.BacnetInputStream;
import com.tridium.bacnet.stack.BacnetStackErrorCodes;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.io.AbortException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Queue;
import javax.baja.util.QueueFullException;
import javax.baja.util.Worker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "clientQueueSize",
      type = "int",
      defaultValue = "100"
   ), @NiagaraProperty(
      name = "serverQueueSize",
      type = "int",
      defaultValue = "100"
   ), @NiagaraProperty(
      name = "lockupThreshold",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(30)"
   )})
@NiagaraActions({@NiagaraAction(
      name = "dumpStats"
   ), @NiagaraAction(
      name = "resetStats"
   )})
public class BBacnetTransportLayer extends BComponent implements BacnetStackErrorCodes {
   public static final Property clientQueueSize = newProperty(0, 100, null);
   public static final Property serverQueueSize = newProperty(0, 100, null);
   public static final Property lockupThreshold = newProperty(0, BRelTime.makeSeconds(30), null);
   public static final Action dumpStats = newAction(0, null);
   public static final Action resetStats = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetTransportLayer.class);
   private BBacnetTransportLayer.TransportHelper server;
   private BBacnetTransportLayer.TransportHelper client;
   private ServerStateMachine ssm;
   private ClientStateMachine csm;
   public static final Logger logger = Logger.getLogger("bacnet.transport");

   public int getClientQueueSize() {
      return this.getInt(clientQueueSize);
   }

   public void setClientQueueSize(int v) {
      this.setInt(clientQueueSize, v, null);
   }

   public int getServerQueueSize() {
      return this.getInt(serverQueueSize);
   }

   public void setServerQueueSize(int v) {
      this.setInt(serverQueueSize, v, null);
   }

   public BRelTime getLockupThreshold() {
      return (BRelTime)this.get(lockupThreshold);
   }

   public void setLockupThreshold(BRelTime v) {
      this.set(lockupThreshold, v, null);
   }

   public void dumpStats() {
      this.invoke(dumpStats, null, null);
   }

   public void resetStats() {
      this.invoke(resetStats, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() {
      TransportStateMachine.startStack((BBacnetStack)this.getParent());
      this.ssm = new ServerStateMachine();
      this.csm = new ClientStateMachine();
      this.server = new BBacnetTransportLayer.TransportHelper("BnTSvr", this.ssm, new Queue(this.getServerQueueSize()));
      this.client = new BBacnetTransportLayer.TransportHelper("BnTCli", this.csm, new Queue(this.getClientQueueSize()));
      this.server.start();
      this.client.start();
   }

   public void stackStopped() {
      this.server.stop();
      this.client.stop();
      this.server = null;
      this.client = null;
      TransportStateMachine.stopStack();
      this.ssm = null;
      this.csm = null;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(clientQueueSize)) {
            this.client.stop();
            this.client = new BBacnetTransportLayer.TransportHelper("BnTCli", this.csm, new Queue(this.getClientQueueSize()));
            this.client.start();
         } else if (p.equals(serverQueueSize)) {
            this.server.stop();
            this.server = new BBacnetTransportLayer.TransportHelper("BnTSvr", this.ssm, new Queue(this.getServerQueueSize()));
            this.server.start();
         } else if (p.equals(lockupThreshold)) {
            if (cx == BacnetConst.fallback) {
               return;
            }

            int retries = BBacnetNetwork.localDevice().getNumberOfApduRetries();
            int tout = BBacnetNetwork.localDevice().getApduTimeout();
            int maxWaitTime = tout * (retries + 1);
            if (this.getLockupThreshold().getMillis() < maxWaitTime) {
               logger.info("Lockup Threshold set too short - resetting to minimum based on APDU_Timeout and Number_Of_APDU_Retries");
               this.set(lockupThreshold, BRelTime.make(maxWaitTime), BacnetConst.fallback);
            }
         }
      }
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetStack;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps("BACnet Transport Layer");
      out.trTitle("Client", 2);
      this.client.spy(out);
      out.trTitle("Server", 2);
      this.server.spy(out);
      out.endProps();
   }

   public BacnetSimpleAck sendConfirmedRequestSimple(BacnetConfirmedRequest request, BBacnetAddress destAddress, BNetworkPriority networkPriority) throws BacnetException {
      return (BacnetSimpleAck)this.sendConfirmedRequest(request, destAddress, networkPriority, 2);
   }

   public BacnetComplexAck sendConfirmedRequestComplex(BacnetConfirmedRequest request, BBacnetAddress destAddress, BNetworkPriority networkPriority) throws BacnetException {
      return (BacnetComplexAck)this.sendConfirmedRequest(request, destAddress, networkPriority, 3);
   }

   public void sendUnconfirmedRequest(BacnetUnconfirmedRequest request, BBacnetAddress destAddress, BNetworkPriority networkPriority) throws BacnetException {
      if (destAddress == null) {
         throw new BacnetStackException("Unresolved device address");
      } else {
         UnconfirmedRequestPdu apdu = new UnconfirmedRequestPdu(destAddress, networkPriority, request.getServiceChoice(), request.toEncodedBytes());

         try {
            this.client.queueApplication(apdu);
         } catch (QueueFullException var6) {
            logger.log(
               Level.WARNING,
               "BBacnetTransportLayer.sendUnconfirmedRequest: QueueFullException: " + var6 + "; apdu: " + apdu,
               (Throwable)(logger.isLoggable(Level.FINE) ? var6 : null)
            );
            throw new BacnetStackException("Transport Queue Overflow", var6);
         }
      }
   }

   private BacnetServicePrimitive sendConfirmedRequest(
      BacnetConfirmedRequest request, BBacnetAddress destAddress, BNetworkPriority networkPriority, int expectedResponse
   ) throws BacnetException {
      if (destAddress == null) {
         throw new BacnetStackException("Unresolved device address");
      } else if (destAddress.getMacAddress().equals(BBacnetOctetString.DEFAULT)) {
         throw new BacnetStackException("Unresolved device address");
      } else {
         int invokeId = IdManager.getInvokeId(destAddress);
         ConfirmedRequestPdu apdu = null;

         BacnetServicePrimitive var21;
         try {
            int protocolRevision = DeviceRegistry.getProtocolRevision(destAddress);
            apdu = new ConfirmedRequestPdu(
               this.network().getAddress(destAddress.getNetworkNumber()),
               destAddress,
               networkPriority,
               invokeId,
               request.getServiceChoice(),
               request.toEncodedBytes(),
               protocolRevision
            );
            apdu.setMaxAPDULengthAccepted(BBacnetNetwork.localDevice().getMaxAPDULengthAccepted());
            apdu.setSegmentedResponseAccepted(BBacnetNetwork.localDevice().getSegmentationSupported().isSegmentedReceive());
            this.client.queueApplication(apdu);
            ApplicationPdu confApdu = apdu.waitForConfirmation();
            BacnetServicePrimitive conf;
            switch (confApdu.getType()) {
               case 2:
                  if (expectedResponse != 2) {
                     throw new InvalidApduTypeException("Invalid Response Type", expectedResponse, 2);
                  }

                  conf = request.parseAck(((SimpleAckPdu)confApdu).getServiceAckChoice(), null);
                  break;
               case 3:
                  if (expectedResponse != 3) {
                     throw new InvalidApduTypeException("Invalid Response Type", expectedResponse, 3);
                  }

                  conf = request.parseAck(((ComplexAckPdu)confApdu).getServiceAckChoice(), ((ComplexAckPdu)confApdu).getServiceAck());
                  break;
               case 4:
               default:
                  throw new BacnetStackException("Unknown PDU type received: " + confApdu.getType());
               case 5:
                  BacnetError error = request.parseError(((ErrorPdu)confApdu).getErrorChoice(), ((ErrorPdu)confApdu).getError());
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Error PDU received:" + error);
                  }

                  throw new ErrorException(error.getError(), error.getErrorParameters());
               case 6:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Reject PDU received:" + confApdu);
                  }

                  throw new RejectException(((RejectPdu)confApdu).getRejectReason());
               case 7:
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("Abort PDU received:" + confApdu);
                  }

                  throw new AbortException(((AbortPdu)confApdu).getAbortReason());
            }

            var21 = conf;
         } catch (QueueFullException var19) {
            logger.log(
               Level.SEVERE,
               "BBacnetTransportLayer.sendConfirmedRequest: QueueFullException: " + var19 + "; apdu: " + apdu,
               (Throwable)(logger.isLoggable(Level.FINE) ? var19 : null)
            );
            throw new AbortException(9);
         } finally {
            try {
               IdManager.releaseId(destAddress, invokeId);
            } catch (Exception var18) {
            }
         }

         return var21;
      }
   }

   public void sendConfirmedResponse(BBacnetAddress sourceAddress, int originalInvokeId, BacnetSimpleAck ack, BNetworkPriority networkPriority) {
      SimpleAckPdu apdu = new SimpleAckPdu(sourceAddress, networkPriority, originalInvokeId, ack.getServiceChoice());

      try {
         this.server.queueApplication(apdu);
      } catch (QueueFullException var7) {
         logger.log(
            Level.SEVERE,
            "BBacnetTransportLayer.sendConfirmedResponse (SimpleAck): QueueFullException: " + var7 + "; apdu: " + apdu,
            (Throwable)(logger.isLoggable(Level.FINE) ? var7 : null)
         );
      }
   }

   public void sendConfirmedResponse(BBacnetAddress sourceAddress, int originalInvokeId, BacnetComplexAck ack, BNetworkPriority networkPriority) {
      ComplexAckPdu apdu = new ComplexAckPdu(sourceAddress, networkPriority, originalInvokeId, ack.getServiceChoice(), ack.toEncodedBytes());

      try {
         this.server.queueApplication(apdu);
      } catch (QueueFullException var7) {
         logger.log(
            Level.SEVERE,
            "BBacnetTransportLayer.sendConfirmedResponse (ComplexAck): QueueFullException: " + var7 + "; apdu: " + apdu,
            (Throwable)(logger.isLoggable(Level.FINE) ? var7 : null)
         );
      }
   }

   public void sendConfirmedResponse(BBacnetAddress sourceAddress, int originalInvokeId, BacnetError error, BNetworkPriority networkPriority) {
      ErrorPdu apdu = new ErrorPdu(sourceAddress, networkPriority, error.getServiceChoice(), originalInvokeId, error.toEncodedBytes());

      try {
         this.server.queueApplication(apdu);
      } catch (QueueFullException var7) {
         logger.log(
            Level.SEVERE,
            "BBacnetTransportLayer.sendConfirmedResponse (Error): QueueFullException: " + var7 + "; apdu: " + apdu,
            (Throwable)(logger.isLoggable(Level.FINE) ? var7 : null)
         );
      }
   }

   public void sendConfirmedResponse(BBacnetAddress sourceAddress, int originalInvokeId, BacnetAbort abort, BNetworkPriority networkPriority) {
      AbortPdu apdu = new AbortPdu(sourceAddress, networkPriority, originalInvokeId, abort.getServiceChoice(), true);

      try {
         this.server.queueApplication(apdu);
      } catch (QueueFullException var7) {
         logger.log(
            Level.SEVERE,
            "BBacnetTransportLayer.sendConfirmedResponse (Abort): QueueFullException: " + var7 + "; apdu: " + apdu,
            (Throwable)(logger.isLoggable(Level.FINE) ? var7 : null)
         );
      }
   }

   public void sendConfirmedResponse(BBacnetAddress sourceAddress, int originalInvokeId, BacnetReject reject, BNetworkPriority networkPriority) {
      RejectPdu apdu = new RejectPdu(sourceAddress, networkPriority, originalInvokeId, reject.getServiceChoice());

      try {
         this.server.queueApplication(apdu);
      } catch (QueueFullException var7) {
         logger.log(
            Level.SEVERE,
            "BBacnetTransportLayer.sendConfirmedResponse (Reject): QueueFullException: " + var7 + "; apdu: " + apdu,
            (Throwable)(logger.isLoggable(Level.FINE) ? var7 : null)
         );
      }
   }

   public void receiveIndication(
      BBacnetAddress srcAddress,
      BBacnetAddress destAddress,
      BacnetInputStream is,
      BNetworkPriority networkPriority,
      boolean dataExpectingReply,
      boolean isBroadcast
   ) {
      ApplicationPdu apdu = null;

      try {
         apdu = ApplicationPdu.parseEncodedBytes(srcAddress, destAddress, is, networkPriority, dataExpectingReply, isBroadcast);
         is.release();
         if (apdu.isClientPDU()) {
            this.server.queueNetwork(apdu);
         } else {
            this.client.queueNetwork(apdu);
         }
      } catch (QueueFullException var9) {
         logger.log(
            Level.SEVERE,
            "QueueFullException while routing incoming APDU: " + apdu + ", exception: " + var9,
            (Throwable)(logger.isLoggable(Level.FINE) ? var9 : null)
         );
      } catch (BacnetException var10) {
         logger.log(Level.SEVERE, "Unable to parse incoming APDU!", (Throwable)var10);
      }
   }

   void serverRequestTimeout(ApplicationPdu apdu) {
      if (((BBacnetStack)this.getParent()).isCommExecutionEnabled()) {
         this.server.queueRequestTimeout(apdu);
      }
   }

   void clientRequestTimeout(ApplicationPdu apdu) {
      this.client.queueRequestTimeout(apdu);
   }

   void serverSegmentTimeout(ApplicationPdu apdu) {
      if (((BBacnetStack)this.getParent()).isCommExecutionEnabled()) {
         this.server.queueSegmentTimeout(apdu);
      }
   }

   void clientSegmentTimeout(ApplicationPdu apdu) {
      this.client.queueSegmentTimeout(apdu);
   }

   private BBacnetNetworkLayer network() {
      return ((BBacnetStack)this.getParent()).getNetwork();
   }

   public void doDumpStats() {
      System.out.println("Client Transaction List:");
      ClientTransaction.activeTransactions.dump();
      System.out.println("\n\nServer Transaction List:");
      ServerTransaction.activeTransactions.dump();
   }

   public void doResetStats() {
   }

   static class TransportHelper extends Worker {
      TransportStateMachine tsm;
      String name;
      Queue q;

      public TransportHelper(String name, TransportStateMachine tsm, Queue q) {
         super(q);
         this.q = q;
         this.name = name;
         this.tsm = tsm;
      }

      public String getName() {
         return this.name;
      }

      public void start() {
         this.start(this.name);
      }

      public void queueApplication(ApplicationPdu apdu) {
         this.enqueue(apdu, 0);
      }

      public void queueNetwork(ApplicationPdu apdu) {
         this.enqueue(apdu, 1);
      }

      public void queueSegmentTimeout(ApplicationPdu apdu) {
         this.enqueue(apdu, 3);
      }

      public void queueRequestTimeout(ApplicationPdu apdu) {
         this.enqueue(apdu, 2);
      }

      private void enqueue(ApplicationPdu apdu, int source) {
         apdu.setSource(source);
         apdu.setTSM(this.tsm);

         try {
            this.q.enqueue(apdu);
         } catch (QueueFullException var4) {
            BBacnetTransportLayer.logger
               .warning(
                  "BBacnetTransportLayer.TransportHelper.enqueue threw QueueFullException; "
                     + this.getName()
                     + ": current size "
                     + this.q.size()
                     + " >= maxSize "
                     + this.q.maxSize()
                     + "; apdu: "
                     + apdu
               );
            throw var4;
         }
      }
   }
}
