package com.tridium.opcUaClient.alarm;

import com.prosysopc.ua.ContentFilterBuilder;
import com.prosysopc.ua.MonitoredItemBase;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.MonitoredEventItemListener;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.Attributes;
import com.prosysopc.ua.stack.core.ElementOperand;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.FilterOperand;
import com.prosysopc.ua.stack.core.FilterOperator;
import com.prosysopc.ua.stack.core.LiteralOperand;
import com.prosysopc.ua.stack.core.MethodIdentifiers;
import com.prosysopc.ua.stack.core.ObjectTypeIdentifiers;
import com.prosysopc.ua.stack.core.SimpleAttributeOperand;
import com.tridium.ndriver.discover.BINDiscoveryHost;
import com.tridium.ndriver.discover.BINDiscoveryObject;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.history.BOpcUaClientHistoryDiscoveryPreferences;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.BAlarmSeverities;
import com.tridium.util.CompUtil;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmSupport;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmSourceInfo;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.driver.alarm.BAlarmDeviceExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "autoSubscribeEnable",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "AlarmSourceInfo",
      type = "BAlarmSourceInfo",
      defaultValue = "new BAlarmSourceInfo()"
   ), @NiagaraProperty(
      name = "opcUaSeverity",
      type = "BAlarmSeverities",
      defaultValue = "BAlarmSeverities.DEFAULT"
   )})
@NiagaraAction(
   name = "submitAlarmDiscoveryJob",
   returnType = "BOrd",
   flags = 4
)
public class BOpcUaClientAlarmDeviceExt extends BAlarmDeviceExt implements MonitoredEventItemListener, BIAlarmSource, BINDiscoveryHost {
   public static final Property autoSubscribeEnable = newProperty(0, true, null);
   public static final Property AlarmSourceInfo = newProperty(0, new BAlarmSourceInfo(), null);
   public static final Property opcUaSeverity = newProperty(0, BAlarmSeverities.DEFAULT, null);
   public static final Action submitAlarmDiscoveryJob = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientAlarmDeviceExt.class);
   private Subscription subscription;
   public static final QualifiedName[] EMPTY_QUALIFIED_NAME = new QualifiedName[0];
   protected static final int FIELD_EVENT_TYPE = 0;
   protected static final int FIELD_MESSAGE = 1;
   protected static final int FIELD_SOURCE_NAME = 2;
   protected static final int FIELD_SOURCE_NODE = 3;
   protected static final int FIELD_TIME = 4;
   protected static final int FIELD_SEVERITY = 5;
   protected static final int FIELD_ACKED_STATE = 6;
   protected static final int FIELD_ACTIVE_STATE = 7;
   protected static final int FIELD_EVENT_ID = 8;
   protected static final int FIELD_EVENT_SOURCE = 9;
   protected static final int FIELD_EVENT_VALUE = 10;
   protected static final QualifiedName[] eventFieldNames = new QualifiedName[]{
      new QualifiedName("EventType"),
      new QualifiedName("Message"),
      new QualifiedName("SourceName"),
      new QualifiedName("SourceNode"),
      new QualifiedName("Time"),
      new QualifiedName("Severity"),
      new QualifiedName("AckedState/Id"),
      new QualifiedName("ActiveState/Id"),
      new QualifiedName("EventId")
   };
   private BOpcUaDevice device;
   private UaClient client;
   private EventFilter filter;
   private AlarmSupport alarmSupport;
   public static final Logger logger = Logger.getLogger("opcUaClient.alarm");
   private Hashtable<Variant, BOpcUaClientAlarmEntry> lastEventItems;

   public boolean getAutoSubscribeEnable() {
      return this.getBoolean(autoSubscribeEnable);
   }

   public void setAutoSubscribeEnable(boolean v) {
      this.setBoolean(autoSubscribeEnable, v, null);
   }

   public BAlarmSourceInfo getAlarmSourceInfo() {
      return (BAlarmSourceInfo)this.get(AlarmSourceInfo);
   }

   public void setAlarmSourceInfo(BAlarmSourceInfo v) {
      this.set(AlarmSourceInfo, v, null);
   }

   public BAlarmSeverities getOpcUaSeverity() {
      return (BAlarmSeverities)this.get(opcUaSeverity);
   }

   public void setOpcUaSeverity(BAlarmSeverities v) {
      this.set(opcUaSeverity, v, null);
   }

   public BOrd submitAlarmDiscoveryJob() {
      return (BOrd)this.invoke(submitAlarmDiscoveryJob, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      this.alarmSupport = new AlarmSupport(this, this.getAlarmSourceInfo());
      this.lastEventItems = new Hashtable<>();
      super.started();
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcUaDevice;
   }

   public boolean isChildLegal(BComponent child) {
      return child instanceof BOpcUaClientAlarmSource;
   }

   public BOrd doSubmitAlarmDiscoveryJob(Context cx) {
      BStatus status = this.getDevice().getStatus();
      return !status.isDisabled() && !status.isDown() ? new BOpcUaClientDiscoverAlarmsJob(this).submit(cx) : null;
   }

   public BOrd submitDiscoveryJob(BNDiscoveryPreferences discoveryParams) {
      return null;
   }

   public BNDiscoveryPreferences getDiscoveryPreferences() {
      return new BOpcUaClientHistoryDiscoveryPreferences();
   }

   public BINDiscoveryObject[] getDiscoveryObjects(BNDiscoveryPreferences prefs) throws Exception {
      ArrayList<BOpcUaNodeLearnEntry> items = this.getAlarmItems();
      if (items == null) {
         ((BOpcUaDevice)this.getDevice()).doLearn();
         items = this.getAlarmItems();
      }

      return items.toArray(new BOpcUaNodeLearnEntry[0]);
   }

   public ArrayList<BOpcUaNodeLearnEntry> getAlarmItems() {
      ArrayList<BOpcUaNodeLearnEntry> list = new ArrayList<>();
      BValue bValue = this.getOpcUaDevice().get("serverRoot");
      if (bValue == null) {
         return null;
      } else {
         BComponent learnRoot = bValue.asComponent();
         BOpcUaNodeLearnEntry[] descendants = (BOpcUaNodeLearnEntry[])CompUtil.getDescendants(learnRoot, BOpcUaNodeLearnEntry.class);

         for (BOpcUaNodeLearnEntry descendant : descendants) {
            if (descendant.hasCondition() || descendant.isEventSubscribable()) {
               list.add((BOpcUaNodeLearnEntry)descendant.newCopy(true));
            }
         }

         return list;
      }
   }

   public BOpcUaDevice getOpcUaDevice() {
      return (BOpcUaDevice)this.getDevice();
   }

   public void doRouteAlarm(BAlarmRecord record) throws Exception {
   }

   public void updateStatus() {
      for (BOpcUaClientAlarmSource alarmSource : (BOpcUaClientAlarmSource[])this.getChildren(BOpcUaClientAlarmSource.class)) {
         alarmSource.updateStatus();
      }
   }

   public BBoolean doAckAlarm(BAlarmRecord record) throws Exception {
      BString eventIdStr = (BString)record.getAlarmFacet("eventId");
      BString eventSourceId = (BString)record.getAlarmFacet("sourceNode");
      NodeId sourceId = NodeId.parseNodeId(eventSourceId.getString());
      NodeId ackMethodId = MethodIdentifiers.AcknowledgeableConditionType_Acknowledge;
      ByteString eventId = ByteString.fromHex(eventIdStr.getString());
      LocalizedText comment = new LocalizedText("TridiumUser", Locale.ENGLISH);
      Variant[] inputs = new Variant[]{new Variant(eventId), new Variant(comment)};

      try {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("acknowledge: eventIdStr   =" + eventIdStr.getString());
            logger.fine("acknowledge: eventSourceId=" + eventSourceId.getString());
         }

         OpcUaClientUtil.call(this.client, sourceId, ackMethodId, inputs);
      } catch (PrivilegedActionException var11) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "PrivilegedActionException occurred when acknowledging Alarm", (Throwable)var11.getException());
         } else {
            logger.log(Level.SEVERE, "PrivilegedActionException occurred when acknowledging Alarm: " + var11.getException());
         }
      }

      try {
         this.getAlarmSupport(sourceId).ackAlarm(record);
      } catch (Exception var10) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred when acknowledging Alarm", (Throwable)var10);
         } else {
            logger.log(Level.SEVERE, "Exception occurred when acknowledging Alarm: " + var10);
         }
      }

      return BBoolean.TRUE;
   }

   public void addAlarm(BOpcUaClientAlarmEntry event, MonitoredEventItem sender) {
      try {
         BFacets niagaraAlarmFacets = event.makeAlarmFacets();
         AlarmSupport almSupport = this.alarmSupport;
         BOpcUaClientAlarmSource alarmSource = this.getAlarmSource(sender.getNodeId());
         if (alarmSource != null) {
            almSupport = alarmSource.getAlarmSupport();
            alarmSource.setLastEvent(event);
         }

         if (event.isFault()) {
            almSupport.newFaultAlarm(niagaraAlarmFacets);
         } else if (event.isOffNormal()) {
            almSupport.newOffnormalAlarm(niagaraAlarmFacets);
         } else {
            almSupport.toNormal(niagaraAlarmFacets, null);
         }
      } catch (Exception var6) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred when adding Alarm", (Throwable)var6);
         } else {
            logger.log(Level.SEVERE, "Exception occurred when adding Alarm: " + var6);
         }
      }
   }

   BOpcUaClientAlarmSource getAlarmSource(NodeId nodeId) {
      for (BOpcUaClientAlarmSource almSource : (BOpcUaClientAlarmSource[])this.getChildren(BOpcUaClientAlarmSource.class)) {
         if (nodeId.toString().equals(almSource.getUaNodeId())) {
            return almSource;
         }
      }

      return null;
   }

   AlarmSupport getAlarmSupport(NodeId nodeId) {
      BOpcUaClientAlarmSource alarmSource = this.getAlarmSource(nodeId);
      return alarmSource != null ? alarmSource.getAlarmSupport() : this.alarmSupport;
   }

   public void onEvent(MonitoredEventItem sender, Variant[] eventFields) {
      BOpcUaClientAlarmEntry newAlarmEvent = BOpcUaClientAlarmEntry.make(this.client, sender, eventFields, this.getOpcUaSeverity());
      BOpcUaClientAlarmEntry lastAlarmEvent = this.lastEventItems.get(eventFields[9]);
      this.lastEventItems.put(eventFields[9], newAlarmEvent);
      if (lastAlarmEvent == null || lastAlarmEvent.getSeverity() != newAlarmEvent.getSeverity() || lastAlarmEvent.isAcked() || !newAlarmEvent.isAcked()) {
         this.addAlarm(newAlarmEvent, sender);
      }
   }

   public void init() {
      this.client = ((BOpcUaDevice)this.getDevice()).uaClient;
      this.createEventFilter(eventFieldNames);

      for (BOpcUaClientAlarmSource clientAlarmSource : (BOpcUaClientAlarmSource[])this.getChildren(BOpcUaClientAlarmSource.class)) {
         if (clientAlarmSource.getSubscribed()) {
            clientAlarmSource.doSubscribeForEvents();
         }
      }
   }

   protected EventFilter createEventFilter(QualifiedName[] eventFields) {
      NodeId eventTypeId = ObjectTypeIdentifiers.BaseEventType;
      UnsignedInteger eventAttributeId = Attributes.Value;
      String indexRange = null;
      SimpleAttributeOperand[] selectClauses = new SimpleAttributeOperand[eventFields.length + 2];

      for (int i = 0; i < eventFields.length; i++) {
         QualifiedName[] browsePath = OpcUaClientUtil.createBrowsePath(eventFields[i]);
         selectClauses[i] = new SimpleAttributeOperand(eventTypeId, browsePath, eventAttributeId, indexRange);
      }

      selectClauses[eventFields.length] = new SimpleAttributeOperand(eventTypeId, EMPTY_QUALIFIED_NAME, Attributes.NodeId, null);
      selectClauses[eventFields.length + 1] = new SimpleAttributeOperand(eventTypeId, EMPTY_QUALIFIED_NAME, Attributes.Value, null);
      this.filter = new EventFilter();
      this.filter.setSelectClauses(selectClauses);
      ContentFilterBuilder fb = new ContentFilterBuilder();
      fb.add(FilterOperator.Not, new FilterOperand[]{new ElementOperand(UnsignedInteger.valueOf(1L))});
      LiteralOperand filteredType = new LiteralOperand(new Variant(ObjectTypeIdentifiers.GeneralModelChangeEventType));
      fb.add(FilterOperator.OfType, new FilterOperand[]{filteredType});
      this.filter.setWhereClause(fb.getContentFilter());
      return this.filter;
   }

   public boolean isEventSubscribed(NodeId nodeId) {
      return this.getSubscription().hasItem(nodeId, Attributes.EventNotifier);
   }

   public void removeMonitorEvent(NodeId nodeId) {
      if (this.getSubscription().hasItem(nodeId, Attributes.EventNotifier)) {
         try {
            OpcUaClientUtil.removeItemFromSubscription(this.getSubscription(), this.getSubscription().getItem(nodeId, Attributes.EventNotifier), true);
         } catch (Exception var3) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.SEVERE, "Exception occurred when removing monitored event to subscription", (Throwable)var3);
            } else {
               logger.log(Level.SEVERE, "Exception occurred when removing monitored event to subscription: " + var3);
            }
         }
      }
   }

   public void addMonitorEvent(NodeId nodeId) {
      EventFilter filter = this.getEventFilter();
      if (filter == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Event filter not initialized. Please connect the device.");
         }
      } else if (this.getSubscription().hasItem(nodeId, Attributes.EventNotifier)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Event already subscribed: " + nodeId.toString());
         }
      } else {
         MonitoredEventItem item = new MonitoredEventItem(nodeId, filter);
         item.setEventListener(this);

         try {
            OpcUaClientUtil.addItemToSubscription(this.getSubscription(), item, true);
         } catch (Exception var5) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.SEVERE, "Exception occurred when adding monitored event to subscription", (Throwable)var5);
            } else {
               logger.log(Level.SEVERE, "Exception occurred when adding monitored event to subscription: " + var5);
            }
         }
      }
   }

   protected EventFilter getEventFilter() {
      return this.filter;
   }

   protected Subscription getSubscription() {
      if (this.subscription != null) {
         UaClient client = this.subscription.getClient();
         if (client == this.getOpcUaClientDevice().uaClient) {
            return this.subscription;
         }

         this.subscription = null;
      }

      this.subscription = new Subscription();

      try {
         this.subscription.setPublishingInterval(1000.0);
         OpcUaClientUtil.addSubscription(this.getOpcUaClientDevice().uaClient, this.subscription);
      } catch (Exception var2) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.SEVERE, "Exception occurred when adding a Subscription", (Throwable)var2);
         } else {
            logger.log(Level.SEVERE, "Exception occurred when adding a Subscription: " + var2);
         }
      }

      return this.subscription;
   }

   private BOpcUaDevice getOpcUaClientDevice() {
      return (BOpcUaDevice)this.getDevice();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      Subscription subscription = this.getSubscription();
      if (subscription != null) {
         boolean first = true;
         out.startProps();

         for (MonitoredItemBase monitoredItem : subscription.getItems()) {
            if (first) {
               out.trTitle("Opc UA Monitored Items", 2);
               first = false;
            }

            out.prop("Item", monitoredItem.toString());
         }

         out.endProps();
      }
   }
}
