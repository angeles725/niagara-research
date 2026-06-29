package com.tridium.opc.client.point;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.client.BOpcDaClient;
import com.tridium.opc.client.BOpcDevice;
import com.tridium.opc.client.BOpcNetwork;
import com.tridium.opc.client.util.BOpcQuality;
import com.tridium.opc.client.util.BOpcQualityBad;
import com.tridium.opc.client.util.BOpcReadMode;
import com.tridium.opc.client.util.BOpcState;
import com.tridium.opc.client.util.BOpcWriteMode;
import com.tridium.opc.jni.client.da.OpcAsyncIo2;
import com.tridium.opc.jni.client.da.OpcGroup;
import com.tridium.opc.jni.client.da.OpcItem;
import com.tridium.opc.jni.client.da.OpcItemMgt;
import com.tridium.opc.jni.client.da.OpcItemProperties;
import com.tridium.opc.jni.client.da.OpcSyncIo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.control.trigger.BIntervalTriggerMode;
import javax.baja.control.trigger.BTimeTrigger;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.driver.point.BTuningPolicy;
import javax.baja.driver.point.BTuningPolicyMap;
import javax.baja.log.Log;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "readDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.make(2000)"
   ), @NiagaraProperty(
      name = "writeDelay",
      type = "BRelTime",
      defaultValue = "BRelTime.make(4000)"
   ), @NiagaraProperty(
      name = "groupName",
      type = "String",
      defaultValue = "CertUtils.LEGACY_CERT_ALIAS"
   ), @NiagaraProperty(
      name = "percentDeadband",
      type = "double",
      defaultValue = "0.0"
   ), @NiagaraProperty(
      name = "updateRate",
      type = "int",
      defaultValue = "1000",
      facets = {@Facet("BFacets.make(BFacets.UNITS, BUnit.getUnit(\"millisecond\"))")}
   ), @NiagaraProperty(
      name = "revisedUpdateRate",
      type = "int",
      defaultValue = "0",
      flags = 3,
      facets = {@Facet("BFacets.make(BFacets.UNITS, BUnit.getUnit(\"millisecond\"))")}
   ), @NiagaraProperty(
      name = "batchLimit",
      type = "int",
      defaultValue = "500",
      facets = {@Facet("BFacets.make(BFacets.MIN, BDouble.make(25.0D))")}
   ), @NiagaraProperty(
      name = "message",
      type = "String",
      defaultValue = "",
      flags = 67
   ), @NiagaraProperty(
      name = "messageTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "serverHandle",
      type = "int",
      defaultValue = "0",
      flags = 67
   ), @NiagaraProperty(
      name = "state",
      type = "BOpcState",
      defaultValue = "BOpcState.detached",
      flags = 3
   ), @NiagaraProperty(
      name = "asyncIo",
      type = "boolean",
      defaultValue = "false",
      flags = 67
   ), @NiagaraProperty(
      name = "readMode",
      type = "BOpcReadMode",
      defaultValue = "BOpcReadMode.cov"
   ), @NiagaraProperty(
      name = "writeMode",
      type = "BOpcWriteMode",
      defaultValue = "BOpcWriteMode.async"
   ), @NiagaraProperty(
      name = "WriteAsACollection",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "defaultPointFacets",
      type = "BFacets",
      defaultValue = "BFacets.NULL"
   ), @NiagaraProperty(
      name = "readTrigger",
      type = "BTimeTrigger",
      defaultValue = "new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeSeconds(1)))",
      flags = 4
   ), @NiagaraProperty(
      name = "writeTrigger",
      type = "BTimeTrigger",
      defaultValue = "new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeSeconds(1)))",
      flags = 4
   ), @NiagaraProperty(
      name = "flatDiscovery",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "maxReadWriteWaitTime",
      type = "int",
      defaultValue = "15",
      flags = 4
   ), @NiagaraProperty(
      name = "useThreadPool",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "writeState",
      type = "int",
      defaultValue = "0",
      flags = 2
   )})
@NiagaraActions({@NiagaraAction(
      name = "getDeviceOrd",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "read",
      flags = 20
   ), @NiagaraAction(
      name = "submitPointDiscoveryJob",
      parameterType = "BValue",
      defaultValue = "BString.DEFAULT",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "write",
      flags = 20
   ), @NiagaraAction(
      name = "clearWritingFlag"
   )})
public class BOpcPointDeviceExt extends BPointDeviceExt {
   public static final Property readDelay = newProperty(0, BRelTime.make(2000L), null);
   public static final Property writeDelay = newProperty(0, BRelTime.make(4000L), null);
   public static final Property groupName = newProperty(0, "tridium", null);
   public static final Property percentDeadband = newProperty(0, 0.0, null);
   public static final Property updateRate = newProperty(0, 1000, BFacets.make("units", BUnit.getUnit("millisecond")));
   public static final Property revisedUpdateRate = newProperty(3, 0, BFacets.make("units", BUnit.getUnit("millisecond")));
   public static final Property batchLimit = newProperty(0, 500, BFacets.make("min", BDouble.make(25.0)));
   public static final Property message = newProperty(67, "", null);
   public static final Property messageTime = newProperty(67, BAbsTime.NULL, null);
   public static final Property serverHandle = newProperty(67, 0, null);
   public static final Property state = newProperty(3, BOpcState.detached, null);
   public static final Property asyncIo = newProperty(67, false, null);
   public static final Property readMode = newProperty(0, BOpcReadMode.cov, null);
   public static final Property writeMode = newProperty(0, BOpcWriteMode.async, null);
   public static final Property WriteAsACollection = newProperty(5, false, null);
   public static final Property defaultPointFacets = newProperty(0, BFacets.NULL, null);
   public static final Property readTrigger = newProperty(4, new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeSeconds(1))), null);
   public static final Property writeTrigger = newProperty(4, new BTimeTrigger(BIntervalTriggerMode.make(BRelTime.makeSeconds(1))), null);
   public static final Property flatDiscovery = newProperty(0, false, null);
   public static final Property maxReadWriteWaitTime = newProperty(4, 15, null);
   public static final Property useThreadPool = newProperty(0, false, null);
   public static final Property writeState = newProperty(2, 0, null);
   public static final Action getDeviceOrd = newAction(4, null);
   public static final Action read = newAction(20, null);
   public static final Action submitPointDiscoveryJob = newAction(4, BString.DEFAULT, null);
   public static final Action write = newAction(20, null);
   public static final Action clearWritingFlag = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BOpcPointDeviceExt.class);
   private BAbsTime attachTime;
   private BOpcDaClient daClient;
   private TreeMap<Object, BOpcProxyExt> extMap;
   private TreeMap<Integer, BOpcProxyExt> extServerHandleMap;
   private Object mutexRead = new Object();
   private Object mutexSubscribe = new Object();
   private Object mutexUnsubscribe = new Object();
   private Object mutexWrite = new Object();
   private Object mutexReadWriteInProgress = new Object();
   private Object mutexAsyncReadMap = new Object();
   private Object mutexAsyncWriteMap = new Object();
   private Object mutexUnregisterCallbackInProgress = new Object();
   private OpcGroup peer = null;
   private TreeMap<Object, BOpcProxyExt> pendingRead = new TreeMap<>();
   private TreeMap<Object, BOpcProxyExt> pendingSubscribe = new TreeMap<>();
   private TreeMap<Object, BOpcProxyExt> pendingUnsubscribe = new TreeMap<>();
   private TreeMap<Object, BOpcProxyExt> pendingWrite = new TreeMap<>();
   private volatile boolean reading = false;
   private volatile boolean writing = false;
   private long writingTicks = 0L;
   private long readingTicks = 0L;
   private int writingState = 0;
   private long writingBusyCount = 0L;
   private long readingBusyCount = 0L;
   private long writingClearedCount = 0L;
   private long readingClearedCount = 0L;
   private long doWriteCount = 0L;
   private long doReadCount = 0L;
   private boolean waitingWr = false;
   private long lastWriteTime = -1L;
   private long lastTotalWriteTime = -1L;
   private BOpcPointDeviceExt.Counter readTxsId = new BOpcPointDeviceExt.Counter();
   private BOpcPointDeviceExt.Counter writeTxsId = new BOpcPointDeviceExt.Counter();
   private TreeMap<Integer, Integer> extAsyncWriteMap = new TreeMap<>();
   private TreeMap<Integer, Integer> extAsyncReadMap = new TreeMap<>();
   private boolean isFirstTime = true;
   public static final int WR_WRITING_TRUE = 1;
   public static final int WR_POSTED = 2;
   public static final int WR_DOWRITE_ENTERED = 3;
   public static final int WR_MUTEX_BLOCK = 4;
   public static final int WR_WRITE_ASYNC_CALL = 5;
   public static final int WR_WRITE_ASYNC_RTN = 6;
   public static final int WR_DOWRITE_FINALLY = 7;
   Log opcLog = Log.getLog("OpcDaLog");
   Log opcWriteLog = Log.getLog("OpcDaWriteLog");
   Log opcSubscriptionLog = Log.getLog("OpcDaSubLog");

   public BRelTime getReadDelay() {
      return (BRelTime)this.get(readDelay);
   }

   public void setReadDelay(BRelTime v) {
      this.set(readDelay, v, null);
   }

   public BRelTime getWriteDelay() {
      return (BRelTime)this.get(writeDelay);
   }

   public void setWriteDelay(BRelTime v) {
      this.set(writeDelay, v, null);
   }

   public String getGroupName() {
      return this.getString(groupName);
   }

   public void setGroupName(String v) {
      this.setString(groupName, v, null);
   }

   public double getPercentDeadband() {
      return this.getDouble(percentDeadband);
   }

   public void setPercentDeadband(double v) {
      this.setDouble(percentDeadband, v, null);
   }

   public int getUpdateRate() {
      return this.getInt(updateRate);
   }

   public void setUpdateRate(int v) {
      this.setInt(updateRate, v, null);
   }

   public int getRevisedUpdateRate() {
      return this.getInt(revisedUpdateRate);
   }

   public void setRevisedUpdateRate(int v) {
      this.setInt(revisedUpdateRate, v, null);
   }

   public int getBatchLimit() {
      return this.getInt(batchLimit);
   }

   public void setBatchLimit(int v) {
      this.setInt(batchLimit, v, null);
   }

   public String getMessage() {
      return this.getString(message);
   }

   public void setMessage(String v) {
      this.setString(message, v, null);
   }

   public BAbsTime getMessageTime() {
      return (BAbsTime)this.get(messageTime);
   }

   public void setMessageTime(BAbsTime v) {
      this.set(messageTime, v, null);
   }

   public int getServerHandle() {
      return this.getInt(serverHandle);
   }

   public void setServerHandle(int v) {
      this.setInt(serverHandle, v, null);
   }

   public BOpcState getState() {
      return (BOpcState)this.get(state);
   }

   public void setState(BOpcState v) {
      this.set(state, v, null);
   }

   public boolean getAsyncIo() {
      return this.getBoolean(asyncIo);
   }

   public void setAsyncIo(boolean v) {
      this.setBoolean(asyncIo, v, null);
   }

   public BOpcReadMode getReadMode() {
      return (BOpcReadMode)this.get(readMode);
   }

   public void setReadMode(BOpcReadMode v) {
      this.set(readMode, v, null);
   }

   public BOpcWriteMode getWriteMode() {
      return (BOpcWriteMode)this.get(writeMode);
   }

   public void setWriteMode(BOpcWriteMode v) {
      this.set(writeMode, v, null);
   }

   public boolean getWriteAsACollection() {
      return this.getBoolean(WriteAsACollection);
   }

   public void setWriteAsACollection(boolean v) {
      this.setBoolean(WriteAsACollection, v, null);
   }

   public BFacets getDefaultPointFacets() {
      return (BFacets)this.get(defaultPointFacets);
   }

   public void setDefaultPointFacets(BFacets v) {
      this.set(defaultPointFacets, v, null);
   }

   public BTimeTrigger getReadTrigger() {
      return (BTimeTrigger)this.get(readTrigger);
   }

   public void setReadTrigger(BTimeTrigger v) {
      this.set(readTrigger, v, null);
   }

   public BTimeTrigger getWriteTrigger() {
      return (BTimeTrigger)this.get(writeTrigger);
   }

   public void setWriteTrigger(BTimeTrigger v) {
      this.set(writeTrigger, v, null);
   }

   public boolean getFlatDiscovery() {
      return this.getBoolean(flatDiscovery);
   }

   public void setFlatDiscovery(boolean v) {
      this.setBoolean(flatDiscovery, v, null);
   }

   public int getMaxReadWriteWaitTime() {
      return this.getInt(maxReadWriteWaitTime);
   }

   public void setMaxReadWriteWaitTime(int v) {
      this.setInt(maxReadWriteWaitTime, v, null);
   }

   public boolean getUseThreadPool() {
      return this.getBoolean(useThreadPool);
   }

   public void setUseThreadPool(boolean v) {
      this.setBoolean(useThreadPool, v, null);
   }

   public int getWriteState() {
      return this.getInt(writeState);
   }

   public void setWriteState(int v) {
      this.setInt(writeState, v, null);
   }

   public BOrd getDeviceOrd() {
      return (BOrd)this.invoke(getDeviceOrd, null, null);
   }

   public void read() {
      this.invoke(read, null, null);
   }

   public BOrd submitPointDiscoveryJob(BValue parameter) {
      return (BOrd)this.invoke(submitPointDiscoveryJob, parameter, null);
   }

   public void write() {
      this.invoke(write, null, null);
   }

   public void clearWritingFlag() {
      this.invoke(clearWritingFlag, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void attach() {
      if (this.getState().isEngaged()) {
         this.opcLog.trace("PointDeviceExt::Attach()--Already attached. Returning");
      } else {
         this.setAttaching();
         this.resetFlags();
         this.opcLog.trace("PointDeviceExt::Attach()--Attaching");
         BOpcDaClient server = this.getDaClient();
         if (server != null && server.getState().isAttached()) {
            String addName = this.getGroupName();
            this.peer = server.getPeer().getGroup(addName);
            if (this.peer == null) {
               this.opcLog.trace("Add group");

               try {
                  this.peer = server.getPeer()
                     .addGroup(addName, true, this.getUpdateRate(), this.getHandle().hashCode(), 0, (float)this.getPercentDeadband(), 0);
                  if (this.peer == null) {
                     this.message("Add group failed - peer is equal to NULL");
                     this.opcLog.error("Add group: " + addName);
                     this.setDetached();
                  } else {
                     this.setRevisedUpdateRate(this.peer.revisedUpdateRate);
                     this.setServerHandle(this.peer.serverHandle);
                     this.setAttached();
                     this.opcLog.trace("PointDeviceExt::Attach()--Attached successfully");
                     this.isFirstTime = true;
                     this.attachTime = Clock.time();
                     BTuningPolicyMap objTuningPolicyMap = this.getOpcNetwork().getTuningPolicies();
                     BTuningPolicy objPolicy = objTuningPolicyMap.getDefaultPolicy();
                     if (!objPolicy.getWriteOnUp()) {
                        BControlPoint[] points = this.getPoints();

                        for (int i = 0; i < points.length; i++) {
                           BAbstractProxyExt ext = points[i].getProxyExt();
                           if (ext instanceof BOpcProxyExt) {
                              ((BOpcProxyExt)ext).writeReset();
                           }
                        }
                     }

                     this.peer.setGroupListener(new BOpcPointDeviceExt.Listener());
                     this.registerCallback();
                  }
               } catch (Exception var8) {
                  this.message("Add group failed (" + var8.getMessage() + ")");
                  this.opcLog.error("Add group: " + addName, var8);
                  this.setDetached();
               }
            } else {
               this.message("Group name already in use.");
               this.opcLog.error("Duplicate group name [" + addName + "] " + this.toPathString());
               this.setDetached();
            }
         } else {
            this.opcLog.trace("PointDeviceExt::Attach()--Server is not attached. Returning");
            this.setDetached();
         }
      }
   }

   public void detach() {
      this.cancelTransaction();
      this.setDetaching();
      BOpcDaClient server = this.getDaClient();
      this.opcLog.trace("Removing group");
      if (this.peer != null && !server.isDown() && !server.isFault()) {
         try {
            this.peer.setGroupListener(null);
            server.getPeer().removeGroup(this.peer);
            this.opcLog.trace("PointDeviceExt::detach()--Removed group");
            this.setAsyncIo(false);
         } catch (Exception var6) {
            this.opcLog.error("Removing group", var6);
            this.setAsyncIo(false);
         } finally {
            this.setDetached();
            this.setServerHandle(0);
            this.isFirstTime = false;
            this.peer = null;
         }
      }

      this.setDetached();
      this.opcLog.trace("PointDeviceExt::detach()--Detached");
   }

   public void doRead() {
      try {
         this.doReadCount++;
         if (!this.isRunning()) {
            this.opcLog.trace("PointDeviceExt::doRead()-Not running. Returning");
            return;
         }

         if (!this.getState().isAttached()) {
            this.opcLog.trace("PointDeviceExt::doRead()-Not attached. Returning");
            return;
         }

         if (this.peer == null) {
            this.opcLog.trace("PointDeviceExt::doRead()-Peer is null. Returning");
            return;
         }

         if (!Clock.time().isBefore(this.attachTime.add(this.getReadDelay()))) {
            this.checkReadWriteMode();
            this.performUnsubscribe();
            this.performSubscribe();
            if (this.getReadMode() != BOpcReadMode.cov) {
               this.performRead();
            }

            return;
         }

         this.opcLog.trace("PointDeviceExt::doRead()-Poll time is less than read delay. Returning");
      } finally {
         this.reading = false;
      }
   }

   public final BOrd doGetDeviceOrd() {
      return this.getOpcDevice().getSlotPathOrd();
   }

   public final BOrd doSubmitPointDiscoveryJob(BValue args, Context cx) {
      return new BOpcPointDiscoveryJob(this, BString.make("")).submit(cx);
   }

   public void doClearWritingFlag() {
      if (this.writing) {
         this.opcWriteLog.message("doClearWritingFlag(): writing flag is set and will be reset.");
         this.writing = false;
      } else {
         this.opcWriteLog.message("doClearWritingFlag(): writing flag is not set.");
      }
   }

   public void doWrite() {
      long writeStart = Clock.ticks();
      this.writingState = 3;
      this.setWriteState(this.writingState);
      this.doWriteCount++;
      OpcAsyncIo2 AsyncIo2 = null;
      OpcSyncIo syncIo = null;
      int len = 0;
      int count = 0;
      int hresult = 0;

      try {
         if (!this.isRunning()) {
            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-Not running. Returning");
         } else if (!this.getState().isAttached()) {
            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-Not attached. Returning");
         } else if (Clock.time().isBefore(this.attachTime.add(this.getWriteDelay()))) {
            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-Write time is less than write delay. Returning");
         } else {
            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-before checkReadWriteMode.");
            this.checkReadWriteMode();
            Collection<BOpcProxyExt> tmp = null;
            tmp = this.pendingWrite.values();
            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-going to acquire mutexWrite lock.");
            synchronized (this.mutexWrite) {
               this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-acquired mutexWrite lock.");
               if (tmp.size() == 0) {
                  return;
               }

               this.pendingWrite = new TreeMap<>();
            }

            this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-going to acquire mutexReadWriteInProgress lock.");
            ArrayList var44;
            synchronized (this.mutexReadWriteInProgress) {
               this.opcWriteLog.trace("OpcWrTrace: PointDeviceExt::doWrite()-acquired mutexReadWriteInProgress lock.");
               ArrayList<BOpcProxyExt> tmp2 = new ArrayList<>();
               Iterator<BOpcProxyExt> it = tmp.iterator();
               len = tmp.size();

               for (int i = 0; i < len; i++) {
                  BOpcProxyExt ext = it.next();
                  if (ext.getIsAddRemoveInProgress()) {
                     synchronized (this.mutexWrite) {
                        this.pendingWrite.put(ext.getHandle(), ext);
                     }
                  } else {
                     ext.setIsWriteInProgress(true);
                     tmp2.add(ext);
                  }

                  it.remove();
               }

               var44 = tmp2;
            }

            len = var44.size();
            this.opcWriteLog.trace("Write List Size::" + this.pendingWrite.size() + " Items actually getting written =" + len);
            if (len > 0) {
               this.opcWriteLog.trace("Writing " + var44.size() + " items");

               try {
                  if (this.getAsyncIo()) {
                     AsyncIo2 = this.peer.getAsyncIo();
                     syncIo = this.peer.getSyncIo();
                  } else {
                     syncIo = this.peer.getSyncIo();
                  }
               } catch (Exception var34) {
                  String msg = var34.getMessage();
                  this.message("Write failed: " + msg);
                  this.opcWriteLog.error("Write", var34);
                  Iterator<BOpcProxyExt> it = var44.iterator();
                  this.opcWriteLog.trace("PointDeviceExt::doWrite()-SyncIo/AsyncIo interface is not valid. Setting all points to write failed");

                  while (it.hasNext()) {
                     it.next().writeFail(msg);
                  }

                  return;
               }

               this.opcWriteLog.trace("PointDeviceExt::doWrite()-Sync IO object is valid");
               if (this.getWriteAsACollection()) {
                  BOpcProxyExt[] exts = new BOpcProxyExt[len];
                  int[] handles = new int[len];
                  int[] dataTypes = new int[len];
                  String[] valuesAry = new String[len];
                  String value = null;
                  Iterator<BOpcProxyExt> it = var44.iterator();

                  for (int i = 0; i < len && it != null && it.hasNext(); i++) {
                     exts[i] = it.next();
                     if (!exts[i].isValidItem()) {
                        exts[i].setIsWriteInProgress(false);
                     } else {
                        BStatusValue val = exts[i].getWriteValue();
                        if (val instanceof BStatusNumeric) {
                           if (exts[i].isNumericOutofRange(val)) {
                              continue;
                           }

                           double d = ((BStatusNumeric)val).getNumeric();
                           value = Double.toString(d);
                        } else if (val instanceof BStatusBoolean) {
                           boolean b = ((BStatusBoolean)val).getValue();
                           String str = Boolean.valueOf(b).toString();
                           value = str;
                        } else {
                           if (!exts[i].isValidDataType(val)) {
                              continue;
                           }

                           if (8203 == exts[i].getOpcActualDataType().getOrdinal() || 8200 == exts[i].getOpcActualDataType().getOrdinal()) {
                              value = exts[i].getStringArrayValue(val);
                           }

                           value = ((BStatusString)val).getValue();
                        }

                        valuesAry[count] = value;
                        handles[count] = exts[i].getServerHandle();
                        dataTypes[count] = exts[i].getOpcDataType().getOrdinal();
                        count++;
                     }
                  }

                  synchronized (this.mutexUnregisterCallbackInProgress) {
                     if (this.getWriteMode() == BOpcWriteMode.async) {
                        if (AsyncIo2 != null) {
                           this.opcWriteLog.trace("Doing Asynchronous Write");
                           this.waitingWr = true;
                           hresult = AsyncIo2.writeAsync(count, this.writeTxsId.increment(), handles, dataTypes, valuesAry);
                           this.waitingWr = false;
                        } else {
                           it = var44.iterator();

                           while (it.hasNext()) {
                              it.next().writeFail("Write Fail: AsyncIo2 Object is null");
                           }

                           return;
                        }
                     } else if (syncIo != null) {
                        this.opcWriteLog.trace("Doing Synchronous Write");
                        hresult = syncIo.writeArray(count, handles, dataTypes, valuesAry);
                     } else {
                        it = var44.iterator();

                        while (it.hasNext()) {
                           it.next().writeFail("Write Fail: SyncIo Object is null");
                        }

                        return;
                     }
                  }
               } else {
                  Iterator<BOpcProxyExt> it = var44.iterator();

                  for (int ix = 0; ix < len && it != null && it.hasNext(); ix++) {
                     int var38 = 0;
                     BOpcProxyExt[] exts = new BOpcProxyExt[1];
                     int[] handles = new int[1];
                     int[] dataTypes = new int[1];
                     String[] values = new String[1];
                     exts[var38] = it.next();
                     if (!exts[var38].isValidItem()) {
                        exts[var38].setIsWriteInProgress(false);
                     } else {
                        BStatusValue val = exts[var38].getWriteValue();
                        if (val instanceof BStatusNumeric) {
                           if (exts[var38].isNumericOutofRange(val)) {
                              continue;
                           }

                           double d = ((BStatusNumeric)val).getNumeric();
                           values[var38] = Double.toString(d);
                        } else if (val instanceof BStatusBoolean) {
                           boolean b = ((BStatusBoolean)val).getValue();
                           String str = Boolean.valueOf(b).toString();
                           values[var38] = str;
                        } else {
                           if (!exts[var38].isValidDataType(val)) {
                              continue;
                           }

                           if (8203 == exts[var38].getOpcActualDataType().getOrdinal() || 8200 == exts[var38].getOpcActualDataType().getOrdinal()) {
                              values[var38] = exts[var38].getStringArrayValue(val);
                           }

                           values[var38] = ((BStatusString)val).getValue();
                        }

                        handles[var38] = exts[var38].getServerHandle();
                        dataTypes[var38] = exts[var38].getOpcDataType().getOrdinal();
                        this.writingState = 4;
                        this.setWriteState(this.writingState);
                        synchronized (this.mutexUnregisterCallbackInProgress) {
                           if (this.getWriteMode() == BOpcWriteMode.async) {
                              if (AsyncIo2 != null) {
                                 this.opcWriteLog.trace("Doing Asynchronous Write");
                                 this.waitingWr = true;
                                 this.writingState = 5;
                                 this.setWriteState(this.writingState);
                                 hresult = AsyncIo2.writeAsync(1, this.writeTxsId.increment(), handles, dataTypes, values);
                                 this.waitingWr = false;
                                 this.writingState = 6;
                                 this.setWriteState(this.writingState);
                              } else {
                                 it = var44.iterator();

                                 while (it.hasNext()) {
                                    it.next().writeFail("Write Fail: AsyncIo2 Object is null");
                                 }
                              }
                           } else if (syncIo != null) {
                              this.opcWriteLog.trace("Doing Synchronous Write");
                              hresult = syncIo.writeArray(1, handles, dataTypes, values);
                           } else {
                              it = var44.iterator();

                              while (it.hasNext()) {
                                 it.next().writeFail("Write Fail: SyncIo Object is null");
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } finally {
         this.writingState = 7;
         this.setWriteState(this.writingState);
         this.writing = false;
         if (syncIo != null) {
            syncIo.release();
         }

         if (AsyncIo2 != null) {
            AsyncIo2.release();
         }

         this.opcWriteLog.trace("PointDeviceExt::doWrite()-Write finally block");
         this.lastWriteTime = Clock.ticks() - writeStart;
         this.lastTotalWriteTime = Clock.ticks() - this.writingTicks;
      }
   }

   public final BOpcDaClient getDaClient() {
      if (this.daClient == null) {
         this.daClient = (BOpcDaClient)this.getParent();
      }

      return this.daClient;
   }

   public final Type getDeviceType() {
      return BOpcDevice.TYPE;
   }

   public final BOpcDevice getOpcDevice() {
      return (BOpcDevice)this.getParent();
   }

   public final BOpcNetwork getOpcNetwork() {
      return this.getOpcDevice().getOpcNetwork();
   }

   public final Type getPointFolderType() {
      return BOpcPointFolder.TYPE;
   }

   public final Type getProxyExtType() {
      return BOpcProxyExt.TYPE;
   }

   public final IFuture post(Action a, BValue arg, Context cx) {
      if (a == read) {
         if (this.reading) {
            this.readingBusyCount++;
            long delta = Clock.ticks() - this.readingTicks;
            if (delta < this.getMaxReadWriteWaitTime() * 1000L) {
               this.opcLog.trace("PointDeviceExt::post()-- Action: Read and currently reading: true. Returning without reading.");
               return null;
            }

            this.opcWriteLog.message("PointDeviceExt::post()-- Action: Write: reading true > MaxReadWriteWaitTime sec. Continue with post.");
            this.readingClearedCount++;
         }

         this.reading = true;
         this.readingTicks = Clock.ticks();
         BOpcNetwork nw = this.getOpcNetwork();
         if (this.getUseThreadPool()) {
            nw.enqueue(new Invocation(this, a, arg, cx));
         } else {
            nw.getRdQueue().enqueue(new Invocation(this, a, arg, cx));
         }
      } else if (a == write) {
         if (this.writing) {
            this.writingBusyCount++;
            long delta = Clock.ticks() - this.writingTicks;
            if (delta < this.getMaxReadWriteWaitTime() * 1000L) {
               this.opcLog.trace("PointDeviceExt::post()-- Action: Write and currently writing: true. Returning without writing.");
               return null;
            }

            this.opcWriteLog.message("PointDeviceExt::post()-- Action: Write: writing true > MaxReadWriteWaitTime sec. Continue with post.");
            this.writingClearedCount++;
            System.out.println("Writing flag appears to be stuck. WritingState = " + this.writingState + ". Clearing writing flag.");
         }

         this.writing = true;
         this.writingTicks = Clock.ticks();
         this.writingState = 1;
         this.setWriteState(this.writingState);
         if (this.get("test") != null) {
            BValue value = this.get("test");
            if (value instanceof BBoolean && ((BBoolean)value).getBoolean()) {
               return null;
            }
         }

         BOpcNetwork nw = this.getOpcNetwork();
         if (this.getUseThreadPool()) {
            nw.enqueue(new Invocation(this, a, arg, cx));
         } else {
            nw.getWrQueue().enqueue(new Invocation(this, a, arg, cx));
         }

         this.writingState = 2;
         this.setWriteState(this.writingState);
      } else {
         this.opcLog.warning("unexpected action: " + a.getName());
      }

      return null;
   }

   public void read(BOpcProxyExt ext) {
      synchronized (this.mutexRead) {
         this.pendingRead.put(ext.getId(), ext);
      }
   }

   public void started() throws Exception {
      this.reading = false;
      this.writing = false;
      BTimeTrigger t = this.getReadTrigger();
      this.linkTo("readLink", t, BTimeTrigger.fireTrigger, read);
      t = this.getWriteTrigger();
      this.linkTo("writeLink", t, BTimeTrigger.fireTrigger, write);
      super.started();
   }

   public void stopped() throws Exception {
      try {
         Property p = this.getProperty("readLink");
         if (p != null) {
            this.remove(p, null);
         }

         p = this.getProperty("writeLink");
         if (p != null) {
            this.opcWriteLog.trace("OpcPointDeviceExt::Stopped, Write Link stopped...");
            this.remove(p, null);
         }
      } catch (Exception var2) {
      }

      super.stopped();
   }

   public void subscribe(BOpcProxyExt ext, Context cx) {
      this.opcSubscriptionLog.trace("PointDeviceExt::Subscribe() called for point:: " + ext.getId());
      synchronized (this.mutexSubscribe) {
         this.pendingSubscribe.put(ext.getHandle(), ext);
      }
   }

   public void unsubscribe(BOpcProxyExt ext, Context cx) {
      synchronized (this.mutexUnsubscribe) {
         this.pendingUnsubscribe.put(ext.getHandle(), ext);
         this.opcSubscriptionLog.trace("PointDeviceExt::unSubscribe() called-for point::" + ext.getId());
      }
   }

   public void write(BOpcProxyExt ext, Context cx) {
      this.opcWriteLog.trace("BOpcPointDeviceExt.write Entered with point::" + ext.getId());
      synchronized (this.mutexWrite) {
         this.pendingWrite.put(ext.getHandle(), ext);
         this.opcWriteLog.trace("BOpcPointDeviceExt.write put point in pending write::" + ext.getId());
      }

      this.opcWriteLog.trace("BOpcPointDeviceExt.write Exited with point::" + ext.getId());
   }

   public void cancelTransaction() {
      int i = 0;
      Integer temp = null;
      int[] readCancelIds = null;
      int[] writeCancelIds = null;
      OpcAsyncIo2 AsyncIo2 = null;

      try {
         AsyncIo2 = this.peer.getAsyncIo();
      } catch (Exception var8) {
         String msg = var8.getMessage();
         this.message("Cancel failed: " + msg);
         this.opcLog.error("Cancel", var8);
         return;
      }

      if (this.extAsyncReadMap != null && this.extAsyncReadMap.size() > 0) {
         readCancelIds = new int[this.extAsyncReadMap.size()];

         for (Entry<Integer, Integer> entry : this.extAsyncReadMap.entrySet()) {
            temp = entry.getValue();
            readCancelIds[i] = temp;
            i++;
         }
      }

      if (this.extAsyncWriteMap != null && this.extAsyncWriteMap.size() > 0) {
         writeCancelIds = new int[this.extAsyncWriteMap.size()];
         Iterator<Entry<Integer, Integer>> entries = this.extAsyncWriteMap.entrySet().iterator();

         for (int var9 = 0; entries.hasNext(); var9++) {
            Entry<Integer, Integer> entry = entries.next();
            temp = entry.getValue();
            writeCancelIds[var9] = temp;
         }
      }

      AsyncIo2.cancel2(readCancelIds, writeCancelIds);
   }

   protected synchronized void add(BOpcProxyExt ext) {
      if (this.extMap == null) {
         this.extMap = new TreeMap<>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
               int i1 = o1.hashCode();
               int i2 = o2.hashCode();
               if (i1 > i2) {
                  return -1;
               } else {
                  return i1 < i2 ? 1 : 0;
               }
            }
         });
      }

      this.extMap.put(ext.getHandle(), ext);
   }

   protected OpcGroup getPeer() {
      return this.peer;
   }

   protected synchronized void remove(BOpcProxyExt ext) {
      this.extMap.remove(ext.getHandle());
   }

   protected synchronized void addToServerHandleMap(BOpcProxyExt ext) {
      if (this.extServerHandleMap == null) {
         this.extServerHandleMap = new TreeMap<>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
               int i1 = o1.hashCode();
               int i2 = o2.hashCode();
               if (i1 > i2) {
                  return -1;
               } else {
                  return i1 < i2 ? 1 : 0;
               }
            }
         });
      }

      this.extServerHandleMap.put(ext.getServerHandle(), ext);
   }

   protected synchronized void removeFromServerHandleMap(int handle) {
      if (this.extServerHandleMap != null) {
         this.extServerHandleMap.remove(handle);
      }
   }

   public void changed(Property prop, Context cx) {
      if (this.isRunning()) {
         super.changed(prop, cx);
      }
   }

   private synchronized void checkReadWriteMode() {
      if (this.getReadMode() == BOpcReadMode.sync && this.getWriteMode() == BOpcWriteMode.sync) {
         if (this.getAsyncIo()) {
            try {
               this.opcLog.trace("PointDeviceExt::Unregistering async callback");
               this.isFirstTime = true;
               synchronized (this.mutexUnregisterCallbackInProgress) {
                  this.cancelTransaction();

                  try {
                     Thread.sleep(1000L);
                  } catch (Exception var4) {
                  }

                  this.peer.unregisterAsyncCallback();
                  this.resetFlags();
                  this.setAsyncIo(false);
               }
            } catch (Exception var7) {
               if (this.isFirstTime) {
                  this.opcLog.warning("Unregistering async callback: " + this.toPathString(), var7);
                  this.isFirstTime = false;
               }
            }
         }
      } else if (!this.getAsyncIo()) {
         try {
            this.opcLog.trace("PointDeviceExt::Registering async callback");
            this.peer.registerAsyncCallback();
            this.setAsyncIo(true);
            this.isFirstTime = true;
         } catch (Exception var6) {
            if (this.isFirstTime) {
               this.opcLog.warning("Registering async callback: " + this.toPathString(), var6);
               this.isFirstTime = false;
            }

            this.setReadMode(BOpcReadMode.sync);
            this.setWriteMode(BOpcWriteMode.sync);
            this.setAsyncIo(false);
         }
      }
   }

   private void registerCallback() {
      try {
         this.opcLog.trace("PointDeviceExt::Registering async callback from attach");
         this.peer.registerAsyncCallback();
         this.setAsyncIo(true);
      } catch (Exception var2) {
         this.opcLog.trace("Registering async callback: " + this.toPathString(), var2);
         this.setReadMode(BOpcReadMode.sync);
         this.setWriteMode(BOpcWriteMode.sync);
         this.setAsyncIo(false);
      }
   }

   private BOpcProxyExt getProxyExtFromClientHandle(int clientHandle) {
      return this.extMap == null ? null : this.extMap.get(clientHandle);
   }

   private BOpcProxyExt getProxyExtFromServerHandle(int serverHandle) {
      if (this.extServerHandleMap == null) {
         return null;
      } else {
         return this.extServerHandleMap.isEmpty() ? null : this.extServerHandleMap.get(serverHandle);
      }
   }

   private void message(String msg) {
      this.setMessage(msg);
      this.setMessageTime(Clock.time());
   }

   private void performRead() {
      Collection<BOpcProxyExt> tmp = null;
      int len = 0;
      synchronized (this.mutexRead) {
         len = this.pendingRead.size();
         if (len > this.getBatchLimit()) {
            Iterator<BOpcProxyExt> it = this.pendingRead.values().iterator();
            tmp = new ArrayList<>(this.getBatchLimit());
            int i = this.getBatchLimit();

            while (--i >= 0) {
               tmp.add(it.next());
               it.remove();
            }
         } else {
            if (len <= 0) {
               return;
            }

            tmp = this.pendingRead.values();
            this.pendingRead = new TreeMap<>();
         }
      }

      ArrayList var26;
      synchronized (this.mutexReadWriteInProgress) {
         ArrayList<BOpcProxyExt> tmp2 = new ArrayList<>();
         Iterator<BOpcProxyExt> it = tmp.iterator();
         len = tmp.size();

         for (int i = 0; i < len; i++) {
            BOpcProxyExt ext = it.next();
            if (ext.getIsAddRemoveInProgress()) {
               synchronized (this.mutexRead) {
                  this.pendingRead.put(ext.getHandle(), ext);
               }
            } else {
               ext.setIsReadInProgress(true);
               tmp2.add(ext);
            }

            it.remove();
         }

         var26 = tmp2;
      }

      len = var26.size();
      this.opcLog.trace("Read List Size::" + this.pendingRead.size() + " Items currently getting read =" + len);
      if (var26 != null) {
         this.opcLog.trace("Reading " + len + " items");
         Iterator<BOpcProxyExt> it = var26.iterator();
         int count = 0;
         int[] handles = new int[len];

         for (int i = 0; i < len; i++) {
            BOpcProxyExt ext = it.next();
            if (ext.getServerHandle() == 0) {
               this.opcLog.trace("PerformRead:: Server Handle is 0, Returning from Read Operation");
               ext.setIsReadInProgress(false);
            } else {
               handles[count] = ext.getServerHandle();
               count++;
            }
         }

         if (count <= 0) {
            return;
         }

         BOpcDaClient server = this.getDaClient();
         OpcSyncIo syncIo = null;
         OpcAsyncIo2 asyncIo2 = null;

         try {
            synchronized (this.mutexUnregisterCallbackInProgress) {
               if (BOpcReadMode.async == this.getReadMode()) {
                  this.opcLog.trace("PointDeviceExt::Asynchronous read");
                  asyncIo2 = this.peer.getAsyncIo();
                  if (asyncIo2 != null) {
                     asyncIo2.readAsync(count, this.readTxsId.increment(), handles);
                  } else {
                     this.opcLog.trace("PointDeviceExt:: AsyncIo object is NULL ");

                     for (BOpcProxyExt ext : var26) {
                        ext.readFail("Read fail: AsyncIo object is NULL");
                     }
                  }
               } else {
                  this.opcLog.trace("PointDeviceExt::Synchronous read");
                  syncIo = this.peer.getSyncIo();
                  if (syncIo != null) {
                     syncIo.read(handles, true);
                  } else {
                     this.opcLog.trace("PointDeviceExt:: SyncIo object is NULL ");

                     for (BOpcProxyExt ext : var26) {
                        ext.readFail("Read fail: SyncIo object is NULL");
                     }
                  }
               }
            }
         } catch (Exception var21) {
            String msg = "Read fail: " + var21.getMessage();
            this.message(msg);
            this.opcLog.error("Read fail", var21);

            for (BOpcProxyExt ext : var26) {
               ext.readFail(msg);
            }
         } finally {
            if (syncIo != null) {
               syncIo.release();
            }

            if (asyncIo2 != null) {
               asyncIo2.release();
            }
         }
      }
   }

   private void performSubscribe() {
      Collection<BOpcProxyExt> tmp = null;
      int len = 0;
      synchronized (this.mutexSubscribe) {
         len = this.pendingSubscribe.size();
         if (len > this.getBatchLimit()) {
            Iterator<BOpcProxyExt> it = this.pendingSubscribe.values().iterator();
            tmp = new ArrayList<>(this.getBatchLimit());
            int i = this.getBatchLimit();

            while (--i >= 0) {
               tmp.add(it.next());
               it.remove();
            }
         } else {
            if (len <= 0) {
               return;
            }

            tmp = this.pendingSubscribe.values();
            this.pendingSubscribe = new TreeMap<>();
         }
      }

      ArrayList var22;
      synchronized (this.mutexSubscribe) {
         ArrayList<BOpcProxyExt> tmp2 = new ArrayList<>();
         Iterator<BOpcProxyExt> it = tmp.iterator();
         len = tmp.size();

         for (int i = 0; i < len; i++) {
            BOpcProxyExt ext = it.next();
            if (ext.getIsInUse()) {
               this.pendingSubscribe.put(ext.getHandle(), ext);
            } else {
               ext.setIsAddRemoveInProgress(true);
               tmp2.add(ext);
            }

            it.remove();
         }

         var22 = tmp2;
      }

      len = var22.size();
      this.opcSubscriptionLog.trace("Subscription List Size::" + this.pendingSubscribe.size() + " Items currently getting added =" + len);
      if (len > 0) {
         BOpcProxyExt[] exts = new BOpcProxyExt[len];

         try {
            this.opcLog.trace("Adding " + len + " items");
            String[] ids = new String[len];
            int[] handles = new int[len];
            int[] tempHandles = new int[len];
            boolean[] active = new boolean[len];
            int[] datatypes = new int[len];
            int[] actualdatatypes = new int[len];
            Iterator<BOpcProxyExt> it = var22.iterator();

            for (int i = len; --i >= 0; actualdatatypes[i] = exts[i].getOpcDataType().getOrdinal()) {
               exts[i] = it.next();
               ids[i] = exts[i].getId();
               handles[i] = exts[i].getHandle().hashCode();
               active[i] = true;
               datatypes[i] = exts[i].getOpcDataType().getOrdinal();
            }

            OpcItemMgt mgt = this.peer.getItemMgt();
            OpcItemMgt.ItemResult[] res = mgt.addItems(ids, handles, active, datatypes);
            mgt.release();
            if (res != null) {
               for (int i = 0; i < len; i++) {
                  try {
                     res[i].actualdataType = res[i].dataType;
                     exts[i].addResult(this, res[i]);
                     this.opcLog.trace("PointDeviceExt::Adding item " + exts[i].getId() + ", Name::" + exts[i].getName());
                     if (exts[i].getItemIDChanged()) {
                        OpcItemProperties props = this.getDaClient().getPeer().getItemProperties();
                        OpcItem ret = new OpcItem(exts[i].getName(), exts[i].getId());
                        if (props != null) {
                           props.queryAvailableProperties(ret);
                           BFacets facets = BOpcPointDiscoveryJob.setItemPropertiesAsFacets(
                              exts[i].getParentPoint().getFacets(), exts[i].getOpcDataType(), ret, props
                           );
                           exts[i].getParentPoint().setFacets(facets);
                           exts[i].setDeviceFacets(facets);
                           exts[i].setItemIDChanged(false);
                        }
                     }
                  } catch (Exception var17) {
                     this.opcLog.error("Add item", var17);
                     exts[i].addResult(this, null);
                  }

                  exts[i] = null;
               }
            } else {
               this.opcLog.error("Perform Subscribe:: Add items, results array is null");

               for (int i = 0; i < len; i++) {
                  if (exts[i] != null) {
                     exts[i].addResult(this, null);
                  }
               }
            }
         } catch (Exception var18) {
            this.opcLog.error("Add items", var18);

            for (int ix = 0; ix < len; ix++) {
               if (exts[ix] != null) {
                  exts[ix].addResult(this, null);
               }
            }
         }
      }
   }

   private void performUnsubscribe() {
      Collection<BOpcProxyExt> tmp = null;
      int len = 0;
      synchronized (this.mutexUnsubscribe) {
         len = this.pendingUnsubscribe.size();
         if (len > this.getBatchLimit()) {
            Iterator<BOpcProxyExt> it = this.pendingUnsubscribe.values().iterator();
            tmp = new ArrayList<>(this.getBatchLimit());
            int i = this.getBatchLimit();

            while (--i >= 0) {
               tmp.add(it.next());
               it.remove();
            }
         } else {
            if (len <= 0) {
               return;
            }

            tmp = this.pendingUnsubscribe.values();
            this.pendingUnsubscribe = new TreeMap<>();
         }
      }

      ArrayList var19;
      synchronized (this.mutexReadWriteInProgress) {
         ArrayList<BOpcProxyExt> tmp2 = new ArrayList<>();
         Iterator<BOpcProxyExt> it = tmp.iterator();
         len = tmp.size();

         for (int i = 0; i < len; i++) {
            BOpcProxyExt ext = it.next();
            if (ext.getIsInUse()) {
               this.opcSubscriptionLog
                  .trace(
                     "####Point getting read/write in progress####"
                        + ext.getId()
                        + ", read::"
                        + ext.getIsReadInProgress()
                        + ", write::"
                        + ext.getIsWriteInProgress()
                  );
               synchronized (this.mutexUnsubscribe) {
                  this.pendingUnsubscribe.put(ext.getHandle(), ext);
               }
            } else {
               ext.setIsAddRemoveInProgress(true);
               tmp2.add(ext);
            }

            it.remove();
         }

         var19 = tmp2;
      }

      len = var19.size();
      this.opcSubscriptionLog.trace("Unsubscription list size::" + this.pendingUnsubscribe.size() + " Items currently getting removed ::" + len);
      if (len > 0) {
         try {
            this.opcWriteLog.trace("Removing " + len + " items");
            int[] handles = new int[len];
            BOpcProxyExt[] exts = new BOpcProxyExt[len];
            Iterator<BOpcProxyExt> it1 = var19.iterator();

            for (int i = 0; i < len; i++) {
               BOpcProxyExt ext = it1.next();
               handles[i] = ext.getServerHandle();
               exts[i] = ext;
               this.opcWriteLog.trace("PointDeviceExt::Removing item" + ext.getId() + ", Name::" + ext.getName());
            }

            OpcItemMgt mgt = this.peer.getItemMgt();
            int[] results = mgt.removeItems(handles);
            if (results != null) {
               int lenRes = results.length;
               this.opcWriteLog.trace("RemoveItems - HRESULT array follows below");

               for (int i = 0; i < lenRes; i++) {
                  exts[i].setIsAddRemoveInProgress(false);
                  if (results[i] != 0) {
                     this.opcWriteLog.trace("Remove Item failed for Handle:: " + handles[i] + ". Item index ::" + i + " in the passed array");

                     try {
                        this.remove(exts[i]);
                     } catch (NotRunningException var13) {
                     }

                     this.removeFromServerHandleMap(handles[i]);
                     exts[i].setServerHandle(0);
                     exts[i].setOpcQuality(BOpcQuality.bad);
                     exts[i].setOpcQualitySubcode(BOpcQualityBad.nonSpecific);
                  } else {
                     try {
                        this.remove(exts[i]);
                     } catch (NotRunningException var12) {
                     }

                     this.removeFromServerHandleMap(handles[i]);
                     exts[i].setServerHandle(0);
                     exts[i].setOpcQuality(BOpcQuality.bad);
                     exts[i].setOpcQualitySubcode(BOpcQualityBad.nonSpecific);
                  }
               }
            } else {
               for (int k = 0; k < exts.length; k++) {
                  exts[k].setIsAddRemoveInProgress(false);

                  try {
                     this.remove(exts[k]);
                  } catch (NotRunningException var11) {
                  }

                  this.removeFromServerHandleMap(handles[k]);
                  exts[k].setServerHandle(0);
                  exts[k].setOpcQuality(BOpcQuality.bad);
                  exts[k].setOpcQualitySubcode(BOpcQualityBad.nonSpecific);
               }
            }

            mgt.release();
         } catch (Exception var15) {
            this.opcWriteLog.message("Remove items", var15);
            this.getDaClient().doDetach();
         }
      }
   }

   private void readResult(int handle, BStatusValue val, long timestamp, int quality, int hresult, boolean isSyncRead) {
      BOpcProxyExt ext = null;
      if (isSyncRead) {
         ext = this.getProxyExtFromServerHandle(handle);
      } else {
         ext = this.getProxyExtFromClientHandle(handle);
      }

      BOpcDaClient server = this.getDaClient();
      if (ext == null) {
         this.opcLog.trace("Item not found " + this.toPathString() + " client handle : " + handle + " HRESULT " + hresult);
      } else if (OpcEnv.failed(hresult)) {
         ext.readFail(OpcEnv.resultString(hresult));
         ext.setIsReadInProgress(false);
         this.opcLog.trace("PointDeviceExt::readResult()-Point::" + ext.getId() + " read failed. HRESULT::" + hresult);
      } else {
         ext.readResult(val, timestamp, quality, hresult);
      }
   }

   private void onReadCompleted(
      int transId,
      int hrMasterQty,
      int hrMaster,
      int noOfItems,
      int[] ClientHandles,
      int[] dataTypes,
      String[] values,
      int[] qualities,
      long[] timeStamps,
      int[] hresultArray
   ) {
      BOpcProxyExt ext = null;
      this.opcLog.trace("PointDeviceExt::onReadCompleted, gets called for item");

      try {
         if (ClientHandles != null) {
            for (int i = 0; i < noOfItems; i++) {
               BStatusValue sv;
               if (1 == dataTypes[i]) {
                  double db = Double.valueOf(values[i].trim());
                  sv = new BStatusNumeric(db);
               } else if (2 == dataTypes[i]) {
                  boolean bl = false;
                  if (values[i].trim().equalsIgnoreCase("true")) {
                     bl = true;
                  }

                  sv = new BStatusBoolean(bl);
               } else if (3 == dataTypes[i]) {
                  sv = new BStatusString(values[i].trim());
               } else {
                  sv = null;
               }

               if (0 == hrMaster) {
                  this.readResult(ClientHandles[i], sv, timeStamps[i], qualities[i], 0, false);
               } else {
                  this.readResult(ClientHandles[i], sv, timeStamps[i], qualities[i], hresultArray[i], false);
               }
            }
         } else {
            this.opcLog.trace("PointDeviceExr::onReadCompleted, clientHandle array is null");
         }

         if (this.extAsyncReadMap.size() > 0) {
            synchronized (this.mutexAsyncReadMap) {
               this.extAsyncReadMap.remove(transId);
            }
         }
      } catch (Exception var18) {
         this.opcLog.error("onReadCompleted", var18);
      }
   }

   private void onWriteCompleted(int transId, int grpHandle, int hrMaster, int noOfItems, int[] clientHandle, int[] hresultArray) {
      this.opcWriteLog.trace("PointDeviceExt::OnWriteCompleted get called......::" + noOfItems + ", master result ::" + (0 == hrMaster ? "OK" : "NOT OK"));
      BOpcProxyExt ext = null;

      try {
         if (clientHandle != null) {
            if (0 == hrMaster) {
               for (int i = 0; i < noOfItems; i++) {
                  ext = this.getProxyExtFromClientHandle(clientHandle[i]);
                  if (ext != null) {
                     ext.updateWriteResult(hresultArray[i]);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::OnWriteCompleted, ClientHandle object is null");
                  }
               }
            } else if (hresultArray != null) {
               for (int ix = 0; ix < noOfItems; ix++) {
                  ext = this.getProxyExtFromClientHandle(clientHandle[ix]);
                  if (ext != null) {
                     ext.updateWriteResult(hresultArray[ix]);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::OnWriteCompleted, ClientHandle object is null");
                  }
               }
            } else {
               this.opcWriteLog.trace("PointDeviceExr::OnWriteCompleted, hresultArray array is null");

               for (int ixx = 0; ixx < noOfItems; ixx++) {
                  ext = this.getProxyExtFromClientHandle(clientHandle[ixx]);
                  if (ext != null) {
                     ext.updateWriteResult(-2147467259);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::OnWriteCompleted, ClientHandle object is null");
                  }
               }
            }
         } else {
            this.opcWriteLog.trace("PointDeviceExr::OnWriteCompleted, clientHandle array is null");
         }

         if (this.extAsyncWriteMap.size() > 0) {
            synchronized (this.mutexAsyncWriteMap) {
               this.extAsyncWriteMap.remove(transId);
            }
         }
      } catch (Exception var11) {
         this.opcWriteLog.error("OnWriteCompleted", var11);
      }
   }

   private void onReturnReadAsync(int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
      BOpcProxyExt ext = null;
      boolean IsCallbackHappen = false;

      try {
         if (serverHandle != null) {
            if (0 == hresult) {
               this.opcLog.trace("PointDeviceExt::onReturnReadAsync, Read Async operation was succeeded will get an Callback");
               IsCallbackHappen = true;
            } else if (1 == hresult) {
               if (hresultArray != null) {
                  for (int i = 0; i < noOfItems; i++) {
                     ext = this.getProxyExtFromServerHandle(serverHandle[i]);
                     if (ext != null) {
                        if (hresultArray[i] == 0) {
                           IsCallbackHappen = true;
                        } else {
                           ext.updateReadResult(hresultArray[i]);
                        }
                     } else {
                        this.opcLog.trace("PointDeviceExt::onReturnReadAsync, serverHandle object is null");
                     }
                  }
               } else {
                  this.opcLog.trace("PointDeviceExt::onReturnReadAsync, hresultArray is null");

                  for (int ix = 0; ix < noOfItems; ix++) {
                     ext = this.getProxyExtFromServerHandle(serverHandle[ix]);
                     if (ext != null) {
                        ext.updateReadResult(-2147467259);
                     }
                  }
               }
            } else {
               this.opcLog.trace("PointDeviceExt::onReturnReadAsync," + OpcEnv.getDescription(hresult));

               for (int ixx = 0; ixx < noOfItems; ixx++) {
                  ext = this.getProxyExtFromServerHandle(serverHandle[ixx]);
                  if (ext != null) {
                     ext.updateReadResult(hresult);
                  } else {
                     this.opcLog.trace("PointDeviceExt::onReturnReadAsync, serverHandle object is null");
                  }
               }
            }
         } else {
            this.opcLog.trace("PointDeviceExt::onReturnReadAsync, incoming array is null");
         }

         if (IsCallbackHappen && this.extAsyncReadMap != null) {
            synchronized (this.mutexAsyncReadMap) {
               this.extAsyncReadMap.put(transId, cancelId);
            }
         }
      } catch (Exception var12) {
         this.opcLog.error("onReturnReadAsync", var12);
      }
   }

   private void onReturnWriteAsync(int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
      BOpcProxyExt ext = null;
      boolean IsCallbackHappen = false;

      try {
         if (serverHandle != null) {
            if (0 == hresult) {
               this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, Write Async operation was succeeded");
               IsCallbackHappen = true;
            } else if (1 == hresult) {
               if (hresultArray != null) {
                  for (int i = 0; i < noOfItems; i++) {
                     ext = this.getProxyExtFromServerHandle(serverHandle[i]);
                     if (ext != null) {
                        if (hresultArray[i] == 0) {
                           IsCallbackHappen = true;
                        } else {
                           ext.updateWriteResult(hresultArray[i]);
                        }
                     } else {
                        this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, serverHandle object is null");
                     }
                  }
               } else {
                  this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, hresultArray is null");

                  for (int ix = 0; ix < noOfItems; ix++) {
                     ext = this.getProxyExtFromServerHandle(serverHandle[ix]);
                     if (ext != null) {
                        ext.updateWriteResult(-2147467259);
                     } else {
                        this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, serverHandle object is null");
                     }
                  }
               }
            } else {
               this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray," + OpcEnv.getDescription(hresult));

               for (int ixx = 0; ixx < noOfItems; ixx++) {
                  ext = this.getProxyExtFromServerHandle(serverHandle[ixx]);
                  if (ext != null) {
                     ext.updateWriteResult(hresult);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, serverHandle object is null");
                  }
               }
            }
         } else {
            this.opcWriteLog.trace("PointDeviceExt::OnReturnWriteArray, incoming array is null");
         }

         if (IsCallbackHappen && this.extAsyncWriteMap != null) {
            synchronized (this.mutexAsyncWriteMap) {
               this.extAsyncWriteMap.put(transId, cancelId);
            }
         }
      } catch (Exception var12) {
         this.opcWriteLog.error("OnReturnWriteArray", var12);
      }
   }

   private void onReturnWriteSync(int hresult, int noOfItems, int[] serverHandle, int[] hresultArray) {
      BOpcProxyExt ext = null;

      try {
         if (serverHandle != null) {
            if (0 == hresult) {
               for (int i = 0; i < noOfItems; i++) {
                  ext = this.getProxyExtFromServerHandle(serverHandle[i]);
                  if (ext != null) {
                     ext.updateWriteResult(0);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync, serverHandle object is null");
                  }
               }
            } else if (1 == hresult) {
               if (hresultArray != null) {
                  for (int ix = 0; ix < noOfItems; ix++) {
                     ext = this.getProxyExtFromServerHandle(serverHandle[ix]);
                     if (ext != null) {
                        ext.updateWriteResult(hresultArray[ix]);
                     } else {
                        this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync, serverHandle object is null");
                     }
                  }
               } else {
                  this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync, hresultArray is null");
               }
            } else {
               this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync," + OpcEnv.getDescription(hresult));

               for (int ixx = 0; ixx < noOfItems; ixx++) {
                  ext = this.getProxyExtFromServerHandle(serverHandle[ixx]);
                  if (ext != null) {
                     ext.updateWriteResult(hresult);
                  } else {
                     this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync, serverHandle object is null");
                  }
               }
            }
         } else {
            this.opcWriteLog.trace("PointDeviceExt::onReturnWriteSync, incoming array is null");
         }
      } catch (Exception var7) {
         this.opcWriteLog.error("onReturnWriteSync", var7);
      }
   }

   private void setAttached() {
      this.setState(BOpcState.attached);
   }

   private void setAttaching() {
      this.setState(BOpcState.attaching);
   }

   private void setDetached() {
      this.setState(BOpcState.detached);
   }

   private void setDetaching() {
      this.setState(BOpcState.detaching);
   }

   private void resetFlags() {
      BControlPoint[] pts = this.getPoints();

      for (int i = 0; i < pts.length; i++) {
         if (pts[i].getProxyExt() instanceof BOpcProxyExt) {
            ((BOpcProxyExt)pts[i].getProxyExt()).setIsReadInProgress(false);
            ((BOpcProxyExt)pts[i].getProxyExt()).setIsWriteInProgress(false);
            ((BOpcProxyExt)pts[i].getProxyExt()).setIsAddRemoveInProgress(false);
            ((BOpcProxyExt)pts[i].getProxyExt()).writeReset();
         }
      }
   }

   public long getWritingBusyCount() {
      return this.writingBusyCount;
   }

   public long getReadingBusyCount() {
      return this.readingBusyCount;
   }

   public long getWritingTicks() {
      return this.writingTicks;
   }

   public int getWritingState() {
      return this.writingState;
   }

   public long getReadingTicks() {
      return this.readingTicks;
   }

   public long getLastWriteTime() {
      return this.lastWriteTime;
   }

   public long getLastTotalWriteTime() {
      return this.lastTotalWriteTime;
   }

   public void spy(SpyWriter out) throws Exception {
      out.startProps();
      out.trTitle("OpcPointDeviceExt", 2);
      out.prop("pendingRead.size()", this.pendingRead.size());
      out.prop("reading", this.reading);
      out.prop("doReadCount", this.doReadCount);
      out.prop("readingBusyCount", this.readingBusyCount);
      out.prop("readingClearedCount", this.readingClearedCount);
      out.prop("pendingWrite.size()", this.pendingWrite.size());
      out.prop("writing", this.writing);
      out.prop("writingState", this.writingState);
      out.prop("doWriteCount", this.doWriteCount);
      out.prop("writingClearedCount", this.writingClearedCount);
      out.prop("writingBusyCount", this.writingBusyCount);
      out.prop("waitingWr", this.waitingWr);
      out.prop("lastWriteTime", this.lastWriteTime);
      out.prop("lastTotalWriteTime", this.lastTotalWriteTime);
      out.endProps();
      super.spy(out);
   }

   private class Counter {
      private int count = 1;

      private Counter() {
      }

      public int increment() {
         if (this.count == Integer.MAX_VALUE) {
            this.count = 1;
         }

         return this.count++;
      }
   }

   private class Listener implements OpcGroup.GroupListener {
      public Listener() {
      }

      @Override
      public void asyncCallbackDeleted() {
         if (BOpcPointDeviceExt.this.getState().isEngaged()) {
         }
      }

      @Override
      public void updateBoolean(int handle, boolean val, long utcTimestamp, int quality, int hresult, boolean isSync) {
         BStatusValue sv = new BStatusBoolean(val);
         BOpcPointDeviceExt.this.readResult(handle, sv, utcTimestamp, quality, hresult, isSync);
      }

      @Override
      public void updateError(int handle, long utcTimestamp, int quality, int hresult, boolean isSync) {
         BOpcPointDeviceExt.this.readResult(handle, null, utcTimestamp, quality, hresult, isSync);
      }

      @Override
      public void updateNumeric(int handle, double val, long utcTimestamp, int quality, int hresult, boolean isSync) {
         BStatusValue sv = new BStatusNumeric(val);
         BOpcPointDeviceExt.this.readResult(handle, sv, utcTimestamp, quality, hresult, isSync);
      }

      @Override
      public void updateString(int handle, String val, long utcTimestamp, int quality, int hresult, boolean isSync) {
         BStatusValue sv = new BStatusString(val);
         BOpcPointDeviceExt.this.readResult(handle, sv, utcTimestamp, quality, hresult, isSync);
      }

      @Override
      public void OnWriteCompleted(int transId, int grpHandle, int hrMaster, int noOfItems, int[] clientHandle, int[] hresultArray) {
         BOpcPointDeviceExt.this.onWriteCompleted(transId, grpHandle, hrMaster, noOfItems, clientHandle, hresultArray);
      }

      @Override
      public void OnReturnWrite(boolean isSync, int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
         if (isSync) {
            BOpcPointDeviceExt.this.onReturnWriteSync(hresult, noOfItems, serverHandle, hresultArray);
         } else {
            BOpcPointDeviceExt.this.onReturnWriteAsync(hresult, noOfItems, transId, cancelId, serverHandle, hresultArray);
         }
      }

      @Override
      public void OnReturnRead(int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
         BOpcPointDeviceExt.this.onReturnReadAsync(hresult, noOfItems, transId, cancelId, serverHandle, hresultArray);
      }

      @Override
      public void OnReadCompleted(
         int transId,
         int hrMasterQty,
         int hrMaster,
         int noOfItems,
         int[] ClientHandles,
         int[] dataTypes,
         String[] values,
         int[] qualities,
         long[] timeStamps,
         int[] hresultArray
      ) {
         BOpcPointDeviceExt.this.onReadCompleted(
            transId, hrMasterQty, hrMaster, noOfItems, ClientHandles, dataTypes, values, qualities, timeStamps, hresultArray
         );
      }
   }
}
