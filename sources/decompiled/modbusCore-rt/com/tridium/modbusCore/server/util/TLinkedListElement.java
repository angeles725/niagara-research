package com.tridium.modbusCore.server.util;

public interface TLinkedListElement {
   TLinkedListElement getNext();

   void setNext(TLinkedListElement var1);

   TLinkedListManager getLinkedListManager();

   void setLinkedListManager(TLinkedListManager var1);
}
