package javax.baja.lonworks;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.NetMessageReceiver;
import com.tridium.lonworks.NvManager;
import com.tridium.lonworks.datatypes.BUtilCmdJob;
import com.tridium.lonworks.datatypes.BUtilitiesCommand;
import com.tridium.lonworks.device.BDownloadJob;
import com.tridium.lonworks.device.BUploadJob;
import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.device.NvDev;
import com.tridium.lonworks.loncomm.NAppBuffer;
import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.TimedCoalesceQueue;
import com.tridium.util.PxUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.driver.BDeviceNetwork;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.license.Feature;
import javax.baja.lonworks.datatypes.BLonCommConfig;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.ext.BLonPollService;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.tuning.BLonTuningPolicyMap;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIService;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;
import javax.baja.util.Queue;
import javax.baja.util.Worker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "lonCommConfig",
      type = "BLonCommConfig",
      defaultValue = "new BLonCommConfig()"
   ), @NiagaraProperty(
      name = "pollService",
      type = "BLonPollService",
      defaultValue = "new BLonPollService()"
   ), @NiagaraProperty(
      name = "lonNetmgmt",
      type = "BLonNetmgmt",
      defaultValue = "new BLonNetmgmt()"
   ), @NiagaraProperty(
      name = "tuningPolicies",
      type = "BLonTuningPolicyMap",
      defaultValue = "new BLonTuningPolicyMap()"
   ), @NiagaraProperty(
      name = "localLonDevice",
      type = "BLocalLonDevice",
      defaultValue = "new BLocalLonDevice()"
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
      name = "executeCommand",
      parameterType = "BUtilitiesCommand",
      defaultValue = "new BUtilitiesCommand()",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "makeNvsNonCritical"
   )})
@NiagaraTopic(
   name = "deviceChange"
)
public class BLonNetwork extends BDeviceNetwork implements BIService {
   public static final Property lonCommConfig = newProperty(0, new BLonCommConfig(), null);
   public static final Property pollService = newProperty(0, new BLonPollService(), null);
   public static final Property lonNetmgmt = newProperty(0, new BLonNetmgmt(), null);
   public static final Property tuningPolicies = newProperty(0, new BLonTuningPolicyMap(), null);
   public static final Property localLonDevice = newProperty(0, new BLocalLonDevice(), null);
   public static final Action upload = newAction(0, new BUploadParameters(), null);
   public static final Action download = newAction(0, new BDownloadParameters(), null);
   public static final Action executeCommand = newAction(4, new BUtilitiesCommand(), null);
   public static final Action makeNvsNonCritical = newAction(0, null);
   public static final Topic deviceChange = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BLonNetwork.class);
   private static Type[] serviceTypes = new Type[]{TYPE};
   private boolean firstPass = true;
   private boolean disabledAtStart = true;
   private boolean serviceRunning = false;
   protected Worker workQueue = null;
   protected Worker coalesceQueue = null;
   protected Worker proxyQueue = null;
   protected Worker timedQueue = null;
   public int dataPntMismatchCount = 0;
   private Logger log = null;
   private NetMessageReceiver netMessageReceiver = null;
   private NLonComm lonComm = null;
   private NvManager nvManager = null;
   private AddressManager addressManager = null;
   private static BFacets noWrite = BFacets.make("noWrite", true);
   private static BFacets noPropagate = BFacets.make("noPropagate", true);
   public static final Context lonNoWrite = new BasicContext(noWrite) {
      public boolean equals(Object obj) {
         return obj != null && obj instanceof Context && ((Context)obj).getFacets().getb("noWrite", false);
      }

      public int hashCode() {
         return super.hashCode();
      }
   };
   public static final Context lonNoPropagate = new BasicContext(noPropagate) {
      public boolean equals(Object obj) {
         return obj != null && obj instanceof Context && ((Context)obj).getFacets().getb("noPropagate", false);
      }

      public int hashCode() {
         return super.hashCode();
      }
   };
   public static final Context lonNoPropagateNoWrite = new BasicContext(BFacets.make(noWrite, noPropagate));

   public BLonCommConfig getLonCommConfig() {
      return (BLonCommConfig)this.get(lonCommConfig);
   }

   public void setLonCommConfig(BLonCommConfig v) {
      this.set(lonCommConfig, v, null);
   }

   public BLonPollService getPollService() {
      return (BLonPollService)this.get(pollService);
   }

   public void setPollService(BLonPollService v) {
      this.set(pollService, v, null);
   }

   public BLonNetmgmt getLonNetmgmt() {
      return (BLonNetmgmt)this.get(lonNetmgmt);
   }

   public void setLonNetmgmt(BLonNetmgmt v) {
      this.set(lonNetmgmt, v, null);
   }

   public BLonTuningPolicyMap getTuningPolicies() {
      return (BLonTuningPolicyMap)this.get(tuningPolicies);
   }

   public void setTuningPolicies(BLonTuningPolicyMap v) {
      this.set(tuningPolicies, v, null);
   }

   public BLocalLonDevice getLocalLonDevice() {
      return (BLocalLonDevice)this.get(localLonDevice);
   }

   public void setLocalLonDevice(BLocalLonDevice v) {
      this.set(localLonDevice, v, null);
   }

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public BOrd executeCommand(BUtilitiesCommand parameter) {
      return (BOrd)this.invoke(executeCommand, parameter, null);
   }

   public void makeNvsNonCritical() {
      this.invoke(makeNvsNonCritical, null, null);
   }

   public void fireDeviceChange(BValue event) {
      this.fire(deviceChange, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "lonworks");
   }

   public final BOrd doExecuteCommand(BUtilitiesCommand cmd) {
      return new BUtilCmdJob(this, cmd).submit(null);
   }

   public final Type[] getServiceTypes() {
      return serviceTypes;
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list = NvDev.fixWireSheet(list, cx);
      AgentInfo[] aia = list.list();

      for (int i = 0; i < aia.length; i++) {
         if (aia[i].getAgentType().getTypeName().equals("LonDeviceManager") && i != 0) {
            list.toTop(i);
         }
      }

      return PxUtil.movePxViewsToTop(this.modifyAgentList(list, cx));
   }

   protected AgentList modifyAgentList(AgentList list, Context cx) {
      return list;
   }

   public final boolean isParentLegal(BComponent parent) {
      return !(parent instanceof BLonNetwork);
   }

   public void checkAdd(String name, BValue value, int flags, BFacets facets, Context context) {
      if (value instanceof BLocalLonDevice) {
         throw new LocalizableRuntimeException("lonworks", "addLocalDeviceError");
      }
   }

   public void added(Property prop, Context context) {
      try {
         if (!this.isRunning()) {
            return;
         }

         if (prop.getType().is(BLonDevice.TYPE)) {
            this.netmgmt().deviceAdded((BLonDevice)this.get(prop));
         } else if (prop.getType().is(BLonRouter.TYPE)) {
            this.netmgmt().routerAdded((BLonRouter)this.get(prop));
         }
      } finally {
         super.added(prop, context);
      }
   }

   public final Type getDeviceType() {
      return BLonDevice.TYPE;
   }

   public final Type getDeviceFolderType() {
      return BLonDeviceFolder.TYPE;
   }

   public final void doUpload(BUploadParameters p, Context cx) throws Exception {
      new BUploadJob(this, p, cx).submit(cx);
   }

   public final void doDownload(BDownloadParameters p, Context cx) throws Exception {
      new BDownloadJob(this, p, cx).submit(cx);
   }

   protected IFuture postPing() {
      return this.postAsync(new Invocation(this, ping, null, null));
   }

   public void doPing() throws Exception {
      Object[] pa = NmUtil.getDecendantsByClass(this, BLonDevice.class);

      for (int i = 0; i < pa.length; i++) {
         ((BLonDevice)pa[i]).doPing();
      }

      this.pingOk();
   }

   public void doMakeNvsNonCritical() {
      BLonDevice[] devA = this.getLonDevices();

      for (int i = 0; i < devA.length; i++) {
         BINetworkVariable[] nvs = devA[i].getNetworkVariables();

         for (int n = 0; n < nvs.length; n++) {
            if (nvs[n] != null && (nvs[n].isNetworkVariable() || nvs[n].isLocalNv())) {
               DynaDev.setNonCritical((BLonData)nvs[n]);
            }
         }
      }
   }

   public final void serviceStarted() {
      if (this.firstPass) {
         this.lonComm = new NLonComm(this);
         this.netMessageReceiver = new NetMessageReceiver(this);
         this.nvManager = new NvManager(this);
         this.addressManager = new NAddressManager(this);
         this.workQueue = new Worker(new Queue(1000));
         this.coalesceQueue = new Worker(new CoalesceQueue(5000));
         this.proxyQueue = new Worker(new Queue(1000));
         this.timedQueue = new Worker(new TimedCoalesceQueue(1000));
         this.firstPass = false;
      }

      if (!this.isDisabled()) {
         this.disabledAtStart = false;

         try {
            this.lonComm.start();
            this.workQueue.start(this.getLogName() + ".Async");
            this.setWorkQueuePriority();
            this.coalesceQueue.start(this.getLogName() + ".AsyncEvent");
            this.proxyQueue.start(this.getLogName() + ".Proxy");
            this.timedQueue.start(this.getLogName() + ".Delay");
            this.log().info("Service started on " + this.getLonCommConfig().getDeviceName());
            this.setStatus(BStatus.make(this.getStatus(), 4, false));
            this.serviceRunning = true;
            this.configOk();
         } catch (Throwable var2) {
            this.log().log(Level.SEVERE, "Error initializing LonNetwork " + this.getDisplayName(null), var2);
            this.configFail(var2.getMessage());
         }
      }
   }

   public final void serviceStopped() {
      this.serviceRunning = false;
      this.setStatus(BStatus.make(this.getStatus(), 4, true));
      this.workQueue.stop();
      this.coalesceQueue.stop();
      this.proxyQueue.stop();
      this.timedQueue.stop();
      this.lonComm.stop();
      this.log().info("Service stopped on " + this.getLonCommConfig().getDeviceName());
   }

   public boolean isServiceRunning() {
      return this.serviceRunning;
   }

   public void started() throws Exception {
      try {
         if (!this.isServiceRunning() && !this.isDisabled() && !this.getStatus().isFault()) {
            this.serviceStarted();
         }
      } finally {
         super.started();
      }
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == enabled) {
            boolean restartDevs = false;
            if (!this.isDisabled() && !this.isServiceRunning()) {
               if (this.disabledAtStart) {
                  restartDevs = true;
               }

               this.serviceStarted();
            }

            if (this.isDisabled() && this.isServiceRunning()) {
               this.serviceStopped();
            }

            if (restartDevs) {
               BLonDevice[] devs = (BLonDevice[])NmUtil.getDecendantsByClass(this, BLonDevice.class);

               for (int i = 0; i < devs.length; i++) {
                  try {
                     devs[i].started();
                  } catch (Exception var9) {
                  }
               }
            }
         }

         if (prop == lonCommConfig) {
            if (!this.isServiceRunning() && !this.isDisabled()) {
               this.serviceStarted();
               if (this.isServiceRunning()) {
                  BLonDevice[] devs = (BLonDevice[])NmUtil.getDecendantsByClass(this, BLonDevice.class);

                  for (int i = 0; i < devs.length; i++) {
                     try {
                        devs[i].started();
                     } catch (Exception var8) {
                     }
                  }
               }
            } else if (this.isServiceRunning()) {
               try {
                  this.lonComm.verifySettings();
                  this.configOk();
               } catch (Throwable var7) {
                  this.log().log(Level.SEVERE, "Error initializing LonNetwork " + this.getDisplayName(null), var7);
                  this.configFail(var7.getMessage());
               }
            }
         }
      }
   }

   public BINavNode[] getNavChildren() {
      BINavNode[] kids = super.getNavChildren();
      Array<BINavNode> acc = new Array(BINavNode.class);
      acc.add(this.getLocalLonDevice());

      for (int i = 0; i < kids.length; i++) {
         acc.add(kids[i]);
      }

      return (BINavNode[])acc.trim();
   }

   public String getLogName() {
      return this.getLonCommConfig().getDeviceName().toLowerCase();
   }

   public final IFuture postAsync(Runnable t) {
      if (!this.isServiceRunning()) {
         return null;
      } else if (this.workQueue != null && this.workQueue.isRunning()) {
         ((Queue)this.workQueue.getTodo()).enqueue(t);
         return null;
      } else {
         throw new NotRunningException();
      }
   }

   private void setWorkQueuePriority() {
      Runnable t = new Runnable() {
         @Override
         public void run() {
            Thread.currentThread().setPriority(6);
         }
      };
      ((Queue)this.workQueue.getTodo()).enqueue(t);
   }

   public final IFuture postWrite(Runnable t) {
      if (!this.isServiceRunning()) {
         return null;
      } else if (this.coalesceQueue != null && this.coalesceQueue.isRunning()) {
         ((Queue)this.coalesceQueue.getTodo()).enqueue(t);
         return null;
      } else {
         throw new NotRunningException();
      }
   }

   public final Worker getProxyQueue() {
      return this.proxyQueue;
   }

   public final Worker getTimedQueue() {
      return this.timedQueue;
   }

   public final LonComm lonComm() {
      return this.lonComm;
   }

   public final NetMessageReceiver netMessageReceiver() {
      return this.netMessageReceiver;
   }

   public final NvManager nvManager() {
      return this.nvManager;
   }

   public final AddressManager addressManager() {
      return this.addressManager;
   }

   public final BLonNetmgmt netmgmt() {
      return this.getLonNetmgmt();
   }

   public final BLonRouter[] getLonRouters() {
      return NmUtil.getLonRouters(this);
   }

   public final BLonDevice[] getLonDevices() {
      return NmUtil.getLonDevices(this);
   }

   public final BLonDevice findDevice(BNeuronId nid) {
      BLonDevice[] devs = this.getLonDevices();

      for (int i = 0; i < devs.length; i++) {
         if (devs[i].getNeuronIdAddress().equals(nid)) {
            return devs[i];
         }
      }

      return null;
   }

   public final BLonRouter findRouter(BNeuronId nid) {
      BLonRouter[] rtrs = this.getLonRouters();

      for (int i = 0; i < rtrs.length; i++) {
         if (rtrs[i].getNeuronIdAddress().equals(nid)) {
            return rtrs[i];
         }
      }

      return null;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps("BLonNetwork");
      out.prop("serviceRunning", this.serviceRunning);
      out.prop("dataPntMismatchCount", this.dataPntMismatchCount);
      out.endProps();
      if (this.workQueue != null) {
         out.trTitle("Async Queue", 1);
         this.workQueue.spy(out);
      }

      if (this.coalesceQueue != null) {
         out.trTitle("Coalescing Write Queue", 1);
         this.coalesceQueue.spy(out);
      }

      if (this.proxyQueue != null) {
         out.trTitle("Proxy Queue", 1);
         this.proxyQueue.spy(out);
      }

      if (this.timedQueue != null) {
         out.trTitle("Delay Queue", 1);
         this.timedQueue.spy(out);
         ((TimedCoalesceQueue)this.timedQueue.getTodo()).spy(out);
      }

      if (this.nvManager != null) {
         out.trTitle("NvManager", 1);
         this.nvManager.spy(out);
      }

      if (this.addressManager != null) {
         out.trTitle("Address Manager", 1);
         ((NAddressManager)this.addressManager).spy(out);
      }

      NAppBuffer.spy(out);
      if (this.lonComm != null) {
         this.lonComm.spy(out);
      }
   }

   public final Logger log() {
      if (this.log == null) {
         this.log = Logger.getLogger(this.getLogName());
      }

      return this.log;
   }
}
