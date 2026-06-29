package com.tridium.lonworks.resource;

import com.tridium.lonworks.CancelOperationException;
import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.xml.XCpTypeDef;
import com.tridium.lonworks.xml.XElementQualifier;
import com.tridium.lonworks.xml.XEnumDef;
import com.tridium.lonworks.xml.XLonInterfaceFile;
import com.tridium.lonworks.xml.XTypeDef;
import com.tridium.lonworks.xml.XUnion;
import com.tridium.lonworks.xml.XUnionBranch;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.units.BUnit;
import javax.baja.units.UnitDatabase;

public class ResourceToXLon {
   public static final String[] JAVA_RESERVED_WORDS = new String[]{
      "true",
      "false",
      "null",
      "abstract",
      "double",
      "int",
      "strictfp",
      "boolean",
      "else",
      "interface",
      "super",
      "break",
      "extends",
      "long",
      "switch",
      "byte",
      "final",
      "native",
      "synchronized",
      "case",
      "finally",
      "new",
      "this",
      "catch",
      "float",
      "package",
      "throw",
      "char",
      "for",
      "private",
      "throws",
      "class",
      "goto",
      "protected",
      "transient",
      "const",
      "if",
      "public",
      "try",
      "continue",
      "implements",
      "return",
      "void",
      "default",
      "import",
      "short",
      "volatile",
      "do",
      "instanceof",
      "static",
      "while"
   };
   private boolean forceOffset = false;
   private int nextBitOffset;
   private int byteOffset;
   private int maxByteOffset;
   private boolean fixNames = true;
   private CrossReference crossRef;
   private PrintStream out;
   private TypeFile typeFile;
   public boolean parseUnions = true;

   public ResourceToXLon(CrossReference crossRef, boolean fixNames, PrintStream out) {
      this.crossRef = crossRef;
      this.fixNames = fixNames;
      this.out = out;
   }

   public synchronized XLonInterfaceFile convert(TypeFile tf, Conversion conv) {
      this.typeFile = tf;
      XLonInterfaceFile xfile = new XLonInterfaceFile();
      EnumSet[] enumSets = tf.enumSets;

      for (int i = 0; i < enumSets.length; i++) {
         if (conv == null || !conv.isBooleanEnum(enumSets[i].enumTag)) {
            XEnumDef xEnum = this.createXEnum(enumSets[i]);
            if (xEnum != null && xEnum.getName().indexOf("~") < 0) {
               if (conv != null) {
                  conv.renameEnumDef(xEnum);
               }

               xfile.addAttribute(xEnum.getName(), xEnum);
            }
         }
      }

      Type[] nvTypes = tf.nvTypes;

      for (int ix = 0; ix < nvTypes.length; ix++) {
         XTypeDef xType = this.createXType(nvTypes[ix], tf.scope);
         if (xType != null) {
            if (conv != null) {
               conv.renameTypeDef(xType);
            }

            xfile.addAttribute(xType.getName(), xType);
         }
      }

      Type[] cpTypes = tf.cpTypes;

      for (int ixx = 0; ixx < cpTypes.length; ixx++) {
         XCpTypeDef xCpType = this.createXCpType((CpType)cpTypes[ixx], tf.scope);
         if (xCpType != null) {
            if (conv != null) {
               conv.renameTypeDef(xCpType);
            }

            xfile.addAttribute(xCpType.getName(), xCpType);
         }
      }

      return xfile;
   }

   private XEnumDef createXEnum(EnumSet enumSet) {
      if (enumSet == null) {
         return null;
      } else {
         Hashtable<String, String> namH = new Hashtable<>();
         XEnumDef xEnum = new XEnumDef();
         xEnum.setName(this.toClassName(enumSet.enumTag));
         EnumSet.EnumMember[] members = enumSet.members;

         for (int i = 0; i < members.length; i++) {
            EnumSet.EnumMember mem = members[i];
            String nam = this.toElementName(mem.eString);
            String ord = Integer.toString(mem.eValue);
            if (namH.get(nam) != null) {
               this.out.print("Duplicate enum tag in " + xEnum.getName() + ". Change tag from " + nam);
               nam = nam.concat(ord);
               this.out.println(" to " + nam);
            }

            namH.put(nam, nam);
            xEnum.addEnum(nam, ord);
         }

         return xEnum;
      }
   }

   private XTypeDef createXType(Type type, int scope) {
      if (type == null) {
         return null;
      } else {
         TypeNode node = type.node;
         if (node.name.indexOf("~") >= 0) {
            return null;
         } else {
            this.resetOffset();
            XTypeDef xType = new XTypeDef();
            String xTypeScope = Integer.toString(scope) + "," + Integer.toString(type.index);
            xType.setTypeScope(xTypeScope);
            String xTypeName = this.toClassName(node.name);
            xType.setName(xTypeName);

            try {
               this.processTypeNode(xType, node, "", false, "");
            } catch (ResourceToXLon.UnionException var9) {
               this.out.println("WARNING: NV Type " + xTypeName + " for scope " + xTypeScope + " has a union. Unions require a class file.");
               if (!this.parseUnions) {
                  return null;
               }
            } catch (BajaRuntimeException var10) {
               this.out.println("WARNING:" + var10.getMessage());
               Throwable t = var10.getCause();
               if (t != null) {
                  this.out.println("\t" + t.getClass().getName() + " " + t.getMessage());
               }

               return null;
            }

            return xType;
         }
      }
   }

   private XCpTypeDef createXCpType(CpType cpType, int scope) {
      if (cpType == null) {
         return null;
      } else {
         TypeNode node = cpType.node;
         if (node.name.indexOf("~") >= 0) {
            return null;
         } else {
            this.resetOffset();
            XCpTypeDef xCpType = new XCpTypeDef();
            String xCpTypeScope = Integer.toString(scope) + "," + Integer.toString(cpType.index);
            xCpType.setTypeScope(xCpTypeScope);
            String xCpTypeName = "Cp" + this.toClassName(node.name);
            xCpType.setName(xCpTypeName);
            if (cpType.min != null) {
               xCpType.min = cpType.min;
            }

            if (cpType.max != null) {
               xCpType.max = cpType.max;
            }

            xCpType.init = cpType.init;
            xCpType.inherited = cpType.inherited;

            try {
               this.processTypeNode(xCpType, node, "", false, "");
            } catch (ResourceToXLon.UnionException var9) {
               this.out.println("WARNING: Cp type " + xCpTypeName + " for scope " + xCpTypeScope + " has a union. Unions require a class file.");
               if (!this.parseUnions) {
                  return null;
               }
            } catch (BajaRuntimeException var10) {
               this.out.println("WARNING:" + var10.getMessage());
               Throwable t = var10.getCause();
               if (t != null) {
                  this.out.println("\t" + t.getClass().getName() + " " + t.getMessage());
               }

               return null;
            }

            return xCpType;
         }
      }
   }

   private void processTypeNode(XTypeDef xType, TypeNode node, String prefix, boolean nestedType, String suffix) throws ResourceToXLon.UnionException {
      this.processTypeNode(xType, node, prefix, nestedType, suffix, null, "");
   }

   private void processTypeNode(XTypeDef xType, TypeNode node, String prefix, boolean nestedType, String suffix, String branchName) throws ResourceToXLon.UnionException {
      this.processTypeNode(xType, node, prefix, nestedType, suffix, branchName, "");
   }

   private void processTypeNode(XTypeDef xType, TypeNode node, String prefix, boolean nestedType, String suffix, String bName, String unionName) throws ResourceToXLon.UnionException {
      try {
         switch (node.nodeType) {
            case 8:
               TypeArray ta = (TypeArray)node;
               prefix = this.buildPrefix(prefix, node.name);
               if (ta.element.nodeType == 2) {
                  XElementQualifier eqx = new XElementQualifier();
                  eqx.setName(this.toElementName(prefix + suffix));
                  eqx.setElementType("st");
                  eqx.setLength(ta.numElements);
                  if (bName != null) {
                     eqx.setUnionBranch(bName);
                  }

                  xType.addAttribute("", eqx);
               } else {
                  for (int i = 0; i < ta.numElements; i++) {
                     if (Character.isDigit(prefix.charAt(prefix.length() - 1))) {
                        prefix = prefix + "n";
                     }

                     String s = suffix;
                     if (suffix.length() == 0) {
                        s = Integer.toString(i);
                     } else if (Character.isDigit(suffix.charAt(suffix.length() - 1))) {
                        s = suffix + "n" + Integer.toString(i);
                     } else {
                        suffix = suffix + Integer.toString(i);
                     }

                     this.processTypeNode(xType, ta.element, prefix, nestedType, s, bName);
                  }
               }
               break;
            case 9:
               TypeNode[] tn = ((TypeStruct)node).fields;
               if (nestedType) {
                  prefix = this.buildPrefix(prefix, node.name);
               }

               for (int i = 0; i < tn.length; i++) {
                  this.processTypeNode(xType, tn[i], prefix, true, suffix, bName);
               }
               break;
            case 10:
               if (!this.parseUnions) {
                  throw new ResourceToXLon.UnionException();
               }

               this.out.println("Union in : " + xType.getName() + "." + node.name);
               this.forceOffset = true;
               int unionOffset = this.adjustOffset(0);
               if (xType.union == null) {
                  xType.union = new XUnion("??");
               }

               TypeNode[] tn = ((TypeStruct)node).fields;
               prefix = this.buildPrefix(prefix, node.name);

               for (int i = 0; i < tn.length; i++) {
                  this.setOffset(unionOffset);
                  String bn = (bName != null ? bName : "") + tn[i].name;
                  if (tn[i].nodeType != 10) {
                     XUnionBranch branch = new XUnionBranch(bn, "??");
                     xType.union.addAttribute("", branch);
                  }

                  this.processTypeNode(xType, tn[i], prefix, true, suffix, bn, (unionName.length() > 0 ? unionName + '_' : "") + xType.getName() + '_' + prefix);
               }

               this.setOffset(this.maxByteOffset);
               nestedType = false;
               break;
            default:
               XElementQualifier eq;
               if (node.nodeType == 14) {
                  eq = this.processReference(xType, (TypeReference)node, prefix, suffix);
               } else {
                  eq = this.processScalar((TypeScalar)node, prefix, suffix);
               }

               if (bName != null) {
                  eq.setUnionBranch(bName);
               }

               xType.addAttribute("", eq);
         }
      } catch (ResourceToXLon.UnionException var14) {
         throw var14;
      } catch (Exception var15) {
         throw new BajaRuntimeException("Unable to process type for " + xType.getName(), var15);
      }
   }

   private XElementQualifier processReference(XTypeDef xType, TypeReference tr, String prefix, String suffix) throws ResourceToXLon.UnionException {
      TypeNode tn = this.crossRef.findNvType(tr.scope, tr.index).node;
      XElementQualifier eq = new XElementQualifier();
      eq.setName(this.toElementName(this.buildPrefix(prefix, tr.name) + suffix));
      if (tn.nodeType == 7) {
         TypeEnum te = (TypeEnum)tn;
         EnumSet es = this.crossRef.findEnum(te.enumScope, te.enumIndex);
         if (es.isBoolean) {
            eq.setElementType("b8");
         } else {
            eq.setElementType("e8");
            eq.setEnumDef(this.toClassName(es.enumTag));
         }
      } else {
         eq.setElementType("ref");
         String tnName = this.toClassName(tn.name);
         eq.setTypeDef(tnName);
      }

      int bOff = this.adjustOffset(tr.typeSize);
      if (this.forceOffset) {
         eq.setByteOffset(bOff);
      }

      return eq;
   }

   private XElementQualifier processScalar(TypeScalar node, String prefix, String suffix) {
      XElementQualifier eq = new XElementQualifier();
      eq.setName(this.toElementName(this.buildPrefix(prefix, node.name) + suffix));
      int bOff;
      switch (node.nodeType) {
         case 1:
            eq.setElementType("s8");
            bOff = this.adjustOffset(1);
            break;
         case 2:
            eq.setElementType("u8");
            bOff = this.adjustOffset(1);
            break;
         case 3:
            eq.setElementType("s8");
            bOff = this.adjustOffset(1);
            break;
         case 4:
            eq.setElementType("u8");
            bOff = this.adjustOffset(1);
            break;
         case 5:
            eq.setElementType("s16");
            bOff = this.adjustOffset(2);
            break;
         case 6:
            eq.setElementType("u16");
            bOff = this.adjustOffset(2);
            break;
         case 7:
            TypeEnum te = (TypeEnum)node;
            EnumSet es = this.crossRef.findEnum(te.enumScope, te.enumIndex);
            if (es == null) {
               throw new RuntimeException("Could not find enumSet " + te.enumScope + "\\" + te.enumIndex + " in " + this.typeFile.fileName);
            }

            if (es.isBoolean) {
               eq.setElementType("b8");
            } else {
               eq.setElementType("e8");
               eq.setEnumDef(this.toClassName(es.enumTag));
            }

            bOff = this.adjustOffset(1);
            return eq;
         case 8:
         case 9:
         case 10:
         case 14:
         default:
            throw new RuntimeException("Invalid NVT_TYPE : " + node.nodeType);
         case 11:
            TypeBitfield bf = (TypeBitfield)node;
            if (!bf.bitfSigned) {
               eq.setElementType("ub");
            } else {
               eq.setElementType("sb");
            }

            if (bf.bitfOffset < this.nextBitOffset) {
               this.incrementByteOffset();
            }

            this.nextBitOffset = bf.bitfOffset + bf.bitfSize;
            bOff = this.byteOffset;
            eq.setByteOffset(this.byteOffset);
            eq.setBitOffset(8 - bf.bitfOffset - bf.bitfSize);
            eq.setLength(bf.bitfSize);
            break;
         case 12:
            eq.setElementType("f32");
            bOff = this.adjustOffset(4);
            break;
         case 13:
            eq.setElementType("s32");
            bOff = this.adjustOffset(4);
            break;
         case 15:
            eq.setElementType("u32");
            bOff = this.adjustOffset(4);
            break;
         case 16:
            eq.setElementType("f64");
            bOff = this.adjustOffset(8);
            break;
         case 17:
            eq.setElementType("s64");
            bOff = this.adjustOffset(8);
            break;
         case 18:
            eq.setElementType("u64");
            bOff = this.adjustOffset(8);
      }

      if (this.forceOffset) {
         eq.setByteOffset(bOff);
      }

      float res = (float)(node.scaleA * Math.pow(10.0, node.scaleB));
      if (res == 0.0F) {
         res = 1.0F;
      }

      if (res != 1.0F) {
         eq.setResolution(res);
      }

      if (node.scaleC != 0) {
         eq.setOffset(-(node.scaleC * res));
      }

      if (node.hasMinimum()) {
         eq.setMinimum(this.applyResolution(node.getMinimum(), res));
      }

      if (node.hasMaximum()) {
         eq.setMaximum(this.applyResolution(node.getMaximum(), res));
      }

      if (node.hasInvalid()) {
         eq.setInvalid(node.getInvalid());
      }

      if (node.resUntIndex > 0) {
         String eu;
         try {
            eu = this.crossRef.getResourceString(node.resUntScope, node.resUntIndex);
         } catch (CancelOperationException var10) {
            throw var10;
         } catch (Throwable var11) {
            eu = "";
         }

         if (eu.length() > 0) {
            eq.setEngUnit(eu);

            try {
               BUnit.getUnit(eu);
            } catch (Throwable var9) {
               this.out.println("Could not find eng units " + eu + " " + node.resUntScope + "/" + node.resUntIndex);
            }
         }
      }

      return eq;
   }

   private Double applyResolution(Number val, float res) {
      if (res == 1.0) {
         return val.doubleValue();
      } else if (val instanceof Integer) {
         return (double)(val.intValue() * res);
      } else {
         return val instanceof Long ? (double)((float)val.longValue() * res) : (Double)val * res;
      }
   }

   private String toClassName(String n) {
      return !this.fixNames ? n : NameUtil.toJavaName(n, true);
   }

   private String toElementName(String n) {
      if (!this.fixNames) {
         return n;
      } else {
         String retVal = NameUtil.toJavaName(n, false);

         for (int i = 0; i < JAVA_RESERVED_WORDS.length; i++) {
            if (retVal.equals(JAVA_RESERVED_WORDS[i])) {
               retVal = retVal.substring(0, 1).toUpperCase() + retVal.substring(1);
            }
         }

         return retVal;
      }
   }

   private String buildPrefix(String prefix, String name) {
      if (prefix.length() == 0) {
         return this.toElementName(name);
      } else {
         return name.length() == 0 ? this.toElementName(prefix) : this.toElementName(prefix) + this.toClassName(name);
      }
   }

   private void resetOffset() {
      this.nextBitOffset = 0;
      this.byteOffset = 0;
      this.forceOffset = false;
      this.maxByteOffset = 0;
   }

   private int adjustOffset(int cnt) {
      if (this.nextBitOffset != 0) {
         this.byteOffset++;
      }

      int bOff = this.byteOffset;
      this.nextBitOffset = 0;
      this.byteOffset += cnt;
      if (this.byteOffset > this.maxByteOffset) {
         this.maxByteOffset = this.byteOffset;
      }

      return bOff;
   }

   private void incrementByteOffset() {
      this.byteOffset++;
      if (this.byteOffset > this.maxByteOffset) {
         this.maxByteOffset = this.byteOffset;
      }
   }

   private void setOffset(int off) {
      this.nextBitOffset = 0;
      this.byteOffset = off;
   }

   public static void main(String[] args) {
      String filename = args[0];
      if (filename == null) {
         System.out.println("must specify file to parse");
      } else {
         try {
            ResourceFileUtil.getResourceFile(filename);
         } catch (IOException var3) {
            System.err.println("ResoureFile.getResourceFile command failed");
            var3.printStackTrace();
         }
      }
   }

   static {
      UnitDatabase.getDefault();
   }

   static class UnionException extends Exception {
   }
}
