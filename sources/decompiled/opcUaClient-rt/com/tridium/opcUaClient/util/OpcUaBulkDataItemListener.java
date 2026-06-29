package com.tridium.opcUaClient.util;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredDataItemListener;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpcUaBulkDataItemListener implements MonitoredDataItemListener {
   private static final Logger LOGGER = Logger.getLogger("opcUaClient.OpcUaBulkDataItemListener");
   private final List<MonitoredDataItemListener> listeners = Collections.synchronizedList(new ArrayList<>());
   private final NodeId nodeId;
   private final UnsignedInteger monitoredAttribute;
   private final OpcUaBulkDataItemListener.DataState lastDataState;

   public OpcUaBulkDataItemListener(NodeId nodeId, UnsignedInteger monitoredAttribute) {
      this.nodeId = nodeId;
      this.monitoredAttribute = monitoredAttribute;
      this.lastDataState = new OpcUaBulkDataItemListener.DataState(null, null, null);
   }

   public void addListener(MonitoredDataItemListener listener, NodeId nodeId, UnsignedInteger monitoredAttribute) {
      if (this.nodeId.equals(nodeId) && this.monitoredAttribute.equals(monitoredAttribute)) {
         synchronized (this.listeners) {
            if (this.listeners.contains(listener)) {
               LOGGER.log(Level.WARNING, "Failed to add listener to bulk listener. Listener already present.");
               return;
            }

            this.listeners.add(listener);
         }

         if (!this.lastDataState.isNull()) {
            listener.onDataChange(this.lastDataState.getMonitoredDataItem(), this.lastDataState.getOldValue(), this.lastDataState.getNewValue());
         }
      } else {
         LOGGER.log(Level.WARNING, "Failed to add listener to bulk listener. Monitoring parameters do not match.");
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Bulk     - NodeId: " + this.nodeId + ", monitoredAttribute: " + this.monitoredAttribute);
            LOGGER.log(Level.FINE, "Listener - NodeId: " + nodeId + ", monitoredAttribute: " + monitoredAttribute);
         }
      }
   }

   public void removeListener(MonitoredDataItemListener listener, NodeId nodeId, UnsignedInteger monitoredAttribute) {
      if (this.nodeId.equals(nodeId) && this.monitoredAttribute.equals(monitoredAttribute)) {
         synchronized (this.listeners) {
            if (!this.listeners.isEmpty()) {
               this.listeners.remove(listener);
            }
         }
      } else {
         LOGGER.log(Level.WARNING, "Failed to remove listener from bulk listener. Monitoring parameters do not match.");
         if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Bulk     - NodeId: " + this.nodeId + ", monitoredAttribute: " + this.monitoredAttribute);
            LOGGER.log(Level.FINE, "Listener - NodeId: " + nodeId + ", monitoredAttribute: " + monitoredAttribute);
         }
      }
   }

   public boolean isEmpty() {
      synchronized (this.listeners) {
         return this.listeners.isEmpty();
      }
   }

   public int getListenerCount() {
      synchronized (this.listeners) {
         return this.listeners.size();
      }
   }

   public void onDataChange(MonitoredDataItem monitoredDataItem, DataValue dataValue, DataValue dataValue1) {
      this.lastDataState.updateState(monitoredDataItem, dataValue, dataValue1);
      if (LOGGER.isLoggable(Level.FINE)) {
         LOGGER.log(Level.FINE, "Updating last data state for monitoredDataItem: \n" + monitoredDataItem.toString());
      }

      synchronized (this.listeners) {
         for (MonitoredDataItemListener listener : this.listeners) {
            listener.onDataChange(monitoredDataItem, dataValue, dataValue1);
         }
      }
   }

   private static class DataState {
      private MonitoredDataItem monitoredDataItem;
      private DataValue oldValue;
      private DataValue newValue;

      public DataState(MonitoredDataItem monitoredDataItem, DataValue oldValue, DataValue newValue) {
         this.monitoredDataItem = monitoredDataItem;
         this.oldValue = oldValue;
         this.newValue = newValue;
      }

      public MonitoredDataItem getMonitoredDataItem() {
         return this.monitoredDataItem;
      }

      public DataValue getOldValue() {
         return this.oldValue;
      }

      public DataValue getNewValue() {
         return this.newValue;
      }

      public void updateState(MonitoredDataItem monitoredDataItem, DataValue oldValue, DataValue newValue) {
         this.monitoredDataItem = monitoredDataItem;
         this.oldValue = oldValue;
         this.newValue = newValue;
      }

      public boolean isNull() {
         return this.monitoredDataItem == null || this.newValue == null;
      }
   }
}
