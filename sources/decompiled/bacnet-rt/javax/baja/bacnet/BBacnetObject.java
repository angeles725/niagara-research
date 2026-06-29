package javax.baja.bacnet;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.asn.NWriteAccessSpec;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.config.BBacnetConfigDeviceExt;
import javax.baja.bacnet.config.BBacnetConfigFolder;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BExtensibleEnumList;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.data.BIDataValue;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BLoadable;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.space.BComponentSpace;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal"
   ), @NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 3
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")}
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_NAME, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(0, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()",
      flags = 20,
      override = true
   ), @NiagaraAction(
      name = "readBacnetProperty",
      parameterType = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue)",
      returnType = "BValue",
      flags = 4
   ), @NiagaraAction(
      name = "writeBacnetProperty",
      parameterType = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue)",
      flags = 4
   ), @NiagaraAction(
      name = "uploadRequiredProperties",
      flags = 4
   ), @NiagaraAction(
      name = "uploadOptionalProperties",
      flags = 4
   )})
public class BBacnetObject extends BLoadable implements BacnetConst, BIBacnetPollable {
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property status = newProperty(3, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.DEFAULT, makeFacets(75, 12));
   public static final Property objectName = newProperty(0, "", makeFacets(77, 7));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Action download = newAction(20, new BDownloadParameters(), null);
   public static final Action readBacnetProperty = newAction(4, BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue), null);
   public static final Action writeBacnetProperty = newAction(4, BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue), null);
   public static final Action uploadRequiredProperties = newAction(4, null);
   public static final Action uploadOptionalProperties = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetObject.class);
   private static final BIcon icon = BIcon.make("module://bacnet/com/tridium/bacnet/ui/icons/bacObject.png");
   protected static final Lexicon lex = Lexicon.make("bacnet");
   public static final Logger log = Logger.getLogger("bacnet.client");
   public static final Logger plog = Logger.getLogger("bacnet.point");
   private static HashMap<Integer, Array<TypeInfo>> byObjectType = new HashMap<>();
   private static boolean initialized = false;
   private static final BRelTime WPM_DELAY = BRelTime.make(20L);
   private static final Object UPLOAD_LOCK = new Object();
   protected volatile ArrayList<PollListEntry> polledProperties = new ArrayList<>();
   private BBacnetConfigDeviceExt config;
   private HashMap<BFacets, BBacnetObject.BacnetPropertyData> propDataMap = new HashMap<>();
   private HashSet<Property> wpmList = new HashSet<>();
   private Ticket wpmTkt = null;
   public static final String PID = "pId";
   public static final String ASN_TYPE = "asn";
   private static final BBacnetObject.BacnetPropertyData NOT_BACNET_PROPERTY = new BBacnetObject.BacnetPropertyData(-1, 0);

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public String getObjectName() {
      return this.getString(objectName);
   }

   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public BEnum getObjectType() {
      return (BEnum)this.get(objectType);
   }

   public void setObjectType(BEnum v) {
      this.set(objectType, v, null);
   }

   public BValue readBacnetProperty(BEnum parameter) {
      return this.invoke(readBacnetProperty, parameter, null);
   }

   public void writeBacnetProperty(BEnum parameter) {
      this.invoke(writeBacnetProperty, parameter, null);
   }

   public void uploadRequiredProperties() {
      this.invoke(uploadRequiredProperties, null, null);
   }

   public void uploadOptionalProperties() {
      this.invoke(uploadOptionalProperties, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BBacnetObject make(BBacnetObjectIdentifier id) {
      if (!initialized) {
         init();
      }

      Array<TypeInfo> o = byObjectType.get(id.getObjectType());
      if (o != null && o.size() > 0) {
         TypeInfo element = (TypeInfo)o.get(0);
         BBacnetObject bo = (BBacnetObject)element.getInstance();
         bo.setObjectId(id);
         return bo;
      } else {
         return new BBacnetObject();
      }
   }

   @Deprecated
   public static TypeInfo getTypeInfo(BBacnetObjectIdentifier id) {
      if (!initialized) {
         init();
      }

      Array<TypeInfo> a = byObjectType.get(id.getObjectType());
      return a != null ? (TypeInfo)a.first() : TYPE.getTypeInfo();
   }

   public static TypeInfo[] getTypeInfos(BBacnetObjectIdentifier id) {
      if (!initialized) {
         init();
      }

      Array<TypeInfo> a = byObjectType.get(id.getObjectType());
      return a != null ? (TypeInfo[])a.trim() : new TypeInfo[]{TYPE.getTypeInfo()};
   }

   public void started() throws Exception {
      this.checkConfig();
      this.buildPolledProperties();
      BBacnetObject obj = this.config().lookupBacnetObject(this.getObjectId());
      if (obj != null && obj != this) {
         log.severe("Duplicate Bacnet Object ID for config object " + this + " in " + this.device() + "; defaulting objectId!");
         this.setObjectId(BBacnetObjectIdentifier.make(this.getObjectType().getOrdinal()));
      }
   }

   public void stopped() throws Exception {
      try {
         this.network().getPollService(this).unsubscribe(this);
      } catch (NotRunningException var2) {
         log.warning("BBacnetObject.stopped:NotRunningException unsubscribing from polling on " + this);
      }

      this.polledProperties = null;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && cx != noWrite) {
         if (p.equals(objectId)) {
            this.removeAll(null);
            this.upload(new BUploadParameters());
            if (this.isSubscribed()) {
               BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
               pollService.unsubscribe(this);
               this.buildPolledProperties();
               pollService.subscribe(this);
            }
         } else {
            if (p.equals(pollFrequency) && this.isSubscribed()) {
               BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
               pollService.unsubscribe(this);
               pollService.subscribe(this);
            }

            if (cx != noWrite && !Flags.isReadonly(this, p) && p.getFacets().getFacet("pId") != null) {
               BBacnetDevice device = this.device();
               if (device != null && this.device().isServiceSupported("writePropertyMultiple")) {
                  synchronized (this.wpmList) {
                     this.wpmList.add(p);
                     if (this.wpmTkt == null) {
                        this.wpmTkt = Clock.schedule(this, WPM_DELAY, download, new BDownloadParameters());
                     }
                  }
               } else if (device != null) {
                  this.network().getWriteWorker().post(() -> {
                     try {
                        BStatus status = device.getStatus();
                        if (status != null && status.isOk()) {
                           this.writeProperty(p);
                        }
                     } catch (BacnetException var4) {
                        log.warning("Unable to write BACnet property " + p + " in " + this + ":" + var4);
                     }
                  });
               }
            }
         }
      }
   }

   public BFacets getSlotFacets(Slot slot) {
      if (slot.equals(objectId)) {
         if (!this.isMounted()) {
            return super.getSlotFacets(slot);
         }

         BFacets f = BBacnetObjectType.getObjectIdFacets(this.getObjectType().getOrdinal());
         if (f != null) {
            return f;
         }

         BBacnetDevice dev = this.device();
         if (dev != null) {
            BExtensibleEnumList elist = dev.getEnumerationList();
            if (elist != null) {
               return elist.getObjectTypeFacets();
            }
         }
      }

      if (slot.equals(objectType)) {
         BBacnetDevice dev = (BBacnetDevice)this.getDevice();
         if (dev != null) {
            BExtensibleEnumList elist = dev.getEnumerationList();
            if (elist != null) {
               return elist.getObjectTypeFacets();
            }
         }
      }

      if (slot.getName().equals(BBacnetPropertyIdentifier.statusFlags.getTag())) {
         return BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS;
      } else if (slot.getName().equals(BBacnetPropertyIdentifier.eventEnable.getTag())) {
         return BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_FACETS;
      } else if (slot.getName().equals(BBacnetPropertyIdentifier.ackedTransitions.getTag())) {
         return BacnetBitStringUtil.BACNET_EVENT_TRANSITION_BITS_FACETS;
      } else {
         return slot.getName().equals(BBacnetPropertyIdentifier.limitEnable.getTag())
            ? BacnetBitStringUtil.BACNET_LIMIT_ENABLE_FACETS
            : super.getSlotFacets(slot);
      }
   }

   public void subscribed() {
      if (this.isRunning()) {
         ((BBacnetPoll)this.network().getPollService(this)).subscribe(this);
         this.upload(new BUploadParameters(false));
      }
   }

   public void unsubscribed() {
      if (this.isRunning()) {
         ((BBacnetPoll)this.network().getPollService(this)).unsubscribe(this);
      }
   }

   public void doUpload(BUploadParameters p, Context cx) {
      BStatus status = this.getStatus();
      if (this.device().getEnabled() && !this.device().getStatus().isDown()) {
         if (this.getObjectId().isValid()) {
            this.setStatus(BStatus.make(status.getBits() | 16, BFacets.make("upload", "PENDING")));
            if (!this.device().isServiceSupported("readPropertyMultiple")) {
               this.uploadIndividual(new NReadAccessSpec(this.getObjectId(), this.device().getPossibleProperties(this.getObjectId())));
            } else {
               Vector specs = new Vector();
               specs.add(new NReadAccessSpec(this.getObjectId(), 8));
               Vector vals = null;
               boolean ok = false;

               try {
                  vals = client().readPropertyMultiple(this.device().getAddress(), specs);
                  if (vals == null) {
                     return;
                  }

                  Iterator it = ((NReadAccessResult)vals.elementAt(0)).getResults();
                  this.updateProperties(it);
                  ok = true;
               } catch (Exception var9) {
                  if (log.isLoggable(Level.FINE)) {
                     log.fine("Exception uploading " + this + " using rpm(ALL):" + var9);
                  }
               }

               if (!ok) {
                  try {
                     specs.clear();
                     specs.add(new NReadAccessSpec(this.getObjectId(), 105));
                     vals = client().readPropertyMultiple(this.device().getAddress(), specs);
                     Iterator it = ((NReadAccessResult)vals.elementAt(0)).getResults();
                     this.updateProperties(it);
                     specs.clear();
                     specs.add(new NReadAccessSpec(this.getObjectId(), 80));
                     vals = client().readPropertyMultiple(this.device().getAddress(), specs);
                     it = ((NReadAccessResult)vals.elementAt(0)).getResults();
                     this.updateProperties(it);
                     ok = true;
                  } catch (Exception var8) {
                     if (log.isLoggable(Level.FINE)) {
                        log.fine("Exception uploading " + this + " using rpm(REQ/OPT):" + var8);
                     }
                  }
               }

               if (!ok) {
                  this.uploadIndividual(new NReadAccessSpec(this.getObjectId(), this.device().getPossibleProperties(this.getObjectId())));
               }
            }

            this.setOutputFacets();
            BComponentSpace space = this.getComponentSpace();
            if (space != null) {
               space.update(this, 0);
            }

            this.buildPolledProperties();
            this.setStatus(BStatus.ok);
            if (log.isLoggable(Level.FINEST)) {
               log.finest(this.device().getName() + " object upload execution finish.");
            }
         }
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine(this.device().getName() + " is either disabled or status is down, object upload is unsuccessful.");
         }
      }
   }

   public void doUploadRequiredProperties() {
      this.uploadProperties(BBacnetPropertyIdentifier.required);
   }

   public void doUploadOptionalProperties() {
      this.uploadProperties(BBacnetPropertyIdentifier.optional);
   }

   private void uploadProperties(final BBacnetPropertyIdentifier propertyId) {
      BBacnetNetwork network = BBacnetNetwork.bacnet();
      if (propertyId == null) {
         if (log.isLoggable(Level.FINE)) {
            log.fine(lex.get("object.upload.unknown.error"));
         }
      } else {
         network.getWorker().post(new Runnable() {
            @Override
            public void run() {
               try {
                  Vector specs = new Vector();
                  specs.add(new NReadAccessSpec(BBacnetObject.this.getObjectId(), propertyId.getOrdinal()));
                  Vector vals = BBacnetObject.client().readPropertyMultiple(BBacnetObject.this.device().getAddress(), specs);
                  Iterator it = ((NReadAccessResult)vals.elementAt(0)).getResults();
                  BBacnetObject.this.updateProperties(it);
               } catch (BacnetException var4) {
                  if (BBacnetObject.log.isLoggable(Level.FINE)) {
                     BBacnetObject.log.log(Level.FINE, BBacnetObject.lex.getText("object.upload." + propertyId.getTag() + ".error"), (Throwable)var4);
                  }
               }
            }
         });
      }
   }

   public void doDownload(BDownloadParameters p, Context cx) {
      Property[] props = null;
      synchronized (this.wpmList) {
         if (this.wpmTkt != null) {
            this.wpmTkt.cancel();
         }

         this.wpmTkt = null;
         props = this.wpmList.toArray(new Property[0]);
         this.wpmList.clear();
      }

      boolean wpmOk = false;
      int firstFailPropId = -1;
      if (this.device().isServiceSupported("writePropertyMultiple")) {
         try {
            NWriteAccessSpec was = new NWriteAccessSpec(this.getObjectId());

            for (int i = 0; i < props.length; i++) {
               BBacnetObject.BacnetPropertyData d = this.getPropertyData(props[i]);
               if (d != NOT_BACNET_PROPERTY && d.propertyId != 75 && d.propertyId != 79) {
                  was.addPropertyValue(d.propertyId, AsnUtil.toAsn(this.get(props[i])));
               }
            }

            Vector writeSpecs = new Vector();
            writeSpecs.add(was);
            client().writePropertyMultiple(this.device().getAddress(), writeSpecs);
            wpmOk = true;
         } catch (ErrorException var10) {
            String msg = MessageFormat.format("BACnet Error downloading " + this + ":\nFailed write for {0}:", var10.getErrorParameters());
            firstFailPropId = ((BBacnetObjectPropertyReference)var10.getErrorParameters()[0]).getPropertyId();
            log.info(msg + var10);
         } catch (BacnetException var11) {
            log.log(Level.INFO, "BacnetException downloading " + this + ":" + var11, (Throwable)var11);
         }
      }

      if (!wpmOk) {
         boolean preFailure = true;

         for (int ix = 0; ix < props.length; ix++) {
            if (preFailure) {
               if (firstFailPropId != ((BInteger)props[ix].getFacets().getFacet("pId")).getInt()) {
                  continue;
               }

               preFailure = false;
            }

            try {
               this.writeProperty(props[ix]);
            } catch (Exception var9) {
               log.warning("Cannot write property " + props[ix] + " in " + this + ":" + var9);
            }
         }
      }

      if (log.isLoggable(Level.FINEST)) {
         log.finest(this.device().getName() + " object download execution finish.");
      }
   }

   public BValue doReadBacnetProperty(BEnum propId) throws BacnetException {
      if (!this.device().isDown()) {
         Property prop = this.lookupBacnetProperty(propId.getOrdinal());
         if (prop != null) {
            this.readProperty(prop);
            return this.get(prop);
         }
      }

      return null;
   }

   public void doWriteBacnetProperty(BEnum propId) throws BacnetException {
      if (!this.device().isDown()) {
         Property prop = this.lookupBacnetProperty(propId.getOrdinal());
         if (prop != null) {
            this.writeProperty(prop);
         }
      }
   }

   protected final BBacnetNetwork network() {
      return this.config != null ? this.config.network() : null;
   }

   @Override
   public final BBacnetDevice device() {
      if (this.config != null) {
         return this.config.device();
      } else {
         for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BBacnetDevice) {
               return (BBacnetDevice)parent;
            }
         }

         return null;
      }
   }

   protected final BBacnetConfigDeviceExt config() {
      return this.config;
   }

   private static final BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   public String toString(Context context) {
      return this.getName() + " [" + this.getObjectId().toString(context) + "]";
   }

   public Property getPresentValueProperty() {
      return null;
   }

   protected void setOutputFacets() {
   }

   protected boolean shouldPoll(int propertyId) {
      return true;
   }

   protected byte[] toEncodedValue(BBacnetObject.BacnetPropertyData d, Property p) {
      return AsnUtil.toAsn(d.getAsnType(), this.get(p));
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetConfigFolder && parent.getParent() instanceof BBacnetConfigFolder ? false : super.isParentLegal(parent);
   }

   public void readProperty(Property prop) throws BacnetException {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(prop);
      if (d != NOT_BACNET_PROPERTY && !this.device().isDown()) {
         if (prop.getType() == BBacnetArray.TYPE) {
            this.readArrayProperty(prop, d);
         } else {
            byte[] encodedValue = null;
            encodedValue = client().readProperty(this.device().getAddress(), this.getObjectId(), d.getPropertyId());
            this.set(prop, AsnUtil.fromAsn(d.getAsnType(), encodedValue, this.get(prop)), noWrite);
         }
      }
   }

   public void writeProperty(Property prop) throws BacnetException {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(prop);
      if (d != NOT_BACNET_PROPERTY) {
         if (!this.device().isServiceSupported("writeProperty")) {
            throw new UnsupportedOperationException(lex.getText("serviceNotSupported.writeProperty"));
         } else {
            client().writeProperty(this.device().getAddress(), this.getObjectId(), d.getPropertyId(), -1, this.toEncodedValue(d, prop), -1);
         }
      }
   }

   public void writeProperty(Property prop, int arrayIndex, byte[] encodedValue) throws BacnetException {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(prop);
      if (d != NOT_BACNET_PROPERTY) {
         if (!this.device().isServiceSupported("writeProperty")) {
            throw new UnsupportedOperationException(lex.getText("serviceNotSupported.writeProperty"));
         } else {
            client().writeProperty(this.device().getAddress(), this.getObjectId(), d.getPropertyId(), arrayIndex, encodedValue);
         }
      }
   }

   public void addListElement(Property prop, BValue listElement) throws BacnetException {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(prop);
      if (d != NOT_BACNET_PROPERTY) {
         if (this.get(prop).getType().is(BBacnetListOf.TYPE)) {
            if (!this.device().isServiceSupported("addListElement")) {
               throw new UnsupportedOperationException(lex.getText("serviceNotSupported.addListElement"));
            } else {
               byte[] encodedListElement = AsnUtil.toAsn(listElement);
               if (this.getObjectId().getInstanceNumber() != -1) {
                  client().addListElement(this.device().getAddress(), this.getObjectId(), d.getPropertyId(), -1, encodedListElement);
               }
            }
         }
      }
   }

   public void removeListElement(Property prop, BValue listElement) throws BacnetException {
      BBacnetObject.BacnetPropertyData d = this.getPropertyData(prop);
      if (d != NOT_BACNET_PROPERTY) {
         if (this.get(prop).getType().is(BBacnetListOf.TYPE)) {
            if (!this.device().isServiceSupported("writeProperty")) {
               throw new UnsupportedOperationException(lex.getText("serviceNotSupported.removeListElement"));
            } else {
               byte[] encodedListElement = AsnUtil.toAsn(listElement);
               client().removeListElement(this.device().getAddress(), this.getObjectId(), d.getPropertyId(), -1, encodedListElement);
            }
         }
      }
   }

   @Override
   public final int getPollableType() {
      return 2;
   }

   @Deprecated
   @Override
   public final boolean poll() {
      log.warning("BBacnetObject.poll() is DEPRECATED!!!");
      return false;
   }

   public final void readOk() {
      this.setStatus(BStatus.makeFault(this.getStatus(), false));
      this.setFaultCause("");
   }

   @Override
   public final void readFail(String failureMsg) {
      this.setStatus(BStatus.makeFault(this.getStatus(), true));
      this.setFaultCause(failureMsg);
   }

   @Override
   public final void fromEncodedValue(byte[] encodedValue, BStatus status, Context cx) {
      try {
         Property prop = this.lookupBacnetProperty(((PollListEntry)cx).getPropertyId());
         BInteger asnType = (BInteger)prop.getFacets().getFacet("asn");
         BValue v = AsnUtil.fromAsn(asnType.getInt(), encodedValue, this.get(prop));
         BacUtil.set(this, prop, v, noWrite);
         this.readOk();
      } catch (AsnException var7) {
         this.readFail(var7.toString());
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception decoding value for " + this + " [" + cx + "]:" + ByteArrayUtil.toHexString(encodedValue), (Throwable)var7);
         }
      } catch (Exception var8) {
         plog.log(Level.SEVERE, "Exception occurred in fromEncodedValue", (Throwable)var8);
      }
   }

   @Override
   public final PollListEntry[] getPollListEntries() {
      return this.polledProperties.toArray(new PollListEntry[0]);
   }

   private void checkConfig() {
      BBacnetConfigDeviceExt config = null;

      for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
         if (parent instanceof BBacnetConfigDeviceExt) {
            config = (BBacnetConfigDeviceExt)parent;
            break;
         }
      }

      this.config = config;
   }

   private void readArrayProperty(Property prop, BBacnetObject.BacnetPropertyData d) throws BacnetException {
      BBacnetAddress address = this.device().getAddress();
      BBacnetObjectIdentifier objectId = this.getObjectId();
      int propertyId = d.getPropertyId();
      byte[] encodedValue = null;

      try {
         encodedValue = client().readProperty(address, objectId, propertyId);
         this.set(prop, AsnUtil.fromAsn(d.getAsnType(), encodedValue, this.get(prop)), noWrite);
      } catch (Exception var27) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("Exception reading property " + prop + " in object " + this + ": " + var27 + "\n building array in groups...");
         }

         int arraySize = AsnUtil.fromAsnInteger(client().readProperty(address, objectId, propertyId, 0));
         boolean readOk = false;
         ByteArrayOutputStream os = new ByteArrayOutputStream();
         int index = 1;
         if (this.device().isServiceSupported("readPropertyMultiple")) {
            try {
               BBacnetArray arr = (BBacnetArray)this.get(prop);
               BTypeSpec arrTypeSpec = arr.getArrayTypeSpec();
               int elemSize = AsnUtil.getSize(arrTypeSpec);
               int hdrSize = 9;
               int elemHdr = 8;
               int maxAPDUSize = this.device().getMaxAPDULengthAccepted();
               int myMax = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
               if (maxAPDUSize > myMax) {
                  maxAPDUSize = myMax;
               }

               int safetyFactor = 10;
               int elemsPerRead = (maxAPDUSize - hdrSize - safetyFactor) / (elemSize + elemHdr);

               do {
                  Vector refs = new Vector();

                  for (int i = index; i < index + elemsPerRead && i <= arraySize; i++) {
                     refs.add(new NBacnetPropertyReference(propertyId, i));
                  }

                  Vector results = client().readPropertyMultiple(address, objectId, refs);

                  for (int j = 0; j < results.size(); j++) {
                     NReadPropertyResult rpr = (NReadPropertyResult)results.get(j);
                     byte[] val = rpr.getPropertyValue();
                     os.write(val, 0, val.length);
                     index++;
                  }
               } while (index <= arraySize);

               readOk = true;
            } catch (Exception var26) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("Exception reading property " + prop + " in object " + this + " in groups: " + var26 + "\n building array element by element...");
               }
            }
         }

         if (!readOk) {
            for (int i = index; i <= arraySize; i++) {
               byte[] encodedElement = client().readProperty(address, objectId, propertyId, i);
               os.write(encodedElement, 0, encodedElement.length);
            }
         }

         byte[] encodedArray = os.toByteArray();
         this.set(prop, AsnUtil.fromAsn(d.getAsnType(), encodedArray, this.get(prop)), noWrite);
      }
   }

   private boolean readArray(BBacnetArray a, int propId, PropertyInfo pi) {
      try {
         BBacnetAddress address = this.device().getAddress();
         BBacnetObjectIdentifier objectId = this.getObjectId();
         int asize = 0;

         try {
            asize = AsnUtil.fromAsnUnsignedInt(client().readProperty(address, objectId, propId, 0));
         } catch (Exception var11) {
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.FINE, "Cannot get array size", (Throwable)var11);
            }

            return false;
         }

         ByteArrayOutputStream os = new ByteArrayOutputStream();
         int i = 1;

         try {
            for (i = 1; i <= asize; i++) {
               byte[] encodedElement = client().readProperty(address, objectId, propId, i);
               os.write(encodedElement, 0, encodedElement.length);
            }

            byte[] encodedArray = os.toByteArray();
            AsnInputStream in = new AsnInputStream(encodedArray);
            a.readAsn(in);
            return true;
         } catch (Exception var12) {
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.FINE, "Exception reading array element " + i + ":" + var12, (Throwable)var12);
            }

            return false;
         }
      } catch (Throwable var13) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Unable to build BacnetArray for property " + propId, var13);
         }

         return false;
      }
   }

   protected void buildPolledProperties() {
      BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
      if (this.isSubscribed()) {
         pollService.unsubscribe(this);
      }

      SlotCursor<Property> sc = this.getProperties();
      BInteger pId = null;
      if (this.polledProperties.size() > 0) {
         this.polledProperties.clear();
      }

      while (sc.next()) {
         Property p = sc.property();
         pId = (BInteger)p.getFacets().getFacet("pId");
         if (pId != null && this.shouldPoll(pId.getInt())) {
            this.polledProperties.add(new PollListEntry(this.getObjectId(), pId.getInt(), this.device(), this));
         }
      }

      if (this.isSubscribed()) {
         pollService.subscribe(this);
      }
   }

   private void updateProperties(Iterator it) {
      synchronized (UPLOAD_LOCK) {
         while (it.hasNext()) {
            NReadPropertyResult rpr = (NReadPropertyResult)it.next();
            int propId = rpr.getPropertyId();
            Property prop = this.lookupBacnetProperty(propId);

            try {
               if (prop == null) {
                  if (!rpr.isError()) {
                     PropertyInfo propInfo = this.getPropertyInfo(propId);
                     BValue value = AsnUtil.asnToValue(propInfo, rpr.getPropertyValue());
                     String name = SlotPath.escape(propInfo.getName());
                     prop = this.add(name, value, 0, makeFacets(propInfo, value), null);
                     if (this.shouldPoll(propId)) {
                        this.polledProperties.add(new PollListEntry(this.getObjectId(), propId, this.device(), this));
                     }

                     if (!this.device().getEnumerationList().getPropertyIdRange().isOrdinal(propId)) {
                        this.device().getEnumerationList().addNewPropertyId(propInfo.getName(), propId);
                     }
                  }
               } else if (rpr.isError()) {
                  if (log.isLoggable(Level.FINE)) {
                     log.fine("Error uploading property " + prop + ":" + rpr.getPropertyAccessError());
                  }
               } else {
                  this.set(prop, AsnUtil.fromAsn(((BInteger)prop.getFacets().getFacet("asn")).getInt(), rpr.getPropertyValue(), this.get(prop)), noWrite);
               }
            } catch (AsnException var10) {
               log.info(
                  "Unable to convert encoded value: prop="
                     + prop
                     + ", id="
                     + propId
                     + ", val="
                     + ByteArrayUtil.toHexString(rpr.getPropertyValue())
                     + "\n"
                     + var10
               );
            } catch (Exception var11) {
               log.info(
                  "Unable to add/update property: prop="
                     + prop
                     + ", id="
                     + propId
                     + ", val="
                     + ByteArrayUtil.toHexString(rpr.getPropertyValue())
                     + "\n"
                     + var11
               );
               if (log.isLoggable(Level.FINE)) {
                  log.log(Level.FINE, "Stack Trace: ", (Throwable)var11);
               }
            }
         }
      }

      if (log.isLoggable(Level.FINEST)) {
         log.finest(this.device().getName() + " object updateProperties execution finish.");
      }
   }

   private void uploadIndividual(NReadAccessSpec spec) {
      PropertyReference[] refs = spec.getListOfPropertyReferences();

      for (int i = 0; i < refs.length; i++) {
         int propId = refs[i].getPropertyId();

         try {
            Property prop = this.lookupBacnetProperty(propId);
            if (prop != null) {
               this.readProperty(prop);
            } else {
               byte[] encodedValue = null;
               PropertyInfo propInfo = this.getPropertyInfo(propId);
               String name = SlotPath.escape(propInfo.getName());

               try {
                  encodedValue = client().readProperty(this.device().getAddress(), this.getObjectId(), propId);
                  BValue value = AsnUtil.asnToValue(propInfo, encodedValue);
                  prop = this.add(name, value, 0, makeFacets(propInfo, value), noWrite);
               } catch (BacnetException var11) {
                  if (var11 instanceof ErrorException && ((ErrorException)var11).getErrorType().getErrorCode() == 32) {
                     if (log.isLoggable(Level.FINE)) {
                        log.fine("Unknown Property " + propId + " in object " + this.getObjectId() + ": " + var11);
                     }
                     continue;
                  }

                  if (propInfo.isArray()) {
                     BBacnetArray a = new BBacnetArray();
                     a.setArrayTypeSpec(BTypeSpec.make(propInfo.getType()));
                     this.readArray(a, propId, propInfo);
                     prop = this.add(name, a, 0, makeFacets(propInfo, a), noWrite);
                  }

                  log.info("BacnetException uploading propertyId " + propId + " in object " + this.getObjectId() + ": " + var11);
               }

               if (this.shouldPoll(propId)) {
                  this.polledProperties.add(new PollListEntry(this.getObjectId(), propId, this.device(), this));
               }
            }
         } catch (TransactionException var12) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("TransactionException uploading object " + this.getObjectId() + " in " + this.device() + ": " + var12);
            }
            break;
         } catch (Exception var13) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("Exception uploading propertyId " + propId + " in object " + this.getObjectId() + ": " + var13);
            }
         }
      }
   }

   private PropertyInfo getPropertyInfo(int propId) {
      PropertyInfo propInfo = this.device().getPropertyInfo(this.getObjectId().getObjectType(), propId);
      if (propInfo == null) {
         propInfo = new PropertyInfo(BBacnetPropertyIdentifier.tag(propId), propId, -6);
      }

      return propInfo;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetObject", 2);
      out.prop("config", this.config);
      if (this.polledProperties != null) {
         int siz = this.polledProperties.size();
         out.prop("polledProperties", siz);

         for (int i = 0; i < siz; i++) {
            out.prop("polledProperties[" + i + "]:", this.polledProperties.get(i).debugString());
         }
      } else {
         out.prop("polledProperties", "NULL");
      }

      out.prop("propDataMap", this.propDataMap.size());

      for (Entry<BFacets, BBacnetObject.BacnetPropertyData> entry : this.propDataMap.entrySet()) {
         out.prop(entry.getKey(), entry.getValue());
      }

      out.endProps();
   }

   private Property lookupBacnetProperty(int propId) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next()) {
         try {
            Property property = c.property();
            BInteger propertyIdFacet = (BInteger)property.getFacets().getFacet("pId");
            if (propertyIdFacet != null && propertyIdFacet.getInt() == propId) {
               return property;
            }
         } catch (Exception var5) {
         }
      }

      return null;
   }

   public BIcon getIcon() {
      return icon;
   }

   static void init() {
      TypeInfo base = TYPE.getTypeInfo();
      TypeInfo[] types = Sys.getRegistry().getConcreteTypes(base);

      for (int i = 0; i < types.length; i++) {
         if (!types[i].equals(base)) {
            BBacnetObject o = (BBacnetObject)types[i].getInstance();
            int objTypOrd = o.getObjectType().getOrdinal();
            Array<TypeInfo> cur = byObjectType.get(objTypOrd);
            if (cur == null) {
               cur = new Array(TypeInfo.class);
            }

            cur.add(types[i]);
            byObjectType.put(objTypOrd, cur);
         }
      }

      initialized = true;
   }

   protected static BFacets makeFacets(int propertyId, int asnType) {
      HashMap<String, BIDataValue> map = new HashMap<>();
      map.put("pId", BInteger.make(propertyId));
      map.put("asn", BInteger.make(asnType));
      return BFacets.make(map);
   }

   protected static BFacets makeFacets(int propertyId, int asnType, Map<String, BIDataValue> m) {
      HashMap<String, BIDataValue> map = new HashMap<>(m);
      map.put("pId", BInteger.make(propertyId));
      map.put("asn", BInteger.make(asnType));
      return BFacets.make(map);
   }

   protected static BFacets makeFacets(int propertyId, int asnType, String[] keys, BIDataValue[] values) {
      if (keys.length != values.length) {
         throw new IllegalArgumentException();
      } else {
         String[] k = new String[keys.length + 2];
         System.arraycopy(keys, 0, k, 0, keys.length);
         k[keys.length] = "pId";
         k[keys.length + 1] = "asn";
         BIDataValue[] v = new BIDataValue[values.length + 2];
         System.arraycopy(values, 0, v, 0, values.length);
         v[values.length] = BInteger.make(propertyId);
         v[values.length + 1] = BInteger.make(asnType);
         return BFacets.make(k, v);
      }
   }

   protected static BFacets makeFacets(PropertyInfo info, BValue value) {
      HashMap<String, BIDataValue> map;
      if (info.isBitString()) {
         map = new HashMap<>(BacnetBitStringUtil.getBitStringMap(info.getBitStringName()));
      } else {
         map = new HashMap<>();
      }

      map.put("pId", BInteger.make(info.getId()));
      map.put("asn", BInteger.make(info.getAsnType()));
      return BFacets.make(map);
   }

   protected BBacnetObject.BacnetPropertyData getPropertyData(Property prop) {
      BFacets f = prop.getFacets();
      if (f == null) {
         return NOT_BACNET_PROPERTY;
      } else if (f.geti("pId", -1) == -1) {
         return NOT_BACNET_PROPERTY;
      } else {
         if (!prop.isDynamic()) {
            BBacnetObject.BacnetPropertyData d = this.propDataMap.get(f);
            if (d == null) {
               d = makePropertyData(f);
               this.propDataMap.put(f, d);
               return d;
            }
         }

         return makePropertyData(f);
      }
   }

   private static BBacnetObject.BacnetPropertyData makePropertyData(BFacets f) {
      int propertyId = -1;
      int asnType = 0;
      BObject s;
      if ((s = f.getFacet("pId")) != null) {
         propertyId = ((BInteger)s).getInt();
      }

      if ((s = f.getFacet("asn")) != null) {
         asnType = ((BInteger)s).getInt();
      }

      return BBacnetObject.BacnetPropertyData.make(propertyId, asnType);
   }

   public static class BacnetPropertyData {
      int propertyId;
      int asnType;

      private BacnetPropertyData(int propertyId, int asnType) {
         this.propertyId = propertyId;
         this.asnType = asnType;
      }

      static BBacnetObject.BacnetPropertyData make(int pid, int asn) {
         return pid == -1 && asn == 0 ? BBacnetObject.NOT_BACNET_PROPERTY : new BBacnetObject.BacnetPropertyData(pid, asn);
      }

      public int getPropertyId() {
         return this.propertyId;
      }

      public int getAsnType() {
         return this.asnType;
      }
   }
}
