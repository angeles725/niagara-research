package com.tridium.template;

import com.tridium.sys.tag.ComponentRelations;
import java.util.Collection;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Relation;
import javax.baja.util.BUuid;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "locationOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 65538
   ), @NiagaraProperty(
      name = "templateName",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "uID",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 65538
   ), @NiagaraProperty(
      name = "vendor",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "version",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "inputs",
      type = "int",
      defaultValue = "0",
      flags = 65538
   ), @NiagaraProperty(
      name = "outputs",
      type = "int",
      defaultValue = "0",
      flags = 65538
   ), @NiagaraProperty(
      name = "relations",
      type = "int",
      defaultValue = "0",
      flags = 65538
   ), @NiagaraProperty(
      name = "defaultPasswords",
      type = "int",
      defaultValue = "0",
      flags = 65538
   ), @NiagaraProperty(
      name = "availVersion",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "status",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "state",
      type = "int",
      defaultValue = "0",
      flags = 65538
   ), @NiagaraProperty(
      name = "ntplSignature",
      type = "String",
      defaultValue = "",
      flags = 65538
   ), @NiagaraProperty(
      name = "localSignature",
      type = "String",
      defaultValue = "",
      flags = 65538
   )})
public final class BTemplateInfo extends BComponent {
   public static final Property locationOrd = newProperty(65538, BOrd.NULL, null);
   public static final Property templateName = newProperty(65538, "", null);
   public static final Property uID = newProperty(65538, BUuid.DEFAULT, null);
   public static final Property vendor = newProperty(65538, "", null);
   public static final Property version = newProperty(65538, "", null);
   public static final Property inputs = newProperty(65538, 0, null);
   public static final Property outputs = newProperty(65538, 0, null);
   public static final Property relations = newProperty(65538, 0, null);
   public static final Property defaultPasswords = newProperty(65538, 0, null);
   public static final Property availVersion = newProperty(65538, "", null);
   public static final Property status = newProperty(65538, "", null);
   public static final Property state = newProperty(65538, 0, null);
   public static final Property ntplSignature = newProperty(65538, "", null);
   public static final Property localSignature = newProperty(65538, "", null);
   public static final Type TYPE = Sys.loadType(BTemplateInfo.class);
   public static int UP_TO_DATE_SEL = 0;
   public static int OUT_OF_DATE_SEL = 1;
   public static int OLDER_VERSION_AVAILABLE_SEL = 2;
   public static int NOT_AVAILABLE_SEL = 3;
   public static int UNKNOWN_SEL = 4;
   public static int UPGRADE_SEL = 5;
   public static int DOWNGRADE_SEL = 6;
   public static int REDEPLOY_SEL = 7;
   public static int UPDATED_SEL = 8;
   BTemplateConfig templateConfig = null;

   public BOrd getLocationOrd() {
      return (BOrd)this.get(locationOrd);
   }

   public void setLocationOrd(BOrd v) {
      this.set(locationOrd, v, null);
   }

   public String getTemplateName() {
      return this.getString(templateName);
   }

   public void setTemplateName(String v) {
      this.setString(templateName, v, null);
   }

   public BUuid getUID() {
      return (BUuid)this.get(uID);
   }

   public void setUID(BUuid v) {
      this.set(uID, v, null);
   }

   public String getVendor() {
      return this.getString(vendor);
   }

   public void setVendor(String v) {
      this.setString(vendor, v, null);
   }

   public String getVersion() {
      return this.getString(version);
   }

   public void setVersion(String v) {
      this.setString(version, v, null);
   }

   public int getInputs() {
      return this.getInt(inputs);
   }

   public void setInputs(int v) {
      this.setInt(inputs, v, null);
   }

   public int getOutputs() {
      return this.getInt(outputs);
   }

   public void setOutputs(int v) {
      this.setInt(outputs, v, null);
   }

   public int getRelations() {
      return this.getInt(relations);
   }

   public void setRelations(int v) {
      this.setInt(relations, v, null);
   }

   public int getDefaultPasswords() {
      return this.getInt(defaultPasswords);
   }

   public void setDefaultPasswords(int v) {
      this.setInt(defaultPasswords, v, null);
   }

   public String getAvailVersion() {
      return this.getString(availVersion);
   }

   public void setAvailVersion(String v) {
      this.setString(availVersion, v, null);
   }

   public String getStatus() {
      return this.getString(status);
   }

   public void setStatus(String v) {
      this.setString(status, v, null);
   }

   public int getState() {
      return this.getInt(state);
   }

   public void setState(int v) {
      this.setInt(state, v, null);
   }

   public String getNtplSignature() {
      return this.getString(ntplSignature);
   }

   public void setNtplSignature(String v) {
      this.setString(ntplSignature, v, null);
   }

   public String getLocalSignature() {
      return this.getString(localSignature);
   }

   public void setLocalSignature(String v) {
      this.setString(localSignature, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BTemplateInfo make(BTemplateConfig tmplConfig) {
      BTemplateInfo info = new BTemplateInfo();
      info.templateConfig = tmplConfig;
      info.setLocationOrd(tmplConfig.getHandleOrd());
      info.setTemplateName(tmplConfig.getTemplateName());
      info.setVendor(tmplConfig.getManifest().vendor);
      info.setUID(tmplConfig.getUID());
      info.setVersion(tmplConfig.getManifest().version);
      info.setInputs(tmplConfig.getInputSlots().length);
      info.setOutputs(tmplConfig.getOutputSlots().length);
      info.setRelations(tmplConfig.getRelationInfos().size());
      info.setDefaultPasswords(tmplConfig.getPasswordInfos().size());
      return info;
   }

   public BTemplateConfig getTemplateConfig() {
      if (this.templateConfig == null) {
         this.templateConfig = (BTemplateConfig)this.getLocationOrd().resolve(this).get();
      }

      return this.templateConfig;
   }

   public BComponent getTemplateRoot() {
      return this.getTemplateConfig().getParent().asComponent();
   }

   public BComponent getTemplateParent() {
      return this.getTemplateRoot().getParent().asComponent();
   }

   public int getModifiedState() {
      this.loadSlots();
      if (this.getNtplSignature().length() <= 1) {
         return -1;
      } else {
         return this.getNtplSignature().equals(this.getLocalSignature()) ? 0 : 1;
      }
   }

   public boolean isRedeploy() {
      return this.getState() == REDEPLOY_SEL;
   }

   public boolean isSubtemplate() {
      int count = 0;

      for (BComplex parent = this.getTemplateConfig().getParent(); parent != null && parent.isComponent(); parent = parent.getParent()) {
         if (parent instanceof BStation) {
            return false;
         }

         if (parent.asComponent().tags().contains(TemplateConst.TEMPLATE_ROOT_TAG_ID)) {
            if (++count > 1) {
               return true;
            }
         }
      }

      return false;
   }

   public int getSubtemplateDepth() {
      Collection<Relation> all = new ComponentRelations(this).getAll(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID, 1);
      return all.size();
   }
}
