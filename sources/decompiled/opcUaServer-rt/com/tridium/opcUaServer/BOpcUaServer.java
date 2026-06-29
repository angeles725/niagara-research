package com.tridium.opcUaServer;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UserTokenPolicies;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.server.NodeBuilderException;
import com.prosysopc.ua.server.ServerUserIdentity;
import com.prosysopc.ua.server.Session;
import com.prosysopc.ua.server.SessionManagerListener;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.ApplicationType;
import com.prosysopc.ua.stack.core.UserIdentityToken;
import com.prosysopc.ua.stack.core.UserTokenPolicy;
import com.prosysopc.ua.stack.core.UserTokenType;
import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.transport.security.PrivKey;
import com.prosysopc.ua.stack.transport.security.SecurityMode;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.StackUtils;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;
import com.tridium.crypto.core.cert.KeyPurpose;
import com.tridium.ndriver.BNNetwork;
import com.tridium.ndriver.poll.BNPollScheduler;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.security.SecretChars;
import com.tridium.opcUaCore.BOpcUserAuthenticationMethods;
import com.tridium.opcUaCore.OpcUaSecurityMode;
import com.tridium.opcUaServer.event.BOpcUaAlarmClass;
import com.tridium.opcUaServer.event.BOpcUaAlarmRecipient;
import com.tridium.opcUaServer.util.OpcUaServerCertificateValidator;
import com.tridium.opcUaServer.util.OpcUaServerUtil;
import com.tridium.opcUaServer.util.OpcUaUserValidator;
import com.tridium.sys.registry.NModuleInfo;
import com.tridium.util.CompUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentList;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmRecipient;
import javax.baja.alarm.BAlarmService;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.registry.ModuleInfo;
import javax.baja.security.BCertificateAliasAndPassword;
import javax.baja.security.crypto.CertManagerFactory;
import javax.baja.security.crypto.ICryptoManager;
import javax.baja.security.crypto.IKeyStore;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BModule;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "pollScheduler",
      type = "BNPollScheduler",
      defaultValue = "new BNPollScheduler()"
   ), @NiagaraProperty(
      name = "opcUaServerName",
      type = "String",
      defaultValue = "N4OpcUaServer"
   ), @NiagaraProperty(
      name = "certAliasAndPassword",
      type = "BCertificateAliasAndPassword",
      defaultValue = "BCertificateAliasAndPassword.DEFAULT",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "opcTcpEndpoint",
      type = "BOpcTcpEndpoint",
      defaultValue = "new BOpcTcpEndpoint()"
   ), @NiagaraProperty(
      name = "userAuthenticationMethods",
      type = "BOpcUserAuthenticationMethods",
      defaultValue = "BOpcUserAuthenticationMethods.DEFAULT"
   ), @NiagaraProperty(
      name = "caCertificate",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "BString.make(\"workbench:UserTrustCertificateAliasFE\")"
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "BString.make(\"webEditors:CertificateAliasEditor\")"
      ), @Facet("BFacets.make(\"storeId\", BString.make(\"USER_TRUST_STORE\"))"), @Facet("BFacets.make(\"purposeId\", BString.make(\"\"))")}
   ), @NiagaraProperty(
      name = "minWorkerThreads",
      type = "int",
      defaultValue = "Math.max(8, OpcUaServerUtil.getBaseThreadCount(Runtime.getRuntime().availableProcessors()))",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "Math.max(8, OpcUaServerUtil.getBaseThreadCount(Runtime.getRuntime().availableProcessors()))"
      )}
   ), @NiagaraProperty(
      name = "maxSessionCount",
      type = "int",
      defaultValue = "500"
   ), @NiagaraProperty(
      name = "maxSessionTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeMinutes(5)"
   ), @NiagaraProperty(
      name = "maxSubcriptionCount",
      type = "int",
      defaultValue = "50"
   ), @NiagaraProperty(
      name = "maxMonitoredItemsPerSubscription",
      type = "int",
      defaultValue = "10000"
   ), @NiagaraProperty(
      name = "opcTcpConnectionAddress",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "serverInfo",
      type = "BOpcUaBuildInfo",
      defaultValue = "new BOpcUaBuildInfo()",
      flags = 3
   ), @NiagaraProperty(
      name = "sessionInfo",
      type = "BOpcUaServerSessions",
      defaultValue = "new BOpcUaServerSessions()",
      flags = 3
   )})
@NiagaraActions({@NiagaraAction(
      name = "shutdown",
      parameterType = "BInteger",
      defaultValue = "BInteger.make(5)",
      flags = 20
   ), @NiagaraAction(
      name = "restart",
      flags = 20
   )})
public class BOpcUaServer extends BNNetwork implements SessionManagerListener {
   public static final Property pollScheduler = newProperty(0, new BNPollScheduler(), null);
   public static final Property opcUaServerName = newProperty(0, "N4OpcUaServer", null);
   public static final Property certAliasAndPassword = newProperty(4, BCertificateAliasAndPassword.DEFAULT, BFacets.make("security", BBoolean.TRUE));
   public static final Property opcTcpEndpoint = newProperty(0, new BOpcTcpEndpoint(), null);
   public static final Property userAuthenticationMethods = newProperty(0, BOpcUserAuthenticationMethods.DEFAULT, null);
   public static final Property caCertificate = newProperty(
      5,
      "",
      BFacets.make(
         BFacets.make(
            BFacets.make(
               BFacets.make("fieldEditor", BString.make("workbench:UserTrustCertificateAliasFE")),
               BFacets.make("uxFieldEditor", BString.make("webEditors:CertificateAliasEditor"))
            ),
            BFacets.make("storeId", BString.make("USER_TRUST_STORE"))
         ),
         BFacets.make("purposeId", BString.make(""))
      )
   );
   public static final Property minWorkerThreads = newProperty(
      0,
      Math.max(8, OpcUaServerUtil.getBaseThreadCount(Runtime.getRuntime().availableProcessors())),
      BFacets.make("min", Math.max(8, OpcUaServerUtil.getBaseThreadCount(Runtime.getRuntime().availableProcessors())))
   );
   public static final Property maxSessionCount = newProperty(0, 500, null);
   public static final Property maxSessionTimeout = newProperty(0, BRelTime.makeMinutes(5), null);
   public static final Property maxSubcriptionCount = newProperty(0, 50, null);
   public static final Property maxMonitoredItemsPerSubscription = newProperty(0, 10000, null);
   public static final Property opcTcpConnectionAddress = newProperty(3, "", null);
   public static final Property serverInfo = newProperty(3, new BOpcUaBuildInfo(), null);
   public static final Property sessionInfo = newProperty(3, new BOpcUaServerSessions(), null);
   public static final Action shutdown = newAction(20, BInteger.make(5), null);
   public static final Action restart = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BOpcUaServer.class);
   boolean debug = false;
   public UaServer server;
   private CountDownLatch syncShutdownServerStart;
   private static final int SERVER_NORMAL_SHUTDOWN_DELAY = 5;
   private static final int SERVER_FORCED_SHUTDOWN_DELAY = 0;
   private static final int SERVER_RESTART_DELAY = 1;
   public static final String APP_NAME = "N4_OpcUaServer";
   public static final Lexicon LEX = Lexicon.make(BOpcUaServer.class);
   private static final Logger logger = Logger.getLogger("opcUaServer.server");

   public BNPollScheduler getPollScheduler() {
      return (BNPollScheduler)this.get(pollScheduler);
   }

   public void setPollScheduler(BNPollScheduler v) {
      this.set(pollScheduler, v, null);
   }

   public String getOpcUaServerName() {
      return this.getString(opcUaServerName);
   }

   public void setOpcUaServerName(String v) {
      this.setString(opcUaServerName, v, null);
   }

   public BCertificateAliasAndPassword getCertAliasAndPassword() {
      return (BCertificateAliasAndPassword)this.get(certAliasAndPassword);
   }

   public void setCertAliasAndPassword(BCertificateAliasAndPassword v) {
      this.set(certAliasAndPassword, v, null);
   }

   public BOpcTcpEndpoint getOpcTcpEndpoint() {
      return (BOpcTcpEndpoint)this.get(opcTcpEndpoint);
   }

   public void setOpcTcpEndpoint(BOpcTcpEndpoint v) {
      this.set(opcTcpEndpoint, v, null);
   }

   public BOpcUserAuthenticationMethods getUserAuthenticationMethods() {
      return (BOpcUserAuthenticationMethods)this.get(userAuthenticationMethods);
   }

   public void setUserAuthenticationMethods(BOpcUserAuthenticationMethods v) {
      this.set(userAuthenticationMethods, v, null);
   }

   public String getCaCertificate() {
      return this.getString(caCertificate);
   }

   public void setCaCertificate(String v) {
      this.setString(caCertificate, v, null);
   }

   public int getMinWorkerThreads() {
      return this.getInt(minWorkerThreads);
   }

   public void setMinWorkerThreads(int v) {
      this.setInt(minWorkerThreads, v, null);
   }

   public int getMaxSessionCount() {
      return this.getInt(maxSessionCount);
   }

   public void setMaxSessionCount(int v) {
      this.setInt(maxSessionCount, v, null);
   }

   public BRelTime getMaxSessionTimeout() {
      return (BRelTime)this.get(maxSessionTimeout);
   }

   public void setMaxSessionTimeout(BRelTime v) {
      this.set(maxSessionTimeout, v, null);
   }

   public int getMaxSubcriptionCount() {
      return this.getInt(maxSubcriptionCount);
   }

   public void setMaxSubcriptionCount(int v) {
      this.setInt(maxSubcriptionCount, v, null);
   }

   public int getMaxMonitoredItemsPerSubscription() {
      return this.getInt(maxMonitoredItemsPerSubscription);
   }

   public void setMaxMonitoredItemsPerSubscription(int v) {
      this.setInt(maxMonitoredItemsPerSubscription, v, null);
   }

   public String getOpcTcpConnectionAddress() {
      return this.getString(opcTcpConnectionAddress);
   }

   public void setOpcTcpConnectionAddress(String v) {
      this.setString(opcTcpConnectionAddress, v, null);
   }

   public BOpcUaBuildInfo getServerInfo() {
      return (BOpcUaBuildInfo)this.get(serverInfo);
   }

   public void setServerInfo(BOpcUaBuildInfo v) {
      this.set(serverInfo, v, null);
   }

   public BOpcUaServerSessions getSessionInfo() {
      return (BOpcUaServerSessions)this.get(sessionInfo);
   }

   public void setSessionInfo(BOpcUaServerSessions v) {
      this.set(sessionInfo, v, null);
   }

   public void shutdown(BInteger parameter) {
      this.invoke(shutdown, parameter, null);
   }

   public void restart() {
      this.invoke(restart, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final Feature getLicenseFeature() {
      return Sys.getLicenseManager().getFeature("tridium", "opcUaServer");
   }

   public void started() throws Exception {
      if (!Flags.has(this, certAliasAndPassword, 268435456)) {
         Flags.add(this, certAliasAndPassword, null, new int[]{268435456});
         this.setFlags(certAliasAndPassword, this.getFlags(certAliasAndPassword) & -5);
         this.getCertAliasAndPassword().setFacets(BCertificateAliasAndPassword.alias, BFacets.make("purposeId", KeyPurpose.SERVER_CERT.name()));
      }

      this.initializeAlarmService();
      super.started();
      this.startServer(false);
   }

   public void stopped() throws Exception {
      if (this.server != null && this.server.isRunning()) {
         this.doShutdown(BInteger.make(5));
      }

      super.stopped();
   }

   public final AgentList getAgents(Context cx) {
      AgentList list = super.getAgents(cx);
      list.remove("ndriver:NDeviceManager");
      return list;
   }

   private void initializeAlarmService() {
      BComponent service = Sys.getService(BAlarmService.TYPE);
      if (service instanceof BAlarmService) {
         BOpcUaAlarmClass[] children = (BOpcUaAlarmClass[])service.getChildren(BOpcUaAlarmClass.class);
         if (children == null || children.length == 0) {
            Property classProp = service.add("OpcUaAlarmClass?", new BOpcUaAlarmClass());
            Property recipientProp = service.add("OpcUaAlarmRecipient?", new BOpcUaAlarmRecipient());
            BOpcUaAlarmClass almClass = (BOpcUaAlarmClass)service.get(classProp);
            BOpcUaAlarmRecipient recipient = (BOpcUaAlarmRecipient)service.get(recipientProp);
            recipient.add("l?", recipient.makeLink(almClass, BAlarmClass.alarm, BAlarmRecipient.routeAlarm, null));
         }

         if (children != null && children.length != 0) {
            this.getAlarmSourceInfo().setAlarmClass(children[0].getName());
         }
      }
   }

   private void startServer(boolean isRestart) {
      if (!this.getStatus().isDisabled()) {
         if (Sys.getService(TYPE) != this) {
            this.configFail(LEX.getText("opcUaServer.moreThanOneServer"));
            throw new IllegalStateException(LEX.getText("opcUaServer.moreThanOneServer"));
         } else {
            try {
               if (!this.isServerLicensed()) {
                  return;
               }

               AccessController.doPrivileged(
                  (PrivilegedExceptionAction<Void>)(() -> {
                     if (this.server != null && this.server.isRunning()) {
                        String msg = "Received server "
                           + (isRestart ? "re" : "")
                           + "start request when the server is already running. Shutting down the current server...";
                        logger.log(Level.INFO, msg);
                        if (isRestart) {
                           this.doShutdown(BInteger.make(1));
                        } else {
                           this.doShutdown(BInteger.make(0));
                        }
                     }

                     if (this.syncShutdownServerStart != null) {
                        try {
                           if (!this.syncShutdownServerStart.await(5L, TimeUnit.SECONDS)) {
                              logger.log(Level.WARNING, "Server start request timed out while waiting for the existing server to stop.");
                              return null;
                           }
                        } catch (Exception var9) {
                           if (logger.isLoggable(Level.FINE)) {
                              logger.log(Level.SEVERE, "Exception occurred while waiting for existing server to shutdown", (Throwable)var9);
                           } else {
                              logger.log(Level.SEVERE, "Exception occurred while waiting for existing server to shutdown");
                           }
                        }
                     }

                     if (this.server != null && this.server.isRunning()) {
                        logger.log(Level.WARNING, "Failed to shutdown the existing server. Cannot start a new server.");
                        return null;
                     } else {
                        String serverName = this.getOpcUaServerName();
                        this.initialize(
                           this.getOpcTcpEndpoint().getPort(), this.getOpcTcpEndpoint().getEnabled(), serverName.isEmpty() ? "N4_OpcUaServer" : serverName
                        );
                        this.server.start();
                        if (this.server != null && this.server.isRunning()) {
                           this.configOk();
                           logger.log(Level.INFO, "OpcUaServer started on port " + this.server.getPort());
                        }

                        ApplicationIdentity applicationIdentity = this.server.getApplicationIdentity();
                        ApplicationDescription applicationDescription = applicationIdentity.getApplicationDescription();

                        for (String epUrls : applicationDescription.getDiscoveryUrls()) {
                           if (logger.isLoggable(Level.FINE)) {
                              logger.fine("OpcUaServer registered at endpoint: " + epUrls);
                           }

                           if (epUrls.startsWith("opc.tcp:")) {
                              this.setOpcTcpConnectionAddress(epUrls);
                           }
                        }

                        this.createAddressSpace(isRestart);
                        return null;
                     }
                  })
               );
            } catch (Exception var3) {
               logger.log(Level.SEVERE, "Exception occurred while starting server", (Throwable)var3);
            }
         }
      }
   }

   private boolean isServerLicensed() {
      try {
         this.getLicenseFeature();
         return true;
      } catch (FeatureNotLicensedException var2) {
         this.setFaultCause(LEX.getText("opcUaServer.licensed"));
         return false;
      }
   }

   public void doRestart() {
      if (!this.getStatus().isDisabled()) {
         if (this.server != null && this.server.isRunning()) {
            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
                  this.syncShutdownServerStart = new CountDownLatch(1);
                  LocalizedText reason = new LocalizedText("OpcUaServer restarting...", Locale.ENGLISH);
                  this.server.shutdown(1, reason);
                  this.server.close();
                  this.server = null;
                  StackUtils.shutdown();
                  this.syncShutdownServerStart.countDown();
                  this.syncShutdownServerStart = null;
                  logger.log(Level.INFO, reason.getText());
                  return null;
               }));
            } catch (Exception var2) {
               logger.log(Level.SEVERE, "Exception occurred while shutting down server", (Throwable)var2);
            }
         }

         this.startServer(true);
      }
   }

   public void doShutdown(BInteger delay) {
      if (this.server != null && this.server.isRunning()) {
         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               this.syncShutdownServerStart = new CountDownLatch(1);
               LocalizedText reason = new LocalizedText("OpcUaServer stopped", Locale.ENGLISH);
               if (this.getStatus().isDisabled()) {
                  reason = new LocalizedText("OpcUaServer has been disabled", Locale.ENGLISH);
               }

               this.server.shutdown(delay.getInt(), reason);
               this.server.close();
               this.server = null;
               StackUtils.shutdown();
               this.syncShutdownServerStart.countDown();
               this.syncShutdownServerStart = null;
               logger.log(Level.INFO, reason.getText());
               return null;
            }));
         } catch (Exception var3) {
            logger.log(Level.SEVERE, "Exception occurred while shutting down server", (Throwable)var3);
         }
      } else {
         logger.log(Level.INFO, "OpcUaServer is already shut down or closed by user");
      }
   }

   public String getNetworkName() {
      return "OpcUaServer";
   }

   public Type getDeviceFolderType() {
      return BOpcUaServerDeviceFolder.TYPE;
   }

   public Type getDeviceType() {
      return BOpcUaNamespace.TYPE;
   }

   public void changed(Property p, Context cx) {
      if (this.isRunning() && !Context.decoding.equals(cx)) {
         if (p.equals(status)) {
            if (this.getStatus().isDisabled()) {
               if (this.server != null && this.server.isRunning()) {
                  this.doShutdown(BInteger.make(5));
               }
            } else {
               this.startServer(true);
            }
         }

         if (p.equals(userAuthenticationMethods) || p.equals(opcTcpEndpoint) || p.equals(certAliasAndPassword) || p.equals(opcUaServerName)) {
            this.doRestart();
         }

         super.changed(p, cx);
      } else {
         super.changed(p, cx);
      }
   }

   private void initialize(int port, boolean tcpEnable, String applicationName) throws SecureIdentityException, IOException, UaServerException {
      try {
         AccessController.doPrivileged(
            (PrivilegedExceptionAction<Void>)(() -> {
               logger.fine("Initializing OpcUaServer...");
               if (OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
                  int adjustedNonBlockingPriority = 7;
                  int adjustedBlockingPriority = 7;
                  int adjustedSelectorPriority = 5;
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("OPCUA using adjustedNonBlockingPriority = " + adjustedNonBlockingPriority);
                     logger.fine("OPCUA using adjustedBlockingPriority = " + adjustedBlockingPriority);
                     logger.fine("OPCUA using adjustedSelectorPriority = " + adjustedSelectorPriority);
                  }

                  StackUtils.setNonBlockingWorkExecutorThreadPriority(adjustedNonBlockingPriority);
                  StackUtils.setBlockingWorkExecutorThreadPriority(adjustedBlockingPriority);
                  StackUtils.setAsyncSelectorThreadPriority(adjustedSelectorPriority);
               }

               StackUtils.setBlockingWorkerThreadPoolCoreSize(this.getMinWorkerThreads());
               StackUtils.getBlockingWorkExecutor();
               StackUtils.getNonBlockingWorkExecutor();
               this.server = new UaServer();
               if (tcpEnable) {
                  this.server.setPort(Protocol.OpcTcp, port);
               }

               this.server.setServerName("OPCUA/" + applicationName);
               Set<InetAddress> bindAddresses = new HashSet<>();
               bindAddresses.add(InetAddress.getByAddress(new byte[]{0, 0, 0, 0}));
               this.server.setBindAddresses(bindAddresses);
               OpcUaServerCertificateValidator validator = new OpcUaServerCertificateValidator(port);
               this.server.setCertificateValidator(validator);
               ApplicationDescription appDescription = new ApplicationDescription();
               appDescription.setApplicationName(new LocalizedText(applicationName + "@localhost"));
               appDescription.setApplicationUri("urn:localhost:OPCUA:" + applicationName);
               appDescription.setProductUri("urn:tridium.com:OPCUA:" + applicationName);
               appDescription.setApplicationType(ApplicationType.Server);
               BCertificateAliasAndPassword aliasAndPassword = this.getCertAliasAndPassword();
               ICryptoManager cryptoManager = CertManagerFactory.getInstance();
               IKeyStore keyStore = cryptoManager.getKeyStore();
               X509Certificate serverCert = keyStore.getCertificate(aliasAndPassword.getAlias());
               PrivKey privKey = new PrivKey(getServerPrivateKey(keyStore, aliasAndPassword));
               ApplicationIdentity identity = new ApplicationIdentity(new Cert(serverCert), privKey);
               identity.setApplicationDescription(appDescription);
               this.server.setApplicationIdentity(identity);
               List<SecurityMode> securityModes = OpcUaSecurityMode.makeSecurityModes(
                  this.getOpcTcpEndpoint().getSecurityMode(), this.getOpcTcpEndpoint().getSecurityPolicies()
               );
               this.server.getSecurityModes().addAll(securityModes);
               BOpcUserAuthenticationMethods userAuthenticationMethods = this.getUserAuthenticationMethods();
               if (userAuthenticationMethods.includes(1)) {
                  this.server.addUserTokenPolicy(UserTokenPolicies.ANONYMOUS);
               }

               if (userAuthenticationMethods.includes(2)) {
                  this.server.addUserTokenPolicy(new UserTokenPolicy("username_plain", UserTokenType.UserName, null, null, SecurityPolicy.NONE.getPolicyUri()));
               }

               if (userAuthenticationMethods.includes(4)) {
                  this.server.addUserTokenPolicy(UserTokenPolicies.SECURE_CERTIFICATE);
               }

               this.server.setUserValidator(new OpcUaUserValidator(validator));
               this.server.init();
               this.initBuildInfo();
               this.server.getSessionManager().setMaxSessionCount(this.getMaxSessionCount());
               this.server.getSessionManager().setMaxSessionTimeout(this.getMaxSessionTimeout().getMillis());
               this.server.getSubscriptionManager().setMaxSubscriptionCount(this.getMaxSubcriptionCount());
               this.server.getSubscriptionManager().setMaxMonitoredItemsPerSubscription(this.getMaxMonitoredItemsPerSubscription());
               this.server.getSessionManager().addListener(this);
               return null;
            })
         );
      } catch (PrivilegedActionException var5) {
         logger.log(Level.SEVERE, "Exception occurred while initializing server", (Throwable)var5);
      }
   }

   private void createAddressSpace(boolean isRestart) throws StatusException, UaInstantiationException, NodeBuilderException {
      this.loadInformationModels();
      logger.fine("Creating OPCUA address space");
      BOpcUaNamespace[] nodeSpaces = (BOpcUaNamespace[])CompUtil.getDescendants(this, BOpcUaNamespace.class);

      for (BOpcUaNamespace nodeSpace : nodeSpaces) {
         try {
            nodeSpace.startNodeSpace(isRestart);
         } catch (Exception var8) {
            logger.log(Level.SEVERE, "Exception occurred while starting NodeSpace", (Throwable)var8);
         }
      }
   }

   private void loadInformationModels() {
   }

   private void initBuildInfo() {
      BuildInfoTypeNode buildInfo = this.server.getNodeManagerRoot().getServerData().getServerStatusNode().getBuildInfoNode();
      buildInfo.setProductName("N4_OpcUaServer");
      BModule module = Sys.getModuleForClass(BOpcUaServer.class);
      String classVersion = BModule.getClassVersion(BOpcUaServer.class).toString();
      String classVendor = BModule.getClassVendor(BOpcUaServer.class);
      buildInfo.setManufacturerName(classVendor);
      buildInfo.setSoftwareVersion(classVersion);
      int splitIndex = classVersion.lastIndexOf(".");
      String substring = classVersion.substring(splitIndex + 1);
      buildInfo.setBuildNumber(substring);
      ModuleInfo moduleInfo = module.getModuleInfo(RuntimeProfile.rt);
      if (moduleInfo instanceof NModuleInfo) {
         long buildTime = ((NModuleInfo)moduleInfo).getBuildTime();
         GregorianCalendar c = new GregorianCalendar();
         c.setTimeInMillis(buildTime);
         buildInfo.setBuildDate(DateTime.fromInstant(c.toInstant()));
      }
   }

   public boolean onActivateSession(Session session, ServerUserIdentity serverUserIdentity) throws StatusException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onActivateSession " + session.getSessionName() + " " + serverUserIdentity.getName());
      }

      return true;
   }

   public void onActivateSessionError(Session session, UserIdentityToken userIdentityToken, Exception e) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onActivateSessionError " + session.getSessionName() + " " + e);
      }
   }

   public void onAfterActivateSession(Session session) {
      this.getSessionInfo().addSession(session);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onAfterActivateSession " + session.getSessionName());
      }
   }

   public void onCancelSession(Session session) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onCancelSession " + session.getSessionName());
      }
   }

   public void onCloseSession(Session session, boolean b) {
      this.getSessionInfo().removeSession(session);
      OpcUaUserValidator.invalidateSessionForNodeId(session.getSessionId());
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onCloseSession " + session.getSessionName() + " " + b);
      }
   }

   public void onCreateSession(Session session) throws StatusException {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("onCreateSession " + session.getSessionName());
      }
   }

   protected boolean useAutoManager() {
      return false;
   }

   private static RSAPrivateKey getServerPrivateKey(IKeyStore keyStore, BCertificateAliasAndPassword aliasAndPassword) throws Exception {
      if (aliasAndPassword.getPassword().isDefault()) {
         return (RSAPrivateKey)keyStore.getKey(aliasAndPassword.getAlias(), null);
      } else {
         SecretChars passwordChars = AccessController.doPrivileged(aliasAndPassword.getPassword()::getSecretChars);
         Throwable var3 = null;

         RSAPrivateKey var4;
         try {
            var4 = (RSAPrivateKey)keyStore.getKey(aliasAndPassword.getAlias(), passwordChars.get());
         } catch (Throwable var13) {
            var3 = var13;
            throw var13;
         } finally {
            if (passwordChars != null) {
               if (var3 != null) {
                  try {
                     passwordChars.close();
                  } catch (Throwable var12) {
                     var3.addSuppressed(var12);
                  }
               } else {
                  passwordChars.close();
               }
            }
         }

         return var4;
      }
   }
}
