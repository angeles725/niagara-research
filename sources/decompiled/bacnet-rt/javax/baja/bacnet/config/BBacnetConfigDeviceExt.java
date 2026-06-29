package javax.baja.bacnet.config;

import com.tridium.bacnet.job.BBacnetDiscoverConfigJob;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.driver.BDeviceExt;
import javax.baja.driver.loadable.BDownloadParameters;
import javax.baja.driver.loadable.BILoadable;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.driver.loadable.LoadUtil;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperty(
   name = "deviceObject",
   type = "BBacnetDeviceObject",
   defaultValue = "new BBacnetDeviceObject()"
)
@NiagaraActions({@NiagaraAction(
      name = "upload",
      parameterType = "BUploadParameters",
      defaultValue = "new BUploadParameters()",
      flags = 16
   ), @NiagaraAction(
      name = "download",
      parameterType = "BDownloadParameters",
      defaultValue = "new BDownloadParameters()",
      flags = 16
   ), @NiagaraAction(
      name = "submitConfigDiscoveryJob",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "getConfigTypes",
      returnType = "BString",
      flags = 4
   )})
public class BBacnetConfigDeviceExt extends BDeviceExt implements BILoadable, BacnetConst, BIBacnetObjectContainer, BIBacnetConfigFolder {
   public static final Property deviceObject = newProperty(0, new BBacnetDeviceObject(), null);
   public static final Action upload = newAction(16, new BUploadParameters(), null);
   public static final Action download = newAction(16, new BDownloadParameters(), null);
   public static final Action submitConfigDiscoveryJob = newAction(4, null);
   public static final Action getConfigTypes = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetConfigDeviceExt.class);
   private static final BIcon icon = BIcon.make("module://bacnet/com/tridium/bacnet/ui/icons/bacObject.png");
   public static final Logger log = Logger.getLogger("bacnet.client");

   public BBacnetDeviceObject getDeviceObject() {
      return (BBacnetDeviceObject)this.get(deviceObject);
   }

   public void setDeviceObject(BBacnetDeviceObject v) {
      this.set(deviceObject, v, null);
   }

   public void upload(BUploadParameters parameter) {
      this.invoke(upload, parameter, null);
   }

   public void download(BDownloadParameters parameter) {
      this.invoke(download, parameter, null);
   }

   public BOrd submitConfigDiscoveryJob() {
      return (BOrd)this.invoke(submitConfigDiscoveryJob, null, null);
   }

   public BString getConfigTypes() {
      return (BString)this.invoke(getConfigTypes, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetConfigDeviceExt() {
   }

   public BBacnetConfigDeviceExt(BBacnetObjectIdentifier objectId) {
      this.getDeviceObject().setObjectId(objectId);
   }

   @Override
   public BBacnetConfigDeviceExt getConfig() {
      return this;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.getDeviceObject().getObjectId();
   }

   public String toString(Context context) {
      return this.getObjectId().toString(context) + " config";
   }

   public final BBacnetNetwork network() {
      return (BBacnetNetwork)this.getNetwork();
   }

   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   public synchronized BBacnetObject[] getObjectList() {
      BBacnetObject[] temp = new BBacnetObject[this.getSlotCount()];
      int count = 0;

      for (SlotCursor<Property> c = this.getProperties(); c.next(BBacnetObject.class); count++) {
         BObject kid = c.get();
         temp[count] = (BBacnetObject)kid;
      }

      BBacnetObject[] result = new BBacnetObject[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetDevice;
   }

   public final boolean isChildLegal(BComponent child) {
      return !(child instanceof BBacnetDeviceObject);
   }

   public BBacnetObject lookupBacnetObject(BBacnetObjectIdentifier objectId) {
      try {
         return (BBacnetObject)this.lookupBacnetObject(objectId, -1, -1, null);
      } catch (ClassCastException var3) {
         return null;
      }
   }

   @Override
   public BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetObject.class)) {
         if (((BBacnetObject)c.get()).getObjectId().equals(objectId)) {
            return c.get();
         }
      }

      return null;
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      if (action.equals(upload)) {
         return this.postUpload((BUploadParameters)arg, cx);
      } else {
         return action.equals(download) ? this.postDownload((BDownloadParameters)arg, cx) : super.post(action, arg, cx);
      }
   }

   protected IFuture postUpload(BUploadParameters params, Context cx) {
      return this.postAsync(new Invocation(this, upload, params, cx));
   }

   protected IFuture postDownload(BDownloadParameters params, Context cx) {
      return this.postAsync(new Invocation(this, download, params, cx));
   }

   public IFuture postAsync(Runnable r) {
      return this.network().postAsync(r);
   }

   public BOrd doSubmitConfigDiscoveryJob(Context cx) {
      return this.device().isFatalFault() ? null : new BBacnetDiscoverConfigJob(this).submit(cx);
   }

   public BString doGetConfigTypes() {
      TypeInfo[] types = Sys.getRegistry().getConcreteTypes(BBacnetObject.TYPE.getTypeInfo());
      List<String> infos = new ArrayList<>(types.length);

      for (TypeInfo type : types) {
         Class<?> typeClass = type.getTypeSpec().getResolvedType().getTypeClass();
         if (!typeClass.isAnnotationPresent(Deprecated.class)) {
            infos.add(type.toString());
         }
      }

      infos.sort(null);
      return BString.make(String.join(";", infos));
   }

   public void doUpload(BUploadParameters p, Context cx) throws Exception {
      if (p.getRecursive()) {
         LoadUtil.uploadChildren(this, p, cx);
      } else {
         this.getDeviceObject().doUpload(p, cx);
      }
   }

   public void doDownload(BDownloadParameters p, Context cx) throws Exception {
      if (p.getRecursive()) {
         LoadUtil.downloadChildren(this, p, cx);
      } else {
         this.getDeviceObject().doDownload(p, cx);
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
