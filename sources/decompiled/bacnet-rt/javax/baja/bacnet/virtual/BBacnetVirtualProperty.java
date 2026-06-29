package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.GrandchildChangedContext;
import javax.baja.bacnet.util.MetaDataContext;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;
import javax.baja.virtual.BVirtualComponent;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "BBacnetPropertyIdentifier.PRESENT_VALUE",
      flags = 7
   ), @NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.stale",
      flags = 3
   ), @NiagaraProperty(
      name = "useFacets",
      type = "boolean",
      defaultValue = "false",
      flags = 7
   )})
public class BBacnetVirtualProperty extends BVirtualComponent implements BacnetConst, BIBacnetPollable, BIStatus {
   public static final Property propertyId = newProperty(7, 85, null);
   public static final Property status = newProperty(3, BStatus.stale, null);
   public static final Property useFacets = newProperty(7, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetVirtualProperty.class);
   private static final int FATAL_FAULT = 1;
   private static final int STALE = 2;
   protected static final Lexicon lex = Lexicon.make("bacnet");
   static Logger log = Logger.getLogger("bacnet.virtual");
   public static final String VALUE = "value";
   protected static final String WRITE_ACTION_NAME = SlotPath.escape(lex.getText("BacnetVirtualProperty.set"));
   private static final String PROPERTY_ARRAY_INDEX = "propertyArrayIndex";
   static final String STATUS_TAG = "status";
   static final int STATUS_TAG_LEN = 7;
   static final String INDEX_TAG = "index=";
   static final int INDEX_TAG_LEN = 6;
   private static final int INVALID_MASK = 22;
   private PollListEntry[] ples = null;
   private int oldStatus = 16;
   private int flags = 2;
   private String readFault = null;
   private String writeFault = null;
   private boolean pollSubscribed = false;
   private boolean resolvingAddress = false;
   private ArrayList<BComponent> kidSubs;
   private static AsnInputStream asnIn = new AsnInputStream();
   protected int DEFAULT_ARRAY_INDEX = 1;
   private Object SUBSCRIPTION_LOCK = new Object();

   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public boolean getUseFacets() {
      return this.getBoolean(useFacets);
   }

   public void setUseFacets(boolean v) {
      this.setBoolean(useFacets, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetVirtualProperty() {
   }

   public BBacnetVirtualProperty(int propertyId, BValue v, String readFault, boolean useFacets) {
      this.setPropertyId(propertyId);
      this.readFault = readFault;
      this.setUseFacets(useFacets);
      this.setValue(v);
   }

   public void started() throws Exception {
      super.started();
      this.kidSubs = new ArrayList<>();
      this.updateStatus();
      if (this.getPropertyId() == 85) {
         if (this.object().getPrioritizedPoint()) {
            this.add(WRITE_ACTION_NAME, new BVirtualPropertyWrite());
         } else {
            this.add(WRITE_ACTION_NAME, new BVirtualPropertyWrite());
         }
      } else {
         this.add(WRITE_ACTION_NAME, new BVirtualPropertyWrite());
      }

      this.setArrayIndex();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (cx != noWrite) {
         if (this.isRunning()) {
            if (!p.equals(status)) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("changed(" + p + ", " + cx + ") on " + this.getName() + " in " + this.object());
               }

               if (p.getName().equals("value")) {
                  byte[] encodedValue = null;
                  int index = -1;
                  int priority = -1;
                  if (cx instanceof GrandchildChangedContext) {
                     GrandchildChangedContext gccCx = (GrandchildChangedContext)cx;
                     index = gccCx.getArrayIndex();
                     encodedValue = gccCx.getEncodedValue();
                  } else {
                     encodedValue = this.encodeValue(this.getValue());
                     if (this.usePriority()) {
                        priority = this.object().getWritePriority();
                     }
                  }

                  this.write(index, encodedValue, priority);
               }
            }
         }
      }
   }

   public void subscribed() {
      this.pollSubscribe();
   }

   public void unsubscribed() {
      this.stale(true);
      if (this.kidSubs.size() == 0) {
         this.pollUnsubscribe();
      }

      this.ples = null;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetVirtualObject;
   }

   public boolean isChildLegal(BComponent child) {
      return true;
   }

   public String toString(Context cx) {
      this.loadSlots();
      StringBuilder sb = new StringBuilder();
      if (this.getStatus().isNull()) {
         sb.append('-');
      } else {
         sb.append(this.valueToString(cx));
      }

      sb.append(' ').append(this.getStatus().toString(cx));
      return sb.toString();
   }

   public String debugString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getName()).append('=').append(this.getValue()).append(':').append(this.getStatus());
      if (this.getValue() instanceof BBacnetArray) {
         BBacnetArray a = (BBacnetArray)this.getValue();
         int len = a.getSize();
         if (len < 0) {
            sb.append(" size UNKNOWN");
         }

         for (int i = 0; i < len; i++) {
            sb.append(" [").append(i).append("]=").append(a.getElement(i));
         }
      }

      return sb.toString();
   }

   public BFacets getSlotFacets(Slot s) {
      return s.getName().equals("value") && this.getUseFacets() && this.object() != null ? this.object().getFacets() : s.getFacets();
   }

   public BValue getValue() {
      return this.get("value");
   }

   public void setValue(BValue v) {
      this.setValue(v, null);
   }

   public void setValue(BValue v, Context cx) {
      Property prop = this.getProperty("value");
      if (prop == null) {
         this.add("value", v, 2, cx);
      } else {
         this.set(prop, v, cx);
      }
   }

   protected String valueToString(Context cx) {
      BValue v = this.getValue();
      return v != null && this.object() != null ? v.toString((Context)(this.getUseFacets() ? this.object().getFacets() : cx)) : "null";
   }

   protected int getArrayIndex() {
      Property p = this.getProperty("propertyArrayIndex");
      return p != null ? this.getInt(p) : this.DEFAULT_ARRAY_INDEX;
   }

   private void setArrayIndex() {
      String name = SlotPath.unescape(this.getName());
      int scndx = name.indexOf(";");
      if (scndx > 0) {
         StringTokenizer st = new StringTokenizer(name, ";");
         st.nextToken();

         while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.startsWith("index=")) {
               token = token.substring(6);
               this.add("propertyArrayIndex", BInteger.make(Integer.parseInt(token)));
               break;
            }
         }
      }
   }

   public BBacnetVirtualObject object() {
      return (BBacnetVirtualObject)this.getParent();
   }

   public void readOk(BValue v) {
      this.setValue(v, noWrite);
      this.stale(false);
      this.readFault = null;
      this.writeFault = null;
      this.updateStatus();
   }

   public void writeOk() {
      this.writeFault = null;
      this.updateStatus();
   }

   public void writeFail(String failureMsg) {
      this.writeFault = failureMsg;
      this.updateStatus();
   }

   public void updateStatus() {
      this.updateStatus(0, null);
      this.checkSubscription();
   }

   public void updateStatus(int metaBits, BFacets metaFacets) {
      int newStatus = this.getStatus().getBits();
      BStatus ds = this.getDeviceStatus();
      if (ds.isDisabled()) {
         newStatus |= 1;
      } else {
         newStatus &= -2;
      }

      if (ds.isDown()) {
         newStatus |= 4;
      } else {
         newStatus &= -5;
      }

      if (!this.fatalFault() && this.readFault == null && this.writeFault == null && !ds.isFault()) {
         newStatus &= -3;
      } else {
         newStatus |= 2;
      }

      if (this.stale()) {
         newStatus |= 16;
      } else {
         newStatus &= -17;
      }

      if (metaFacets != null) {
         if ((metaBits & 8) != 0) {
            newStatus |= 8;
         } else {
            newStatus &= -9;
         }
      }

      newStatus |= metaBits;
      if (this.oldStatus != newStatus || metaFacets != null) {
         BFacets f = BFacets.NULL;
         if ((newStatus & 22) == 0) {
            f = BFacets.make(this.getStatus().getFacets(), metaFacets);
         }

         this.setStatus(BStatus.make(newStatus, f));
         this.oldStatus = newStatus;
      }
   }

   public void childSubscribed(BComponent kid) {
      this.kidSubs.add(kid);
      if (this.kidSubs.size() == 1) {
         this.pollSubscribe();
      }
   }

   public void childUnsubscribed(BComponent kid) {
      this.kidSubs.remove(kid);
      if (this.kidSubs.size() == 0 && !this.isSubscribed()) {
         this.pollUnsubscribe();
      }
   }

   protected boolean usePriority() {
      return this.getPropertyId() == 85;
   }

   protected boolean auditWrites() {
      return ((BBacnetVirtualGateway)this.getVirtualGateway()).getAuditWrites();
   }

   protected String getAuditName() {
      return this.device().getName() + "/" + this.object().getName() + ":" + this.getName();
   }

   public String getReadFault() {
      return this.readFault;
   }

   public String getWriteFault() {
      return this.writeFault;
   }

   public BPollFrequency getPollFrequency() {
      BBacnetVirtualObject o = this.object();
      if (o != null) {
         BBacnetTuningPolicy policy = o.getPolicy();
         if (policy != null) {
            return policy.getPollFrequency();
         }
      }

      return BPollFrequency.normal;
   }

   @Override
   public BBacnetDevice device() {
      return this.getVirtualGateway() == null ? null : ((BBacnetVirtualGateway)this.getVirtualGateway()).device();
   }

   @Override
   public int getPollableType() {
      return 3;
   }

   @Deprecated
   @Override
   public boolean poll() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void readFail(String failureMsg) {
      this.readFault = failureMsg;
      this.oldStatus = -1;
      this.updateStatus();
   }

   @Override
   public void fromEncodedValue(byte[] encodedValue, BStatus status, Context cx) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("fromEncodedValue() on " + this.object() + "." + this.getName() + ": ev=" + ByteArrayUtil.toHexString(encodedValue));
      }

      try {
         Context baseCx = cx.getBase();
         if (baseCx instanceof MetaDataContext) {
            this.readMetaData(encodedValue, cx, (MetaDataContext)baseCx);
            return;
         }

         synchronized (asnIn) {
            asnIn.setBuffer(encodedValue);
            BValue v = BacnetVirtualUtil.readValue(asnIn, this.getValue());
            this.readOk(v);
         }
      } catch (Exception var9) {
         if (log.isLoggable(Level.FINE)) {
            log.log(
               Level.FINE, "Exception decoding value for " + this.getName() + ":" + var9 + " ev=" + ByteArrayUtil.toHexString(encodedValue), (Throwable)var9
            );
         }

         this.readFail(var9.toString());
      }
   }

   @Override
   public PollListEntry[] getPollListEntries() {
      String name = SlotPath.unescape(this.getName());
      if (this.ples == null) {
         Array<PollListEntry> a = new Array(PollListEntry.class);
         int ndx = -1;
         int scndx = name.indexOf(";");
         if (scndx > 0) {
            StringTokenizer st = new StringTokenizer(name, ";");
            st.nextToken();

            while (st.hasMoreTokens()) {
               String token = st.nextToken();
               if (token.startsWith("status")) {
                  BBacnetDeviceObjectPropertyReference dopr = this.createObjectPropRef(token);
                  if (dopr != null) {
                     BBacnetObjectIdentifier deviceId = dopr.getDeviceId();
                     if (dopr.isDeviceIdUsed() && deviceId.hashCode() != this.device().getObjectId().hashCode()) {
                        BBacnetDevice dev = this.network().doLookupDeviceById(deviceId);
                        if (dev == null) {
                           throw new IllegalStateException("Cannot find BACnet device for virtual component metadata:" + this + "  ref=" + dopr);
                        }

                        a.add(this.createPLE(dopr, dev, token));
                     } else {
                        a.add(this.createPLE(dopr, this.device(), token));
                     }
                  }
               } else if (token.startsWith("index=")) {
                  String tokenValue = token.substring(6);

                  try {
                     ndx = Integer.parseInt(tokenValue);
                     BValue v = null;
                     if ((v = this.getValue()) instanceof BBacnetArray) {
                        BBacnetArray ba = (BBacnetArray)v;
                        if (ndx == 0) {
                           this.setValue(BInteger.make(-1), noWrite);
                        } else {
                           this.setValue((BValue)ba.getArrayTypeSpec().getResolvedType().getInstance(), noWrite);
                        }
                     }
                  } catch (NumberFormatException var10) {
                     if (log.isLoggable(Level.WARNING)) {
                        log.log(Level.WARNING, "NumberFormatException for VirtualBacnetProperty Index: " + tokenValue, (Throwable)var10);
                     }
                  }
               }
            }
         }

         a.add(new PollListEntry(this.object().getObjectId(), this.getPropertyId(), ndx, this.device(), this));
         this.ples = (PollListEntry[])a.trim();
      }

      return this.ples;
   }

   private PollListEntry createPLE(BBacnetDeviceObjectPropertyReference dopr, BBacnetDevice device, String token) {
      return new PollListEntry(dopr.getObjectId(), dopr.getPropertyId(), dopr.getPropertyArrayIndex(), device, this, new MetaDataContext(token));
   }

   private BBacnetDeviceObjectPropertyReference createObjectPropRef(String token) {
      BBacnetDeviceObjectPropertyReference dopr = null;
      if (token != null && token.length() > 7) {
         token = token.substring(7);
         dopr = BBacnetDeviceObjectPropertyReference.fromString(token);
      } else {
         dopr = new BBacnetDeviceObjectPropertyReference();
         dopr.setObjectId(this.object().getObjectId());
         dopr.setPropertyId(111);
      }

      return dopr;
   }

   void write(int propertyArrayIndex, byte[] encodedValue, int priority) {
      this.network().postWrite(new BBacnetVirtualProperty.Write(this.getPropertyId(), propertyArrayIndex, encodedValue, priority));
   }

   void write(int propertyArrayIndex, BValue v, int priority) {
      byte[] encodedValue = this.encodeValue(v);
      this.write(propertyArrayIndex, encodedValue, priority);
   }

   protected BStatus getDeviceStatus() {
      return this.device() == null ? BStatus.nullStatus : this.device().getStatus();
   }

   protected BBacnetNetwork network() {
      return ((BBacnetVirtualGateway)this.getVirtualGateway()).network();
   }

   private void readMetaData(byte[] encodedValue, Context pleCx, MetaDataContext metaCx) throws AsnException {
      PollListEntry ple = (PollListEntry)pleCx;
      BBacnetObjectIdentifier objectId = ple.getObjectId();
      String key = "";
      if (!this.object().getObjectId().equals(objectId)) {
         key = objectId.toShortString() + "_";
      }

      int propId = ple.getPropertyId();
      key = key + BBacnetPropertyIdentifier.tag(propId);
      PropertyInfo pi = this.device().getPropertyInfo(ple.getObjectId().getObjectType(), propId);
      BValue metaData = AsnUtil.asnToValue(pi, encodedValue);
      if (propId == 111) {
         int ibits = 0;
         BBacnetBitString bs = (BBacnetBitString)metaData;
         boolean[] bits = bs.getBits();
         if (bits[0]) {
            ibits |= 8;
         }

         if (bits[1]) {
            ibits |= 2;
         }

         if (bits[2]) {
            ibits |= 32;
         }

         if (bits[3]) {
            ibits |= 1;
         }

         this.updateStatus(ibits, BFacets.make(key, BString.make(metaData.toString())));
      } else if (propId == 36) {
         BEnum e = (BEnum)metaData;
         int ibitsx = 0;
         if (BBacnetEventState.isFault(e)) {
            ibitsx |= 2;
         } else if (BBacnetEventState.isOffnormal(e)) {
            ibitsx |= 8;
         }

         this.updateStatus(ibitsx, BFacets.make(key, BString.make(metaData.toString())));
      } else if (propId == 103) {
         BEnum e = (BEnum)metaData;
         int ibitsx = 0;
         if (e.getOrdinal() != 0) {
            ibitsx |= 2;
         }

         this.updateStatus(ibitsx, BFacets.make(key, BString.make(metaData.toString())));
      } else if (propId == 81) {
         boolean oos = ((BBoolean)metaData).getBoolean();
         if (oos) {
            this.updateStatus(1, BFacets.make("outOfService", BBoolean.TRUE));
         } else {
            this.updateStatus(0, BFacets.make(key, BBoolean.FALSE));
         }
      } else {
         this.updateStatus(0, BFacets.make(key, BString.make(metaData.toString())));
      }
   }

   protected void pollSubscribe() {
      synchronized (this.SUBSCRIPTION_LOCK) {
         if (!this.pollSubscribed) {
            if (this.device().isAddressValid()) {
               this.resolvingAddress = false;
               BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
               pollService.subscribe(this);
               this.pollSubscribed = true;
            } else {
               this.resolvingAddress = true;
               this.device().checkAddress();
            }
         }
      }
   }

   protected void pollUnsubscribe() {
      synchronized (this.SUBSCRIPTION_LOCK) {
         if (this.pollSubscribed) {
            BBacnetPoll pollService = (BBacnetPoll)this.network().getPollService(this);
            pollService.unsubscribe(this);
            this.pollSubscribed = false;
         }

         this.resolvingAddress = false;
      }
   }

   protected void checkSubscription() {
      synchronized (this.SUBSCRIPTION_LOCK) {
         if (this.resolvingAddress && this.device().isAddressValid()) {
            this.pollSubscribe();
         }
      }
   }

   byte[] encodeValue(BValue v) {
      byte[] ev = null;
      PropertyInfo pi = this.device().getPropertyInfo(this.object().getObjectId().getObjectType(), this.getPropertyId());
      if (pi != null) {
         ev = AsnUtil.toAsn(pi.getAsnType(), v);
      } else {
         ev = AsnUtil.toAsn(v);
      }

      return ev;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetVirtualProperty", 2);
      out.prop("oldStatus", this.oldStatus);
      out.prop("flags", this.flags);
      out.prop("readFault", this.readFault);
      out.prop("writeFault", this.writeFault);
      if (this.ples != null) {
         out.prop("PollListEntries", this.ples.length);

         for (int i = 0; i < this.ples.length; i++) {
            out.prop("  " + i, this.ples[i]);
         }
      }

      out.prop("pollSubscribed", this.pollSubscribed);
      if (this.kidSubs != null) {
         out.prop("kidSubs", this.kidSubs.size());
         int cnt = 0;
         Iterator<BComponent> i = this.kidSubs.iterator();

         while (i.hasNext()) {
            out.prop("  " + cnt++, i.next());
         }
      }

      out.endProps();
   }

   private boolean fatalFault() {
      return (this.flags & 1) != 0;
   }

   private boolean stale() {
      return (this.flags & 2) != 0;
   }

   private void stale(boolean b) {
      if (b) {
         this.flags |= 2;
      } else {
         this.flags &= -3;
      }
   }

   class Write implements Runnable {
      int propId;
      int propertyArrayIndex = -1;
      byte[] encodedValue = null;
      int priority = -1;

      Write(int propId, int propertyArrayIndex, byte[] ev, int pri) {
         if (propId == 87) {
            this.propId = 85;
            this.priority = propertyArrayIndex;
         } else {
            this.propId = propId;
            this.propertyArrayIndex = propertyArrayIndex;
            this.priority = pri;
         }

         this.encodedValue = ev;
      }

      @Override
      public void run() {
         try {
            BBacnetVirtualProperty.this.network()
               .getBacnetComm()
               .writeProperty(
                  BBacnetVirtualProperty.this.device().getAddress(),
                  BBacnetVirtualProperty.this.object().getObjectId(),
                  this.propId,
                  this.propertyArrayIndex,
                  this.encodedValue,
                  this.priority
               );
            BBacnetVirtualProperty.this.writeOk();
         } catch (TransactionException var2) {
            BBacnetVirtualProperty.this.device().ping();
            BBacnetVirtualProperty.log.warning("TransactionException writing " + BBacnetPropertyIdentifier.tag(this.propId) + " in " + this);
            BBacnetVirtualProperty.this.writeFail(var2.toString());
         } catch (BacnetException var3) {
            BBacnetVirtualProperty.log
               .log(Level.SEVERE, "BacnetException writing " + BBacnetPropertyIdentifier.tag(this.propId) + " in " + this, (Throwable)var3);
            BBacnetVirtualProperty.this.writeFail(var3.toString());
         }
      }
   }
}
