package com.honeywell.easybinding.util;

public class EbConstantValues {
   public static final String ALARM_IMAGE;
   public static final String OVERRIDE_IMAGE;
   public static final String VALUE_IMAGE;
   public static final String UNDERSCORE = "_";
   public static final String TRIDIUM;
   public static final String WORKBENCH;
   public static final String BRAND_FEATURE;
   public static final String BRANDID;
   public static final String TREND;
   public static final String CENTRALINE;
   public static final String HONEYWELL_BMS;
   public static final String SBC;
   public static final String WEBS;
   public static final String ALERTON;
   public static final String COMFORTANDENERGY;
   public static final String COMFORTPOINT;
   public static final String HBS_VENDOR;
   public static final String TREND_VENDOR;
   public static final String CENTRALINE_VENDOR;
   public static final String ALERTON_VENDOR;
   public static final String SBC_VENDOR;
   public static final String WEBS_VENDOR;
   public static final String EB_FEATURE;
   public static final String EB_FEATURE_UNLICENSED;
   public static final String EB_CONFIG_XML_FILE_PATH;
   public static final String EB_CONFIG_XML_ELEM_ROOT;
   public static final String EB_CONFIG_XML_ELEM_POINT_CHOOSER;
   public static final String EB_CONFIG_XML_ELEM_LAST_SEARCH_TEXT;
   public static final String EB_CONFIG_XML_ELEM_LAST_BOOLEAN_CHECKBOX_VALUE;
   public static final String EB_CONFIG_XML_ELEM_LAST_NUMERIC_CHECKBOX_VALUE;
   public static final String EB_CONFIG_XML_ELEM_LAST_ENUM_CHECKBOX_VALUE;
   public static final String EB_CONFIG_XML_ELEM_LAST_SELECTED_FOLDER;
   public static final String EB_CONFIG_XML_ELEM_LAST_SELECTED_POINT;
   public static final String EB_CONFIG_XML_KEY_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_SEARCH_TEXT;
   public static final String EB_CONFIG_JSON_KEY_LAST_BOOLEAN_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_NUMERIC_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_ENUM_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_SELECTED_FOLDER;
   public static final String EB_CONFIG_JSON_KEY_LAST_SELECTED_POINT;
   public static final String EB_CONFIG_XML_ELEM_LAST_SECURE_ENUM_CHECKBOX_VALUE;
   public static final String EB_CONFIG_XML_ELEM_LAST_SECURE_BOOLEAN_CHECKBOX_VALUE;
   public static final String EB_CONFIG_XML_ELEM_LAST_SECURE_NUMERIC_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_SECURE_BOOLEAN_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_SECURE_NUMERIC_CHECKBOX_VALUE;
   public static final String EB_CONFIG_JSON_KEY_LAST_SECURE_ENUM_CHECKBOX_VALUE;
   public static final String EB_WIDGET_TYPE;
   public static final String PROP_POINT_ID;
   public static final String STATION_PREFIX;
   public static final String SLOT_PREFIX;
   public static final String VIRTUAL_PREFIX;

   private EbConstantValues() {
   }

   static {
      String var10000 = "<\u007fI@~\u001eVNZi";
      int var10001 = -1;

      while (true) {
         char[] var3 = var10000.toCharArray();
         int var10003 = var3.length;
         char[] var6 = var3;
         var10001 = var10003;

         for (int var0 = 0; var10001 > var0; var0++) {
            char var10005 = var6[var0];
            byte var10006;
            switch (var0 % 5) {
               case 0:
                  var10006 = 127;
                  break;
               case 1:
                  var10006 = 26;
                  break;
               case 2:
                  var10006 = 39;
                  break;
               case 3:
                  var10006 = 52;
                  break;
               default:
                  var10006 = 12;
            }

            var6[var0] = (char)(var10005 ^ var10006);
         }

         String var10 = new String(var6).intern();
         switch (var10001) {
            case 0:
               EB_FEATURE = var10;
               var10000 = "7uIQu\b\u007fKX";
               var10001 = 1;
               break;
            case 1:
               HBS_VENDOR = var10;
               var10000 = "<uJRc\rnfZh:tBFk\u0006";
               var10001 = 2;
               break;
            case 2:
               COMFORTANDENERGY = var10;
               var10000 = "<uJRc\rnw[e\u0011n";
               var10001 = 3;
               break;
            case 3:
               COMFORTPOINT = var10;
               var10000 = "\u001evFFa6wFSi";
               var10001 = 4;
               break;
            case 4:
               ALARM_IMAGE = var10;
               var10000 = "\u001dhFZh";
               var10001 = 5;
               break;
            case 5:
               BRAND_FEATURE = var10;
               var10000 = "(\u007fEG";
               var10001 = 6;
               break;
            case 6:
               WEBS = var10;
               var10000 = ",{NUN\nh@Q\u007f\fYHZx\ruKG";
               var10001 = 7;
               break;
            case 7:
               SBC_VENDOR = var10;
               var10000 = "\u0003lNFx\n{K\u000e#";
               var10001 = 8;
               break;
            case 8:
               VIRTUAL_PREFIX = var10;
               var10000 = "7uIQu\b\u007fKXN2I";
               var10001 = 9;
               break;
            case 9:
               HONEYWELL_BMS = var10;
               var10000 = "3{T@I\u0011oJwd\u001ayLVc\u0007LFXy\u001a";
               var10001 = 10;
               break;
            case 10:
               EB_CONFIG_XML_ELEM_LAST_ENUM_CHECKBOX_VALUE = var10;
               var10000 = "\u000fuNZx6~";
               var10001 = 11;
               break;
            case 11:
               PROP_POINT_ID = var10;
               var10000 = "3{T@N\u0010uKQm\u0011YOQo\u0014xHLZ\u001evRQ";
               var10001 = 12;
               break;
            case 12:
               EB_CONFIG_XML_ELEM_LAST_BOOLEAN_CHECKBOX_VALUE = var10;
               var10000 = ":{TMN\u0016tC]b\u0018YHZj\u0016}RFm\u000bsHZ\u007f";
               var10001 = 13;
               break;
            case 13:
               EB_CONFIG_XML_ELEM_ROOT = var10;
               var10000 = "\u0013{T@I\u0011oJwd\u001ayLVc\u0007LFXy\u001a";
               var10001 = 14;
               break;
            case 14:
               EB_CONFIG_JSON_KEY_LAST_ENUM_CHECKBOX_VALUE = var10;
               var10000 = "+hBZh YHZx\ruKk_\u0006iSQa\fEk@h";
               var10001 = 15;
               break;
            case 15:
               TREND_VENDOR = var10;
               var10000 = "\u0013{T@N\u0010uKQm\u0011YOQo\u0014xHLZ\u001evRQ";
               var10001 = 16;
               break;
            case 16:
               EB_CONFIG_JSON_KEY_LAST_BOOLEAN_CHECKBOX_VALUE = var10;
               var10000 = "\fnF@e\u0010t\u001dH";
               var10001 = 17;
               break;
            case 17:
               STATION_PREFIX = var10;
               var10000 = "\u001dhFZh6~";
               var10001 = 18;
               break;
            case 18:
               BRANDID = var10;
               var10000 = "3{T@_\u001avBWx\u001a~a[`\u001b\u007fU";
               var10001 = 19;
               break;
            case 19:
               EB_CONFIG_XML_ELEM_LAST_SELECTED_FOLDER = var10;
               var10000 = "3{T@_\u001ayRFi=uHXi\u001etd\\i\u001cqE[t){KAi";
               var10001 = 20;
               break;
            case 20:
               EB_CONFIG_XML_ELEM_LAST_SECURE_BOOLEAN_CHECKBOX_VALUE = var10;
               var10000 = ",Xd";
               var10001 = 21;
               break;
            case 21:
               SBC = var10;
               var10000 = "\u001a{TMN\u0016tC]b\u0018 bU\u007f\u0006XNZh\u0016t@ce\u001b}B@";
               var10001 = 22;
               break;
            case 22:
               EB_WIDGET_TYPE = var10;
               var10000 = "\u0013{T@_\u001a{UWd+\u007f_@";
               var10001 = 23;
               break;
            case 23:
               EB_CONFIG_JSON_KEY_LAST_SEARCH_TEXT = var10;
               var10000 = "3{T@_\u001ayRFi:tRYO\u0017\u007fD_n\u0010bqU`\n\u007f";
               var10001 = 24;
               break;
            case 24:
               EB_CONFIG_XML_ELEM_LAST_SECURE_ENUM_CHECKBOX_VALUE = var10;
               var10000 = ">vBFx\u0010t";
               var10001 = 25;
               break;
            case 25:
               ALERTON_VENDOR = var10;
               var10000 = "*tK]o\u001atTQh_\\BUx\nhB\u0014d\u0010tbU\u007f\u0006XNZh\u0016t@";
               var10001 = 26;
               break;
            case 26:
               EB_FEATURE_UNLICENSED = var10;
               var10000 = "+hBZh";
               var10001 = 27;
               break;
            case 27:
               TREND = var10;
               var10000 = "\u0010lBF~\u0016~B}a\u001e}B";
               var10001 = 28;
               break;
            case 28:
               OVERRIDE_IMAGE = var10;
               var10000 = "\u0013{T@_\u001ayRFi1oJQ~\u0016yd\\i\u001cqE[t){KAi";
               var10001 = 29;
               break;
            case 29:
               EB_CONFIG_JSON_KEY_LAST_SECURE_NUMERIC_CHECKBOX_VALUE = var10;
               var10000 = "7uIQu\b\u007fKXO\u001atSFm3sIQ";
               var10001 = 30;
               break;
            case 30:
               CENTRALINE_VENDOR = var10;
               var10000 = "/uNZx<rH[\u007f\u001ah";
               var10001 = 31;
               break;
            case 31:
               EB_CONFIG_XML_ELEM_POINT_CHOOSER = var10;
               var10000 = "\u0013{T@_\u001ayRFi:tRYO\u0017\u007fD_n\u0010bqU`\n\u007f";
               var10001 = 32;
               break;
            case 32:
               EB_CONFIG_JSON_KEY_LAST_SECURE_ENUM_CHECKBOX_VALUE = var10;
               var10000 = "3{T@B\nwBFe\u001cYOQo\u0014xHLZ\u001evRQ";
               var10001 = 33;
               break;
            case 33:
               EB_CONFIG_XML_ELEM_LAST_NUMERIC_CHECKBOX_VALUE = var10;
               var10000 = "\u0013{T@B\nwBFe\u001cYOQo\u0014xHLZ\u001evRQ";
               var10001 = 34;
               break;
            case 34:
               EB_CONFIG_JSON_KEY_LAST_NUMERIC_CHECKBOX_VALUE = var10;
               var10000 = "3{T@_\u001avBWx\u001a~w[e\u0011n";
               var10001 = 35;
               break;
            case 35:
               EB_CONFIG_XML_ELEM_LAST_SELECTED_POINT = var10;
               var10000 = "\u0013{T@_\u001avBWx\u001a~w[e\u0011n";
               var10001 = 36;
               break;
            case 36:
               EB_CONFIG_JSON_KEY_LAST_SELECTED_POINT = var10;
               var10000 = "\u0013{T@_\u001ayRFi=uHXi\u001etd\\i\u001cqE[t){KAi";
               var10001 = 37;
               break;
            case 37:
               EB_CONFIG_JSON_KEY_LAST_SECURE_BOOLEAN_CHECKBOX_VALUE = var10;
               var10000 = "\fvH@6P";
               var10001 = 38;
               break;
            case 38:
               SLOT_PREFIX = var10;
               var10000 = ">vBFx\u0010t\n";
               var10001 = 39;
               break;
            case 39:
               ALERTON = var10;
               var10000 = "7uIQu\b\u007fKX";
               var10001 = 40;
               break;
            case 40:
               WEBS_VENDOR = var10;
               var10000 = "3{T@_\u001ayRFi1oJQ~\u0016yd\\i\u001cqE[t){KAi";
               var10001 = 41;
               break;
            case 41:
               EB_CONFIG_XML_ELEM_LAST_SECURE_NUMERIC_CHECKBOX_VALUE = var10;
               var10000 = "\buU_n\u001atD\\";
               var10001 = 42;
               break;
            case 42:
               WORKBENCH = var10;
               var10000 = "\t{KAi";
               var10001 = 43;
               break;
            case 43:
               EB_CONFIG_XML_KEY_VALUE = var10;
               var10000 = "!yHZj\u0016}\bQn yHZj\u0016}\tLa\u0013";
               var10001 = 44;
               break;
            case 44:
               EB_CONFIG_XML_FILE_PATH = var10;
               var10000 = "+hNPe\nw";
               var10001 = 45;
               break;
            case 45:
               TRIDIUM = var10;
               var10000 = "\u0013{T@_\u001avBWx\u001a~a[`\u001b\u007fU";
               var10001 = 46;
               break;
            case 46:
               EB_CONFIG_JSON_KEY_LAST_SELECTED_FOLDER = var10;
               var10000 = "\t{KAi6wFSi";
               var10001 = 47;
               break;
            case 47:
               VALUE_IMAGE = var10;
               var10000 = "3{T@_\u001a{UWd+\u007f_@";
               var10001 = 48;
               break;
            case 48:
               EB_CONFIG_XML_ELEM_LAST_SEARCH_TEXT = var10;
               return;
            default:
               CENTRALINE = var10;
               var10000 = "\u0017uIqm\fce]b\u001bsIS";
               var10001 = 0;
         }
      }
   }
}
