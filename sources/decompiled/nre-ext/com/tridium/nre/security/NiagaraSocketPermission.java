package com.tridium.nre.security;

import com.tridium.nre.security.policy.NiagaraPermission;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class NiagaraSocketPermission extends NiagaraPermission {
   static final Set<Class<? extends Permission>> permissionClasses = new HashSet<>();
   private static final boolean NO_DNS_LOOKUP = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.socketPermission.noDns"));
   private static final String FULL_WILDCARD = "*";
   private static final String PARTIAL_WILDCARD_START = "*.";
   private static final Pattern IPv4PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
   private final SocketPermission socketPermission;
   private final EnumSet<NiagaraSocketPermission.Action> mask;
   private InetAddress[] addresses = null;
   private NiagaraSocketPermission.AddressType addressType;
   private NiagaraSocketPermission.PortRange portRange;
   private String host;
   private String fqdn;
   private boolean resolved = false;

   public NiagaraSocketPermission(SocketPermission socketPermission) {
      super(socketPermission.getName(), socketPermission.getActions());
      this.socketPermission = socketPermission;
      this.mask = getMask(socketPermission.getActions());
      this.parseHost();
   }

   public NiagaraSocketPermission(String name, String actions) {
      super(name, actions);
      this.socketPermission = new SocketPermission(name, actions);
      this.mask = getMask(this.socketPermission.getActions());
      this.parseHost();
   }

   private void parseHost() {
      String name = this.socketPermission.getName();
      int index = name.lastIndexOf(":");
      if (name.charAt(0) == '[') {
         this.host = name.substring(1, name.indexOf("]"));
         this.portRange = index < 0 ? NiagaraSocketPermission.PortRange.ALL : new NiagaraSocketPermission.PortRange(name.substring(index + 1));
         this.addressType = NiagaraSocketPermission.AddressType.IPV6;
      } else if (index < 0) {
         this.host = name;
         this.portRange = NiagaraSocketPermission.PortRange.ALL;
         this.addressType = IPv4PATTERN.matcher(this.host).matches() ? NiagaraSocketPermission.AddressType.IPV4 : NiagaraSocketPermission.AddressType.HOSTNAME;
      } else {
         this.host = name.substring(0, index);
         this.portRange = new NiagaraSocketPermission.PortRange(name.substring(index + 1));
         this.addressType = IPv4PATTERN.matcher(this.host).matches() ? NiagaraSocketPermission.AddressType.IPV4 : NiagaraSocketPermission.AddressType.HOSTNAME;
      }

      try {
         if (this.addressType != NiagaraSocketPermission.AddressType.HOSTNAME) {
            this.addresses = new InetAddress[]{InetAddress.getByName(this.host)};
         }
      } catch (UnknownHostException var4) {
      }
   }

   private void resolve() {
      if (!this.resolved) {
         try {
            if (this.addressType == NiagaraSocketPermission.AddressType.HOSTNAME) {
               this.addresses = InetAddress.getAllByName(this.host);
            }

            this.fqdn = this.addresses[0].getCanonicalHostName();
         } catch (UnknownHostException var2) {
         }

         this.resolved = true;
      }
   }

   @Override
   public Set<Class<? extends Permission>> getSupportedClasses() {
      return permissionClasses;
   }

   @Override
   public boolean implies(Permission permission) {
      if (!(permission instanceof SocketPermission)) {
         return false;
      } else if (!NO_DNS_LOOKUP) {
         return this.socketPermission.implies(permission);
      } else {
         return !this.mask.containsAll(getMask(permission.getActions())) ? false : this.impliesIgnoreMask((SocketPermission)permission);
      }
   }

   @Override
   public String getActions() {
      return this.socketPermission.getActions();
   }

   @Override
   public PermissionCollection newPermissionCollection() {
      return new NiagaraSocketPermission.NiagaraSocketPermissionCollection();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         NiagaraSocketPermission that = (NiagaraSocketPermission)o;
         if (!NO_DNS_LOOKUP) {
            return this.socketPermission.equals(that.socketPermission);
         }

         if (this.mask != null ? this.mask.equals(that.mask) : that.mask == null) {
            if (this.addressType != that.addressType) {
               return false;
            } else if (this.portRange != null ? this.portRange.equals(that.portRange) : that.portRange == null) {
               return this.host != null ? this.host.equals(that.host) : that.host == null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      if (!NO_DNS_LOOKUP) {
         return this.socketPermission.hashCode();
      }

      int result = this.mask != null ? this.mask.hashCode() : 0;
      result = 31 * result + (this.addressType != null ? this.addressType.hashCode() : 0);
      result = 31 * result + (this.portRange != null ? this.portRange.hashCode() : 0);
      return 31 * result + (this.host != null ? this.host.hashCode() : 0);
   }

   private boolean impliesIgnoreMask(SocketPermission permission) {
      if (!NO_DNS_LOOKUP) {
         return this.socketPermission.implies(permission);
      }

      NiagaraSocketPermission that = new NiagaraSocketPermission(permission);
      if (!this.portRange.contains(that.portRange)) {
         return false;
      }

      if (this.host.equals("*")) {
         return true;
      }

      if (that.host.equals("*")) {
         return false;
      }

      if (this.host.startsWith("*.")) {
         String thatHostEnd = that.host.startsWith("*.") ? that.host.substring(1) : that.host;
         return thatHostEnd.substring(1).endsWith(this.host.substring(1));
      }

      if (!this.resolved) {
         this.resolve();
      }

      switch (that.addressType) {
         case HOSTNAME:
            return this.host.equals(that.host) || this.fqdn.equals(that.host);
         case IPV4:
         case IPV6:
         default:
            for (InetAddress ipAddress : this.addresses) {
               if (ipAddress.getHostAddress().equals(that.host)) {
                  return true;
               }
            }

            return false;
      }
   }

   private static EnumSet<NiagaraSocketPermission.Action> getMask(String actionString) {
      if (actionString != null && !"".equals(actionString)) {
         EnumSet<NiagaraSocketPermission.Action> actionSet = EnumSet.noneOf(NiagaraSocketPermission.Action.class);
         String[] actions = actionString.split(",");

         for (String action : actions) {
            actionSet.add(NiagaraSocketPermission.Action.valueOf(action.trim().toUpperCase(Locale.ENGLISH)));
         }

         return actionSet;
      } else {
         throw new IllegalArgumentException("Parameter <action> must not be empty or null.");
      }
   }

   static {
      permissionClasses.add(SocketPermission.class);
   }

   private enum Action {
      CONNECT,
      LISTEN,
      ACCEPT,
      RESOLVE;
   }

   private enum AddressType {
      HOSTNAME,
      IPV4,
      IPV6;
   }

   static final class NiagaraSocketPermissionCollection extends PermissionCollection {
      private List<Permission> permissions = new ArrayList<>();

      public NiagaraSocketPermissionCollection() {
      }

      @Override
      public void add(Permission permission) {
         if (!(permission instanceof NiagaraSocketPermission)) {
            throw new IllegalArgumentException(
               "Attempting to add invalid Permission type <" + permission.getClass().getName() + "> to NiagaraSocketPermissionCollection"
            );
         }

         synchronized (this) {
            this.permissions.add(permission);
         }
      }

      @Override
      public boolean implies(Permission permission) {
         if (!(permission instanceof SocketPermission)) {
            return false;
         }

         SocketPermission that = (SocketPermission)permission;
         EnumSet<NiagaraSocketPermission.Action> requiredActions = NiagaraSocketPermission.getMask(that.getActions());
         EnumSet<NiagaraSocketPermission.Action> missingActions = requiredActions.clone();
         EnumSet<NiagaraSocketPermission.Action> foundActions = EnumSet.noneOf(NiagaraSocketPermission.Action.class);
         synchronized (this) {
            for (Permission perm : this.permissions) {
               NiagaraSocketPermission niagaraSocketPermission = (NiagaraSocketPermission)perm;
               EnumSet<NiagaraSocketPermission.Action> potentialActions = missingActions.clone();
               potentialActions.retainAll(niagaraSocketPermission.mask);
               if (!potentialActions.isEmpty() && niagaraSocketPermission.impliesIgnoreMask(that)) {
                  foundActions.addAll(niagaraSocketPermission.mask);
                  if (foundActions.containsAll(requiredActions)) {
                     return true;
                  }

                  missingActions.removeAll(niagaraSocketPermission.mask);
               }
            }

            return false;
         }
      }

      @Override
      public Enumeration<Permission> elements() {
         synchronized (this) {
            return Collections.enumeration(this.permissions);
         }
      }
   }

   private static class PortRange {
      int low;
      int high;
      private static final int PORT_MIN = 0;
      private static final int PORT_MAX = Integer.MAX_VALUE;
      public static NiagaraSocketPermission.PortRange ALL = new NiagaraSocketPermission.PortRange(null);

      PortRange(String portRange) {
         if (portRange != null && !portRange.equals("") && !portRange.equals("*")) {
            String[] ports = portRange.split("-");
            this.low = Integer.parseInt(ports[0]);
            this.high = ports.length > 1 ? Integer.parseInt(ports[1]) : this.low;
         } else {
            this.low = 0;
            this.high = Integer.MAX_VALUE;
         }
      }

      boolean contains(NiagaraSocketPermission.PortRange ports) {
         return ports.low >= this.low && ports.high <= this.high;
      }
   }
}
