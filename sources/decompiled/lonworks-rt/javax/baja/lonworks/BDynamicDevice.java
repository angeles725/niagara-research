package javax.baja.lonworks;

import com.tridium.lonworks.device.BLonLearnNvJob;
import com.tridium.lonworks.device.DynaDev;
import com.tridium.lonworks.netmgmt.BLonNetmgmt;
import com.tridium.lonworks.xml.LonXMLReader;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.lonworks.datatypes.BImportParameters;
import javax.baja.lonworks.datatypes.BLearnNvParameters;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "xmlFile",
   type = "BOrd",
   defaultValue = "BOrd.NULL",
   flags = 1
)
@NiagaraActions({@NiagaraAction(
      name = "learnNv",
      flags = 4
   ), @NiagaraAction(
      name = "learnNv_",
      parameterType = "BLearnNvParameters",
      defaultValue = "new BLearnNvParameters()",
      returnType = "BOrd"
   ), @NiagaraAction(
      name = "trim"
   ), @NiagaraAction(
      name = "importXml",
      parameterType = "BImportParameters",
      defaultValue = "new BImportParameters()",
      flags = 4
   )})
@NiagaraTopic(
   name = "dynamicOpComplete"
)
public class BDynamicDevice extends BLonDevice {
   public static final Property xmlFile = newProperty(1, BOrd.NULL, null);
   public static final Action learnNv = newAction(4, null);
   public static final Action learnNv_ = newAction(0, new BLearnNvParameters(), null);
   public static final Action trim = newAction(0, null);
   public static final Action importXml = newAction(4, new BImportParameters(), null);
   public static final Topic dynamicOpComplete = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BDynamicDevice.class);

   public BOrd getXmlFile() {
      return (BOrd)this.get(xmlFile);
   }

   public void setXmlFile(BOrd v) {
      this.set(xmlFile, v, null);
   }

   public void learnNv() {
      this.invoke(learnNv, null, null);
   }

   public BOrd learnNv_(BLearnNvParameters parameter) {
      return (BOrd)this.invoke(learnNv_, parameter, null);
   }

   public void trim() {
      this.invoke(trim, null, null);
   }

   public void importXml(BImportParameters parameter) {
      this.invoke(importXml, parameter, null);
   }

   public void fireDynamicOpComplete(BValue event) {
      this.fire(dynamicOpComplete, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      if (this.getXmlFile() != BOrd.NULL && this.getNetworkVariables().length == 0 && this.getNeuronIdAddress().isZero()) {
         this.doImportXml(new BImportParameters(false, this.getLonNetwork().netmgmt().getUseLonObjects()));
      }
   }

   public BValue getActionParameterDefault(Action action) {
      if (action == learnNv_) {
         BLonNetmgmt nm = this.getLonNetwork().netmgmt();
         this.getComponentSpace().update(nm, 1);
         return new BLearnNvParameters(nm.getUseLonObjects());
      } else {
         return super.getActionParameterDefault(action);
      }
   }

   public void doLearnNv() {
      if (this.getXmlFile() != BOrd.NULL) {
         throw new LocalizableRuntimeException("lonworks", "learnNv.block");
      } else {
         BLearnNvParameters param = new BLearnNvParameters(this.getLonNetwork().netmgmt().getUseLonObjects());
         new BLonLearnNvJob(this.getLonNetwork(), this, param).submit(null);
      }
   }

   public BOrd doLearnNv_(BLearnNvParameters param) {
      if (this.getXmlFile() != BOrd.NULL) {
         throw new LocalizableRuntimeException("lonworks", "learnNv.block");
      } else {
         return new BLonLearnNvJob(this.getLonNetwork(), this, param).submit(null);
      }
   }

   public void doImportXml(BImportParameters param) {
      if (!this.getXmlFile().isNull()) {
         XLonInterfaceFile root = LonXMLReader.decode(this.getXmlFile());
         DynaDev.importXLon(this, root, param);
      }
   }

   public final void doImportXml(BImportParameters param, Logger log) {
      this.log = log;
      this.doImportXml(param);
   }

   public void doExportXml() {
      this.log().fine("doExportXml not implemented");
   }

   public final void doTrim() {
      BINetworkVariable[] nvs = this.getNetworkVariables();

      for (int i = 0; i < nvs.length; i++) {
         if (nvs[i] != null && nvs[i].isNetworkVariable()) {
            BNetworkVariable nv = (BNetworkVariable)nvs[i];
            BComponent p = (BComponent)nv.getParent();
            Property nvProp = nv.getPropertyInParent();
            BNvConfigData nvCfg = nv.getNvConfigData();
            if (!nv.hasProxies()
               && !nvCfg.isBoundNv()
               && (nvCfg.isInput() && p.getLinks(nvProp).length == 0 || nvCfg.isOutput() && p.getKnobs(nvProp).length == 0)) {
               p.remove(nv);
               if (this.log().isLoggable(Level.FINE)) {
                  this.log().fine("trim " + this.getDisplayName(null) + ":" + nv.getDisplayName(null));
               }
            }
         }
      }
   }
}
