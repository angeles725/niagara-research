package javax.baja.lonworks;

import com.tridium.lonworks.Lon;
import com.tridium.lonworks.local.BLocalNci;
import com.tridium.lonworks.local.BLocalNv;
import com.tridium.lonworks.local.LocalDev;
import com.tridium.lonworks.local.SnvtInfo;
import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmessages.QueryDomainResponse;
import com.tridium.lonworks.netmessages.UnprocessedNV;
import com.tridium.lonworks.netmessages.WriteMemRequest;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.util.Neuron;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.xif.LocalToXif;
import java.util.logging.Level;
import javax.baja.agent.AgentList;
import javax.baja.lonworks.datatypes.BAuthenticationKey;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.datatypes.BExtDeviceData;
import javax.baja.lonworks.datatypes.BLocal;
import javax.baja.lonworks.datatypes.BLocalExtractXifParameter;
import javax.baja.lonworks.datatypes.BLocalImportXmlParameter;
import javax.baja.lonworks.datatypes.BNcProps;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.lonworks.enums.BLonNvDirection;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "externalConfig",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "selfDoc",
      type = "String",
      defaultValue = "&3.0@0;Niagara Server Node"
   )})
@NiagaraActions({@NiagaraAction(
      name = "servicePin"
   ), @NiagaraAction(
      name = "importXml",
      parameterType = "BLocalImportXmlParameter",
      defaultValue = "new BLocalImportXmlParameter()",
      flags = 4
   ), @NiagaraAction(
      name = "extractXif",
      parameterType = "BLocalExtractXifParameter",
      defaultValue = "new BLocalExtractXifParameter()",
      returnType = "BString",
      flags = 4
   )})
@NiagaraTopic(
   name = "wink"
)
public class BLocalLonDevice extends BLonDevice {
   public static final Property externalConfig = newProperty(0, false, null);
   public static final Property selfDoc = newProperty(0, "&3.0@0;Niagara Server Node", null);
   public static final Action servicePin = newAction(0, null);
   public static final Action importXml = newAction(4, new BLocalImportXmlParameter(), null);
   public static final Action extractXif = newAction(4, new BLocalExtractXifParameter(), null);
   public static final Topic wink = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BLocalLonDevice.class);
   private static final BIcon icon = BIcon.std("deviceLocal.png");
   private SnvtInfo snvtInfo = null;
   private Object syncSnvtInfo = new Object();
   private static BFacets noChange = BFacets.make("noChange", true);
   public static final Context noInfoChange = new BasicContext(noChange) {
      public boolean equals(Object obj) {
         return obj != null && obj instanceof Context && ((Context)obj).getFacets().getb("noChange", false);
      }

      public int hashCode() {
         return super.hashCode();
      }
   };

   public boolean getExternalConfig() {
      return this.getBoolean(externalConfig);
   }

   public void setExternalConfig(boolean v) {
      this.setBoolean(externalConfig, v, null);
   }

   public String getSelfDoc() {
      return this.getString(selfDoc);
   }

   public void setSelfDoc(String v) {
      this.setString(selfDoc, v, null);
   }

   public void servicePin() {
      this.invoke(servicePin, null, null);
   }

   public void importXml(BLocalImportXmlParameter parameter) {
      this.invoke(importXml, parameter, null);
   }

   public BString extractXif(BLocalExtractXifParameter parameter) {
      return (BString)this.invoke(extractXif, parameter, null);
   }

   public void fireWink(BValue event) {
      this.fire(wink, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public final boolean receiveNvUpdate(int sel, UnprocessedNV msg) {
      BINetworkVariable[] nvs = this.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         BINetworkVariable nv = nvs[i];
         if (nv != null && nv.getNvConfigData().getSelector() == sel) {
            if (msg.getDirection().equals(BLonNvDirection.output)) {
               if (nv.getNvConfigData().isOutput()) {
                  this.sendNvPollResponse(msg, nv);
               }
            } else if (nv.getNvConfigData().isInput()) {
               nv.receiveUpdate(msg.getData());
            }

            return true;
         }
      }

      return false;
   }

   private void sendNvPollResponse(UnprocessedNV msg, BINetworkVariable nv) {
      try {
         BNvConfigData configData = nv.getNvConfigData();
         UnprocessedNV rsp = new UnprocessedNV(configData.getDirection(), configData.getSelector(), nv.getData().toNetBytes());
         this.lonComm().sendResponse(msg, rsp);
      } catch (LonException var5) {
         this.lonNetwork().log().log(Level.SEVERE, "Unable to create or send nvPoll response.", (Throwable)var5);
      }
   }

   @Override
   public final boolean programIdChanges() {
      return true;
   }

   @Override
   public final boolean isLocal() {
      return true;
   }

   @Override
   public void doPing() {
      if (this.isRunning() && this.getEnabled() && this.lonNetwork().isServiceRunning()) {
         if (this.getExternalConfig()) {
            try {
               BDeviceData dd = this.getDeviceData();
               dd.set(BDeviceData.nodeState, NmUtil.getDeviceState(this), AddressManager.noDeviceChange);

               for (int i = 0; i < dd.getAddressCount(); i++) {
                  dd.setAddressEntry(i, NmUtil.getBAddressTableEntry(this, i), AddressManager.noDeviceChange);
                  NmUtil.wait(600);
               }

               dd.setInt(BDeviceData.channelId, Neuron.getChannelId(this), AddressManager.noDeviceChange);
               NmUtil.wait(600);
               QueryDomainResponse qd0 = NmUtil.queryDomain(this, 0);
               NmUtil.wait(600);
               QueryDomainResponse qd1 = NmUtil.queryDomain(this, 1);
               QueryDomainResponse wrkg = qd0;
               int wrkgDmn = 0;
               if (qd1.inUse() && (!qd0.inUse() || qd0.getLen() == 0 && qd1.getLen() >= 1)) {
                  wrkgDmn = 1;
                  wrkg = qd1;
               }

               dd.set(BDeviceData.subnetNodeId, BSubnetNode.make(wrkg.getSubnet(), wrkg.getNodeId()), AddressManager.noDeviceChange);
               dd.setInt(BDeviceData.workingDomain, wrkgDmn, AddressManager.noDeviceChange);
               BLonNetmgmt netmgmt = this.lonNetwork().netmgmt();
               netmgmt.set(BLonNetmgmt.domainId, BDomainId.make(wrkg.getLen(), wrkg.getDomainId()), BLonNetwork.lonNoWrite);
               netmgmt.set(BLonNetmgmt.authenticationKey, BAuthenticationKey.make(wrkg.getKey()), BLonNetwork.lonNoWrite);
            } catch (LonException var7) {
               this.lonNetwork().log().log(Level.SEVERE, "Unable to initialize local lon interface.", (Throwable)var7);
            }
         }

         this.pingOk();
      }
   }

   @Override
   public AgentList getAgents(Context cx) {
      AgentList alist = super.getAgents(cx);
      alist.remove("lonworks:NcManager");
      alist.remove("lonworks:UxNcManager");
      alist.remove("lonworks:NvManager");
      alist.remove("lonworks:UxNvManager");
      return alist;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.verifyNvIndices();
      this.verifyLonMark();
      this.verifyNetmgmtEnable();
   }

   public void forceStart() {
      this.lonDeviceInit();
   }

   @Override
   protected void lonDeviceInit() {
      if (this.getSubnetNodeAddress().equals(BSubnetNode.DEFAULT)) {
         this.getDeviceData().setSubnetNodeId(BSubnetNode.make(1, 127));
      }

      this.runAsyncUpdate();
   }

   private void runAsyncUpdate() {
      Runnable req = new Runnable() {
         @Override
         public void run() {
            BLocalLonDevice.this.asyncUpdate();
         }
      };
      this.lonNetwork().postAsync(req);
   }

   private void asyncUpdate() {
      try {
         try {
            NmUtil.queryStatus(this, 0);
         } catch (LonException var5) {
            try {
               NmUtil.queryStatus(this, 0);
            } catch (LonException var4) {
            }
         }

         BDeviceData dd = this.getDeviceData();
         BNeuronId nid = Neuron.getNeuronId(this);
         if (!dd.getNeuronId().equals(nid)) {
            dd.set(BDeviceData.neuronId, nid, AddressManager.noDeviceChange);
            dd.setBoolean(BDeviceData.twoDomains, Neuron.isTwoDomains(this.lonComm(), BLocal.local, false, false), AddressManager.noDeviceChange);
            dd.setInt(BDeviceData.channelId, 1, AddressManager.noDeviceChange);
            dd.set(BDeviceData.subnetNodeId, BSubnetNode.make(1, 127), AddressManager.noDeviceChange);
            dd.setBoolean(BDeviceData.hosted, true, AddressManager.noDeviceChange);
            dd.set(BDeviceData.programId, BProgramId.TRIDIUM_PID, AddressManager.noDeviceChange);
            dd.set(BDeviceData.nodeState, BLonNodeState.configOnline, AddressManager.noDeviceChange);
         }

         dd.setInt(BDeviceData.addressCount, Neuron.getAddressCount(this.lonComm(), BLocal.local, false, false), AddressManager.noDeviceChange);
         if (dd.getAddressCount() > 15 && !(dd instanceof BExtDeviceData)) {
            BExtDeviceData edd = BExtDeviceData.make(dd);
            edd.setExtended(false);
            this.setDeviceData(edd);
         }

         BLonNetmgmt nm = this.lonNetwork().netmgmt();
         NmUtil.updateDomainTable(this, nm.getDomainId(), nm.getAuthenticationKey(), false);
         NmUtil.updateAddressTable(this);
         Neuron.updateChannelId(this);
         NmUtil.setDeviceState(this, this.getDeviceData().getNodeState());
         this.writeProgramId(this.getDeviceData().getProgramId());
         this.ping();
      } catch (LonException var6) {
         this.lonNetwork().log().log(Level.SEVERE, "Error initializing local lon interface.", (Throwable)var6);
         var6.printStackTrace();
      }

      this.lonNetwork().netMessageReceiver().okToReceive();
      if (!Lon.disableResetOnStart()) {
         this.doReset();
      }
   }

   @Override
   public void deviceDataChanged(Property prop, Context context) {
      if (prop == BDeviceData.workingDomain) {
         ((NLonComm)this.getLonNetwork().lonComm()).updateWorkingDomain();
      }

      if (context != AddressManager.noDeviceChange) {
         if (prop == BDeviceData.authenticate
            || prop == BDeviceData.addressTable
            || prop == BDeviceData.subnetNodeId
            || prop == BDeviceData.programId
            || prop == BDeviceData.workingDomain
            || prop == BDeviceData.channelId) {
            this.runAsyncUpdate();
         }
      }
   }

   @Override
   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == externalConfig) {
            this.verifyNetmgmtEnable();
         }
      }
   }

   private void verifyNetmgmtEnable() {
      this.lonNetwork().netmgmt().setEnabled(!this.getExternalConfig());
   }

   public final void doServicePin() {
      try {
         NmUtil.sendServicePin(this);
      } catch (LonException var2) {
         throw new BajaRuntimeException("Unable to do servicePin." + var2);
      }
   }

   private void writeProgramId(BProgramId pid) throws LonException {
      byte[] a = pid.getByteArray();
      WriteMemRequest writeReq = new WriteMemRequest(1, 13, a.length, 3, a);
      this.lonComm().sendUnacknowledged(BLocal.local, writeReq);
   }

   public final BIcon getIcon() {
      return icon;
   }

   public boolean isChildLegal(BComponent child) {
      return child.getType().is(BINetworkVariable.TYPE) && !((BINetworkVariable)child).isLocalNv() && !((BINetworkVariable)child).isLocalNci()
         ? false
         : super.isChildLegal(child);
   }

   @Override
   public void added(Property prop, Context context) {
      super.added(prop, context);
      if (this.isRunning() && !noInfoChange.equals(context)) {
         if (prop.getType().is(BINetworkVariable.TYPE)) {
            this.verifyNvIndices();
         }

         this.updateSnvtInfo();
      }
   }

   @Override
   public void removed(Property prop, BValue value, Context context) {
      super.removed(prop, value, context);
      if (this.isRunning() && !noInfoChange.equals(context)) {
         if (prop.getType().is(BINetworkVariable.TYPE)) {
            this.verifyNvIndices();
         }

         this.updateSnvtInfo();
      }
   }

   private void verifyLonMark() {
      BINetworkVariable[] a = this.getNetworkVariables();
      if (a.length <= 0) {
         BLocalNv nv = new BLocalNv(0, 92, 0, 1, 0, BLonNvDirection.input, "@0|1");
         this.add("nviRequest", nv, 1024);
         nv = new BLocalNv(1, 93, 0, 2, 0, BLonNvDirection.output, "@0|1");
         this.add("nvoStatus", nv, 0);
      }
   }

   private void verifyNvIndices() {
      BINetworkVariable[] a = this.getNetworkVariables();
      boolean stale = false;
      int i = 0;

      for (int n = 0; i < a.length; i++) {
         if (a[i] == null) {
            stale = true;
         } else {
            if (a[i].getNvIndex() != n) {
               if (a[i].isLocalNv()) {
                  ((BLocalNv)a[i]).getNvProps().setInt(BNvProps.nvIndex, n, noInfoChange);
               } else if (a[i].isLocalNci()) {
                  ((BLocalNci)a[i]).getNcProps().setInt(BNcProps.nvIndex, n, noInfoChange);
               }

               stale = true;
               BNvConfigData cd = a[i].getNvConfigData();
               if (!cd.isBoundNv()) {
                  cd.setInt(BNvConfigData.selector, 16383 - n, noInfoChange);
               }
            }

            n++;
         }
      }

      if (stale) {
         this.refreshNvList();
      }
   }

   public void checkAdd(String name, BValue value, int flags, BFacets facets, Context context) {
      super.checkAdd(name, value, flags, facets, context);
      if (this.isRunning()) {
         if (value.getType().is(BINetworkVariable.TYPE)) {
            BINetworkVariable nv = (BINetworkVariable)value;
            if (nv.getNvConfigData().getSelector() < 0) {
               BINetworkVariable[] a = this.getNetworkVariables();
               int nvIndex = 0;

               while (nvIndex < a.length && a[nvIndex] != null) {
                  nvIndex++;
               }

               if (nvIndex > 4095) {
                  throw new LocalizableRuntimeException("lonworks", "check.add.maxNvIndex");
               } else {
                  if (nvIndex >= a.length) {
                     BINetworkVariable[] newA = new BINetworkVariable[nvIndex + 1];
                     System.arraycopy(a, 0, newA, 0, a.length);
                     a = this.nvList = newA;
                  }

                  a[nvIndex] = nv;
                  nv.setNvIndex(nvIndex);
                  nv.getNvConfigData().setUnbound(nvIndex);
               }
            }
         }
      }
   }

   @Override
   protected void lonDeviceAtSteadyState() {
      this.updateSnvtInfo();
   }

   @Override
   public void renamed(Property prop, String oldName, Context context) {
      super.renamed(prop, oldName, context);
      if (this.isRunning() && context != noInfoChange) {
         if (prop.getType().is(BINetworkVariable.TYPE)) {
            this.updateSnvtInfo();
         }
      }
   }

   public void nvChanged(BINetworkVariable inv) {
      this.updateSnvtInfo();
   }

   private void updateSnvtInfo() {
      synchronized (this.syncSnvtInfo) {
         this.snvtInfo = null;
      }
   }

   public final SnvtInfo getSnvtInfo() {
      synchronized (this.syncSnvtInfo) {
         if (this.snvtInfo == null) {
            this.snvtInfo = new SnvtInfo(this);
         }

         return this.snvtInfo;
      }
   }

   public void doImportXml(BLocalImportXmlParameter p) {
      synchronized (this.syncSnvtInfo) {
         LocalDev.importXLon(this, p);
         this.snvtInfo = null;
         this.verifyNvIndices();
         this.refreshNvList();
      }
   }

   public BString doExtractXif(BLocalExtractXifParameter p) {
      String rtn = "error";

      try {
         rtn = LocalToXif.extractXif(this, p);
      } catch (Exception var4) {
         rtn = var4.toString();
      }

      return BString.make(rtn);
   }
}
