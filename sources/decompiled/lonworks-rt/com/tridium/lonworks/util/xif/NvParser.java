package com.tridium.lonworks.util.xif;

import com.tridium.lonworks.resource.CpType;
import com.tridium.lonworks.resource.CrossReference;
import com.tridium.lonworks.resource.Type;
import com.tridium.lonworks.util.NameUtil;
import com.tridium.lonworks.util.selfdoc.NodeSelfDoc;
import com.tridium.lonworks.util.selfdoc.NvSelfDoc;
import com.tridium.lonworks.xml.XLonData;
import com.tridium.lonworks.xml.XLonDataUtil;
import com.tridium.lonworks.xml.XMessageTag;
import com.tridium.lonworks.xml.XNetVariable;
import com.tridium.lonworks.xml.XNetworkConfig;
import com.tridium.lonworks.xml.XNetworkVariable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.baja.nre.util.IntHashMap;

public class NvParser {
   static IntHashMap objMemHash = new IntHashMap(256);
   private XifLineReader reader;
   private PrintStream out;
   private CrossReference crossRef;
   private NodeSelfDoc nodeSelfDoc;
   private boolean readElems;
   private int version = 0;

   public NvParser(XifLineReader reader, PrintStream out, boolean readElems, CrossReference cr, NodeSelfDoc nodeSelfDoc) {
      this.reader = reader;
      this.out = out;
      this.readElems = readElems;
      this.crossRef = cr;
      this.nodeSelfDoc = nodeSelfDoc;
      this.version = nodeSelfDoc.minorVersion;
   }

   public XLonData parseNetwork(String line1) {
      XNetVariable netVar = null;

      try {
         String line2 = this.reader.readXifLine();
         StringBuilder doc = new StringBuilder();

         String line;
         for (line = this.reader.readXifLine(); line.startsWith("\""); line = this.reader.readXifLine()) {
            doc.append(line.substring(1));
         }

         String selfDoc;
         String line4;
         if (line.startsWith("*")) {
            selfDoc = "*";
            line4 = this.reader.readXifLine();
         } else {
            selfDoc = doc.toString();
            line4 = line;
         }

         StringTokenizer st = new StringTokenizer(line4, "*", false);
         String snvtType = XLonDataUtil.snvtTypeToString(Integer.parseInt(st.nextToken().trim()));
         int elemCount = Integer.parseInt(st.nextToken().trim());
         if (line2.endsWith("0")) {
            XNetworkVariable nv = new XNetworkVariable();
            nv = (XNetworkVariable)this.parseNetVariable(line1, line2, nv);
            nv.snvtType = snvtType;
            NvSelfDoc nvSDoc = new NvSelfDoc(selfDoc, this.version);
            if (!nvSDoc.isEmpty()) {
               if (nvSDoc.isNvDoc()) {
                  NvSelfDoc.NvSelfDocData nvData = nvSDoc.nv;
                  nv.objectIndex = nvData.getObjectIndexString();
                  nv.memberIndex = nvData.memberNumber;
                  nv.memberArraySize = nvData.memberArraySize;
                  nv.mfgMember = nvData.mfgMember;
                  nv.changeType = nvData.changeableType;
                  nv.comment = nvData.description;
               } else {
                  this.out.println("ERROR:\"" + selfDoc + "\" is not a network variable self-doc string");
               }
            }

            if (nv.objectIndex.length() > 0) {
               int ndx = nv.objectIndex.indexOf(45);
               int firstObjNdx;
               if (ndx > 0) {
                  firstObjNdx = Integer.parseInt(nv.objectIndex.substring(0, ndx));
               } else {
                  firstObjNdx = Integer.parseInt(nv.objectIndex);
               }

               boolean valid = this.validateObjMem(nv, firstObjNdx, firstObjNdx);
               if (valid && this.crossRef != null && snvtType.equals("xxx")) {
                  try {
                     int objType = this.nodeSelfDoc.getObjectType(firstObjNdx);
                     Type typ = this.crossRef.findNvTypeByObject(objType, nv.memberIndex, nv.mfgMember);
                     if (typ.scope == 0) {
                        nv.snvtType = XLonDataUtil.snvtTypeToString(typ.index);
                     } else {
                        nv.setTypeDef(NameUtil.toJavaName(typ.node.name, true));
                        this.crossRef.mark(typ.scope);
                     }
                  } catch (Throwable var20) {
                     this.out.println("WARNING:cannot resolve typedef for nv " + nv.getName() + "\n\t" + var20.getClass().getName() + " " + var20.getMessage());
                  }
               }
            }

            netVar = nv;
         } else if (line2.endsWith("1")) {
            XNetworkConfig xnci = new XNetworkConfig();
            xnci = (XNetworkConfig)this.parseNetVariable(line1, line2, xnci);
            xnci.snvtType = snvtType;

            NvSelfDoc nvSDocx;
            try {
               nvSDocx = new NvSelfDoc(selfDoc, this.version);
            } catch (Throwable var19) {
               this.out.println("Bad selfdoc string: " + selfDoc + " using \"*\" string instead.");
               nvSDocx = new NvSelfDoc("*", this.version);
            }

            if (!nvSDocx.isEmpty()) {
               if (!nvSDocx.isNciDoc()) {
                  this.out.println("ERROR:\"" + selfDoc + "\" is not a network config self-doc string");
               } else {
                  NvSelfDoc.ConfigNvSelfDocData nciData = nvSDocx.nvConfig;
                  xnci.scope = XLonDataUtil.scopeToString(nciData.hdr);
                  xnci.select = nciData.select;
                  xnci.modifyFlag = XLonDataUtil.flagToString(nciData.flg);
                  if (nciData.minSpecified) {
                     xnci.min = nciData.min;
                  }

                  if (nciData.minSpecified) {
                     xnci.max = nciData.max;
                  }

                  xnci.changeType = nciData.changeType;
                  int tScope = nciData.typeScope;
                  int tIndex = nciData.configIndex;
                  if (this.crossRef != null && tScope >= 0 && tIndex >= 0) {
                     try {
                        CpType typ = this.crossRef.findCpType(tScope, tIndex);
                        if (tScope == 0) {
                           xnci.scptType = "Cp" + NameUtil.toJavaName(typ.node.name, true);
                        } else {
                           xnci.setTypeDef("Cp" + NameUtil.toJavaName(typ.node.name, true));
                           this.crossRef.mark(typ.scope);
                        }
                     } catch (Throwable var18) {
                        this.out
                           .println(
                              "WARNING: cannot resolve typedef for config " + xnci.getName() + "\n" + var18.getClass().getName() + " " + var18.getMessage()
                           );
                     }
                  }
               }
            }

            netVar = xnci;
         }

         this.parseElements(elemCount, netVar);
      } catch (IOException var21) {
         var21.printStackTrace();
      }

      return netVar;
   }

   private boolean validateObjMem(XNetworkVariable nv, int firstObjNdx, int objArraySize) {
      boolean valid = true;

      for (int i = firstObjNdx; i < objArraySize; i++) {
         int hash = (nv.mfgMember ? 1048576 : 0) + (nv.memberIndex << 10) + i;
         XNetworkVariable otherNv;
         if ((otherNv = (XNetworkVariable)objMemHash.get(hash)) != null) {
            this.out.println("WARNING: Conflicting object member selfdoc declarations:");
            this.out.println("  " + nv.getName() + " & " + otherNv.getName() + "  both declared object/member " + i + "/" + nv.memberIndex);
            valid = false;
         }

         objMemHash.put(hash, nv);
      }

      return valid;
   }

   public XNetVariable parseNetVariable(String line1, String line2, XNetVariable netVariable) {
      try {
         StringTokenizer st = new StringTokenizer(line1);
         st.nextToken();
         netVariable.setName(NameUtil.toJavaName(st.nextToken(), false));
         netVariable.index = Integer.parseInt(st.nextToken());
         netVariable.averateRate = Integer.parseInt(st.nextToken());
         netVariable.maximumRate = Integer.parseInt(st.nextToken());
         netVariable.arraySize = Integer.parseInt(st.nextToken());
         if (netVariable.arraySize == 0) {
            netVariable.arraySize = 1;
         }

         st = new StringTokenizer(line2);
         netVariable.offline = Integer.parseInt(st.nextToken()) == 1;
         netVariable.bindable = Integer.parseInt(st.nextToken()) == 1;
         st.nextToken();
         netVariable.direction = Integer.parseInt(st.nextToken()) == 1 ? "output" : "input";
         netVariable.serviceType = XLonDataUtil.serviceTypeToString(Integer.parseInt(st.nextToken()));
         if (netVariable.serviceType.equals("request")) {
            netVariable.serviceType = "acked";
         }

         netVariable.serviceTypeConfigurable = Integer.parseInt(st.nextToken()) == 1;
         netVariable.authenticated = Integer.parseInt(st.nextToken()) == 1;
         netVariable.authenticatedConfigurable = Integer.parseInt(st.nextToken()) == 1;
         netVariable.priority = Integer.parseInt(st.nextToken()) == 1;
         netVariable.priorityConfigurable = Integer.parseInt(st.nextToken()) == 1;
         netVariable.polled = Integer.parseInt(st.nextToken()) == 1;
         netVariable.sync = Integer.parseInt(st.nextToken()) == 1;
         netVariable.config = Integer.parseInt(st.nextToken()) == 1;
      } catch (NoSuchElementException var5) {
         this.out.println("fields missing in the definition for nv " + netVariable.getName());
      } catch (Throwable var6) {
         this.out.println(var6);
      }

      return netVariable;
   }

   private void parseElements(int elemCount, XNetVariable netVariable) {
      if (netVariable != null) {
         try {
            if (netVariable.snvtType.equals("xxx") && this.readElems) {
               ElementParser elementParser = new ElementParser(this.out);

               for (int i = 0; i < elemCount; i++) {
                  String line = this.reader.readXifLine();
                  elementParser.parse(line, netVariable, i);
               }
            } else {
               for (int i = 0; i < elemCount; i++) {
                  this.reader.readXifLine();
               }
            }
         } catch (NoSuchElementException var6) {
            this.out.println("fields missing in the definition for nv " + netVariable.getName());
         } catch (Throwable var7) {
            this.out.println(var7);
         }
      }
   }

   public XMessageTag parseTag(String line) {
      StringTokenizer st = new StringTokenizer(line);
      XMessageTag messageTag = new XMessageTag();

      try {
         st.nextToken();
         messageTag.setName(NameUtil.toJavaName(st.nextToken(), false));
         messageTag.index = Integer.parseInt(st.nextToken());
         messageTag.averateRate = Integer.parseInt(st.nextToken());
         messageTag.maximumRate = Integer.parseInt(st.nextToken());
         st = new StringTokenizer(this.reader.readXifLine());
         st.nextToken();
         messageTag.bindable = Integer.parseInt(st.nextToken()) == 1;
      } catch (NoSuchElementException var5) {
         this.out.println("not all expected fields were present in the xif file");
      } catch (Throwable var6) {
         var6.printStackTrace();
      }

      return messageTag;
   }
}
