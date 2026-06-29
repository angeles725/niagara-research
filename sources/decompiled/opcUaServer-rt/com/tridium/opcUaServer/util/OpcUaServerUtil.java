package com.tridium.opcUaServer.util;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.types.opcua.DiscreteAlarmType;
import com.prosysopc.ua.types.opcua.LimitAlarmType;
import com.prosysopc.ua.types.opcua.server.AcknowledgeableConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.point.BOpcUaServerProxyExt;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.control.BControlPoint;
import javax.baja.control.BPointExtension;
import javax.baja.history.BTrendRecord;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.util.Lexicon;

public final class OpcUaServerUtil {
   private static final Lexicon lex = Lexicon.make("opcUaServer");
   private static final Logger logger = Logger.getLogger("opcUaServer.util");

   private OpcUaServerUtil() {
   }

   public static BControlPoint findOpcUaPoint(String nodeId, BOpcUaNamespace nodeSpace) {
      for (BOpcUaServerProxyExt proxyExt : (BOpcUaServerProxyExt[])nodeSpace.getPoints().getChildren(BOpcUaServerProxyExt.class)) {
         if (proxyExt.getUaNodeId().equals(nodeId)) {
            return proxyExt.getParentPoint();
         }
      }

      return null;
   }

   public static BHistoryExt getPointHistoryExt(BControlPoint point) {
      for (BPointExtension extension : point.getExtensions()) {
         if (extension instanceof BHistoryExt) {
            return (BHistoryExt)extension;
         }
      }

      return null;
   }

   public static BAlarmSourceExt getPointAlarmExt(BControlPoint point) {
      for (BPointExtension extension : point.getExtensions()) {
         if (extension instanceof BAlarmSourceExt) {
            return (BAlarmSourceExt)extension;
         }
      }

      return null;
   }

   public static DataValue trendRecordToDataValue(BTrendRecord tr) {
      DateTime dt = absTimeToDateTime(tr.getTimestamp());
      Property prop = tr.getValueProperty();
      BValue bValue = tr.get(prop);
      Object value = null;
      if (bValue instanceof BDouble) {
         value = ((BDouble)bValue).getDouble();
      } else if (bValue instanceof BBoolean) {
         value = ((BBoolean)bValue).getBoolean();
      } else if (bValue instanceof BEnum) {
         value = ((BEnum)bValue).getOrdinal();
      } else if (bValue instanceof BString) {
         value = ((BString)bValue).getString();
      }

      if (value != null) {
         Variant variant = new Variant(value);
         return new DataValue(variant, StatusCode.GOOD, dt, dt);
      } else {
         return null;
      }
   }

   public static DateTime absTimeToDateTime(BAbsTime dt) {
      return DateTime.fromMillis(dt.getMillis());
   }

   public static BAbsTime dateTimeToAbsTime(DateTime dt) {
      return BAbsTime.make(dt.getTimeInMillis());
   }

   public static UaNode getNode(NodeManagerUaNode nodeManager, NodeId nodeId) {
      try {
         return AccessController.doPrivileged((PrivilegedExceptionAction<UaNode>)(() -> nodeManager.getNode(nodeId)));
      } catch (PrivilegedActionException var3) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while getting node " + nodeId, (Throwable)var3);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while getting node " + nodeId + ": " + var3);
         }

         return null;
      }
   }

   public static AlarmConditionTypeNode createAlarmTypeInstance(NodeManagerUaNode uaNodeManager, Class<?> alarmTypeClass, String name, NodeId myAlarmId) {
      try {
         return AccessController.doPrivileged(
            (PrivilegedExceptionAction<AlarmConditionTypeNode>)(() -> (AlarmConditionTypeNode)uaNodeManager.createInstance(alarmTypeClass, name, myAlarmId))
         );
      } catch (PrivilegedActionException var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while creating AlarmConditionTypeNode " + myAlarmId, (Throwable)var5);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while creating AlarmConditionTypeNode " + myAlarmId + ": " + var5);
         }

         return null;
      }
   }

   public static void triggerEvent(ConditionTypeNode opcUaEvent, BAlarmRecord n4Alarm) {
      BFacets alarmData = n4Alarm.getAlarmData();
      BObject toState = alarmData.get("toState", BString.DEFAULT);
      BObject presentValue = alarmData.get("presentValue", BString.DEFAULT);
      if (n4Alarm.isNormal()) {
         opcUaEvent.setMessage(new LocalizedText(lex.getText("alarm.returnToNormal", new Object[]{presentValue})));
      } else if (opcUaEvent instanceof LimitAlarmType) {
         opcUaEvent.setMessage(new LocalizedText(lex.getText("alarm.levelExceeded", new Object[]{toState, presentValue})));
      } else if (opcUaEvent instanceof DiscreteAlarmType) {
         opcUaEvent.setMessage(new LocalizedText(lex.getText("alarm.discrete", new Object[]{toState, presentValue})));
      }

      if (opcUaEvent instanceof AcknowledgeableConditionTypeNode) {
         AcknowledgeableConditionTypeNode opcUaEvent1 = (AcknowledgeableConditionTypeNode)opcUaEvent;
         opcUaEvent1.setAcked(n4Alarm.isAcknowledged());
      }

      opcUaEvent.triggerEvent(ByteString.valueOf(n4Alarm.getUuid().getBytes()));
   }

   public static int getBaseThreadCount(int cores) {
      return Math.min(8, cores);
   }
}
