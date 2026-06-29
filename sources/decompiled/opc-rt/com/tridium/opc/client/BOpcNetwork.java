package com.tridium.opc.client;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.OpcException;
import com.tridium.opc.client.point.BOpcPointDeviceExt;
import com.tridium.opc.client.util.BOpcThreadPool;
import com.tridium.opc.client.util.BOpcTuningPolicyMap;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.client.common.OpcCommon;
import com.tridium.opc.jni.client.common.OpcServerList2;
import com.tridium.opc.jni.client.da.OpcAsyncIo2;
import com.tridium.opc.jni.client.da.OpcBrowse;
import com.tridium.opc.jni.client.da.OpcBrowseServerAddressSpace;
import com.tridium.opc.jni.client.da.OpcDaServer;
import com.tridium.opc.jni.client.da.OpcGroup;
import com.tridium.opc.jni.client.da.OpcGroupStateMgt;
import com.tridium.opc.jni.client.da.OpcItem;
import com.tridium.opc.jni.client.da.OpcItemMgt;
import com.tridium.opc.jni.client.da.OpcItemProperties;
import com.tridium.opc.jni.client.da.OpcNTSecurity;
import com.tridium.opc.jni.client.da.OpcPrivateSecurity;
import com.tridium.opc.jni.client.da.OpcSyncIo;
import javax.baja.driver.BDeviceNetwork;
import javax.baja.driver.BDriverContainer;
import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.license.Feature;
import javax.baja.log.Log;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.CoalesceQueue;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;
import javax.baja.util.Lexicon;
import javax.baja.util.Worker;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "threadPool",
      type = "BOpcThreadPool",
      defaultValue = "new BOpcThreadPool()"
   ), @NiagaraProperty(
      name = "tuningPolicies",
      type = "BTuningPolicyMap",
      defaultValue = "new BOpcTuningPolicyMap()"
   )})
@NiagaraActions({@NiagaraAction(
      name = "initEnv",
      flags = 20
   ), @NiagaraAction(
      name = "submitDeviceDiscoveryJob",
      parameterType = "BValue",
      defaultValue = "BString.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "testNative",
      flags = 4
   )})
public class BOpcNetwork extends BDeviceNetwork {
   public static final Property threadPool = newProperty(0, new BOpcThreadPool(), null);
   public static final Property tuningPolicies = newProperty(0, new BOpcTuningPolicyMap(), null);
   public static final Action initEnv = newAction(20, null);
   public static final Action submitDeviceDiscoveryJob = newAction(4, BString.DEFAULT, null);
   public static final Action testNative = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BOpcNetwork.class);
   Worker rdWorker;
   CoalesceQueue rdQueue = new CoalesceQueue(5);
   Worker wrWorker;
   CoalesceQueue wrQueue = new CoalesceQueue(5);
   long enqueueCount = 0L;
   static Lexicon lexicon = null;
   Log opcLog = Log.getLog("OpcDaLog");

   public BOpcThreadPool getThreadPool() {
      return (BOpcThreadPool)this.get(threadPool);
   }

   public void setThreadPool(BOpcThreadPool v) {
      this.set(threadPool, v, null);
   }

   public BTuningPolicyMap getTuningPolicies() {
      return (BTuningPolicyMap)this.get(tuningPolicies);
   }

   public void setTuningPolicies(BTuningPolicyMap v) {
      this.set(tuningPolicies, v, null);
   }

   public void initEnv() {
      this.invoke(initEnv, null, null);
   }

   public BOrd submitDeviceDiscoveryJob(BValue parameter) {
      return (BOrd)this.invoke(submitDeviceDiscoveryJob, parameter, null);
   }

   public void testNative() {
      this.invoke(testNative, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void descendantsStarted() throws Exception {
      this.initEnv();
      super.descendantsStarted();
   }

   public void doInitEnv() {
      OpcEnv.initializeEnv();
   }

   public final BOrd doSubmitDeviceDiscoveryJob(BValue args, Context cx) {
      return new BOpcDeviceDiscoveryJob(this, args).submit(cx);
   }

   public final void doTestNative() {
      try {
         OpcEnv.initializeEnv();
         OpcEnv.initializeThread();
         ComObjectClient client = new ComObjectClient();

         try {
            client.release();
         } catch (OpcException var39) {
         }

         try {
            client.query(OpcCommon.IID);
         } catch (OpcException var38) {
         }

         OpcCommon common = new OpcCommon();
         common.availableLocales();

         try {
            common.getErrorString(1);
         } catch (OpcException var37) {
         }

         try {
            common.getLocale();
         } catch (OpcException var36) {
         }

         try {
            common.setClientName("foo");
         } catch (OpcException var35) {
         }

         try {
            common.setLocale(1);
         } catch (OpcException var34) {
         }

         OpcServerList2 serverList2 = new OpcServerList2();

         try {
            serverList2.discoverServersInNetwork("foo", "test");
         } catch (OpcException var33) {
         }

         OpcAsyncIo2 asyncIo2 = new OpcAsyncIo2();
         int[] handles = new int[]{0};
         String[] strings = new String[]{null};

         try {
            asyncIo2.readAsync(0, 0, handles);
         } catch (OpcException var32) {
         }

         try {
            asyncIo2.writeAsync(0, 0, handles, handles, strings);
         } catch (OpcException var31) {
         }

         try {
            asyncIo2.cancel2(handles, handles);
         } catch (OpcException var30) {
         }

         try {
            asyncIo2.registerCallback(null);
         } catch (OpcException var29) {
         }

         try {
            asyncIo2.unregisterCallback(1000L);
         } catch (OpcException var28) {
         }

         asyncIo2.refresh(true);
         OpcBrowse opcBrowse = new OpcBrowse();
         opcBrowse.browse("foo");
         OpcBrowseServerAddressSpace space = new OpcBrowseServerAddressSpace();
         space.browse(new String[]{"foo"}, null);
         space.getGroups("foo");
         space.getGroups(new String[]{"foo"});
         space.getItems("foo", true);
         space.getItems(new String[]{"foo"});
         space.queryOrganization();
         OpcDaServer server = new OpcDaServer();
         server.getStatus();

         try {
            OpcDaServer.newServer("foo");
         } catch (OpcException var27) {
         }

         try {
            OpcDaServer.newServer("foo", "foo");
         } catch (OpcException var26) {
         }

         server.addGroup("foo", true, 0, 0, 0, 0.0F, 0);
         server.removeGroup(new OpcGroup("foo", 0L));
         OpcGroupStateMgt stateMgt = new OpcGroupStateMgt();

         try {
            stateMgt.getState();
         } catch (OpcException var25) {
         }

         try {
            stateMgt.setState(0, true, 0, 0.0F, 0, 0);
         } catch (OpcException var24) {
         }

         OpcItemMgt itemMgt = new OpcItemMgt();

         try {
            itemMgt.addItems(new String[]{"foo"}, new int[]{0}, new boolean[]{false}, new int[]{0});
         } catch (OpcException var23) {
         }

         itemMgt.removeItems(new int[]{0});
         itemMgt.setActiveState(new int[]{0}, false);

         try {
            itemMgt.validateItems(new String[]{"foo"}, new int[]{0}, new boolean[]{false}, new int[]{0});
         } catch (OpcException var22) {
         }

         OpcItemProperties properties = new OpcItemProperties();
         properties.getBooleanProperty(new OpcItem("foo", "foo"), new OpcItem.Property(0, "foo", 0));
         properties.getNumericProperty(new OpcItem("foo", "foo"), new OpcItem.Property(0, "foo", 0));
         properties.getStringProperty(new OpcItem("foo", "foo"), new OpcItem.Property(0, "foo", 0));
         properties.queryAvailableProperties(new OpcItem("foo", "foo"));
         OpcNTSecurity security = new OpcNTSecurity();
         security.changeUser("foo", "foo");
         security.isAvailableNT();
         security.queryImpersonationLevel();
         OpcPrivateSecurity privateSecurity = new OpcPrivateSecurity();
         privateSecurity.isAvailablePrivate();
         privateSecurity.logOn("foo", "foo");
         privateSecurity.logOff();
         OpcSyncIo syncIo = new OpcSyncIo();

         try {
            syncIo.read(new int[]{0}, false);
         } catch (OpcException var21) {
         }

         try {
            syncIo.writeBoolean(0, false);
         } catch (OpcException var20) {
         }

         try {
            syncIo.writeNumeric(0, 0, 0.0);
         } catch (OpcException var19) {
         }

         try {
            syncIo.writeString(0, "foo", 0);
         } catch (OpcException var18) {
         }

         try {
            syncIo.writeArray(0, new int[]{0}, new int[]{0}, new String[]{"foo"});
         } catch (OpcException var17) {
         }
      } catch (Error var40) {
         this.opcLog.error("Failed native test:");
         var40.printStackTrace();
      }
   }

   public final void enqueue(Runnable r) {
      this.getThreadPool().post(r);
   }

   public CoalesceQueue getRdQueue() {
      return this.rdQueue;
   }

   public CoalesceQueue getWrQueue() {
      return this.wrQueue;
   }

   public final Type getDeviceFolderType() {
      return BOpcDeviceFolder.TYPE;
   }

   public final Type getDeviceType() {
      return BOpcDevice.TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "opc");
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BDriverContainer;
   }

   public String lex(String key) {
      if (lexicon == null) {
         lexicon = Lexicon.make(this.getType().getTypeClass());
      }

      return lexicon.getText(key);
   }

   public final IFuture post(Action a, BValue arg, Context cx) {
      if (a.equals(BOpcPointDeviceExt.read)) {
         this.rdQueue.enqueue(new Invocation(this, a, arg, cx));
      } else {
         if (!a.equals(BOpcPointDeviceExt.write)) {
            this.enqueue(new Invocation(this, a, arg, cx));
            return null;
         }

         this.wrQueue.enqueue(new Invocation(this, a, arg, cx));
      }

      return null;
   }

   public void started() throws Exception {
      OpcEnv.setLog(new BOpcNetwork.NativeLog());
      this.rdWorker = new Worker(this.rdQueue);
      this.wrWorker = new Worker(this.wrQueue);
      this.rdWorker.start("OpcRd");
      this.wrWorker.start("Opcwr");
      super.started();
   }

   public void stopped() throws Exception {
      if (this.rdWorker != null) {
         this.rdWorker.stop();
      }

      if (this.wrWorker != null) {
         this.wrWorker.stop();
      }

      this.rdWorker = null;
      this.wrWorker = null;
   }

   private class NativeLog implements com.tridium.opc.OpcEnv.Log {
      private NativeLog() {
      }

      public void error(String msg, Throwable t) {
         BOpcNetwork.this.opcLog.error(msg, t);
      }

      public void message(String msg, Throwable t) {
         BOpcNetwork.this.opcLog.message(msg, t);
      }

      public void trace(String msg, Throwable t) {
         BOpcNetwork.this.opcLog.trace(msg, t);
      }

      public void warning(String msg, Throwable t) {
         BOpcNetwork.this.opcLog.warning(msg, t);
      }
   }
}
