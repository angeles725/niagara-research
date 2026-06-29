package com.tridium.lonworks.xml;

import com.tridium.lonworks.util.NameUtil;
import java.util.Vector;

public class XLonDataUtil {
   private static String[] SnvtTypeNames = null;
   private static String[] ScptTypeNames = null;
   private static final String[] ApplicationTypes = new String[]{"unknown", "mip", "neuron", "hostSelect", "hostNISelect"};
   private static final String[] ServiceTypes = new String[]{"acked", "unackedRpt", "unacked", "request"};
   private static final String[] Scope = new String[]{"node", "object", "nv"};

   public static String snvtTypeToString(int type) {
      return getString(getSnvtTypeNames(), type, SnvtTypeNames[0]);
   }

   public static int snvtTypeFromString(String type) {
      if (type != null && type.length() != 0) {
         int ndx = type.indexOf(43);
         String name = ndx > 0 ? type.substring(0, ndx) : type;
         return fromString(getSnvtTypeNames(), name, 0);
      } else {
         return 0;
      }
   }

   public static boolean isDiffQualifier(String type) {
      int ndx = type.indexOf(43);
      return ndx < 0 ? false : type.substring(ndx + 1).equals("diff");
   }

   public static String getSnvtTypeName(int snvt, String def) {
      String[] snvts = getSnvtTypeNames();
      return snvt >= 1 && snvt < snvts.length ? snvts[snvt] : def;
   }

   private static String[] getSnvtTypeNames() {
      if (SnvtTypeNames == null) {
         Vector<XTypeDef> types = XUtil.getStandard().types;
         int maxType = 0;

         for (int i = 0; i < types.size(); i++) {
            XTypeDef t = types.elementAt(i);
            if (!t.isCpType()) {
               int ndx = t.getTypeIndex();
               if (ndx > maxType) {
                  maxType = ndx;
               }
            }
         }

         SnvtTypeNames = new String[maxType + 1];

         for (int ix = 0; ix < types.size(); ix++) {
            XTypeDef t = types.elementAt(ix);
            if (!t.isCpType()) {
               int ndx = t.getTypeIndex();
               if (ndx >= 0) {
                  SnvtTypeNames[ndx] = NameUtil.toJavaName(t.getName(), false);
               }
            }
         }

         for (int ixx = 0; ixx <= maxType; ixx++) {
            if (SnvtTypeNames[ixx] == null) {
               SnvtTypeNames[ixx] = "xxx";
            }
         }
      }

      return SnvtTypeNames;
   }

   public static String scptTypeToString(int type) {
      return getString(getScptTypeNames(), type, ScptTypeNames[0]);
   }

   public static int scptTypeFromString(String type) {
      return fromString(getScptTypeNames(), type, 0);
   }

   public static String getScptTypeName(int scpt, String def) {
      String[] scpts = getScptTypeNames();
      return scpt >= 1 && scpt < scpts.length ? scpts[scpt] : def;
   }

   private static String[] getScptTypeNames() {
      if (ScptTypeNames == null) {
         Vector<XTypeDef> types = XUtil.getStandard().types;
         int maxType = 0;

         for (int i = 0; i < types.size(); i++) {
            XTypeDef t = types.elementAt(i);
            if (t.isCpType()) {
               int ndx = t.getTypeIndex();
               if (ndx > maxType) {
                  maxType = ndx;
               }
            }
         }

         ScptTypeNames = new String[maxType + 1];

         for (int ix = 0; ix < types.size(); ix++) {
            XTypeDef t = types.elementAt(ix);
            if (t.isCpType()) {
               int ndx = t.getTypeIndex();
               if (ndx >= 0) {
                  ScptTypeNames[ndx] = NameUtil.toJavaName(t.getName(), false);
               }
            }
         }

         for (int ixx = 0; ixx <= maxType; ixx++) {
            if (ScptTypeNames[ixx] == null) {
               ScptTypeNames[ixx] = "xxx";
            }
         }
      }

      return ScptTypeNames;
   }

   public static String applicationTypeToString(int ndx) {
      return getString(ApplicationTypes, ndx, ApplicationTypes[0]);
   }

   public static int applicationTypeFromString(String type) {
      return fromString(ApplicationTypes, type, 0);
   }

   public static boolean isHostedApplication(String type) {
      return type.equals("mip") || type.equals("hostSelect") || type.equals("hostNISelect");
   }

   public static String serviceTypeToString(int ndx) {
      return getString(ServiceTypes, ndx, ServiceTypes[0]);
   }

   public static int serviceTypeFromString(String type) {
      return fromString(ServiceTypes, type, 0);
   }

   public static String scopeToString(int ndx) {
      return getString(Scope, ndx, Scope[0]);
   }

   public static int scopeFromString(String type) {
      return fromString(Scope, type, 0);
   }

   public static String flagToString(int flag) {
      flag &= 255;
      String sendBack = "";
      if ((flag & 127) == 0) {
         return "anytime";
      } else {
         if ((flag & 1) != 0) {
            sendBack = sendBack + " objDisable";
         }

         if ((flag & 2) != 0) {
            sendBack = sendBack + " offline";
         }

         if ((flag & 4) != 0) {
            if ((flag & 32) != 0) {
               sendBack = sendBack + " deviceSpecific";
            } else {
               sendBack = sendBack + " constant";
            }
         }

         if ((flag & 8) != 0) {
            sendBack = sendBack + " reset";
         }

         if ((flag & 16) != 0) {
            sendBack = sendBack + " mfgOnly";
         }

         if (sendBack.startsWith(" ")) {
            sendBack = sendBack.substring(1);
         }

         return sendBack;
      }
   }

   public static int flagFromString(String string) {
      int flag = 128;
      if (string.indexOf("anytime") != -1) {
         return flag;
      } else {
         if (string.indexOf("objDisable") != -1) {
            flag |= 1;
         }

         if (string.indexOf("offline") != -1) {
            flag |= 2;
         }

         if (string.indexOf("constant") != -1) {
            flag |= 4;
         }

         if (string.indexOf("reset") != -1) {
            flag |= 8;
         }

         if (string.indexOf("mfgOnly") != -1) {
            flag |= 16;
         }

         if (string.indexOf("deviceSpecific") != -1) {
            flag |= 36;
         }

         return flag;
      }
   }

   public static boolean isReadOnly(String flag) {
      return flag.indexOf("constant") >= 0 || flag.indexOf("deviceSpecific") >= 0;
   }

   public static String getString(String[] a, int ndx, String def) {
      return ndx >= a.length ? def : a[ndx];
   }

   public static int fromString(String[] a, String s, int def) {
      for (int i = 0; i < a.length; i++) {
         if (a[i].equals(s)) {
            return i;
         }
      }

      return def;
   }
}
