package com.tridium.opcUaServer.event;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.types.opcua.server.AcknowledgeableConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import com.tridium.alarm.BConsoleRecipient;
import com.tridium.opcUaCore.BAlarmSeverities;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.BOpcUaServer;
import com.tridium.opcUaServer.point.BOpcUaServerProxyExt;
import com.tridium.opcUaServer.util.OpcUaServerUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.driver.BDevice;
import javax.baja.naming.BOrdList;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "opcUaSeverity",
   type = "BAlarmSeverities",
   defaultValue = "BAlarmSeverities.DEFAULT"
)
public class BOpcUaAlarmRecipient extends BConsoleRecipient {
   public static final Property opcUaSeverity = newProperty(0, BAlarmSeverities.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BOpcUaAlarmRecipient.class);
   private static final Lexicon lex = Lexicon.make("opcUaServer");
   private BOpcUaServer opcUaServer = null;
   private Logger logger = Logger.getLogger("opcUaServer.event");

   public BAlarmSeverities getOpcUaSeverity() {
      return (BAlarmSeverities)this.get(opcUaSeverity);
   }

   public void setOpcUaSeverity(BAlarmSeverities v) {
      this.set(opcUaSeverity, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void handleAlarm(BAlarmRecord alarmRecord) {
      try {
         super.handleAlarm(alarmRecord);
         if (!alarmRecord.isAcknowledged()) {
            BOrdList list = alarmRecord.getSource();
            BObject obj = list.get(0).resolve().get();
            if (obj instanceof BAlarmSourceExt) {
               BAlarmSourceExt almExt = (BAlarmSourceExt)obj;
               BControlPoint parentPoint = almExt.getParentPoint();
               BAbstractProxyExt abstractProxyExt = parentPoint.getProxyExt();
               if (abstractProxyExt instanceof BOpcUaServerProxyExt) {
                  BOpcUaServerProxyExt proxyExt = (BOpcUaServerProxyExt)abstractProxyExt;
                  BDevice device = proxyExt.getDevice();
                  if (device instanceof BOpcUaNamespace) {
                     BOpcUaNamespace ns = (BOpcUaNamespace)device;
                     NodeManagerUaNode uaNodeManager = ns.getUaNodeManager();
                     String nodeId = proxyExt.getUaNodeId();
                     NodeId proxyExtNodeId = NodeId.parseNodeId(nodeId);
                     int index = proxyExtNodeId.getNamespaceIndex();
                     if (index == ns.getNamespaceIndex()) {
                        UaNode sourceNode = OpcUaServerUtil.getNode(uaNodeManager, proxyExtNodeId);
                        UaReference reference = sourceNode.getReference(Identifiers.HasCondition, false);
                        if (reference != null) {
                           UaNode conditionNode = reference.getOppositeNode(sourceNode);
                           if (conditionNode instanceof ConditionTypeNode) {
                              ConditionTypeNode almType = (ConditionTypeNode)conditionNode;
                              if (almType.isEnabled()) {
                                 int severity = this.getOpcUaSeverity().alarmStateToSeverity(alarmRecord.getSourceState());
                                 almType.setSeverity(severity);
                                 if (almType instanceof AlarmConditionTypeNode) {
                                    ((AlarmConditionTypeNode)almType).setActive(!alarmRecord.isNormal());
                                 }

                                 if (almType instanceof AcknowledgeableConditionTypeNode) {
                                    ((AcknowledgeableConditionTypeNode)almType).setAcked(false);
                                 }

                                 if (this.logger.isLoggable(Level.FINE)) {
                                    this.logger.fine("trigger alarm " + alarmRecord.getAckState());
                                 }

                                 OpcUaServerUtil.triggerEvent(almType, alarmRecord);
                              }
                           }
                        }
                     }
                  }
               } else if (this.logger.isLoggable(Level.FINE)) {
                  this.logger.fine("abstractProxyExt: " + abstractProxyExt.getTypeDisplayName(null));
               }
            } else if (this.logger.isLoggable(Level.FINE)) {
               this.logger.fine("alarm source is not BAlarmSourceExt: " + obj.getTypeDisplayName(null));
            }
         } else {
            this.logger.fine("alarmRecord is acknowledged");
         }
      } catch (Exception var19) {
         this.logger.severe("Exception while handling an alarm: " + var19);
      }
   }

   public BOpcUaServer getOpcUaServer() {
      if (this.opcUaServer != null) {
         return this.opcUaServer;
      } else {
         this.opcUaServer = (BOpcUaServer)Sys.getService(BOpcUaServer.TYPE);
         return this.opcUaServer;
      }
   }
}
