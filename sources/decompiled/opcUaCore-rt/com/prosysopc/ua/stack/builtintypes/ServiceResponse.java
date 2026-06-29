package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.ResponseHeader;

public interface ServiceResponse extends Structure {
   ResponseHeader getResponseHeader();

   void setResponseHeader(ResponseHeader var1);
}
