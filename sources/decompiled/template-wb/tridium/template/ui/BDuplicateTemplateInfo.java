package com.tridium.template.ui;

import com.tridium.template.api.TemplateType;
import com.tridium.template.file.NtplUtil;
import com.tridium.workbench.fieldeditors.BSubdirectoryFE;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "fileName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "folderName",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"workbench:SubdirectoryFE\""
      )}
   ), @NiagaraProperty(
      name = "title",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "version",
      type = "String",
      defaultValue = "BDuplicateTemplateInfo.DEFAULT_VERSION_STRING"
   )})
public class BDuplicateTemplateInfo extends BComponent {
   public static final Property fileName = newProperty(0, "", null);
   public static final Property folderName = newProperty(0, BOrd.DEFAULT, BFacets.make("fieldEditor", "workbench:SubdirectoryFE"));
   public static final Property title = newProperty(0, "", null);
   public static final Property version = newProperty(0, "0.0", null);
   public static final Type TYPE = Sys.loadType(BDuplicateTemplateInfo.class);
   private static final String DEFAULT_VERSION_STRING = "0.0";

   public String getFileName() {
      return this.getString(fileName);
   }

   public void setFileName(String v) {
      this.setString(fileName, v, null);
   }

   public BOrd getFolderName() {
      return (BOrd)this.get(folderName);
   }

   public void setFolderName(BOrd v) {
      this.set(folderName, v, null);
   }

   public String getTitle() {
      return this.getString(title);
   }

   public void setTitle(String v) {
      this.setString(title, v, null);
   }

   public String getVersion() {
      return this.getString(version);
   }

   public void setVersion(String v) {
      this.setString(version, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDuplicateTemplateInfo() {
   }

   public BDuplicateTemplateInfo(String fileName, String folderName, String title, String version, TemplateType templateType) {
      this.setFileName(fileName);
      this.setFolderName(this.buildFolderNameOrd(folderName, templateType));
      this.setTitle(title);
      this.setVersion(version);
   }

   public String getFolderNameText() {
      return BSubdirectoryFE.getSubdirectoryNameFromValue(this.getFolderName());
   }

   private BOrd buildFolderNameOrd(String folderName, TemplateType templateType) {
      return BSubdirectoryFE.buildValue(NtplUtil.makeTemplateDirectoryOrd(templateType), folderName);
   }
}
