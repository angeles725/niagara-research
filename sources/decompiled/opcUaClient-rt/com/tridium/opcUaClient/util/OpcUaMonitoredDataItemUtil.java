package com.tridium.opcUaClient.util;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.MonitoringMode;
import com.tridium.opcUaClient.point.BOpcUaClientProxyExt;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class OpcUaMonitoredDataItemUtil {
   private static final ConcurrentHashMap<Integer, OpcUaBulkDataItemListener> bulkDataItemListenerHashMap = new ConcurrentHashMap<>();

   private OpcUaMonitoredDataItemUtil() {
   }

   public static synchronized MonitoredDataItem getOrCreateAndGetMonitoredItem(Subscription subscription, NodeId nodeId, UnsignedInteger monitoredAttribute) throws Exception {
      MonitoredDataItem item = (MonitoredDataItem)subscription.getItem(nodeId, monitoredAttribute);
      boolean itemExists = item != null;
      if (!itemExists) {
         item = new MonitoredDataItem(nodeId, monitoredAttribute, MonitoringMode.Reporting);
      }

      if (item.getDataChangeListener() == null) {
         item.setDataChangeListener(getOrCreateAndGetBulkListener(item, nodeId, monitoredAttribute));
      }

      if (!itemExists) {
         try {
            OpcUaClientUtil.addItemToSubscription(subscription, item, true);
         } catch (Exception var6) {
            OpcUaClientUtil.removeItemFromSubscription(subscription, item, false);
            throw var6;
         }
      }

      return item;
   }

   public static synchronized OpcUaBulkDataItemListener getOrCreateAndGetBulkListener(
      MonitoredDataItem monitoredDataItem, NodeId nodeId, UnsignedInteger monitoredAttribute
   ) throws Exception {
      if (Objects.equals(nodeId, monitoredDataItem.getNodeId()) && Objects.equals(monitoredAttribute, monitoredDataItem.getAttributeId())) {
         int dataItemHashCode = monitoredDataItem.hashCode();
         if (bulkDataItemListenerHashMap.get(dataItemHashCode) != null) {
            return bulkDataItemListenerHashMap.get(dataItemHashCode);
         } else {
            OpcUaBulkDataItemListener bulkDataItemListener = new OpcUaBulkDataItemListener(nodeId, monitoredAttribute);
            bulkDataItemListenerHashMap.put(dataItemHashCode, bulkDataItemListener);
            return bulkDataItemListener;
         }
      } else {
         throw new IllegalArgumentException("Cannot create bulk listener. Item parameters do not match with the monitoring parameters.");
      }
   }

   public static void subscribe(MonitoredDataItem item, BOpcUaClientProxyExt proxyExt, NodeId nodeId, UnsignedInteger monitoredAttribute) {
      if (item.getDataChangeListener() instanceof OpcUaBulkDataItemListener) {
         ((OpcUaBulkDataItemListener)item.getDataChangeListener()).addListener(proxyExt, nodeId, monitoredAttribute);
      } else {
         item.setDataChangeListener(proxyExt);
      }
   }

   public static void unsubscribe(
      Subscription subscription, MonitoredDataItem item, BOpcUaClientProxyExt proxyExt, NodeId nodeId, UnsignedInteger monitoredAttribute
   ) throws Exception {
      if (item.getDataChangeListener() instanceof OpcUaBulkDataItemListener) {
         OpcUaBulkDataItemListener bulkDataItemListener = (OpcUaBulkDataItemListener)item.getDataChangeListener();
         bulkDataItemListener.removeListener(proxyExt, nodeId, monitoredAttribute);
         if (bulkDataItemListener.isEmpty()) {
            deleteItemAndListener(subscription, item);
         }
      } else {
         item.setDataChangeListener(null);
         OpcUaClientUtil.removeItemFromSubscription(subscription, item, false);
      }
   }

   private static synchronized void deleteItemAndListener(Subscription subscription, MonitoredDataItem item) throws Exception {
      item.setDataChangeListener(null);
      bulkDataItemListenerHashMap.remove(item.hashCode());
      OpcUaClientUtil.removeItemFromSubscription(subscription, item, false);
   }

   public static synchronized void clearBulkListeners() {
      bulkDataItemListenerHashMap.clear();
   }
}
