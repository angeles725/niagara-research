package com.tridium.opcUaServer.event;

import com.prosysopc.ua.EventData;
import com.prosysopc.ua.EventListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.ContentFilterDefinition;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.EventFilterResult;
import com.prosysopc.ua.stack.core.HistoryEventFieldList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventHistory {
   private final int capacity = 10000;
   private final List<EventData> events = new CopyOnWriteArrayList<>();
   private final EventListener listener = new EventListener() {
      public boolean isMonitored(UaNode event) {
         return false;
      }

      public void onEvent(UaNode node, EventData eventData) {
         EventHistory.this.events.add(eventData);

         while (EventHistory.this.events.size() > 10000) {
            EventHistory.this.events.remove(0);
         }
      }
   };
   private final UaObjectNode node;

   public EventHistory(UaObjectNode node) {
      this.node = node;
      node.addEventListener(this.listener);
   }

   public void deleteEvents(ByteString[] eventIds, StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics) {
      for (int i = this.events.size() - 1; i >= 0; i--) {
         EventData event = this.events.get(i);
         ByteString id1 = event.getEventId();

         for (ByteString eventId : eventIds) {
            if (eventId.equals(id1)) {
               this.events.remove(i);
               break;
            }
         }
      }
   }

   public Integer readEvents(DateTime startTime, DateTime endTime, int maxValues, EventFilter eventFilter, List<HistoryEventFieldList> history, int firstIndex) {
      int i = 0;
      boolean startTimeDefined = startTime.compareTo(DateTime.MIN_VALUE) > 0;
      boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
      List<List<QualifiedName>> fieldPaths = new ArrayList<>();
      ContentFilterDefinition filterDefinition = new ContentFilterDefinition();
      EventFilterResult eventFilterResult = new EventFilterResult();
      ContentFilterDefinition.parseEventFilter(
         this.node.getNodeManager().getNodeManagerTable().getNodeManagerRoot(), eventFilter, fieldPaths, filterDefinition, eventFilterResult
      );
      if (!startTimeDefined && endTimeDefined) {
         for (int j = this.events.size() - 1; j >= 0; j--) {
            EventData event = this.events.get(j);
            DateTime t = event.getTime();
            int compareToEnd = t.compareTo(endTime);
            if (compareToEnd <= 0) {
               if (i >= firstIndex) {
                  history.add(new HistoryEventFieldList(event.getFieldValues(fieldPaths)));
               }

               i++;
               if (history.size() == maxValues) {
                  return startTimeDefined && j > 0 ? i : null;
               }
            }
         }
      } else {
         for (int jx = 0; jx < this.events.size(); jx++) {
            EventData event = this.events.get(jx);
            DateTime t = event.getTime();
            int compareToEnd = endTimeDefined ? t.compareTo(endTime) : -1;
            if (compareToEnd > 0) {
               break;
            }

            int compareToStart = t.compareTo(startTime);
            if (compareToStart >= 0) {
               if (i >= firstIndex && filterDefinition.evaluate(event, true)) {
                  history.add(new HistoryEventFieldList(event.getFieldValues(fieldPaths)));
               }

               i++;
               if (history.size() == maxValues) {
                  return endTimeDefined && jx < this.events.size() ? i : null;
               }
            }
         }
      }

      return null;
   }
}
