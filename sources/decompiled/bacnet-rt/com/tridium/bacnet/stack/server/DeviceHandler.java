package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetReject;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetSimpleAck;
import com.tridium.bacnet.services.confirmed.DeviceCommunicationControlRequest;
import com.tridium.bacnet.services.confirmed.ReinitializeDeviceRequest;
import com.tridium.bacnet.services.error.SimpleError;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import com.tridium.bacnet.services.unconfirmed.WhoHasRequest;
import com.tridium.bacnet.services.unconfirmed.WhoIsRequest;
import com.tridium.bacnet.stack.DeviceRegistry;
import com.tridium.bacnet.stack.IAmListener;
import com.tridium.bacnet.stack.IHaveListener;
import com.tridium.platform.BSystemPlatformService;
import com.tridium.sys.station.Station;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.backup.BBackupService;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConfirmedServiceChoice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetBackupState;
import javax.baja.bacnet.enums.BBacnetDeviceStatus;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetRestartReason;
import javax.baja.bacnet.export.BBacnetFileDescriptor;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.export.BLocalBacnetDevice;
import javax.baja.bacnet.io.BBacnetComm;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.security.BPasswordCache;
import javax.baja.sys.Clock;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Clock.Ticket;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

public class DeviceHandler implements ServiceHandler, BacnetUnconfirmedServiceChoice, BacnetConfirmedServiceChoice {
   private final BBacnetServerLayer server;
   private final List<IAmListener> iAmListeners = new ArrayList<>();
   private final List<IHaveListener> iHaveListeners = new ArrayList<>();
   private Ticket ticket = null;
   BBacnetAddress backupRestoreClient = null;
   final AtomicReference<BIFile> backupRestoreFile = new AtomicReference<>();
   final AtomicReference<BBacnetFileDescriptor> backupRestoreFileDesc = new AtomicReference<>();
   final Object backupRestoreStepLock = new Object();
   DeviceHandler.BackupRestoreStep backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
   private static Boolean supportsWarmRestart = null;
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private static final String BACKUP_FILENAME = "backup_" + Sys.getStation().getStationName() + ".dist";
   private static final String BACKUP_PATH = "~backups/";
   private static final String BACKUP_FILEPATH = "~backups/" + BACKUP_FILENAME;
   private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

   DeviceHandler(BBacnetServerLayer server) {
      this.server = server;
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 0:
            this.processIAmRequest((IAmRequest)request, sourceAddress);
            return null;
         case 1:
            this.processIHaveRequest((IHaveRequest)request, sourceAddress);
            return null;
         case 7:
            this.processWhoHasRequest((WhoHasRequest)request, sourceAddress);
            return null;
         case 8:
            this.processWhoIsRequest((WhoIsRequest)request, sourceAddress);
            return null;
         case 17:
            return this.processDeviceCommunicationControlRequest((DeviceCommunicationControlRequest)request, sourceAddress);
         case 20:
            return this.processReinitializeDeviceRequest((ReinitializeDeviceRequest)request, sourceAddress);
         default:
            logger.info("DeviceHandler.receiveRequest: Unknown request! " + request);
            return null;
      }
   }

   private void processWhoIsRequest(WhoIsRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: WhoIsRequest received: " + request);
      }

      if (!request.useLimits()) {
         this.server.iAm(sourceAddress);
      } else {
         int myDeviceNumber = BBacnetNetwork.localDevice().getObjectId().getInstanceNumber();
         if (myDeviceNumber >= request.getDeviceInstanceRangeLowLimit() && myDeviceNumber <= request.getDeviceInstanceRangeHighLimit()) {
            this.server.iAm(sourceAddress);
         }
      }
   }

   private void processIAmRequest(IAmRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: IAmRequest received: " + request);
      }

      DeviceRegistry.update(request.getObjectId(), sourceAddress, request.getMaxAPDULengthAccepted(), request.getSegmentationSupported());
      this.routeToIAmListeners(request, sourceAddress);
   }

   private void processWhoHasRequest(WhoHasRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: WhoHasRequest received: " + request);
      }

      BIBacnetExportObject object = null;
      if (!request.useLimits()) {
         if (request.getObjectId() != null) {
            object = BBacnetNetwork.localDevice().lookupBacnetObject(request.getObjectId());
         } else {
            object = BBacnetNetwork.localDevice().lookupBacnetObject(request.getObjectName());
         }
      } else {
         int myDeviceNumber = BBacnetNetwork.localDevice().getObjectId().getInstanceNumber();
         if (myDeviceNumber >= request.getDeviceInstanceRangeLowLimit() && myDeviceNumber <= request.getDeviceInstanceRangeHighLimit()) {
            if (request.getObjectId() != null) {
               object = BBacnetNetwork.localDevice().lookupBacnetObject(request.getObjectId());
            } else {
               object = BBacnetNetwork.localDevice().lookupBacnetObject(request.getObjectName());
            }
         }
      }

      if (object != null) {
         this.server.iHave(object.getObjectId(), object.getObjectName(), request.getEncoding(), sourceAddress);
      }
   }

   private void processIHaveRequest(IHaveRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: IHaveRequest received: " + request);
      }

      this.routeToIHaveListeners(request, sourceAddress);
   }

   private BacnetServicePrimitive processDeviceCommunicationControlRequest(DeviceCommunicationControlRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: DeviceCommunicationControlRequest received: " + request);
      }

      String requestPassword = request.getPassword();
      NErrorType err = checkUserPassword(requestPassword);
      if (err != null) {
         return new SimpleError(17, err);
      } else {
         boolean enable = false;
         switch (request.getEnableDisable().getOrdinal()) {
            case 0:
               this.server.stack().enableComm();
               enable = true;
               break;
            case 1:
               logger.info(" Disabling Bacnet Communications per DCC request from address " + sourceAddress);
               this.server.stack().disableComm();
               break;
            case 2:
               logger.info(" Disabling Bacnet Communications Initiation per DCC request from address " + sourceAddress);
               this.server.stack().disableInitiation();
               break;
            default:
               return new BacnetReject(6);
         }

         if (this.ticket != null) {
            this.ticket.cancel();
         }

         if (!enable && request.isDurationUsed() && !request.isIndefinite()) {
            this.ticket = Clock.schedule(this.server.stack(), request.getDuration(), BBacnetComm.enableComm, null);
         }

         return new BacnetSimpleAck(17);
      }
   }

   private BacnetServicePrimitive processReinitializeDeviceRequest(ReinitializeDeviceRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("DeviceHandler: ReinitializeDeviceRequest received: " + request);
      }

      if (!BBacnetNetwork.bacnet().hasServerLicense()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("ReinitializeDeviceRequest rejected: station does not contain the bacnet.export license attribute or it is set to false");
         }

         return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.optionalFunctionalityNotSupported);
      } else {
         String requestPassword = request.getPassword();
         NErrorType err = checkUserPassword(requestPassword);
         if (err != null) {
            return new SimpleError(20, err);
         } else if (!this.server.getReinitializeAllowed()) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine("ReinitializeDeviceRequest rejected: reinitialization is not allowed");
            }

            return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.serviceRequestDenied);
         } else {
            BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
            switch (request.getReinitializedStateOfDevice().getOrdinal()) {
               case 0:
                  return processRestart(0);
               case 1:
                  if (supportsWarmRestart()) {
                     return processRestart(1);
                  }

                  return processRestart(0);
               case 2:
                  if (!this.isCommExecutionEnabled()) {
                     return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.communicationDisabled);
                  } else {
                     synchronized (this.backupRestoreStepLock) {
                        if (this.backupRestoreStep == DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE) {
                           logger.info("BACnet device at address " + sourceAddress + " has initiated a START_BACKUP...");
                           localDevice.setBackupAndRestoreState(BBacnetBackupState.preparingForBackup);
                           localDevice.updateSystemStatus(BBacnetDeviceStatus.backupInProgress);
                           this.backupRestoreClient = sourceAddress;
                           BBacnetNetwork.bacnet().postAsync(new DeviceHandler.StartBackup());
                           this.backupRestoreStep = DeviceHandler.BackupRestoreStep.RUNNING_BACKUP;
                           return new BacnetSimpleAck(20);
                        }

                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(
                              "Cannot start backup on receipt of START_BACKUP from device at address "
                                 + sourceAddress
                                 + "; systemStatus: "
                                 + localDevice.getSystemStatus()
                                 + "; backupAndRestoreState: "
                                 + localDevice.getBackupAndRestoreState()
                                 + "; backupRestoreStep: "
                                 + this.backupRestoreStep
                           );
                        }

                        return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                     }
                  }
               case 3:
                  if (!this.isCommExecutionEnabled()) {
                     return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.communicationDisabled);
                  } else {
                     logger.info("BACnet device at address " + sourceAddress + " has initiated an END_BACKUP...");
                     synchronized (this.backupRestoreStepLock) {
                        switch (this.backupRestoreStep) {
                           case RUNNING_BACKUP:
                              logger.fine("END_BACKUP received while preparing for backup");
                              this.backupRestoreStep = DeviceHandler.BackupRestoreStep.ABORTING_BACKUP;
                              return new BacnetSimpleAck(20);
                           case ABORTING_BACKUP:
                              logger.fine("END_BACKUP received while already aborting a backup");
                              return new BacnetSimpleAck(20);
                           case FINISHED_BACKUP:
                           case BACKUP_FAILED:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("END_BACKUP received in backup step " + this.backupRestoreStep + "; clearing backup");
                              }

                              this.clearBackupRestore();
                              this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
                              return new BacnetSimpleAck(20);
                           case PREPARING_FOR_RESTORE:
                           case ABORTING_RESTORE:
                           case READY_FOR_RESTORE:
                           case RESTORE_FAILED:
                           case RUNNING_RESTORE:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("END_BACKUP received while in restore step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case BACKUP_RESTORE_IDLE:
                           default:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("END_BACKUP received in invalid backup/restore step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.other);
                        }
                     }
                  }
               case 4:
                  if (!this.isCommExecutionEnabled()) {
                     return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.communicationDisabled);
                  } else {
                     synchronized (this.backupRestoreStepLock) {
                        if (this.backupRestoreStep == DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE) {
                           logger.info("BACnet device at address " + sourceAddress + " has initiated a START_RESTORE...");
                           localDevice.setBackupAndRestoreState(BBacnetBackupState.preparingForRestore);
                           localDevice.updateSystemStatus(BBacnetDeviceStatus.downloadInProgress);
                           this.backupRestoreClient = sourceAddress;
                           BBacnetNetwork.bacnet().postAsync(new DeviceHandler.StartRestore());
                           this.backupRestoreStep = DeviceHandler.BackupRestoreStep.PREPARING_FOR_RESTORE;
                           return new BacnetSimpleAck(20);
                        }

                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(
                              "Cannot start restore on receipt of START_RESTORE from device at address "
                                 + sourceAddress
                                 + "; systemStatus: "
                                 + localDevice.getSystemStatus()
                                 + "; backupAndRestoreState: "
                                 + localDevice.getBackupAndRestoreState()
                                 + "; backupRestoreStep: "
                                 + this.backupRestoreStep
                           );
                        }

                        return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                     }
                  }
               case 5:
                  if (!this.isCommExecutionEnabled()) {
                     return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.communicationDisabled);
                  } else {
                     logger.info("BACnet device at address " + sourceAddress + " has initiated an END_RESTORE...");
                     synchronized (this.backupRestoreStepLock) {
                        switch (this.backupRestoreStep) {
                           case RUNNING_BACKUP:
                           case ABORTING_BACKUP:
                           case FINISHED_BACKUP:
                           case BACKUP_FAILED:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("END_RESTORE received while in backup step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case PREPARING_FOR_RESTORE:
                              logger.info("END_RESTORE received while preparing for restore");
                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case ABORTING_RESTORE:
                              logger.fine("END_RESTORE received while aborting restore");
                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case READY_FOR_RESTORE:
                              logger.fine("END_RESTORE received while performing a restore; starting end restore task");
                              BBacnetNetwork.bacnet().postAsync(new DeviceHandler.EndRestore());
                              this.backupRestoreStep = DeviceHandler.BackupRestoreStep.RUNNING_RESTORE;
                              return new BacnetSimpleAck(20);
                           case RESTORE_FAILED:
                              logger.fine("END_RESTORE received after restore failure; restore should be aborted");
                              return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.other);
                           case RUNNING_RESTORE:
                              logger.fine("END_RESTORE received while already running a restore");
                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case BACKUP_RESTORE_IDLE:
                           default:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("END_RESTORE received in invalid backup/restore step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.other);
                        }
                     }
                  }
               case 6:
                  if (!this.isCommExecutionEnabled()) {
                     return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.communicationDisabled);
                  } else {
                     logger.info("BACnet device at address " + sourceAddress + " has initiated an ABORT_RESTORE...");
                     synchronized (this.backupRestoreStepLock) {
                        switch (this.backupRestoreStep) {
                           case RUNNING_BACKUP:
                           case ABORTING_BACKUP:
                           case FINISHED_BACKUP:
                           case BACKUP_FAILED:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("ABORT_RESTORE received while in backup step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case PREPARING_FOR_RESTORE:
                              logger.info("ABORT_RESTORE received while preparing for restore");
                              this.backupRestoreStep = DeviceHandler.BackupRestoreStep.ABORTING_RESTORE;
                              return new BacnetSimpleAck(20);
                           case ABORTING_RESTORE:
                              logger.info("ABORT_RESTORE received while already aborting the restore");
                              return new BacnetSimpleAck(20);
                           case READY_FOR_RESTORE:
                           case RESTORE_FAILED:
                              logger.fine("ABORT_RESTORE received after preparing for a restore but before ending it");
                              this.clearBackupRestore();
                              this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
                              return new BacnetSimpleAck(20);
                           case RUNNING_RESTORE:
                              logger.fine("ABORT_RESTORE received while ending a restore");
                              return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.configurationInProgress);
                           case BACKUP_RESTORE_IDLE:
                           default:
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine("ABORT_RESTORE received in invalid backup/restore step " + this.backupRestoreStep);
                              }

                              return makeReinitializeDeviceError(BBacnetErrorClass.services, BBacnetErrorCode.other);
                        }
                     }
                  }
               default:
                  return new BacnetReject(6);
            }
         }
      }
   }

   private static SimpleError makeReinitializeDeviceError(BBacnetErrorClass errorClass, BBacnetErrorCode errorCode) {
      return new SimpleError(20, new NErrorType(errorClass.getOrdinal(), errorCode.getOrdinal()));
   }

   private boolean isCommExecutionEnabled() {
      return this.server.stack().isCommExecutionEnabled();
   }

   private static BBacnetFileDescriptor addBackupRestoreFileDesc(FilePath path) {
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      BBacnetExportTable exportTable = (BBacnetExportTable)localDevice.getExportTable();
      int instNum = exportTable.getNextInstance(10);
      BBacnetObjectIdentifier objectId = BBacnetObjectIdentifier.make(10, instNum);
      BBacnetFileDescriptor fileDesc = new BBacnetFileDescriptor();
      fileDesc.setObjectId(objectId);
      fileDesc.setFileOrd(BOrd.make(path));
      fileDesc.setObjectName(BACKUP_FILENAME);
      fileDesc.setDescription("Station backup file");
      fileDesc.setBackupConigFile(true);
      exportTable.add(fileDesc.getObjectId().toString(BacnetConst.nameContext), fileDesc);
      return fileDesc;
   }

   private void clearBackupRestore() {
      BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
      localDevice.setBackupAndRestoreState(BBacnetBackupState.idle);
      localDevice.restoreSystemStatus();
      this.server.checkBackupTicket.cancel();
      this.server.cleanupBackupMode();
   }

   private void markRestoreFailure() {
      synchronized (this.backupRestoreStepLock) {
         BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
         if (this.backupRestoreStep == DeviceHandler.BackupRestoreStep.ABORTING_RESTORE) {
            logger.fine("Restore aborted during preparingForRestore after restore failure");
            this.server.cleanupBackupMode();
            localDevice.setBackupAndRestoreState(BBacnetBackupState.idle);
            localDevice.restoreSystemStatus();
            this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
         } else {
            this.server.cleanupBackupMode();
            this.server.scheduleBackupRestoreFailure();
            localDevice.setBackupAndRestoreState(BBacnetBackupState.restoreFailure);
            this.backupRestoreStep = DeviceHandler.BackupRestoreStep.RESTORE_FAILED;
         }
      }
   }

   private static BacnetServicePrimitive processRestart(int restartType) {
      try {
         BSystemPlatformService sps = (BSystemPlatformService)Sys.getService(BSystemPlatformService.TYPE);
         Runnable restart = null;
         BBacnetRestartReason reason = null;
         if (restartType == 1) {
            restart = new DeviceHandler.WarmStart(sps);
            reason = BBacnetRestartReason.warmstart;
         } else if (restartType == 0) {
            restart = new DeviceHandler.ColdStart(sps);
            reason = BBacnetRestartReason.coldstart;
         }

         if (reason != null) {
            BBacnetNetwork.bacnet().getLocalDevice().setLastRestartReason(reason);
         }

         BBacnetNetwork.bacnet().postAsync(restart);
         return new BacnetSimpleAck(20);
      } catch (ServiceNotFoundException var4) {
         logger.log(Level.SEVERE, "Cannot find System Platform Service!!", (Throwable)var4);
         return makeReinitializeDeviceError(BBacnetErrorClass.device, BBacnetErrorCode.operationalProblem);
      }
   }

   private static boolean supportsWarmRestart() {
      if (supportsWarmRestart == null) {
         BSystemPlatformService platService = (BSystemPlatformService)Sys.getService(BSystemPlatformService.TYPE);
         if (platService.getAllowStationRestart()) {
            supportsWarmRestart = Boolean.TRUE;
         } else {
            supportsWarmRestart = Boolean.FALSE;
         }
      }

      return supportsWarmRestart;
   }

   private static NErrorType checkUserPassword(String pw) {
      BUserService us;
      try {
         us = (BUserService)Sys.getService(BUserService.TYPE);
      } catch (ServiceNotFoundException var3) {
         logger.severe("Unable to locate User Service; password cannot be verified!");
         return new NErrorType(5, 0);
      }

      BUser bacnetUser = us.getUser("BACnet");
      if (bacnetUser == null) {
         logger.severe("BACnet User required for validation of Device Management request!");
         return new NErrorType(5, 29);
      } else if (pw == null) {
         return new NErrorType(4, 26);
      } else if (((BPasswordCache)bacnetUser.getAuthenticator()).validate(pw)) {
         logger.info("Password validated for BACnet User");
         return null;
      } else {
         logger.severe("Incorrect Password for BACnet Device Management Request!");
         return new NErrorType(4, 26);
      }
   }

   void addIAmListener(IAmListener listener) {
      this.iAmListeners.add(listener);
   }

   void removeIAmListener(IAmListener listener) {
      this.iAmListeners.remove(listener);
   }

   private void routeToIAmListeners(IAmRequest request, BBacnetAddress sourceAddress) {
      for (IAmListener listener : this.iAmListeners) {
         if (listener != null && request != null && sourceAddress != null) {
            listener.receiveIAm(request, sourceAddress);
         }
      }
   }

   void addIHaveListener(IHaveListener listener) {
      this.iHaveListeners.add(listener);
   }

   void removeIHaveListener(IHaveListener listener) {
      this.iHaveListeners.remove(listener);
   }

   private void routeToIHaveListeners(IHaveRequest request, BBacnetAddress sourceAddress) {
      for (IHaveListener iHaveListener : this.iHaveListeners) {
         iHaveListener.receiveIHave(request, sourceAddress);
      }
   }

   public static enum BackupRestoreStep {
      BACKUP_RESTORE_IDLE,
      RUNNING_BACKUP,
      ABORTING_BACKUP,
      FINISHED_BACKUP,
      BACKUP_FAILED,
      PREPARING_FOR_RESTORE,
      READY_FOR_RESTORE,
      RESTORE_FAILED,
      RUNNING_RESTORE,
      ABORTING_RESTORE;
   }

   private static class ColdStart implements Runnable {
      BSystemPlatformService sps = null;

      ColdStart(BSystemPlatformService sps) {
         this.sps = sps;
      }

      @Override
      public void run() {
         if (this.sps != null) {
            DeviceHandler.logger.warning("Rebooting station from BACnet ReinitializeDevice Request...");
            this.sps.reboot();
         } else {
            DeviceHandler.logger.info("SystemPlatformService not found!  Cannot execute BACnet Cold Start.");
         }
      }
   }

   private class EndRestore implements Runnable {
      private EndRestore() {
      }

      @Override
      public void run() {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               BBackupService backupService = (BBackupService)Sys.getService(BBackupService.TYPE);
               BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
               int restoreDatabaseRev = localDevice.getDatabaseRevision();
               FilePath path = new FilePath("~backups/lastRestoreTime");
               BIFile lastRestoreFile = BFileSystem.INSTANCE.makeFile(path, null);

               try (PrintStream ps = new PrintStream(lastRestoreFile.getOutputStream(), false, "UTF-8")) {
                  ps.print(Clock.time().encodeToString() + '\n');
                  ps.print(restoreDatabaseRev + "\n");
               }

               backupService.restoreFiles(DeviceHandler.this.backupRestoreFile.get(), null);
               return null;
            }));
         } catch (Exception var3) {
            Exception unwrapped = var3;
            if (var3 instanceof PrivilegedActionException) {
               unwrapped = ((PrivilegedActionException)var3).getException();
            }

            DeviceHandler.logger.log(Level.SEVERE, "Unable to complete restore procedure:" + unwrapped, (Throwable)unwrapped);
            DeviceHandler.this.markRestoreFailure();
         }
      }
   }

   private class StartBackup implements Runnable {
      private StartBackup() {
      }

      @Override
      public void run() {
         try {
            long t0 = Clock.ticks();
            Station.saveSync();
            long t1 = Clock.ticks();
            if (this.isBackupAborted()) {
               DeviceHandler.logger.fine("Backup ended during preparingForBackup after station save");
               return;
            }

            FilePath path = new FilePath(DeviceHandler.BACKUP_FILEPATH);
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               BBackupService backupService = (BBackupService)Sys.getService(BBackupService.TYPE);
               BIFile file = BFileSystem.INSTANCE.makeFile(path, null);
               DeviceHandler.this.backupRestoreFile.set(file);
               backupService.zip(null, file.getOutputStream(), true, null);
               return null;
            }));
            long t2 = Clock.ticks();
            if (this.isBackupAborted()) {
               DeviceHandler.logger.fine("Backup ended during preparingForBackup after station backup");
               return;
            }

            BBacnetFileDescriptor fileDesc = DeviceHandler.addBackupRestoreFileDesc(path);
            DeviceHandler.this.backupRestoreFileDesc.set(fileDesc);
            if (fileDesc.getStatus().isFault()) {
               DeviceHandler.logger.severe("Failed to export the backup file descriptor; faultCause: " + fileDesc.getFaultCause());
               this.markBackupFailure();
               return;
            }

            BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
            localDevice.getConfigurationFiles().addElement(fileDesc.getObjectId());
            long t3 = Clock.ticks();
            if (DeviceHandler.logger.isLoggable(Level.FINE)) {
               DeviceHandler.logger.fine("timing: tSave=" + (t1 - t0) + " tBackup=" + (t2 - t1) + " tExport=" + (t3 - t2) + " total=" + (t3 - t0));
            }

            synchronized (DeviceHandler.this.backupRestoreStepLock) {
               if (this.isBackupAborted()) {
                  DeviceHandler.logger.fine("Backup ended during preparingForBackup after exportFile descriptor created");
               } else {
                  DeviceHandler.this.server.scheduleBackupRestoreFailure();
                  localDevice.setBackupAndRestoreState(BBacnetBackupState.performingABackup);
                  DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.FINISHED_BACKUP;
               }
            }
         } catch (Exception var15) {
            Exception unwrapped = var15;
            if (var15 instanceof PrivilegedActionException) {
               unwrapped = ((PrivilegedActionException)var15).getException();
            }

            DeviceHandler.logger.log(Level.SEVERE, "Exception occurred in StartBackup runnable", (Throwable)unwrapped);
            this.markBackupFailure();
         }
      }

      private boolean isBackupAborted() {
         synchronized (DeviceHandler.this.backupRestoreStepLock) {
            if (DeviceHandler.this.backupRestoreStep == DeviceHandler.BackupRestoreStep.ABORTING_BACKUP) {
               DeviceHandler.this.clearBackupRestore();
               DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
               return true;
            } else {
               return false;
            }
         }
      }

      private void markBackupFailure() {
         synchronized (DeviceHandler.this.backupRestoreStepLock) {
            BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
            if (DeviceHandler.this.backupRestoreStep == DeviceHandler.BackupRestoreStep.ABORTING_BACKUP) {
               DeviceHandler.logger.fine("Backup ended during preparingForBackup after backup failure");
               DeviceHandler.this.server.cleanupBackupMode();
               localDevice.setBackupAndRestoreState(BBacnetBackupState.idle);
               localDevice.restoreSystemStatus();
               DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
            } else {
               DeviceHandler.this.server.cleanupBackupMode();
               DeviceHandler.this.server.scheduleBackupRestoreFailure();
               localDevice.setBackupAndRestoreState(BBacnetBackupState.backupFailure);
               DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_FAILED;
            }
         }
      }
   }

   private class StartRestore implements Runnable {
      private StartRestore() {
      }

      @Override
      public void run() {
         try {
            FilePath path = new FilePath(DeviceHandler.BACKUP_FILEPATH);
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               BIFile file = BFileSystem.INSTANCE.makeFile(path, null);
               DeviceHandler.this.backupRestoreFile.set(file);
               file.write(DeviceHandler.EMPTY_BYTE_ARRAY);
               return null;
            }));
            BBacnetFileDescriptor fileDesc = DeviceHandler.addBackupRestoreFileDesc(path);
            DeviceHandler.this.backupRestoreFileDesc.set(fileDesc);
            if (fileDesc.getStatus().isFault()) {
               DeviceHandler.logger.severe("Failed to export the restore file descriptor; faultCause: " + fileDesc.getFaultCause());
               DeviceHandler.this.markRestoreFailure();
               return;
            }

            BLocalBacnetDevice localDevice = BBacnetNetwork.localDevice();
            localDevice.getConfigurationFiles().addElement(fileDesc.getObjectId());
            synchronized (DeviceHandler.this.backupRestoreStepLock) {
               if (DeviceHandler.this.backupRestoreStep == DeviceHandler.BackupRestoreStep.ABORTING_RESTORE) {
                  DeviceHandler.logger.fine("Restore aborted during preparingForRestore");
                  DeviceHandler.this.clearBackupRestore();
                  DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.BACKUP_RESTORE_IDLE;
               } else {
                  DeviceHandler.this.server.scheduleBackupRestoreFailure();
                  localDevice.setBackupAndRestoreState(BBacnetBackupState.performingARestore);
                  DeviceHandler.this.backupRestoreStep = DeviceHandler.BackupRestoreStep.READY_FOR_RESTORE;
               }
            }
         } catch (Exception var7) {
            Exception unwrapped = var7;
            if (var7 instanceof PrivilegedActionException) {
               unwrapped = ((PrivilegedActionException)var7).getException();
            }

            DeviceHandler.logger.log(Level.SEVERE, "Exception occurred in StartRestore runnable", (Throwable)unwrapped);
            DeviceHandler.this.markRestoreFailure();
         }
      }
   }

   private static class WarmStart implements Runnable {
      BSystemPlatformService sps = null;

      WarmStart(BSystemPlatformService sps) {
         this.sps = sps;
      }

      @Override
      public void run() {
         if (this.sps != null) {
            DeviceHandler.logger.warning("Restarting station from BACnet ReinitializeDevice Request...");
            this.sps.restartStation();
         } else {
            DeviceHandler.logger.info("SystemPlatformService not found!  Cannot execute BACnet Warm Start.");
         }
      }
   }
}
