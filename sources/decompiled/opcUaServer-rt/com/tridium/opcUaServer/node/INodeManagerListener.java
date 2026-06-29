package com.tridium.opcUaServer.node;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaReferenceType;
import com.prosysopc.ua.server.MonitoredDataItem;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.Subscription;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.AggregateFilterResult;
import com.prosysopc.ua.stack.core.MonitoringFilter;
import com.prosysopc.ua.stack.core.MonitoringMode;
import com.prosysopc.ua.stack.core.MonitoringParameters;
import com.prosysopc.ua.stack.core.NodeAttributes;
import com.prosysopc.ua.stack.core.NodeClass;
import com.prosysopc.ua.stack.core.ViewDescription;
import com.prosysopc.ua.stack.utils.NumericRange;
import java.util.List;

public interface INodeManagerListener extends NodeManagerListener {
   default void onAfterAddNode(
      ServiceContext serviceContext,
      NodeId nodeId,
      UaNode uaNode,
      NodeId nodeId1,
      UaNode uaNode1,
      NodeClass nodeClass,
      QualifiedName qualifiedName,
      NodeAttributes nodeAttributes,
      UaReferenceType uaReferenceType,
      ExpandedNodeId expandedNodeId,
      UaNode uaNode2
   ) throws StatusException {
   }

   default void onAfterAddReference(
      ServiceContext serviceContext,
      NodeId nodeId,
      UaNode uaNode,
      ExpandedNodeId expandedNodeId,
      UaNode uaNode1,
      NodeId nodeId1,
      UaReferenceType uaReferenceType,
      boolean b
   ) throws StatusException {
   }

   default void onAfterCreateMonitoredDataItem(ServiceContext serviceContext, Subscription subscription, MonitoredDataItem item) {
   }

   default void onAfterDeleteMonitoredDataItem(ServiceContext serviceContext, Subscription subscription, MonitoredDataItem item) {
   }

   default void onAfterModifyMonitoredDataItem(ServiceContext serviceContext, Subscription subscription, MonitoredDataItem item) {
   }

   default void onCreateMonitoredDataItem(
      ServiceContext serviceContext,
      Subscription subscription,
      NodeId nodeId,
      UaNode uaNode,
      UnsignedInteger unsignedInteger,
      NumericRange numericRange,
      MonitoringParameters monitoringParameters,
      MonitoringFilter monitoringFilter,
      AggregateFilterResult aggregateFilterResult,
      MonitoringMode monitoringMode
   ) throws StatusException {
   }

   default void onCreateMonitoredDataItem(
      ServiceContext serviceContext,
      Subscription subscription,
      UaNode node,
      UnsignedInteger attributeId,
      NumericRange indexRange,
      MonitoringParameters params,
      MonitoringFilter filter,
      AggregateFilterResult filterResult
   ) throws StatusException {
   }

   default void onDeleteMonitoredDataItem(ServiceContext serviceContext, Subscription subscription, MonitoredDataItem monitoredItem) {
   }

   default void onGetReferences(ServiceContext serviceContext, ViewDescription viewDescription, NodeId nodeId, UaNode node, List<UaReference> references) {
   }

   default void onModifyMonitoredDataItem(
      ServiceContext serviceContext,
      Subscription subscription,
      MonitoredDataItem item,
      UaNode node,
      MonitoringParameters params,
      MonitoringFilter filter,
      AggregateFilterResult filterResult
   ) {
   }
}
