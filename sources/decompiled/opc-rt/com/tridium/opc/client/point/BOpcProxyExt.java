package com.tridium.opc.client.point;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.client.BOpcDaClient;
import com.tridium.opc.client.BOpcDevice;
import com.tridium.opc.client.BOpcNetwork;
import com.tridium.opc.client.util.BIOpcPollable;
import com.tridium.opc.client.util.BOpcDataType;
import com.tridium.opc.client.util.BOpcLimit;
import com.tridium.opc.client.util.BOpcQuality;
import com.tridium.opc.client.util.BOpcQualityBad;
import com.tridium.opc.client.util.BOpcQualityGood;
import com.tridium.opc.client.util.BOpcQualityUncertain;
import com.tridium.opc.client.util.BOpcQualityUnknown;
import com.tridium.opc.client.util.BOpcReadMode;
import com.tridium.opc.client.util.BOpcTuningPolicy;
import com.tridium.opc.jni.client.da.OpcItemMgt;
import com.tridium.opc.jni.client.da.OpcSyncIo;
import javax.baja.control.BControlPoint;
import javax.baja.control.BIWritablePoint;
import javax.baja.driver.point.BProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.BNumber;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "id",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "opcDataType",
      type = "BOpcDataType",
      defaultValue = "BOpcDataType.vtEmpty"
   ), @NiagaraProperty(
      name = "opcReadTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "opcQuality",
      type = "BOpcQuality",
      defaultValue = "BOpcQuality.good",
      flags = 67
   ), @NiagaraProperty(
      name = "opcQualitySubcode",
      type = "BFrozenEnum",
      defaultValue = "BOpcQualityGood.nonSpecific",
      flags = 67
   ), @NiagaraProperty(
      name = "opcLimit",
      type = "BOpcLimit",
      defaultValue = "BOpcLimit.notLimited",
      flags = 67
   ), @NiagaraProperty(
      name = "serverHandle",
      type = "int",
      defaultValue = "0",
      flags = 67
   ), @NiagaraProperty(
      name = "mode",
      type = "BReadWriteMode",
      defaultValue = "BReadWriteMode.readWrite",
      flags = 65
   ), @NiagaraProperty(
      name = "opcActualDataType",
      type = "BOpcDataType",
      defaultValue = "BOpcDataType.vtEmpty"
   )})
@NiagaraActions({@NiagaraAction(
      name = "read"
   ), @NiagaraAction(
      name = "forceWrite"
   )})
public class BOpcProxyExt extends BProxyExt implements BIOpcPollable {
   public static final Property id = newProperty(0, "", null);
   public static final Property opcDataType = newProperty(0, BOpcDataType.vtEmpty, null);
   public static final Property opcReadTime = newProperty(67, BAbsTime.NULL, null);
   public static final Property opcQuality = newProperty(67, BOpcQuality.good, null);
   public static final Property opcQualitySubcode = newProperty(67, BOpcQualityGood.nonSpecific, null);
   public static final Property opcLimit = newProperty(67, BOpcLimit.notLimited, null);
   public static final Property serverHandle = newProperty(67, 0, null);
   public static final Property mode = newProperty(65, BReadWriteMode.readWrite, null);
   public static final Property opcActualDataType = newProperty(0, BOpcDataType.vtEmpty, null);
   public static final Action read = newAction(0, null);
   public static final Action forceWrite = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BOpcProxyExt.class);
   private BOpcDaClient daClient;
   private BOpcPointDeviceExt deviceExt = null;
   private BOpcPointFolder folder = null;
   private boolean subscribed = false;
   private boolean writePending = false;
   private boolean itemIdChanged = false;
   private boolean isWriteInProgress = false;
   private Object syncIsWriteInProgress = new Object();
   private boolean isReadInProgress = false;
   private Object syncIsReadInProgress = new Object();
   private boolean isAddRemoveInProgress = false;
   Log opcLog = Log.getLog("OpcDaLog");
   Log opcWriteLog = Log.getLog("OpcDaWriteLog");

   public String getId() {
      return this.getString(id);
   }

   public void setId(String v) {
      this.setString(id, v, null);
   }

   public BOpcDataType getOpcDataType() {
      return (BOpcDataType)this.get(opcDataType);
   }

   public void setOpcDataType(BOpcDataType v) {
      this.set(opcDataType, v, null);
   }

   public BAbsTime getOpcReadTime() {
      return (BAbsTime)this.get(opcReadTime);
   }

   public void setOpcReadTime(BAbsTime v) {
      this.set(opcReadTime, v, null);
   }

   public BOpcQuality getOpcQuality() {
      return (BOpcQuality)this.get(opcQuality);
   }

   public void setOpcQuality(BOpcQuality v) {
      this.set(opcQuality, v, null);
   }

   public BFrozenEnum getOpcQualitySubcode() {
      return (BFrozenEnum)this.get(opcQualitySubcode);
   }

   public void setOpcQualitySubcode(BFrozenEnum v) {
      this.set(opcQualitySubcode, v, null);
   }

   public BOpcLimit getOpcLimit() {
      return (BOpcLimit)this.get(opcLimit);
   }

   public void setOpcLimit(BOpcLimit v) {
      this.set(opcLimit, v, null);
   }

   public int getServerHandle() {
      return this.getInt(serverHandle);
   }

   public void setServerHandle(int v) {
      this.setInt(serverHandle, v, null);
   }

   public BReadWriteMode getMode() {
      return (BReadWriteMode)this.get(mode);
   }

   public void setMode(BReadWriteMode v) {
      this.set(mode, v, null);
   }

   public BOpcDataType getOpcActualDataType() {
      return (BOpcDataType)this.get(opcActualDataType);
   }

   public void setOpcActualDataType(BOpcDataType v) {
      this.set(opcActualDataType, v, null);
   }

   public void read() {
      this.invoke(read, null, null);
   }

   public void forceWrite() {
      this.invoke(forceWrite, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void changed(Property p, Context c) {
      super.changed(p, c);
      if (c != Context.decoding && this.subscribed && this.isRunning()) {
         if (p == id) {
            this.itemIdChanged = true;
            this.readUnsubscribed(c);
            this.readSubscribed(c);
         }

         BOpcDaClient server = this.getDaClient();
         if (server.isDown() || server.isDisabled()) {
            this.readUnsubscribed(null);
         }
      }
   }

   public void doRead() {
      if (this.isRunning()) {
         this.getOpcPointDeviceExt().read(this);
      }
   }

   public final Type getDeviceExtType() {
      return BOpcPointDeviceExt.TYPE;
   }

   public final BOpcPointFolder getFolder() {
      if (this.folder == null) {
         for (BComplex cur = this.getParent(); cur != null; cur = cur.getParent()) {
            if (cur instanceof BOpcPointFolder) {
               this.folder = (BOpcPointFolder)cur;
               break;
            }
         }
      }

      return this.folder;
   }

   public final BOpcDevice getOpcDevice() {
      return (BOpcDevice)this.getDevice();
   }

   public final BOpcDaClient getDaClient() {
      if (this.daClient == null) {
         this.daClient = (BOpcDaClient)this.getDevice();
      }

      return this.daClient;
   }

   public final BOpcPointDeviceExt getOpcPointDeviceExt() {
      if (this.deviceExt == null) {
         this.deviceExt = (BOpcPointDeviceExt)this.getDeviceExt();
      }

      return this.deviceExt;
   }

   public final BOpcNetwork getOpcNetwork() {
      return this.getOpcDevice().getOpcNetwork();
   }

   public BPollFrequency getPollFrequency() {
      return ((BOpcTuningPolicy)this.getTuningPolicy()).getPollFrequency();
   }

   public final boolean isReadSubscribed() {
      return this.subscribed;
   }

   @Override
   public void poll() {
      if (BOpcReadMode.cov != this.getOpcPointDeviceExt().getReadMode()) {
         this.read();
      }
   }

   public final IFuture post(Action a, BValue arg, Context cx) {
      this.getOpcNetwork().enqueue(new Invocation(this, a, arg, cx));
      return null;
   }

   public final void readSubscribed(Context cx) {
      synchronized (this) {
         if (this.subscribed) {
            return;
         }

         this.subscribed = true;
      }

      this.getOpcPointDeviceExt().subscribe(this, cx);
      this.writeReset();
   }

   public final void readUnsubscribed(Context cx) {
      synchronized (this) {
         if (this.subscribed) {
            this.setSubscribed(false);
            this.getOpcDevice().getPollScheduler().unsubscribe(this);
            BOpcDaClient server = this.getDaClient();
            if (!server.isDown() && !server.isDisabled() && !server.getState().isDisengaged()) {
               this.getOpcPointDeviceExt().unsubscribe(this, cx);
            } else {
               this.setServerHandle(0);
               this.setOpcQuality(BOpcQuality.bad);
               this.setOpcQualitySubcode(BOpcQualityBad.nonSpecific);
            }

            this.writeReset();
         }
      }
   }

   public final void setStale(boolean stale, Context cx) {
      super.setStale(stale, cx);
      if (stale && this.subscribed) {
         if (this.getServerHandle() == 0) {
            this.readUnsubscribed(cx);
            this.readSubscribed(cx);
         } else {
            this.read();
         }
      }
   }

   public final void started() throws Exception {
      if (this.isWritablePoint()) {
         this.setMode(BReadWriteMode.readWrite);
      } else {
         this.setMode(BReadWriteMode.readonly);
      }

      BOpcPointDeviceExt ext = this.getOpcPointDeviceExt();
      ext.add(this);
   }

   public final void stopped() throws Exception {
      this.readUnsubscribed(null);
      this.deviceExt = null;
      this.folder = null;
      super.stopped();
   }

   public boolean write(Context cx) {
      this.opcWriteLog.trace("BOpcProxyExt.write Entered with point::" + this.getId());
      BStatusValue val = this.getWriteValue();
      if (val.getStatus().isNull()) {
         this.opcWriteLog.trace("BOpcProxyExt.write Status is null for::" + this.getId());
         return false;
      } else {
         this.writePending = true;
         if (!this.subscribed) {
            this.opcWriteLog.trace("Proxy::Write,point not subscribed.Calling read subscribed::" + this.getId());
            this.readSubscribed(null);
         } else if (this.getServerHandle() == 0) {
            this.opcWriteLog.trace("BOpcProxyExt.write server handle is zero for::" + this.getId());
            this.writeReset();
            if (!this.subscribed) {
               this.readSubscribed(null);
            }
         } else {
            this.opcWriteLog.trace("Proxy::Write,point subscribed.adding to pendingWriteMap::" + this.getId());
            this.getOpcPointDeviceExt().write(this, cx);
         }

         this.opcWriteLog.trace("BOpcProxyExt.write Exited with point::" + this.getId());
         return true;
      }
   }

   public void writablePointActionInvoked() {
      super.writablePointActionInvoked();
      this.getParentPoint().execute();
   }

   public void doForceWrite() {
      this.write(null);
   }

   protected void addResult(BOpcPointDeviceExt ext, OpcItemMgt.ItemResult res) {
      this.opcLog.trace("Proxy::addResult. Point " + this.getId() + " with server handle at beginning:" + this.getServerHandle());
      BOpcDaClient server = this.getDaClient();
      server.getPollScheduler().unsubscribe(this);
      String msg = "";
      this.setIsAddRemoveInProgress(false);
      if (res == null) {
         this.setServerHandle(0);
         this.setOpcQuality(BOpcQuality.bad);
         this.setOpcQualitySubcode(BOpcQualityBad.nonSpecific);
         msg = "Add item, no result";
         this.readFail(msg);
         if (this.isRunning() && !this.isDisabled() && !this.isDown()) {
            ext.subscribe(this, null);
            this.opcLog.trace("Proxy::Additem, no result. Subscribing " + this.getId() + " again for adding in next trail ");
         } else {
            ext.unsubscribe(this, null);
            this.opcLog.trace("Proxy::Additem, no result.Point " + this.getId() + " is unsubscribed");
         }
      } else {
         this.opcLog.trace("Proxy::addResult. Point " + this.getId() + " with hresult:" + res.hresult + " and new server handle" + res.serverHandle);
         if (OpcEnv.failed(res.hresult)) {
            msg = OpcEnv.resultString(res.hresult);
            this.readFail(msg);
            if (!this.isRunning() || this.isDisabled() || this.isDown()) {
               return;
            }

            ext.subscribe(this, null);
            this.opcLog
               .trace(
                  "Proxy::Additem failed with hresult::"
                     + Integer.toHexString(res.hresult)
                     + ". Putting the Point subscribe list for next trial "
                     + this.getId()
               );
         }

         this.opcLog.trace("Proxy::Add item succeeded::" + res.hresult + " for item " + this.getId() + ", Name::" + this.getName());
         this.setServerHandle(res.serverHandle);
         ext.addToServerHandleMap(this);
         ext.add(this);
         boolean r = OpcEnv.isReadable(res.access);
         boolean w = OpcEnv.isWritable(res.access);
         if (r && w) {
            if (this.isWritablePoint() && this.getMode() != BReadWriteMode.readWrite) {
               this.setMode(BReadWriteMode.readWrite);
            }
         } else if (w) {
            if (this.isWritablePoint() && this.getMode() != BReadWriteMode.writeonly) {
               this.setMode(BReadWriteMode.writeonly);
            }
         } else if (this.getMode() != BReadWriteMode.readonly) {
            this.setMode(BReadWriteMode.readonly);
         }

         try {
            this.setOpcDataType(BOpcDataType.make(res.dataType));
            this.setOpcActualDataType(BOpcDataType.make(res.actualdataType));
         } catch (Exception var8) {
            this.opcLog.error("Unexpected datatype: " + res.dataType);
         }

         if (ext.getReadMode() != BOpcReadMode.cov) {
            server.getPollScheduler().subscribe(this);
         }

         if (this.writePending) {
            this.write(null);
         }
      }
   }

   protected boolean isValidItem() {
      if (!this.isRunning()) {
         return false;
      } else if (this.isDown() || this.isDisabled()) {
         this.opcWriteLog.error("Proxy::IsValidItem, UnOperationl. Returning from write operation::" + this.getId());
         return false;
      } else if (this.getServerHandle() == 0) {
         this.getOpcPointDeviceExt().write(this, null);
         this.opcWriteLog.error("Proxy::IsValidItem, Serverhandle is 0. Returning from write operation::" + this.getId());
         return false;
      } else {
         this.writePending = false;
         BStatusValue val = this.getWriteValue();
         if (val.getStatus().isNull()) {
            this.writePending = true;
            this.writeOk(val);
            this.opcWriteLog.trace("Proxy::IsValidItem, value.getStatus =Null, WriteOK called, Returning.." + this.getId());
            return false;
         } else {
            return true;
         }
      }
   }

   protected boolean isNumericOutofRange(BStatusValue val) {
      double d = ((BStatusNumeric)val).getNumeric();
      BNumber bn = (BNumber)this.getDeviceFacets().get("min");
      if (bn != null && bn.getDouble() > d) {
         this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
         this.opcWriteLog.message("Proxy::isOutofRange, Write failed as value out of range::" + this.getId());
         return true;
      } else {
         bn = (BNumber)this.getDeviceFacets().get("max");
         if (bn != null && bn.getDouble() < d) {
            this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
            this.opcWriteLog.message("Proxy::isOutofRange, Write failed as value out of range::" + this.getId());
            return true;
         } else {
            BControlPoint cp = (BControlPoint)this.getParent();
            bn = (BNumber)cp.getFacets().get("min");
            if (bn != null && bn.getDouble() > d) {
               this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
               this.opcWriteLog.message("Proxy::isOutofRange, Write failed as value out of range::" + this.getId());
               return true;
            } else {
               bn = (BNumber)cp.getFacets().get("max");
               if (bn != null && bn.getDouble() < d) {
                  this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
                  this.opcWriteLog.message("Proxy::isOutofRange, Write failed as value out of range::" + this.getId());
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   protected boolean isValidDataType(BStatusValue val) {
      int hresult = 0;
      String strTemp = ((BStatusString)val).getValue();
      String[] strArr = TextUtil.split(strTemp, ',');
      boolean isBool = true;
      if (strArr.length == 0) {
         if (this.getOpcActualDataType().getOrdinal() == 8) {
            return true;
         }

         if (this.getOpcActualDataType().getOrdinal() == 8200) {
            return true;
         }

         hresult = -1073479676;
      } else {
         if (this.getOpcActualDataType().getOrdinal() == 8) {
            return true;
         }

         if (8203 == this.getOpcActualDataType().getOrdinal()) {
            for (int i = 0; i < strArr.length; i++) {
               try {
                  strArr[i] = strArr[i].trim();
                  if (strArr[i].length() == 0) {
                     hresult = -1073479676;
                     isBool = false;
                  }
               } catch (Exception var8) {
                  isBool = false;
                  break;
               }
            }

            if (isBool) {
               return true;
            }

            hresult = -1073479676;
         } else {
            if (this.getOpcActualDataType().getOrdinal() == 8200) {
               return true;
            }

            int intValid = this.validateOpcDataType(val, this.getOpcActualDataType().getOrdinal());
            if (0 == intValid) {
               return true;
            }

            if (1 == intValid) {
               hresult = -1073479669;
            } else {
               hresult = -1073479676;
            }
         }
      }

      if (OpcEnv.failed(hresult)) {
         this.opcWriteLog.error(this.getId() + " write failed 1 [val=" + val.getValueValue() + "] " + OpcEnv.getDescription(hresult));
         this.writeFail(OpcEnv.getDescription(hresult));
      }

      return false;
   }

   protected String getStringArrayValue(BStatusValue val) {
      String strBoolVal = "";
      String strTemp = ((BStatusString)val).getValue();
      String[] strArr = TextUtil.split(strTemp, ',');
      boolean isBool = true;
      if (8203 == this.getOpcActualDataType().getOrdinal()) {
         for (int i = 0; i < strArr.length; i++) {
            try {
               strArr[i] = strArr[i].trim();
               if (strArr[i].length() != 0) {
                  int intTemp = Integer.parseInt(strArr[i]);
                  if (intTemp != 0) {
                     strBoolVal = strBoolVal + "true";
                  } else {
                     strBoolVal = strBoolVal + "false";
                  }

                  if (i != strArr.length - 1) {
                     strBoolVal = strBoolVal + ",";
                  }
               }
            } catch (Exception var8) {
               isBool = false;
               break;
            }
         }

         if (isBool) {
            return strBoolVal;
         }
      } else if (this.getOpcActualDataType().getOrdinal() == 8200) {
         return ((BStatusString)val).getValue().trim();
      }

      return strTemp;
   }

   protected void updateWriteResult(int hresult) {
      if (OpcEnv.failed(hresult)) {
         this.opcWriteLog.error(this.getId() + " write failed 2 [val=" + this.getWriteValue().getValueValue() + "] " + OpcEnv.getDescription(hresult));
         this.writeFail(OpcEnv.getDescription(hresult));
      } else {
         this.opcWriteLog.trace(this.getId() + " write success [val=" + this.getWriteValue().getValueValue() + "] " + OpcEnv.getDescription(hresult));
         this.writeOk(this.getWriteValue());
         if (this.getParentPoint().isSubscribed()) {
            this.read();
         }
      }
   }

   protected void updateReadResult(int hresult) {
      if (OpcEnv.failed(hresult)) {
         this.setIsReadInProgress(false);
         this.readFail(OpcEnv.getDescription(hresult));
         this.opcLog.trace("PointDeviceExt::updateReadResult()-Point::" + this.getId() + " read failed. HRESULT::" + OpcEnv.getDescription(hresult));
      }
   }

   protected void performWrite(OpcSyncIo sio) {
      if (this.isRunning()) {
         if (this.isDown() || this.isDisabled() || this.isFatalFault()) {
            this.opcWriteLog.error("Proxy::PerformWrite, UnOperationl. Returning from write operation::" + this.getId());
         } else if (this.getServerHandle() == 0) {
            this.getOpcPointDeviceExt().write(this, null);
            this.opcWriteLog.error("Proxy::PerformWrite, Serverhandle is 0. Returning from write operation::" + this.getId());
         } else {
            this.writePending = false;
            BStatusValue val = this.getWriteValue();
            if (val.getStatus().isNull()) {
               this.writePending = true;
               this.writeOk(val);
               this.opcWriteLog.trace("Proxy::PerformWrite, value.getStatus =Null, WriteOK called, Returning.." + this.getId());
            } else {
               if (val instanceof BStatusNumeric) {
                  double d = ((BStatusNumeric)val).getNumeric();
                  BNumber bn = (BNumber)this.getDeviceFacets().get("min");
                  if (bn != null && bn.getDouble() > d) {
                     this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
                     this.opcWriteLog.message("Proxy::PerformWrite, Write failed as value out of range::" + this.getId());
                     return;
                  }

                  bn = (BNumber)this.getDeviceFacets().get("max");
                  if (bn != null && bn.getDouble() < d) {
                     this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
                     this.opcWriteLog.message("Proxy::PerformWrite, Write failed as value out of range::" + this.getId());
                     return;
                  }

                  BControlPoint cp = (BControlPoint)this.getParent();
                  bn = (BNumber)cp.getFacets().get("min");
                  if (bn != null && bn.getDouble() > d) {
                     this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
                     this.opcWriteLog.message("Proxy::PerformWrite, Write failed as value out of range::" + this.getId());
                     return;
                  }

                  bn = (BNumber)cp.getFacets().get("max");
                  if (bn != null && bn.getDouble() < d) {
                     this.writeFail(this.getOpcNetwork().lex("proxyExt.outOfRange"));
                     this.opcWriteLog.message("Proxy::PerformWrite, Write failed as value out of range::" + this.getId());
                     return;
                  }
               }

               try {
                  int hresult = 0;
                  if (val instanceof BStatusBoolean) {
                     hresult = sio.writeBoolean(this.getServerHandle(), ((BStatusBoolean)val).getValue());
                  } else if (val instanceof BStatusNumeric) {
                     hresult = sio.writeNumeric(this.getServerHandle(), this.getOpcDataType().getOrdinal(), ((BStatusNumeric)val).getValue());
                  } else {
                     String strBoolVal = "";
                     String strTemp = ((BStatusString)val).getValue();
                     String[] strArr = TextUtil.split(strTemp, ',');
                     boolean isBool = true;
                     if (strArr.length == 0) {
                        if (this.getOpcActualDataType().getOrdinal() == 8) {
                           hresult = sio.writeString(this.getServerHandle(), ((BStatusString)val).getValue(), this.getOpcActualDataType().getOrdinal());
                        } else if (this.getOpcActualDataType().getOrdinal() == 8200) {
                           hresult = sio.writeString(this.getServerHandle(), ((BStatusString)val).getValue(), this.getOpcActualDataType().getOrdinal());
                        } else {
                           hresult = -1073479676;
                        }
                     } else if (this.getOpcActualDataType().getOrdinal() == 8) {
                        hresult = sio.writeString(this.getServerHandle(), ((BStatusString)val).getValue(), this.getOpcActualDataType().getOrdinal());
                     } else if (8203 != this.getOpcActualDataType().getOrdinal()) {
                        if (this.getOpcActualDataType().getOrdinal() == 8200) {
                           ((BStatusString)val).setValue(((BStatusString)val).getValue().trim());
                           hresult = sio.writeString(this.getServerHandle(), ((BStatusString)val).getValue(), this.getOpcActualDataType().getOrdinal());
                        } else {
                           int intValid = this.validateOpcDataType(val, this.getOpcActualDataType().getOrdinal());
                           if (0 == intValid) {
                              hresult = sio.writeString(this.getServerHandle(), ((BStatusString)val).getValue(), this.getOpcActualDataType().getOrdinal());
                           } else if (1 == intValid) {
                              hresult = -1073479669;
                           } else {
                              hresult = -1073479676;
                           }
                        }
                     } else {
                        for (int i = 0; i < strArr.length; i++) {
                           try {
                              strArr[i] = strArr[i].trim();
                              if (strArr[i].length() == 0) {
                                 hresult = -1073479676;
                                 isBool = false;
                              } else {
                                 int intTemp = Integer.parseInt(strArr[i]);
                                 if (intTemp != 0) {
                                    strBoolVal = strBoolVal + "true";
                                 } else {
                                    strBoolVal = strBoolVal + "false";
                                 }

                                 if (i != strArr.length - 1) {
                                    strBoolVal = strBoolVal + ",";
                                 }
                              }
                           } catch (Exception var10) {
                              isBool = false;
                              break;
                           }
                        }

                        if (isBool) {
                           ((BStatusString)val).setValue(strBoolVal);
                           hresult = sio.writeString(this.getServerHandle(), strBoolVal, this.getOpcActualDataType().getOrdinal());
                        } else {
                           hresult = -1073479676;
                        }
                     }
                  }

                  if (OpcEnv.failed(hresult)) {
                     this.opcWriteLog.error(this.getId() + " write failed 3 [val=" + val.getValueValue() + "] " + OpcEnv.getDescription(hresult));
                     this.writeFail(OpcEnv.getDescription(hresult));
                  } else {
                     this.opcWriteLog.trace("Proxy::PerformWrite, Write successful::" + this.getId());
                     this.writeOk(val);
                     if (this.getParentPoint().isSubscribed()) {
                        this.read();
                     }
                  }
               } catch (Exception var11) {
                  this.opcWriteLog.error("Write fail [val=" + val.getValueValue() + "] " + this.getId(), var11);
                  this.writeFail(var11.getMessage());
               }
            }
         }
      }
   }

   protected void readResult(BStatusValue val, long timestamp, int quality, int hresult) {
      this.setIsReadInProgress(false);
      this.setOpcReadTime(BAbsTime.make(timestamp));
      this.setOpcQuality(BOpcQuality.getQuality(quality));
      this.setOpcLimit(BOpcLimit.getLimit(quality));
      if (BOpcQuality.isBad(quality)) {
         BOpcQualityBad q = BOpcQualityBad.getQuality(quality);
         if (val != null) {
            val.setStatus(BStatus.fault);
         }

         this.setOpcQualitySubcode(q);
      } else if (BOpcQuality.isGood(quality)) {
         BOpcQualityGood q = BOpcQualityGood.getQuality(quality);
         if (val != null && q == BOpcQualityGood.localOverride) {
            val.setStatus(BStatus.overridden);
         }

         this.setOpcQualitySubcode(q);
      } else if (BOpcQuality.isUncertain(quality)) {
         this.setOpcQualitySubcode(BOpcQualityUncertain.getQuality(quality));
      } else {
         this.setOpcQualitySubcode(BOpcQualityUnknown.getQuality(quality));
      }

      if (val != null) {
         this.readOk(val);
      } else {
         this.readFail(OpcEnv.resultString(hresult));
         this.opcLog.trace("Proxy::write, Read failed with hresult ::" + hresult + "for Point ::" + this.getId());
      }
   }

   public int validateOpcDataType(BStatusValue val, int DataType) {
      int intValid = 0;
      String strTemp = ((BStatusString)val).getValue();
      String[] strArr = TextUtil.split(strTemp, ',');
      if (strArr.length == 0) {
         intValid = 2;
      } else {
         for (int i = 0; i < strArr.length; i++) {
            try {
               if (strArr[i].length() == 0) {
                  return 2;
               }

               double temp = Double.parseDouble(strArr[i]);
               BStatusNumeric strStatusArr = new BStatusNumeric(temp);
               double d = strStatusArr.getNumeric();
               BNumber bn = (BNumber)this.getDeviceFacets().get("min");
               if (bn != null && bn.getDouble() > d) {
                  return 1;
               }

               bn = (BNumber)this.getDeviceFacets().get("max");
               if (bn != null && bn.getDouble() < d) {
                  return 1;
               }

               if (DataType == 8208) {
                  if (d > 127.0 || d < -127.0) {
                     return 1;
                  }
               } else if (DataType == 8209) {
                  if (d > 255.0 || d < 0.0) {
                     return 1;
                  }
               } else if (DataType == 8210) {
                  if (d > 65535.0 || d < 0.0) {
                     return 1;
                  }
               } else if (DataType == 8211 && d < 0.0) {
                  return 1;
               }
            } catch (Exception var13) {
               intValid = 2;
            }

            if (intValid != 0) {
               break;
            }
         }
      }

      return intValid;
   }

   public boolean getItemIDChanged() {
      return this.itemIdChanged;
   }

   public void setItemIDChanged(boolean changed) {
      this.itemIdChanged = changed;
   }

   private boolean isWritablePoint() {
      boolean isWritablePoint = false;
      BControlPoint parentPt = this.getParentPoint();
      TypeInfo info = parentPt.getType().getTypeInfo();
      if (info.is(BIWritablePoint.TYPE)) {
         isWritablePoint = true;
      } else {
         isWritablePoint = false;
      }

      return isWritablePoint;
   }

   public boolean getIsReadInProgress() {
      synchronized (this.syncIsReadInProgress) {
         return this.isReadInProgress;
      }
   }

   public synchronized void setIsReadInProgress(boolean isInProg) {
      synchronized (this.syncIsReadInProgress) {
         this.isReadInProgress = isInProg;
      }
   }

   public boolean getIsWriteInProgress() {
      synchronized (this.syncIsWriteInProgress) {
         return this.isWriteInProgress;
      }
   }

   public synchronized void setIsWriteInProgress(boolean isInProg) {
      synchronized (this.syncIsWriteInProgress) {
         this.isWriteInProgress = isInProg;
      }
   }

   public synchronized boolean getIsAddRemoveInProgress() {
      return this.isAddRemoveInProgress;
   }

   public synchronized void setIsAddRemoveInProgress(boolean isInProg) {
      this.isAddRemoveInProgress = isInProg;
   }

   public boolean getIsInUse() {
      return this.getIsReadInProgress() || this.getIsWriteInProgress();
   }

   public void setSubscribed(boolean value) {
      this.subscribed = value;
   }

   public void readOk(BStatusValue newValue) {
      super.readOk(newValue);
      if (this.getIsReadInProgress()) {
         this.setIsReadInProgress(false);
      }
   }

   public void readFail(String cause) {
      super.readFail(cause);
      if (this.getIsReadInProgress()) {
         this.setIsReadInProgress(false);
      }
   }

   public void writeOk(BStatusValue writeValue) {
      super.writeOk(writeValue);
      if (this.getIsWriteInProgress()) {
         this.setIsWriteInProgress(false);
      }
   }

   public void writeFail(String cause) {
      super.writeFail(cause);
      if (this.getIsWriteInProgress()) {
         this.setIsWriteInProgress(false);
      }
   }
}
