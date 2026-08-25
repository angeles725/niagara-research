package com.tridium.nre.security.policy;

import java.security.Permission;
import java.util.Set;

public interface PermissionMapper {
   boolean appliesTo(String var1);

   Set<Permission> get(String var1);
}
