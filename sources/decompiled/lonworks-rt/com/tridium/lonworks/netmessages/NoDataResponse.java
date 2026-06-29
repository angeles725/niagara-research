package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.LonMessage;

public class NoDataResponse extends LonMessage implements NetMessages {
   public NoDataResponse(int code) {
      this.code = code;
   }
}
