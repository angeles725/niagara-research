package com.tridium.lonworks.xml;

import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.xml.XElem;
import javax.baja.xml.XException;
import javax.baja.xml.XParser;

public class LonXMLReader {
   XParser parser;
   BIFile file;

   public static XLonInterfaceFile decode(BOrd ord) {
      return decode((BIFile)ord.resolve().get());
   }

   public static XLonInterfaceFile decode(File file) {
      BOrd ord = BOrd.make("file:/" + file.getAbsolutePath().replace('\\', '/'));

      try {
         return decode(ord);
      } catch (XException var3) {
         throw new XException("XML Parsing Error in Ord: " + ord + " - " + var3.toString());
      }
   }

   public static XLonInterfaceFile decode(BIFile file) {
      try {
         InputStream xmlIn = file.getInputStream();
         return new LonXMLReader(XParser.make(xmlIn), file).decode();
      } catch (XException var2) {
         throw var2;
      } catch (Throwable var3) {
         System.err.println("-----------");
         System.err.println("-----------");
         var3.printStackTrace();
         System.err.println("-----------");
         System.err.println("-----------");
         throw new RuntimeException("Cannot access " + file.getNavOrd().toString(null) + "\n" + var3);
      }
   }

   private LonXMLReader(XParser p, BIFile file) {
      this.parser = p;
      this.file = file;
   }

   private XLonInterfaceFile decode() throws Exception {
      this.parser.next();
      XElem root = this.parser.elem();
      if (!root.get("type", null).equals("XLonInterfaceFile")) {
         throw this.err("Root element type must be \"XLonInterfaceFile\"");
      } else {
         XLonInterfaceFile container = new XLonInterfaceFile();
         container.setName(root.name());
         this.parseType(container);
         this.parser.close();
         return container;
      }
   }

   private void parseType(Object parent) throws Exception {
      while (true) {
         int ptype = this.parser.next();
         if (ptype == -1) {
            throw new EOFException();
         }

         if (ptype == 2) {
            return;
         }

         if (ptype != 1) {
            throw this.err("Expected obj start");
         }

         this.parseElem(parent, this.parser.elem());
      }
   }

   private void parseElem(Object parent, XElem xelem) throws Exception {
      Object obj = null;
      String name = xelem.name();
      String value = xelem.get("v", null);
      String type = xelem.get("type", null);
      String file = xelem.get("file", null);
      if (value != null && value.equals("shortName")) {
         System.out.println("=================================================================");
         System.out.println("=================================================================");
         System.out.println("WAKE UP!");
         System.out.println("=================================================================");
         System.out.println("=================================================================");
      }

      if (file != null) {
         try {
            obj = this.parseFile(file);
            if (obj == null) {
               throw new RuntimeException("Unable to parse " + file);
            }
         } catch (UnresolvedException var14) {
            StringBuilder prettyError = new StringBuilder();
            prettyError.append("\nSorry. There is a problem with the following XML element:\n\n");
            prettyError.append(xelem.toString()).append("\n\n");
            prettyError.append("The system is unable to locate the specified include file:\n");
            prettyError.append("  ").append(file).append("\n\n");
            prettyError.append("Suggestion:\n");
            prettyError.append("- Verify the exact spelling and case of each letter of the file name in both the file system/module and the lon xml file.");
            prettyError.append("\n\n");
            throw this.err(prettyError.toString(), var14);
         }
      } else if (type != null) {
         try {
            obj = createLonType(type);
         } catch (Throwable var13) {
            throw this.err("Unable to create class com.tridium.lonworks.xml." + type);
         }
      } else if (name.equals("union")) {
         try {
            obj = createLonType("XUnion");
         } catch (Throwable var12) {
            throw this.err("Unable to create class XUnion");
         }
      } else if (value != null) {
         if (parent == null) {
            throw this.err("Invalid xml format. No container specified for " + value);
         }

         if (parent instanceof XEnumDef && !name.equals("typeSpec")) {
            ((XEnumDef)parent).addEnum(name, value);
         } else {
            this.updateElement(parent, name, value);
         }

         obj = value;
      } else if (name.equals("elem")) {
         if (parent == null) {
            throw this.err("Invalid xml format. No container specified");
         }

         String ename = xelem.get("n");
         String quals = xelem.get("qual", null);
         XElementQualifier eq = new XElementQualifier(ename, quals);
         eq.setEnumDef(xelem.get("enumDef", null));
         eq.setDefault(xelem.get("default", null));
         eq.setEngUnit(xelem.get("engUnit", null));
         eq.setTag0(xelem.get("tag0", null));
         eq.setTag1(xelem.get("tag1", null));
         eq.setUnionBranch(xelem.get("u", null));
         ((XLonData)parent).addAttribute(ename, eq);
         obj = quals;
      } else if (name.equals("branch")) {
         if (parent == null) {
            throw this.err("Invalid xml format. No container specified for branch");
         }

         String nam = xelem.get("n", null);
         String cond = xelem.get("c", null);
         XUnionBranch ub = new XUnionBranch(nam, cond);
         ((XLonData)parent).addAttribute(name, ub);
         obj = nam;
      } else {
         if (parent == null) {
            throw this.err("no parent for " + name);
         }

         try {
            obj = parent.getClass().getField(name).getType().getDeclaredConstructor().newInstance();
         } catch (Throwable var11) {
            throw this.err("Unable to create class for " + name);
         }
      }

      if (obj instanceof XLonData) {
         ((XLonData)obj).setName(name);
      }

      if (parent != null && !(obj instanceof String)) {
         this.addObject(parent, name, obj);
      }

      if (obj != null) {
         this.parseType(obj);
      }
   }

   private void updateElement(Object parent, String elemName, String value) {
      try {
         Field f = parent.getClass().getField(elemName);
         String name = "set" + Character.toUpperCase(elemName.charAt(0)) + elemName.substring(1);
         Class<?>[] parms = new Class[]{String.class};
         Method m = null;

         try {
            m = parent.getClass().getMethod(name, parms);
         } catch (NoSuchMethodException var10) {
         }

         if (m != null) {
            Object[] args = new Object[]{value};
            m.invoke(parent, args);
         } else {
            Object v;
            if (Boolean.class.isInstance(v = f.get(parent))) {
               f.setBoolean(parent, Boolean.valueOf(value));
            } else if (Character.class.isInstance(v)) {
               f.setChar(parent, value.charAt(0));
            } else if (Byte.class.isInstance(v)) {
               f.setByte(parent, Byte.valueOf(value));
            } else if (Short.class.isInstance(v)) {
               f.setShort(parent, Short.valueOf(value));
            } else if (Integer.class.isInstance(v)) {
               f.setInt(parent, Integer.valueOf(value));
            } else if (Long.class.isInstance(v)) {
               f.setLong(parent, Long.valueOf(value));
            } else if (Float.class.isInstance(v)) {
               f.setFloat(parent, value.equals("NaN") ? Float.NaN : Float.valueOf(value));
            } else if (Double.class.isInstance(v)) {
               f.setDouble(parent, Double.valueOf(value));
            } else if (String.class.isInstance(v)) {
               f.set(parent, value);
            } else {
               System.out.println("No setter method and not a public primitive java type for:" + elemName);
            }
         }
      } catch (Throwable var11) {
         System.out.println("WARNING:Unable to access property " + elemName);
      }
   }

   private void addObject(Object parent, String name, Object value) {
      try {
         Field f = parent.getClass().getField(name);
         f.set(parent, value);
      } catch (Throwable var5) {
         ((XLonData)parent).addAttribute(name, value);
      }
   }

   private Object parseFile(String fileName) throws UnresolvedException {
      String fName = fileName.replace('\\', '/');

      String p;
      for (p = this.file.getNavParent().getNavOrd().toString(); fName.startsWith("../"); fName = fName.substring(3)) {
         p = p.substring(0, p.lastIndexOf(47));
      }

      BOrd o = BOrd.make(p + (p.endsWith("/") ? "" : "/") + fName);
      XLonInterfaceFile xfile = decode(o);
      xfile.setFile(fileName);
      return xfile;
   }

   private static Object createLonType(String name) {
      if ("XConfigProperty".equals(name)) {
         return new XConfigProperty();
      } else if ("XCpTypeDef".equals(name)) {
         return new XCpTypeDef();
      } else if ("XDeviceData".equals(name)) {
         return new XDeviceData();
      } else if ("XDeviceFacets".equals(name)) {
         return new XDeviceFacets();
      } else if ("XElementQualifier".equals(name)) {
         return new XElementQualifier();
      } else if ("XEnumDef".equals(name)) {
         return new XEnumDef();
      } else if ("XLonDataUtil".equals(name)) {
         return new XLonDataUtil();
      } else if ("XLonDevice".equals(name)) {
         return new XLonDevice();
      } else if ("XLonEngUnits".equals(name)) {
         return new XLonEngUnits();
      } else if ("XLonInterfaceFile".equals(name)) {
         return new XLonInterfaceFile();
      } else if ("XMessageTag".equals(name)) {
         return new XMessageTag();
      } else if ("XNetworkConfig".equals(name)) {
         return new XNetworkConfig();
      } else if ("XNetworkVariable".equals(name)) {
         return new XNetworkVariable();
      } else if ("XTypeDef".equals(name)) {
         return new XTypeDef();
      } else if ("XUnion".equals(name)) {
         return new XUnion();
      } else if ("XUtil".equals(name)) {
         return new XUtil();
      } else {
         throw new XException("unable to create class " + name);
      }
   }

   XException err(String msg, Throwable cause) {
      return new XException(msg, this.parser, cause);
   }

   XException err(String msg) {
      return new XException(msg, this.parser);
   }
}
