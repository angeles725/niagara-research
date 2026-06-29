package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.log.Log;

public class ClientStateMachine extends TransportStateMachine {
   private static final Log logger = Log.getLog("bacnet.transport");

   @Override
   public String getName() {
      return "BacnetClientTSM";
   }

   @Override
   public void route(ApplicationPdu apdu) {
      process(apdu);
   }

   public static final void process(ApplicationPdu apdu) {
      ClientTransaction transaction = null;

      try {
         transaction = ClientTransaction.find(apdu);
         if (transaction == null) {
            ClientStateMachine.Idle.process(apdu);
            return;
         }

         switch (transaction.getState()) {
            case 1:
               ClientStateMachine.SegmentedRequest.process(apdu, transaction);
               break;
            case 2:
               ClientStateMachine.AwaitConfirmation.process(apdu, transaction);
               break;
            case 3:
               ClientStateMachine.SegmentedConf.process(apdu, transaction);
               break;
            default:
               throw new IllegalStateException("CSM.process():" + apdu.trace());
         }
      } catch (UnsupportedOperationException var3) {
         if (transaction != null) {
            transaction.stopRequestTimer();
         }

         if (apdu != null && apdu instanceof ConfirmedRequestPdu) {
            ((ConfirmedRequestPdu)apdu).cannotSend(var3);
         }
      } catch (Exception var4) {
         if (logger.isLoggable(1)) {
            logger.message("Exception in CSM.process:transaction=" + transaction + " apdu=" + apdu.trace() + "\n exception:", var4);
         }

         if (transaction != null) {
            logger.message("transaction pdu:" + transaction.getRequestPdu().trace());
            transaction.stopRequestTimer();
         }

         if (apdu != null && apdu instanceof ConfirmedRequestPdu) {
            ((ConfirmedRequestPdu)apdu).abandon(var4);
         }
      }
   }

   private static void sendConfirmedRequest(ClientTransaction transaction, ConfirmedRequestPdu apdu) {
      int devMaxLen = DeviceRegistry.getMaxApduLengthSupported(apdu.getServerAddress());
      int myMaxLen = apdu.getMaxAPDULengthAccepted();
      int maxLength = Math.min(devMaxLen, myMaxLen);
      if (apdu.getLength() + 4 > maxLength) {
         BBacnetSegmentation seg = DeviceRegistry.getSegmentationSupported(apdu.getServerAddress());
         if (!BBacnetNetwork.localDevice().getSegmentationSupported().isSegmentedTransmit()) {
            throw new UnsupportedOperationException("Cannot send segmented packet: Niagara not configured for segmentation");
         }

         int deviceMaxSegs = DeviceRegistry.getMaxSegmentsAccepted(apdu.getServerAddress());
         if (!seg.isSegmentedReceive()) {
            throw new UnsupportedOperationException(
               "For device address: "
                  + apdu.getServerAddress()
                  + ": Cannot send segmented packet: device does not support segmentation "
                  + seg
                  + "; device maxAPDULength: "
                  + devMaxLen
                  + "; maxSegmentsAccepted: "
                  + deviceMaxSegs
            );
         }

         int numSegments = apdu.getLength() / (maxLength - 6) + 1;
         if (!ConfirmedRequestPdu.canFit(deviceMaxSegs, numSegments)) {
            throw new UnsupportedOperationException(
               "For device address: "
                  + apdu.getServerAddress()
                  + ": Cannot send segmented packet: number of segments required "
                  + numSegments
                  + " exceeds device's max segments accepted: "
                  + deviceMaxSegs
                  + "; device maxAPDULength: "
                  + devMaxLen
                  + "; segmentation:  "
                  + seg
            );
         }

         sendConfirmedRequestSegmented(transaction, apdu, maxLength, numSegments);
      } else {
         sendConfirmedRequestUnsegmented(transaction, apdu);
      }
   }

   private static void sendConfirmedRequestUnsegmented(ClientTransaction transaction, ConfirmedRequestPdu apdu) {
      transaction.setSentAllSegments(true);
      apdu.setSegmentedMessage(false);
      network().sendRequest(transaction.getServerAddress(), apdu);
      transaction.setState(2);
      transaction.startRequestTimer(BBacnetNetwork.localDevice().getApduTimeout());
   }

   private static void sendConfirmedRequestSegmented(ClientTransaction transaction, ConfirmedRequestPdu apdu, int maxSegmentLength, int numSegments) {
      apdu.setSegmentedMessage(true);
      ConfirmedRequestPdu seg = new ConfirmedRequestPdu(apdu, 0, maxSegmentLength, 0);
      transaction.setSentAllSegments(false);
      transaction.setSegmentRetryCount(0);
      transaction.setInitialSequenceNumber(0);
      transaction.setSegmentCounter(0);
      transaction.setProposedWindowSize(DEFAULT_SEGMENTATION_WINDOW_SIZE);
      transaction.setActualWindowSize(1);
      transaction.setSegmentSize(maxSegmentLength);
      transaction.setNumSegments(numSegments);
      seg.setMoreFollows(true);
      seg.setProposedWindowSize(transaction.getProposedWindowSize());
      network().sendRequest(transaction.getServerAddress(), seg);
      transaction.setState(1);
      transaction.startSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
   }

   private static void sendAbort(ApplicationPdu apdu, int abortReason) {
      AbortPdu abort = new AbortPdu(apdu.getServerAddress(), apdu.getPriority(), apdu.getInvokeId(), abortReason, false);
      network().sendRequest(apdu.getServerAddress(), abort);
   }

   private static void fillWindow(ClientTransaction transaction) {
      int ix = 0;
      int lastSegment = transaction.getNumSegments() - 1;
      int sequenceNumber = transaction.getInitialSequenceNumber();
      int segSize = transaction.getSegmentSize();
      int segmentCounter = transaction.getSegmentCounter();
      ConfirmedRequestPdu req = transaction.getRequestPdu();

      do {
         int seqNum = sequenceNumber + ix;
         int segCtr = segmentCounter + ix;
         if (segCtr >= lastSegment) {
            ConfirmedRequestPdu seg = new ConfirmedRequestPdu(req, modulo(lastSegment, 256), segSize, segCtr);
            seg.setSegmentedMessage(true);
            seg.setMoreFollows(false);
            seg.setProposedWindowSize(transaction.getProposedWindowSize());
            network().sendRequest(transaction.getServerAddress(), seg);
            transaction.setSentAllSegments(true);
            return;
         }

         ConfirmedRequestPdu seg = new ConfirmedRequestPdu(req, modulo(seqNum, 256), segSize, segCtr);
         seg.setSegmentedMessage(true);
         seg.setMoreFollows(true);
         seg.setProposedWindowSize(transaction.getProposedWindowSize());
         network().sendRequest(transaction.getServerAddress(), seg);
      } while (++ix < transaction.getActualWindowSize());
   }

   private static void sendSegmentNak(ClientTransaction transaction, int sequenceNumber) {
      sendSegmentAck(transaction, true, sequenceNumber);
   }

   private static void sendSegmentAck(ClientTransaction transaction, boolean negativeAck, int sequenceNumber) {
      SegmentAckPdu segAck = new SegmentAckPdu(negativeAck, false, transaction.getInvokeId(), sequenceNumber, transaction.getActualWindowSize());
      network().sendRequest(transaction.getServerAddress(), segAck);
   }

   private static void receiveSegmentedComplexAck(ClientTransaction transaction, ComplexAckPdu complexAck) {
      transaction.stopRequestTimer();
      transaction.setComplexAck(complexAck);
      transaction.setActualWindowSize(complexAck.getProposedWindowSize());
      transaction.setLastSequenceNumber(0);
      transaction.setInitialSequenceNumber(0);
      transaction.setDuplicateCount(0);
      sendSegmentAck(transaction, false, 0);
      transaction.startSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
      transaction.setState(3);
   }

   private static BBacnetNetworkLayer network() {
      return stack.getNetwork();
   }

   private static class AwaitConfirmation {
      public static final void process(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetworkIndication(apdu, transaction);
               break;
            case 2:
               processRequestTimeout(apdu, transaction);
               break;
            default:
               throw new IllegalStateException("AwaitConfirmation.process():" + apdu.trace());
         }
      }

      private static void processApplication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 7:
               transaction.stopRequestTimer();
               ClientStateMachine.network().sendRequest(transaction.getServerAddress(), apdu);
               transaction.setState(0);
               return;
            default:
               throw new IllegalStateException("AwaitConfirmation.processApplication():" + apdu.trace());
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 2:
               transaction.stopRequestTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            case 3:
               ComplexAckPdu complexAck = (ComplexAckPdu)apdu;
               if (complexAck.isSegmentedMessage()) {
                  if (!BBacnetNetwork.localDevice().getSegmentationSupported().isSegmentedReceive()) {
                     transaction.stopRequestTimer();
                     transaction.timeout();
                     ClientStateMachine.sendAbort(apdu, 4);
                     transaction.setState(0);
                  } else if (complexAck.getSequenceNumber() != 0) {
                     transaction.stopRequestTimer();
                     transaction.timeout();
                     ClientStateMachine.sendAbort(apdu, 2);
                     transaction.setState(0);
                  } else {
                     ClientStateMachine.receiveSegmentedComplexAck(transaction, complexAck);
                  }
               } else {
                  transaction.stopRequestTimer();
                  transaction.postConfirmation(apdu);
                  transaction.setState(0);
               }
            case 4:
               break;
            case 5:
               transaction.stopRequestTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            case 6:
               transaction.stopRequestTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            case 7:
               transaction.stopRequestTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            default:
               throw new IllegalStateException("AwaitConfirmation.processNetworkIndication():" + apdu.trace());
         }
      }

      private static void processRequestTimeout(ApplicationPdu apdu, ClientTransaction transaction) {
         int numRetries = transaction.getRetryCount();
         if (numRetries < BBacnetNetwork.localDevice().getNumberOfApduRetries()) {
            transaction.setRetryCount(++numRetries);
            ClientStateMachine.sendConfirmedRequest(transaction, (ConfirmedRequestPdu)apdu);
         } else {
            transaction.stopRequestTimer();
            transaction.timeout();
            transaction.setState(0);
         }
      }
   }

   static class Idle {
      public static void process(ApplicationPdu apdu) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu);
               break;
            case 1:
               processNetworkIndication(apdu);
               break;
            default:
               ClientStateMachine.logger.warning("invalid apdu source:" + apdu.getSource());
               throw new IllegalStateException("Idle.process():" + apdu.trace());
         }
      }

      private static void processApplication(ApplicationPdu apdu) {
         int apduType = apdu.getType();
         switch (apduType) {
            case 0:
               ClientTransaction transaction = new ClientTransaction((ConfirmedRequestPdu)apdu);
               transaction.setRetryCount(0);
               ClientStateMachine.sendConfirmedRequest(transaction, (ConfirmedRequestPdu)apdu);
               break;
            case 1:
               ClientStateMachine.network().sendRequest(apdu.getServerAddress(), apdu);
               break;
            default:
               throw new IllegalStateException("Idle.processApplication():" + apdu.trace());
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu) {
         switch (apdu.getType()) {
            case 2:
            case 5:
            case 6:
            case 7:
               break;
            case 3:
               ComplexAckPdu complexAck = (ComplexAckPdu)apdu;
               if (complexAck.isSegmentedMessage()) {
                  ClientStateMachine.sendAbort(apdu, 2);
               }
               break;
            case 4:
               if (apdu.isServerPDU()) {
                  ClientStateMachine.sendAbort(apdu, 2);
               }
               break;
            default:
               throw new IllegalStateException("Idle.processNetworkIndication():" + apdu.trace());
         }
      }
   }

   private static class SegmentedConf {
      public static final void process(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetworkIndication(apdu, transaction);
               break;
            case 2:
            default:
               throw new IllegalStateException("SegmentedConf.process():" + apdu.trace());
            case 3:
               processSegmentTimeout(apdu, transaction);
         }
      }

      private static void processApplication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 7:
               transaction.stopSegmentTimer();
               ClientStateMachine.network().sendRequest(transaction.getServerAddress(), apdu);
               transaction.setState(0);
               return;
            default:
               throw new IllegalStateException("SegmentedConf.processApplication():" + apdu.trace());
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 2:
            case 4:
            case 5:
            case 6:
               transaction.stopSegmentTimer();
               transaction.timeout();
               ClientStateMachine.sendAbort(apdu, 2);
               transaction.setState(0);
               break;
            case 3:
               ComplexAckPdu complexAck = (ComplexAckPdu)apdu;
               if (!complexAck.isSegmentedMessage()) {
                  transaction.stopSegmentTimer();
                  transaction.timeout();
                  ClientStateMachine.sendAbort(apdu, 2);
                  transaction.setState(0);
                  return;
               }

               int lastSeqNum = transaction.getLastSequenceNumber();
               int thisSeqNum = complexAck.getSequenceNumber();
               int initSeqNum = transaction.getInitialSequenceNumber();
               int actWinSize = transaction.getActualWindowSize();
               if (thisSeqNum != TransportStateMachine.modulo(lastSeqNum + 1, 256)) {
                  if (!TransportStateMachine.duplicateInWindow(thisSeqNum, TransportStateMachine.modulo(initSeqNum + 1, 256), lastSeqNum, actWinSize)) {
                     ClientStateMachine.sendSegmentAck(transaction, true, lastSeqNum);
                     transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                     transaction.setInitialSequenceNumber(lastSeqNum);
                     transaction.setDuplicateCount(0);
                     return;
                  }

                  transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  if (transaction.getDuplicateCount() < actWinSize) {
                     transaction.incrementDuplicateCount();
                  } else {
                     ClientStateMachine.sendSegmentNak(transaction, lastSeqNum);
                     transaction.setDuplicateCount(0);
                  }
               }

               if (complexAck.getMoreFollows()) {
                  if (thisSeqNum != TransportStateMachine.modulo(initSeqNum + actWinSize, 256)) {
                     transaction.getResponse().appendSegment(complexAck);
                     transaction.setLastSequenceNumber(TransportStateMachine.modulo(lastSeqNum + 1, 256));
                     transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                     return;
                  }

                  transaction.getResponse().appendSegment(complexAck);
                  lastSeqNum = TransportStateMachine.modulo(lastSeqNum + 1, 256);
                  transaction.setLastSequenceNumber(lastSeqNum);
                  transaction.setInitialSequenceNumber(lastSeqNum);
                  transaction.setDuplicateCount(0);
                  ClientStateMachine.sendSegmentAck(transaction, false, thisSeqNum);
                  transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  return;
               }

               transaction.getResponse().appendSegment(complexAck);
               transaction.stopSegmentTimer();
               ClientStateMachine.sendSegmentAck(transaction, false, thisSeqNum);
               transaction.postConfirmation(transaction.getResponse());
               transaction.setState(0);
               break;
            case 7:
               transaction.stopSegmentTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            default:
               throw new IllegalStateException("SegmentedConf.processNetworkIndication():" + apdu.trace());
         }
      }

      private static void processSegmentTimeout(ApplicationPdu apdu, ClientTransaction transaction) {
         transaction.stopSegmentTimer();
         transaction.timeout();
         transaction.setState(0);
      }
   }

   private static class SegmentedRequest {
      public static final void process(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetworkIndication(apdu, transaction);
               break;
            case 2:
            default:
               throw new IllegalStateException("SegmentedRequest.process():" + apdu.trace());
            case 3:
               processSegmentTimeout(apdu, transaction);
         }
      }

      private static void processApplication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 7:
               transaction.stopSegmentTimer();
               ClientStateMachine.network().sendRequest(transaction.getServerAddress(), apdu);
               transaction.setState(0);
               return;
            default:
               throw new IllegalStateException("SegmentedRequest.processApplication():" + apdu.trace());
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu, ClientTransaction transaction) {
         switch (apdu.getType()) {
            case 2:
               if (transaction.getSentAllSegments()) {
                  transaction.stopRequestTimer();
                  transaction.stopSegmentTimer();
                  transaction.postConfirmation(apdu);
                  transaction.setState(0);
               } else {
                  transaction.stopSegmentTimer();
                  transaction.timeout();
                  ClientStateMachine.sendAbort(apdu, 2);
                  transaction.setState(0);
               }
               break;
            case 3:
               ComplexAckPdu complexAck = (ComplexAckPdu)apdu;
               if (!transaction.getSentAllSegments()) {
                  transaction.stopSegmentTimer();
                  transaction.timeout();
                  ClientStateMachine.sendAbort(apdu, 2);
                  transaction.setState(0);
                  return;
               }

               if (!complexAck.isSegmentedMessage()) {
                  transaction.stopRequestTimer();
                  transaction.stopSegmentTimer();
                  transaction.postConfirmation(apdu);
                  transaction.setState(0);
                  return;
               }

               if (complexAck.getSequenceNumber() != 0) {
                  transaction.stopSegmentTimer();
                  transaction.timeout();
                  ClientStateMachine.sendAbort(apdu, 2);
                  transaction.setState(0);
                  return;
               }

               transaction.stopSegmentTimer();
               ClientStateMachine.receiveSegmentedComplexAck(transaction, complexAck);
               break;
            case 4:
               SegmentAckPdu segAck = (SegmentAckPdu)apdu;
               if (!TransportStateMachine.inWindow(segAck.getSequenceNumber(), transaction.getInitialSequenceNumber(), segAck.getActualWindowSize())) {
                  transaction.restartSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  return;
               }

               if (!transaction.getSentAllSegments()) {
                  int segCtr = transaction.getSegmentCounter();
                  transaction.setSegmentCounter(segCtr + transaction.getActualWindowSize());
                  transaction.setInitialSequenceNumber(TransportStateMachine.modulo(segAck.getSequenceNumber() + 1, 256));
                  transaction.setActualWindowSize(segAck.getActualWindowSize());
                  transaction.setSegmentRetryCount(0);
                  ClientStateMachine.fillWindow(transaction);
                  transaction.restartSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  return;
               }

               transaction.stopSegmentTimer();
               transaction.startRequestTimer(BBacnetNetwork.localDevice().getApduTimeout());
               transaction.setState(2);
               break;
            case 5:
            case 6:
               if (transaction.getSentAllSegments()) {
                  transaction.stopRequestTimer();
                  transaction.stopSegmentTimer();
                  transaction.postConfirmation(apdu);
                  transaction.setState(0);
               } else {
                  transaction.stopSegmentTimer();
                  transaction.timeout();
                  ClientStateMachine.sendAbort(apdu, 2);
                  transaction.setState(0);
               }
               break;
            case 7:
               transaction.stopSegmentTimer();
               transaction.postConfirmation(apdu);
               transaction.setState(0);
               break;
            default:
               throw new IllegalStateException("SegmentedRequest.processNetworkIndication():" + apdu.trace());
         }
      }

      private static void processSegmentTimeout(ApplicationPdu apdu, ClientTransaction transaction) {
         int numRetries = transaction.getSegmentRetryCount();
         if (numRetries < BBacnetNetwork.localDevice().getNumberOfApduRetries()) {
            transaction.setSegmentRetryCount(++numRetries);
            ClientStateMachine.fillWindow(transaction);
            transaction.restartSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
         } else {
            transaction.stopSegmentTimer();
            transaction.timeout();
            transaction.setState(0);
         }
      }
   }
}
