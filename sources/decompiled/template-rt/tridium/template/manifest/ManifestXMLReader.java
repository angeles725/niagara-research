package com.tridium.template.manifest;

import com.tridium.install.BDependency;
import com.tridium.template.BTemplateState;
import java.io.File;
import java.io.InputStream;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.sys.BajaException;
import javax.baja.util.BUuid;
import javax.baja.xml.XElem;
import javax.baja.xml.XException;
import javax.baja.xml.XParser;

public class ManifestXMLReader {
   XParser parser;
   BIFile file;
   TemplateManifest tplMnfst;

   public static TemplateManifest decode(BOrd ord) {
      return decode((BIFile)ord.resolve().get());
   }

   public static TemplateManifest decode(File file) {
      BOrd ord = BOrd.make("file:/" + file.getAbsolutePath().replace('\\', '/'));

      try {
         return decode(ord);
      } catch (XException var3) {
         throw new XException("XML Parsing Error in Ord: " + ord + " - " + var3.toString());
      }
   }

   public static TemplateManifest decode(BIFile file) {
      try {
         InputStream ins = file.getInputStream();
         ManifestXMLReader rdr = new ManifestXMLReader(XParser.make(ins), file);
         return rdr.decode();
      } catch (XException var3) {
         throw var3;
      } catch (Throwable var4) {
         var4.printStackTrace();
         throw new RuntimeException("Cannot access " + file.getNavOrd().toString(null) + "\n" + var4);
      }
   }

   private ManifestXMLReader(XParser p, BIFile file) {
      this.parser = p;
      this.file = file;
      this.tplMnfst = new TemplateManifest();
   }

   private TemplateManifest decode() throws Exception {
      XElem root = this.parser.parse();
      if (!root.name().equals("template")) {
         throw new BajaException("Root element must be <template>");
      } else {
         this.parseVersion(root);

         for (int i = 0; i < root.contentSize(); i++) {
            XElem xelem = root.elem(i);
            String name = xelem.name();
            switch (name) {
               case "info":
                  this.tplMnfst.info = xelem.get("i", "");
                  break;
               case "settings":
                  this.getValues(this.tplMnfst.settings, xelem);
                  break;
               case "links":
                  this.getValues(this.tplMnfst.links, xelem);
                  break;
               case "bindings":
                  this.getValues(this.tplMnfst.bindings, xelem);
                  break;
               case "resources":
                  this.getResources(xelem);
                  break;
               case "subtemplates":
                  this.getSubtemplates(xelem);
                  break;
               case "tags":
                  this.getTags(xelem);
                  break;
               case "dependencies":
                  this.getDependencies(xelem);
                  break;
               case "revisions":
                  this.getRevisions(xelem);
                  break;
               case "optionals":
                  this.getOptionals(xelem);
            }
         }

         this.parser.close();
         return this.tplMnfst;
      }
   }

   private void parseVersion(XElem xelem) {
      try {
         this.tplMnfst.uID = BUuid.make(xelem.get("id"));
      } catch (XException var4) {
         this.tplMnfst.uID = BUuid.make();
      }

      this.tplMnfst.title = xelem.get("title", "");
      this.tplMnfst.version = xelem.get("version", "1.0");
      this.tplMnfst.vendor = xelem.get("vendor", "");
      this.tplMnfst.description = xelem.get("description", "");
      this.tplMnfst.info = xelem.get("info", "");
      this.tplMnfst.buildVersion = xelem.get("buildVersion", "");
      this.tplMnfst.bogSignature = xelem.get("signature", "0");
      this.tplMnfst.isApplication = xelem.getb("isApplication", false);
      this.tplMnfst.isStation = xelem.getb("isStation", false);

      try {
         this.tplMnfst.state = BTemplateState.make(xelem.geti("state"));
      } catch (XException var3) {
         this.tplMnfst.state = BTemplateState.DEFAULT;
      }
   }

   private void getValues(Array<TemplateManifest.Value> a, XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         TemplateManifest.Value val = new TemplateManifest.Value();
         val.name = xelem.name();
         val.name = xelem.get("n", val.name);
         val.required = xelem.getb("req", false);
         val.type = xelem.get("typ", "num");
         val.slotPath = xelem.get("slotPath", "/" + val.name);
         String s = xelem.get("units", "");
         val.hasUnit = s.length() > 0;
         if (val.hasUnit) {
            val.unit = s;
         }

         s = xelem.get("min", "");
         val.hasMin = s.length() > 0;
         if (val.hasMin) {
            val.min = Float.parseFloat(s);
         }

         s = xelem.get("max", "");
         val.hasMax = s.length() > 0;
         if (val.hasMax) {
            val.max = Float.parseFloat(s);
         }

         a.add(val);
      }
   }

   private void getResources(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         TemplateManifest.Resource res = new TemplateManifest.Resource();
         res.name = xelem.name();
         res.name = xelem.get("n", res.name);
         res.type = xelem.get("type", "");
         res.sourceOrd = xelem.get("sourceOrd", "");
         this.tplMnfst.resources.add(res);
      }
   }

   private void getSubtemplates(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         TemplateManifest.Subtemplate st = new TemplateManifest.Subtemplate();
         st.name = xelem.name();
         st.name = xelem.get("n", st.name);
         st.vendor = xelem.get("vendor", "");
         st.version = xelem.get("version", "");
         st.locationOrd = xelem.get("locationOrd", "");
         st.ntplFileOrd = xelem.get("ntplFileOrd", "");
         this.tplMnfst.subtemplates.add(st);
      }
   }

   private void getTags(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         TemplateManifest.Tag tag = new TemplateManifest.Tag();
         tag.name = xelem.name();
         tag.name = xelem.get("n", tag.name);
         this.tplMnfst.tags.add(tag);
      }
   }

   private void getRevisions(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         TemplateManifest.Revision rev = new TemplateManifest.Revision();
         rev.version = xelem.get("v", "");
         rev.description = xelem.get("d", "");
         this.tplMnfst.revisionHistory.add(rev);
      }
   }

   private void getOptionals(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         BOrd opt = BOrd.make(xelem.get("ord", ""));
         this.tplMnfst.optional.add(opt);
      }
   }

   private void getDependencies(XElem xl) throws Exception {
      for (int i = 0; i < xl.contentSize(); i++) {
         XElem xelem = xl.elem(i);
         BDependency dep = BDependency.make(xelem);
         this.tplMnfst.dependencies.add(dep);
      }
   }

   XException err(String msg, Throwable cause) {
      return new XException(msg, this.parser, cause);
   }

   XException err(String msg) {
      return new XException(msg, this.parser);
   }
}
