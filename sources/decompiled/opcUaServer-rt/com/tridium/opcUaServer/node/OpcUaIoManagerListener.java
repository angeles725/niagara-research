package com.tridium.opcUaServer.node;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaValueNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.io.IoManagerListener;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.AccessLevelType;
import com.prosysopc.ua.stack.core.AttributeWriteMask;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.prosysopc.ua.stack.core.AccessLevelType.Options;
import com.prosysopc.ua.stack.utils.NumericRange;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.point.BOpcUaServerProxyExt;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.status.BStatus;

public class OpcUaIoManagerListener implements IoManagerListener {
   private static Logger logger = Logger.getLogger("opcUaServer.ioListener");
   private HashMap<String, BOpcUaServerProxyExt> importPoints;
   private BOpcUaNamespace opcUaNamespace;

   public AccessLevelType onGetUserAccessLevel(ServiceContext serviceContext, NodeId nodeId, UaVariable node) {
      return node.getHistorizing()
         ? AccessLevelType.of(new Options[]{Options.CurrentRead, Options.CurrentWrite, Options.HistoryRead})
         : AccessLevelType.of(new Options[]{Options.CurrentRead, Options.CurrentWrite});
   }

   public Boolean onGetUserExecutable(ServiceContext serviceContext, NodeId nodeId, UaMethod node) {
      return true;
   }

   public AttributeWriteMask onGetUserWriteMask(ServiceContext serviceContext, NodeId nodeId, UaNode node) {
      return AttributeWriteMask.of(com.prosysopc.ua.stack.core.AttributeWriteMask.Options.values());
   }

   public boolean onReadNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node, UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
      return false;
   }

   public boolean onReadValue(
      ServiceContext serviceContext,
      NodeId nodeId,
      UaValueNode node,
      NumericRange indexRange,
      TimestampsToReturn timestampsToReturn,
      DateTime minTimestamp,
      DataValue dataValue
   ) throws StatusException {
      this.beGoodOrThrowBeforeReadOperation(nodeId);
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest("onReadValue: nodeId=" + nodeId + (node != null ? " node=" + node.getBrowseName() : ""));
      }

      return false;
   }

   public boolean onWriteNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node, UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
      return false;
   }

   public boolean onWriteValue(ServiceContext serviceContext, NodeId nodeId, UaValueNode node, NumericRange indexRange, DataValue dataValue) throws StatusException {
      this.beGoodOrThrowBeforeWriteOperation(nodeId);
      if (logger.isLoggable(Level.FINEST)) {
         logger.finest(
            "onWriteValue: nodeId="
               + nodeId
               + (node != null ? " node=" + node.getBrowseName() : "")
               + (indexRange != null ? " indexRange=" + indexRange : "")
               + " value="
               + dataValue
         );
      }

      if (this.importPoints != null) {
         BOpcUaServerProxyExt proxyExt = this.importPoints.get(nodeId.toString());
         if (proxyExt != null) {
            dataValue.setServerTimestamp(DateTime.currentTime());
            proxyExt.processValueUpdate(dataValue, "ioPush:");
         }
      }

      return false;
   }

   public void addMonitorPoint(BOpcUaServerProxyExt proxyExt) {
      if (this.importPoints == null) {
         this.importPoints = new HashMap<>();
      }

      this.importPoints.put(proxyExt.getUaNodeId(), proxyExt);
   }

   public void removeMonitorPoint(BOpcUaServerProxyExt proxyExt) {
      if (this.importPoints == null) {
         this.importPoints = new HashMap<>();
      }

      this.importPoints.remove(proxyExt.getUaNodeId());
   }

   private void beGoodOrThrowBeforeReadOperation(NodeId nodeId) throws StatusException {
      BStatus status = this.opcUaNamespace.getControlPointStatus(nodeId);
      if (status == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Failed to determine control point status. Node Id: " + nodeId);
         }

         throw new StatusException("Unable to get status for node " + nodeId, StatusCodes.Bad_OutOfService);
      } else if (status.isFault() || status.isDown() || status.isDisabled() || status.isNull()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Control Point status: " + status + ". Node Id: " + nodeId);
         }

         throw new StatusException(status.toString(), StatusCodes.Bad);
      }
   }

   private void beGoodOrThrowBeforeWriteOperation(NodeId nodeId) throws StatusException {
      BStatus status = this.opcUaNamespace.getControlPointStatus(nodeId);
      if (status != null && (status.isDown() || status.isDisabled())) {
         throw new StatusException(status.toString(), StatusCodes.Bad_OutOfService);
      }
   }

   public void setOpcUaNamespace(BOpcUaNamespace opcUaNamespace) {
      this.opcUaNamespace = opcUaNamespace;
   }

   public BOpcUaNamespace getOpcUaNamespace() {
      return this.opcUaNamespace;
   }
}
