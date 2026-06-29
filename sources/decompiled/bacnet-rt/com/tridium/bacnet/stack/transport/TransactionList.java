package com.tridium.bacnet.stack.transport;

import java.util.ArrayList;
import java.util.List;
import javax.baja.bacnet.datatypes.BBacnetAddress;

public class TransactionList {
   private List<Transaction> transactions = new ArrayList<>();

   public synchronized Transaction get(BBacnetAddress addr, int invokeId) {
      if (invokeId == -1) {
         return null;
      } else {
         int networkNumber = addr.getNetworkNumber();

         for (int i = 0; i < this.transactions.size(); i++) {
            Transaction t = this.transactions.get(i);
            if (invokeId == t.getInvokeId() && networkNumber == t.getAddress().getNetworkNumber() && addr.macEquals(t.getAddress().getMacAddress().getAddr())) {
               return t;
            }
         }

         return null;
      }
   }

   public synchronized void add(Transaction transaction) {
      this.transactions.add(transaction);
   }

   public synchronized Transaction remove(Transaction transaction) {
      this.transactions.remove(transaction);
      return transaction;
   }

   public synchronized void dump() {
      System.out.println("Transaction List Dump:");

      for (int i = 0; i < this.transactions.size(); i++) {
         Transaction t = this.transactions.get(i);
         System.out.println(t.toString());
      }
   }
}
