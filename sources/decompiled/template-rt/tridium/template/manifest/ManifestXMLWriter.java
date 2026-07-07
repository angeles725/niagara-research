package com.tridium.template.manifest;

import com.tridium.install.BDependency;
import com.tridium.template.BTemplateState;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.xml.XWriter;

public class ManifestXMLWriter extends XWriter {
   TemplateManifest tplMnfst;
   private int indent;

   public ManifestXMLWriter(File file) throws IOException {
      super(file);
   }

   public ManifestXMLWriter(OutputStream out) throws IOException {
      super(out);
   }

   public void encode(TemplateManifest root) throws IOException {
      this.tplMnfst = root;
      this.prolog();
      this.w("<template");
      this.attrs("id", this.tplMnfst.uID.toString());
      this.attrs("version", this.tplMnfst.version);
      this.attrs("vendor", this.tplMnfst.vendor);
      this.attrs("title", this.tplMnfst.title);
      this.attrs("description", this.tplMnfst.description);
      this.attrs("state", String.valueOf(this.tplMnfst.state == null ? BTemplateState.DEFAULT : this.tplMnfst.state.getOrdinal()));
      this.attrs("buildVersion", this.tplMnfst.buildVersion);
      this.attrs("signature", this.tplMnfst.bogSignature);
      if (this.tplMnfst.isApplication) {
         this.attrs("isApplication", Boolean.TRUE.toString());
      }

      if (this.tplMnfst.isStation) {
         this.attrs("isStation", Boolean.TRUE.toString());
      }

      this.w(">").nl().nl();
      this.indent(2).w("<info");
      this.attrs("i", this.tplMnfst.info);
      this.w("/>").nl().nl();
      this.writeRevisions().nl();
      this.writeValues("settings", this.tplMnfst.settings, false).nl();
      this.writeValues("links", this.tplMnfst.links, true).nl();
      this.writeValues("bindings", this.tplMnfst.bindings, false).nl();
      this.writeResources().nl();
      this.writeSubtemplates().nl();
      this.writeTags().nl();
      this.writeDependencies().nl();
      this.writeOptionals().nl();
      this.w("</template>");
      this.flush();
   }

   private ManifestXMLWriter attrs(String name, String value) {
      this.w(" ");
      return (ManifestXMLWriter)this.attr(name, value);
   }

   private XWriter writeValues(String name, Array<TemplateManifest.Value> a, boolean writeDirection) {
      this.indent(2).w("<").w(name).w(">").nl();

      for (TemplateManifest.Value val : a) {
         this.indent(4).w("<p");
         this.attrs("n", val.name);
         this.attrs("req", Boolean.toString(val.required));
         if (writeDirection) {
            this.attrs("dir", val.direction);
         }

         if (!val.type.equals("num")) {
            this.attrs("typ", val.type);
         }

         if (val.hasUnit) {
            this.attrs("units", val.unit);
         }

         if (val.hasMin) {
            this.attrs("min", Float.toString((Float)val.min));
         }

         if (val.hasMax) {
            this.attrs("max", Float.toString((Float)val.max));
         }

         if (val.slotPath.length() > 0) {
            this.attrs("slotPath", val.slotPath);
         }

         this.w("/>").nl();
      }

      this.indent(2).w("</").w(name).w(">").nl();
      return this;
   }

   private XWriter writeResources() {
      this.indent(2).w("<resources>").nl();

      for (TemplateManifest.Resource res : this.tplMnfst.resources) {
         this.indent(4).w("<r");
         this.attrs("n", res.name);
         this.attrs("type", res.type).attrs("sourceOrd", res.sourceOrd);
         this.w("/>").nl();
      }

      this.indent(2).w("</resources>").nl();
      return this;
   }

   private XWriter writeSubtemplates() {
      this.indent(2).w("<subtemplates>").nl();

      for (TemplateManifest.Subtemplate st : this.tplMnfst.subtemplates) {
         this.indent(4).w("<s");
         this.attrs("n", st.name);
         this.attrs("vendor", st.vendor).attrs("version", st.version).attrs("locationOrd", st.locationOrd).attrs("ntplFileOrd", st.ntplFileOrd);
         this.w("/>").nl();
      }

      this.indent(2).w("</subtemplates>").nl();
      return this;
   }

   private XWriter writeDependencies() {
      this.indent(2).w("<dependencies>").nl();

      for (BDependency dep : this.tplMnfst.dependencies) {
         this.indent(4).w("<dependency");
         this.attrs("name", dep.getPartName());
         this.attrs("vendorVersion", dep.getVersion().getVendorVersionString());
         this.attrs("rel", dep.getVersionRelation().getTag());
         this.attrs("vendor", dep.getVersion().getVendor());
         this.w("/>").nl();
      }

      this.indent(2).w("</dependencies>").nl();
      return this;
   }

   private XWriter writeTags() {
      this.indent(2).w("<tags>").nl();

      for (TemplateManifest.Tag tg : this.tplMnfst.tags) {
         this.indent(4).w("<t");
         this.attrs("n", tg.name);
         this.w("/>").nl();
      }

      this.indent(2).w("</tags>").nl();
      return this;
   }

   private XWriter writeRevisions() {
      this.indent(2).w("<revisions>").nl();

      for (TemplateManifest.Revision rev : this.tplMnfst.revisionHistory) {
         this.indent(4).w("<rev");
         this.attrs("v", rev.version);
         this.attrs("d", rev.description);
         this.w("/>").nl();
      }

      this.indent(2).w("</revisions>").nl();
      return this;
   }

   private XWriter writeOptionals() {
      this.indent(2).w("<optionals>").nl();

      for (BOrd opt : this.tplMnfst.optional) {
         this.indent(4).w("<optional");
         this.attrs("ord", opt.encodeToString());
         this.w("/>").nl();
      }

      this.indent(2).w("</optionals>").nl();
      return this;
   }
}
