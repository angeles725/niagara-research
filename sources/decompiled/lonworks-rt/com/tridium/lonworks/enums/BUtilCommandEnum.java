package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("status"), @Range("find"), @Range("identify"), @Range("file"), @Range("dataStructs"), @Range("disableAuthentication"), @Range("reports"), @Range("readMem"), @Range("test"), @Range("display"), @Range("clear"), @Range("setUnconfigured"), @Range("setOnline"), @Range("reset"), @Range("fileDirectory"), @Range("configTemplateFile"), @Range("configValueFile"), @Range("other"), @Range("wink"), @Range("servicePin"), @Range("clearServicePin"), @Range("addressTable"), @Range("domainTable"), @Range("readOnlyStructure"), @Range("configStructure"), @Range("nvAliasTable"), @Range("nvConfig"), @Range("nvValue"), @Range("selfDocumentation"), @Range("routeTables"), @Range("netmgmtSummary"), @Range("programIds"), @Range("transmitErrors"), @Range("transmitErrorsNoClear"), @Range("verify"), @Range("networkSummary"), @Range("recalcChecksum"), @Range("verifyChannels")}
)
public final class BUtilCommandEnum extends BFrozenEnum {
   public static final int STATUS = 0;
   public static final int FIND = 1;
   public static final int IDENTIFY = 2;
   public static final int FILE = 3;
   public static final int DATA_STRUCTS = 4;
   public static final int DISABLE_AUTHENTICATION = 5;
   public static final int REPORTS = 6;
   public static final int READ_MEM = 7;
   public static final int TEST = 8;
   public static final int DISPLAY = 9;
   public static final int CLEAR = 10;
   public static final int SET_UNCONFIGURED = 11;
   public static final int SET_ONLINE = 12;
   public static final int RESET = 13;
   public static final int FILE_DIRECTORY = 14;
   public static final int CONFIG_TEMPLATE_FILE = 15;
   public static final int CONFIG_VALUE_FILE = 16;
   public static final int OTHER = 17;
   public static final int WINK = 18;
   public static final int SERVICE_PIN = 19;
   public static final int CLEAR_SERVICE_PIN = 20;
   public static final int ADDRESS_TABLE = 21;
   public static final int DOMAIN_TABLE = 22;
   public static final int READ_ONLY_STRUCTURE = 23;
   public static final int CONFIG_STRUCTURE = 24;
   public static final int NV_ALIAS_TABLE = 25;
   public static final int NV_CONFIG = 26;
   public static final int NV_VALUE = 27;
   public static final int SELF_DOCUMENTATION = 28;
   public static final int ROUTE_TABLES = 29;
   public static final int NETMGMT_SUMMARY = 30;
   public static final int PROGRAM_IDS = 31;
   public static final int TRANSMIT_ERRORS = 32;
   public static final int TRANSMIT_ERRORS_NO_CLEAR = 33;
   public static final int VERIFY = 34;
   public static final int NETWORK_SUMMARY = 35;
   public static final int RECALC_CHECKSUM = 36;
   public static final int VERIFY_CHANNELS = 37;
   public static final BUtilCommandEnum status = new BUtilCommandEnum(0);
   public static final BUtilCommandEnum find = new BUtilCommandEnum(1);
   public static final BUtilCommandEnum identify = new BUtilCommandEnum(2);
   public static final BUtilCommandEnum file = new BUtilCommandEnum(3);
   public static final BUtilCommandEnum dataStructs = new BUtilCommandEnum(4);
   public static final BUtilCommandEnum disableAuthentication = new BUtilCommandEnum(5);
   public static final BUtilCommandEnum reports = new BUtilCommandEnum(6);
   public static final BUtilCommandEnum readMem = new BUtilCommandEnum(7);
   public static final BUtilCommandEnum test = new BUtilCommandEnum(8);
   public static final BUtilCommandEnum display = new BUtilCommandEnum(9);
   public static final BUtilCommandEnum clear = new BUtilCommandEnum(10);
   public static final BUtilCommandEnum setUnconfigured = new BUtilCommandEnum(11);
   public static final BUtilCommandEnum setOnline = new BUtilCommandEnum(12);
   public static final BUtilCommandEnum reset = new BUtilCommandEnum(13);
   public static final BUtilCommandEnum fileDirectory = new BUtilCommandEnum(14);
   public static final BUtilCommandEnum configTemplateFile = new BUtilCommandEnum(15);
   public static final BUtilCommandEnum configValueFile = new BUtilCommandEnum(16);
   public static final BUtilCommandEnum other = new BUtilCommandEnum(17);
   public static final BUtilCommandEnum wink = new BUtilCommandEnum(18);
   public static final BUtilCommandEnum servicePin = new BUtilCommandEnum(19);
   public static final BUtilCommandEnum clearServicePin = new BUtilCommandEnum(20);
   public static final BUtilCommandEnum addressTable = new BUtilCommandEnum(21);
   public static final BUtilCommandEnum domainTable = new BUtilCommandEnum(22);
   public static final BUtilCommandEnum readOnlyStructure = new BUtilCommandEnum(23);
   public static final BUtilCommandEnum configStructure = new BUtilCommandEnum(24);
   public static final BUtilCommandEnum nvAliasTable = new BUtilCommandEnum(25);
   public static final BUtilCommandEnum nvConfig = new BUtilCommandEnum(26);
   public static final BUtilCommandEnum nvValue = new BUtilCommandEnum(27);
   public static final BUtilCommandEnum selfDocumentation = new BUtilCommandEnum(28);
   public static final BUtilCommandEnum routeTables = new BUtilCommandEnum(29);
   public static final BUtilCommandEnum netmgmtSummary = new BUtilCommandEnum(30);
   public static final BUtilCommandEnum programIds = new BUtilCommandEnum(31);
   public static final BUtilCommandEnum transmitErrors = new BUtilCommandEnum(32);
   public static final BUtilCommandEnum transmitErrorsNoClear = new BUtilCommandEnum(33);
   public static final BUtilCommandEnum verify = new BUtilCommandEnum(34);
   public static final BUtilCommandEnum networkSummary = new BUtilCommandEnum(35);
   public static final BUtilCommandEnum recalcChecksum = new BUtilCommandEnum(36);
   public static final BUtilCommandEnum verifyChannels = new BUtilCommandEnum(37);
   public static final BUtilCommandEnum DEFAULT = status;
   public static final Type TYPE = Sys.loadType(BUtilCommandEnum.class);

   public static BUtilCommandEnum make(int ordinal) {
      return (BUtilCommandEnum)status.getRange().get(ordinal, false);
   }

   public static BUtilCommandEnum make(String tag) {
      return (BUtilCommandEnum)status.getRange().get(tag);
   }

   private BUtilCommandEnum(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isNoDeviceCmd() {
      switch (this.getOrdinal()) {
         case 1:
         case 8:
         case 18:
         case 19:
         case 30:
         case 31:
         case 32:
         case 34:
         case 35:
            return true;
         case 2:
         case 3:
         case 4:
         case 5:
         case 6:
         case 7:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 33:
         default:
            return false;
      }
   }

   public boolean isNoRouterCmd() {
      switch (this.getOrdinal()) {
         case 3:
         case 14:
         case 15:
         case 16:
         case 21:
         case 25:
         case 26:
         case 27:
         case 28:
            return true;
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 17:
         case 18:
         case 19:
         case 20:
         case 22:
         case 23:
         case 24:
         default:
            return false;
      }
   }

   public boolean isRouterOnlyCmd() {
      switch (this.getOrdinal()) {
         case 29:
            return true;
         default:
            return false;
      }
   }
}
