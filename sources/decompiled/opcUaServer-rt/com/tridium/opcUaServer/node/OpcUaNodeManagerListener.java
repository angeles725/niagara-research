package com.tridium.opcUaServer.node;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaReferenceType;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.core.NodeAttributes;
import com.prosysopc.ua.stack.core.NodeClass;
import com.prosysopc.ua.stack.core.StatusCodes;
import com.prosysopc.ua.stack.core.UserTokenType;
import com.prosysopc.ua.stack.core.ViewDescription;
import java.util.logging.Logger;

public class OpcUaNodeManagerListener implements INodeManagerListener {
   public static final Logger logger = Logger.getLogger("opcUaServer.nodeManager");

   public void onAddNode(
      ServiceContext serviceContext,
      NodeId parentNodeId,
      UaNode parent,
      NodeId nodeId,
      NodeClass nodeClass,
      QualifiedName browseName,
      NodeAttributes attributes,
      UaReferenceType referenceType,
      ExpandedNodeId typeDefinitionId,
      UaNode typeDefinition
   ) throws StatusException {
      this.checkUserAccess(serviceContext);
   }

   public void onAddReference(
      ServiceContext serviceContext,
      NodeId sourceNodeId,
      UaNode sourceNode,
      ExpandedNodeId targetNodeId,
      UaNode targetNode,
      NodeId referenceTypeId,
      UaReferenceType referenceType,
      boolean isForward
   ) throws StatusException {
      this.checkUserAccess(serviceContext);
   }

   public boolean onBrowseNode(ServiceContext serviceContext, ViewDescription view, NodeId nodeId, UaNode node, UaReference reference) {
      return true;
   }

   public void onDeleteNode(ServiceContext serviceContext, NodeId nodeId, UaNode node, boolean deleteTargetReferences) throws StatusException {
      this.checkUserAccess(serviceContext);
   }

   public void onDeleteReference(
      ServiceContext serviceContext,
      NodeId sourceNodeId,
      UaNode sourceNode,
      ExpandedNodeId targetNodeId,
      UaNode targetNode,
      NodeId referenceTypeId,
      UaReferenceType referenceType,
      boolean isForward,
      boolean deleteBidirectional
   ) throws StatusException {
      this.checkUserAccess(serviceContext);
   }

   private void checkUserAccess(ServiceContext serviceContext) throws StatusException {
      if (serviceContext.getSession().getUserIdentity().getType().equals(UserTokenType.Anonymous)) {
         throw new StatusException(StatusCodes.Bad_UserAccessDenied);
      }
   }
}
