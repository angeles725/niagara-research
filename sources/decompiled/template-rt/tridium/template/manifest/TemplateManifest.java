package com.tridium.template.manifest;

import com.tridium.install.BDependency;
import com.tridium.template.BTemplateState;
import java.util.ArrayList;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.util.BUuid;

public class TemplateManifest {
   public String version = "1.0";
   public String vendor = "";
   public String description = "";
   public String info = "";
   public String title = "";
   public BUuid uID;
   public String buildVersion = "";
   public String bogSignature = "";
   public BTemplateState state;
   public boolean isApplication = false;
   public boolean isStation = false;
   public Array<TemplateManifest.Value> settings = new Array(TemplateManifest.Value.class);
   public Array<TemplateManifest.Value> links = new Array(TemplateManifest.Value.class);
   public Array<TemplateManifest.Value> bindings = new Array(TemplateManifest.Value.class);
   public Array<TemplateManifest.Resource> resources = new Array(TemplateManifest.Resource.class);
   public Array<TemplateManifest.Subtemplate> subtemplates = new Array(TemplateManifest.Subtemplate.class);
   public Array<TemplateManifest.Tag> tags = new Array(TemplateManifest.Tag.class);
   public Array<BDependency> dependencies = new Array(BDependency.class);
   public Array<TemplateManifest.Revision> revisionHistory = new Array(TemplateManifest.Revision.class);
   public Array<BOrd> optional = new Array(BOrd.class);
   public boolean newCreation = false;
   public static final String NUMERIC = "num";
   public static final String BOOLEAN = "bool";
   public static final String STRING = "str";
   public static final String CONFIG = "cfg";
   public static final String INPUT = "in";
   public static final String OUTPUT = "out";
   public static final String PX = "px";
   public static final String PNG = "png";
   public static final String IMAGE = "image";
   public static final String DATA = "data";
   public static final String SUBTEMPLATE = "subtemplate";

   public void addDependencies(BDependency[] dependencies) {
      this.dependencies = new Array(BDependency.class);
      this.dependencies.addAll(dependencies);
   }

   public BDependency[] getDependencies() {
      return (BDependency[])this.dependencies.trim();
   }

   public void addResource(String name, String type, String sourceOrd) {
      TemplateManifest.Resource res = new TemplateManifest.Resource();
      res.name = name;
      res.type = type;
      res.sourceOrd = sourceOrd;
      this.resources.add(res);
   }

   public TemplateManifest.Resource getResource(String nam, String type) {
      for (int i = 0; i < this.resources.size(); i++) {
         TemplateManifest.Resource res = (TemplateManifest.Resource)this.resources.get(i);
         if (res.name.equals(nam) && (type == null || type.equals(res.type))) {
            return res;
         }
      }

      return null;
   }

   public TemplateManifest.Resource removeResource(String nam) {
      for (int i = 0; i < this.resources.size(); i++) {
         TemplateManifest.Resource res = (TemplateManifest.Resource)this.resources.get(i);
         if (res.name.equals(nam)) {
            this.resources.remove(i);
            return res;
         }
      }

      return null;
   }

   public TemplateManifest.Value addSetting(String name, String type, String slotPath) {
      TemplateManifest.Value val = new TemplateManifest.Value();
      val.name = name;
      val.type = type;
      val.slotPath = slotPath;
      this.settings.add(val);
      return val;
   }

   public void addSubtemplate(String name, String vendor, String version, String locationOrd, String ntplFileOrd) {
      TemplateManifest.Subtemplate st = new TemplateManifest.Subtemplate();
      st.name = name;
      st.vendor = vendor;
      st.version = version;
      st.locationOrd = locationOrd;
      st.ntplFileOrd = ntplFileOrd;
      this.subtemplates.add(st);
   }

   public TemplateManifest.Subtemplate getSubTemplate(String nam) {
      for (int i = 0; i < this.subtemplates.size(); i++) {
         TemplateManifest.Subtemplate st = (TemplateManifest.Subtemplate)this.subtemplates.get(i);
         if (st.name.equals(nam)) {
            return st;
         }
      }

      return null;
   }

   public TemplateManifest.Subtemplate removeSubtemplate(String nam) {
      for (int i = 0; i < this.subtemplates.size(); i++) {
         TemplateManifest.Subtemplate st = (TemplateManifest.Subtemplate)this.subtemplates.get(i);
         if (st.name.equals(nam)) {
            this.subtemplates.remove(i);
            return st;
         }
      }

      return null;
   }

   public TemplateManifest.Revision getRevision(String v) {
      for (int i = 0; i < this.revisionHistory.size(); i++) {
         TemplateManifest.Revision r = (TemplateManifest.Revision)this.revisionHistory.get(i);
         if (r.version.equals(v)) {
            return r;
         }
      }

      return null;
   }

   public String[] getVersions() {
      if (this.revisionHistory.size() <= 0) {
         return null;
      } else {
         ArrayList<String> versionArray = new ArrayList<>();

         for (int i = 0; i < this.revisionHistory.size(); i++) {
            TemplateManifest.Revision r = (TemplateManifest.Revision)this.revisionHistory.get(i);
            versionArray.add(r.version);
         }

         return versionArray.toArray(new String[1]);
      }
   }

   public void addRevision(String version, String description) {
      TemplateManifest.Revision newRevision = new TemplateManifest.Revision();
      newRevision.version = version;
      newRevision.description = description;
      this.revisionHistory.add(newRevision);
   }

   public static class Resource {
      public String name = "";
      public String type = "px";
      public String sourceOrd = "";
   }

   public static class Revision {
      public String version = "";
      public String description = "";
   }

   public static class Subtemplate {
      public String name = "";
      public String type = "subtemplate";
      public String vendor = "";
      public String version = "";
      public String locationOrd = "";
      public String ntplFileOrd = "";
   }

   public static class Tag {
      public String name = "";
   }

   public static class Value {
      public String slotPath = "";
      public String name = "";
      public boolean required = false;
      public String type = "num";
      public String direction = "cfg";
      public boolean hasUnit = false;
      public String unit = "";
      public boolean hasMin = false;
      public Number min = null;
      public boolean hasMax = false;
      public Number max = null;
   }
}
