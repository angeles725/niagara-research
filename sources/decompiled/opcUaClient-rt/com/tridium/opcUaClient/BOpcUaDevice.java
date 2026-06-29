package com.tridium.opcUaClient;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.MonitoredItemBase;
import com.prosysopc.ua.OperationLimits;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.client.UaClientListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.ApplicationType;
import com.prosysopc.ua.stack.core.ObjectIdentifiers;
import com.prosysopc.ua.stack.core.ReferenceTypeIdentifiers;
import com.prosysopc.ua.stack.core.VariableIdentifiers;
import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.transport.security.HttpsSecurityPolicy;
import com.prosysopc.ua.stack.transport.security.PrivKey;
import com.prosysopc.ua.stack.transport.security.SecurityMode;
import com.tridium.crypto.core.cert.KeyPurpose;
import com.tridium.ndriver.BNDevice;
import com.tridium.ndriver.discover.BNDiscoveryJob;
import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.ndriver.poll.BINPollable;
import com.tridium.ndriver.poll.BNPollScheduler;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.nre.security.SecretChars;
import com.tridium.opcUaClient.alarm.BOpcUaClientAlarmDeviceExt;
import com.tridium.opcUaClient.history.BOpcUaClientHistoryDeviceExt;
import com.tridium.opcUaClient.point.BOpcUaClientPointDeviceExt;
import com.tridium.opcUaClient.point.BOpcUaClientPointDiscoveryPreferences;
import com.tridium.opcUaClient.point.BOpcUaClientProxyExt;
import com.tridium.opcUaClient.point.BOpcUaLearnBase;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import com.tridium.opcUaClient.util.OpcUaClientCertificateValidator;
import com.tridium.opcUaClient.util.OpcUaClientListener;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaClient.util.OpcUaServerStatusListener;
import com.tridium.opcUaCore.enums.BSecurityMode;
import com.tridium.opcUaCore.enums.BServerState;
import com.tridium.opcUaCore.util.OpcUaCoreUtil;
import com.tridium.util.CompUtil;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BCertificateAliasAndPassword;
import javax.baja.security.BPassword;
import javax.baja.security.crypto.CertManagerFactory;
import javax.baja.security.crypto.ICryptoManager;
import javax.baja.security.crypto.IKeyStore;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.IPropertyValidator;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Validatable;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.BFormat;
import javax.baja.util.BTypeSpec;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75,
      facets = {@Facet("SfUtil.incl(SfUtil.MGR_EDIT_READONLY)")},
      override = true
   ), @NiagaraProperty(
      name = "pollScheduler",
      type = "BNPollScheduler",
      defaultValue = "new BNPollScheduler()"
   ), @NiagaraProperty(
      name = "serverEndpointUrl",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "securityMode",
      type = "BSecurityMode",
      defaultValue = "BSecurityMode.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "certificate",
      type = "String",
      defaultValue = "CertUtils.LEGACY_CERT_ALIAS",
      facets = {@Facet("BFacets.make(BFacets.FIELD_EDITOR, BString.make(\"workbench:CertificateAliasFE\"))"), @Facet("BFacets.make(BFacets.UX_FIELD_EDITOR, BString.make(\"webEditors:CertificateAliasEditor\"))"), @Facet("BFacets.make(\"purposeId\", BString.make(\"CLIENT_CERT\"))")},
      deprecated = true
   ), @NiagaraProperty(
      name = "certAliasAndPassword",
      type = "BCertificateAliasAndPassword",
      defaultValue = "BCertificateAliasAndPassword.DEFAULT",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "userAuthenticationMode",
      type = "BOpcUserAuthenticationMode",
      defaultValue = "BOpcUserAuthenticationMode.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "userName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "password",
      type = "BPassword",
      defaultValue = "BPassword.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "userAuthenticationCertificate",
      type = "BCertificateAliasAndPassword",
      defaultValue = "BCertificateAliasAndPassword.DEFAULT",
      flags = 4,
      facets = {@Facet("BFacets.make(BFacets.SECURITY, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "serverState",
      type = "BServerState",
      defaultValue = "BServerState.Unknown",
      flags = 67
   ), @NiagaraProperty(
      name = "serverCurrentTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "serverStartTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "timeoutInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.make(30*1000)",
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "statusCheckTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.make(10*1000)",
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "operationLimit",
      type = "BRelTime",
      defaultValue = "BRelTime.make(10*1000)",
      flags = 4,
      facets = {@Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "serverInfo",
      type = "BOpcUaBuildInfo",
      defaultValue = "new BOpcUaBuildInfo()",
      flags = 1
   ), @NiagaraProperty(
      name = "AlarmExt",
      type = "BOpcUaClientAlarmDeviceExt",
      defaultValue = "new BOpcUaClientAlarmDeviceExt()",
      flags = 1
   ), @NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal",
      flags = 4
   ), @NiagaraProperty(
      name = "points",
      type = "BOpcUaClientPointDeviceExt",
      defaultValue = "new BOpcUaClientPointDeviceExt()"
   ), @NiagaraProperty(
      name = "histories",
      type = "BOpcUaClientHistoryDeviceExt",
      defaultValue = "new BOpcUaClientHistoryDeviceExt()"
   ), @NiagaraProperty(
      name = "lastLearnedInfo",
      type = "String",
      defaultValue = "",
      flags = 7
   ), @NiagaraProperty(
      name = "learnRootNodeIdString",
      type = "String",
      defaultValue = "ObjectIdentifiers.RootFolder.toString()",
      flags = 6
   ), @NiagaraProperty(
      name = "initialHistoryArchiveFromDate",
      type = "BAbsTime",
      defaultValue = "BAbsTime.DEFAULT"
   )})
@NiagaraActions({@NiagaraAction(
      name = "learn",
      flags = 16
   ), @NiagaraAction(
      name = "resetComm",
      parameterType = "BInteger",
      defaultValue = "BInteger.make(60)",
      flags = 16
   ), @NiagaraAction(
      name = "updateNamespaceUri",
      flags = 20
   ), @NiagaraAction(
      name = "resetDelay",
      flags = 20
   )})
public class BOpcUaDevice extends BNDevice implements BINPollable, IPropertyValidator {
   public static final Property status = newProperty(75, BStatus.ok, SfUtil.incl("ed.ro"));
   public static final Property pollScheduler = newProperty(0, new BNPollScheduler(), null);
   public static final Property serverEndpointUrl = newProperty(0, "", null);
   public static final Property securityMode = newProperty(64, BSecurityMode.DEFAULT, null);
   @Deprecated
   public static final Property certificate = newProperty(
      0,
      "tridium",
      BFacets.make(
         BFacets.make(
            BFacets.make("fieldEditor", BString.make("workbench:CertificateAliasFE")),
            BFacets.make("uxFieldEditor", BString.make("webEditors:CertificateAliasEditor"))
         ),
         BFacets.make("purposeId", BString.make("CLIENT_CERT"))
      )
   );
   public static final Property certAliasAndPassword = newProperty(4, BCertificateAliasAndPassword.DEFAULT, BFacets.make("security", BBoolean.TRUE));
   public static final Property userAuthenticationMode = newProperty(64, BOpcUserAuthenticationMode.DEFAULT, null);
   public static final Property userName = newProperty(64, "", null);
   public static final Property password = newProperty(64, BPassword.DEFAULT, null);
   public static final Property userAuthenticationCertificate = newProperty(4, BCertificateAliasAndPassword.DEFAULT, BFacets.make("security", BBoolean.TRUE));
   public static final Property serverState = newProperty(67, BServerState.Unknown, null);
   public static final Property serverCurrentTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property serverStartTime = newProperty(3, BAbsTime.NULL, null);
   public static final Property timeoutInterval = newProperty(
      0, BRelTime.make(30000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.make(1L)))
   );
   public static final Property statusCheckTimeout = newProperty(
      0, BRelTime.make(10000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.make(1L)))
   );
   public static final Property operationLimit = newProperty(
      4, BRelTime.make(10000L), BFacets.make(BFacets.make("showMilliseconds", true), BFacets.make("min", BRelTime.make(1L)))
   );
   public static final Property serverInfo = newProperty(1, new BOpcUaBuildInfo(), null);
   public static final Property AlarmExt = newProperty(1, new BOpcUaClientAlarmDeviceExt(), null);
   public static final Property pollFrequency = newProperty(4, BPollFrequency.normal, null);
   public static final Property points = newProperty(0, new BOpcUaClientPointDeviceExt(), null);
   public static final Property histories = newProperty(0, new BOpcUaClientHistoryDeviceExt(), null);
   public static final Property lastLearnedInfo = newProperty(7, "", null);
   public static final Property learnRootNodeIdString = newProperty(6, ObjectIdentifiers.RootFolder.toString(), null);
   public static final Property initialHistoryArchiveFromDate = newProperty(0, BAbsTime.DEFAULT, null);
   public static final Action learn = newAction(16, null);
   public static final Action resetComm = newAction(16, BInteger.make(60), null);
   public static final Action updateNamespaceUri = newAction(20, null);
   public static final Action resetDelay = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BOpcUaDevice.class);
   private boolean initialized;
   protected final UaClientListener clientListener = new OpcUaClientListener();
   protected final OpcUaServerStatusListener serverStatusListener = new OpcUaServerStatusListener(this);
   public UaClient uaClient;
   private int lastLearnCount = 0;
   private Ticket commResetTicket;
   private BOpcUaNodeLearnEntry[] descendants;
   public static final Logger logger = Logger.getLogger("opcUaClient.client");
   public static final Lexicon lex = Lexicon.make(BOpcUaDevice.class);
   public static final String SERVER_ROOT_COMPONENT_NAME = "serverRoot";
   public static final String APP_NAME = "NiagaraOpcUaClient";
   public static final Pattern OPCUA_IPV4_PATTERN = Pattern.compile("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b");
   private static final String OPC_TCP = "opc.tcp";
   public static final String SERVER_KEY_PASSWORD_SLOT_NAME = "serverKeyPassword";
   private static final String WRONG_KEY_USED_FOR_DECRYPTION_ERROR_MESSAGE = "Given final block not properly padded.";

   public BNPollScheduler getPollScheduler() {
      return (BNPollScheduler)this.get(pollScheduler);
   }

   public void setPollScheduler(BNPollScheduler v) {
      this.set(pollScheduler, v, null);
   }

   public String getServerEndpointUrl() {
      return this.getString(serverEndpointUrl);
   }

   public void setServerEndpointUrl(String v) {
      this.setString(serverEndpointUrl, v, null);
   }

   public BSecurityMode getSecurityMode() {
      return (BSecurityMode)this.get(securityMode);
   }

   public void setSecurityMode(BSecurityMode v) {
      this.set(securityMode, v, null);
   }

   @Deprecated
   public String getCertificate() {
      return this.getString(certificate);
   }

   @Deprecated
   public void setCertificate(String v) {
      this.setString(certificate, v, null);
   }

   public BCertificateAliasAndPassword getCertAliasAndPassword() {
      return (BCertificateAliasAndPassword)this.get(certAliasAndPassword);
   }

   public void setCertAliasAndPassword(BCertificateAliasAndPassword v) {
      this.set(certAliasAndPassword, v, null);
   }

   public BOpcUserAuthenticationMode getUserAuthenticationMode() {
      return (BOpcUserAuthenticationMode)this.get(userAuthenticationMode);
   }

   public void setUserAuthenticationMode(BOpcUserAuthenticationMode v) {
      this.set(userAuthenticationMode, v, null);
   }

   public String getUserName() {
      return this.getString(userName);
   }

   public void setUserName(String v) {
      this.setString(userName, v, null);
   }

   public BPassword getPassword() {
      return (BPassword)this.get(password);
   }

   public void setPassword(BPassword v) {
      this.set(password, v, null);
   }

   public BCertificateAliasAndPassword getUserAuthenticationCertificate() {
      return (BCertificateAliasAndPassword)this.get(userAuthenticationCertificate);
   }

   public void setUserAuthenticationCertificate(BCertificateAliasAndPassword v) {
      this.set(userAuthenticationCertificate, v, null);
   }

   public BServerState getServerState() {
      return (BServerState)this.get(serverState);
   }

   public void setServerState(BServerState v) {
      this.set(serverState, v, null);
   }

   public BAbsTime getServerCurrentTime() {
      return (BAbsTime)this.get(serverCurrentTime);
   }

   public void setServerCurrentTime(BAbsTime v) {
      this.set(serverCurrentTime, v, null);
   }

   public BAbsTime getServerStartTime() {
      return (BAbsTime)this.get(serverStartTime);
   }

   public void setServerStartTime(BAbsTime v) {
      this.set(serverStartTime, v, null);
   }

   public BRelTime getTimeoutInterval() {
      return (BRelTime)this.get(timeoutInterval);
   }

   public void setTimeoutInterval(BRelTime v) {
      this.set(timeoutInterval, v, null);
   }

   public BRelTime getStatusCheckTimeout() {
      return (BRelTime)this.get(statusCheckTimeout);
   }

   public void setStatusCheckTimeout(BRelTime v) {
      this.set(statusCheckTimeout, v, null);
   }

   public BRelTime getOperationLimit() {
      return (BRelTime)this.get(operationLimit);
   }

   public void setOperationLimit(BRelTime v) {
      this.set(operationLimit, v, null);
   }

   public BOpcUaBuildInfo getServerInfo() {
      return (BOpcUaBuildInfo)this.get(serverInfo);
   }

   public void setServerInfo(BOpcUaBuildInfo v) {
      this.set(serverInfo, v, null);
   }

   public BOpcUaClientAlarmDeviceExt getAlarmExt() {
      return (BOpcUaClientAlarmDeviceExt)this.get(AlarmExt);
   }

   public void setAlarmExt(BOpcUaClientAlarmDeviceExt v) {
      this.set(AlarmExt, v, null);
   }

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public BOpcUaClientPointDeviceExt getPoints() {
      return (BOpcUaClientPointDeviceExt)this.get(points);
   }

   public void setPoints(BOpcUaClientPointDeviceExt v) {
      this.set(points, v, null);
   }

   public BOpcUaClientHistoryDeviceExt getHistories() {
      return (BOpcUaClientHistoryDeviceExt)this.get(histories);
   }

   public void setHistories(BOpcUaClientHistoryDeviceExt v) {
      this.set(histories, v, null);
   }

   public String getLastLearnedInfo() {
      return this.getString(lastLearnedInfo);
   }

   public void setLastLearnedInfo(String v) {
      this.setString(lastLearnedInfo, v, null);
   }

   public String getLearnRootNodeIdString() {
      return this.getString(learnRootNodeIdString);
   }

   public void setLearnRootNodeIdString(String v) {
      this.setString(learnRootNodeIdString, v, null);
   }

   public BAbsTime getInitialHistoryArchiveFromDate() {
      return (BAbsTime)this.get(initialHistoryArchiveFromDate);
   }

   public void setInitialHistoryArchiveFromDate(BAbsTime v) {
      this.set(initialHistoryArchiveFromDate, v, null);
   }

   public void learn() {
      this.invoke(learn, null, null);
   }

   public void resetComm(BInteger parameter) {
      this.invoke(resetComm, parameter, null);
   }

   public void updateNamespaceUri() {
      this.invoke(updateNamespaceUri, null, null);
   }

   public void resetDelay() {
      this.invoke(resetDelay, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getNetworkType() {
      return BOpcUaNetwork.TYPE;
   }

   public void started() throws Exception {
      super.started();
      if (!Flags.has(this, certificate, 268435456)) {
         this.getCertAliasAndPassword().setAlias(this.getCertificate());
         this.getCertAliasAndPassword().setPassword(BPassword.DEFAULT);
         Flags.add(this, certificate, null, new int[]{268435461});
      }

      if (!Flags.has(this, certAliasAndPassword, 268435456)) {
         Flags.add(this, certAliasAndPassword, null, new int[]{268435456});
         this.setFlags(certAliasAndPassword, this.getFlags(certAliasAndPassword) & -5);
         this.getCertAliasAndPassword().setFacets(BCertificateAliasAndPassword.alias, BFacets.make("purposeId", KeyPurpose.CLIENT_CERT.name()));
      }

      if (!Flags.has(this, userAuthenticationCertificate, 268435456)) {
         Flags.add(this, userAuthenticationCertificate, null, new int[]{268435456});
         this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) & -5);
         this.getUserAuthenticationCertificate().setFacets(BCertificateAliasAndPassword.alias, BFacets.make("purposeId", KeyPurpose.CLIENT_CERT.name()));
      }

      Slot serverKeyPasswordSlot = this.getSlot("serverKeyPassword");
      if (serverKeyPasswordSlot != null) {
         if (serverKeyPasswordSlot.isProperty()) {
            this.getCertAliasAndPassword().setPassword((BPassword)this.get("serverKeyPassword"));
         }

         this.remove("serverKeyPassword");
      }

      BOpcUaNetwork opcUaNetwork = this.getOpcUaClientNetwork();
      if (!opcUaNetwork.isDisabled() && !opcUaNetwork.isDown() && !opcUaNetwork.isFault()) {
         if (BOpcUserAuthenticationMode.userNameAndPassword.equals(this.getUserAuthenticationMode())) {
            this.setFlags(userName, this.getFlags(userName) & -5);
            this.setFlags(password, this.getFlags(password) & -5);
            this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) | 4);
         } else if (BOpcUserAuthenticationMode.certificate.equals(this.getUserAuthenticationMode())) {
            this.setFlags(userName, this.getFlags(userName) | 4);
            this.setFlags(password, this.getFlags(password) | 4);
            this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) & -5);
         } else {
            if (!BOpcUserAuthenticationMode.anonymous.equals(this.getUserAuthenticationMode())) {
               throw new IllegalArgumentException("Unknown authentication mode: " + this.getUserAuthenticationMode().getTag());
            }

            this.setFlags(userName, this.getFlags(userName) | 4);
            this.setFlags(password, this.getFlags(password) | 4);
            this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) | 4);
         }

         if (securityMode.getDefaultDisplayName(null).equals(this.getDisplayName(securityMode, null))) {
            this.setDisplayName(securityMode, BFormat.make(lex.getText("opcUaClient.securityMode.displayName")), null);
         }

         try {
            OpcUaClientUtil.connect(this);
         } catch (Exception var4) {
            logException(Level.SEVERE, "Connection failed while starting device: " + this.getName() + ": " + var4, var4);
            throw var4;
         }

         this.initDeviceAfterConnect(null);
      }
   }

   public void stopped() throws Exception {
      if (this.commResetTicket != null) {
         this.commResetTicket.cancel();
         this.commResetTicket = null;
      }

      OpcUaClientUtil.disconnect(this);
      super.stopped();
   }

   public void changed(Property p, Context cx) {
      boolean subsPoints = false;
      if (!this.isRunning() || Context.decoding.equals(cx)) {
         super.changed(p, cx);
      } else if (!this.getNetwork().getEnabled()) {
         try {
            OpcUaClientUtil.disconnect(this);
         } catch (Exception var9) {
            logException(Level.WARNING, "Unable to disconnect client when network is disabled: " + var9, var9);
         }

         super.changed(p, cx);
      } else {
         if (p.equals(securityMode)
            || p.equals(timeoutInterval)
            || p.equals(statusCheckTimeout)
            || p.equals(userName)
            || p.equals(password)
            || p.equals(certAliasAndPassword)
            || p.equals(userAuthenticationCertificate)) {
            try {
               OpcUaClientUtil.disconnect(this);
               OpcUaClientUtil.connect(this);
               subsPoints = true;
            } catch (Exception var14) {
               logException(Level.SEVERE, "Connection failed while changing security parameters for device: " + this.getName() + ": " + var14, var14);
            }
         }

         if (p.equals(serverEndpointUrl)) {
            try {
               OpcUaClientUtil.disconnect(this);
               OpcUaClientUtil.connect(this);
               subsPoints = true;
            } catch (Exception var13) {
               logException(Level.SEVERE, "Connection failed while changing endpoint URL for device: " + this.getName() + ": " + var13, var13);
            }
         }

         if (p == userAuthenticationMode) {
            if (BOpcUserAuthenticationMode.certificate.equals(this.getUserAuthenticationMode())) {
               this.getUserAuthenticationCertificate().setAlias(BCertificateAliasAndPassword.DEFAULT.getAlias());
               this.getUserAuthenticationCertificate().setPassword(BCertificateAliasAndPassword.DEFAULT.getPassword());
               this.setFlags(userName, this.getFlags(userName) | 4, cx);
               this.setFlags(password, this.getFlags(password) | 4, cx);
               this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) & -5, cx);
            } else if (BOpcUserAuthenticationMode.userNameAndPassword.equals(this.getUserAuthenticationMode())) {
               this.setUserName("");
               this.setPassword(BPassword.DEFAULT);
               this.setFlags(userName, this.getFlags(userName) & -5, cx);
               this.setFlags(password, this.getFlags(password) & -5, cx);
               this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) | 4, cx);
            } else {
               if (!BOpcUserAuthenticationMode.anonymous.equals(this.getUserAuthenticationMode())) {
                  throw new IllegalArgumentException("Unknown authentication mode: " + this.getUserAuthenticationMode().getTag());
               }

               this.setFlags(userName, this.getFlags(userName) | 4, cx);
               this.setFlags(password, this.getFlags(password) | 4, cx);
               this.setFlags(userAuthenticationCertificate, this.getFlags(userAuthenticationCertificate) | 4, cx);
            }

            try {
               OpcUaClientUtil.disconnect(this);
               OpcUaClientUtil.connect(this);
               subsPoints = true;
            } catch (Exception var12) {
               logException(Level.SEVERE, "Connection failed while changing user authentication mode for device: " + this.getName() + ": " + var12, var12);
            }
         }

         if (p.equals(serverState)) {
            BServerState state = this.getServerState();
            if (state.equals(BServerState.Running)) {
               if (this.uaClient == null || !this.uaClient.isConnected()) {
                  try {
                     OpcUaClientUtil.connect(this);
                  } catch (Exception var11) {
                     logException(Level.SEVERE, "Connection failed while changing server state for device: " + this.getName() + ": " + var11, var11);
                  }
               }

               subsPoints = true;
            } else {
               this.pingFail(lex.getText("opcUaClient.pingFailServerStateNotRunning.serverState", new Object[]{state.getTag()}));

               for (BOpcUaClientProxyExt proxyExt : (BOpcUaClientProxyExt[])CompUtil.getDescendants(this.getPoints(), BOpcUaClientProxyExt.class)) {
                  proxyExt.setUaStatusCode(StatusCode.BAD.toString());
               }
            }
         }

         if (p.equals(enabled)) {
            try {
               if (!this.getEnabled()) {
                  OpcUaClientUtil.disconnect(this);
               } else {
                  OpcUaClientUtil.connect(this);
                  subsPoints = true;
               }
            } catch (Exception var10) {
               this.setServerState(BServerState.Unknown);
               logException(Level.SEVERE, "Connection failed while changing enabled state for device: " + this.getName() + ": " + var10, var10);
            }
         }

         if (subsPoints) {
            this.initDeviceAfterConnect(cx);
         }

         super.changed(p, cx);
      }
   }

   private void initDeviceAfterConnect(Context cx) {
      this.getAlarmExt().init();
   }

   public void setCertificateValidator() {
      OpcUaClientCertificateValidator validator = new OpcUaClientCertificateValidator(this.uaClient.getAddress());
      this.uaClient.setCertificateValidator(validator);
   }

   public void initialize() throws Exception {
      this.initializing();
      long startMillis = 0L;
      if (logger.isLoggable(Level.FINE)) {
         startMillis = System.currentTimeMillis();
         logger.fine("OpcUaDevice initializing for " + this.getServerEndpointUrl());
      }

      this.uaClient.setSessionName("NiagaraOpcUaClient");
      this.uaClient.getEndpointConfiguration().setMaxByteStringLength(Integer.MAX_VALUE);
      this.uaClient.getEndpointConfiguration().setMaxArrayLength(Integer.MAX_VALUE);
      this.uaClient.setLocale(Locale.ENGLISH);
      this.uaClient.setTimeout(this.getTimeoutInterval().getMillis());
      this.uaClient.setStatusCheckTimeout(this.getStatusCheckTimeout().getMillis());
      if (!this.uaClient.getOperationLimits().equals(OperationLimits.allOf(UnsignedInteger.valueOf(this.getOperationLimit().getMillis())))) {
         this.uaClient.setOperationLimits(OperationLimits.allOf(UnsignedInteger.valueOf(this.getOperationLimit().getMillis())));
      }

      this.uaClient.setListener(this.clientListener);
      this.uaClient.addServerStatusListener(this.serverStatusListener);
      this.setCertificateValidator();
      SecurityMode securityMode = this.getSecurityMode().getSecurityMode();
      this.uaClient.setSecurityMode(securityMode);
      if (!securityMode.equals(SecurityMode.NONE)) {
         this.uaClient
            .getHttpsSettings()
            .setHttpsSecurityPolicies(new HttpsSecurityPolicy[]{HttpsSecurityPolicy.TLS_1_0, HttpsSecurityPolicy.TLS_1_1, HttpsSecurityPolicy.TLS_1_2});

         try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
               BCertificateAliasAndPassword aliasAndPassword = this.getCertAliasAndPassword();
               ICryptoManager cryptoManager = CertManagerFactory.getInstance();
               IKeyStore keyStore = cryptoManager.getKeyStore();
               X509Certificate clientCert = keyStore.getCertificate(aliasAndPassword.getAlias());
               PrivKey privKey = new PrivKey(getServerPrivateKey(keyStore, aliasAndPassword));
               ApplicationIdentity applicationIdentity = new ApplicationIdentity(new Cert(clientCert), privKey);
               ApplicationDescription appDescription = new ApplicationDescription();
               appDescription.setApplicationName(new LocalizedText("NiagaraOpcUaClient@localhost"));
               appDescription.setApplicationUri("urn:localhost:OPCUA:NiagaraOpcUaClient");
               appDescription.setProductUri("urn:tridium.com:OPCUA:NiagaraOpcUaClient");
               appDescription.setApplicationType(ApplicationType.Client);
               applicationIdentity.setApplicationDescription(appDescription, true);
               this.uaClient.setApplicationIdentity(applicationIdentity);
               return null;
            }));
         } catch (PrivilegedActionException var7) {
            logger.log(Level.SEVERE, "Exception occurred while creating OPCUA application identity: " + var7.getException().getMessage());
            throw var7.getException();
         }
      }

      try {
         this.checkAuthenticationMode(this.uaClient, this.getUserAuthenticationCertificate());
      } catch (PrivilegedActionException var6) {
         logger.log(Level.SEVERE, "Exception occurred while creating OPCUA user identity: " + var6.getException().getMessage());
         throw var6.getException();
      }

      this.initializationComplete();
      if (logger.isLoggable(Level.FINE)) {
         long endMillis = System.currentTimeMillis();
         logger.fine("OpcUaDevice initialization complete for " + this.getServerEndpointUrl() + " (" + (endMillis - startMillis) + "ms)");
      }
   }

   private void checkAuthenticationMode(UaClient client, BCertificateAliasAndPassword userAuthCert) throws SessionActivationException, PrivilegedActionException {
      if (BOpcUserAuthenticationMode.userNameAndPassword.equals(this.getUserAuthenticationMode())) {
         String username = this.getUserName().trim();
         String password = this.getPassword().getValue().trim();
         if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            logger.log(Level.WARNING, "Username/Password invalid!");
         }

         client.setUserIdentity(new UserIdentity(username, password));
      } else if (BOpcUserAuthenticationMode.certificate.equals(this.getUserAuthenticationMode())) {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            ICryptoManager cryptoManager = CertManagerFactory.getInstance();
            IKeyStore keyStore = cryptoManager.getKeyStore();
            X509Certificate userAuthCertificate = keyStore.getCertificate(userAuthCert.getAlias());
            PrivKey userAuthPrivateKey = new PrivKey(getServerPrivateKey(keyStore, userAuthCert));
            client.setUserIdentity(new UserIdentity(new Cert(userAuthCertificate), userAuthPrivateKey));
            return null;
         }));
      } else {
         if (!BOpcUserAuthenticationMode.anonymous.equals(this.getUserAuthenticationMode())) {
            throw new IllegalArgumentException("Unknown authentication mode: " + this.getUserAuthenticationMode().getTag());
         }

         client.setUserIdentity(new UserIdentity());
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      return this.postAsync(new Invocation(this, action, arg, cx));
   }

   public synchronized void doPing() {
      try {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine("OpcUaDevice doPing() to " + this.getServerEndpointUrl());
         }

         if (!this.getNetwork().getEnabled()) {
            this.pingFail(lex.getText("opcUaClient.pingFailNetworkDisabled"));
            return;
         }

         if (!this.getEnabled()) {
            this.pingFail(lex.getText("opcUaClient.pingFailDeviceDisabled"));
            return;
         }

         if (this.commResetTicket != null) {
            this.pingFail(lex.getText("opcUaClient.pingFailCommReset"));
            return;
         }

         if (this.getServerState().equals(BServerState.Shutdown)) {
            return;
         }

         if (this.uaClient != null && this.isInitialized()) {
            try {
               AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
                  if (!this.uaClient.isConnected()) {
                     long startMillis = 0L;
                     if (logger.isLoggable(Level.FINE)) {
                        startMillis = System.currentTimeMillis();
                        logger.fine("OpcUaDevice connecting to " + this.getServerEndpointUrl());
                     }

                     this.uaClient.connect();
                     if (logger.isLoggable(Level.FINE)) {
                        long endMillis = System.currentTimeMillis();
                        logger.fine("OpcUaDevice connection complete to " + this.getServerEndpointUrl() + " (" + (endMillis - startMillis) + "ms)");
                     }
                  }

                  DataValue value = this.uaClient.readValue(VariableIdentifiers.Server_ServerStatus_State);
                  DataValue curTime = this.uaClient.readValue(VariableIdentifiers.Server_ServerStatus_CurrentTime);
                  DataValue startTime = this.uaClient.readValue(VariableIdentifiers.Server_ServerStatus_StartTime);
                  this.setServerCurrentTime(OpcUaClientUtil.dataValueToAbsTime(curTime));
                  this.setServerStartTime(OpcUaClientUtil.dataValueToAbsTime(startTime));
                  this.pingOk();
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine("ping successful with appURI: " + this.uaClient.getApplicationIdentity().getApplicationDescription().getApplicationUri());
                  }

                  return null;
               }));
            } catch (PrivilegedActionException var5) {
               throw var5.getException();
            }
         } else {
            this.pingFail(lex.getText("opcUaClient.pingFailDeviceUninitialized"));
         }
      } catch (Exception var6) {
         String appUri = null;

         try {
            appUri = this.uaClient.getApplicationIdentity().getApplicationDescription().getApplicationUri();
         } catch (Exception var4) {
            appUri = "<unknown>";
         }

         logException(Level.SEVERE, "Exception occurred during ping for device: " + this.getName() + " with appURI: " + appUri + ": " + var6, var6);
         this.pingFail(OpcUaCoreUtil.getLocalizedMessage(var6));
      }
   }

   public void doPoll() {
   }

   public void doResetComm(BInteger delay) {
      try {
         if (this.commResetTicket == null) {
            this.commResetTicket = Clock.schedule(this, BRelTime.makeSeconds(delay.getInt()), resetDelay, null);
            OpcUaClientUtil.disconnect(this);
         }
      } catch (Exception var3) {
         logException(Level.SEVERE, "Exception occurred while resetting communications for device: " + this.getName() + ": " + var3, var3);
      }
   }

   public void doResetDelay() {
      this.commResetTicket = null;

      try {
         OpcUaClientUtil.connect(this);
      } catch (Exception var2) {
         logException(Level.SEVERE, "Connection failed while resetting delay for device: " + this.getName() + ": " + var2, var2);
      }
   }

   public void doLearn() throws Exception {
      this.doLearn(null);
   }

   public void doLearn(BNDiscoveryJob job) throws Exception {
      this.getPoints().setDiscoveryFailCause("");

      try {
         if (this.uaClient == null || !this.uaClient.isConnected()) {
            OpcUaClientUtil.connect(this);
         }

         AddressSpace addressSpace = this.uaClient.getAddressSpace();
         addressSpace.setMaxReferencesPerNode(1000);
         addressSpace.setReferenceTypeId(ReferenceTypeIdentifiers.HierarchicalReferences);
         NodeId learnBaseNodeId = NodeId.parseNodeId(this.getLearnRootNodeIdString());

         String learnBaseRootEntryName;
         try {
            UaNode node = OpcUaClientUtil.getAddressSpaceNode(this.uaClient.getAddressSpace(), learnBaseNodeId);
            learnBaseRootEntryName = node.getBrowseName().toString();
         } catch (Exception var12) {
            logException(Level.SEVERE, "Failed to determine learn root node for Node ID: " + this.getLearnRootNodeIdString() + ": " + var12, var12);
            if (job != null) {
               job.log().message(BOpcUaNetwork.lex.getText("clientPointLearn.noRootObject", new Object[]{this.getLearnRootNodeIdString()}));
            }

            throw var12;
         }

         BOpcUaNodeLearnEntry learnRoot = new BOpcUaNodeLearnEntry(learnBaseRootEntryName, learnBaseNodeId.toString(), "", "", BTypeSpec.DEFAULT);
         if (this.get("serverRoot") != null) {
            this.remove("serverRoot");
         }

         this.add("serverRoot", learnRoot, 2);
         int expectedCount = this.lastLearnCount <= 0 ? 3000 : this.lastLearnCount;
         BOpcUaClientPointDiscoveryPreferences preferences = (BOpcUaClientPointDiscoveryPreferences)this.getPoints().getDiscoveryPreferences();
         this.lastLearnCount = OpcUaClientUtil.learn(
            this, learnRoot, learnBaseNodeId, job, 0, expectedCount, preferences.getExcludeServer(), preferences.getExcludeTypesFolder()
         );
         this.descendants = (BOpcUaNodeLearnEntry[])CompUtil.getDescendants(learnRoot, BOpcUaNodeLearnEntry.class);

         for (BOpcUaNodeLearnEntry descendant : this.descendants) {
            descendant.updateFacets();
         }
      } catch (Exception var13) {
         logException(Level.SEVERE, "Exception occurred during learn for device: " + this.getName() + ": " + var13, var13);
         throw var13;
      }

      String failCause = this.getPoints().getDiscoveryFailCause();
      if (!failCause.isEmpty()) {
         throw new RuntimeException(failCause);
      }
   }

   public void doUpdateNamespaceUri() throws Exception {
      this.doLearn(this.getPoints().getDiscoveryPreferences().getJob());

      for (BOpcUaClientProxyExt proxyExt : (BOpcUaClientProxyExt[])CompUtil.getDescendants(this.getPoints(), BOpcUaClientProxyExt.class)) {
         this.updateNamespaceUri(proxyExt, this.descendants);
      }
   }

   private void updateNamespaceUri(BOpcUaClientProxyExt proxyExt, BOpcUaNodeLearnEntry[] descendants) {
      if (proxyExt.getNameSpaceUri().isEmpty() || proxyExt.getNameSpaceUri() == null) {
         try {
            for (BOpcUaNodeLearnEntry descendant : descendants) {
               if (descendant.getUaNodeId().equals(proxyExt.getUaNodeId())) {
                  proxyExt.setNameSpaceUri(descendant.getNameSpaceUri());
               }
            }
         } catch (Exception var7) {
            logException(Level.SEVERE, "Exception occurred while setting NamespaceUri for node: " + proxyExt.getUaNodeId() + ": " + var7, var7);
         }
      }
   }

   public BOpcUaLearnBase[] getLearnedPoints() {
      BNDiscoveryPreferences prefs = this.getPoints().getDiscoveryPreferences();
      boolean showAddableOnly = prefs instanceof BOpcUaClientPointDiscoveryPreferences
         ? ((BOpcUaClientPointDiscoveryPreferences)prefs).getShowAddableOnly()
         : false;
      return this.getLearnedPoints(showAddableOnly);
   }

   public BOpcUaLearnBase[] getLearnedPoints(boolean showAddableOnly) {
      BComponent root = this.get("serverRoot").asComponent();
      return !(root instanceof BOpcUaNodeLearnEntry)
         ? new BOpcUaLearnBase[0]
         : new BOpcUaLearnBase[]{((BOpcUaNodeLearnEntry)root).toDiscoveryTree(showAddableOnly)};
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      if (this.isRunning() && this.uaClient != null) {
         out.startProps();
         out.trTitle("uaClient subscriptions ", 2);

         for (Subscription subscription : this.uaClient.getSubscriptions()) {
            out.prop("subscription", subscription.getPublishingInterval());

            for (MonitoredItemBase monitoredItem : subscription.getItems()) {
               out.prop("", monitoredItem.toString());
            }
         }

         out.endProps();
      }
   }

   public final BOpcUaNetwork getOpcUaClientNetwork() {
      return (BOpcUaNetwork)this.getNetwork();
   }

   public boolean isCommReset() {
      return this.commResetTicket != null;
   }

   public void cancelCommReset() {
      if (this.commResetTicket != null) {
         this.commResetTicket.cancel();
         this.commResetTicket = null;
      }
   }

   public void validateSet(Validatable validatable, Context context) {
      BString proposedValue = (BString)validatable.getProposedValue(serverEndpointUrl);
      if (proposedValue != null
         && !proposedValue.getString().startsWith("opc.tcp")
         && !OPCUA_IPV4_PATTERN.matcher(proposedValue.getString()).matches()
         && !proposedValue.getString().isEmpty()) {
         throw new LocalizableRuntimeException("opcUaClient", "opcUaClient.endPoint", new Object[]{proposedValue.getString(), this.getName()});
      }
   }

   public void validateSet(BComplex instance, Property property, BValue newValue, Context context) {
      if (property != null
         && property.equals(serverEndpointUrl)
         && newValue != null
         && !((BString)newValue).getString().startsWith("opc.tcp")
         && !OPCUA_IPV4_PATTERN.matcher(((BString)newValue).getString()).matches()
         && !((BString)newValue).getString().isEmpty()) {
         throw new LocalizableRuntimeException("opcUaClient", "opcUaClient.endPoint", new Object[]{((BString)newValue).getString(), this.getName()});
      }
   }

   public IPropertyValidator getPropertyValidator(Property property, Context context) {
      return this;
   }

   public IPropertyValidator getPropertyValidator(Property[] properties, Context context) {
      return this;
   }

   private static RSAPrivateKey getServerPrivateKey(IKeyStore keyStore, BCertificateAliasAndPassword aliasAndPassword) throws Exception {
      if (aliasAndPassword.getPassword().isDefault()) {
         try {
            return (RSAPrivateKey)keyStore.getKey(aliasAndPassword.getAlias(), null);
         } catch (Exception var18) {
            if (var18.getMessage() != null && !var18.getMessage().isEmpty() && var18.getMessage().startsWith("Given final block not properly padded.")) {
               throw new Exception(lex.getText("opcUaClient.device.error.incorrectCertificateKeyPassword.details", new Object[]{var18.getLocalizedMessage()}));
            } else {
               throw var18;
            }
         }
      } else {
         try {
            SecretChars passwordChars = AccessController.doPrivileged(aliasAndPassword.getPassword()::getSecretChars);
            Throwable var3 = null;

            RSAPrivateKey var4;
            try {
               var4 = (RSAPrivateKey)keyStore.getKey(aliasAndPassword.getAlias(), passwordChars.get());
            } catch (Throwable var15) {
               var3 = var15;
               throw var15;
            } finally {
               if (passwordChars != null) {
                  if (var3 != null) {
                     try {
                        passwordChars.close();
                     } catch (Throwable var14) {
                        var3.addSuppressed(var14);
                     }
                  } else {
                     passwordChars.close();
                  }
               }
            }

            return var4;
         } catch (Exception var17) {
            if (var17.getMessage() != null && !var17.getMessage().isEmpty() && var17.getMessage().startsWith("Given final block not properly padded.")) {
               throw new Exception(lex.getText("opcUaClient.device.error.incorrectCertificateKeyPassword.details", new Object[]{var17.getLocalizedMessage()}));
            } else {
               throw var17;
            }
         }
      }
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   private void initializing() {
      this.initialized = false;
   }

   private void initializationComplete() {
      this.initialized = true;
   }

   private static void logException(Level level, String msg, Exception e) {
      if (logger.isLoggable(Level.FINE)) {
         logger.log(level, msg, (Throwable)e);
      } else {
         logger.log(level, msg);
      }
   }
}
