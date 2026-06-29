package com.tridium.opcUaClient.point;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.core.Attributes;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.util.OpcUaMonitoredDataItemUtil;
import com.tridium.opcUaCore.util.OpcUaCoreUtil;
import java.util.logging.Level;
import javax.baja.util.ICoalesceable;
import javax.baja.util.Lexicon;

public class PointSubscribeCmd implements Runnable, ICoalesceable {
   private static final Lexicon LEX = Lexicon.make(PointSubscribeCmd.class);
   private BOpcUaDevice device;
   private BOpcUaClientPointDeviceExt deviceExt;
   private NodeId nodeId;
   private BOpcUaClientProxyExt proxyExt;

   private PointSubscribeCmd() {
   }

   public PointSubscribeCmd(BOpcUaDevice device, BOpcUaClientPointDeviceExt deviceExt, NodeId nodeId, BOpcUaClientProxyExt proxyExt) {
      this.device = device;
      this.deviceExt = deviceExt;
      this.nodeId = nodeId;
      this.proxyExt = proxyExt;
   }

   private String updateUaNodeId(String oldUaNodeId, String nameSpaceUri) throws ServiceException, StatusException {
      return nameSpaceUri != null && !nameSpaceUri.isEmpty()
         ? oldUaNodeId.substring(0, oldUaNodeId.indexOf(61) + 1)
            + this.device.uaClient.getNamespaceTable(true).getIndex(nameSpaceUri)
            + oldUaNodeId.substring(oldUaNodeId.indexOf(59))
         : oldUaNodeId;
   }

   @Override
   public void run() {
      if (this.proxyExt.getNodeId() == null) {
         try {
            NodeId newNodeId = NodeId.parseNodeId(this.proxyExt.getUaNodeId());
            this.proxyExt.setNodeId(newNodeId);
            this.nodeId = newNodeId;
         } catch (Exception var7) {
            this.proxyExt.configFail(OpcUaCoreUtil.getLocalizedMessage(var7));
            return;
         }
      }

      String updatedNodeId;
      try {
         updatedNodeId = this.updateUaNodeId(this.proxyExt.getUaNodeId(), this.proxyExt.getNameSpaceUri());
      } catch (StatusException | ServiceException var6) {
         throw new RuntimeException(var6);
      }

      if (updatedNodeId != null && !updatedNodeId.equals(this.proxyExt.getUaNodeId())) {
         this.proxyExt.setUaNodeId(updatedNodeId);

         try {
            NodeId newNodeId = NodeId.parseNodeId(updatedNodeId);
            this.proxyExt.setNodeId(newNodeId);
            this.nodeId = newNodeId;
         } catch (Exception var5) {
            this.proxyExt.configFail(OpcUaCoreUtil.getLocalizedMessage(var5));
            return;
         }
      }

      Subscription subscription = this.deviceExt.getSubscription();
      if (subscription != null) {
         try {
            subscribe(subscription, this.proxyExt, this.nodeId);
         } catch (Exception var8) {
            if (var8 instanceof StatusException) {
               StatusCode statusCode = ((StatusException)var8).getStatusCode();
               this.proxyExt.setUaStatusCode(statusCode.toString());
            }

            this.proxyExt.readFail(LEX.getText("opcUaClient.point.subscribeFailed", new Object[]{var8.getLocalizedMessage()}));
            if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINER)) {
               BOpcUaClientProxyExt.rdlogger.log(Level.FINER, "Subscribe failed: ", (Throwable)var8);
            } else if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINE)) {
               BOpcUaClientProxyExt.rdlogger.log(Level.FINE, "Subscribe failed: " + var8.getLocalizedMessage());
            }
         }
      }
   }

   private static void subscribe(Subscription subscription, BOpcUaClientProxyExt proxyExt, NodeId nodeId) throws Exception {
      MonitoredDataItem item = OpcUaMonitoredDataItemUtil.getOrCreateAndGetMonitoredItem(subscription, nodeId, Attributes.Value);
      if (BOpcUaClientProxyExt.rdlogger.isLoggable(Level.FINEST)) {
         BOpcUaClientProxyExt.rdlogger.log(Level.FINEST, "Adding listener for: " + nodeId);
      }

      proxyExt.setMonitoredItem(item);
      OpcUaMonitoredDataItemUtil.subscribe(item, proxyExt, nodeId, Attributes.Value);
   }

   @Override
   public int hashCode() {
      return this.proxyExt.hashCode();
   }

   @Override
   public boolean equals(Object object) {
      if (object instanceof PointSubscribeCmd) {
         PointSubscribeCmd o = (PointSubscribeCmd)object;
         return this.proxyExt.equals(o.proxyExt);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      StringBuilder cmdString = new StringBuilder();
      cmdString.append("PointSubscribeCmd (pointDeviceExt=").append(this.deviceExt);
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
