package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.RequestHeader;

public interface ServiceRequest<T extends ServiceResponse> extends Structure {
   RequestHeader getRequestHeader();

   void setRequestHeader(RequestHeader var1);
}
