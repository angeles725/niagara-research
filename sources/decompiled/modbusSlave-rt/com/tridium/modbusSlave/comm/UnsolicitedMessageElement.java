package com.tridium.modbusSlave.comm;

import com.tridium.modbusCore.server.util.TLinkedListElement;
import com.tridium.modbusCore.server.util.TLinkedListManager;
import java.util.Arrays;

public class UnsolicitedMessageElement implements TLinkedListElement {
   private byte[] message;
   private TLinkedListManager manager;
   private TLinkedListElement next;

   public UnsolicitedMessageElement(byte[] message) {
      this.message = message;
      this.manager = null;
   }

   public TLinkedListElement getNext() {
      return this.next;
   }

   public void setNext(TLinkedListElement next) {
      this.next = next;
   }

   public TLinkedListManager getLinkedListManager() {
      return this.manager;
   }

   public void setLinkedListManager(TLinkedListManager manager) {
      this.manager = manager;
   }

   public byte[] getMessage() {
      return this.message;
   }

   public void setMessage(byte[] message) {
      this.message = message;
   }

   @Override
   public String toString() {
      return Arrays.toString(this.message);
   }
}
