package javax.baja.lonworks;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.util.NmUtil;
import java.util.logging.Level;
import javax.baja.driver.point.BITunable;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.point.Tuning;
import javax.baja.driver.util.BIPollable;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.lonworks.enums.BLonServiceType;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.tuning.BLonTuningPolicy;
import javax.baja.lonworks.util.SnvtUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nvProps",
      type = "BNvProps",
      defaultValue = "new BNvProps()"
   ), @NiagaraProperty(
      name = "nvConfigData",
      type = "BNvConfigData",
      defaultValue = "new BNvConfigData()"
   ), @NiagaraProperty(
      name = "tuningPolicyName",
      type = "String",
      defaultValue = "defaultPolicy",
      facets = {@Facet("TUNING_POLICY_NAME_FACETS")}
   )})
@NiagaraAction(
   name = "nvUpdate",
   flags = 4
)
@NiagaraTopic(
   name = "receivedUpdate"
)
public class BNetworkVariable extends BLonComponent implements BINetworkVariable, BITunable, BIPollable {
   public static final Property nvProps = newProperty(0, new BNvProps(), null);
   public static final Property nvConfigData = newProperty(0, new BNvConfigData(), null);
   public static final Property tuningPolicyName = newProperty(0, "defaultPolicy", TUNING_POLICY_NAME_FACETS);
   public static final Action nvUpdate = newAction(4, null);
   public static final Topic receivedUpdate = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BNetworkVariable.class);
   private boolean registered = false;
   private boolean pollSubscribe = false;
   private int knobCount = 0;
   boolean linked = false;
   boolean pseudoLink = false;
   boolean remoteLink = false;
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/nv.png");
   Tuning tuning = new Tuning(this);
   Object wipSync = null;
   Array<BLonLink> linksToPropagate = null;
   boolean writeInProgress = false;

   public BNvProps getNvProps() {
      return (BNvProps)this.get(nvProps);
   }

   public void setNvProps(BNvProps v) {
      this.set(nvProps, v, null);
   }

   @Override
   public BNvConfigData getNvConfigData() {
      return (BNvConfigData)this.get(nvConfigData);
   }

   @Override
   public void setNvConfigData(BNvConfigData v) {
      this.set(nvConfigData, v, null);
   }

   public String getTuningPolicyName() {
      return this.getString(tuningPolicyName);
   }

   public void setTuningPolicyName(String v) {
      this.setString(tuningPolicyName, v, null);
   }

   public void nvUpdate() {
      this.invoke(nvUpdate, null, null);
   }

   public void fireReceivedUpdate(BValue event) {
      this.fire(receivedUpdate, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BNetworkVariable() {
   }

   public BNetworkVariable(int nvIndex, int snvtType, int objectIndex, int memberIndex, int flags, BLonNvDirection direction) {
      BNvProps nvProps = this.getNvProps();
      nvProps.setNvIndex(nvIndex);
      nvProps.setSnvtType(snvtType);
      nvProps.setObjectIndex(objectIndex);
      nvProps.setMemberIndex(memberIndex);
      BNvConfigData nvCfg = this.getNvConfigData();
      nvCfg.setDirection(direction);
      setFlags(nvProps, nvCfg, flags);
      BLonData ld = SnvtUtil.getLonData(snvtType);
      DynaDev.setNonCritical(ld);
      this.setData(ld);
   }

   public BNetworkVariable(int nvIndex, BLonData data, int objectIndex, int memberIndex, int flags, BLonNvDirection direction) {
      BNvProps nvProps = this.getNvProps();
      nvProps.setNvIndex(nvIndex);
      nvProps.setSnvtType(-1);
      nvProps.setObjectIndex(objectIndex);
      nvProps.setMemberIndex(memberIndex);
      BNvConfigData nvCfg = this.getNvConfigData();
      nvCfg.setDirection(direction);
      setFlags(nvProps, nvCfg, flags);
      this.setData(data);
   }

   public static void setFlags(BNvProps nvProps, BNvConfigData nvCfg, int flags) {
      if ((flags & 1) > 0) {
         nvProps.setPolled(true);
      }

      if ((flags & 2) > 0) {
         nvProps.setAuthConf(false);
      }

      if ((flags & 4) > 0) {
         nvProps.setServiceConf(false);
      }

      if ((flags & 8) > 0) {
         nvProps.setPriorityConf(true);
      }

      if ((flags & 16) > 0) {
         nvCfg.setAuthenticated(true);
      }

      if ((flags & 32) > 0) {
         nvCfg.setPriority(true);
      }

      if ((flags & 64) > 0) {
         nvCfg.setServiceType(BLonServiceType.acked);
      }

      if ((flags & 128) > 0) {
         nvCfg.setServiceType(BLonServiceType.unackedRpt);
      }
   }

   @Override
   public boolean isNetworkVariable() {
      return true;
   }

   @Override
   public int getNvIndex() {
      return this.getNvProps().getNvIndex();
   }

   @Override
   public void setNvIndex(int nvIndex) {
      this.getNvProps().setNvIndex(nvIndex);
   }

   @Override
   public int getSnvtType() {
      return this.getNvProps().getSnvtType();
   }

   @Override
   public void setUnbound() {
      int sel = this.getNvConfigData().getSelector();
      this.getNvConfigData().setUnbound(this.getNvProps().getNvIndex());
      this.getNvProps().setUnbound();
      this.unbound(sel);
   }

   public void unbound(int sel) {
      if (this.getNvConfigData().isOutput()) {
         this.unregisterSelector(sel);
      }

      this.evaluatePollSubscribe();

      try {
         this.getDevice().unbound(this.getNvProps().getNvIndex());
      } catch (Throwable var3) {
      }
   }

   public void bound() {
      this.evaluatePollSubscribe();
      if (this.getNvConfigData().isOutput() && this.getNvProps().getBoundToLocal()) {
         this.registerSelector();
      }

      try {
         this.getDevice().bound(this.getNvProps().getNvIndex());
      } catch (Throwable var2) {
      }
   }

   @Override
   public void lonComponentStarted() {
      if (this.getNvConfigData().getSelector() == -1) {
         this.getNvConfigData().setUnbound(this.getNvProps().getNvIndex());
      }

      this.getTuning().transition();
      if (this.getNvConfigData().isOutput() && this.getNvProps().getBoundToLocal()) {
         this.registerSelector();
      }

      this.evaluateLinkedState();
      if (Sys.atSteadyState()) {
         this.evaluatePollSubscribe();
      }
   }

   @Override
   public void lonComponentStopped() {
      this.getTuning().transition();
      if (this.getNvConfigData().isOutput() && this.getNvProps().getBoundToLocal()) {
         this.unregisterSelector(this.getNvConfigData().getSelector());
      }

      this.lonNetwork().getPollService().unsubscribe(this);
      this.linksToPropagate = null;
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      if (this.getNvConfigData().isInput()) {
         this.getNvProps().setBoundToLocal(false);
      }

      if (this.getNvConfigData().isOutput()) {
         this.propagateLinks();
      }

      this.getTuning().transition();
      this.evaluatePollSubscribe();
   }

   public boolean requiresPrioritySlot() {
      BNvConfigData nvCfg = this.getNvConfigData();
      return nvCfg.getPriority() && !nvCfg.isInput() && nvCfg.isBoundNv();
   }

   private void registerSelector() {
      if (!this.registered) {
         this.lonNetwork().nvManager().registerSelector(this.getNvConfigData().getSelector(), this, this.getDevice());
         this.registered = true;
      }
   }

   private void unregisterSelector(int sel) {
      if (this.registered) {
         this.lonNetwork().nvManager().unregisterSelector(sel, this, this.getDevice());
         this.registered = false;
      }
   }

   void reregisterSelector() {
      if (this.registered) {
         int sel = this.getNvConfigData().getSelector();
         this.lonNetwork().nvManager().unregisterSelector(sel, this, this.getDevice());
         this.lonNetwork().nvManager().registerSelector(sel, this, this.getDevice());
      }
   }

   public BPollFrequency getPollFrequency() {
      return ((BLonTuningPolicy)this.getTuning().getPolicy()).getPollFrequency();
   }

   public int getWriteDelay() {
      return ((BLonTuningPolicy)this.getTuning().getPolicy()).getWriteDelay();
   }

   public void pollNv() {
      if (this.getDevice().isReadyForNvUpdates()) {
         if (this.writeInProgress()) {
            return;
         }

         this.doForceRead();
      }
   }

   private void evaluatePollSubscribe() {
      if (this.isRunning()) {
         boolean allLinksBound = this.evaluateBound();
         boolean isBoundToLocal = this.getNvProps().getBoundToLocal();
         boolean enablePoll = !isBoundToLocal && this.knobCount > 0 && !allLinksBound || isBoundToLocal && this.getNvProps().getPolled();
         boolean sub = (enablePoll || !isBoundToLocal && this.subscribeCount > 0) && this.getNvProps().getPollEnable();
         if (sub && !this.pollSubscribe) {
            this.lonNetwork().getPollService().subscribe(this);
         } else if (!sub && this.pollSubscribe) {
            this.lonNetwork().getPollService().unsubscribe(this);
         }

         this.pollSubscribe = sub;
      }
   }

   @Override
   protected void lonComponentSubscribed() {
      if (this.isRunning() && this.getDevice().isReadyForNvUpdates()) {
         this.forceRead();
      }

      this.evaluatePollSubscribe();
   }

   @Override
   protected void lonComponentUnsubscribed() {
      this.evaluatePollSubscribe();
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (prop == nvProps) {
         this.evaluatePollSubscribe();
      }
   }

   @Override
   protected void dataChanged(Context cx) {
      if (this.isRunning()) {
         if (cx == null || !BLonNetwork.lonNoPropagate.equals(cx)) {
            this.propagateLinks();
         }

         if (cx == null || !BLonNetwork.lonNoWrite.equals(cx)) {
            if (this.wipSync == null) {
               this.wipSync = new Object();
            }

            synchronized (this.wipSync) {
               this.writeInProgress = true;
            }

            this.getTuning().writeDesired();
         }
      }
   }

   private boolean writeInProgress() {
      if (this.wipSync == null) {
         return false;
      } else {
         synchronized (this.wipSync) {
            return this.writeInProgress;
         }
      }
   }

   protected void propagateLinks() {
      Array<BLonLink> a = this.getLinksToPropagate(false);
      if (a != null) {
         synchronized (a) {
            BLonLink[] lks = (BLonLink[])a.trim();

            for (int i = 0; i < lks.length; i++) {
               lks[i].propagateNv(this);
            }
         }
      }
   }

   @Override
   public void doForceRead() {
      this.lonDevice().checkState();
      if (this.illegalLength) {
         throw new BajaRuntimeException(this.getDisplayName(null) + " data length > maxNvLength of " + Lon.maxNvLength() + " bytes");
      } else {
         try {
            if (Lon.d()) {
               byte[] nvData = NmUtil.fetchNv(this.lonDevice(), this.getNvIndex());
               this.getData().fromNetBytes(nvData);
            }

            this.getData().readOk();
            this.getTuning().readOk();
         } catch (Throwable var3) {
            this.getData().readFail(var3.toString());
            this.getTuning().readFail();
            String errMsg = "Unable to read " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var3);
            throw new BajaRuntimeException(errMsg, var3);
         }
      }
   }

   public void doNvUpdate() {
      try {
         BNvConfigData configData = this.getNvConfigData();
         if (configData.isOutput() && configData.isBoundNv()) {
            byte[] a = NmUtil.pollNv(this.lonDevice(), configData);
            this.receiveUpdate(a);
         }
      } catch (Exception var3) {
      }
   }

   @Override
   public void doForceWrite() {
      try {
         this.lonDevice().checkState();
         if (this.illegalLength) {
            throw new BajaRuntimeException(this.getDisplayName(null) + " data length > maxNvLength of " + Lon.maxNvLength() + " bytes");
         }

         BNvConfigData configData = this.getNvConfigData();

         try {
            if (configData.isInput()) {
               if (Lon.d()) {
                  NmUtil.setNvValue(this.lonDevice(), configData, this.getData().toNetBytes());
               }

               this.getData().writeOk();
               this.getTuning().writeOk();
            }
         } catch (Throwable var13) {
            this.getData().writeFail(var13.toString());
            this.getTuning().writeFail();
            String errMsg = "Unable to write " + this.debugName();
            this.lonNetwork().log().log(Level.SEVERE, errMsg, var13);
            throw new BajaRuntimeException(errMsg, var13);
         }
      } finally {
         if (this.wipSync != null) {
            synchronized (this.wipSync) {
               this.writeInProgress = false;
            }
         }
      }
   }

   @Override
   public boolean isWriteable() {
      return this.getNvConfigData().isInput();
   }

   @Override
   public void receiveUpdate(byte[] nvData) {
      try {
         this.getData().fromNetBytes(nvData);
         this.getData().readOk();
         this.getTuning().readOk();
         this.fireReceivedUpdate(null);
      } catch (Throwable var4) {
         String err = var4.toString();
         this.getData().readFail(err);
         this.getTuning().readFail();
         this.lonNetwork().log().log(Level.SEVERE, "Could not decode nv update data " + this.debugName(), var4);
      }
   }

   public void lonKnobAdded(Knob knob) {
      this.knobCount++;
      Array<BLonLink> a = this.getLinksToPropagate(true);
      synchronized (a) {
         a.add((BLonLink)knob.getLink());
      }

      this.evaluatePollSubscribe();
      if (this.getDevice().isRunning()) {
         this.pollNv();
      }
   }

   public void lonKnobRemove(Knob knob) {
      this.knobCount--;
      Array<BLonLink> a = this.getLinksToPropagate(false);
      if (a != null) {
         synchronized (a) {
            a.remove((BLonLink)knob.getLink());
         }
      }

      this.evaluatePollSubscribe();
   }

   public void lonLinkAdded() {
      this.evaluateLinkedState();
   }

   public void lonLinkRemoved() {
      this.evaluateLinkedState();
   }

   private boolean evaluateBound() {
      BNvConfigData nvCfg = this.getNvConfigData();
      if (nvCfg.isInput()) {
         return nvCfg.isBoundNv();
      } else {
         BComponent p = (BComponent)this.getParent();
         Array<BLonLink> a = this.getLinksToPropagate(false);
         if (p != null && a != null) {
            BLonLink[] lks = (BLonLink[])a.trim();

            for (int i = 0; i < lks.length; i++) {
               if (!lks[i].isBound(this)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   protected void evaluateLinkedState() {
      this.linked = false;
      this.pseudoLink = false;
      this.remoteLink = false;
      BComponent p = (BComponent)this.getParent();
      if (p != null) {
         BLink[] lks = p.getLinks(this.getPropertyInParent());

         for (int i = 0; i < lks.length; i++) {
            if (lks[i] instanceof BLonLink) {
               this.linked = true;
               if (((BLonLink)lks[i]).getPseudoLink()) {
                  this.pseudoLink = true;
               }

               if (((BLonLink)lks[i]).getRemoteLink()) {
                  this.remoteLink = true;
               }
            }
         }
      }
   }

   protected boolean isLinked() {
      return this.linked;
   }

   public Tuning getTuning() {
      return this.tuning;
   }

   public boolean isFatalFault() {
      return this.getDevice().isFatalFault();
   }

   public BStatus getStatus() {
      return this.getDevice().getStatus();
   }

   public boolean isSubscribedDesired() {
      return false;
   }

   public BReadWriteMode getMode() {
      return this.isWriteable() ? BReadWriteMode.writeonly : BReadWriteMode.readonly;
   }

   public void readSubscribed(Context cx) {
      super.readSubscribed();
   }

   public void readUnsubscribed(Context cx) {
      super.readUnsubscribed();
   }

   public boolean write(Context cx) {
      boolean stateWrite = cx == BTuningPolicy.maxWriteTimeContext
         || cx == BTuningPolicy.writeOnStartContext
         || cx == BTuningPolicy.writeOnUpContext
         || cx == BTuningPolicy.writeOnEnabledContext;
      if (stateWrite && (!this.linked || this.getNvConfigData().isBoundNv()) && !this.getData().hasWriteProxies() && !this.pseudoLink && !this.remoteLink) {
         this.getTuning().writeOk();
         return false;
      } else {
         if (cx == BTuningPolicy.maxWriteTimeContext && this.getData().hasWriteProxies()) {
            this.getData().forceProxyUpdates();
         }

         if (DeviceFacets.delayNvUpdate(this.lonDevice(), this)) {
            return false;
         } else {
            this.forceWrite();
            return false;
         }
      }
   }

   public void setStale(boolean s, Context cx) {
      if (this.getData().hasProxies()) {
         this.getData().markStale(s, cx);
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BNetworkVariable", 2);
      out.prop("linked", this.linked);
      out.prop("knobCount(nvo)", this.knobCount);
      out.prop("allLinksBound", this.evaluateBound());
      out.prop("isBoundToLocal", this.getNvProps().getBoundToLocal());
      out.prop("hasProxies", this.getData().hasProxies());
      out.prop("hasWriteProxies", this.getData().hasWriteProxies());
      out.prop("pseudoLink", this.pseudoLink);
      out.prop("remoteLink", this.remoteLink);
      out.prop("getMode", this.getMode());
      out.prop("subscribeCount", this.subscribeCount);
      out.prop("selector registered", this.registered);
      out.prop("pollSubscribe", this.pollSubscribe);
      out.prop("writeInProgress", this.writeInProgress);
      out.prop("illegalLength", this.illegalLength);
      out.endProps();
   }

   protected Array<BLonLink> getLinksToPropagate(boolean create) {
      if (create && this.linksToPropagate == null) {
         this.linksToPropagate = new Array(BLonLink.class);
      }

      return this.linksToPropagate;
   }

   public BIcon getIcon() {
      return icon;
   }
}
