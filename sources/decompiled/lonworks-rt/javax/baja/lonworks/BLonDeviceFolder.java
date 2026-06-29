package javax.baja.lonworks;

import com.tridium.lonworks.BLonRouter;
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
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

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
public class BLonDeviceFolder extends BDeviceFolder {
   public static final Action upload = newAction(0, new BUploadParameters(), null);
   public static final Action download = newAction(0, new BDownloadParameters(), null);
   public static final Type TYPE = Sys.loadType(BLonDeviceFolder.class);

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
      return parent.getType().is(BLonNetwork.TYPE) || parent.getType().is(TYPE);
   }

   public boolean isChildLegal(BComponent child) {
      return !child.getType().is(BDeviceFolder.TYPE) || child.getType().is(TYPE);
   }

   public void checkAdd(String name, BValue value, int flags, BFacets facets, Context context) {
      if (value.getType().is(BLocalLonDevice.TYPE)) {
         throw new LocalizableRuntimeException("lonworks", "addLocalDeviceError");
      }
   }

   public void added(Property prop, Context context) {
      try {
         if (!this.isRunning()) {
            return;
         }

         if (prop.getType().is(BLonDevice.TYPE)) {
            this.getLonNetwork().netmgmt().deviceAdded((BLonDevice)this.get(prop));
         } else if (prop.getType().is(BLonRouter.TYPE)) {
            this.getLonNetwork().netmgmt().routerAdded((BLonRouter)this.get(prop));
         }
      } finally {
         super.added(prop, context);
      }
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

   public void doUpload(BUploadParameters params, Context cx) throws Exception {
      new BUploadJob(this, params, cx).submit(cx);
   }

   public void doDownload(BDownloadParameters params, Context cx) throws Exception {
      new BDownloadJob(this, params, cx).submit(cx);
   }
}
