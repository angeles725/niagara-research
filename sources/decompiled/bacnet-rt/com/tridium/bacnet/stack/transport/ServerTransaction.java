package com.tridium.bacnet.stack.transport;

import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public class ServerTransaction extends Transaction {
   private ConfirmedRequestPdu apdu;
   private ComplexAckPdu complexAck;
   private boolean abortedByClient;
   static TransactionList activeTransactions = new TransactionList();
   private static final Logger logger = Logger.getLogger("bacnet.transport.server");

   public ServerTransaction(ConfirmedRequestPdu apdu) {
      this.apdu = apdu;
   }

   @Override
   public boolean isServerTransaction() {
      return true;
   }

   @Override
   protected ApplicationPdu getApdu() {
      return this.apdu;
   }

   @Override
   protected TransactionList activeTransactions() {
      return activeTransactions;
   }

   @Override
   public long getHashCode() {
      return hash(this.getClientAddress(), this.getInvokeId());
   }

   public static ServerTransaction find(ApplicationPdu apdu) {
      while (true) {
         ServerTransaction serverTransaction = (ServerTransaction)activeTransactions.get(apdu.getClientAddress(), apdu.getInvokeId());
         if (serverTransaction == null) {
            return null;
         }

         if (serverTransaction.getState() != 0) {
            return serverTransaction;
         }

         logger.warning(
            "Removing idle transaction found in the server active transactions list; clientAddress: "
               + apdu.getClientAddress()
               + ", invokeId: "
               + apdu.getInvokeId()
         );
         activeTransactions.remove(serverTransaction);
      }
   }

   @Override
   public BBacnetAddress getClientAddress() {
      return this.apdu.getClientAddress();
   }

   @Override
   public BBacnetAddress getServerAddress() {
      return this.apdu.getServerAddress();
   }

   @Override
   public BBacnetAddress getAddress() {
      return this.apdu.getClientAddress();
   }

   @Override
   public int getInvokeId() {
      return this.apdu.getInvokeId();
   }

   public void setComplexAck(ComplexAckPdu ack) {
      this.complexAck = ack;
   }

   public ComplexAckPdu getResponse() {
      return this.complexAck;
   }

   public ConfirmedRequestPdu getRequest() {
      return this.apdu;
   }

   public void clientAbort() {
      this.abortedByClient = true;
   }

   public boolean isClientAbort() {
      return this.abortedByClient;
   }

   @Override
   protected String getTag(int state) {
      switch (state) {
         case 0:
            return "IDLE";
         case 1:
            return "SEGMENTED_REQUEST";
         case 2:
            return "AWAIT_RESPONSE";
         case 3:
            return "SEGMENTED_RESP";
         default:
            throw new IllegalStateException();
      }
   }
}
