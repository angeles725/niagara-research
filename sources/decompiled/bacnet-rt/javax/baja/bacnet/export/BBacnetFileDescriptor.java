package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetFileAccessMethod;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.file.BIFile;
import javax.baja.file.BLocalFileStore;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.space.BISpaceNode;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"baja:AbstractFile"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "fileOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 64,
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"baja:IFile\""
      )}
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.FILE)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "fileType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "archiveTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT"
   ), @NiagaraProperty(
      name = "fileAccessMethod",
      type = "BBacnetFileAccessMethod",
      defaultValue = "BBacnetFileAccessMethod.streamAccess",
      flags = 1
   )})
public class BBacnetFileDescriptor extends BComponent implements BIBacnetExportObject, BacnetPropertyListProvider {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property fileOrd = newProperty(64, BOrd.NULL, BFacets.make("targetType", "baja:IFile"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(10), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property fileType = newProperty(0, "", null);
   public static final Property archiveTime = newProperty(0, BAbsTime.DEFAULT, null);
   public static final Property fileAccessMethod = newProperty(1, BBacnetFileAccessMethod.streamAccess, null);
   public static final Type TYPE = Sys.loadType(BBacnetFileDescriptor.class);
   private boolean fatalFault = false;
   private static final BIcon icon = BIcon.make(BIcon.std("file.png"), BIcon.std("badges/export.png"));
   private BIFile file;
   private boolean eof = true;
   private Subscriber subscriber;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private boolean backupConigFile = false;
   public static final int FILE_WRITE_OK = -1;
   public static final int APPEND_START_POSITION = -1;
   static Logger log = Logger.getLogger("bacnet.server");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 43, 42, 71, 13, 99, 41};
   private static final int[] OPTIONAL_PROPS = new int[]{28};
   private static final Logger logger = Logger.getLogger("bacnet.export");

   @Override
   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BOrd getFileOrd() {
      return (BOrd)this.get(fileOrd);
   }

   public void setFileOrd(BOrd v) {
      this.set(fileOrd, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public String getObjectName() {
      return this.getString(objectName);
   }

   @Override
   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public String getFileType() {
      return this.getString(fileType);
   }

   public void setFileType(String v) {
      this.setString(fileType, v, null);
   }

   public BAbsTime getArchiveTime() {
      return (BAbsTime)this.get(archiveTime);
   }

   public void setArchiveTime(BAbsTime v) {
      this.set(archiveTime, v, null);
   }

   public BBacnetFileAccessMethod getFileAccessMethod() {
      return (BBacnetFileAccessMethod)this.get(fileAccessMethod);
   }

   public void setFileAccessMethod(BBacnetFileAccessMethod v) {
      this.set(fileAccessMethod, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void started() throws Exception {
      super.started();
      this.checkFatalFault();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.subscriber = new BBacnetFileDescriptor.FileSubscriber(this);
      this.checkConfiguration();
      if (!this.isBackupConigFile() && Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      this.subscriber.unsubscribeAll();
      this.file = null;
      this.subscriber = null;
      this.oldId = null;
      this.oldName = null;
      if (!this.isBackupConigFile() && local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var4) {
            }

            if (!this.isBackupConigFile() && this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (!this.isBackupConigFile() && this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(fileOrd)) {
            this.checkConfiguration();
            if (!this.isBackupConigFile() && this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         }
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(10) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return (BObject)this.getFile();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getFileOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(fileOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         log.warning(this + ": is in fatal fault");
      } else {
         this.subscriber.unsubscribeAll();
         this.getFile();
         boolean configOk = true;
         if (this.file == null) {
            this.setFaultCause("Cannot find exported file");
            configOk = false;
            log.warning(this + ": cannot find exported file");
         } else {
            this.subscriber.subscribe((BComponent)this.getParent());
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
            log.warning(this + ": invalid Object Id");
         }

         if (configOk) {
            String err = BBacnetNetwork.localDevice().export(this);
            if (err != null) {
               this.duplicate = true;
               this.setFaultCause(err);
               configOk = false;
               log.warning(this + ": found duplicate id or name");
            } else {
               this.duplicate = false;
            }
         }

         if (configOk) {
            this.setFaultCause("");
         }

         this.setStatus(BStatus.makeFault(this.getStatus(), !configOk));
         log.info(this + ": configurationOk state is " + configOk);
      }
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      this.getFile();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getFile();
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }

               props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 80:
               int[] props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 105:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            default:
               results.add(this.readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
         }
      }

      return results.toArray(new PropertyValue[0]);
   }

   @Override
   public RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      return !hasProperty(propertyId)
         ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   private static boolean hasProperty(int propertyId) {
      for (int id : REQUIRED_PROPS) {
         if (id == propertyId) {
            return true;
         }
      }

      for (int idx : OPTIONAL_PROPS) {
         if (idx == propertyId) {
            return true;
         }
      }

      return propertyId == 371;
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      this.getFile();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (this.file == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (pId == 371) {
         return this.readPropertyList(ndx);
      } else if (ndx >= 0) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 13:
               boolean archive = this.getArchiveTime().isAfter(this.file.getLastModified());
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(archive));
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 41:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getFileAccessMethod()));
            case 42:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.file.getSize()));
            case 43:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.file.getMimeType()));
            case 71:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toBacnetDateTime(this.file.getLastModified()));
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 99:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.file.isReadonly()));
            default:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (this.file == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && pId != 371) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 13:
                  boolean archive = AsnUtil.fromAsnBoolean(val);
                  if (archive) {
                     this.set(archiveTime, BAbsTime.make(), BLocalBacnetDevice.getBacnetContext());
                  } else {
                     this.set(archiveTime, BAbsTime.DEFAULT, BLocalBacnetDevice.getBacnetContext());
                  }

                  return null;
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 41:
                  return new NErrorType(2, 40);
               case 42:
                  if (this.file.isReadonly()) {
                     return new NErrorType(2, 40);
                  } else {
                     try {
                        if (AsnUtil.fromAsnUnsignedInt(val) == 0) {
                           this.file.write(new byte[0]);
                           return null;
                        }

                        return new NErrorType(2, 37);
                     } catch (IOException var6) {
                        return new NErrorType(0, 25);
                     } catch (IllegalArgumentException var7) {
                        return new NErrorType(2, 37);
                     }
                  }
               case 43:
                  return new NErrorType(2, 40);
               case 71:
                  return new NErrorType(2, 40);
               case 75:
               case 79:
               case 371:
                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               case 99:
                  return new NErrorType(2, 40);
               default:
                  return new NErrorType(2, 32);
            }
         } catch (AsnException var8) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
            return new NErrorType(2, 9);
         } catch (PermissionException var9) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var9);
            return new NErrorType(2, 40);
         }
      }
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   protected final BIFile getFile() {
      return this.file == null ? this.findFile() : this.file;
   }

   private BIFile findFile() {
      try {
         if (!fileOrd.isEquivalentToDefaultValue(this.getFileOrd())) {
            BObject o = this.getFileOrd().get(this);
            if (o instanceof BIFile) {
               this.file = (BIFile)o;
            } else {
               this.file = null;
            }
         }
      } catch (Exception var2) {
         log.warning("Unable to resolve file ord for " + this + ":" + this.getFileOrd() + ": " + var2);
         this.file = null;
      }

      if (this.file == null) {
         this.setFaultCause("Cannot find exported file");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      return this.file;
   }

   public final byte[] read(int fileStartPosition, int requestedOctetCount) throws IOException {
      return this.readFile(fileStartPosition, requestedOctetCount);
   }

   protected byte[] readFile(int fileStartPosition, int requestedOctetCount) throws IOException {
      if (this.file == null) {
         return null;
      } else {
         this.eof = false;
         InputStream in = this.file.getInputStream();

         byte[] var7;
         try {
            in.skip(fileStartPosition);
            int avail = in.available();
            byte[] b;
            if (avail <= requestedOctetCount) {
               this.eof = true;
               b = new byte[avail];
            } else {
               this.eof = false;
               b = new byte[requestedOctetCount];
            }

            int numRead = in.read(b);
            if (log.isLoggable(Level.FINE)) {
               log.fine(
                  "BacnetFile "
                     + this.file.getFileName()
                     + " {"
                     + this.getObjectId()
                     + "}.read(): "
                     + requestedOctetCount
                     + " bytes requested, "
                     + numRead
                     + " bytes read from file."
               );
            }

            if (numRead < 0) {
               this.eof = true;
            }

            var7 = b;
         } finally {
            in.close();
         }

         return var7;
      }
   }

   public final int write(int fileStartPosition, byte[] fileData) throws IOException {
      return this.writeFile(fileStartPosition, fileData);
   }

   protected int writeFile(int fileStartPosition, byte[] fileData) throws IOException {
      if (this.file == null) {
         return 5;
      } else if (this.file.isReadonly()) {
         return 5;
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine(
               "BacnetFile "
                  + this.file.getFileName()
                  + " {"
                  + this.getObjectId()
                  + "}.write() :"
                  + fileData.length
                  + " bytes starting at "
                  + fileStartPosition
            );
         }

         long len = this.file.getSize();
         File f = ((BLocalFileStore)this.file.getStore()).getLocalFile();
         RandomAccessFile out = new RandomAccessFile(f, "rw");

         try {
            if (fileStartPosition < len) {
               if (fileStartPosition == -1) {
                  out.seek(len);
               } else {
                  if (fileStartPosition < 0) {
                     return 11;
                  }

                  out.seek(fileStartPosition);
               }
            } else {
               int diff = (int)(fileStartPosition - len);
               out.seek(len);
               byte[] b = new byte[diff];

               for (int i = 0; i < diff; i++) {
                  b[i] = 0;
               }

               out.write(b);
            }

            out.write(fileData);
            log.fine("File write OK");
            return -1;
         } finally {
            out.close();
         }
      }
   }

   public long getFileSize() {
      return this.getFile().getSize();
   }

   public final boolean isEOF() {
      return this.eof;
   }

   @Override
   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      BBacnetExportTable exports = null;
      BLocalBacnetDevice local = null;
      BBacnetNetwork network = null;
      if (!this.fatalFault) {
         for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BBacnetExportTable) {
               exports = (BBacnetExportTable)parent;
            } else if (parent instanceof BLocalBacnetDevice) {
               local = (BLocalBacnetDevice)parent;
               break;
            }
         }

         if (exports == null || local == null) {
            this.fatalFault = true;
            this.setFaultCause("Not under LocalBacnetDevice Export Table");
         } else if (local.isFatalFault()) {
            this.fatalFault = true;
            this.setFaultCause("LocalDevice fault: " + local.getFaultCause());
         } else {
            network = (BBacnetNetwork)local.getParent();
            if (network == null) {
               this.fatalFault = true;
               this.setFaultCause("Not under BacnetNetwork");
            } else if (network.isFatalFault()) {
               this.fatalFault = true;
               this.setFaultCause("Network fault: " + network.getFaultCause());
            } else if (!network.hasServerLicense()) {
               this.fatalFault = true;
               this.setFaultCause("Server capability not licensed");
            } else {
               this.setFaultCause("");
            }
         }
      }
   }

   public boolean isBackupConigFile() {
      return this.backupConigFile;
   }

   public void setBackupConigFile(boolean backupConigFile) {
      this.backupConigFile = backupConigFile;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetFileDescriptor", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("file", this.file);
      out.prop("eof", this.eof);
      out.prop("subscriber", this.subscriber);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }

   static class FileSubscriber extends Subscriber {
      private final BBacnetFileDescriptor obj;

      public FileSubscriber(BBacnetFileDescriptor obj) {
         this.obj = obj;
      }

      public void event(BComponentEvent event) {
         try {
            if (event.getId() == 3 && event.getSlotName().equals(this.obj.getObjectName())) {
               this.obj.setObjectOrd(((BISpaceNode)this.obj.getObject()).getOrdInSpace(), null);
               this.obj.checkConfiguration();
            }
         } catch (Exception var3) {
            BBacnetFileDescriptor.logger.log(Level.WARNING, "obj=" + this.obj.getObjectId(), (Throwable)var3);
         }
      }
   }
}
