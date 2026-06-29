package javax.baja.bacnet.config;

import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BILoadable;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.driver.loadable.LoadUtil;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "upload",
      parameterType = "BUploadParameters",
      defaultValue = "new BUploadParameters()",
      flags = 4
   ), @NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()",
      flags = 4
   )})
public class BBacnetConfigFolder extends BFolder implements BIBacnetConfigFolder, BILoadable {
   public static final Action upload = newAction(4, new BUploadParameters(), null);
   public static final Action download = newAction(4, new BDownloadParameters(), null);
   public static final Type TYPE = Sys.loadType(BBacnetConfigFolder.class);
   private static final BIcon icon = BIcon.make(BIcon.std("folder.png"), BIcon.make("module://bacnet/com/tridium/bacnet/ui/icons/bacnetBadge.png"));

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public BBacnetConfigDeviceExt getConfig() {
      for (BComplex p = this.getParent(); p != null; p = p.getParent()) {
         if (p instanceof BBacnetConfigDeviceExt) {
            return (BBacnetConfigDeviceExt)p;
         }
      }

      throw new IllegalStateException();
   }

   public void doUpload(BUploadParameters params, Context cx) throws Exception {
      if (params.getRecursive()) {
         LoadUtil.uploadChildren(this, params, cx);
      }
   }

   public void doDownload(BDownloadParameters params, Context cx) throws Exception {
      if (params.getRecursive()) {
         LoadUtil.downloadChildren(this, params, cx);
      }
   }

   public String toString(Context cx) {
      return "";
   }

   public BIcon getIcon() {
      return icon;
   }
}
