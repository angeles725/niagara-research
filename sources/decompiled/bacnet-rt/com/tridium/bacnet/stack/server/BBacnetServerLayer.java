package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetAbort;
import com.tridium.bacnet.services.BacnetComplexAck;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetError;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.IAmListener;
import com.tridium.bacnet.stack.IHaveListener;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import com.tridium.bacnet.stack.server.object.BObjectHandler;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.export.BBacnetFileDescriptor;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.BBacnetComm;
import javax.baja.bacnet.io.BacnetServiceListener;
import javax.baja.bacnet.io.EventNotificationListener;
import javax.baja.bacnet.io.PrivateTransferListener;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.BBacnetWorker;
import javax.baja.bacnet.util.worker.IBacnetAddress;
import javax.baja.file.BIFile;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.Lexicon;
import javax.baja.util.QueueFullException;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "worker",
      type = "BBacnetWorker",
      defaultValue = "new BBacnetWorker(\"BacnetServer:worker\")"
   ), @NiagaraProperty(
      name = "eventHandler",
      type = "BEventHandler",
      defaultValue = "new BEventHandler()"
   ), @NiagaraProperty(
      name = "reinitializeAllowed",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "timeSynchAllowed",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "updateStatusOnCov",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "issueUnicastIHave",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "confirmedWorker",
      type = "BBacnetWorker",
      defaultValue = "new BBacnetWorker(\"BacnetServer:confirmedWorker\")"
   ), @NiagaraProperty(
      name = "objectHandler",
      type = "BObjectHandler",
      defaultValue = "new BObjectHandler()"
   ), @NiagaraProperty(
      name = "overrideMode",
      type = "BOverrideMode",
      defaultValue = "BOverrideMode.legacy",
      flags = 4
   )})
@NiagaraAction(
   name = "checkBackupComm",
   flags = 4
)
public class BBacnetServerLayer extends BComponent implements BacnetConfirmedServiceChoice, BacnetUnconfirmedServiceChoice {
   public static final Property worker = newProperty(0, new BBacnetWorker("BacnetServer:worker"), null);
   public static final Property eventHandler = newProperty(0, new BEventHandler(), null);
   public static final Property reinitializeAllowed = newProperty(4, false, null);
   public static final Property timeSynchAllowed = newProperty(4, false, null);
   public static final Property updateStatusOnCov = newProperty(4, false, null);
   public static final Property issueUnicastIHave = newProperty(4, false, null);
   public static final Property confirmedWorker = newProperty(0, new BBacnetWorker("BacnetServer:confirmedWorker"), null);
   public static final Property objectHandler = newProperty(0, new BObjectHandler(), null);
   public static final Property overrideMode = newProperty(4, BOverrideMode.legacy, null);
   public static final Action checkBackupComm = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetServerLayer.class);
   public static final String BACNET_USER = "Bacnet";
   public static final int NO_INVOKE_ID = -1;
   private DeviceHandler deviceHandler;
   private PropertyHandler propertyHandler;
   private FileHandler fileHandler;
   private CovHandler covHandler;
   private TimeSyncHandler timeSyncHandler;
   private PrivateTransferHandler privateTransferHandler;
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static BBacnetServerLayer cachedServerLayer;
   Ticket checkBackupTicket = Clock.expiredTicket;
   long lastBackupRestoreCommTime = 0L;
   private static final Logger logger = Logger.getLogger("bacnet.server");

   public BBacnetWorker getWorker() {
      return (BBacnetWorker)this.get(worker);
   }

   public void setWorker(BBacnetWorker v) {
      this.set(worker, v, null);
   }

   public BEventHandler getEventHandler() {
      return (BEventHandler)this.get(eventHandler);
   }

   public void setEventHandler(BEventHandler v) {
      this.set(eventHandler, v, null);
   }

   public boolean getReinitializeAllowed() {
      return this.getBoolean(reinitializeAllowed);
   }

   public void setReinitializeAllowed(boolean v) {
      this.setBoolean(reinitializeAllowed, v, null);
   }

   public boolean getTimeSynchAllowed() {
      return this.getBoolean(timeSynchAllowed);
   }

   public void setTimeSynchAllowed(boolean v) {
      this.setBoolean(timeSynchAllowed, v, null);
   }

   public boolean getUpdateStatusOnCov() {
      return this.getBoolean(updateStatusOnCov);
   }

   public void setUpdateStatusOnCov(boolean v) {
      this.setBoolean(updateStatusOnCov, v, null);
   }

   public boolean getIssueUnicastIHave() {
      return this.getBoolean(issueUnicastIHave);
   }

   public void setIssueUnicastIHave(boolean v) {
      this.setBoolean(issueUnicastIHave, v, null);
   }

   public BBacnetWorker getConfirmedWorker() {
      return (BBacnetWorker)this.get(confirmedWorker);
   }

   public void setConfirmedWorker(BBacnetWorker v) {
      this.set(confirmedWorker, v, null);
   }

   public BObjectHandler getObjectHandler() {
      return (BObjectHandler)this.get(objectHandler);
   }

   public void setObjectHandler(BObjectHandler v) {
      this.set(objectHandler, v, null);
   }

   public BOverrideMode getOverrideMode() {
      return (BOverrideMode)this.get(overrideMode);
   }

   public void setOverrideMode(BOverrideMode v) {
      this.set(overrideMode, v, null);
   }

   public void checkBackupComm() {
      this.invoke(checkBackupComm, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BBacnetServerLayer getServerLayer() {
      BBacnetServerLayer serverLayer = cachedServerLayer;
      if (serverLayer == null || serverLayer.getComponentSpace() == null || !serverLayer.isRunning()) {
         BBacnetNetwork network = BBacnetNetwork.bacnet();
         BBacnetComm comm = network != null ? network.getBacnetComm() : null;
         if (comm instanceof BBacnetStack) {
            serverLayer = ((BBacnetStack)comm).getServer();
            cachedServerLayer = serverLayer;
         } else {
            serverLayer = null;
            cachedServerLayer = null;
         }
      }

      return serverLayer;
   }

   public void started() {
      this.deviceHandler = new DeviceHandler(this);
      this.propertyHandler = new PropertyHandler(this);
      this.fileHandler = new FileHandler(this);
      this.covHandler = new CovHandler();
      this.timeSyncHandler = new TimeSyncHandler(this);
      this.privateTransferHandler = new PrivateTransferHandler(this);
      setServicesSupportedBit(18, false);
      setServicesSupportedBit(30, false);
      boolean[] servicesSupported = BBacnetNetwork.localDevice().getProtocolServicesSupported().getBits();
      boolean timeSynchAllowed = this.getTimeSynchAllowed();
      if (servicesSupported[32] != timeSynchAllowed || servicesSupported[36] != timeSynchAllowed) {
         setServicesSupportedBit(32, timeSynchAllowed);
         setServicesSupportedBit(36, timeSynchAllowed);
      }

      boolean reinitializeAllowed = this.getReinitializeAllowed() && BBacnetNetwork.bacnet().hasServerLicense();
      if (servicesSupported[20] != reinitializeAllowed) {
         setServicesSupportedBit(20, reinitializeAllowed);
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(timeSynchAllowed)) {
            setServicesSupportedBit(32, this.getTimeSynchAllowed());
            setServicesSupportedBit(36, this.getTimeSynchAllowed());
         } else if (p.equals(reinitializeAllowed)) {
            setServicesSupportedBit(20, this.getReinitializeAllowed() && BBacnetNetwork.bacnet().hasServerLicense());
         }
      }
   }

   public void stackStopped() {
      this.deviceHandler = null;
      this.propertyHandler = null;
      this.fileHandler = null;
      this.covHandler = null;
      this.timeSyncHandler = null;
      this.privateTransferHandler = null;
      setServicesSupportedBit(18, false);
      setServicesSupportedBit(30, false);
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetStack;
   }

   final BBacnetStack stack() {
      return (BBacnetStack)this.getParent();
   }

   void scheduleBackupRestoreFailure() {
      logger.fine("Scheduling new checkBackupTicket");
      this.checkBackupTicket.cancel();
      this.updateLastBackupRestoreCommTime();
      this.checkBackupTicket = Clock.schedulePeriodically(this, BRelTime.makeSeconds(30), checkBackupComm, null);
   }

   public void doCheckBackupComm() {
      synchronized (this.deviceHandler.backupRestoreStepLock) {
         switch (this.deviceHandler.backupRestoreStep) {
            case FINISHED_BACKUP:
            case BACKUP_FAILED:
            case READY_FOR_RESTORE:
            case RESTORE_FAILED:
               long timeSinceBackupComm = Clock.ticks() - this.lastBackupRestoreCommTime;
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(
                     "Checking backup comm: lastBackupCommTime = " + this.lastBackupRestoreCommTime + "; time since last comm [ms] = " + timeSinceBackupComm
                  );
               }

               BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
               if (timeSinceBackupComm > localDevice.getBackupFailureTimeout().getMillis()) {
                  logger.info(
                     "Last comm time was "
                        + this.lastBackupRestoreCommTime
                        + " and "
                        + timeSinceBackupComm
                        + " ms ago; Backup_Failure_Timeout is "
                        + localDevice.getBackupFailureTimeout()
                        + "; exiting Backup/Restore mode..."
                  );
                  this.checkBackupTicket.cancel();
                  this.cleanupBackupMode();
                  localDevice.setBackupAndRestoreState(BBacnetBackupState.idle);
                  localDevice.restoreSystemStatus();
                  this.deviceHandler.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
               }
               break;
            case RUNNING_RESTORE:
               this.updateLastBackupRestoreCommTime();
               break;
            default:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Cancelling checkBackupTicket that should not be active in backupRestoreStep " + this.deviceHandler.backupRestoreStep);
               }

               this.checkBackupTicket.cancel();
         }
      }
   }

   void updateLastBackupRestoreCommTime() {
      this.lastBackupRestoreCommTime = Clock.ticks();
   }

   public void cleanupBackupMode() {
      this.deviceHandler.backupRestoreClient = null;
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      localDevice.getConfigurationFiles().setSize(0);
      BBacnetFileDescriptor fileDesc = this.deviceHandler.backupRestoreFileDesc.getAndSet(null);
      if (fileDesc != null) {
         BComponent parent = (BComponent)fileDesc.getParent();
         if (parent != null) {
            parent.remove(fileDesc);
         }
      }

      BIFile file = this.deviceHandler.backupRestoreFile.getAndSet(null);
      if (file != null) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               file.delete();
               return null;
            }));
         } catch (Exception var6) {
            Exception unwrapped = var6;
            if (var6 instanceof PrivilegedActionException) {
               unwrapped = ((PrivilegedActionException)var6).getException();
            }

            logger.log(Level.SEVERE, "Exception occurred while deleting the backup/restore file in cleanupBackupRestore", (Throwable)unwrapped);
         }
      }
   }

   public void iAm() {
      this.iAm(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS);
   }

   public void iAm(BBacnetAddress sourceAddress) {
      if (this.stack().isCommExecutionEnabled()) {
         BLocalBacnetDevice local = BBacnetNetwork.localDevice();
         IAmRequest request = new IAmRequest(local.getObjectId(), local.getMaxAPDULengthAccepted(), local.getSegmentationSupported(), local.getVendorId());
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Server.Sending I-Am....\n" + request + " to " + sourceAddress);
         }

         try {
            this.transport().sendUnconfirmedRequest(request, sourceAddress, BNetworkPriority.normal);
         } catch (BacnetException var5) {
            logger.log(Level.SEVERE, "Exception sending I-Am service!", (Throwable)var5);
         }
      }
   }

   public void iHave(BBacnetObjectIdentifier objectId, String objectName, BCharacterSetEncoding encoding) {
      this.iHave(objectId, objectName, encoding, BBacnetAddress.GLOBAL_BROADCAST_ADDRESS);
   }

   public void iHave(BBacnetObjectIdentifier objectId, String objectName, BCharacterSetEncoding encoding, BBacnetAddress sourceAddress) {
      if (this.stack().isCommInitiationEnabled()) {
         IHaveRequest request = new IHaveRequest(this.bacnet().getObjectId(), objectId, objectName, encoding);
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Server.Sending I-Have....\n" + request);
         }

         try {
            if (!this.getIssueUnicastIHave()) {
               sourceAddress = BBacnetAddress.GLOBAL_BROADCAST_ADDRESS;
            }

            this.transport().sendUnconfirmedRequest(request, sourceAddress, BNetworkPriority.normal);
         } catch (BacnetException var7) {
            logger.log(Level.SEVERE, "Exception sending I-Have service!", (Throwable)var7);
         }
      }
   }

   public void receiveConfirmedRequest(BBacnetAddress srcAddr, int invokeId, BacnetConfirmedRequest request, BNetworkPriority networkPriority) {
      BBacnetServerLayer.ServerRequest sr = new BBacnetServerLayer.ServerRequest(srcAddr, invokeId, request, networkPriority);
      this.getConfirmedWorker().post(sr);
   }

   public void receiveUnconfirmedRequest(BBacnetAddress srcAddr, BacnetUnconfirmedRequest request, BNetworkPriority networkPriority) {
      BBacnetServerLayer.ServerRequest sr = new BBacnetServerLayer.ServerRequest(srcAddr, -1, request, networkPriority);

      try {
         this.getWorker().post(sr);
      } catch (QueueFullException var6) {
         logger.log(Level.SEVERE, "QueueFullException in receiveUnconfirmedRequest", (Throwable)var6);
      }
   }

   protected void process(BBacnetServerLayer.ServerRequest sr) {
      try {
         switch (sr.getType()) {
            case 0:
               this.processConfirmedRequest(sr);
               break;
            case 1:
               this.processUnconfirmedRequest(sr);
               break;
            default:
               logger.info("Bacnet server request ignored: " + sr.getType());
         }
      } catch (RejectException var3) {
         this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), new BacnetReject(var3.getRejectReason()), sr.getNetworkPriority());
      } catch (Exception var4) {
         logger.log(Level.SEVERE, "Bacnet Server Error!", (Throwable)var4);
      }
   }

   private void processUnconfirmedRequest(BBacnetServerLayer.ServerRequest sr) {
      if (this.isFromNonBackupRestoreClient(sr) && logger.isLoggable(Level.FINE)) {
         logger.fine("Unconfirmed Request from non-backup client ignored...");
      }

      BacnetUnconfirmedRequest request = (BacnetUnconfirmedRequest)sr.getRequest();
      ServiceHandler handler = null;
      int serviceChoice = request.getServiceChoice();
      switch (serviceChoice) {
         case 0:
         case 1:
         case 7:
         case 8:
            handler = this.deviceHandler;
            break;
         case 2:
            handler = this.covHandler;
            break;
         case 3:
            handler = this.getEventHandler();
            break;
         case 4:
            handler = this.privateTransferHandler;
            break;
         case 5:
         default:
            logger.info("BBacnetServerLayer: Unknown unconfirmed service choice encountered " + serviceChoice);
            break;
         case 6:
         case 9:
            handler = this.timeSyncHandler;
      }

      if (handler != null) {
         handler.receiveRequest(serviceChoice, request, sr.getSrcAddr());
      } else {
         logger.warning("BBacnetServerLayer: No handler for unconfirmed service request! " + request);
      }
   }

   private boolean isFromNonBackupRestoreClient(BBacnetServerLayer.ServerRequest serverRequest) {
      BBacnetAddress backupRestoreClientAddress = this.deviceHandler.backupRestoreClient;
      if (backupRestoreClientAddress != null) {
         BBacnetAddress srcAddr = serverRequest.getSrcAddr();
         return !backupRestoreClientAddress.equals(srcAddr.getNetworkNumber(), srcAddr.getMacAddress().getBytes());
      } else {
         return false;
      }
   }

   private void processConfirmedRequest(BBacnetServerLayer.ServerRequest sr) throws RejectException {
      if (this.isFromNonBackupRestoreClient(sr)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("Confirmed Request from non-backup client rejected...");
         }

         this.sendConfigurationInProgress(sr);
      } else {
         BacnetConfirmedRequest request = (BacnetConfirmedRequest)sr.getRequest();
         int serviceChoice = request.getServiceChoice();
         boolean serviceSupported = BBacnetNetwork.localDevice().getProtocolServicesSupported().getBit(request.getServiceBitIndex());
         if (!serviceSupported) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("Throwing reject for unsupported service: " + serviceChoice + " [" + BacnetConfirmedServiceChoice.TAGS[serviceChoice] + ']');
            }

            throw new RejectException(9);
         } else {
            ServiceHandler handler = null;
            switch (serviceChoice) {
               case 0:
               case 2:
               case 3:
               case 4:
               case 29:
                  handler = this.getEventHandler();
                  break;
               case 1:
               case 5:
               case 28:
                  handler = this.covHandler;
                  break;
               case 6:
               case 7:
                  handler = this.fileHandler;
                  break;
               case 8:
               case 9:
               case 12:
               case 14:
               case 15:
               case 16:
               case 26:
                  handler = this.propertyHandler;
                  break;
               case 10:
               case 11:
                  handler = this.getObjectHandler();
                  break;
               case 13:
               case 19:
               case 21:
               case 22:
               case 23:
               case 24:
               case 25:
               case 27:
               default:
                  logger.info("BBacnetServerLayer: Unknown confirmed service choice encountered: " + serviceChoice);
                  throw new RejectException(9);
               case 17:
               case 20:
                  handler = this.deviceHandler;
                  break;
               case 18:
                  handler = this.privateTransferHandler;
            }

            if (handler == null) {
               logger.warning("BBacnetServerLayer: No handler for confirmed service request! " + request);
            } else {
               BacnetServicePrimitive response = handler.receiveRequest(serviceChoice, request, sr.getSrcAddr());
               switch (response.getServiceType()) {
                  case 2:
                     this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), (BacnetSimpleAck)response, sr.getNetworkPriority());
                     break;
                  case 3:
                     this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), (BacnetComplexAck)response, sr.getNetworkPriority());
                     break;
                  case 4:
                  default:
                     logger.severe("BBacnetServerLayer: Unknown response type from handler! " + response.getServiceType());
                     break;
                  case 5:
                     this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), (BacnetError)response, sr.getNetworkPriority());
                     break;
                  case 6:
                     this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), (BacnetReject)response, sr.getNetworkPriority());
                     break;
                  case 7:
                     this.transport().sendConfirmedResponse(sr.getSrcAddr(), sr.getInvokeId(), (BacnetAbort)response, sr.getNetworkPriority());
               }
            }
         }
      }
   }

   private void sendConfigurationInProgress(BBacnetServerLayer.ServerRequest sr) {
      this.transport()
         .sendConfirmedResponse(
            sr.getSrcAddr(), sr.getInvokeId(), new SimpleError(sr.getRequest().getServiceChoice(), new NErrorType(0, 2)), sr.getNetworkPriority()
         );
   }

   public void registerBacnetServiceListener(BacnetServiceListener listener, int serviceIndex) {
      if (listener != null) {
         String serviceName = lex.getText("BacnetServicesSupported.bit" + serviceIndex);
         boolean err = false;

         try {
            switch (serviceIndex) {
               case 2:
               case 29:
                  if (listener instanceof EventNotificationListener) {
                     this.getEventHandler().addListener((EventNotificationListener)listener, serviceIndex);
                  } else {
                     err = true;
                  }
                  break;
               case 18:
               case 30:
                  if (listener instanceof PrivateTransferListener) {
                     this.privateTransferHandler.addListener((PrivateTransferListener)listener, serviceIndex == 18);
                     setServicesSupportedBit(serviceIndex, true);
                  } else {
                     err = true;
                  }
                  break;
               case 26:
                  if (listener instanceof IAmListener) {
                     this.deviceHandler.addIAmListener((IAmListener)listener);
                  } else {
                     err = true;
                  }
                  break;
               case 27:
                  if (listener instanceof IHaveListener) {
                     this.deviceHandler.addIHaveListener((IHaveListener)listener);
                  } else {
                     err = true;
                  }
               case 33:
               case 34:
            }
         } catch (Exception var6) {
            logger.log(Level.WARNING, "Unable to register " + listener.getClass() + " for service " + serviceName + ":" + var6, (Throwable)var6);
         }

         if (err) {
            logger.warning("Listener/Service mismatch: attempt to register " + listener.getClass() + " for service " + serviceName);
         } else {
            logger.info("Listener " + listener + " registered for service:" + serviceName);
         }
      }
   }

   public void unregisterBacnetServiceListener(BacnetServiceListener listener, int serviceIndex) {
      if (listener != null) {
         String serviceName = lex.getText("BacnetServicesSupported.bit" + serviceIndex);
         boolean err = false;
         switch (serviceIndex) {
            case 2:
            case 29:
               if (listener instanceof EventNotificationListener) {
                  this.getEventHandler().removeListener((EventNotificationListener)listener, serviceIndex);
               } else {
                  err = true;
               }
               break;
            case 18:
               if (listener instanceof PrivateTransferListener) {
                  this.privateTransferHandler.removeListener((PrivateTransferListener)listener, true);
                  setServicesSupportedBit(serviceIndex, !this.privateTransferHandler.confirmedListeners.isEmpty());
               } else {
                  err = true;
               }
               break;
            case 26:
               if (listener instanceof IAmListener) {
                  this.deviceHandler.removeIAmListener((IAmListener)listener);
               } else {
                  err = true;
               }
               break;
            case 27:
               if (listener instanceof IHaveListener) {
                  this.deviceHandler.removeIHaveListener((IHaveListener)listener);
               } else {
                  err = true;
               }
               break;
            case 30:
               if (listener instanceof PrivateTransferListener) {
                  this.privateTransferHandler.removeListener((PrivateTransferListener)listener, false);
                  setServicesSupportedBit(serviceIndex, !this.privateTransferHandler.unconfirmedListeners.isEmpty());
               } else {
                  err = true;
               }
            case 33:
            case 34:
         }

         if (err) {
            logger.warning("Listener/Service mismatch: attempt to unregister " + listener.getClass() + " for service " + serviceName);
         } else {
            logger.info("Listener " + listener + " unregistered for service:" + serviceName);
         }
      }
   }

   private static void setServicesSupportedBit(int index, boolean newState) {
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      BBacnetBitString newBitString = BBacnetBitString.make(localDevice.getProtocolServicesSupported(), index, newState);
      localDevice.setProtocolServicesSupported(newBitString);
   }

   @Deprecated
   public void registerPrivateTransferListener(PrivateTransferListener listener) {
      logger.warning("This method is deprecated! Use registerBacnetServiceListener() instead.");
   }

   @Deprecated
   public void unregisterPrivateTransferListener(PrivateTransferListener listener) {
      logger.warning("This method is deprecated! Use unregisterBacnetServiceListener() instead.");
   }

   public void registerIAmListener(IAmListener listener) {
      this.deviceHandler.addIAmListener(listener);
   }

   public void unregisterIAmListener(IAmListener listener) {
      this.deviceHandler.removeIAmListener(listener);
   }

   public void registerIHaveListener(IHaveListener listener) {
      this.deviceHandler.addIHaveListener(listener);
   }

   public void unregisterIHaveListener(IHaveListener listener) {
      this.deviceHandler.removeIHaveListener(listener);
   }

   BBacnetNetwork bacnet() {
      return (BBacnetNetwork)this.getParent().getParent();
   }

   private BBacnetTransportLayer transport() {
      return ((BBacnetStack)this.getParent()).getTransport();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetServerLayer", 2);
      out.prop("deviceHandler", this.deviceHandler);
      out.prop("propertyHandler", this.propertyHandler);
      out.prop("fileHandler", this.fileHandler);
      out.prop("covHandler", this.covHandler);
      out.prop("timeSyncHandler", this.timeSyncHandler);
      out.prop("privateTransferHandler", this.privateTransferHandler);
      out.trTitle("Private Transfer ConfirmedListeners:" + this.privateTransferHandler.confirmedListeners.size(), 2);
      Iterator it = this.privateTransferHandler.confirmedListeners.iterator();

      while (it.hasNext()) {
         Object o = it.next();
         out.prop("  " + it.key(), o);
      }

      out.trTitle("Private Transfer UnconfirmedListeners:" + this.privateTransferHandler.unconfirmedListeners.size(), 2);
      it = this.privateTransferHandler.unconfirmedListeners.iterator();

      while (it.hasNext()) {
         Object o = it.next();
         out.prop("  " + it.key(), o);
      }

      out.prop("backupFileId", this.getBackupRestoreFileId());
      out.prop("exitBackupTicket", this.checkBackupTicket);
      out.prop("now", Clock.ticks());
      out.prop("lastBackupCommTime", this.lastBackupRestoreCommTime);
      out.prop("backupRestoreClientAddress", this.deviceHandler.backupRestoreClient);
      out.endProps();
   }

   BBacnetObjectIdentifier getBackupRestoreFileId() {
      BBacnetFileDescriptor fileDesc = this.deviceHandler.backupRestoreFileDesc.get();
      return fileDesc == null ? null : fileDesc.getObjectId();
   }

   private class ServerRequest implements IBacnetAddress, Runnable {
      private final BBacnetAddress srcAddr;
      private final int invokeId;
      private final BacnetServicePrimitive request;
      private final BNetworkPriority networkPriority;

      public ServerRequest(BBacnetAddress srcAddr, int invokeId, BacnetServicePrimitive request, BNetworkPriority networkPriority) {
         this.srcAddr = srcAddr;
         this.invokeId = invokeId;
         this.request = request;
         this.networkPriority = networkPriority;
      }

      public int getServiceType() {
         return this.getType();
      }

      public int getType() {
         return this.request.getServiceType();
      }

      public BacnetServicePrimitive getRequest() {
         return this.request;
      }

      @Override
      public BBacnetAddress getAddress() {
         return this.srcAddr;
      }

      public BBacnetAddress getSrcAddr() {
         return this.srcAddr;
      }

      public int getInvokeId() {
         return this.invokeId;
      }

      public BNetworkPriority getNetworkPriority() {
         return this.networkPriority;
      }

      @Override
      public void run() {
         BBacnetServerLayer.this.process(this);
      }
   }
}
