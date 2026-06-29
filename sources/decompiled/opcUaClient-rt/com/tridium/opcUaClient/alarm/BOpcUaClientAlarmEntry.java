package com.tridium.opcUaClient.alarm;

import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.Attributes;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.BAlarmSeverities;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.data.BIDataValue;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "eventType",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "message",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "sourceNode",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "value",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "eventTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "severity",
      type = "int",
      defaultValue = "0",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "ackedState",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "activeState",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "eventId",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("SfUtil.incl()")}
   )})
public class BOpcUaClientAlarmEntry extends BComponent {
   public static final Property eventType = newProperty(1, "", SfUtil.incl());
   public static final Property message = newProperty(1, "", SfUtil.incl());
   public static final Property sourceNode = newProperty(1, "", SfUtil.incl());
   public static final Property value = newProperty(1, "", SfUtil.incl());
   public static final Property eventTime = newProperty(1, BAbsTime.NULL, SfUtil.incl());
   public static final Property severity = newProperty(1, 0, SfUtil.incl());
   public static final Property ackedState = newProperty(1, "", SfUtil.incl());
   public static final Property activeState = newProperty(1, "", SfUtil.incl());
   public static final Property eventId = newProperty(1, "", SfUtil.incl());
   public static final Type TYPE = Sys.loadType(BOpcUaClientAlarmEntry.class);
   public int faultSeverity = 900;
   public int normalSeverity = 500;
   public int offNormalSeverity = 700;
   public int alertSeverity = 600;
   private static final BIcon alarmIcon = BIcon.make("module://icons/x16/alarm.png");
   private static final BIcon redAlarmIcon = BIcon.make("module://alarm/com/tridium/alarm/icons/alarmRed.png");
   private static final BIcon greenAlarmIcon = BIcon.make("module://alarm/com/tridium/alarm/icons/alarmGreen.png");
   private static final BIcon orangeAlarmIcon = BIcon.make("module://alarm/com/tridium/alarm/icons/alarmOrange.png");
   private static final BIcon whiteAlarmIcon = BIcon.make("module://alarm/com/tridium/alarm/icons/alarmWhite.png");
   private static final Logger logger = Logger.getLogger("opcUaClient.alarm");

   public String getEventType() {
      return this.getString(eventType);
   }

   public void setEventType(String v) {
      this.setString(eventType, v, null);
   }

   public String getMessage() {
      return this.getString(message);
   }

   public void setMessage(String v) {
      this.setString(message, v, null);
   }

   public String getSourceNode() {
      return this.getString(sourceNode);
   }

   public void setSourceNode(String v) {
      this.setString(sourceNode, v, null);
   }

   public String getValue() {
      return this.getString(value);
   }

   public void setValue(String v) {
      this.setString(value, v, null);
   }

   public BAbsTime getEventTime() {
      return (BAbsTime)this.get(eventTime);
   }

   public void setEventTime(BAbsTime v) {
      this.set(eventTime, v, null);
   }

   public int getSeverity() {
      return this.getInt(severity);
   }

   public void setSeverity(int v) {
      this.setInt(severity, v, null);
   }

   public String getAckedState() {
      return this.getString(ackedState);
   }

   public void setAckedState(String v) {
      this.setString(ackedState, v, null);
   }

   public String getActiveState() {
      return this.getString(activeState);
   }

   public void setActiveState(String v) {
      this.setString(activeState, v, null);
   }

   public String getEventId() {
      return this.getString(eventId);
   }

   public void setEventId(String v) {
      this.setString(eventId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcUaClientAlarmEntry make(UaClient client, MonitoredEventItem sender, Variant[] eventFields, BAlarmSeverities severities) {
      if (logger.isLoggable(Level.FINER)) {
         logger.finer("BOpcUaClientAlarmEntry.Make event fields:");

         for (int i = 0; i < BOpcUaClientAlarmDeviceExt.eventFieldNames.length; i++) {
            System.out.println("     " + BOpcUaClientAlarmDeviceExt.eventFieldNames[i] + " = " + eventFields[i]);
         }
      }

      BOpcUaClientAlarmEntry alarmEntry = new BOpcUaClientAlarmEntry();
      alarmEntry.faultSeverity = severities.getToFault();
      alarmEntry.normalSeverity = severities.getToNormal();
      alarmEntry.offNormalSeverity = severities.getToOffnormal();
      alarmEntry.alertSeverity = severities.getToAlert();
      NodeId eventTypeId = (NodeId)eventFields[0].getValue();
      alarmEntry.setMessage(eventFields[1].toString(false));
      alarmEntry.setSourceNode(eventFields[9].toString(false));
      NodeId alarmNodeId = (NodeId)eventFields[9].getValue();
      NodeId sourceNodeId = (NodeId)eventFields[3].getValue();
      Object value = eventFields[4].getValue();
      if (value instanceof DateTime) {
         alarmEntry.setEventTime(OpcUaClientUtil.dateTimeToAbsTime((DateTime)value));
      }

      alarmEntry.setSeverity(eventFields[5].intValue());
      alarmEntry.setActiveState(alarmEntry.getSeverity() != severities.getToNormal() ? "true" : "false");
      alarmEntry.setAckedState(eventFields[6].toString(false));
      Variant eventId = eventFields[8];
      if (OpcUaClientUtil.getValueClass(eventId).equals(ByteString.class)) {
         ByteString eventIdValue = (ByteString)eventId.getValue();
         alarmEntry.setEventId(eventIdValue.toHex());
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("eventId=" + alarmEntry.getEventId());
         }
      }

      DataValue dataValue = null;

      try {
         dataValue = client.getAddressSpace().getNode(sourceNodeId).readAttribute(Attributes.Value);
         alarmEntry.setValue(dataValue.getValue().toString(false));
         alarmEntry.setEventType(eventTypeId.toString());
      } catch (Exception var12) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred while setting a Value and Event", (Throwable)var12);
         } else {
            logger.log(Level.SEVERE, "Exception occurred while setting a Value and Event: " + var12);
         }
      }

      return alarmEntry;
   }

   public BFacets makeAlarmFacets() {
      Property[] properties = this.getPropertiesArray();
      String[] keys = new String[properties.length];
      BIDataValue[] values = new BIDataValue[properties.length];

      for (int i = 0; i < properties.length; i++) {
         keys[i] = properties[i].getName();
         values[i] = (BIDataValue)this.get(properties[i]);
      }

      BFacets almFacets = BFacets.make(keys, values);
      BFacets msgFacet = BFacets.make("msgText", this.getAlarmMessage());
      return BFacets.make(almFacets, msgFacet);
   }

   public String getAlarmMessage() {
      return this.getMessage();
   }

   public boolean isOffNormal() {
      return this.getActiveState().equals("true");
   }

   public boolean isFault() {
      return this.getSeverity() == this.faultSeverity;
   }

   public boolean isNormal() {
      return this.getSeverity() == this.normalSeverity;
   }

   public boolean isAcked() {
      return this.getAckedState().equals("true");
   }

   public BIcon getIcon() {
      if (this.isFault()) {
         return orangeAlarmIcon;
      } else {
         return this.isOffNormal() ? redAlarmIcon : greenAlarmIcon;
      }
   }
}
