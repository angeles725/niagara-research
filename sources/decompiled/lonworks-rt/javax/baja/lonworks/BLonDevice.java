package javax.baja.lonworks;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.device.BDownloadJob;
import com.tridium.lonworks.device.BUploadJob;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.device.NvDev;
import com.tridium.lonworks.local.BPseudoNvContainer;
import com.tridium.lonworks.netmgmt.BChangeNvTypeAction;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.control.BControlPoint;
import javax.baja.driver.BDevice;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.datatypes.BAliasTable;
import javax.baja.lonworks.datatypes.BConfigProps;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.enums.BLonObjectRequestEnum;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.proxy.BLonPointDeviceExt;
import javax.baja.lonworks.proxy.BLonPointFolder;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.lonworks.util.LonFile;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceData",
      type = "BDeviceData",
      defaultValue = "new BDeviceData()",
      flags = 8
   ), @NiagaraProperty(
      name = "points",
      type = "BLonPointDeviceExt",
      defaultValue = "new BLonPointDeviceExt()"
   ), @NiagaraProperty(
      name = "messageIn",
      type = "BMessageTag",
      defaultValue = "new BMessageTag()",
      flags = 1024
   )})
@NiagaraActions({@NiagaraAction(
      name = "upload",
      parameterType = "BUploadParameters",
      defaultValue = "new BUploadParameters()"
   ), @NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()"
   ), @NiagaraAction(
      name = "reset"
   ), @NiagaraAction(
      name = "renewProxies",
      flags = 4
   ), @NiagaraAction(
      name = "initImport",
      flags = 4
   ), @NiagaraAction(
      name = "closeImport",
      flags = 4
   )})
public class BLonDevice extends BDevice implements BINvContainer, BILonLoadable {
   public static final Property deviceData = newProperty(8, new BDeviceData(), null);
   public static final Property points = newProperty(0, new BLonPointDeviceExt(), null);
   public static final Property messageIn = newProperty(1024, new BMessageTag(), null);
   public static final Action upload = newAction(0, new BUploadParameters(), null);
   public static final Action download = newAction(0, new BDownloadParameters(), null);
   public static final Action reset = newAction(0, null);
   public static final Action renewProxies = newAction(4, null);
   public static final Action initImport = newAction(4, null);
   public static final Action closeImport = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BLonDevice.class);
   NvDev.SaveNv snv = null;
   BLink[] toLnks = null;
   Object nvSync = new Object();
   BINetworkVariable[] nvList;
   protected boolean staleNvList = true;
   boolean linkUpdateDone = false;
   private int fileState = 0;
   private boolean hasReadOnly = false;
   private LonFile lonFile = null;
   private LonFile readWritefile = null;
   private LonFile readOnlyfile = null;
   private static final int FILE_STATE_INIT = 0;
   private static final int FILE_STATE_NO_FILES = 1;
   private static final int FILE_STATE_DOWNLOAD = 2;
   private static final int FILE_STATE_UPLOAD = 3;
   private static final int FILE_STATE_IDLE = 4;
   private int maxInMessageSize = 0;
   private int maxOutMessageSize = 0;
   public int dataPntMismatchCount = 0;
   protected Logger log = null;
   private BLonNetwork net = null;
   protected boolean downloading = false;
   protected boolean uploading = false;

   @Override
   public BDeviceData getDeviceData() {
      return (BDeviceData)this.get(deviceData);
   }

   public void setDeviceData(BDeviceData v) {
      this.set(deviceData, v, null);
   }

   public BLonPointDeviceExt getPoints() {
      return (BLonPointDeviceExt)this.get(points);
   }

   public void setPoints(BLonPointDeviceExt v) {
      this.set(points, v, null);
   }

   public BMessageTag getMessageIn() {
      return (BMessageTag)this.get(messageIn);
   }

   public void setMessageIn(BMessageTag v) {
      this.set(messageIn, v, null);
   }

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public void reset() {
      this.invoke(reset, null, null);
   }

   public void renewProxies() {
      this.invoke(renewProxies, null, null);
   }

   public void initImport() {
      this.invoke(initImport, null, null);
   }

   public void closeImport() {
      this.invoke(closeImport, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final boolean authenticate() {
      return this.getDeviceData().getAuthenticate();
   }

   public boolean isLocal() {
      return false;
   }

   public boolean programIdChanges() {
      return false;
   }

   public final BNeuronId getNeuronIdAddress() {
      return this.getDeviceData().getNeuronId();
   }

   public final BSubnetNode getSubnetNodeAddress() {
      return this.getDeviceData().getSubnetNodeId();
   }

   public final boolean isConfigOnline() {
      return this.getDeviceData().getNodeState() == BLonNodeState.configOnline;
   }

   public final boolean isConfigOffline() {
      return this.getDeviceData().getNodeState() == BLonNodeState.configOffline;
   }

   public final boolean isConfigured() {
      return this.isConfigOnline() || this.isConfigOffline();
   }

   public boolean isExtended() {
      return this.getDeviceData().isExtended();
   }

   public int getWorkingDomain() {
      return this.getDeviceData().getWorkingDomain();
   }

   public Type getNetworkType() {
      return BLonNetwork.TYPE;
   }

   public void started() throws Exception {
      super.started();
      BLonNetwork net = this.lonNetwork();
      if (net.isServiceRunning() && !this.isFatalFault()) {
         if (this.lonComm() == null) {
            this.configFail("No LonComm stack available");
            this.log().warning("No LonComm stack available. Fix problems and restart station.");
         } else {
            DeviceFacets.moveDeviceFacets(this);
            this.lonDeviceInit();
            net.addressManager().registerLonDevice(this);
            this.lonDeviceStarted();
            this.changeableNvCheck();
         }
      }
   }

   public void descendantsStarted() throws Exception {
      super.descendantsStarted();
      this.updateFaultCause();
   }

   public final void atSteadyState() {
      if (this.lonNetwork().isServiceRunning() && !this.isFatalFault()) {
         this.ping();
         this.lonDeviceAtSteadyState();
      }
   }

   public final void stopped() throws Exception {
      try {
         this.net = null;
         this.log = null;
         this.clearFiles();
         synchronized (this.nvSync) {
            this.nvList = null;
            this.staleNvList = true;
         }

         this.lonNetwork().addressManager().unregisterLonDevice(this);
         if (this.lonNetwork().isServiceRunning() && !this.isFatalFault()) {
            this.lonDeviceStopped();
            return;
         }
      } finally {
         super.stopped();
      }
   }

   public void renamed(Property property, String oldName, Context context) {
      if (property.getType().is(BLonComponent.TYPE)) {
         lonComponentRenamed(this, property.getName(), oldName, context);
      }
   }

   static void lonComponentRenamed(BLonDevice dev, String newName, String oldName, Context context) {
      BControlPoint[] cps = dev.getLonProxies();

      for (int i = 0; i < cps.length; i++) {
         BLonProxyExt lp = (BLonProxyExt)cps[i].getProxyExt();
         if (lp.getTargetComp().equals(oldName)) {
            lp.setTargetComp(newName);
            lp.renew();
         }
      }
   }

   public void updateDomainTable() {
      try {
         NmUtil.updateDomainTable(this.lonNetwork().getLonNetmgmt(), this);
      } catch (Throwable var2) {
         this.log().log(Level.SEVERE, "Unable to update domain table in " + this.getDisplayName(null), var2);
      }
   }

   public void updateNodeState() {
      try {
         BDeviceData dd = this.getDeviceData();
         BLonNodeState nState = dd.getNodeState();
         if (nState == BLonNodeState.applicationless && (dd.getHosted() || this.isLocal())) {
            try {
               this.set(BDeviceData.nodeState, NmUtil.getDeviceState(this), AddressManager.noDeviceChange);
            } catch (LonException var4) {
            }

            throw new BajaRuntimeException("Changing this device to applicationless state will render it inoperable.");
         }

         if (Lon.d()) {
            NmUtil.setDeviceState(this, nState);
         }
      } catch (Throwable var5) {
         this.log().log(Level.SEVERE, "Unable to update nodeState in " + this.getDisplayName(null), var5);
      }
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == status) {
            this.statusChanged();
         }

         if (prop.getType().is(BINetworkVariable.TYPE)) {
            synchronized (this.nvSync) {
               this.staleNvList = true;
            }
         }
      }
   }

   private void statusChanged() {
      this.updateFaultCause();
      BINetworkVariable[] nvs = this.getNvList();

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].isNetworkVariable()) {
            ((BNetworkVariable)nvs[i]).getTuning().transition();
         }
      }
   }

   protected void updateFaultCause() {
      if (this.isFault() && this.getFaultCause().length() == 0 && this.getLonNetwork().isFault()) {
         this.setFaultCause("Network fault: " + this.getLonNetwork().getFaultCause());
      }

      if (!this.isFault()) {
         this.setFaultCause("");
      }
   }

   public void added(Property prop, Context context) {
      super.added(prop, context);
      if (prop.getType().is(BINetworkVariable.TYPE)) {
         synchronized (this.nvSync) {
            this.staleNvList = true;
         }
      } else if (prop.getType().is(BLonLink.TYPE)) {
         BLonLink lnk = (BLonLink)this.get(prop);
         if (this.isRunning() && !lnk.getMessageTag()) {
            lnk.getDestinationNv().lonLinkAdded();
         }
      }
   }

   public void checkRemove(Property prop, Context context) {
      this.snv = NvDev.checkRemove(this, prop, context);
      super.checkRemove(prop, context);
   }

   public void removed(Property prop, BValue value, Context context) {
      super.removed(prop, value, context);
      if (prop.getType().is(BINetworkVariable.TYPE)) {
         synchronized (this.nvSync) {
            this.staleNvList = true;
         }
      }

      NvDev.removed(this, this.snv, prop, value, context);
      this.snv = null;
   }

   public final void knobAdded(Knob knob, Context context) {
      NvDev.knobAdded(this, knob, context);
   }

   public final void knobRemoved(Knob knob, Context context) {
      NvDev.knobRemoved(this, knob, context);
   }

   public IFuture postAsync(Runnable r) {
      return ((BLonNetwork)this.getNetwork()).postAsync(r);
   }

   protected IFuture postPing() {
      return this.postAsync(new Invocation(this, ping, null, null));
   }

   public final void doUpload(BUploadParameters params, Context cx) throws Exception {
      this.checkState();
      this.checkUpload();
      new BUploadJob(this, params, cx).submit(cx);
   }

   public final void doDownload(BDownloadParameters params, Context cx) throws Exception {
      this.checkState();
      this.checkDownload();
      new BDownloadJob(this, params, cx).submit(cx);
   }

   public void checkState() {
      if (this.isDown()) {
         throw new LocalizableRuntimeException("lonworks", "check.down");
      } else if (this.isDisabled()) {
         throw new LocalizableRuntimeException("lonworks", "check.disabled");
      } else if (this.isFault()) {
         throw new LocalizableRuntimeException("lonworks", "check.fault");
      }
   }

   public void beginConfigWrite() {
   }

   public void endConfigWrite() {
   }

   public AgentList getAgents(Context cx) {
      AgentList alist = super.getAgents(cx);
      if (this.getAction("ChangeNvTypeAction") == null) {
         alist.remove("lonworks:ChangeableNvManager");
      }

      return NvDev.fixWireSheet(alist, cx);
   }

   public void doReset() {
      try {
         if (Lon.d()) {
            NmUtil.resetNode(this);
         }
      } catch (Throwable var2) {
         this.log().log(Level.SEVERE, "Exception in LonDevice.reset()", var2);
      }
   }

   public void doRenewProxies() {
      BControlPoint[] cps = this.getLonProxies();

      for (int i = 0; i < cps.length; i++) {
         try {
            BLonProxyExt lp = (BLonProxyExt)cps[i].getProxyExt();
            lp.renew(true);
         } catch (Throwable var4) {
            System.out.println(var4);
            var4.printStackTrace();
         }
      }
   }

   public void doInitImport() {
      BINvContainer[] nvCntrs = this.getNvContainers();
      Array<BLink> to = new Array(BLink.class);

      for (int i = 0; i < nvCntrs.length; i++) {
         BLink[] lnks = nvCntrs[i].asComponent().getLinks();

         for (int ndx = 0; ndx < lnks.length; ndx++) {
            if (lnks[ndx].getType().is(BLonLink.TYPE)) {
               to.add(lnks[ndx]);
            }
         }

         Knob[] knobs = nvCntrs[i].asComponent().getKnobs();

         for (int ndxx = 0; ndxx < knobs.length; ndxx++) {
            BLink lnk = knobs[ndxx].getLink();
            if (lnk.getType().is(BLonLink.TYPE)) {
               to.add(lnk);
            }
         }
      }

      this.toLnks = (BLink[])to.trim();
   }

   public void doCloseImport() {
      this.doRenewProxies();
      if (this.toLnks != null) {
         for (int i = 0; i < this.toLnks.length; i++) {
            this.toLnks[i].deactivate();

            try {
               this.toLnks[i].activate();
            } catch (Throwable var3) {
               if (this.toLnks[i].getParent() != null) {
                  ((BComponent)this.toLnks[i].getParent()).remove(this.toLnks[i]);
               }
            }
         }

         this.toLnks = null;
      }
   }

   protected void lonDeviceInit() {
   }

   protected void lonDeviceStarted() {
   }

   protected void lonDeviceAtSteadyState() {
   }

   protected void lonDeviceStopped() {
   }

   public void deviceDataChanged(Property prop, Context context) {
      if (prop.equals(BDeviceData.programId)) {
         this.changeableNvCheck();
      }

      if (prop.equals(BDeviceData.subnetNodeId)) {
         this.updateLocallyBoundNvs();
      }

      if (prop == BDeviceData.neuronId) {
         this.clearMaxMessageLength();
      }
   }

   public void updateLocalState() {
   }

   public void beginCommission() {
   }

   public void postCommission() {
   }

   private void changeableNvCheck() {
      if (!this.isLocal()) {
         boolean hasChangeAction = this.getAction("ChangeNvTypeAction") != null;
         if (this.hasChangeableNvs()) {
            if (hasChangeAction) {
               return;
            }

            this.add("ChangeNvTypeAction", new BChangeNvTypeAction(), 4);
         } else if (hasChangeAction) {
            this.remove("ChangeNvTypeAction");
         }
      }
   }

   private void updateLocallyBoundNvs() {
      BINetworkVariable[] nvs = this.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].isNetworkVariable()) {
            ((BNetworkVariable)nvs[i]).reregisterSelector();
         }
      }
   }

   public boolean hasChangeableNvs() {
      return this.getDeviceData().getProgramId().hasChangeableNvs();
   }

   public void bound(int nvIndex) {
   }

   public void unbound(int nvIndex) {
   }

   public void bindComplete() {
   }

   public final BMessageTag getMessageTag(int mtIndex) {
      if (mtIndex == -1) {
         return this.getMessageIn();
      } else {
         SlotCursor<Property> c = this.getProperties();

         while (c.next(BMessageTag.class)) {
            BMessageTag mt = (BMessageTag)c.get();
            if (mt.getIndex() == mtIndex) {
               return mt;
            }
         }

         return null;
      }
   }

   @Override
   public BLonDevice getLonDevice() {
      return this;
   }

   @Override
   public BLonNetwork getLonNetwork() {
      return this.lonNetwork();
   }

   @Override
   public boolean isLonObject() {
      return false;
   }

   public BINvContainer[] getNvContainers() {
      Array<BINvContainer> a = new Array(BINvContainer.class);
      a.add(this);
      this.doGetNvContainers(this, a);
      return (BINvContainer[])a.trim();
   }

   private void doGetNvContainers(BComponent comp, Array<BINvContainer> a) {
      SlotCursor<Property> sc = comp.getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BINvContainer.TYPE) && !c.getType().is(BPseudoNvContainer.TYPE)) {
            a.add((BINvContainer)c);
         } else if (c.getType().is(BLonObjectFolder.TYPE)) {
            this.doGetNvContainers(c, a);
         }
      }
   }

   public BLonObject[] getLonObjects() {
      Array<BLonObject> a = new Array(BLonObject.class);
      this.doGetLonObjects(this, a);
      return (BLonObject[])a.trim();
   }

   private void doGetLonObjects(BComponent comp, Array<BLonObject> a) {
      SlotCursor<Property> sc = comp.getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonObject.TYPE)) {
            a.add((BLonObject)c);
         } else if (c.getType().is(BLonObjectFolder.TYPE)) {
            this.doGetLonObjects(c, a);
         }
      }
   }

   public BLonObject getLonObject(int objectId) {
      return this.doGetLonObject(this, objectId);
   }

   private BLonObject doGetLonObject(BComponent comp, int id) {
      SlotCursor<Property> sc = comp.getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonObject.TYPE)) {
            BLonObject lo = (BLonObject)c;
            if (lo.getObjectId() == id) {
               return lo;
            }
         } else if (c.getType().is(BLonObjectFolder.TYPE)) {
            return this.doGetLonObject(c, id);
         }
      }

      return null;
   }

   public final BNetworkVariable getNetworkVariable(int nvIndex) {
      if (nvIndex < 0) {
         return null;
      } else {
         BINetworkVariable[] nvs = this.getNvList();
         return nvIndex < nvs.length && nvs[nvIndex] != null && nvs[nvIndex].isNetworkVariable() ? (BNetworkVariable)nvs[nvIndex] : null;
      }
   }

   public final BNetworkConfig getNetworkConfig(int nvIndex) {
      if (nvIndex < 0) {
         return null;
      } else {
         BINetworkVariable[] nvs = this.getNvList();
         return nvIndex < nvs.length && nvs[nvIndex] != null && nvs[nvIndex].isNetworkConfig() ? (BNetworkConfig)nvs[nvIndex] : null;
      }
   }

   public final Property findLonObjectNvProperty(int objectIndex, int memberIndex, int snvtType) {
      BINetworkVariable[] nvs = this.getNvList();

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].isNetworkVariable()) {
            BNetworkVariable nv = (BNetworkVariable)nvs[i];
            if (nv.getNvProps().getObjectIndex() == objectIndex && nv.getNvProps().getMemberIndex() == memberIndex) {
               if (snvtType > 0 && nv.getSnvtType() != snvtType) {
                  throw new BajaRuntimeException(
                     "Invalid nvtype for object/member " + objectIndex + "|" + memberIndex + " snvtType=" + nv.getSnvtType() + ". Expected " + snvtType
                  );
               }

               this.log().fine("found nv for " + objectIndex + "  " + memberIndex + " - " + nv.getDisplayName(null));
               return this.getProperty(nv.getName());
            }
         }
      }

      return null;
   }

   public final BINetworkVariable findSnvtType(int snvtType) {
      BINetworkVariable[] nvs = this.getNvList();

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].getSnvtType() == snvtType) {
            return nvs[i];
         }
      }

      return null;
   }

   @Override
   public final BINetworkVariable[] getNetworkVariables() {
      return this.getNvList();
   }

   public final BNetworkConfig[] getNetworkConfigs() {
      return (BNetworkConfig[])NmUtil.getDecendantsByClass(this, BNetworkConfig.class);
   }

   public final BConfigParameter[] getConfigParameters() {
      return (BConfigParameter[])NmUtil.getDecendantsByClass(this, BConfigParameter.class);
   }

   public final void refreshNvList() {
      synchronized (this.nvSync) {
         this.staleNvList = true;
      }
   }

   private BINetworkVariable[] getNvList() {
      synchronized (this.nvSync) {
         if (this.staleNvList) {
            Vector<BINetworkVariable> v = new Vector<>(100);
            int maxNvIndex = -1;
            BINvContainer[] nvcs = this.getNvContainers();

            for (int i = 0; i < nvcs.length; i++) {
               SlotCursor<Property> c = ((BComponent)nvcs[i]).getProperties();

               while (c.next(BINetworkVariable.class)) {
                  BINetworkVariable nv = (BINetworkVariable)c.get();
                  int nvIndex = nv.getNvIndex();
                  if (nvIndex > maxNvIndex) {
                     maxNvIndex = nvIndex;
                  }

                  v.add(nv);
               }
            }

            this.nvList = new BINetworkVariable[maxNvIndex + 1];

            for (int i = 0; i < this.nvList.length; i++) {
               this.nvList[i] = null;
            }

            for (int i = 0; i < v.size(); i++) {
               BINetworkVariable nv = v.elementAt(i);
               if (nv.getNvIndex() >= 0) {
                  this.nvList[nv.getNvIndex()] = nv;
               }
            }

            this.staleNvList = false;
         }

         return this.nvList;
      }
   }

   public final BAliasConfigData getAlias(BNetworkVariable nv, int initialIndex) {
      BAliasTable tab = this.getDeviceData().getAliasTable();
      if (tab.getAliasCount() == 0) {
         return null;
      } else {
         int nvIndex = nv.getNvIndex();

         for (int i = initialIndex; i < tab.getAliasCount(); i++) {
            BAliasConfigData aDat = tab.getAliasEntry(i);
            if (aDat.getPrimary() == nvIndex) {
               return aDat;
            }
         }

         return null;
      }
   }

   public BControlPoint[] getLonProxies() {
      return this.getLonProxies(null);
   }

   public BControlPoint[] getLonProxies(BLonComponent lc) {
      Array<BControlPoint> a = new Array(BControlPoint.class);
      this.getLonProxies(this, a, lc);
      return (BControlPoint[])a.trim();
   }

   private void getLonProxies(BComponent comp, Array<BControlPoint> a, BLonComponent lc) {
      SlotCursor<Property> sc = comp.getProperties();

      while (sc.nextComponent()) {
         BComponent c = (BComponent)sc.get();
         if (c.getType().is(BLonPointFolder.TYPE) || c.getType().is(BLonPointDeviceExt.TYPE)) {
            this.getLonProxies(c, a, lc);
         } else if (c.getType().is(BControlPoint.TYPE)) {
            BComponent ext = ((BControlPoint)c).getProxyExt();
            if (ext instanceof BLonProxyExt && (lc == null || ((BLonProxyExt)ext).getTargetComp().equals(lc.getName()))) {
               a.add((BControlPoint)c);
            }
         }
      }
   }

   public void doPing() {
      if (this.isRunning() && this.getEnabled() && this.lonNetwork().isServiceRunning() && !this.isFatalFault()) {
         this.pingImpl();
      }
   }

   private void pingImpl() {
      try {
         if (this.getNeuronIdAddress().isZero()) {
            throw new RuntimeException("Neuron id is zero");
         }

         if (this.getDeviceData().getNodeState() == BLonNodeState.unknown) {
            throw new RuntimeException("Unknown state - Device requires commissioning");
         }

         if (Lon.d()) {
            NmUtil.getDeviceState(this);
         }

         this.pingOk();
         this.setStatus(BStatus.make(this.getStatus(), 4, false));
      } catch (NotRunningException var2) {
      } catch (Throwable var3) {
         this.pingFail(var3.getMessage());
         this.setStatus(BStatus.make(this.getStatus(), 4, true));
      }
   }

   public boolean isReadyForNvUpdates() {
      return Sys.atSteadyState() && this.getStatus().isValid() && this.isConfigOnline() && this.getEnabled();
   }

   protected LinkCheck doCheckLink(BComponent source, Slot sourceSlot, Slot targetSlot, Context cx) {
      return NvDev.doNvCheckLink(source, sourceSlot, this, targetSlot, cx);
   }

   @Override
   public void linkUpdate() {
      if (!this.linkUpdateDone) {
         this.getComponentSpace().update(this, 1);
         this.getComponentSpace().update(this.getDeviceData(), 2);
         this.linkUpdateDone = true;
         BINvContainer[] ca = this.getNvContainers();

         for (int i = 1; i < ca.length; i++) {
            ca[i].linkUpdate();
         }
      }
   }

   public final BLink makeLink(BComponent source, Slot sourceSlot, Slot targetSlot, Context cx) {
      return !NvDev.requiresLonLink(targetSlot)
         ? super.makeLink(source, sourceSlot, targetSlot, cx)
         : NvDev.makeLonLink(source, sourceSlot, this, targetSlot, cx);
   }

   final void initDownload(boolean allowRandomAccess) {
      try {
         if (this.fileState == 0) {
            this.createFiles();
         }

         if (this.fileState == 1) {
            return;
         }

         if (this.readWritefile != null && !this.readWritefile.isOpen()) {
            if (this.hasReadOnly || !this.hasReadOnlyCp()) {
               this.readWritefile.open(1, true, allowRandomAccess);
            } else if (this.readWritefile.supportsRandomAccess()) {
               this.readWritefile.open(1, true, true);
            } else {
               this.readWritefile.open(1, false, false);
            }
         }

         if (this.readOnlyfile != null && !this.readOnlyfile.isOpen()) {
            this.readOnlyfile.open(2, true, allowRandomAccess);
         }
      } catch (LonException var3) {
         this.close();
         this.clearFiles();
         throw new BajaRuntimeException("Unable to open files for download.", var3);
      }

      this.fileState = 2;
   }

   private boolean hasReadOnlyCp() {
      BINvContainer[] nvcs = this.getNvContainers();

      for (int i = 0; i < nvcs.length; i++) {
         SlotCursor<Property> c = ((BComponent)nvcs[i]).getProperties();

         while (c.next(BConfigParameter.class)) {
            BConfigParameter cp = (BConfigParameter)c.get();
            if (!cp.isWriteable()) {
               return true;
            }
         }
      }

      return false;
   }

   final void cleanupDownload() {
      this.close();
      this.fileState = 4;
   }

   public final boolean isDownLoadInProgress() {
      return this.downloading;
   }

   public final boolean isUpLoadInProgress() {
      return this.uploading;
   }

   public void checkUpload() {
   }

   @Override
   public void beginUpload() {
      this.uploading = true;
      this.initUpload(false);
   }

   @Override
   public void endUpload() {
      this.cleanupUpload();
      this.uploading = false;
   }

   public void checkChangeNvType() {
   }

   public void changeNvTypeComplete() {
   }

   public void checkDownload() {
   }

   @Override
   public void beginDownload() {
      this.downloading = true;
      this.initDownload(false);
   }

   @Override
   public void endDownload() {
      this.cleanupDownload();
      this.downloading = false;
   }

   final void initUpload(boolean allowRandomAccess) {
      try {
         if (this.fileState == 0) {
            this.createFiles();
         }

         if (this.fileState == 1) {
            return;
         }

         if (this.readWritefile != null && !this.readWritefile.isOpen()) {
            this.readWritefile.open(1, false, allowRandomAccess);
         }

         if (this.readOnlyfile != null && !this.readOnlyfile.isOpen()) {
            this.readOnlyfile.open(2, false, allowRandomAccess);
         }
      } catch (LonException var3) {
         this.close();
         this.clearFiles();
         throw new BajaRuntimeException("Unable to open files for upload.", var3);
      }

      this.fileState = 3;
   }

   final void cleanupUpload() {
      this.close();
      this.fileState = 4;
   }

   public final LonFile getLonFileOpen(int fileNum) {
      return this.getLonFileOpen(fileNum, false, true);
   }

   public final LonFile getLonFileOpen(int fileNum, boolean create, boolean random) {
      try {
         if (this.fileState == 0) {
            this.createFiles();
         }

         if (this.fileState == 1) {
            return null;
         } else {
            LonFile f = this.lonFile.copy();
            if (fileNum < 0) {
               return f;
            } else {
               if (f != null && !f.isOpen()) {
                  f.open(fileNum, create, random);
               }

               return f;
            }
         }
      } catch (LonException var5) {
         var5.printStackTrace();
         this.log().log(Level.SEVERE, "error accessing file " + fileNum, (Throwable)var5);
         return null;
      }
   }

   protected LonFile getReadWriteFile() {
      try {
         if (this.fileState == 0) {
            this.createFiles();
         }

         if (this.fileState == 1) {
            return null;
         } else {
            LonFile f = this.readWritefile;
            if (f != null && !f.isOpen()) {
               f.open(1, false, true);
            }

            return f;
         }
      } catch (LonException var2) {
         var2.printStackTrace();
         this.log().log(Level.SEVERE, "error accessing file readOnly config file", (Throwable)var2);
         return null;
      }
   }

   protected LonFile getReadOnlyFile() {
      try {
         if (this.fileState == 0) {
            this.createFiles();
         }

         if (this.fileState == 1) {
            return null;
         } else {
            LonFile f = this.readOnlyfile;
            int fnum = 2;
            if (!this.hasReadOnly) {
               f = this.readWritefile;
               fnum = 1;
            }

            if (f != null && !f.isOpen()) {
               f.open(fnum, false, true);
            }

            return f;
         }
      } catch (LonException var3) {
         var3.printStackTrace();
         this.log().log(Level.SEVERE, "error accessing file readOnly config file", (Throwable)var3);
         return null;
      }
   }

   private void createFiles() throws LonException {
      if (!this.getDeviceData().getHasNodeObject()) {
         this.fileState = 1;
      } else {
         this.lonFile = LonFile.createFile(this);
         if (this.lonFile == null) {
            this.fileState = 1;
         } else {
            if (this.lonFile.findFileNum(2) == 0) {
               this.readWritefile = this.lonFile;
               this.hasReadOnly = this.lonFile.findFileNum(1, 1) > 0;
               if (this.hasReadOnly) {
                  this.readOnlyfile = this.lonFile.copy();
               }
            }

            this.fileState = 4;
         }
      }
   }

   private void close() {
      if (this.lonFile != null) {
         try {
            this.lonFile.close();
         } catch (LonException var4) {
         }
      }

      if (this.readWritefile != null) {
         try {
            this.readWritefile.close();
         } catch (LonException var3) {
         }
      }

      if (this.readOnlyfile != null) {
         try {
            this.readOnlyfile.close();
         } catch (LonException var2) {
         }
      }
   }

   public final void clearFiles() {
      this.fileState = 0;
      this.lonFile = null;
      this.readWritefile = null;
      this.readOnlyfile = null;
   }

   @Deprecated
   public boolean disableObjectForCpWrite(BConfigProps configProps) {
      throw new UnsupportedOperationException("Deprecated");
   }

   @Deprecated
   public void enableObject(BConfigProps configProps) {
      throw new UnsupportedOperationException("Deprecated");
   }

   @Deprecated
   public final void resetDevice() {
      throw new UnsupportedOperationException("Deprecated : use doReset()");
   }

   public boolean isObjectDisabled(int objNdx) {
      if (!this.isConfigOnline()) {
         return true;
      } else {
         Property reqProp = this.findLonObjectNvProperty(0, 1, 92);
         int reqNdx = ((BNetworkVariable)this.get(reqProp)).getNvIndex();
         if (reqNdx < 0) {
            throw new BajaRuntimeException("Can not find SNVT_OBJ_REQUEST in " + this.getDisplayName(null));
         } else {
            BNetworkVariable reqNv = this.getNetworkVariable(reqNdx);
            Property statNdx = this.findLonObjectNvProperty(0, 2, 93);
            if (statNdx == null) {
               throw new BajaRuntimeException("Can not find nvoStatus.");
            } else {
               BNetworkVariable statNv = (BNetworkVariable)this.get(statNdx);

               try {
                  this.sendObjectRequest(BLonObjectRequestEnum.rqUpdateStatus, objNdx, reqNv);
                  NmUtil.wait(50);
                  BNetworkVariable objStat = this.getObjectStatus(objNdx, statNv);
                  return objStat.getData().getLonBoolean("disabled");
               } catch (LonException var8) {
                  throw new BajaRuntimeException("Unable to set object " + objNdx + " status in " + this.getDisplayName(null), var8);
               }
            }
         }
      }
   }

   public boolean enableObject(int objNdx, boolean en) {
      if (objNdx < 0) {
         return false;
      } else {
         Property reqProp = this.findLonObjectNvProperty(0, 1, 92);
         int reqNdx = ((BNetworkVariable)this.get(reqProp)).getNvIndex();
         if (reqNdx < 0) {
            return false;
         } else {
            BNetworkVariable reqNv = this.getNetworkVariable(reqNdx);
            Property statNdx = this.findLonObjectNvProperty(0, 2, 93);
            if (statNdx == null) {
               return false;
            } else {
               BNetworkVariable statNv = (BNetworkVariable)this.get(statNdx);

               for (int attempts = 3; attempts > 0; attempts--) {
                  try {
                     BLonObjectRequestEnum req = en ? BLonObjectRequestEnum.rqEnable : BLonObjectRequestEnum.rqDisabled;
                     this.sendObjectRequest(req, objNdx, reqNv);
                     NmUtil.wait(50);
                     BNetworkVariable objStat = this.getObjectStatus(objNdx, statNv);
                     if (objStat.getData().getLonBoolean("invalidRequest")) {
                        return false;
                     }

                     if (objStat.getData().getLonBoolean("disabled") == en) {
                        return true;
                     }
                  } catch (Exception var11) {
                     System.out.println("enableObject() " + var11);
                     var11.printStackTrace();
                  }
               }

               return false;
            }
         }
      }
   }

   private void sendObjectRequest(BLonObjectRequestEnum req, int objNdx, BNetworkVariable nv) throws LonException {
      BLonData ld = nv.getData();
      ld.setLonInt("objectId", objNdx, BLonNetwork.lonNoWrite);
      ld.setLonEnum("objectRequest", req.getTag(), BLonNetwork.lonNoWrite);
      nv.doForceWrite();
   }

   private BNetworkVariable getObjectStatus(int objNdx, BNetworkVariable statNv) throws LonException {
      for (int i = 0; i < 4; i++) {
         if (!statNv.getNvProps().getBoundToLocal()) {
            statNv.doForceRead();
         }

         int objId = statNv.getData().getLonInt("objectId");
         if (objId == objNdx) {
            return statNv;
         }

         NmUtil.wait(100);
      }

      throw new LonException("Can not read status nv");
   }

   void disableObjectsForWrite(int[] sels, boolean[] objDis) {
      if (!this.isDownLoadInProgress() && this.isConfigOnline()) {
         for (int i = 0; i < sels.length; i++) {
            if (this.isObjectDisabled(sels[i])) {
               objDis[i] = false;
            } else {
               try {
                  objDis[i] = this.enableObject(sels[i], false);
               } catch (Throwable var5) {
                  objDis[i] = false;
                  System.out.println(var5);
               }
            }
         }
      } else {
         for (int ix = 0; ix < sels.length; ix++) {
            objDis[ix] = false;
         }
      }
   }

   void enableObjectsAfterWrite(int[] sels, boolean[] objDis) {
      for (int i = 0; i < sels.length; i++) {
         if (objDis[i]) {
            try {
               this.enableObject(sels[i], true);
            } catch (Throwable var5) {
               System.out.println(var5);
            }
         }
      }
   }

   public int getMaxMessageLengthOut() {
      if (this.maxOutMessageSize == 0) {
         this.updateMaxMessageLength();
      }

      return this.maxOutMessageSize;
   }

   public int getMaxMessageLengthIn() {
      if (this.maxInMessageSize == 0) {
         this.updateMaxMessageLength();
      }

      return this.maxInMessageSize;
   }

   private final void clearMaxMessageLength() {
      this.maxOutMessageSize = 0;
      this.maxInMessageSize = 0;
   }

   public final void updateMaxMessageLength() {
      try {
         if (!Lon.d()) {
            return;
         }

         byte[] a = Neuron.readMemory(this.lonComm(), 1, NmUtil.getSendAddress(this), 21, 5, this.authenticate(), false, 8);
         boolean explicit = (a[0] >> 5 & 1) > 0;
         int appBufOutSize = Neuron.getBufferSize((a[3] & 240) >> 4);
         int appBufInSize = Neuron.getBufferSize(a[3] & 15);
         int netBufOutSize = Neuron.getBufferSize((a[4] & 240) >> 4);
         int netBufInSize = Neuron.getBufferSize(a[4] & 15);
         int appOverhead = explicit ? 16 : 5;
         this.maxInMessageSize = Math.min(appBufInSize - appOverhead, netBufInSize - 26);
         this.maxOutMessageSize = Math.min(appBufOutSize - appOverhead, netBufOutSize - 26);
         if (!this.isLocal()) {
            BLonDevice local = this.lonNetwork().getLocalLonDevice();
            this.maxInMessageSize = Math.min(this.maxInMessageSize, local.getMaxMessageLengthOut());
            this.maxOutMessageSize = Math.min(this.maxOutMessageSize, local.getMaxMessageLengthIn());
         }

         if (this.maxInMessageSize < 0) {
            this.maxInMessageSize = 8;
         }

         if (this.maxOutMessageSize < 0) {
            this.maxOutMessageSize = 8;
         }
      } catch (Exception var9) {
         this.maxInMessageSize = 8;
         this.maxOutMessageSize = 8;
         this.log().log(Level.SEVERE, "error reading buffer size ", (Throwable)var9);
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.trTitle("LonDevice", 1);
      out.startProps("Files");
      out.prop("Has node object", this.getDeviceData().getHasNodeObject());
      switch (this.fileState) {
         case 0:
            out.prop("File state", "FILE_STATE_INIT    ");
            break;
         case 1:
            out.prop("File state", "FILE_STATE_NO_FILES");
            break;
         case 2:
            out.prop("File state", "FILE_STATE_DOWNLOAD");
            break;
         case 3:
            out.prop("File state", "FILE_STATE_UPLOAD  ");
            break;
         case 4:
            out.prop("File state", "FILE_STATE_IDLE    ");
            break;
         default:
            out.prop("File state", "unknown");
      }

      out.prop("Has readWrite file", this.readWritefile != null);
      out.prop("Has readOnly file", this.readOnlyfile != null);
      out.prop("maxMessageSizeIn", this.getMaxMessageLengthIn());
      out.prop("maxMessageSizeOut", this.getMaxMessageLengthOut());
      out.prop("dataPntMismatchCount", this.dataPntMismatchCount);
      out.prop("downloading", this.downloading);
      out.prop("uploading", this.uploading);
      out.endProps();
      DeviceFacets.spy(this, out);
   }

   public final LonComm lonComm() {
      return this.lonNetwork().lonComm();
   }

   public Logger log() {
      if (this.log == null) {
         this.log = this.lonNetwork().log();
      }

      return this.log;
   }

   public final BLonNetwork lonNetwork() {
      if (this.net == null) {
         BComplex p = this.getParent();

         while (p != null && !(p instanceof BLonNetwork)) {
            p = p.getParent();
         }

         if (p != null) {
            this.net = (BLonNetwork)p;
         }
      }

      return this.net;
   }
}
