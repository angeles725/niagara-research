package com.tridium.opcUaServer.point;

import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaValueNode;
import com.prosysopc.ua.server.NodeManagerTable;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.tridium.driver.util.DrUtil;
import com.tridium.ndriver.point.BNProxyExt;
import com.tridium.opcUaServer.BOpcUaNamespace;
import com.tridium.opcUaServer.BOpcUaServer;
import com.tridium.opcUaServer.export.BIOpcExport;
import com.tridium.opcUaServer.node.OpcUaIoManagerListener;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Logger;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.control.BControlPoint;
import javax.baja.control.BPointExtension;
import javax.baja.driver.point.BIPointFolder;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLink;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "localPoint",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 4
   ), @NiagaraProperty(
      name = "localPointSlot",
      type = "BDynamicEnum",
      defaultValue = "BDynamicEnum.make(0, BEnumRange.make(new String[]{\"na\"}))",
      flags = 4
   ), @NiagaraProperty(
      name = "infoLogging",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   )})
@NiagaraAction(
   name = "enableInfoLog",
   parameterType = "BBoolean",
   defaultValue = "BBoolean.make(false)"
)
public class BOpcUaServerProxyExt extends BNProxyExt {
   public static final Property uaNodeId = newProperty(1, "", null);
   public static final Property localPoint = newProperty(4, BOrd.NULL, null);
   public static final Property localPointSlot = newProperty(4, BDynamicEnum.make(0, BEnumRange.make(new String[]{"na"})), null);
   public static final Property infoLogging = newProperty(4, false, null);
   public static final Action enableInfoLog = newAction(0, BBoolean.make(false), null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerProxyExt.class);
   public static final Logger rdlogger = Logger.getLogger("opcUaServer.point.rd");
   public static final Logger wrlogger = Logger.getLogger("opcUaServer.point.wr");
   boolean configFault = false;
   boolean hasHistoryExt = false;
   boolean hasAlarmExt = false;
   private static final String IMPORT_LINK_NAME = "impL";

   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public BOrd getLocalPoint() {
      return (BOrd)this.get(localPoint);
   }

   public void setLocalPoint(BOrd v) {
      this.set(localPoint, v, null);
   }

   public BDynamicEnum getLocalPointSlot() {
      return (BDynamicEnum)this.get(localPointSlot);
   }

   public void setLocalPointSlot(BDynamicEnum v) {
      this.set(localPointSlot, v, null);
   }

   public boolean getInfoLogging() {
      return this.getBoolean(infoLogging);
   }

   public void setInfoLogging(boolean v) {
      this.setBoolean(infoLogging, v, null);
   }

   public void enableInfoLog(BBoolean parameter) {
      this.invoke(enableInfoLog, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
         BOpcUaNamespace nodeSpace = this.getBOpcUaServerDevice();
         String results = nodeSpace.addControlPoint(this);
         if (results.startsWith("*")) {
            this.readFail(results);
         } else {
            this.setUaNodeId(results);
         }

         return null;
      }));

      try {
         if (this.isExport()) {
            this.addExportLink();
         } else {
            this.addImportLink();
         }
      } catch (Exception var2) {
         this.logMessage(rdlogger, "Error Starting an Proxy Ext");
      }
   }

   private boolean addExportLink() {
      String slotName = this.getLocalPointSlot().getTag();
      boolean isExport = "out".equals(slotName);
      if (isExport) {
         BControlPoint targetPoint = this.getParentPoint();
         BOrd ord = this.getLocalPoint();
         if (!ord.equals(BOrd.NULL)) {
            BControlPoint sourcePoint = (BControlPoint)this.getLocalPoint().get(this);
            BFacets sourceFacets = sourcePoint.getFacets();
            targetPoint.setFacets(sourceFacets);
            BLink newLink = new BLink(sourcePoint.getHandleOrd(), slotName, "in", true);
            Property rdLinkProp = null;

            try {
               rdLinkProp = targetPoint.add("rdLink", newLink, 2, Context.decoding);
            } catch (Exception var10) {
               this.logMessage(wrlogger, "Exception while adding and Export link: " + var10);
            }

            if (rdLinkProp != null && ((BLink)this.get(rdLinkProp)).isActive()) {
               BStatusValue sourceValue = sourcePoint.getOutStatusValue();
               targetPoint.set("in", sourceValue.newCopy(), null);
            }

            return true;
         }
      }

      return false;
   }

   private boolean addImportLink() {
      String slotName = this.getLocalPointSlot().getTag();
      boolean isExport = "out".equals(slotName);
      if (!isExport) {
         BControlPoint sourcePoint = this.getParentPoint();
         BOrd ord = this.getLocalPoint();
         if (!ord.equals(BOrd.NULL)) {
            BControlPoint targetPoint = (BControlPoint)ord.get(this);
            BLink newLink = new BLink(sourcePoint.getHandleOrd(), "out", slotName, true);
            Property wrLinkProp = null;

            try {
               wrLinkProp = targetPoint.add("impL", newLink, 2);
            } catch (Exception var9) {
               wrlogger.info(var9.getLocalizedMessage());
            }

            if (wrLinkProp != null && ((BLink)this.get(wrLinkProp)).isActive()) {
               BStatusValue sourceValue = (BStatusValue)sourcePoint.getOutStatusValue().newCopy();
               sourceValue.setStatus(BStatus.make(sourceValue.getStatus(), 1));
               targetPoint.set(slotName, sourceValue, null);
            }

            return true;
         }
      }

      return false;
   }

   public void stopped() throws Exception {
      if (!this.getUaNodeId().isEmpty()) {
         BOpcUaNamespace nodeSpace = this.getBOpcUaServerDevice();
         nodeSpace.deleteControlPoint(this);
      }

      super.stopped();
   }

   public BIPointFolder getParentPointFolder() {
      BComplex parent = this.getParent();

      while (parent != null && !(parent instanceof BIPointFolder)) {
         parent = parent.getParent();
      }

      return parent == null ? null : (BIPointFolder)parent;
   }

   public final BOpcUaServer getOpcUaServer() {
      return (BOpcUaServer)this.getNetwork();
   }

   public final BOpcUaNamespace getBOpcUaServerDevice() {
      return (BOpcUaNamespace)DrUtil.getParent(this, BOpcUaNamespace.TYPE);
   }

   public final BOpcUaServerPointDeviceExt getOpcUaServerPointDeviceExt() {
      return (BOpcUaServerPointDeviceExt)this.getDeviceExt();
   }

   public boolean isExport() {
      return "out".equals(this.getLocalPointSlot().getTag());
   }

   public void pointFacetsChanged() {
      BOpcUaNamespace uaNodeSpace = this.getBOpcUaServerDevice();
      uaNodeSpace.pointFacetsChanged(this);
      super.pointFacetsChanged();
   }

   public boolean requiresPointSubscription() {
      if (!Sys.atSteadyState()) {
         return super.requiresPointSubscription();
      } else {
         BControlPoint parentPoint = this.getParentPoint();
         boolean needToAddHistory = false;
         boolean needToAddAlarm = false;
         boolean historyRemoved = true;
         boolean alarmRemoved = true;

         for (BPointExtension extension : parentPoint.getExtensions()) {
            if (extension instanceof BHistoryExt) {
               historyRemoved = false;
               if (!this.hasHistoryExt) {
                  needToAddHistory = true;
               }
            } else if (extension instanceof BAlarmSourceExt) {
               alarmRemoved = false;
               if (!this.hasAlarmExt) {
                  needToAddAlarm = true;
               }
            }
         }

         if (needToAddHistory && !this.hasHistoryExt) {
            this.hasHistoryExt = this.getBOpcUaServerDevice().addUaNodeHistory(this.getUaNodeId(), parentPoint);
         } else if (!needToAddHistory && this.hasHistoryExt && historyRemoved) {
            this.hasHistoryExt = this.getBOpcUaServerDevice().removeUaNodeHistory(this.getUaNodeId(), parentPoint);
         }

         if (needToAddAlarm && !this.hasAlarmExt) {
            this.hasAlarmExt = this.getBOpcUaServerDevice().addUaNodeCondition(this.getUaNodeId(), parentPoint);
         } else if (!needToAddAlarm && this.hasAlarmExt && alarmRemoved) {
            this.hasAlarmExt = this.getBOpcUaServerDevice().removeUaNodeCondition(this.getUaNodeId(), parentPoint);
         }

         return super.requiresPointSubscription();
      }
   }

   public void readSubscribed(Context cx) throws Exception {
      BOpcUaServer opcUaServer = this.getOpcUaServer();
      if (opcUaServer != null) {
         OpcUaIoManagerListener ioManagerListener = this.getBOpcUaServerDevice().getIoManagerListener();
         if (ioManagerListener != null) {
            ioManagerListener.getOpcUaNamespace().addPointToControlPointsMap(this);
         }

         if (!this.getMode().equals(BReadWriteMode.writeonly) && ioManagerListener != null) {
            ioManagerListener.addMonitorPoint(this);
         }
      }
   }

   public void readUnsubscribed(Context cx) throws Exception {
      BOpcUaServer opcUaServer = this.getOpcUaServer();
      if (opcUaServer != null) {
         OpcUaIoManagerListener ioManagerListener = this.getBOpcUaServerDevice().getIoManagerListener();
         if (ioManagerListener != null) {
            ioManagerListener.getOpcUaNamespace().removePointFromControlPointsMap(this);
         }

         if (!this.getMode().equals(BReadWriteMode.writeonly) && ioManagerListener != null) {
            ioManagerListener.removeMonitorPoint(this);
         }
      }
   }

   public boolean write(Context cx) throws Exception {
      boolean isWritable = this.getParentPoint().isWritablePoint() || this.getParentPoint() instanceof BIOpcExport;
      if (!this.configFault && isWritable && !this.getUaNodeId().isEmpty() && cx != null) {
         BStatusValue wValue = this.getWriteValue();
         this.readOk(wValue);

         try {
            UaNode node = AccessController.doPrivileged((PrivilegedExceptionAction<UaNode>)(() -> {
               NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());
               NodeManagerTable addressSpace = this.getOpcUaServer().server.getAddressSpace();
               return addressSpace.getNode(nodeId);
            }));
            if (node instanceof UaValueNode) {
               UaValueNode valueNode = (UaValueNode)node;
               BValue bValue = wValue.getValueValue();
               Object wrValue;
               if (bValue instanceof BBoolean) {
                  wrValue = ((BBoolean)bValue).getBoolean();
               } else if (bValue instanceof BDouble) {
                  wrValue = ((BDouble)bValue).getNumeric();
               } else if (bValue instanceof BEnum) {
                  wrValue = ((BEnum)bValue).getOrdinal();
               } else {
                  wrValue = ((BString)bValue).getString();
               }

               DataValue wrDataValue = new DataValue(new Variant(wrValue), StatusCode.GOOD, DateTime.currentTime(), DateTime.currentTime());
               valueNode.setValue(wrDataValue);
            }
         } catch (Exception var9) {
            this.writeFail(var9.getLocalizedMessage());
         }

         return false;
      } else {
         return false;
      }
   }

   public void doWrite(BStatusValue out) {
      this.writeOk(out);
   }

   public Type getDeviceExtType() {
      return BOpcUaServerPointDeviceExt.TYPE;
   }

   public BReadWriteMode getMode() {
      BControlPoint parentPoint = this.getParentPoint();
      if (parentPoint instanceof BIOpcExport) {
         return BReadWriteMode.writeonly;
      } else {
         return parentPoint.isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
      }
   }

   public boolean isBoolean() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusBoolean;
   }

   public boolean isNumeric() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusNumeric;
   }

   public boolean isString() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusString;
   }

   public boolean isEnum() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusEnum;
   }

   public BValue getActionParameterDefault(Action action) {
      return (BValue)(action.equals(enableInfoLog) ? BBoolean.make(!this.getInfoLogging()) : super.getActionParameterDefault(action));
   }

   public void doEnableInfoLog(BBoolean enable) {
      this.setInfoLogging(enable.getBoolean());
      rdlogger.info("enableInfoLog:" + this.getParentPoint().getName() + " " + enable.getBoolean());
   }

   public void processValueUpdate(DataValue dataValue, String source) {
      boolean isUnwritten = dataValue.getSourceTimestamp() == null && dataValue.getServerTimestamp() == null;
      BStatus valueStatus = isUnwritten ? BStatus.stale : BStatus.ok;
      BStatusValue readValue = null;
      if (this.isNumeric()) {
         double dValue = dataValue.getValue().doubleValue();
         readValue = new BStatusNumeric(dValue, valueStatus);
      } else if (this.isBoolean()) {
         boolean value = dataValue.getValue().booleanValue();
         readValue = new BStatusBoolean(value, valueStatus);
      } else if (this.isEnum()) {
         BEnumRange range = (BEnumRange)this.getPointFacets().get("range");
         int value = dataValue.getValue().intValue();
         readValue = new BStatusEnum(BDynamicEnum.make(value, range), valueStatus);
      } else if (this.isString()) {
         String value = dataValue.getValue().toString();
         readValue = new BStatusString(value, valueStatus);
      }

      if (readValue == null) {
         this.readFail("unsupported type");
      } else {
         this.logMessage(rdlogger, source + this.getParentPoint().getName() + " rdValue = " + readValue);
         this.readOk(readValue);
      }
   }

   public void logMessage(Logger logger, String message) {
      if (this.getInfoLogging()) {
         logger.info(message);
      } else {
         logger.fine(message);
      }
   }
}
