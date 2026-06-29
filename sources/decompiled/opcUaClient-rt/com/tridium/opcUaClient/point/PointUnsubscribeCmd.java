package com.tridium.opcUaClient.point;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.core.Attributes;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.util.OpcUaMonitoredDataItemUtil;
import java.util.logging.Level;
import javax.baja.util.ICoalesceable;
import javax.baja.util.Lexicon;

public class PointUnsubscribeCmd implements Runnable, ICoalesceable {
   private static final Lexicon LEX = Lexicon.make(PointUnsubscribeCmd.class);
   private BOpcUaDevice device;
   private BOpcUaClientPointDeviceExt deviceExt;
   private NodeId nodeId;
   private BOpcUaClientProxyExt proxyExt;

   private PointUnsubscribeCmd() {
   }

   public PointUnsubscribeCmd(BOpcUaDevice device, BOpcUaClientPointDeviceExt deviceExt, NodeId nodeId, BOpcUaClientProxyExt proxyExt) {
      this.device = device;
      this.deviceExt = deviceExt;
      this.nodeId = nodeId;
      this.proxyExt = proxyExt;
   }

   @Override
   public void run() {
      Subscription subscription = this.deviceExt.getSubscription();
      if (subscription != null) {
         try {
            unsubscribe(subscription, this.proxyExt, this.nodeId);
         } catch (Exception var3) {
            if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINER)) {
               BOpcUaClientProxyExt.rdlogger.log(Level.FINER, "Unsubscribe failed: ", (Throwable)var3);
            } else if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINE)) {
               BOpcUaClientProxyExt.rdlogger.log(Level.FINE, "Unsubscribe failed: " + var3.getLocalizedMessage());
            }
         }
      }
   }

   private static void unsubscribe(Subscription subscription, BOpcUaClientProxyExt proxyExt, NodeId nodeId) throws Exception {
      MonitoredDataItem item = (MonitoredDataItem)subscription.getItem(nodeId, Attributes.Value);
      if (item != null && item.getDataChangeListener() != null) {
         if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINEST)) {
            BOpcUaClientProxyExt.rdlogger.log(Level.FINEST, "Removing listener for: " + nodeId);
         }

         OpcUaMonitoredDataItemUtil.unsubscribe(subscription, item, proxyExt, nodeId, Attributes.Value);
      }

      proxyExt.setMonitoredItem(null);
   }

   @Override
   public int hashCode() {
      return this.proxyExt.hashCode();
   }

   @Override
   public boolean equals(Object object) {
      if (object instanceof PointUnsubscribeCmd) {
         PointUnsubscribeCmd o = (PointUnsubscribeCmd)object;
         return this.proxyExt.equals(o.proxyExt);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      StringBuilder cmdString = new StringBuilder();
      cmdString.append("PointUnsubscribeCmd (pointDeviceExt=").append(this.deviceExt);
      cmdString.append(", opcUaDevice=").append(this.device);
      cmdString.append(", proxyExt=").append(this.proxyExt);
      cmdString.append(", nodeId=").append(this.nodeId).append(')');
      return cmdString.toString();
   }

   public Object getCoalesceKey() {
      return this;
   }

   public ICoalesceable coalesce(ICoalesceable c) {
      return c;
   }
}
