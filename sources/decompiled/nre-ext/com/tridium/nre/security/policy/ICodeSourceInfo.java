package com.tridium.nre.security.policy;

public interface ICodeSourceInfo {
   String getUrl();

   String getName();

   boolean isSigned();
}
