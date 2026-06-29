package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryAliasResponse extends LonMessage implements NetMessages {
   public static final int QUERY_ALIAS_RESP_NETLEN = 4;
   BAliasConfigData aliasData = null;

   public QueryAliasResponse() {
   }

   public QueryAliasResponse(LonInputStream in) throws LonException {
      this.code = 40;
      this.fromInputStream(in);
   }

   public BAliasConfigData getAliasConfigData() {
      return this.aliasData;
   }

   public void setAliasConfigData(BAliasConfigData aliasData) {
      this.aliasData = aliasData;
   }

   public boolean isExtended() {
      return false;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      this.aliasData.writeNetworkBytes(out);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      in.readUnsigned8();
      this.aliasData = new BAliasConfigData();
      this.aliasData.fromInputStream(in);
   }
}
