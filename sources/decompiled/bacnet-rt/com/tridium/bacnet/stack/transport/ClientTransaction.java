package com.tridium.bacnet.stack.transport;

import javax.baja.bacnet.datatypes.BBacnetAddress;

public class ClientTransaction extends Transaction {
   private ConfirmedRequestPdu apdu;
   private ComplexAckPdu complexAck;
   static TransactionList activeTransactions = new TransactionList();

   public ClientTransaction(ConfirmedRequestPdu apdu) {
      this.apdu = apdu;
   }

   public ConfirmedRequestPdu getRequestPdu() {
      return this.apdu;
   }

   @Override
   protected ApplicationPdu getApdu() {
      return this.apdu;
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
      return this.apdu.getServerAddress();
   }

   @Override
   public int getInvokeId() {
      return this.apdu.getInvokeId();
   }

   @Override
   public boolean isServerTransaction() {
      return false;
   }

   public void setComplexAck(ComplexAckPdu ack) {
      this.complexAck = ack;
   }

   public ComplexAckPdu getResponse() {
      return this.complexAck;
   }

   @Override
   protected TransactionList activeTransactions() {
      return activeTransactions;
   }

   @Override
   public long getHashCode() {
      return hash(this.getServerAddress(), this.getInvokeId());
   }

   public void postConfirmation(ApplicationPdu confApdu) {
      this.apdu.postConfirmation(confApdu);
   }

   public void timeout() {
      this.apdu.timeout();
   }

   public static ClientTransaction find(ApplicationPdu apdu) {
      return (ClientTransaction)activeTransactions.get(apdu.getServerAddress(), apdu.getInvokeId());
   }

   @Override
   protected String getTag(int state) {
      switch (state) {
         case 0:
            return "IDLE";
         case 1:
            return "SEGMENTED_REQUEST";
         case 2:
            return "AWAIT_CONFIRMATION";
         case 3:
            return "SEGMENT_CONF";
         default:
            throw new IllegalStateException();
      }
   }
}
