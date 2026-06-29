package com.tridium.lonworks.xml;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;
import javax.baja.xml.XWriter;

public class LonXMLWriter extends XWriter {
   private boolean ouputDefaults;
   private int indent;

   public LonXMLWriter(File file, boolean ouputDefaults) throws IOException {
      super(file);
      this.ouputDefaults = ouputDefaults;
   }

   public LonXMLWriter(OutputStream out, boolean ouputDefaults) throws IOException {
      super(out);
      this.ouputDefaults = ouputDefaults;
   }

   public void encode(Object root) throws IOException {
      this.w("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      this.encodeDocument((XLonData)root, true);
      this.flush();
      this.close();
   }

   private void encodeDocument(XLonData obj, boolean specType) throws IOException {
      String objName = obj.getName();
      this.encodeName(objName);
      if (obj instanceof XLonInterfaceFile && ((XLonInterfaceFile)obj).getFile().length() > 0) {
         this.encodeFileName((XLonInterfaceFile)obj);
      } else {
         if (specType) {
            this.encodeType(obj);
         }

         this.w(">\n");
         this.indent++;
         if (obj instanceof XEnumDef) {
            this.encodeEnum((XEnumDef)obj);
         } else {
            Class<?> cl = obj.getClass();
            Field[] a = cl.getFields();
            Object def = null;
            if (!this.ouputDefaults) {
               try {
                  def = cl.getDeclaredConstructor().newInstance();
               } catch (Throwable var11) {
               }
            }

            for (int i = 0; i < a.length; i++) {
               Object attr;
               Object defAttr;
               try {
                  attr = a[i].get(obj);
                  defAttr = def != null ? a[i].get(def) : null;
               } catch (Throwable var12) {
                  System.out.println(var12);
                  continue;
               }

               if (attr != null && !Modifier.isStatic(a[i].getModifiers())) {
                  if (attr instanceof XLonData) {
                     this.encodeDocument((XLonData)attr, false);
                  } else if (attr instanceof Vector) {
                     this.encodeVector((Vector<?>)attr);
                  } else {
                     this.encodeAttribute(obj, attr, a[i].getName(), def, defAttr);
                  }
               }
            }
         }

         this.indent--;
         this.printIndent();
         this.w("</");
         this.w(objName);
         this.w(">\n");
      }
   }

   private void encodeVector(Vector<?> v) throws IOException {
      for (int i = 0; i < v.size(); i++) {
         Object obj = v.elementAt(i);
         if (obj instanceof XElementQualifier) {
            this.encodeElementQualifier((XElementQualifier)obj);
         } else if (obj instanceof XUnionBranch) {
            this.encodeUnionBranch((XUnionBranch)obj);
         } else {
            this.encodeDocument((XLonData)obj, true);
         }
      }
   }

   private void encodeEnum(XEnumDef obj) throws IOException {
      String objName = obj.getName();
      String[] tags = obj.getEnumTags();
      int[] ids = obj.getEnumIds();
      if (tags.length != ids.length) {
         throw new RuntimeException("Error encoding enum " + objName + ".\nNumber of tags and ids mismatched.");
      } else {
         for (int i = 0; i < tags.length; i++) {
            this.printIndent();
            this.w("<");
            this.w(tags[i]);
            this.w(" v=\"");
            this.w(Integer.toString(ids[i]));
            this.w("\"/>\n");
         }
      }
   }

   private void encodeElementQualifier(XElementQualifier eq) {
      this.printIndent();
      this.w("<elem");
      if (eq.branch != null) {
         this.w(" u=\"");
         this.w(eq.branch);
         this.w("\"");
      }

      this.w(" n=\"");
      this.w(eq.name);
      this.w("\" qual=\"");
      this.w(eq.encodeToString());
      if (eq.enumDef != null) {
         this.w("\" enumDef=\"");
         this.w(eq.enumDef);
      }

      if (eq.defaultValue != null) {
         this.w("\" default=\"");
         this.w(eq.defaultValue);
      }

      if (eq.engUnit != null) {
         this.w("\" engUnit=\"");
         this.w(eq.engUnit);
      }

      if (eq.tag0 != null) {
         this.w("\" tag0=\"");
         this.w(eq.tag0);
      }

      if (eq.tag1 != null) {
         this.w("\" tag1=\"");
         this.w(eq.tag1);
      }

      this.w("\"/>\n");
   }

   private void encodeUnionBranch(XUnionBranch brch) {
      this.printIndent();
      this.w("<branch n=\"");
      this.w(brch.branchName);
      this.w("\" c=\"");
      this.w(brch.condition);
      this.w("\"/>\n");
   }

   private void encodeAttribute(Object parent, Object attr, String name, Object defParent, Object defAttr) {
      String value = null;

      try {
         value = this.getValue(parent, attr, name);
         if (!this.ouputDefaults && defAttr != null) {
            String defValue = this.getValue(defParent, defAttr, name);
            if (defValue.equals(value)) {
               return;
            }
         }
      } catch (Throwable var8) {
         throw new RuntimeException("Unable to encode " + name + var8);
      }

      this.printIndent();
      this.w("<");
      this.w(name);
      this.w(" v=\"");
      this.writeSafe(value);
      this.w("\"/>\n");
   }

   private String getValue(Object parent, Object attr, String name) throws Exception {
      String nm = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
      Method m = null;

      try {
         m = parent.getClass().getDeclaredMethod(nm);
      } catch (NoSuchMethodException var8) {
      }

      String value;
      if (m != null) {
         value = (String)m.invoke(parent);
      } else if (Boolean.class.isInstance(attr)) {
         value = ((Boolean)attr).toString();
      } else if (Character.class.isInstance(attr)) {
         value = ((Character)attr).toString();
      } else if (Byte.class.isInstance(attr)) {
         value = ((Byte)attr).toString();
      } else if (Short.class.isInstance(attr)) {
         value = ((Short)attr).toString();
      } else if (Integer.class.isInstance(attr)) {
         value = ((Integer)attr).toString();
      } else if (Long.class.isInstance(attr)) {
         value = ((Long)attr).toString();
      } else if (Float.class.isInstance(attr)) {
         value = ((Float)attr).toString();
      } else if (Double.class.isInstance(attr)) {
         value = ((Double)attr).toString();
      } else {
         if (!String.class.isInstance(attr)) {
            throw new RuntimeException("No getter method and not a primitive java type for:" + name);
         }

         value = ((String)attr).toString();
      }

      return value;
   }

   private void encodeName(String name) throws IOException {
      this.printIndent();
      this.w('<');
      this.w(name);
   }

   private void encodeType(Object object) throws IOException {
      String className = object.getClass().getName();
      this.w(" type=\"");
      this.w(className.substring(className.lastIndexOf(46) + 1));
      this.w('"');
   }

   private void encodeFileName(XLonInterfaceFile obj) {
      this.w(" file=\"");
      this.w(obj.getFile());
      this.w("\"/>\n");
   }

   private void printIndent() {
      for (int i = 0; i < this.indent; i++) {
         this.w(' ');
      }
   }

   private void writeSafe(String s) {
      int len = s.length();

      for (int i = 0; i < len; i++) {
         char c = s.charAt(i);
         switch (c) {
            case '"':
               this.w("&quot;");
               break;
            case '&':
               this.w("&amp;");
               break;
            case '\'':
               this.w("&apos;");
               break;
            case '<':
               this.w("&lt;");
               break;
            case '>':
               this.w("&gt;");
               break;
            default:
               this.w(c);
         }
      }
   }
}
