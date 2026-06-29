package javax.baja.lonworks;

import com.tridium.lonworks.device.BDownloadJob;
import com.tridium.lonworks.device.BUploadJob;
import com.tridium.lonworks.device.NvDev;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 67
   ), @NiagaraProperty(
      name = "objectId",
      type = "int",
      defaultValue = "UNASSIGNED_ID",
      flags = 1
   ), @NiagaraProperty(
      name = "objectType",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
@NiagaraActions({@NiagaraAction(
      name = "upload",
      parameterType = "BUploadParameters",
      defaultValue = "new BUploadParameters()"
   ), @NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()"
   )})
public class BLonObject extends BComponent implements BINvContainer, BIStatus, BILonLoadable {
   public static final int UNASSIGNED_ID = -1;
   public static final Property status = newProperty(75, BStatus.ok, null);
   public static final Property faultCause = newProperty(67, "", null);
   public static final Property objectId = newProperty(1, -1, null);
   public static final Property objectType = newProperty(1, "", null);
   public static final Action upload = newAction(0, new BUploadParameters(), null);
   public static final Action download = newAction(0, new BDownloadParameters(), null);
   public static final Type TYPE = Sys.loadType(BLonObject.class);
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/object.png");
   private boolean objUploadInProgress = false;
   boolean linkUpdateDone = false;
   NvDev.SaveNv snv = null;
   BNetworkVariable saveNv = null;
   Property nvProp = null;

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

   public int getObjectId() {
      return this.getInt(objectId);
   }

   public void setObjectId(int v) {
      this.setInt(objectId, v, null);
   }

   public String getObjectType() {
      return this.getString(objectType);
   }

   public void setObjectType(String v) {
      this.setString(objectType, v, null);
   }

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BIcon getIcon() {
      return icon;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent.getType().is(BLonDevice.TYPE) || parent.getType().is(BLonObjectFolder.TYPE);
   }

   @Override
   public BLonNetwork getLonNetwork() {
      return this.getLonDevice().getLonNetwork();
   }

   @Override
   public BLonDevice getLonDevice() {
      return (BLonDevice)NmUtil.getParent(this, BLonDevice.TYPE);
   }

   @Override
   public BDeviceData getDeviceData() {
      return this.getLonDevice().getDeviceData();
   }

   @Override
   public BINetworkVariable[] getNetworkVariables() {
      return this.getLonDevice().getNetworkVariables();
   }

   @Override
   public boolean isLonObject() {
      return true;
   }

   public final void doUpload(BUploadParameters p, Context cx) throws Exception {
      this.checkState();
      this.getLonDevice().checkState();
      this.checkUpload();
      new BUploadJob(this, p, cx).submit(cx);
   }

   public void checkState() {
      if (this.getStatus().isDown()) {
         throw new BajaRuntimeException("Object is down. Can't perform operation.");
      } else if (this.getStatus().isDisabled()) {
         throw new BajaRuntimeException("Object is disabled. Can't perform operation.");
      } else if (this.getStatus().isFault()) {
         throw new BajaRuntimeException("Object is in fault. Can't perform operation.");
      }
   }

   public final void doDownload(BDownloadParameters p, Context cx) throws Exception {
      this.checkState();
      this.getLonDevice().checkState();
      this.checkDownload();
      new BDownloadJob(this, p, cx).submit(cx);
   }

   public void checkUpload() {
   }

   @Override
   public void beginUpload() {
      BLonDevice dev = this.getLonDevice();
      if (!dev.isUpLoadInProgress()) {
         dev.initUpload(true);
         this.objUploadInProgress = true;
      }
   }

   @Override
   public void endUpload() {
      BLonDevice dev = this.getLonDevice();
      if (this.objUploadInProgress) {
         dev.cleanupUpload();
         this.objUploadInProgress = false;
      }
   }

   public void checkDownload() {
   }

   @Override
   public void beginDownload() {
      BLonDevice dev = this.getLonDevice();
      if (!dev.isDownLoadInProgress()) {
         dev.initDownload(true);
      }
   }

   @Override
   public void endDownload() {
      BLonDevice dev = this.getLonDevice();
      if (!dev.isDownLoadInProgress()) {
         dev.cleanupDownload();
      }
   }

   public BLink makeLink(BComponent source, Slot sourceSlot, Slot targetSlot, Context cx) {
      return !NvDev.requiresLonLink(targetSlot)
         ? super.makeLink(source, sourceSlot, targetSlot, cx)
         : NvDev.makeLonLink(source, sourceSlot, this, targetSlot, cx);
   }

   protected LinkCheck doCheckLink(BComponent source, Slot sourceSlot, Slot targetSlot, Context cx) {
      return NvDev.doNvCheckLink(source, sourceSlot, this, targetSlot, cx);
   }

   @Override
   public void linkUpdate() {
      if (!this.linkUpdateDone) {
         this.getComponentSpace().update(this, 1);
         this.getComponentSpace().update(this.getLonDevice().getDeviceData(), 2);
         this.linkUpdateDone = true;
      }
   }

   public void renamed(Property property, String oldName, Context context) {
      super.renamed(property, oldName, context);
      if (property.getType().is(BLonComponent.TYPE)) {
         BLonDevice.lonComponentRenamed(this.getLonDevice(), property.getName(), oldName, context);
      }
   }

   public void knobAdded(Knob knob, Context context) {
      super.knobAdded(knob, context);
      NvDev.knobAdded(this, knob, context);
   }

   public void knobRemoved(Knob knob, Context context) {
      super.knobRemoved(knob, context);
      NvDev.knobRemoved(this, knob, context);
   }

   public void checkRemove(Property prop, Context context) {
      this.snv = NvDev.checkRemove(this, prop, context);
      super.checkRemove(prop, context);
   }

   public void removed(Property prop, BValue value, Context context) {
      super.removed(prop, value, context);
      NvDev.removed(this, this.snv, prop, value, context);
      this.snv = null;
   }

   public void added(Property prop, Context context) {
      super.added(prop, context);
      if (prop.getType().is(BLonLink.TYPE)) {
         BLonLink lnk = (BLonLink)this.get(prop);
         if (this.isRunning() && !lnk.getMessageTag()) {
            lnk.getDestinationNv().lonLinkAdded();
         }
      }
   }
}
