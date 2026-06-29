package javax.baja.lonworks;

import com.tridium.lonworks.device.BDownloadJob;
import com.tridium.lonworks.device.BUploadJob;
import com.tridium.lonworks.device.NvDev;
import javax.baja.agent.AgentList;
import javax.baja.driver.BDeviceFolder;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "upload",
      parameterType = "BUploadParameters",
      defaultValue = "new BUploadParameters()"
   ), @NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()"
   )})
public class BLonObjectFolder extends BFolder implements BILonLoadable {
   public static final Action upload = newAction(0, new BUploadParameters(), null);
   public static final Action download = newAction(0, new BDownloadParameters(), null);
   public static final Type TYPE = Sys.loadType(BLonObjectFolder.class);
   private boolean objUploadInProgress = false;
   private boolean objDownloadInProgress = false;
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/objectFolder.png");

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent.getType().is(BLonDevice.TYPE) || parent.getType().is(TYPE);
   }

   public boolean isChildLegal(BComponent child) {
      return !child.getType().is(BDeviceFolder.TYPE);
   }

   public AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      return NvDev.fixWireSheet(list, cx);
   }

   public final BLonNetwork getLonNetwork() {
      for (BComplex p = this.getParent(); p != null; p = p.getParent()) {
         if (p.getType().is(BLonNetwork.TYPE)) {
            return (BLonNetwork)p;
         }
      }

      throw new IllegalStateException();
   }

   @Override
   public final BLonDevice getLonDevice() {
      for (BComplex p = this.getParent(); p != null; p = p.getParent()) {
         if (p.getType().is(BLonDevice.TYPE)) {
            return (BLonDevice)p;
         }
      }

      throw new IllegalStateException();
   }

   public final void doUpload(BUploadParameters p, Context cx) throws Exception {
      this.getLonDevice().checkState();
      new BUploadJob(this, p, cx).submit(cx);
   }

   public final void doDownload(BDownloadParameters p, Context cx) throws Exception {
      this.getLonDevice().checkState();
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
         this.objDownloadInProgress = true;
      }
   }

   @Override
   public void endDownload() {
      BLonDevice dev = this.getLonDevice();
      if (!dev.isDownLoadInProgress()) {
         dev.cleanupDownload();
         this.objDownloadInProgress = false;
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
