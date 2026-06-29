package com.tridium.opc.client;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.client.point.BOpcPointDeviceExt;
import com.tridium.opc.jni.client.common.OpcServer;
import com.tridium.opc.jni.client.da.OpcDaServer;
import java.security.AccessController;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLong;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "serverState",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "serverCurrentTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "serverLastUpdateTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "serverStartTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "serverGroupCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "serverBandWidth",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "serverMajorVersion",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "serverMinorVersion",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "serverBuildNumber",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "serverVendorInfo",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "lastConnectTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   ), @NiagaraProperty(
      name = "lastShutdownRequest",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   ), @NiagaraProperty(
      name = "points",
      type = "BOpcPointDeviceExt",
      defaultValue = "new BOpcPointDeviceExt()"
   ), @NiagaraProperty(
      name = "security",
      type = "BOpcDASecurity",
      defaultValue = "new BOpcDASecurity()"
   ), @NiagaraProperty(
      name = "isUserLoggedIn",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   )})
@NiagaraActions({@NiagaraAction(
      name = "attach",
      flags = 20,
      override = true
   ), @NiagaraAction(
      name = "detach",
      flags = 20,
      override = true
   ), @NiagaraAction(
      name = "setSecurity",
      returnType = "BLong",
      flags = 4
   ), @NiagaraAction(
      name = "getServerSecured",
      returnType = "BBoolean",
      flags = 4
   ), @NiagaraAction(
      name = "setServerSecured",
      parameterType = "BBoolean",
      defaultValue = "BBoolean.FALSE",
      flags = 4
   )})
public class BOpcDaClient extends BOpcDevice {
   public static final Property serverState = newProperty(3, "", null);
   public static final Property serverCurrentTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property serverLastUpdateTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property serverStartTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property serverGroupCount = newProperty(3, 0, null);
   public static final Property serverBandWidth = newProperty(3, 0, null);
   public static final Property serverMajorVersion = newProperty(3, 0, null);
   public static final Property serverMinorVersion = newProperty(3, 0, null);
   public static final Property serverBuildNumber = newProperty(3, 0, null);
   public static final Property serverVendorInfo = newProperty(3, "", null);
   public static final Property lastConnectTime = newProperty(1, BAbsTime.NULL, null);
   public static final Property lastShutdownRequest = newProperty(1, BAbsTime.NULL, null);
   public static final Property points = newProperty(0, new BOpcPointDeviceExt(), null);
   public static final Property security = newProperty(0, new BOpcDASecurity(), null);
   public static final Property isUserLoggedIn = newProperty(5, false, null);
   public static final Action attach = newAction(20, null);
   public static final Action detach = newAction(20, null);
   public static final Action setSecurity = newAction(4, null);
   public static final Action getServerSecured = newAction(4, null);
   public static final Action setServerSecured = newAction(4, BBoolean.FALSE, null);
   public static final Type TYPE = Sys.loadType(BOpcDaClient.class);
   private Object detachMutex = new Object();
   private Object attachMutex = new Object();
   private OpcDaServer server;
   boolean bNTSecurityInterfaceSupport = false;
   boolean bPrivateSecurityInterfaceSupport = false;
   Log opcLog = Log.getLog("OpcDaLog");
   Log opcPingLog = Log.getLog("OpcDaPingLog");

   public String getServerState() {
      return this.getString(serverState);
   }

   public void setServerState(String v) {
      this.setString(serverState, v, null);
   }

   public BAbsTime getServerCurrentTime() {
      return (BAbsTime)this.get(serverCurrentTime);
   }

   public void setServerCurrentTime(BAbsTime v) {
      this.set(serverCurrentTime, v, null);
   }

   public BAbsTime getServerLastUpdateTime() {
      return (BAbsTime)this.get(serverLastUpdateTime);
   }

   public void setServerLastUpdateTime(BAbsTime v) {
      this.set(serverLastUpdateTime, v, null);
   }

   public BAbsTime getServerStartTime() {
      return (BAbsTime)this.get(serverStartTime);
   }

   public void setServerStartTime(BAbsTime v) {
      this.set(serverStartTime, v, null);
   }

   public int getServerGroupCount() {
      return this.getInt(serverGroupCount);
   }

   public void setServerGroupCount(int v) {
      this.setInt(serverGroupCount, v, null);
   }

   public int getServerBandWidth() {
      return this.getInt(serverBandWidth);
   }

   public void setServerBandWidth(int v) {
      this.setInt(serverBandWidth, v, null);
   }

   public int getServerMajorVersion() {
      return this.getInt(serverMajorVersion);
   }

   public void setServerMajorVersion(int v) {
      this.setInt(serverMajorVersion, v, null);
   }

   public int getServerMinorVersion() {
      return this.getInt(serverMinorVersion);
   }

   public void setServerMinorVersion(int v) {
      this.setInt(serverMinorVersion, v, null);
   }

   public int getServerBuildNumber() {
      return this.getInt(serverBuildNumber);
   }

   public void setServerBuildNumber(int v) {
      this.setInt(serverBuildNumber, v, null);
   }

   public String getServerVendorInfo() {
      return this.getString(serverVendorInfo);
   }

   public void setServerVendorInfo(String v) {
      this.setString(serverVendorInfo, v, null);
   }

   public BAbsTime getLastConnectTime() {
      return (BAbsTime)this.get(lastConnectTime);
   }

   public void setLastConnectTime(BAbsTime v) {
      this.set(lastConnectTime, v, null);
   }

   public BAbsTime getLastShutdownRequest() {
      return (BAbsTime)this.get(lastShutdownRequest);
   }

   public void setLastShutdownRequest(BAbsTime v) {
      this.set(lastShutdownRequest, v, null);
   }

   public BOpcPointDeviceExt getPoints() {
      return (BOpcPointDeviceExt)this.get(points);
   }

   public void setPoints(BOpcPointDeviceExt v) {
      this.set(points, v, null);
   }

   public BOpcDASecurity getSecurity() {
      return (BOpcDASecurity)this.get(security);
   }

   public void setSecurity(BOpcDASecurity v) {
      this.set(security, v, null);
   }

   public boolean getIsUserLoggedIn() {
      return this.getBoolean(isUserLoggedIn);
   }

   public void setIsUserLoggedIn(boolean v) {
      this.setBoolean(isUserLoggedIn, v, null);
   }

   public BLong setSecurity() {
      return (BLong)this.invoke(setSecurity, null, null);
   }

   public BBoolean getServerSecured() {
      return (BBoolean)this.invoke(getServerSecured, null, null);
   }

   public void setServerSecured(BBoolean parameter) {
      this.invoke(setServerSecured, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void doAttach() {
      synchronized (this.attachMutex) {
         this.opcLog.trace("Connecting");
         if (this.isRunning()) {
            if (!this.getState().isEngaged() && !this.isDisabled() && !this.isFatalFault()) {
               try {
                  this.setAttaching();
                  if (this.getLocal()) {
                     if (this.getUseVersionIndependentProgId()) {
                        this.server = OpcDaServer.newServer(this.getVersionIndependentProgramId());
                     } else {
                        this.server = OpcDaServer.newServer(this.getProgramId());
                     }
                  } else {
                     this.server = OpcDaServer.newServer(this.getClassId(), this.getAddress());
                  }

                  if (this.server.getPeerObject() == 0L) {
                     this.pingFail("Cannot connect: Server creation failed ");
                     this.opcPingLog.warning("Cannot connect: Server creation failed, calling pingFail ");
                     this.setDetached();
                     return;
                  }

                  this.bNTSecurityInterfaceSupport = false;
                  this.bPrivateSecurityInterfaceSupport = false;

                  try {
                     if (this.server.getNTSecurity() != null) {
                        this.bNTSecurityInterfaceSupport = true;
                     }

                     if (this.server.getPrivateSecurity() != null) {
                        this.bPrivateSecurityInterfaceSupport = true;
                     }
                  } catch (Exception var7) {
                     this.opcLog.trace("Server does not support OpcSecurityNT/OpcSecurityPrivate interface");
                  }

                  this.MakeSlotsReadOnly();
                  this.setLoginSettingsToCurrentInstance();
                  this.setAttached();
                  OpcDaServer.Status status = this.server.getStatus();
                  this.setLastConnectTime(Clock.time());
                  boolean isOk = this.setServerStatusInfo(status);
                  if (isOk) {
                     this.pingOk();
                     this.opcPingLog.trace("Server is in running state, connected. Calling pingok()");
                  } else {
                     this.opcPingLog.warning("Server is not in running state ");
                  }

                  try {
                     this.server.setShutdownListener(new BOpcDaClient.Shutdown());
                     this.opcLog.trace("Registered shutdown callback.");
                  } catch (Throwable var6) {
                     this.opcLog.message("Unable to register shutdown callback: " + var6.toString(), var6);
                  }

                  this.getPoints().attach();
               } catch (Throwable var8) {
                  this.pingFail("Cannot connect: " + var8.toString());
                  this.opcPingLog.warning("Cannot connect, calling pingfail ", var8);
                  this.setDetached();
                  return;
               }
            }
         }
      }
   }

   @Override
   public void doDetach() {
      synchronized (this.detachMutex) {
         if (!this.getState().isDetached()) {
            try {
               this.opcLog.trace("Disconnecting");
               this.setDetaching();
               if (this.server != null) {
                  this.getPoints().detach();
                  this.server.release();
                  this.opcLog.trace("Server release() called");
               }

               this.opcLog.trace("Disconnected");
            } catch (Throwable var8) {
               this.opcLog.error("Failure disconnecting", var8);
               this.opcLog.trace("doDetach::AsyncIo set to false");
            } finally {
               this.server = null;
               this.setServerState("");
               this.setDetached();
               this.opcLog.trace("doDetach::Detached");
            }
         }
      }
   }

   @Override
   public void doPing() {
      OpcEnv.initializeThread();
      this.opcPingLog.trace("doPing()::Initialized Threads");

      try {
         if (this.getState().isAttached()) {
            this.opcPingLog.trace("doPing()::Server is attached already. Refreshing status");
            OpcDaServer.Status status = this.server.getStatus();
            this.setServerStartTime(BAbsTime.make(status.startTime, BTimeZone.UTC));
            this.setServerCurrentTime(BAbsTime.make(status.currentTime, BTimeZone.UTC));
            this.setServerLastUpdateTime(BAbsTime.make(status.lastUpdateTime, BTimeZone.UTC));
            boolean isOk = this.setServerStatusInfo(status);
            if (isOk) {
               this.pingOk();
               this.opcPingLog.trace("doPing()::Ping is successful");
            } else {
               this.pingFail(this.getServerState());
               this.opcPingLog.trace("doPing()::Ping failed. The server state is:: " + this.getServerState());
            }
         } else if (this.getState().isDetached()) {
            this.opcPingLog.trace("doPing()::Currently server detached. calling Attach()");
            this.attach();
         }
      } catch (Throwable var3) {
         this.opcPingLog.error("Ping failed", var3);
         this.pingFail(var3.getMessage());
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcNetwork || parent instanceof BOpcDeviceFolder;
   }

   private boolean setServerStatusInfo(OpcDaServer.Status status) {
      boolean isOk = this.updateServerState(status.state);
      this.setServerGroupCount(status.groupCount);
      this.setServerBandWidth(status.bandwidth);
      this.setServerMajorVersion(status.majorVersion);
      this.setServerMinorVersion(status.minorVersion);
      this.setServerBuildNumber(status.buildNumber);
      this.setServerVendorInfo(status.vendorInfo);
      return isOk;
   }

   public OpcDaServer getPeer() {
      return this.server;
   }

   private boolean updateServerState(int state) {
      String msg = null;
      boolean isOk = true;
      BOpcNetwork net = this.getOpcNetwork();
      if (net == null) {
         System.out.println("Network object is NULL");
         return isOk;
      } else {
         switch (state) {
            case 1:
               msg = net.lex("daServerState.running");
               isOk = true;
               break;
            case 2:
               msg = net.lex("daServerState.failed");
               isOk = false;
               break;
            case 3:
               msg = net.lex("daServerState.noConfig");
               isOk = false;
               break;
            case 4:
               msg = net.lex("daServerState.suspended");
               isOk = false;
               break;
            case 5:
               msg = net.lex("daServerState.test");
               isOk = true;
               break;
            case 6:
               msg = net.lex("daServerState.commFault");
               isOk = true;
               break;
            default:
               msg = net.lex("daServerState.unknown") + ": " + state;
               isOk = true;
         }

         if (!this.getServerState().equals(msg)) {
            this.setServerState(msg);
            if (!isOk) {
               this.opcLog.warning("Server state: " + msg);
            }
         }

         return isOk;
      }
   }

   public BLong doSetSecurity() {
      return BLong.make(this.setSecurityConfiguration());
   }

   public BBoolean doGetServerSecured() {
      return BBoolean.make(this.getIsUserLoggedIn());
   }

   public void doSetServerSecured(BBoolean blnlog) {
      this.setIsUserLoggedIn(blnlog.getBoolean());
   }

   public long setSecurityConfiguration() {
      long returnVal = 0L;
      long result = 0L;
      this.opcLog.trace("Set Security Configuration");
      if (!this.getSecurity().getPrivateSecurity()) {
         this.opcLog.trace("Private security is not selected");
         if (this.bNTSecurityInterfaceSupport) {
            this.opcLog.trace("NT security is supported by the server");
            result = this.getPeer()
               .getNTSecurity()
               .changeUser(this.getSecurity().getLoginName(), AccessController.doPrivileged(this.getSecurity().getLoginPassword()::getValue));
            switch ((int)result) {
               case -5:
                  returnVal = -5L;
                  break;
               case -4:
                  returnVal = -4L;
                  break;
               case -3:
                  returnVal = -3L;
                  break;
               case -2:
                  returnVal = -2L;
                  break;
               case -1:
                  returnVal = -1L;
                  break;
               case 0:
                  returnVal = 0L;
            }
         } else {
            returnVal = 1L;
         }
      } else if (this.getSecurity().getPrivateSecurity()) {
         this.opcLog.trace("Private security is selected");
         if (this.bPrivateSecurityInterfaceSupport) {
            this.opcLog.trace("Private security is supported by the server");
            if (this.getSecurity().getState().getOrdinal() == 0) {
               result = this.getPeer()
                  .getPrivateSecurity()
                  .logOn(this.getSecurity().getLoginName(), AccessController.doPrivileged(this.getSecurity().getLoginPassword()::getValue));
               switch ((int)result) {
                  case -5:
                     returnVal = -10L;
                     break;
                  case -4:
                     returnVal = -9L;
                     break;
                  case -3:
                     returnVal = -8L;
                     break;
                  case -2:
                     returnVal = -7L;
                     break;
                  case -1:
                     returnVal = -6L;
                     break;
                  case 0:
                     returnVal = 0L;
               }
            } else {
               boolean bResult = this.getPeer().getPrivateSecurity().logOff();
               if (bResult) {
                  returnVal = 0L;
               } else {
                  returnVal = -11L;
               }
            }
         } else {
            returnVal = 2L;
         }
      }

      this.setIsUserLoggedIn(true);
      this.lease(5);
      return returnVal;
   }

   private void setLoginSettingsToCurrentInstance() {
      if (this.getSecurity().getLoginName() != "" && AccessController.doPrivileged(this.getSecurity().getLoginPassword()::getValue) != "") {
         this.setSecurityConfiguration();
      }
   }

   private void MakeSlotsReadOnly() {
      if (!this.bNTSecurityInterfaceSupport && !this.bPrivateSecurityInterfaceSupport) {
         this.setFlags(this.getSlot("security"), 4);
      } else {
         this.getSecurity().MakeSlotsReadOnly(this.bNTSecurityInterfaceSupport, this.bPrivateSecurityInterfaceSupport);
      }
   }

   private class Shutdown implements OpcServer.ShutdownListener {
      private Shutdown() {
      }

      @Override
      public void shutdownDeleted() {
         if (BOpcDaClient.this.getState().isAttached() && BOpcDaClient.this.isRunning()) {
            BOpcDaClient.this.opcLog.warning("Shutdown callback deleted.");
            BOpcDaClient.this.server.setShutdownListener(this);
         }
      }

      @Override
      public void shutdownRequest(String reason) {
         BOpcDaClient.this.setLastShutdownRequest(Clock.time());
         BOpcDaClient.this.opcLog.warning("Shutdown request. Detaching...");
         BOpcDaClient.this.doDetach();
         BOpcDaClient.this.pingFail("Server shutdown request");
      }
   }
}
