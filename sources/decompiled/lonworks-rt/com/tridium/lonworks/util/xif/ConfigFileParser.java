package com.tridium.lonworks.util.xif;

import com.tridium.lonworks.resource.CpType;
import com.tridium.lonworks.resource.CrossReference;
import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.util.selfdoc.ConfigTemplateRecord;
import com.tridium.lonworks.util.selfdoc.NodeSelfDoc;
import com.tridium.lonworks.util.selfdoc.SelfDocUtil;
import com.tridium.lonworks.xml.XConfigProperty;
import com.tridium.lonworks.xml.XLonDataUtil;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.baja.lonworks.enums.BLonConfigScope;

public class ConfigFileParser {
   private NodeSelfDoc nodeSelfDoc = null;
   private XifLineReader reader;
   private PrintStream out;
   private CrossReference crossRef;

   public ConfigFileParser(XifLineReader reader, PrintStream out, CrossReference crossRef, String selfDoc) {
      this.reader = reader;
      this.out = out;
      this.crossRef = crossRef;
      this.nodeSelfDoc = new NodeSelfDoc(selfDoc);
   }

   public Vector<XConfigProperty> parse() {
      Vector<XConfigProperty> vect = new Vector<>();

      try {
         int ndx = 0;

         while (true) {
            String line = this.readRecord();
            if (line.length() == 0) {
               break;
            }

            String comment = line.substring(line.indexOf(";") + 1);
            line = line.substring(0, line.indexOf(";"));
            if (line.equals("")) {
               break;
            }

            vect.addElement(this.parseConfig(line, comment, ndx++));
         }

         ConfigFileParser.DuplicateNamesUtil.eliminateDuplicateNames(vect);
      } catch (IOException var5) {
      }

      return vect;
   }

   private String readRecord() throws IOException {
      boolean done = false;
      StringBuilder sb = new StringBuilder();

      while (!done) {
         String line = this.reader.readXifLine(false);
         if (line.length() == 0) {
            return "";
         }

         if (line.startsWith("\"")) {
            line = line.substring(1);
         }

         if (line.indexOf(";") >= 0) {
            done = true;
         }

         sb.append(line);
      }

      return sb.toString();
   }

   private XConfigProperty parseConfig(String line, String comment, int index) {
      ConfigTemplateRecord tmplRec = new ConfigTemplateRecord(line);
      XConfigProperty cp = new XConfigProperty();
      cp.scope = XLonDataUtil.scopeToString(tmplRec.getHdr());
      cp.select = tmplRec.getSelect();
      cp.modifyFlag = XLonDataUtil.flagToString(tmplRec.getFlags());
      cp.length = tmplRec.getLength();
      cp.dimension = tmplRec.getDimension();
      if (tmplRec.hasMin()) {
         cp.min = tmplRec.getMin();
      }

      if (tmplRec.hasMax()) {
         cp.max = tmplRec.getMax();
      }

      if (this.crossRef != null) {
         try {
            CpType typ = this.crossRef.findCpType(tmplRec.getTypeScope(), tmplRec.getConfigIndex());
            if (typ.inherited && cp.scope.equals(BLonConfigScope.object.getTag())) {
               int objType = this.nodeSelfDoc.getObjectType(SelfDocUtil.getFirstIndex(cp.select));
               cp.principalNv = this.crossRef.getPrincipalNv(objType);
            }

            if (tmplRec.getTypeScope() == 0) {
               cp.scptType = "Cp" + NameUtil.toJavaName(typ.node.name, true);
            } else {
               cp.setTypeDef("Cp" + NameUtil.toJavaName(typ.node.name, true));
               this.crossRef.mark(typ.scope);
            }
         } catch (Throwable var8) {
            this.warning(var8);
         }
      }

      String name = this.getCpName(cp, tmplRec, comment);
      if (name.length() == 0) {
         name = "config" + index;
      } else if (name.startsWith("nci")) {
         name = name.substring(3);
      } else if (name.startsWith("Cp")) {
         name = name.substring(2);
      }

      name = NameUtil.toJavaName(name, false);
      cp.setName(name);
      return cp;
   }

   private String getCpName(XConfigProperty cp, ConfigTemplateRecord tmplRec, String comment) {
      int pos = comment.indexOf("n=\"");
      if (pos >= 0) {
         return comment.substring(pos + 3, comment.indexOf("\"", pos + 4));
      } else {
         try {
            if (this.crossRef != null && cp.scope.equals("object")) {
               int objType = this.getObjectType(cp.select);
               if (objType >= 0) {
                  try {
                     return this.crossRef.getCpName(objType, tmplRec.getTypeScope(), tmplRec.getConfigIndex());
                  } catch (Throwable var7) {
                  }
               }
            }

            return cp.scptType.length() > 0 ? cp.scptType : cp.getTypeDef();
         } catch (Throwable var8) {
            this.warning(var8);
            return "";
         }
      }
   }

   private int getObjectType(String select) {
      try {
         StringTokenizer tok = new StringTokenizer(select, ",-~.");
         int objNdx = Integer.decode(tok.nextToken());
         return this.nodeSelfDoc.getObject(objNdx).getType();
      } catch (Exception var4) {
         return -1;
      }
   }

   private void warning(Throwable e) {
      String msg = e.getMessage();
      String exception = e instanceof RuntimeException ? "" : e.getClass().getName();
      this.out.println("WARNING:" + exception + " " + (msg != null ? msg : ""));
   }

   static class DuplicateNamesUtil {
      private static void bucketize(Hashtable<String, Vector<XConfigProperty>> sameNameBuckets, Iterator<XConfigProperty> xit) {
         while (xit.hasNext()) {
            XConfigProperty xConfig = xit.next();
            String xConfigName = xConfig.getName();
            Vector<XConfigProperty> sameNameBucket = sameNameBuckets.get(xConfigName);
            if (sameNameBucket == null) {
               sameNameBucket = new Vector<>();
               sameNameBuckets.put(xConfigName, sameNameBucket);
            }

            sameNameBucket.addElement(xConfig);
         }
      }

      private static Hashtable<String, Vector<XConfigProperty>> bucketize(Vector<XConfigProperty> xConfigs) {
         Hashtable<String, Vector<XConfigProperty>> sameNameBuckets = new Hashtable<>(xConfigs.size());
         bucketize(sameNameBuckets, xConfigs.iterator());
         return sameNameBuckets;
      }

      private static void examineBucket(Vector<XConfigProperty> sameNameBucket) {
         if (sameNameBucket.size() > 1) {
            addNameSuffixes(sameNameBucket.elements());
         }
      }

      private static void addNameSuffixes(Enumeration<XConfigProperty> sameNameCps) {
         for (int suffix = 1; sameNameCps.hasMoreElements(); suffix++) {
            XConfigProperty cp = sameNameCps.nextElement();
            cp.setName(cp.getName() + suffix);
         }
      }

      static void eliminateDuplicateNames(Vector<XConfigProperty> xConfigs) {
         Enumeration<Vector<XConfigProperty>> eSameNameBuckets = bucketize(xConfigs).elements();

         while (eSameNameBuckets.hasMoreElements()) {
            examineBucket(eSameNameBuckets.nextElement());
         }
      }
   }
}
