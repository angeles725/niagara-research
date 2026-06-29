package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.job.BacnetDiscoveryUtil;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetNull;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.datatypes.BIBacnetDataType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.point.BBacnetTuningPolicyMap;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.MetaDataContext;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.data.BIDataValue;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIEnum;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.virtual.BVirtualComponent;
import javax.baja.virtual.BVirtualComponentSpace;

@NiagaraType
@Deprecated
@NiagaraProperty(
   name = "facets",
   type = "BFacets",
   defaultValue = "BFacets.NULL"
)
@NiagaraAction(
   name = "subscribe",
   flags = 20
)
public class BBacnetVirtualComponent extends BVirtualComponent implements BIBacnetPollable, BacnetConst {
   public static final Property facets = newProperty(0, BFacets.NULL, null);
   public static final Action subscribe = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetVirtualComponent.class);
   private BBacnetObjectIdentifier objectId = BBacnetObjectIdentifier.DEFAULT;
   private String tpName = "defaultPolicy";
   private BBacnetTuningPolicy cachedPolicy = null;
   private int writePriority = -1;
   private PollListEntry[] ples = null;
   private Hashtable<String, Property> propsMap = new Hashtable<>();
   private Hashtable<Property, Hashtable<PollListEntry, BStatus>> propsStatusMap = null;
   private boolean isPollSubscribed = false;
   private static AsnInputStream asnIn = new AsnInputStream();
   private static Logger log = Logger.getLogger("bacnet.virtual");
   static final String USE_FACETS = "useFacets";
   static final String PROPERTY_ID = "propertyId";
   public static final String INDEX = "index";
   static final String POLICY_DEF = "policy=";
   static final int POLICY_DEF_LEN = 7;
   static final String PRIORITY_DEF = "priority=";
   static final int PRIORITY_DEF_LEN = 9;
   static final String STATUS_TAG = "status=";
   static final int STATUS_TAG_LEN = 7;
   static final String STATUS_SOURCE_FACET = "statusSrc";

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public void subscribe() {
      this.invoke(subscribe, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetVirtualComponent() {
   }

   public BBacnetVirtualComponent(String virtualPathName) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("BVC ctor(): vpath=" + virtualPathName);
      }

      try {
         StringTokenizer st = new StringTokenizer(virtualPathName, ";");
         this.objectId = (BBacnetObjectIdentifier)BBacnetObjectIdentifier.DEFAULT.decodeFromString(st.nextToken());

         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.startsWith("policy=")) {
               this.tpName = s.substring(7);
            } else if (s.startsWith("priority=")) {
               try {
                  this.writePriority = Integer.parseInt(s.substring(9));
               } catch (Exception var5) {
                  if (log.isLoggable(Level.FINE)) {
                     log.fine("Invalid priority: " + s + " in virtualPathName");
                  }
               }
            }
         }
      } catch (IOException var6) {
         log.log(Level.SEVERE, "IOException occurred in BBacnetVirtualComponent", (Throwable)var6);
      }
   }

   public void doSubscribe() {
      BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
      pollService.subscribe(this);
      this.isPollSubscribed = true;
   }

   public String toString(Context cx) {
      return this.getName() + " (V)";
   }

   public boolean isChildLegal(BComponent c) {
      return true;
   }

   public void stopped() throws Exception {
      super.stopped();
      BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
      pollService.unsubscribe(this);
   }

   public void added(Property p, Context cx) {
      if (log.isLoggable(Level.FINE)) {
         log.fine(
            this
               + ".added(): "
               + p.getName()
               + " ["
               + p
               + "] cx="
               + cx
               + (this.isSubscribed() ? " subscribed" : " unsubscribed")
               + (this.ples != null ? "  ples:" + this.ples.length : " ples=null")
         );
      }

      if (this.isRunning()) {
         if (this.isSubscribed()) {
            Array<PollListEntry> a;
            if (this.ples == null) {
               a = new Array(PollListEntry.class);
            } else {
               a = new Array(this.ples);
            }

            this.addPolledProperty(p, a);
            if (a.size() > 0) {
               this.ples = (PollListEntry[])a.trim();
            }

            if (!this.isPollSubscribed) {
               this.subscribe();
            }
         }
      }
   }

   public void removed(Property p, BValue oldValue, Context cx) {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".removed(): " + p.getName() + " [" + p + "] ov=" + oldValue + " cx=" + cx + (this.isSubscribed() ? " subscribed" : " unsubscribed"));
      }

      if (this.isRunning()) {
         if (this.isSubscribed()) {
            Array<PollListEntry> a;
            if (this.ples == null) {
               a = new Array(PollListEntry.class);
            } else {
               a = new Array(this.ples);
            }

            this.removePolledProperty(p, oldValue, a);
            this.ples = (PollListEntry[])a.trim();
         }
      }
   }

   public void subscribed() {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".subscribed(): path=" + this.getSlotPathOrd());
      }

      this.network().postAsync(new Runnable() {
         @Override
         public void run() {
            HashMap<String, BIDataValue> m = BacnetDiscoveryUtil.discoverFacets(BBacnetVirtualComponent.this.objectId, BBacnetVirtualComponent.this.device());
            BBacnetVirtualComponent.this.setFacets(BFacets.make(m));
         }
      });
      if (this.getPollListEntries() != null) {
         this.isPollSubscribed = true;
         this.subscribe();
      }
   }

   public void unsubscribed() {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".unsubscribed(): path=" + this.getSlotPathOrd());
      }

      BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
      pollService.unsubscribe(this);
      this.ples = null;
      this.isPollSubscribed = false;
      this.propsMap.clear();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (cx != noWrite) {
         if (this.isRunning()) {
            BFacets f = p.getFacets();
            if (log.isLoggable(Level.FINE)) {
               log.fine("changed(" + p + ", " + cx + ") on " + this + " prop facets=" + f);
            }

            BInteger pid = (BInteger)f.getFacet("propertyId");
            if (pid != null) {
               BValue v = this.get(p);
               int index = -1;
               if (v instanceof BBacnetVirtualArray && cx != null) {
                  BInteger ndx = (BInteger)cx.getFacet("index");
                  if (ndx != null) {
                     index = ndx.getInt();
                     v = ((BBacnetVirtualArray)v).getElement(index);
                  }
               }

               byte[] encodedValue = null;
               PropertyInfo pi = this.device().getPropertyInfo(this.objectId.getObjectType(), pid.getInt());
               if (v instanceof BStatusValue) {
                  BStatusValue sv = (BStatusValue)v;
                  if (sv.getStatus().isNull()) {
                     encodedValue = AsnUtil.toAsnNull();
                  } else if (pi != null) {
                     encodedValue = AsnUtil.toAsn(pi.getAsnType(), sv.getValueValue());
                  } else {
                     encodedValue = AsnUtil.toAsn(v);
                  }
               } else if (pi != null) {
                  encodedValue = AsnUtil.toAsn(pi.getAsnType(), v);
               } else {
                  encodedValue = AsnUtil.toAsn(v);
               }

               this.network().postWrite(new BBacnetVirtualComponent.Write(pid.getInt(), index, encodedValue, this.writePriority));
            }
         }
      }
   }

   protected BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public int getWritePriority() {
      return this.writePriority;
   }

   public BBacnetTuningPolicy getPolicy() {
      if (this.cachedPolicy == null) {
         BBacnetTuningPolicyMap map = this.getPolicyMap();
         BValue x = map.get(this.tpName);
         if (x instanceof BBacnetTuningPolicy) {
            this.cachedPolicy = (BBacnetTuningPolicy)x;
         } else {
            log.warning("TuningPolicy not found: " + this.tpName);
            this.cachedPolicy = (BBacnetTuningPolicy)map.getDefaultPolicy();
         }
      }

      return this.cachedPolicy;
   }

   public BFacets getSlotFacets(Slot s) {
      BFacets f = s.getFacets();
      BBoolean useFacets = (BBoolean)f.getFacet("useFacets");
      if (useFacets != null && useFacets.getBoolean()) {
         BFacets vcfac = this.getFacets();
         return vcfac.equals(BFacets.NULL) ? f : vcfac;
      } else {
         return f;
      }
   }

   public static boolean isStatusProp(String virtualPathName) {
      return virtualPathName.indexOf("status=") >= 0;
   }

   public static BString getStatusSource(String propertyName) {
      StringBuilder sb = new StringBuilder();
      StringTokenizer st = new StringTokenizer(propertyName, ";");

      while (st.hasMoreTokens()) {
         String s = st.nextToken();
         if (s != null && s.startsWith("status=")) {
            sb.append(s.substring(7)).append(';');
         }
      }

      return BString.make(sb.toString());
   }

   public void updateStatus() {
      BStatus devStatus = this.device().getStatus();
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BStatusValue.class)) {
         BStatusValue sv = (BStatusValue)sc.get();
         int oldStatus = sv.getStatus().getBits();
         int newStatus = sv.getStatus().getBits();
         if (devStatus.isDisabled()) {
            newStatus |= 1;
         } else {
            newStatus &= -2;
         }

         if (devStatus.isDown()) {
            newStatus |= 4;
         } else {
            newStatus &= -5;
         }

         if (devStatus.isFault()) {
            newStatus |= 2;
         } else {
            newStatus &= -3;
         }

         if (oldStatus != newStatus) {
            sv.setStatus(BStatus.make(newStatus));
         }
      }
   }

   public BPollFrequency getPollFrequency() {
      BBacnetTuningPolicy policy = this.getPolicy();
      return policy != null ? policy.getPollFrequency() : BPollFrequency.normal;
   }

   @Override
   public BBacnetDevice device() {
      return (BBacnetDevice)((BVirtualComponentSpace)this.getComponentSpace()).getVirtualGateway().getParent();
   }

   @Override
   public int getPollableType() {
      return 3;
   }

   @Deprecated
   @Override
   public boolean poll() {
      return false;
   }

   @Override
   public void readFail(String failureMsg) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("readFail(" + failureMsg + ") on " + this);
      }
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus status, Context cx) {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".fromEncodedValue:" + ByteArrayUtil.toHexString(encodedValue) + " status=" + status + "  cx=" + cx);
      }

      try {
         PollListEntry ple = (PollListEntry)cx;
         Context baseCx = cx.getBase();
         if (baseCx instanceof MetaDataContext) {
            this.readMetaData(encodedValue, cx, (MetaDataContext)baseCx);
            return;
         }

         Property p = this.getProperty(ple.getPropertyId(), ple.getPropertyArrayIndex());
         if (p == null) {
            this.readFail("No property in virtual point for " + ple);
            ((BBacnetPoll)this.network().getPollService(this)).removePLE(this, ple);
            return;
         }

         BValue v = this.get(p);
         BStatusValue sv = null;
         if (v instanceof BStatusValue) {
            sv = (BStatusValue)v.newCopy();
         }

         synchronized (asnIn) {
            this.setProp(p, encodedValue, v, sv, ple.getPropertyArrayIndex());
            SlotCursor<Property> c = this.getProperties();
            String pname = p.getName();

            while (c.next()) {
               if (c.property().getName().startsWith(pname)) {
                  v = c.get();
                  sv = null;
                  if (v instanceof BStatusValue) {
                     sv = (BStatusValue)v.newCopy();
                  }

                  this.setProp(c.property(), encodedValue, v, sv, ple.getPropertyArrayIndex());
               }
            }
         }

         this.readOk(p, ple);
      } catch (AsnException var14) {
         this.readFail(var14.toString());
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Exception decoding value for " + this + ":" + var14 + " ev=" + ByteArrayUtil.toHexString(encodedValue), (Throwable)var14);
         }
      }
   }

   @Override
   public PollListEntry[] getPollListEntries() {
      if (!this.isRunning()) {
         return null;
      } else {
         if (this.ples == null) {
            Array<PollListEntry> a = new Array(PollListEntry.class);
            SlotCursor<Property> sc = this.getProperties();

            while (sc.next()) {
               this.addPolledProperty(sc.property(), a);
            }

            if (a.size() > 0) {
               this.ples = (PollListEntry[])a.trim();
            }
         }

         return this.ples;
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetVirtualComponent", 2);
      out.prop("objectId", this.objectId);
      out.prop("tpName", this.tpName);
      out.prop("cachedPolicy", this.cachedPolicy);
      out.prop("writePriority", this.writePriority);
      out.prop("isPollSubscribed", this.isPollSubscribed);
      if (this.ples != null) {
         out.prop("PollListEntries (unsynch)", this.ples.length);

         for (int i = 0; i < this.ples.length; i++) {
            out.prop("  " + i, this.ples[i]);
         }
      }

      out.prop("props", this.propsMap.size());
      Enumeration<String> e = this.propsMap.keys();

      while (e.hasMoreElements()) {
         Object k = e.nextElement();
         out.prop(" " + k, this.propsMap.get(k));
      }

      out.endProps();
   }

   private Property getProperty(int propertyId, int index) {
      return this.propsMap.get(String.valueOf(propertyId << 16 | index & 65535));
   }

   private BBacnetNetwork network() {
      return (BBacnetNetwork)this.device().getNetwork();
   }

   private BBacnetTuningPolicyMap getPolicyMap() {
      BBacnetTuningPolicyMap map = (BBacnetTuningPolicyMap)this.network().get("tuningPolicies");
      if (map != null) {
         return map;
      } else {
         throw new IllegalStateException("Network missing tuningPolicies property");
      }
   }

   private void readOk(Property p, PollListEntry ple) {
      if (this.get(p) instanceof BStatusValue) {
         this.updateStatus(p, ple, BStatus.ok);
      }
   }

   private void addPolledProperty(Property p, Array<PollListEntry> a) {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".addPolledProperty(): " + p + " facets=" + this.getFacets());
      }

      int index = -1;
      BFacets f = p.getFacets();
      BInteger pid = (BInteger)f.getFacet("propertyId");
      if (pid != null) {
         BValue v = this.get(p);
         if (v instanceof BBacnetVirtualArray) {
            BBacnetVirtualArray va = (BBacnetVirtualArray)this.get(p);
            SlotCursor<Property> sc = va.getProperties();
            sc.next();
            sc.next();

            while (sc.next()) {
               index = BBacnetVirtualArray.index(sc.property().getName());
               a.add(new PollListEntry(this.objectId, pid.getInt(), index, this.device(), this));
               String key = String.valueOf(pid.getInt() << 16 | index & 65535);
               this.propsMap.put(key, sc.property());
            }
         } else {
            BInteger ndx = (BInteger)f.getFacet("index");
            if (ndx != null) {
               index = ndx.getInt();
            }

            a.add(new PollListEntry(this.objectId, pid.getInt(), index, this.device(), this));
            String key = String.valueOf(pid.getInt() << 16 | index & 65535);
            this.propsMap.put(key, p);
            BString statusSourceFacet = (BString)f.getFacet("statusSrc");
            if (v instanceof BStatusValue && statusSourceFacet != null) {
               for (StringTokenizer st = new StringTokenizer(statusSourceFacet.getString(), ";"); st.hasMoreTokens(); key = p.getName()) {
                  this.addDoprPLE(st.nextToken(), a, p);
               }
            }

            this.propsMap.put(key, p);
         }
      }
   }

   private void addDoprPLE(String token, Array<PollListEntry> a, Property baseProp) {
      BBacnetDeviceObjectPropertyReference dopr = BBacnetDeviceObjectPropertyReference.fromString(token);
      if (dopr != null) {
         BBacnetObjectIdentifier deviceId = dopr.getDeviceId();
         if (dopr.isDeviceIdUsed() && deviceId.hashCode() != this.device().getObjectId().hashCode()) {
            BBacnetDevice dev = this.network().doLookupDeviceById(deviceId);
            if (dev == null) {
               throw new IllegalStateException("Cannot find BACnet device for virtual component metadata:" + this + "  ref=" + dopr);
            }

            a.add(new PollListEntry(dopr.getObjectId(), dopr.getPropertyId(), dopr.getPropertyArrayIndex(), dev, this, new MetaDataContext(baseProp.getName())));
            this.propsMap.put(baseProp.getName(), baseProp);
         } else {
            a.add(
               new PollListEntry(
                  dopr.getObjectId(), dopr.getPropertyId(), dopr.getPropertyArrayIndex(), this.device(), this, new MetaDataContext(baseProp.getName())
               )
            );
            this.propsMap.put(baseProp.getName(), baseProp);
         }
      }
   }

   private void removePolledProperty(Property p, BValue oldValue, Array<PollListEntry> a) {
      if (log.isLoggable(Level.FINE)) {
         log.fine(this + ".removePolledProperty(): " + p);
      }

      int index = -1;
      BFacets f = p.getFacets();
      BInteger pid = (BInteger)f.getFacet("propertyId");
      if (pid != null) {
         PollListEntry ple = null;
         BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
         if (oldValue instanceof BBacnetVirtualArray) {
            BBacnetVirtualArray va = (BBacnetVirtualArray)oldValue;
            SlotCursor<Property> sc = va.getProperties();
            sc.next();
            sc.next();

            while (sc.next()) {
               index = Integer.parseInt(sc.property().getName().substring(7));
               ple = new PollListEntry(this.objectId, pid.getInt(), index, this.device(), this);
               a.remove(ple);
               this.propsMap.remove(String.valueOf(pid.getInt() << 16 | index & 65535));
               pollService.removePLE(this, ple);
            }
         } else {
            BInteger ndx = (BInteger)f.getFacet("index");
            if (ndx != null) {
               index = ndx.getInt();
            }

            ple = new PollListEntry(this.objectId, pid.getInt(), index, this.device(), this);
            a.remove(ple);
            this.propsMap.remove(String.valueOf(pid.getInt() << 16 | index & 65535));
            pollService.removePLE(this, ple);
         }
      }
   }

   private void setProp(Property p, byte[] encodedValue, BValue v, BStatusValue sv, int index) throws AsnException {
      synchronized (asnIn) {
         asnIn.setBuffer(encodedValue);
         if (v instanceof BBacnetVirtualArray) {
            ((BBacnetVirtualArray)v).readAsn(asnIn, index);
         } else if (v instanceof BIBacnetDataType) {
            BIBacnetDataType obj = (BIBacnetDataType)v;
            obj.readAsn(asnIn);
         } else {
            v = this.readAsn(v, sv, p.getFacets(), encodedValue);
            if (sv != null) {
               this.set(p, sv, noWrite);
            } else {
               this.set(p, v, noWrite);
            }
         }
      }
   }

   private void readMetaData(byte[] encodedValue, Context cx, MetaDataContext meta) throws AsnException {
      PollListEntry ple = (PollListEntry)cx;
      Property p = this.getProperty(meta.getPropertyName());
      BStatus metaStatus = null;
      StringBuilder sb = new StringBuilder();
      if (ple.getObjectId().hashCode() != this.objectId.hashCode()) {
         sb.append(ple.getObjectId().toString(facetsContext)).append('_');
      }

      sb.append(BBacnetPropertyIdentifier.tag(ple.getPropertyId()));
      String propertyName = sb.toString();
      synchronized (asnIn) {
         asnIn.setBuffer(encodedValue);
         metaStatus = this.readAsnMetaData(encodedValue, ple.getObjectId(), ple.getPropertyId(), propertyName);
      }

      this.updateStatus(p, ple, metaStatus);
   }

   private BValue readAsn(BValue val, BStatusValue sv, BFacets f, byte[] encodedValue) throws AsnException {
      BValue v = null;
      synchronized (asnIn) {
         int tag = asnIn.peekApplicationTag();
         switch (tag) {
            case 0:
               v = BBacnetNull.DEFAULT;
               this.setValueValue(v, sv, null);
               break;
            case 1:
               v = BBoolean.make(asnIn.readBoolean());
               this.setValueValue(v, sv, null);
               break;
            case 2:
               BBacnetUnsigned u = asnIn.readUnsigned();
               v = u;
               if (sv != null) {
                  sv.setStatusNull(false);
                  if (!(sv instanceof BStatusEnum)) {
                     sv.setValueValue(BString.make(u.toString()));
                  }
               }
               break;
            case 3:
               v = asnIn.readSigned();
               this.setValueValue(v, sv, null);
               break;
            case 4:
               v = BDouble.make(asnIn.readReal());
               this.setValueValue(v, sv, null);
               break;
            case 5:
               v = BDouble.make(asnIn.readDouble());
               this.setValueValue(v, sv, null);
               break;
            case 6:
               v = BBacnetOctetString.make(asnIn.readOctetString());
               this.setValueValue(v, sv, null);
               break;
            case 7:
               v = BString.make(asnIn.readCharacterString());
               this.setValueValue(v, sv, null);
               break;
            case 8:
               v = asnIn.readBitString();
               this.setValueValue(v, sv, null);
               break;
            case 9:
               int en = asnIn.readEnumerated();
               BEnumRange r = (BEnumRange)f.getFacet("range");
               if (val instanceof BIEnum && r == null) {
                  r = (BEnumRange)((BIEnum)val).getEnumFacets().getFacet("range");
               }

               if (r != null) {
                  v = r.get(en);
               } else {
                  v = BDynamicEnum.make(en);
               }

               this.setValueValue(BInteger.make(en), sv, r);
               break;
            case 10:
               v = asnIn.readDate();
               this.setValueValue(v, sv, null);
               break;
            case 11:
               v = asnIn.readTime();
               this.setValueValue(v, sv, null);
               break;
            case 12:
               v = asnIn.readObjectIdentifier();
               this.setValueValue(v, sv, null);
            case 13:
            case 14:
            case 15:
               break;
            default:
               if (log.isLoggable(Level.FINE)) {
                  log.fine(this + ": unexpected tag:" + tag);
               }

               var obj = (BIBacnetDataType & BValue)val;
               obj.readAsn(asnIn);
               if (obj instanceof BValue) {
                  v = (BValue)obj;
                  this.setValueValue(v, sv, null);
               }
         }

         return v;
      }
   }

   private void setValueValue(BValue v, BStatusValue sv, BEnumRange r) {
      if (sv != null) {
         if (v != null && !(v instanceof BBacnetNull)) {
            sv.setStatusNull(false);
            if (sv instanceof BStatusNumeric) {
               if (v instanceof BDouble) {
                  sv.setValueValue(v);
               } else {
                  if (!(v instanceof BNumber)) {
                     throw new IllegalArgumentException("Can't setValueValue: v=" + v + " [" + v.getType() + "] sv=" + sv + " [baja:StatusNumeric]");
                  }

                  ((BStatusNumeric)sv).setValue(((BNumber)v).getDouble());
               }
            } else if (sv instanceof BStatusBoolean) {
               if (v instanceof BBoolean) {
                  sv.setValueValue(v);
               } else {
                  if (!(v instanceof BNumber)) {
                     throw new IllegalArgumentException("Can't setValueValue: v=" + v + " [" + v.getType() + "] sv=" + sv + " [baja:StatusBoolean]");
                  }

                  ((BStatusBoolean)sv).setValue(((BNumber)v).getInt() != 0);
               }
            } else if (sv instanceof BStatusEnum) {
               if (v instanceof BDynamicEnum) {
                  sv.setValueValue(v);
               } else {
                  if (!(v instanceof BNumber)) {
                     throw new IllegalArgumentException("Can't setValueValue: v=" + v + " [" + v.getType() + "] sv=" + sv + " [baja:StatusEnum]");
                  }

                  ((BStatusEnum)sv).setValue(BDynamicEnum.make(((BNumber)v).getInt(), r));
               }
            } else if (sv instanceof BStatusString) {
               sv.setValueValue(BString.make(v.toString()));
            } else {
               sv.setValueValue(v);
            }
         } else {
            sv.setStatusNull(true);
         }
      }
   }

   private BStatus readAsnMetaData(byte[] encodedValue, BBacnetObjectIdentifier objectId, int propertyId, String propertyName) throws AsnException {
      int bits = 0;
      BFacets f = BFacets.NULL;
      synchronized (asnIn) {
         int tag = asnIn.peekApplicationTag();
         switch (tag) {
            case 0:
               bits |= 64;
               break;
            case 1:
               f = BFacets.make(propertyName, BBoolean.make(asnIn.readBoolean()));
               break;
            case 2:
               f = BFacets.make(propertyName, BLong.make(asnIn.readUnsignedInteger()));
               break;
            case 3:
               f = BFacets.make(propertyName, asnIn.readSigned());
               break;
            case 4:
               f = BFacets.make(propertyName, asnIn.readFloat());
               break;
            case 5:
               f = BFacets.make(propertyName, BDouble.make(asnIn.readDouble()));
               break;
            case 6:
               f = BFacets.make(propertyName, BString.make(ByteArrayUtil.toHexString(asnIn.readOctetString())));
               break;
            case 7:
               f = BFacets.make(propertyName, BString.make(asnIn.readCharacterString()));
               break;
            case 8:
               if (propertyId == 111) {
                  bits |= asnIn.readStatusFlags().getBits();
               } else {
                  f = BFacets.make(propertyName, asnIn.readBitString().toString(BacnetBitStringUtil.getBitStringTags(objectId.getObjectType(), propertyId)));
               }
               break;
            case 9:
               f = BFacets.make(propertyName, BString.make(this.device().getEnumRange(objectId.getObjectType(), propertyId).getTag(asnIn.readEnumerated())));
               break;
            case 10:
               f = BFacets.make(propertyName, BString.make(asnIn.readDate().toString()));
               break;
            case 11:
               f = BFacets.make(propertyName, BString.make(asnIn.readTime().toString()));
               break;
            case 12:
               f = BFacets.make(propertyName, BString.make(asnIn.readObjectIdentifier().toString()));
            case 13:
            case 14:
            case 15:
               break;
            default:
               f = BFacets.make(propertyName, BString.make(AsnUtil.fromAsn(encodedValue)[0].toString()));
         }
      }

      return BStatus.make(bits, f);
   }

   private void updateStatus(Property p, PollListEntry ple, BStatus status) {
      if (this.propsStatusMap == null) {
         this.propsStatusMap = new Hashtable<>();
      }

      Hashtable<PollListEntry, BStatus> propMap = this.propsStatusMap.get(p);
      if (propMap == null) {
         this.propsStatusMap.put(p, propMap = new Hashtable<>());
      }

      propMap.put(ple, status);
      this.updateStatus(p, 0);
   }

   private void updateStatus(Property p, int bits) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("updateStatus(" + p.getName() + ", " + bits + ")");
      }

      if (this.propsStatusMap == null) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("no propsStatusMap");
         }
      } else if (!(this.get(p) instanceof BStatusValue)) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("prop not a statusValue");
         }
      } else {
         BStatusValue sv = (BStatusValue)this.get(p).newCopy();
         Hashtable<PollListEntry, BStatus> propMap = this.propsStatusMap.get(p);
         if (propMap == null) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("no propMap for " + p.getName());
            }
         } else {
            Enumeration<BStatus> e = propMap.elements();
            BFacets facets = BFacets.NULL;

            while (e.hasMoreElements()) {
               BStatus s = e.nextElement();
               bits |= s.getBits();
               facets = BFacets.make(facets, s.getFacets());
            }

            sv.setStatus(BStatus.make(bits, facets));
            this.set(p, sv, noWrite);
         }
      }
   }

   class Write implements Runnable {
      int propertyId;
      int propertyArrayIndex = -1;
      byte[] ev = null;
      int priority = -1;

      Write(int propertyId, int propertyArrayIndex, byte[] ev, int pri) {
         if (propertyId == 87) {
            this.propertyId = 85;
            this.priority = propertyArrayIndex;
         } else {
            this.propertyId = propertyId;
            this.propertyArrayIndex = propertyArrayIndex;
            this.priority = pri;
         }

         this.ev = ev;
      }

      @Override
      public void run() {
         try {
            BBacnetVirtualComponent.this.network()
               .getBacnetComm()
               .writeProperty(
                  BBacnetVirtualComponent.this.device().getAddress(),
                  BBacnetVirtualComponent.this.objectId,
                  this.propertyId,
                  this.propertyArrayIndex,
                  this.ev,
                  this.priority
               );
         } catch (TransactionException var2) {
            BBacnetVirtualComponent.this.device().ping();
            BBacnetVirtualComponent.log.warning("TransactionException writing " + BBacnetPropertyIdentifier.tag(this.propertyId) + " in " + this);
         } catch (BacnetException var3) {
            BBacnetVirtualComponent.log
               .log(Level.SEVERE, "BacnetException writing " + BBacnetPropertyIdentifier.tag(this.propertyId) + " in " + this, (Throwable)var3);
         }
      }
   }
}
