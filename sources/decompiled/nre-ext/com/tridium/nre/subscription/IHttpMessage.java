package com.tridium.nre.subscription;

import java.util.Map;

public interface IHttpMessage {
   int getLength();

   byte[] getPayload();

   Map<String, Object> getMetadata();
}
