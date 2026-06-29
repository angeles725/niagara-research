package com.tridium.bacnet.stack.network;

public interface PrioritizedQueueEntry {
   PrioritizedQueueEntry getNext();

   void setNext(PrioritizedQueueEntry var1);

   int getPriority();
}
