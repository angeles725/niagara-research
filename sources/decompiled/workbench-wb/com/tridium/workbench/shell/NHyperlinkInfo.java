package com.tridium.workbench.shell;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.FoxSession;
import com.tridium.fox.sys.BFoxClientConnection;
import com.tridium.fox.sys.BFoxConnection;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.user.BUserChannel;
import com.tridium.ui.Binder;
import com.tridium.util.BISubstitutableOrdScheme;
import com.tridium.util.CompUtil;
import com.tridium.workbench.nav.BFileMenuAgent;
import com.tridium.workbench.pathbar.BNavFilePA;
import com.tridium.workbench.pathbar.BPathBarAgent;
import com.tridium.workbench.web.browser.BWebBrowser;
import com.tridium.workbench.web.browser.BWebBrowserView;
import com.tridium.workbench.web.browser.BWebWidget;
import com.tridium.workbench.web.browser.WebWidgetUtil;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.agent.BAbstractPxView;
import javax.baja.gx.BImage;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdScheme;
import javax.baja.naming.BViewScheme;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.naming.ViewQuery;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavFileSpace;
import javax.baja.nre.util.Array;
import javax.baja.security.AuthenticationException;
import javax.baja.security.BICredentials;
import javax.baja.security.BPassword;
import javax.baja.security.BPasswordCache;
import javax.baja.security.BPermissions;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.security.PermissionException;
import javax.baja.space.BSpace;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.ui.BDialog;
import javax.baja.ui.BHyperlinkMode;
import javax.baja.ui.BLabel;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.user.BPasswordStrength;
import javax.baja.util.Lexicon;
import javax.baja.web.BIFormFactorMax;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.WbSys;
import javax.baja.workbench.px.BWbPxView;
import javax.baja.workbench.view.BWbView;

public class NHyperlinkInfo extends HyperlinkInfo {
   static final AgentFilter pathBarAgentFilter = AgentFilter.is(BPathBarAgent.TYPE);
   static Logger log = Logger.getLogger("wb.hyperlink");
   static BPathBarAgent[] noPathBarAgents = new BPathBarAgent[0];
   private static final boolean DISABLE_HYPERLINK_ORD_SUBSTITUTION = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.hyperlink.ord.substitution.disabled"))
   );
   BWbProfile profile;
   BOrd currentOrd;
   boolean appendToHistory;
   BICredentials credentials;
   BNiagaraWbShell shell;
   BViewTab tab;
   WbHistory.Entry historyEntry;
   BImage icon;
   OrdQuery[] queries;
   boolean hasParent;
   OrdTarget target;
   BPermissions permissions;
   AgentList viewAgents;
   AgentInfo agent;
   BWbView view;
   Context context;
   BPathBarAgent[] pathBarAgents;
   String fault;

   public static NHyperlinkInfo make(HyperlinkInfo info) {
      return info instanceof NHyperlinkInfo ? (NHyperlinkInfo)info : new NHyperlinkInfo(info.getOrd(), info.getMode(), true, null);
   }

   public NHyperlinkInfo(BOrd ord, BHyperlinkMode mode, boolean appendToHistory) {
      this(ord, mode, appendToHistory, null);
   }

   public NHyperlinkInfo(BOrd ord, BHyperlinkMode mode, boolean appendToHistory, BICredentials credentials) {
      super(ord, mode);
      this.appendToHistory = appendToHistory;
      this.credentials = credentials;
      if (log.isLoggable(Level.FINE)) {
         log.fine("--- NHyperlinkInfo(" + ord + ", " + mode + ", " + appendToHistory + ", " + credentials + ") ---");
      }
   }

   public void hyperlink(BViewTab tab) throws Throwable {
      this.tab = tab;
      this.shell = tab.shell;
      this.profile = this.shell.profile;

      try {
         this.currentOrd = tab.ord;
         this.ord = this.normalizeOrd();

         try {
            this.target = this.resolve();
            if (!DISABLE_HYPERLINK_ORD_SUBSTITUTION) {
               Optional<BISubstitutableOrdScheme> substitutableOrdScheme = BISubstitutableOrdScheme.findSubstitutableOrdScheme(this.ord);
               if (substitutableOrdScheme.isPresent()) {
                  this.ord = substitutableOrdScheme.get().convertToSubstituteOrd(this.ord, this.target, BISubstitutableOrdScheme.SUBSTITUTE_NAV_ORD_FACET);
               }
            }
         } finally {
            this.hasParent = this.hasParent();
            this.historyEntry = this.appendToHistory();
            this.queries = this.parseOrd();
         }

         this.icon = this.getIcon();
         this.permissions = this.getPermissions();
         this.viewAgents = this.getViewAgentsList();
         this.agent = this.getAgent();
         this.view = this.getView();
         this.context = this.getContext();
         this.pathBarAgents = this.makePathBarAgents();
         this.fault = this.getFault();
         this.resetTab();
         this.updateTab();
         this.updateViewTab();
         this.activateView();
         this.loadView();
         this.view = this.profile.customizeView(this.view);
         this.startBinder();
      } finally {
         try {
            this.updateTab();
            tab.updateShell();
         } catch (Exception var13) {
            var13.printStackTrace();
         }
      }
   }

   private void resetTab() {
      if (log.isLoggable(Level.FINE)) {
         log.fine("resetTab: " + this.tab);
      }

      this.tab.stopView(true);
   }

   private BOrd normalizeOrd() {
      BOrd oldOrd = this.ord;
      BOrd newOrd = this.ord;

      try {
         BOrd n = this.ord;
         if (this.currentOrd != null && !this.ord.toString().contains("/module.palette")) {
            n = BOrd.make(this.currentOrd, n);
         }

         newOrd = n.normalize();
      } catch (Throwable var4) {
      }

      if (log.isLoggable(Level.FINE)) {
         log.fine("normalizeOrd: " + oldOrd + " -> " + newOrd);
      }

      return newOrd;
   }

   private boolean hasParent() {
      boolean hasParent = false;

      try {
         hasParent = this.ord.getParent() != null && !this.ord.toString().startsWith("workbench:/");
      } catch (Exception var3) {
         var3.printStackTrace();
      }

      if (log.isLoggable(Level.FINE)) {
         log.fine("hasParent: " + hasParent);
      }

      return hasParent;
   }

   private WbHistory.Entry appendToHistory() throws Exception {
      if (log.isLoggable(Level.FINE)) {
         log.fine("appendToHistory");
      }

      WbHistory.Entry historyEntry = null;
      if (this.appendToHistory) {
         historyEntry = this.tab.history.append(this.ord);
         BNiagaraWbShell.history.recentAdd(historyEntry);
      } else {
         this.tab.history.recentTouch(this.ord);
         BNiagaraWbShell.history.recentTouch(this.ord);
      }

      return historyEntry;
   }

   private OrdQuery[] parseOrd() throws Exception {
      if (log.isLoggable(Level.FINE)) {
         log.fine("parseOrd");
      }

      try {
         return this.ord.parse();
      } catch (Exception var2) {
         throw new LocalizableException("bajaui", "errorPanel.invalidOrd", var2);
      }
   }

   private OrdTarget resolve() throws Exception {
      if (!WbMain.isKioskAccessible(this.ord)) {
         throw new UnresolvedException("Local access denied in kiosk mode");
      } else {
         OrdTarget target = this.shell.resolve(this.ord, this.credentials);
         if (log.isLoggable(Level.FINE)) {
            log.fine("resolved: " + target.get().toDebugString());
         }

         if (target.get() instanceof BComponent) {
            BComponent comp = (BComponent)target.get();
            comp.lease();
         }

         BFoxSession s = null;
         if (target.get() instanceof BComponent && ((BComponent)target.get()).getSession() instanceof BFoxSession) {
            s = (BFoxSession)((BComponent)target.get()).getSession();
         } else if (target.get() instanceof BFoxSession) {
            s = (BFoxSession)target.get();
         } else {
            try {
               BSpace space = target.getSpace();
               if (space != null) {
                  BISession session = space.getSession();
                  if (session instanceof BFoxSession) {
                     s = (BFoxSession)session;
                  }
               }
            } catch (Exception var16) {
               var16.printStackTrace();
            }
         }

         if (s != null) {
            BFoxClientConnection c = s.getConnection();
            FoxSession fs = c.session();
            FoxMessage m = fs.getRemoteWelcome();
            BAbsTime exp = BAbsTime.make(m.getTime("passwordExpires", BAbsTime.NULL.getMillis()));
            boolean resetPassword = m.getBoolean("forceReset", false);
            Lexicon lex = Lexicon.make("fox");
            if (!resetPassword && !exp.isNull() && fs.promptForPasswordReset) {
               if (m.getBoolean("networkUser", false)) {
                  BDialog.open(
                     this.shell,
                     "Password Expiration",
                     lex.getText("fox.network.user.password.expiring", new Object[]{exp}),
                     1,
                     BImage.make("module://icons/x32/keys.png")
                  );
                  fs.promptForPasswordReset = false;
               } else if (!c.getUseFoxs()) {
                  BDialog.open(
                     this.shell,
                     "Password Expiration",
                     lex.getText("fox.insecure.password.expiring", new Object[]{exp}),
                     1,
                     BImage.make("module://icons/x32/keys.png")
                  );
                  fs.promptForPasswordReset = false;
               } else if (BDialog.open(
                     this.shell,
                     "Password Expiration",
                     lex.getText("fox.user.password.expiring", new Object[]{exp}),
                     12,
                     BImage.make("module://icons/x32/keys.png")
                  )
                  == 4) {
                  resetPassword = true;
               } else {
                  fs.promptForPasswordReset = false;
               }
            }

            if (m.getBoolean("illegalNetworkUserState", false)) {
               BDialog.open(
                  this.shell,
                  "Password Expiration",
                  lex.getText("fox.illegal.network.user.passwordReset", new Object[]{exp}),
                  1,
                  BImage.make("module://icons/x32/keys.png")
               );
               this.ensureDisconnect(s);
               throw new AuthenticationException("Cannot reset password.");
            }

            if (!c.getUseFoxs() && m.getBoolean("noInsecureReset", false)) {
               BDialog.open(
                  this.shell,
                  "Password Expiration",
                  lex.getText("fox.login.passwordReset.secureOnly", new Object[]{exp}),
                  1,
                  BImage.make("module://icons/x32/keys.png")
               );
               this.ensureDisconnect(s);
               throw new AuthenticationException("Cannot reset password.");
            }

            if (resetPassword && fs.promptForPasswordReset) {
               boolean passwordReset = false;
               String errMsg = null;
               int loopCount = 0;
               BPasswordStrength passwordStrength = new BPasswordStrength(
                  m.getInt("minLength", ((BInteger)BPasswordStrength.minimumLength.getDefaultValue()).getInt()),
                  m.getInt("minLowerCase", ((BInteger)BPasswordStrength.minimumLowerCase.getDefaultValue()).getInt()),
                  m.getInt("minUpperCase", ((BInteger)BPasswordStrength.minimumUpperCase.getDefaultValue()).getInt()),
                  m.getInt("minDigits", ((BInteger)BPasswordStrength.minimumDigits.getDefaultValue()).getInt()),
                  m.getInt("minSpecial", ((BInteger)BPasswordStrength.minimumSpecial.getDefaultValue()).getInt()),
                  m.getInt("maxLength", ((BInteger)BPasswordStrength.maximumLength.getDefaultValue()).getInt())
               );

               while (!passwordReset) {
                  try {
                     loopCount++;
                     BUserChannel userChannel = ((BFoxConnection)fs.conn()).getChannels().getUserChannel();
                     userChannel.fetchPrefs();
                     BPassword password = BPasswordResetDialog.open(this.shell, passwordStrength, errMsg);
                     if (password == null) {
                        passwordReset = true;
                        throw new AuthenticationException("password not changed as required");
                     }

                     userChannel.setAuthenticator(new BPasswordCache(password), true);
                     fs.promptForPasswordReset = false;
                     passwordReset = true;
                     s.disconnect();
                     BUsernameAndPassword credentials = new BUsernameAndPassword(s.getUsername(), password);
                     s.getConnection().setCredentials(credentials);
                     s.getConnection().setAuthenticationClient(s.getConnection());
                     s.connect();
                  } catch (IOException | AuthenticationException var17) {
                     this.ensureDisconnect(s);
                     throw var17;
                  } catch (Exception var18) {
                     errMsg = var18.getLocalizedMessage();
                     if (loopCount > 100) {
                        this.ensureDisconnect(s);
                        throw var18;
                     }
                  }
               }
            }
         }

         return target;
      }
   }

   private void ensureDisconnect(BFoxSession s) {
      try {
         if (s != null) {
            s.disconnect();
         }
      } catch (Exception var3) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Cannot disconnect", (Throwable)var3);
         }
      }
   }

   private BImage getIcon() {
      try {
         BImage icon = BImage.make(this.target.get().getIcon());
         if (this.historyEntry != null) {
            this.historyEntry.icon = icon;
         }

         return icon;
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   private BPermissions getPermissions() throws Exception {
      BPermissions permissions = this.target.getPermissionsForTarget();
      if (log.isLoggable(Level.FINE)) {
         log.fine("getPermissions: " + permissions);
      }

      return permissions;
   }

   private AgentList getViewAgentsList() throws Exception {
      BObject obj = this.target.get();
      AgentList list = WbSys.getFilteredViewList(this.shell, obj, BFileMenuAgent.disable ? AgentFilter.has(this.permissions).toPredicate() : agent -> true);
      if (log.isLoggable(Level.FINE)) {
         log.fine("getAgents: " + list);
      }

      if (list.size() == 0) {
         AgentList agents = this.target.get().getAgents().filter(WbSys.getBaseViewsFilter());
         if (agents.size() > 0) {
            throw new ViewRestrictedException(obj.getType());
         } else {
            throw new NoViewRegisteredException(obj.getType());
         }
      } else {
         return list;
      }
   }

   private AgentInfo getAgent() throws Exception {
      String viewId = null;
      if (this.queries.length > 0) {
         OrdQuery lastQuery = this.queries[this.queries.length - 1];
         if (lastQuery instanceof ViewQuery) {
            viewId = ((ViewQuery)lastQuery).getViewId();
         }
      }

      AgentInfo agent;
      if (viewId == null && this.viewAgents.size() > 0) {
         agent = this.viewAgents.getDefault();
      } else {
         agent = this.viewAgents.get(viewId);
      }

      if (this.historyEntry != null && viewId != null) {
         this.historyEntry.ordNoView = BOrd.make(this.queries, 0, this.queries.length - 1);
         this.historyEntry.viewId = viewId;
      }

      if (agent == null) {
         System.out.println("No views accessible:");
         System.out.println("  ord:         " + this.ord);
         System.out.println("  target.type: " + this.target.get().getType());
         System.out.println("  target:      " + this.target.get());
         if (viewId != null) {
            throw new LocalizableRuntimeException("bajaui", "errorPanel.invalidView", new Object[]{viewId});
         } else {
            throw new LocalizableRuntimeException("bajaui", "errorPanel.noViews");
         }
      } else if (!this.permissions.has(agent.getRequiredPermissions())) {
         throw new PermissionException(this.permissions + " < " + agent.getRequiredPermissions());
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine("getAgent: " + agent);
         }

         return agent;
      }
   }

   private BWbView getView() throws Exception {
      BObject view = this.agent.getInstance();
      if (log.isLoggable(Level.FINE)) {
         log.fine("getView: " + view.toDebugString());
      }

      if (view instanceof BWbView) {
         return (BWbView)view;
      } else if (view instanceof BAbstractPxView) {
         return new BWbPxView((BAbstractPxView)view);
      } else if (view instanceof BIFormFactorMax) {
         return WebWidgetUtil.getCacheableWebWidget(
            this.tab.getView(), BOrd.toSession(this.ord.get()), this.target.getOrd(), this.agent, this.target.getSpace()
         );
      } else {
         throw new LocalizableRuntimeException("bajaui", "errorPanel.notWbView");
      }
   }

   public static void clearCache(BWbView view) {
      if (view instanceof BWebBrowserView) {
         BWebBrowserView browserView = (BWebBrowserView)view;
         browserView.clearCache();
      }

      if (view instanceof BWebWidget) {
         BWebWidget ww = (BWebWidget)view;
         WebWidgetUtil.clearCache(ww);
         BWebBrowser webBrowser = ww.getBrowser();
         if (webBrowser != null) {
            webBrowser.clearCache();
         }
      }

      if (view instanceof BWbPxView) {
         BWbPxView pxView = (BWbPxView)view;

         for (BWebWidget ww : (BWebWidget[])CompUtil.getDescendants(pxView, BWebWidget.class)) {
            WebWidgetUtil.clearCache(ww);
            BWebBrowser webBrowser = ww.getBrowser();
            if (webBrowser != null && webBrowser.getParentWidget() == null) {
               webBrowser.clearCache();
            }
         }

         for (BWebBrowser browserView : (BWebBrowser[])CompUtil.getDescendants(pxView, BWebBrowser.class)) {
            browserView.clearCache();
         }
      }
   }

   private Context getContext() throws Exception {
      try {
         BISession session = BOrd.toSession(this.target.get());
         if (session != null) {
            return session.getSessionContext();
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      return null;
   }

   private BPathBarAgent[] makePathBarAgents() throws Exception {
      if (this.shell.getProfile().makeLocatorBar() == null) {
         return noPathBarAgents;
      } else {
         try {
            if (this.shell.commands.pathBarUsesNavFile.isSelected()) {
               BObject obj = this.target.get();
               BISession session = BOrd.toSession(obj);
               if (session != null) {
                  BINavNode nav = session.getNavChild("navfile");
                  if (nav != null) {
                     BPathBarAgent[] navAgents = BNavFilePA.make((BNavFileSpace)nav, this.target);
                     if (navAgents != null) {
                        return navAgents;
                     }
                  }
               }
            }

            Array<BPathBarAgent> acc = new Array(BPathBarAgent.class);

            for (int i = 0; i < this.queries.length; i++) {
               BOrd base = i == 0 ? BOrd.NULL : this.ord.getSubOrd(0, i);
               BPathBarAgent agent = this.makePathBarAgent(this.queries[i]);
               if (agent != null) {
                  if (i == this.queries.length - 1) {
                     agent.setAgentOrd(base, this.queries[i], this.target.get());
                  } else {
                     agent.setAgentOrd(base, this.queries[i]);
                  }

                  for (BPathBarAgent kid : agent.explode()) {
                     acc.add(kid);
                  }
               }
            }

            return (BPathBarAgent[])acc.trim();
         } catch (Exception var9) {
            var9.printStackTrace();
            return null;
         }
      }
   }

   private BPathBarAgent makePathBarAgent(OrdQuery query) {
      BOrdScheme scheme = BOrdScheme.lookup(query.getScheme());
      if (scheme.getType().is(BViewScheme.TYPE)) {
         return null;
      } else {
         AgentList list = scheme.getAgents().filter(pathBarAgentFilter);
         return list.size() == 0 ? null : (BPathBarAgent)list.getDefault().getInstance();
      }
   }

   private String getFault() throws Exception {
      if (log.isLoggable(Level.FINE)) {
         log.fine("getFault");
      }

      BISession session = BOrd.toSession(this.target.get());
      return session instanceof BFoxSession ? ((BFoxSession)session).getStationFault() : null;
   }

   private void updateTab() throws Exception {
      if (log.isLoggable(Level.FINE)) {
         log.fine("updateTab");
      }

      OrdTarget lastTarget = this.tab.target;
      this.tab.ord = this.ord;
      this.tab.hasParent = this.hasParent;
      this.tab.target = this.target;
      this.tab.agents = this.viewAgents;
      this.tab.agent = this.agent;
      this.tab.view = this.view;
      this.tab.pathBarAgents = this.pathBarAgents;
      this.tab.fault = this.fault;
      BObject lastObject = lastTarget == null ? null : lastTarget.get();
      BISession lastSession = lastObject == null ? null : BOrd.toSession(lastObject);
      BObject object = this.target == null ? null : this.target.get();
      BISession session = object == null ? null : BOrd.toSession(object);
      if (lastSession != session) {
         if (lastSession instanceof BFoxSession) {
            ((BFoxSession)lastSession).getActivityMonitor().removeNotifyListener(this.tab);
         }

         if (session instanceof BFoxSession) {
            ((BFoxSession)session).getActivityMonitor().addNotifyListener(this.tab);
         }
      }
   }

   private void updateViewTab() {
      String tabName = BViewTabbedPane.toTabName(this.ord, this.target);
      if (log.isLoggable(Level.FINE)) {
         log.fine("updateViewTab: " + tabName);
      }

      BImage tabIcon = this.icon;
      BLabel label = this.tab.getLabel();
      if (tabIcon != null) {
         label.setImage(tabIcon);
      }

      label.setText(tabName);
      if (this.view.getParent() == null) {
         this.tab.setContent(this.view);
      }
   }

   private void activateView() {
      if (log.isLoggable(Level.FINE)) {
         log.fine("activateView");
      }

      this.tab.fireActivated();
   }

   private void loadView() throws Throwable {
      if (log.isLoggable(Level.FINE)) {
         log.fine("loadView");
      }

      this.view.fw(403);
      this.view.loadValue(this.target.get(), this.context);
      Throwable e = this.view.getLoadError();
      if (e != null) {
         throw e;
      } else {
         this.view.prime();
         this.shell.relayout();
         this.view.relayout();
      }
   }

   private void startBinder() throws Throwable {
      Binder binder = (Binder)this.view.fw(302);
      binder.start(this.target, this.context);
   }
}
