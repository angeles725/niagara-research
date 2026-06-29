package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.io.RejectException;
import javax.baja.util.QueueFullException;

public class ServerStateMachine extends TransportStateMachine {
   private static final int STACK_PROCESSING_TIME = 25;
   private static final Logger logger = Logger.getLogger("bacnet.transport.server");

   @Override
   public String getName() {
      return "BacnetServerTSM";
   }

   @Override
   public void route(ApplicationPdu apdu) {
      process(apdu);
   }

   public static final void process(ApplicationPdu apdu) {
      ServerTransaction transaction = ServerTransaction.find(apdu);
      if (transaction == null) {
         ServerStateMachine.Idle.process(apdu);
      } else {
         switch (transaction.getState()) {
            case 0:
               logger.warning("ServerStateMachine.process: Transaction is Idle; transaction: " + transaction + ", apdu: " + apdu);
               break;
            case 1:
               ServerStateMachine.SegmentedRequest.process(apdu, transaction);
               break;
            case 2:
               ServerStateMachine.AwaitResponse.process(apdu, transaction);
               break;
            case 3:
               ServerStateMachine.SegmentedResponse.process(apdu, transaction);
               break;
            default:
               logger.warning("ServerStateMachine.process: Invalid state for transaction: " + transaction + "; apdu: " + apdu);
         }
      }
   }

   private static boolean shouldRespond(ApplicationPdu apdu) {
      return !apdu.isBroadcast() || !(apdu instanceof ConfirmedRequestPdu);
   }

   private static void sendAbort(ApplicationPdu apdu, int abortReason) {
      AbortPdu abort = new AbortPdu(apdu.getClientAddress(), apdu.getPriority(), apdu.getInvokeId(), abortReason, true);
      network().sendRequest(apdu.getClientAddress(), abort);
   }

   private static void sendReject(ApplicationPdu apdu, int rejectReason) {
      RejectPdu reject = new RejectPdu(apdu.getClientAddress(), apdu.getPriority(), apdu.getInvokeId(), rejectReason);
      network().sendRequest(apdu.getClientAddress(), reject);
   }

   private static void sendSegmentNak(ServerTransaction transaction, int sequenceNumber) {
      sendSegmentAck(transaction, true, sequenceNumber);
   }

   private static void sendSegmentAck(ServerTransaction transaction, boolean negativeAck, int sequenceNumber) {
      SegmentAckPdu segAck = new SegmentAckPdu(negativeAck, true, transaction.getInvokeId(), sequenceNumber, transaction.getActualWindowSize());
      network().sendRequest(transaction.getClientAddress(), segAck);
   }

   private static void fillWindow(ServerTransaction transaction) {
      int ix = 0;
      int lastSegment = transaction.getNumSegments() - 1;
      int sequenceNumber = transaction.getInitialSequenceNumber();
      int segSize = transaction.getSegmentSize();
      int segmentCounter = transaction.getSegmentCounter();
      ComplexAckPdu response = transaction.getResponse();

      do {
         int seqNum = sequenceNumber + ix;
         int segCtr = segmentCounter + ix;
         if (segCtr >= lastSegment) {
            ComplexAckPdu seg = new ComplexAckPdu(response, modulo(seqNum, 256), segSize, segCtr);
            seg.setSegmentedMessage(true);
            seg.setMoreFollows(false);
            seg.setProposedWindowSize(transaction.getProposedWindowSize());
            network().sendRequest(transaction.getClientAddress(), seg);
            transaction.setSentAllSegments(true);
            return;
         }

         ComplexAckPdu seg = new ComplexAckPdu(response, modulo(seqNum, 256), segSize, segCtr);
         seg.setSegmentedMessage(true);
         seg.setMoreFollows(true);
         seg.setProposedWindowSize(transaction.getProposedWindowSize());
         network().sendRequest(transaction.getClientAddress(), seg);
      } while (++ix < transaction.getActualWindowSize());
   }

   private static boolean commEnabled(ApplicationPdu apdu) {
      boolean commEnabled = stack.isCommExecutionEnabled();
      if (!commEnabled && apdu != null) {
         int apduType = apdu.getType();
         if (apduType == 0) {
            ConfirmedRequestPdu request = (ConfirmedRequestPdu)apdu;
            commEnabled = request.getServiceChoice() == 17 || request.getServiceChoice() == 20;
         }
      }

      return commEnabled;
   }

   private static BBacnetNetworkLayer network() {
      return stack.getNetwork();
   }

   private static BBacnetServerLayer server() {
      return stack.getServer();
   }

   private static class AwaitResponse {
      private static void process(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetwork(apdu, transaction);
               break;
            case 2:
               processRequestTimeout(apdu, transaction);
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("AwaitResponse.process: Invalid apdu source: " + apdu.getSource() + "; apdu: " + apdu);
               }
         }
      }

      private static void processApplication(ApplicationPdu apdu, ServerTransaction transaction) {
         if (!transaction.isClientAbort()) {
            switch (apdu.getType()) {
               case 2:
                  transaction.stopRequestTimer();
                  ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
                  transaction.setState(0);
                  break;
               case 3:
                  ComplexAckPdu ack = (ComplexAckPdu)apdu;
                  transaction.stopRequestTimer();
                  transaction.setComplexAck(ack);
                  ConfirmedRequestPdu request = transaction.getRequest();
                  int maxRequestAPDULen = request.getMaxAPDULengthAccepted();
                  int maxDeviceAPDULen = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
                  int maxSegmentLength = -1;
                  if (maxRequestAPDULen <= 0) {
                     ServerStateMachine.logger
                        .warning("AwaitResponse.processApplication: Invalid `Max request APDU length` in request. Defaulting to `Max Device APDU`.");
                     maxSegmentLength = maxDeviceAPDULen;
                  } else {
                     maxSegmentLength = Math.min(maxRequestAPDULen, maxDeviceAPDULen);
                  }

                  if (ack.getLength() + 3 <= maxSegmentLength) {
                     ack.setSegmentedMessage(false);
                     ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
                     transaction.setState(0);
                     return;
                  }

                  if (!BBacnetNetwork.localDevice().getSegmentationSupported().isSegmentedTransmit()) {
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("AwaitResponse.processApplication: device not configured to transmit segmented message; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(request, 4);
                     transaction.setState(0);
                     return;
                  }

                  if (!request.getSegmentedResponseAccepted()) {
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("AwaitResponse.processApplication: client cannot accept segmented response; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(request, 4);
                     transaction.setState(0);
                     return;
                  }

                  int numSegs = ack.getLength() / maxSegmentLength + 1;
                  int maxSegs = request.getMaxSegmentsAccepted();
                  if (!ConfirmedRequestPdu.canFit(maxSegs, numSegs)) {
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("AwaitResponse.processApplication: client cannot accept necessary number of segments; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(request, 1);
                     transaction.setState(0);
                     return;
                  }

                  ComplexAckPdu seg = new ComplexAckPdu(ack, 0, maxSegmentLength, 0);
                  transaction.setSentAllSegments(false);
                  transaction.setSegmentRetryCount(0);
                  transaction.setDuplicateCount(0);
                  transaction.setInitialSequenceNumber(0);
                  transaction.setSegmentCounter(0);
                  transaction.setProposedWindowSize(TransportStateMachine.DEFAULT_SEGMENTATION_WINDOW_SIZE);
                  transaction.setActualWindowSize(1);
                  transaction.setSegmentSize(maxSegmentLength);
                  transaction.setNumSegments(ack.getLength() / (maxSegmentLength - 5) + 1);
                  seg.setSegmentedMessage(true);
                  seg.setMoreFollows(true);
                  seg.setProposedWindowSize(transaction.getProposedWindowSize());
                  ServerStateMachine.network().sendRequest(apdu.getClientAddress(), seg);
                  transaction.startSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  transaction.setState(3);
                  break;
               case 4:
               default:
                  if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                     ServerStateMachine.logger.info("AwaitResponse.processApplication: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
                  }
                  break;
               case 5:
                  transaction.stopRequestTimer();
                  ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
                  transaction.setState(0);
                  break;
               case 6:
                  transaction.stopRequestTimer();
                  ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
                  transaction.setState(0);
                  break;
               case 7:
                  transaction.stopRequestTimer();
                  ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
                  transaction.setState(0);
            }
         }
      }

      private static void processNetwork(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getType()) {
            case 0:
               ConfirmedRequestPdu request = (ConfirmedRequestPdu)apdu;
               if (!request.isSegmentedMessage()) {
                  if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                     ServerStateMachine.logger.fine("AwaitResponse.processNetwork: ConfirmedRequest is ignored as duplicate; apdu: " + apdu);
                  }

                  return;
               }

               ServerStateMachine.sendSegmentAck(transaction, false, transaction.getLastSequenceNumber());
               break;
            case 4:
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("AwaitResponse.processNetwork: Received unexpected SegmentAck; apdu: " + apdu);
               }

               ServerStateMachine.sendAbort(apdu, 2);
               transaction.setState(0);
               break;
            case 7:
               transaction.stopRequestTimer();
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("AwaitResponse.processNetwork: Aborted by client; transaction: " + transaction + ", apdu: " + apdu);
               }

               transaction.clientAbort();
               transaction.setState(0);
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("AwaitResponse.processNetwork: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
               }
         }
      }

      private static void processRequestTimeout(ApplicationPdu apdu, ServerTransaction transaction) {
         transaction.clientAbort();
         if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
            ServerStateMachine.logger.fine("AwaitResponse.processRequestTimeout: Aborted by client; apdu: " + apdu);
         }

         ServerStateMachine.sendAbort(apdu, 0);
         transaction.setState(0);
      }
   }

   private static class Idle {
      private static void process(ApplicationPdu apdu) {
         switch (apdu.getSource()) {
            case 0:
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("ServerStateMachine.Idle.process: Invalid apdu source: APPLICATION; apdu: " + apdu);
               }
               break;
            case 1:
               processNetworkIndication(apdu);
               break;
            case 2:
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("ServerStateMachine.Idle.process: Invalid apdu source: REQUEST_TIMEOUT; apdu: " + apdu);
               }
               break;
            case 3:
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("ServerStateMachine.Idle.process: Invalid apdu source: SEGMENT_TIMEOUT; apdu: " + apdu);
               }
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("ServerStateMachine.Idle.process: Invalid apdu source: " + apdu.getSource() + "; apdu: " + apdu);
               }
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu) {
         if (ServerStateMachine.commEnabled(apdu)) {
            switch (apdu.getType()) {
               case 0:
                  ConfirmedRequestPdu request = (ConfirmedRequestPdu)apdu;
                  ServerTransaction transaction = new ServerTransaction(request);
                  if (!request.isSegmentedMessage()) {
                     try {
                        BacnetServicePrimitive requestPrimitivex = BacnetConfirmedRequest.parseAPDU(request);
                        if (!ServerStateMachine.shouldRespond(apdu)) {
                           if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                              ServerStateMachine.logger
                                 .fine(
                                    "ServerStateMachine.Idle.processNetworkIndication: skipping broadcast APDU that should not receive response; apdu: " + apdu
                                 );
                           }

                           return;
                        }

                        ServerStateMachine.server()
                           .receiveConfirmedRequest(apdu.getClientAddress(), apdu.getInvokeId(), (BacnetConfirmedRequest)requestPrimitivex, apdu.getPriority());
                        transaction.startRequestTimer(BBacnetNetwork.localDevice().getApduTimeout() - 25);
                        transaction.setState(2);
                     } catch (QueueFullException var5) {
                        ServerStateMachine.logger
                           .log(
                              Level.WARNING,
                              "ServerStateMachine.Idle.processNetworkIndication: QueueFullException in receiveConfirmedRequest; apdu: "
                                 + apdu
                                 + ", exception: "
                                 + var5,
                              (Throwable)(ServerStateMachine.logger.isLoggable(Level.FINE) ? var5 : null)
                           );
                        ServerStateMachine.sendAbort(apdu, 9);
                     } catch (RejectException var6) {
                        ServerStateMachine.sendReject(apdu, var6.getRejectReason());
                     }

                     return;
                  }

                  if (request.getSequenceNumber() != 0) {
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("Idle.processNetworkIndication: Received unexpected confirmedRequest; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(apdu, 2);
                     transaction.setState(0);
                     return;
                  }

                  if (!BBacnetNetwork.localDevice().getSegmentationSupported().isSegmentedReceive()) {
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("Idle.processNetworkIndication: Device not configured to receive segmented messages; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(apdu, 4);
                     transaction.setState(0);
                     return;
                  }

                  transaction.setActualWindowSize(request.getProposedWindowSize());
                  transaction.setLastSequenceNumber(0);
                  transaction.setInitialSequenceNumber(0);
                  transaction.setDuplicateCount(0);
                  ServerStateMachine.sendSegmentAck(transaction, false, 0);
                  transaction.startSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  transaction.setState(1);
                  break;
               case 1:
                  BacnetServicePrimitive requestPrimitive = BacnetUnconfirmedRequest.parseAPDU((UnconfirmedRequestPdu)apdu);
                  if (requestPrimitive != null) {
                     ServerStateMachine.server()
                        .receiveUnconfirmedRequest(apdu.getClientAddress(), (BacnetUnconfirmedRequest)requestPrimitive, apdu.getPriority());
                  }
                  break;
               case 2:
               case 3:
               case 5:
               case 6:
               default:
                  if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                     ServerStateMachine.logger.info("Idle.processNetworkIndication: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
                  }
                  break;
               case 4:
                  if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                     ServerStateMachine.logger.fine("Idle.processNetworkIndication; Received unexpected segmentAck; apdu: " + apdu);
                  }

                  ServerStateMachine.sendAbort(apdu, 2);
               case 7:
            }
         }
      }
   }

   private static class SegmentedRequest {
      public static final void process(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetworkIndication(apdu, transaction);
               break;
            case 2:
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("SegmentedRequest.process: Invalid apdu source: " + apdu.getSource() + "; apdu: " + apdu);
               }
               break;
            case 3:
               processSegmentTimeout(apdu, transaction);
         }
      }

      private static void processApplication(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getType()) {
            case 7:
               transaction.stopSegmentTimer();
               ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
               transaction.setState(0);
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("SegmentedRequest.processApplication: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
               }
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu, ServerTransaction transaction) {
         if (ServerStateMachine.commEnabled(apdu)) {
            switch (apdu.getType()) {
               case 0:
                  ConfirmedRequestPdu request = (ConfirmedRequestPdu)apdu;
                  if (!request.isSegmentedMessage()) {
                     transaction.stopSegmentTimer();
                     if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                        ServerStateMachine.logger.fine("SegmentedRequest.processNetworkIndication: Received unexpected confirmedRequest; apdu: " + apdu);
                     }

                     ServerStateMachine.sendAbort(apdu, 2);
                     transaction.setState(0);
                     return;
                  }

                  int lastSeqNum = transaction.getLastSequenceNumber();
                  int thisSeqNum = request.getSequenceNumber();
                  int initSeqNum = transaction.getInitialSequenceNumber();
                  int actWinSize = transaction.getActualWindowSize();
                  if (thisSeqNum != TransportStateMachine.modulo(lastSeqNum + 1, 256)) {
                     if (!TransportStateMachine.duplicateInWindow(thisSeqNum, TransportStateMachine.modulo(initSeqNum + 1, 256), lastSeqNum, actWinSize)) {
                        ServerStateMachine.sendSegmentAck(transaction, true, lastSeqNum);
                        transaction.setInitialSequenceNumber(lastSeqNum);
                        transaction.setDuplicateCount(0);
                        transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                        return;
                     }

                     transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                     if (transaction.getDuplicateCount() < actWinSize) {
                        transaction.incrementDuplicateCount();
                     } else {
                        ServerStateMachine.sendSegmentNak(transaction, lastSeqNum);
                        transaction.setDuplicateCount(0);
                     }
                  }

                  if (request.getMoreFollows()) {
                     if (thisSeqNum != TransportStateMachine.modulo(initSeqNum + actWinSize, 256)) {
                        transaction.getRequest().appendSegment(request);
                        transaction.setLastSequenceNumber(TransportStateMachine.modulo(lastSeqNum + 1, 256));
                        transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                        return;
                     }

                     transaction.getRequest().appendSegment(request);
                     lastSeqNum = TransportStateMachine.modulo(lastSeqNum + 1, 256);
                     transaction.setLastSequenceNumber(lastSeqNum);
                     transaction.setInitialSequenceNumber(lastSeqNum);
                     transaction.setDuplicateCount(0);
                     ServerStateMachine.sendSegmentAck(transaction, false, lastSeqNum);
                     transaction.restartSegmentWaitTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                     return;
                  }

                  transaction.getRequest().appendSegment(request);
                  transaction.stopSegmentTimer();
                  lastSeqNum = TransportStateMachine.modulo(lastSeqNum + 1, 256);
                  transaction.setLastSequenceNumber(lastSeqNum);
                  transaction.setInitialSequenceNumber(lastSeqNum);
                  ServerStateMachine.sendSegmentAck(transaction, false, lastSeqNum);

                  try {
                     BacnetServicePrimitive requestPrimitive = BacnetConfirmedRequest.parseAPDU(transaction.getRequest());
                     ServerStateMachine.server()
                        .receiveConfirmedRequest(apdu.getClientAddress(), apdu.getInvokeId(), (BacnetConfirmedRequest)requestPrimitive, apdu.getPriority());
                     transaction.startRequestTimer(BBacnetNetwork.localDevice().getApduTimeout() - 25);
                     transaction.setState(2);
                  } catch (QueueFullException var9) {
                     ServerStateMachine.logger
                        .log(
                           Level.WARNING,
                           "ServerStateMachine.SegmentedRequest.processNetworkIndication: QueueFullException in receiveConfirmedRequest; apdu: "
                              + transaction.getRequest()
                              + ", exception: "
                              + var9,
                           (Throwable)(ServerStateMachine.logger.isLoggable(Level.FINE) ? var9 : null)
                        );
                     ServerStateMachine.sendAbort(apdu, 9);
                     transaction.setState(0);
                  } catch (RejectException var10) {
                     ServerStateMachine.sendReject(apdu, var10.getRejectReason());
                  }
                  break;
               case 4:
                  if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                     ServerStateMachine.logger.fine("SegmentedRequest.processNetworkIndication: Received unexpected segmentAck; apdu: " + apdu);
                  }

                  ServerStateMachine.sendAbort(apdu, 2);
                  break;
               case 7:
                  transaction.stopSegmentTimer();
                  transaction.setState(0);
                  break;
               default:
                  if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                     ServerStateMachine.logger.info("SegmentedRequest.processNetworkIndication: Invalid apdu type: " + apdu.getType() + "; apdu=" + apdu);
                  }
            }
         }
      }

      private static void processSegmentTimeout(ApplicationPdu apdu, ServerTransaction transaction) {
         transaction.stopSegmentTimer();
         transaction.setState(0);
      }
   }

   private static class SegmentedResponse {
      public static final void process(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getSource()) {
            case 0:
               processApplication(apdu, transaction);
               break;
            case 1:
               processNetworkIndication(apdu, transaction);
               break;
            case 2:
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("SegmentedResponse.process: Invalid apdu source: " + apdu.getSource() + "; apdu: " + apdu);
               }
               break;
            case 3:
               processSegmentTimeout(apdu, transaction);
         }
      }

      private static final void processApplication(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getType()) {
            case 7:
               transaction.stopSegmentTimer();
               ServerStateMachine.network().sendRequest(apdu.getClientAddress(), apdu);
               transaction.setState(0);
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("SegmentedResponse.processApplication: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
               }
         }
      }

      private static void processNetworkIndication(ApplicationPdu apdu, ServerTransaction transaction) {
         switch (apdu.getType()) {
            case 0:
               transaction.stopSegmentTimer();
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger.fine("SegmentedResponse.processNetworkIndication: Received unexpected confirmedRequest; apdu: " + apdu);
               }

               ServerStateMachine.sendAbort(apdu, 2);
               transaction.setState(0);
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
                  ServerStateMachine.fillWindow(transaction);
                  transaction.restartSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
                  return;
               }

               transaction.stopSegmentTimer();
               transaction.setState(0);
               break;
            case 7:
               transaction.stopSegmentTimer();
               if (ServerStateMachine.logger.isLoggable(Level.FINE)) {
                  ServerStateMachine.logger
                     .fine("SegmentedResponse.processNetworkIndication: aborted by client; transaction: " + transaction + ", apdu: " + apdu);
               }

               transaction.clientAbort();
               transaction.setState(0);
               break;
            default:
               if (ServerStateMachine.logger.isLoggable(Level.INFO)) {
                  ServerStateMachine.logger.info("SegmentedResponse.processNetworkIndication: Invalid apdu type: " + apdu.getType() + "; apdu: " + apdu);
               }
         }
      }

      private static void processSegmentTimeout(ApplicationPdu apdu, ServerTransaction transaction) {
         int numRetries = transaction.getSegmentRetryCount();
         if (numRetries < BBacnetNetwork.localDevice().getNumberOfApduRetries()) {
            transaction.setSegmentRetryCount(++numRetries);
            ServerStateMachine.fillWindow(transaction);
            transaction.restartSegmentTimer(BBacnetNetwork.localDevice().getApduSegmentTimeout());
         } else {
            transaction.stopSegmentTimer();
            transaction.setState(0);
         }
      }
   }
}
