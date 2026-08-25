package com.tridium.nre.security;

import com.tridium.nre.security.policy.NiagaraPermission;
import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class NiagaraBasicPermission extends Permission {
   private static final String[] NAMES_ARRAY = new String[]{
      "GET_BOOTSTRAP_CLASS_LOADER",
      "MANAGE_NIAGARA_POLICY",
      "VIEW_NIAGARA_POLICY",
      "GET_KEY_RING",
      "INIT_SEC_PROVIDERS",
      "GET_SECURITY_INFO_PROVIDER",
      "RUNTIME_EXEC",
      "SYSTEM_PASSWORD",
      "DISABLE_SECURITY_MANAGER",
      "DIAGNOSTIC_SECURITY_MANAGER",
      "GET_BOG_KEY",
      "TRANSCODE_KEY_RING",
      "RESET_KEYRING",
      "KEY_MATERIAL",
      "ROLL_KEY_MATERIAL",
      "SET_EXEMPTION_APPROVER",
      "FIND_MAIN_THREAD_GROUP",
      "LOAD_UNVERIFIED_MODULE",
      "SET_CONTEXT_KEY",
      "GET_CONTEXT_KEY",
      "SET_ENCODING_KEY",
      "NIAGARA_PERMISSIONS_MANAGER",
      "GET_MANAGER",
      "SET_SECURITY_AUDITOR",
      "ENTITLEMENT_WORKFLOW",
      "RESET_ENTITLEMENT",
      "GET_ENTITLEMENT_PRIVATE_KEY",
      "ROTATE_ENTITLEMENT_KEYS",
      "INIT_PLATFORM",
      "RENAME_AND_RESTART",
      "PLAT_GET_PROPS",
      "PLAT_GET_LOCAL_SESSION",
      "MODIFY_SESSION_IDS",
      "SET_SESSION_AUTHENTICATED",
      "GET_AUTHENTICATED_USER",
      "SET_FOX_SESSION_KEY",
      "SET_HTTP_SESSION_KEY",
      "UNAUTHENTICATED_SERVLET",
      "GET_PLATFORM_PROVIDER",
      "SET_TIME",
      "RESTORE_BACKUP",
      "CHANNEL_MARSHAL",
      "CLOUD_GET_CONNECTION_INFORMATION",
      "NIAGARA_SYSTEM_INDEX",
      "SYSTEM_DB_MODIFY",
      "MANAGE_SERVER_TRUST_ANCHORS",
      "MANAGE_ACE",
      "MQTT_ROLL_KEYS",
      "MANAGE_MAX_PRO",
      "CERT_MANAGEMENT",
      "GET_EMAIL_AUTHENTICATOR",
      "SIGNING_SERVICE",
      "MANAGE_AWS_ASSETS",
      "MANAGE_NAC"
   };
   private static final String WILDCARD = "*";
   private static final Set<String> NAMES = new HashSet<>();
   private final SortedSet<String> actions = new TreeSet<>();
   private final String actionsString;

   public NiagaraBasicPermission(String name) {
      super(name);
      if (!NAMES.contains(name)) {
         throw new IllegalArgumentException("Unrecognized name for NiagaraBasicPermission:" + name);
      }

      this.actionsString = "";
   }

   public NiagaraBasicPermission(String name, String actions) {
      super(name);
      if (!NAMES.contains(name)) {
         throw new IllegalArgumentException("Unrecognized name for NiagaraBasicPermission:" + name);
      }

      if (actions != null && !actions.isEmpty()) {
         for (String action : actions.split(",")) {
            this.actions.add(action.trim());
         }
      }

      this.actionsString = String.join(",", this.actions);
   }

   @Override
   public boolean implies(Permission permission) {
      if (!(permission instanceof NiagaraBasicPermission)) {
         return false;
      }

      if (!Objects.equals(this.getName(), permission.getName())) {
         return false;
      }

      if (this.actions.contains("*")) {
         return true;
      }

      NiagaraBasicPermission that = (NiagaraBasicPermission)permission;
      return this.actions.containsAll(that.actions);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         NiagaraBasicPermission that = (NiagaraBasicPermission)o;
         return Objects.equals(this.actions, that.actions) && Objects.equals(this.getName(), that.getName());
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.actions, this.getName());
   }

   @Override
   public String getActions() {
      return this.actionsString;
   }

   static {
      Collections.addAll(NAMES, NAMES_ARRAY);
   }

   public static class AllBasicPermissions extends NiagaraPermission {
      private final Set<Permission> impliedPermissions;

      public AllBasicPermissions() {
         super("AllBasicPermissions", "");
         Set<Permission> permissions = new HashSet<>();
         NiagaraBasicPermission.NAMES.stream().map(name -> new NiagaraBasicPermission(name, "*")).forEach(permissions::add);
         this.impliedPermissions = Collections.unmodifiableSet(permissions);
      }

      @Override
      public Set<Permission> getImpliedPermissions() {
         return new HashSet<>(this.impliedPermissions);
      }
   }
}
