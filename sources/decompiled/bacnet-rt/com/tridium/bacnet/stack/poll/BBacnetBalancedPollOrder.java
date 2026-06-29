package com.tridium.bacnet.stack.poll;

import com.tridium.bacnet.stack.BBacnetPollOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.baja.bacnet.util.PollList;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetBalancedPollOrder extends BBacnetPollOrder {
   public static final Type TYPE = Sys.loadType(BBacnetBalancedPollOrder.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void sort(List<PollList> entries) {
      Map<Integer, Queue<PollList>> networkQueues = this.queueByNetwork(entries);
      entries.clear();
      this.rrAdd(entries, networkQueues);
   }

   private void rrAdd(Collection<PollList> entries, Map<Integer, Queue<PollList>> networkQueues) {
      Collection<Queue<PollList>> pollLists = networkQueues.values();

      while (!this.isEmpty(networkQueues)) {
         for (Queue<PollList> pl : pollLists) {
            if (pl != null && !pl.isEmpty()) {
               entries.add(pl.remove());
            }
         }
      }
   }

   private Map<Integer, Queue<PollList>> queueByNetwork(Collection<PollList> entries) {
      Map<Integer, Queue<PollList>> map = new HashMap<>();

      for (PollList pl : entries) {
         int networkNumber = pl.getDevice().getAddress().getNetworkNumber();
         Queue<PollList> queue = map.get(networkNumber);
         if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            map.put(networkNumber, queue);
         }

         queue.add(pl);
      }

      return map;
   }

   private boolean isEmpty(Map<Integer, Queue<PollList>> networkQueues) {
      for (Queue<PollList> ple : networkQueues.values()) {
         if (!ple.isEmpty()) {
            return false;
         }
      }

      return true;
   }
}
