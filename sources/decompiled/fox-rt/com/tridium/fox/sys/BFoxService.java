package com.tridium.fox.sys;

import com.tridium.crypto.core.cert.KeyPurpose;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.ServerCertificateHealth;
import com.tridium.crypto.core.io.TrustAnchorConsumer;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxConnection;
import com.tridium.fox.session.FoxServer;
import com.tridium.fox.session.FoxSession;
import com.tridium.nre.firewall.IpProtocol;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.nre.security.SigningPasswordPermission;
import com.tridium.security.BISecurityInfoSource;
import com.tridium.security.BISecurityService;
import com.tridium.security.BSecurityInfo;
import com.tridium.security.BServerCertificateHealth;
import com.tridium.sys.Nre;
import com.tridium.sys.NreLib;
import com.tridium.sys.engine.NClockTicket;
import com.tridium.sys.license.Brand;
import com.tridium.sys.service.BServiceEvent;
import com.tridium.sys.service.ServiceListener;
import com.tridium.sys.service.ServiceManager;
import com.tridium.util.ArrayUtil;
import com.tridium.util.CertAliasCasePropertyValidator;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.firewall.BServerPort;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.security.ServerTlsParameters;
import javax.baja.nre.security.TlsCipherSuiteGroup;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BCertificateAliasAndPassword;
import javax.baja.security.BICertificateAliasAndPasswordContainer;
import javax.baja.security.BPassword;
import javax.baja.security.crypto.BSslTlsEnum;
import javax.baja.security.crypto.BTlsCipherSuiteGroup;
import javax.baja.security.crypto.ICryptoManager;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIService;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.IPropertyValidator;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BIRestrictedComponent;
import javax.net.ServerSocketFactory;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "foxPort",
      type = "BServerPort",
      defaultValue = "new BServerPort(1911, IpProtocol.TCP)"
   ), @NiagaraProperty(
      name = "foxEnabled",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "foxsPort",
      type = "BServerPort",
      defaultValue = "new BServerPort(4911, IpProtocol.TCP)"
   ), @NiagaraProperty(
      name = "foxsEnabled",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "foxsOnly",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "foxsMinProtocol",
      type = "BSslTlsEnum",
      defaultValue = "BSslTlsEnum.DEFAULT",
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "cipherSuiteGroup",
      type = "BTlsCipherSuiteGroup",
      defaultValue = "BTlsCipherSuiteGroup.recommended",
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "foxsCert",
      type = "String",
      defaultValue = "CertUtils.LEGACY_CERT_ALIAS",
      facets = {@Facet("BFacets.make(BFacets.FIELD_EDITOR, BString.make(\"workbench:CertificateAliasFE\"))"), @Facet("BFacets.make(BFacets.UX_FIELD_EDITOR, BString.make(\"webEditors:CertificateAliasEditor\"))"), @Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)"), @Facet("BFacets.make(\"purposeId\", \"SERVER_CERT\")")},
      deprecated = true
   ), @NiagaraProperty(
      name = "certAliasAndPassword",
      type = "BCertificateAliasAndPassword",
      defaultValue = "BCertificateAliasAndPassword.DEFAULT",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "serverCertificateHealth",
      type = "BServerCertificateHealth",
      defaultValue = "new BServerCertificateHealth()",
      flags = 7
   ), @NiagaraProperty(
      name = "requestTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.make(Fox.requestTimeout)",
      facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1))")}
   ), @NiagaraProperty(
      name = "socketOptionTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.make(Fox.soTimeout)",
      facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1))")}
   ), @NiagaraProperty(
      name = "socketTcpNoDelay",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "keepAliveInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.make(Fox.keepAliveInterval)"
   ), @NiagaraProperty(
      name = "maxServerSessions",
      type = "int",
      defaultValue = "Fox.maxServerSessions"
   ), @NiagaraProperty(
      name = "multicastEnabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "enableAnnouncement",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "multicastTimeToLive",
      type = "int",
      defaultValue = "Fox.multicastTimeToLive"
   ), @NiagaraProperty(
      name = "serverConnections",
      type = "BServerConnections",
      defaultValue = "new BServerConnections()"
   ), @NiagaraProperty(
      name = "traceSessionStates",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "traceReadFrame",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "traceWriteFrame",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "traceMulticast",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "auditStationLoginEvents",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "supportLegacyClients",
      type = "BDynamicEnum",
      defaultValue = "BDynamicEnum.make(0, GENERIC_SUPPORT_LEGACY_CLIENTS_RANGE)",
      facets = {@Facet("BFacets.make(BFacets.FIELD_EDITOR, BString.make(\"workbench:FrozenEnumFE\"), BFacets.UX_FIELD_EDITOR, BString.make(\"webEditors:FrozenEnumEditor\"), BFacets.SECURITY, BBoolean.TRUE)")}
   )})
@NiagaraActions({@NiagaraAction(
      name = "resetAllConnections",
      flags = 128
   ), @NiagaraAction(
      name = "delayStationStarted",
      flags = 4
   )})
public class BFoxService
   extends BComponent
   implements BIService,
   BIRestrictedComponent,
   BISecurityInfoSource,
   TrustAnchorConsumer,
   BICertificateAliasAndPasswordContainer {
   public static final boolean SUPPORT_LEGACY_CLIENTS_BY_DEFAULT = true;
   private static final BFacets LEXICON_FACET = BFacets.make("lexicon", "fox");
   private static final BEnumRange GENERIC_SUPPORT_LEGACY_CLIENTS_RANGE = BEnumRange.make(
      null,
      new int[]{0, 1, 2},
      new String[]{SlotPath.escape("Use Default On Installed Platform"), SlotPath.escape("No (Recommended)"), SlotPath.escape("Yes")},
      3,
      LEXICON_FACET
   );
   private static final BEnumRange LOCAL_SUPPORT_LEGACY_CLIENTS_RANGE = BEnumRange.make(
      null,
      new int[]{0, 1, 2},
      new String[]{SlotPath.escape("Default: Yes (may change in future version)"), SlotPath.escape("No (Recommended)"), SlotPath.escape("Yes")},
      3,
      LEXICON_FACET
   );
   public static final Property foxPort = newProperty(0, new BServerPort(1911, IpProtocol.TCP), null);
   public static final Property foxEnabled = newProperty(0, true, BFacets.make("security", BBoolean.TRUE));
   public static final Property foxsPort = newProperty(0, new BServerPort(4911, IpProtocol.TCP), null);
   public static final Property foxsEnabled = newProperty(0, false, BFacets.make("security", BBoolean.TRUE));
   public static final Property foxsOnly = newProperty(0, false, BFacets.make("security", BBoolean.TRUE));
   public static final Property foxsMinProtocol = newProperty(0, BSslTlsEnum.DEFAULT, BFacets.make("security", BBoolean.TRUE));
   public static final Property cipherSuiteGroup = newProperty(0, BTlsCipherSuiteGroup.recommended, BFacets.make("security", BBoolean.TRUE));
   @Deprecated
   public static final Property foxsCert = newProperty(
      0,
      "tridium",
      BFacets.make(
         BFacets.make(
            BFacets.make(
               BFacets.make("fieldEditor", BString.make("workbench:CertificateAliasFE")),
               BFacets.make("uxFieldEditor", BString.make("webEditors:CertificateAliasEditor"))
            ),
            BFacets.make("security", BBoolean.TRUE)
         ),
         BFacets.make("purposeId", "SERVER_CERT")
      )
   );
   public static final Property certAliasAndPassword = newProperty(4, BCertificateAliasAndPassword.DEFAULT, BFacets.make("security", BBoolean.TRUE));
   public static final Property serverCertificateHealth = newProperty(7, new BServerCertificateHealth(), null);
   public static final Property requestTimeout = newProperty(0, BRelTime.make(Fox.requestTimeout), BFacets.make("min", BRelTime.make(1L)));
   public static final Property socketOptionTimeout = newProperty(0, BRelTime.make(Fox.soTimeout), BFacets.make("min", BRelTime.make(1L)));
   public static final Property socketTcpNoDelay = newProperty(0, true, null);
   public static final Property keepAliveInterval = newProperty(0, BRelTime.make(Fox.keepAliveInterval), null);
   public static final Property maxServerSessions = newProperty(0, Fox.maxServerSessions, null);
   public static final Property multicastEnabled = newProperty(0, true, null);
   public static final Property enableAnnouncement = newProperty(0, true, null);
   public static final Property multicastTimeToLive = newProperty(0, Fox.multicastTimeToLive, null);
   public static final Property serverConnections = newProperty(0, new BServerConnections(), null);
   public static final Property traceSessionStates = newProperty(0, false, null);
   public static final Property traceReadFrame = newProperty(0, false, null);
   public static final Property traceWriteFrame = newProperty(0, false, null);
   public static final Property traceMulticast = newProperty(0, false, null);
   public static final Property auditStationLoginEvents = newProperty(0, false, null);
   public static final Property supportLegacyClients = newProperty(
      0,
      BDynamicEnum.make(0, GENERIC_SUPPORT_LEGACY_CLIENTS_RANGE),
      BFacets.make(
         "fieldEditor", BString.make("workbench:FrozenEnumFE"), "uxFieldEditor", BString.make("webEditors:FrozenEnumEditor"), "security", BBoolean.TRUE
      )
   );
   public static final Action resetAllConnections = newAction(128, null);
   public static final Action delayStationStarted = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BFoxService.class);
   private static final Type[] serviceTypes = new Type[]{TYPE};
   private final ServiceListener serviceListener = new ServiceListener() {
      public void serviceEvent(BServiceEvent event) {
         if (event.getServiceType().is(BISecurityService.TYPE) && event.getId() == 0) {
            ((BISecurityService)event.getService().as(BISecurityService.class)).register(BFoxService.this);
         }
      }
   };
   public static final Logger LOG = Logger.getLogger("fox");
   private static final BIcon ICON = BIcon.std("fox.png");
   private static final double MILLIS_IN_NINETY_DAYS = BRelTime.makeDays(90).getMillis();
   private final List<BFoxService.FoxServerConnectionListener> serverConnectionListeners = new ArrayList<>();
   private BFoxService.Daemon daemon;
   private Ticket restartTicket;
   private String trustAnchorConsumerId;
   private CertAliasCasePropertyValidator validator = new CertAliasCasePropertyValidator(certAliasAndPassword.getName());

   public BServerPort getFoxPort() {
      return (BServerPort)this.get(foxPort);
   }

   public void setFoxPort(BServerPort v) {
      this.set(foxPort, v, null);
   }

   public boolean getFoxEnabled() {
      return this.getBoolean(foxEnabled);
   }

   public void setFoxEnabled(boolean v) {
      this.setBoolean(foxEnabled, v, null);
   }

   public BServerPort getFoxsPort() {
      return (BServerPort)this.get(foxsPort);
   }

   public void setFoxsPort(BServerPort v) {
      this.set(foxsPort, v, null);
   }

   public boolean getFoxsEnabled() {
      return this.getBoolean(foxsEnabled);
   }

   public void setFoxsEnabled(boolean v) {
      this.setBoolean(foxsEnabled, v, null);
   }

   public boolean getFoxsOnly() {
      return this.getBoolean(foxsOnly);
   }

   public void setFoxsOnly(boolean v) {
      this.setBoolean(foxsOnly, v, null);
   }

   public BSslTlsEnum getFoxsMinProtocol() {
      return (BSslTlsEnum)this.get(foxsMinProtocol);
   }

   public void setFoxsMinProtocol(BSslTlsEnum v) {
      this.set(foxsMinProtocol, v, null);
   }

   public BTlsCipherSuiteGroup getCipherSuiteGroup() {
      return (BTlsCipherSuiteGroup)this.get(cipherSuiteGroup);
   }

   public void setCipherSuiteGroup(BTlsCipherSuiteGroup v) {
      this.set(cipherSuiteGroup, v, null);
   }

   @Deprecated
   public String getFoxsCert() {
      return this.getString(foxsCert);
   }

   @Deprecated
   public void setFoxsCert(String v) {
      this.setString(foxsCert, v, null);
   }

   public BCertificateAliasAndPassword getCertAliasAndPassword() {
      return (BCertificateAliasAndPassword)this.get(certAliasAndPassword);
   }

   public void setCertAliasAndPassword(BCertificateAliasAndPassword v) {
      this.set(certAliasAndPassword, v, null);
   }

   public BServerCertificateHealth getServerCertificateHealth() {
      return (BServerCertificateHealth)this.get(serverCertificateHealth);
   }

   public void setServerCertificateHealth(BServerCertificateHealth v) {
      this.set(serverCertificateHealth, v, null);
   }

   public BRelTime getRequestTimeout() {
      return (BRelTime)this.get(requestTimeout);
   }

   public void setRequestTimeout(BRelTime v) {
      this.set(requestTimeout, v, null);
   }

   public BRelTime getSocketOptionTimeout() {
      return (BRelTime)this.get(socketOptionTimeout);
   }

   public void setSocketOptionTimeout(BRelTime v) {
      this.set(socketOptionTimeout, v, null);
   }

   public boolean getSocketTcpNoDelay() {
      return this.getBoolean(socketTcpNoDelay);
   }

   public void setSocketTcpNoDelay(boolean v) {
      this.setBoolean(socketTcpNoDelay, v, null);
   }

   public BRelTime getKeepAliveInterval() {
      return (BRelTime)this.get(keepAliveInterval);
   }

   public void setKeepAliveInterval(BRelTime v) {
      this.set(keepAliveInterval, v, null);
   }

   public int getMaxServerSessions() {
      return this.getInt(maxServerSessions);
   }

   public void setMaxServerSessions(int v) {
      this.setInt(maxServerSessions, v, null);
   }

   public boolean getMulticastEnabled() {
      return this.getBoolean(multicastEnabled);
   }

   public void setMulticastEnabled(boolean v) {
      this.setBoolean(multicastEnabled, v, null);
   }

   public boolean getEnableAnnouncement() {
      return this.getBoolean(enableAnnouncement);
   }

   public void setEnableAnnouncement(boolean v) {
      this.setBoolean(enableAnnouncement, v, null);
   }

   public int getMulticastTimeToLive() {
      return this.getInt(multicastTimeToLive);
   }

   public void setMulticastTimeToLive(int v) {
      this.setInt(multicastTimeToLive, v, null);
   }

   public BServerConnections getServerConnections() {
      return (BServerConnections)this.get(serverConnections);
   }

   public void setServerConnections(BServerConnections v) {
      this.set(serverConnections, v, null);
   }

   public boolean getTraceSessionStates() {
      return this.getBoolean(traceSessionStates);
   }

   public void setTraceSessionStates(boolean v) {
      this.setBoolean(traceSessionStates, v, null);
   }

   public boolean getTraceReadFrame() {
      return this.getBoolean(traceReadFrame);
   }

   public void setTraceReadFrame(boolean v) {
      this.setBoolean(traceReadFrame, v, null);
   }

   public boolean getTraceWriteFrame() {
      return this.getBoolean(traceWriteFrame);
   }

   public void setTraceWriteFrame(boolean v) {
      this.setBoolean(traceWriteFrame, v, null);
   }

   public boolean getTraceMulticast() {
      return this.getBoolean(traceMulticast);
   }

   public void setTraceMulticast(boolean v) {
      this.setBoolean(traceMulticast, v, null);
   }

   public boolean getAuditStationLoginEvents() {
      return this.getBoolean(auditStationLoginEvents);
   }

   public void setAuditStationLoginEvents(boolean v) {
      this.setBoolean(auditStationLoginEvents, v, null);
   }

   public BDynamicEnum getSupportLegacyClients() {
      return (BDynamicEnum)this.get(supportLegacyClients);
   }

   public void setSupportLegacyClients(BDynamicEnum v) {
      this.set(supportLegacyClients, v, null);
   }

   public void resetAllConnections() {
      this.invoke(resetAllConnections, null, null);
   }

   public void delayStationStarted() {
      this.invoke(delayStationStarted, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type[] getServiceTypes() {
      return serviceTypes;
   }

   public boolean isServing() {
      BFoxService.Daemon daemon = this.daemon;
      return daemon != null && daemon.isServing();
   }

   public final Object fw(int x, Object a, Object b, Object c, Object d) {
      switch (x) {
         case 11:
            this.fwStarted();
         default:
            return super.fw(x, a, b, c, d);
      }
   }

   private void fwStarted() {
      if (!Flags.has(this, foxsCert, 268435456)) {
         this.getCertAliasAndPassword().setAlias(this.getFoxsCert());
         this.getCertAliasAndPassword().setPassword(BPassword.DEFAULT);
         Flags.add(this, foxsCert, null, new int[]{268435461});
      }

      if (!Flags.has(this, certAliasAndPassword, 268435456)) {
         Flags.add(this, certAliasAndPassword, null, new int[]{268435456});
         this.setFlags(certAliasAndPassword, this.getFlags(certAliasAndPassword) & -5);
         this.setFlags(serverCertificateHealth, this.getFlags(serverCertificateHealth) & -5);
         this.getCertAliasAndPassword().setFacets(BCertificateAliasAndPassword.alias, BFacets.make("purposeId", KeyPurpose.SERVER_CERT.name()));
      }
   }

   public void stationStarted() {
      BServerPort tfoxPort = null;
      BServerPort tfoxsPort = null;

      try {
         List<String> keys = new ArrayList<>();
         List<String> values = new ArrayList<>();
         this.serviceStopped();
         this.initOptions();
         if (this.getFoxEnabled()) {
            tfoxPort = this.getFoxPort();
         }

         if (this.getFoxsEnabled()) {
            tfoxsPort = this.getFoxsPort();
         }

         this.daemon = new BFoxService.Daemon(this, tfoxPort, tfoxsPort);
         this.daemon.run();
         keys.add("foxport");
         if (tfoxPort != null) {
            values.add(String.valueOf(tfoxPort.getPublicServerPort()));
         } else {
            values.add("-1");
         }

         keys.add("foxsport");
         if (tfoxsPort != null) {
            values.add(String.valueOf(tfoxsPort.getPublicServerPort()));
         } else {
            values.add("-1");
         }

         Nre.getPlatform().reportSummaryFields(keys.toArray(new String[0]), values.toArray(new String[0]));
      } catch (Exception var5) {
         LOG.log(Level.SEVERE, "Cannot start", (Throwable)var5);
      }

      Optional<BIService> securityService = Sys.findService(BISecurityService.TYPE);
      if (securityService.isPresent()) {
         ((BISecurityService)securityService.get().as(BISecurityService.class)).register(this);
      } else {
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            Nre.getServiceManager().addServiceListener(this.serviceListener);
            return null;
         }));
      }

      this.trustAnchorConsumerId = AccessController.doPrivileged((PrivilegedAction<String>)(() -> CoreCryptoManager.get().registerTrustAnchorConsumer(this)));
   }

   public void serviceStarted() {
      try {
         AccessController.doPrivileged((PrivilegedAction<ServiceManager>)(() -> Nre.getServiceManager())).getService("platCrypto:CertManagerService");
         this.setFlags(foxsPort, this.getFlags(foxsPort) & -6);
         this.setFlags(foxsEnabled, this.getFlags(foxsEnabled) & -6);
         this.setFlags(foxsOnly, this.getFlags(foxsOnly) & -6);
         this.setFlags(foxsMinProtocol, this.getFlags(foxsMinProtocol) & -6);
      } catch (Exception var5) {
         this.setFlags(foxsPort, this.getFlags(foxsPort) | 1 | 4);
         this.setFoxsEnabled(false);
         this.setFlags(foxsEnabled, this.getFlags(foxsEnabled) | 1 | 4);
         this.setFoxsOnly(false);
         this.setFlags(foxsOnly, this.getFlags(foxsOnly) | 1 | 4);
         this.setFlags(foxsMinProtocol, this.getFlags(foxsMinProtocol) | 1 | 4);
      } finally {
         this.setSupportLegacyClients(BDynamicEnum.make(this.getSupportLegacyClients().getOrdinal(), LOCAL_SUPPORT_LEGACY_CLIENTS_RANGE));
      }
   }

   public void serviceStopped() {
      if (this.daemon != null) {
         LOG.info("Service stopped");
         this.daemon.stop();
      }

      this.daemon = null;
      Sys.findService(BISecurityService.TYPE).ifPresent(s -> ((BISecurityService)s.as(BISecurityService.class)).unregister(this));
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         Nre.getServiceManager().removeServiceListener(this.serviceListener);
         return null;
      }));
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         CoreCryptoManager.get().unregisterTrustAnchorConsumer(this.trustAnchorConsumerId);
         return null;
      }));
   }

   private void initOptions() {
      Fox.appName = "Station";
      Fox.appVersion = String.valueOf(Sys.getBajaVersion());
      Fox.hostName = Sys.getHostName();
      Fox.hostAddress = Sys.getLocalHost(null).getHostAddress();
      Fox.ipv4Enabled = !NreLib.getLocalHost(false).isLoopbackAddress();
      Fox.ipv6Enabled = Fox.ipv6Enabled && !NreLib.getLocalHost(true).isLoopbackAddress();
      Fox.requestTimeout = (int)this.getRequestTimeout().getMillis();
      Fox.keepAliveInterval = (int)this.getKeepAliveInterval().getMillis();
      Fox.soTimeout = (int)this.getSocketOptionTimeout().getMillis();
      Fox.tcpNoDelay = this.getSocketTcpNoDelay();
      Fox.multicastEnabled = this.getMulticastEnabled();
      Fox.multicastTimeToLive = this.getMulticastTimeToLive();
      Fox.maxServerSessions = this.getMaxServerSessions();
      Fox.traceSessionStates = this.getTraceSessionStates();
      Fox.traceReadFrame = this.getTraceReadFrame();
      Fox.traceWriteFrame = this.getTraceWriteFrame();
      Fox.traceMulticast = this.getTraceMulticast();
   }

   public static BFoxService waitUntilPortOpen(long timeout) {
      try {
         BFoxService service = (BFoxService)Sys.getService(TYPE);
         long start = Clock.ticks();

         while (!service.isServing()) {
            if (Clock.ticks() - start > timeout) {
               throw new BajaRuntimeException("BFoxService.waitUntilPortOpen timed out");
            }

            Thread.sleep(100L);
         }

         return service;
      } catch (BajaRuntimeException var5) {
         throw var5;
      } catch (Exception var6) {
         System.out.println("ERROR: BFoxService.waitUntilPortOpen failed");
         var6.printStackTrace();
         throw new BajaRuntimeException("FoxService.waitUntilPortOpen", var6);
      }
   }

   public final void checkParentForRestrictedComponent(BComponent parent, Context cx) {
      BIRestrictedComponent.checkParentForRestrictedComponent(parent, this);
   }

   public BSecurityInfo getSecurityInfo() {
      BSecurityInfo info = new BSecurityInfo();
      info.setSourceName(this.getDisplayName(null));
      if (this.isMounted()) {
         info.setHyperlink(this.getSlotPathOrd());
      }

      info.add("alias", BString.make(this.getCertAliasAndPassword().getAlias()));
      return info;
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      if (prop != serverCertificateHealth) {
         if (this.isRunning()) {
            if (prop == foxPort || prop == foxsPort) {
               this.triggerDelayedStationStarted();
            } else if (prop != certAliasAndPassword && prop != foxsMinProtocol && prop != foxsOnly && prop != cipherSuiteGroup) {
               if (prop != foxEnabled && prop != foxsEnabled && prop != multicastEnabled) {
                  this.initOptions();
               } else {
                  this.triggerDelayedStationStarted();
               }
            } else if (this.getFoxsEnabled()) {
               this.triggerDelayedStationStarted();
            }
         }
      }
   }

   private synchronized void triggerDelayedStationStarted() {
      this.triggerDelayedStationStarted(500L);
   }

   private synchronized void triggerDelayedStationStarted(long waitMillis) {
      if (!this.isRunning()) {
         if (this.restartTicket != null) {
            this.restartTicket.cancel();
            this.restartTicket = null;
         }
      } else {
         this.setServerCertificateHealth(new BServerCertificateHealth());
         if (this.restartTicket == null) {
            this.restartTicket = Clock.schedule(this, BRelTime.make(waitMillis), delayStationStarted, null);
         }

         if (this.restartTicket instanceof NClockTicket && waitMillis < ((NClockTicket)this.restartTicket).millisLeft()) {
            this.restartTicket.cancel();
            this.restartTicket = Clock.schedule(this, BRelTime.make(waitMillis), delayStationStarted, null);
         }
      }
   }

   public void doResetAllConnections() {
      (new Thread() {
         @Override
         public void run() {
            for (BFoxServerConnection connection : (BFoxServerConnection[])BFoxService.this.getServerConnections().getChildren(BFoxServerConnection.class)) {
               try {
                  connection.forceDisconnect();
               } catch (Exception var6) {
                  Logger.getLogger("fox").log(Level.WARNING, "Error resetting connections", (Throwable)var6);
               }
            }
         }
      }).start();
   }

   public void doDelayStationStarted() {
      synchronized (this) {
         this.restartTicket = null;
      }

      this.stationStarted();
   }

   public void updateTrustAnchors() {
      this.triggerDelayedStationStarted(120000L);
   }

   public BFoxServerConnection makePersistentServerConnection(String name) {
      BFoxServerConnection conn = (BFoxServerConnection)this.getServerConnections().get(name);
      if (conn != null && conn.isPersistent()) {
         this.getServerConnections().remove(name);
         conn = null;
      }

      if (conn == null) {
         conn = new BFoxServerConnection();
         this.getServerConnections().add(name, conn, 3);
      }

      conn.setPersistent(true);
      return conn;
   }

   public BFoxServerConnection makeServerConnection(FoxSession session, FoxMessage remoteHello) throws Exception {
      BFoxServerConnection conn = null;

      for (BFoxService.FoxServerConnectionListener listener : this.serverConnectionListeners) {
         conn = listener.getPersistentConnection(session, remoteHello);
         if (conn != null) {
            break;
         }
      }

      if (conn == null) {
         String stationName = remoteHello.getString("station.name", null);
         if (stationName != null) {
            String serverConnName = "Station_" + stationName;
            BFoxServerConnection existingServerConn = (BFoxServerConnection)this.getServerConnections().get(serverConnName);
            if (existingServerConn != null && !existingServerConn.isPersistent()) {
               conn = existingServerConn;
            } else {
               conn = new BFoxServerConnection();
            }
         } else {
            conn = new BFoxServerConnection();
         }
      }

      for (BFoxService.FoxServerConnectionListener listenerx : this.serverConnectionListeners) {
         listenerx.serverConnectionCreated(conn, session, remoteHello);
      }

      return conn;
   }

   public void registerServerConnectionListener(BFoxService.FoxServerConnectionListener listener) {
      this.serverConnectionListeners.add(listener);
   }

   public void unregisterServerConnectionListener(BFoxService.FoxServerConnectionListener listener) {
      this.serverConnectionListeners.remove(listener);
   }

   public void notifyServerConnectionClosed(BFoxServerConnection connection, Throwable cause) {
      this.serverConnectionListeners.forEach(listener -> {
         try {
            listener.serverConnectionClosed(connection, cause);
         } catch (Throwable var4) {
            Logger.getLogger("fox").log(Level.SEVERE, "Error notifying connection closed", var4);
         }
      });
      if (this.getServerConnections() == connection.getParent() && !connection.isPersistent()) {
         this.getServerConnections().remove(connection.getPropertyInParent());
      }
   }

   public FoxServer getFoxServer() {
      return this.daemon;
   }

   public static NiagaraNetwork getNiagaraNetwork() {
      try {
         return (NiagaraNetwork)Sys.getService(Sys.getType("niagaraDriver:NiagaraNetwork"));
      } catch (ServiceNotFoundException var1) {
         return null;
      }
   }

   public static int getHttpPort() {
      try {
         Type type = Sys.getType("web:WebService");
         BComponent webService = Sys.getService(type);
         BInteger port = (BInteger)webService.get("httpPort");
         return port.getInt();
      } catch (Exception var3) {
         return -1;
      }
   }

   public static boolean auditConnection(String operation, FoxSession session) {
      String appName = session.getRemoteHello().getString("app.name", "Station");
      if (appName.equals("Station")) {
         switch (operation) {
            case "Login":
            case "Logout":
            case "Logout (Timeout)":
               BFoxService svc = (BFoxService)Sys.getService(TYPE);
               return svc.getAuditStationLoginEvents();
            default:
               return true;
         }
      } else {
         return true;
      }
   }

   public final boolean allowLegacyClients() {
      int ordinal = this.getSupportLegacyClients().getOrdinal();
      switch (ordinal) {
         case 1:
            return false;
         case 2:
            return true;
         default:
            return true;
      }
   }

   public IPropertyValidator getPropertyValidator(Property[] properties, Context context) {
      return (IPropertyValidator)(ArrayUtil.indexOf(properties, certAliasAndPassword) > -1 ? this.validator : super.getPropertyValidator(properties, context));
   }

   public IPropertyValidator getPropertyValidator(Property property, Context context) {
      return (IPropertyValidator)(certAliasAndPassword.equals(property) ? this.validator : super.getPropertyValidator(property, context));
   }

   public BIcon getIcon() {
      return ICON;
   }

   public final Property getCertificateAliasAndPasswordProperty(Context context) {
      return certAliasAndPassword;
   }

   public BPassword retrieveCertificatePassword(Property certAliasAndPasswordProperty, Context context) {
      if (!certAliasAndPassword.equals(certAliasAndPasswordProperty)) {
         throw new IllegalArgumentException(
            "Unexpected CertificateAliasAndPassword property argument: \"" + certAliasAndPasswordProperty + "\" != \"" + certAliasAndPassword + '"'
         );
      } else {
         SecurityManager sm = System.getSecurityManager();
         if (sm != null) {
            sm.checkPermission(new SigningPasswordPermission(TYPE.getTypeSpec().getModuleName()));
         }

         return BPassword.make(AccessController.doPrivileged((PrivilegedAction<String>)(() -> this.getCertAliasAndPassword().getPassword().getValue())));
      }
   }

   public void certificateSigned(Property certAliasAndPasswordProperty, X509Certificate[] certificateChain, Context context) {
      if (!certAliasAndPassword.equals(certAliasAndPasswordProperty)) {
         throw new IllegalArgumentException(
            "Unexpected CertificateAliasAndPassword property argument: \"" + certAliasAndPasswordProperty + "\" != \"" + certAliasAndPassword + '"'
         );
      } else {
         this.changed(certAliasAndPassword, null);
      }
   }

   class Daemon extends FoxServer {
      private final BFoxService service;

      public Daemon(BFoxService service, BServerPort foxPort, BServerPort foxsPort) {
         super(foxPort, foxsPort);
         this.service = service;
      }

      @Override
      public void run() {
         try {
            super.run();
         } catch (Exception var2) {
         }
      }

      @Override
      public ServerSocket getFoxsServerSocket() throws IOException {
         try {
            return AccessController.doPrivileged(
               (PrivilegedExceptionAction<ServerSocket>)(() -> {
                  ICryptoManager cryptoService;
                  CoreCryptoManager ccm;
                  try {
                     cryptoService = (ICryptoManager)Nre.getServiceManager().getService("platCrypto:CertManagerService");
                     ccm = CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
                  } catch (Exception var18) {
                     throw new IOException("unable to get crypto services references", var18);
                  }

                  SecretChars certPasswordChars = null;
                  BCertificateAliasAndPassword aliasAndPassword = this.service.getCertAliasAndPassword();

                  ServerSocket var10;
                  try {
                     String certAlias = null;
                     if (!aliasAndPassword.getPassword().isDefault()) {
                        if (BFoxService.LOG.isLoggable(Level.FINE)) {
                           BFoxService.LOG.fine("using cert password to retrieve certificate " + aliasAndPassword.getAlias());
                        }

                        try {
                           certPasswordChars = AccessController.doPrivileged(aliasAndPassword.getPassword()::getSecretChars);
                        } catch (Exception var17) {
                           BFoxService.LOG.log(Level.SEVERE, "error decoding password for cert", (Throwable)var17);
                        }

                        ServerCertificateHealth certStatus = ccm.checkServerCertificateStatus(aliasAndPassword.getAlias(), certPasswordChars, BFoxService.LOG);
                        this.service.setServerCertificateHealth(new BServerCertificateHealth(certStatus));
                        certAlias = certStatus.getReturnedCert();
                        if ("default".equals(certAlias)) {
                           certPasswordChars = null;
                        }
                     } else {
                        ServerCertificateHealth certStatus = ccm.checkServerCertificateStatus(aliasAndPassword.getAlias(), null, BFoxService.LOG);
                        this.service.setServerCertificateHealth(new BServerCertificateHealth(certStatus));
                        certAlias = certStatus.getReturnedCert();
                     }

                     if (BFoxService.LOG.isLoggable(Level.FINE)) {
                        BFoxService.LOG.fine("service started with cert " + aliasAndPassword.getAlias());
                     }

                     if ("tridium".equals(certAlias) || "default".equals(certAlias)) {
                        BFoxService.LOG
                           .warning(
                              "Using default TLS server certificate '"
                                 + certAlias
                                 + "' is not recommended. Generate a certificate specifically for this installation and sign it with a proper CA."
                           );
                     }

                     TlsCipherSuiteGroup cipherSuiteGroup = this.service.getCipherSuiteGroup().getCipherSuiteGroup();
                     boolean wantClientAuth = !ccm.getTrustAnchors().isEmpty();
                     ServerTlsParameters tlsParams = new ServerTlsParameters(
                        this.service.getFoxsMinProtocol().getTag(), certAlias, cipherSuiteGroup, wantClientAuth
                     );
                     if (certPasswordChars != null) {
                        tlsParams.setKeyPassphrase(certPasswordChars.get());
                     }

                     if (cipherSuiteGroup != TlsCipherSuiteGroup.recommended) {
                        BFoxService.LOG.warning("not using recommended tls cipher suite group, using " + tlsParams);
                     } else if (BFoxService.LOG.isLoggable(Level.FINE)) {
                        BFoxService.LOG.fine("using " + tlsParams);
                     }

                     ServerSocketFactory fact = cryptoService.getServerSocketFactory(tlsParams);
                     if (!this.foxsPort.getBindToLoopback()) {
                        return fact.createServerSocket(BFoxService.this.getFoxsPort().getBindingPort(), 10);
                     }

                     var10 = fact.createServerSocket(BFoxService.this.getFoxsPort().getBindingPort(), 10, InetAddress.getByName(null));
                  } catch (BindException var19) {
                     throw var19;
                  } catch (Exception var20) {
                     BFoxService.LOG.log(Level.FINE, "unable to create socket for foxs", (Throwable)var20);
                     throw new IOException("unable to create socket for foxs", var20);
                  } finally {
                     if (certPasswordChars != null) {
                        certPasswordChars.close();
                     }
                  }

                  return var10;
               })
            );
         } catch (PrivilegedActionException var2) {
            throw (IOException)var2.getException();
         }
      }

      @Override
      public FoxConnection makeConnection(FoxSession session, FoxMessage remoteHello) throws Exception {
         return BFoxService.this.makeServerConnection(session, remoteHello);
      }

      @Override
      public void connectionAuthenticated(FoxConnection conn, FoxSession session, FoxMessage remoteHello) throws Exception {
         if (conn instanceof BFoxServerConnection) {
            BFoxServerConnection serverConn = (BFoxServerConnection)conn;
            if (serverConn.getParent() == null) {
               String stationName = remoteHello.getString("station.name", null);
               String serverConnName;
               if (stationName != null) {
                  serverConnName = "Station_" + stationName;
                  if (BFoxService.this.getServerConnections().getSlot(serverConnName) != null) {
                     serverConnName = "Session?";
                  }
               } else {
                  serverConnName = "Session?";
               }

               BFoxService.this.getServerConnections().add(serverConnName, serverConn, 3);
            }
         }

         for (BFoxService.FoxServerConnectionListener listener : BFoxService.this.serverConnectionListeners) {
            listener.serverConnectionAuthenticated(conn, session, remoteHello);
         }
      }

      @Override
      public FoxMessage getAnnouncement(FoxMessage message) {
         if (!BFoxService.this.getEnableAnnouncement()) {
            return null;
         } else {
            FoxMessage msg = new FoxMessage();
            msg.add("station", Sys.getStation().getStationName());
            if (Fox.ipv4Enabled) {
               InetAddress announcementAddressV4 = NreLib.getLocalHost(false);
               if (!announcementAddressV4.isLoopbackAddress()) {
                  msg.add("hostName", announcementAddressV4.getHostName());
                  msg.add("hostAddress", announcementAddressV4.getHostAddress());
               }
            }

            if (Fox.ipv6Enabled) {
               InetAddress announcementAddressV6 = NreLib.getLocalHost(true);
               if (!announcementAddressV6.isLoopbackAddress()) {
                  msg.add("hostNameIPv6", announcementAddressV6.getHostName());
                  msg.add("hostAddressIPv6", announcementAddressV6.getHostAddress());
               }
            }

            if (BFoxService.this.getFoxEnabled()) {
               msg.add("foxPort", BFoxService.this.getFoxPort().getPublicServerPort());
            }

            if (BFoxService.this.getFoxsEnabled()) {
               msg.add("foxsPort", BFoxService.this.getFoxsPort().getPublicServerPort());
            }

            msg.add("httpPort", BFoxService.getHttpPort());
            msg.add("version", Sys.getBajaVersion().toString());
            msg.add("hostId", Nre.getHostId());
            msg.add("hostModel", Nre.getHostModel());
            msg.add("brandId", Brand.getBrandId());
            msg.add("vmName", Fox.vmName);
            msg.add("vmVersion", Fox.vmVersion);
            msg.add("osName", Fox.osName);
            msg.add("osVersion", Fox.osVersion);
            return msg;
         }
      }

      @Override
      public boolean authenticateBasic(FoxSession session, String username, String password) throws Exception {
         throw new AuthenticationException("Unsupported method call authenticateBasic in BFoxService. This call should be handled by an Authentication Agent.");
      }

      @Override
      public boolean authenticateDigest(FoxSession session, String username, byte[] nonce, byte[] digest) throws Exception {
         throw new AuthenticationException("Unsupported method call authenticateDigest in BFoxService. This call should be handled by an Authentication Agent.");
      }
   }

   public interface FoxServerConnectionListener {
      default BFoxServerConnection getPersistentConnection(FoxSession session, FoxMessage remoteHello) {
         return null;
      }

      default void serverConnectionCreated(BFoxServerConnection connection, FoxSession session, FoxMessage remoteHello) {
      }

      default void serverConnectionClosed(BFoxServerConnection connection, Throwable cause) {
      }

      default void serverConnectionAuthenticated(FoxConnection conn, FoxSession session, FoxMessage remoteHello) {
      }
   }
}
