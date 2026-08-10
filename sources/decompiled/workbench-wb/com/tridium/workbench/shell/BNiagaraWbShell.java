package com.tridium.workbench.shell;

import com.tridium.authn.GenericReportableAuthenticationException;
import com.tridium.authn.LoginFailureCause;
import com.tridium.fox.session.IncompatibleVersionException;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.ui.BOptionDialog;
import com.tridium.ui.NiagaraWbShell;
import com.tridium.ui.ShellManager;
import com.tridium.ui.ShellPeer;
import com.tridium.ui.ShellManager.ShellPeerFactory;
import com.tridium.ui.theme.palladium.PalladiumWidgetTheme;
import com.tridium.util.ThrowableUtil;
import com.tridium.workbench.auth.AuthUtil;
import com.tridium.workbench.auth.WbAuthenticationClient;
import com.tridium.workbench.bql.builder.BBqlQueryBuilder;
import com.tridium.workbench.console.BConsole;
import com.tridium.workbench.file.BTextFileEditor;
import com.tridium.workbench.sidebars.BNavSideBar;
import com.tridium.workbench.web.browser.BrowserUtil;
import java.io.File;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BFileSystem;
import javax.baja.file.BITemplate;
import javax.baja.file.BSubSpaceFile;
import javax.baja.fox.BFoxProxySession;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.license.LicenseException;
import javax.baja.naming.BISession;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavRoot;
import javax.baja.nav.NavEvent;
import javax.baja.nav.NavListener;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuthenticationException;
import javax.baja.security.AuthenticationRealm;
import javax.baja.security.BICredentials;
import javax.baja.security.CancelledAuthenticationException;
import javax.baja.security.ChangeUserAuthenticationException;
import javax.baja.security.PermissionException;
import javax.baja.sys.BObject;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BDialog;
import javax.baja.ui.BHyperlinkMode;
import javax.baja.ui.BLabel;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.Command;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.ui.file.BDirectoryChooser;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.pane.BLabelPane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbPlugin;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.sidebar.BIWbSideBar;
import javax.baja.workbench.view.BWbView;
import javax.net.ssl.SSLHandshakeException;

@NiagaraType
public abstract class BNiagaraWbShell extends BWbShell implements NiagaraWbShell, ShellPeerFactory, NavListener {
   public static final Type TYPE = Sys.loadType(BNiagaraWbShell.class);
   static Logger log;
   static String statusDefault;
   static BImage errorIcon;
   static WbHistory history;
   public final BWbProfile profile;
   public final WbCommands commands;
   public final BWbPane pane;
   public BFileChooser fileChooser;
   public BDirectoryChooser dirChooser;
   public BBqlQueryBuilder bqlBuilder;
   private Set<BWbShell.ActivityListener> activityListeners = Collections.newSetFromMap(new WeakHashMap<>());
   public static final int OPTION_CLOSE_CHANGE_USER = 1000;
   public static Lexicon lex;

   @Override
   public Type getType() {
      return TYPE;
   }

   BNiagaraWbShell(Type profileType) {
      this.profile = BWbProfile.make(this, profileType);
      this.commands = new WbCommands(this);
      this.pane = new BWbPane(this);
      this.setContent(this.pane);
   }

   BNiagaraWbShell(Type profileType, ShellPeer peer) {
      super(peer);
      this.profile = BWbProfile.make(this, profileType);
      this.commands = new WbCommands(this);
      this.pane = new BWbPane(this);
      this.setContent(this.pane);
   }

   abstract void doShowStatus(String var1);

   BSideBarPane makeSideBar() {
      return new BSideBarPane();
   }

   @Override
   public Command getRefreshCommand() {
      return this.commands.refresh;
   }

   @Override
   public Command getSaveCommand() {
      return this.commands.save;
   }

   @Override
   public Command getExportCommand() {
      return this.commands.export;
   }

   @Override
   public Command getBackCommand() {
      return this.commands.back;
   }

   @Override
   public Command getForwardCommand() {
      return this.commands.forward;
   }

   @Override
   public Command getLogoffCommand() {
      return this.commands.logoff;
   }

   @Override
   public final BWbProfile getProfile() {
      return this.profile;
   }

   public final void showStatus(String msg) {
      if (msg == null) {
         msg = statusDefault;
      }

      this.doShowStatus(msg);
   }

   @Override
   public final BOrd getActiveOrd() {
      return this.tab().ord;
   }

   @Override
   public final OrdTarget getActiveOrdTarget() {
      return this.tab().target;
   }

   @Override
   public final BWbView getActiveView() {
      return this.tab().view;
   }

   public final String getActiveViewId() {
      return this.tab().agent.getAgentId();
   }

   public void updateCommandStates(BWbView view) {
      this.commands.updateView(view);
   }

   public void exitBusy() {
      super.exitBusy();
      this.commands.updateBogCommands();
   }

   public void debug() {
   }

   @Deprecated
   public final boolean isApplet() {
      return this instanceof BNiagaraWbApplet;
   }

   public final boolean isWebStart() {
      return this instanceof BNiagaraWbWebStartShell;
   }

   public final boolean isAppletOrWebStart() {
      return this.isApplet() || this.isWebStart();
   }

   public final boolean isFrame() {
      return this instanceof BNiagaraWbFrame;
   }

   public void updateTransferWidgetStates() {
      if (this.tab().view != null) {
         this.tab().view.updateTransferWidgetStates();
      }
   }

   public static BWbApplication app() {
      return (BWbApplication)getApplication();
   }

   public String getSelectedText() {
      if (this.tab().view instanceof BTextFileEditor) {
         BTextFileEditor editor = (BTextFileEditor)this.tab().view;
         String text = editor.getSelectedText();
         if (text != null) {
            text = text.trim();
            if (text.length() > 0) {
               return text;
            }
         }
      }

      return null;
   }

   public void started() throws Exception {
      super.started();
      BNavRoot.INSTANCE.addNavListener(this);
   }

   public void stopped() throws Exception {
      super.stopped();
      BNavRoot.INSTANCE.removeNavListener(this);
   }

   public void setModified(BWbPlugin view) {
      if (view == this.tab().view) {
         this.commands.save.setEnabled(true);
      }
   }

   public void clearModified(BWbPlugin view) {
      if (view == this.tab().view) {
         this.commands.save.setEnabled(false);
      }
   }

   public void navEvent(NavEvent event) {
      this.commands.updateBogCommands();
      if (event.getId() == 2 || event.getId() == 5) {
         this.checkTabsForRemoveEvent(event);
      }
   }

   private void checkTabsForRemoveEvent(NavEvent event) {
      BViewTab[] tabs = this.pane.views.getTabs();
      boolean tabRemoved = false;

      for (int i = 0; i < tabs.length; i++) {
         BViewTab tab = tabs[i];
         BOrd activeOrd = tab.ord;
         if (activeOrd != null && tab.getTarget() != null) {
            OrdTarget target = tab.getTarget();
            BObject object = target == null ? null : target.get();
            if (!(object instanceof BITemplate)) {
               BISession session = object == null ? null : BOrd.toSession(object);
               if (!(tab.view instanceof BIRebootable) || !((BIRebootable)tab.view).isRebooting()) {
                  if (session != null && !session.isConnected()) {
                     LoginFailureCause cause = null;
                     this.closeSubSpaceFile(object);
                     tabRemoved = true;
                     tab.stopView(false);
                     if (session instanceof BFoxSession) {
                        cause = ((BFoxSession)session).getFailureCause();
                     }

                     Exception e = new LocalizableException("bajaui", "errorPanel.disconnected");
                     if (cause == null) {
                        this.error(tab, activeOrd, e, false);
                     } else {
                        String message = cause.getDefaultFailureTitle() + "\n" + cause.getDefaultFailureMessage();
                        if (cause.equals(LoginFailureCause.SESSION_TIMEOUT)) {
                           String info = UiLexicon.bajaui().getText("errorPanel.autoLogoffInformation");
                           message = message + "\n" + info;
                        }

                        this.error(tab, activeOrd, message, e, false);
                     }
                  } else if (WbMain.isRemoved(event, activeOrd)) {
                     this.closeSubSpaceFile(object);
                     tabRemoved = true;
                     tab.stopView(false);
                     this.error(tab, activeOrd, new LocalizableException("bajaui", "errorPanel.targetRemoved"), false);
                  }
               }
            }
         }
      }

      if (tabRemoved) {
         this.tab().updateShell();
         this.relayout();
      }
   }

   private void closeSubSpaceFile(BObject target) {
      BSubSpaceFile file = this.getSubSpaceFile(target);
      if (file != null && file.isOpen()) {
         file.close();
      }
   }

   private BSubSpaceFile getSubSpaceFile(BObject target) {
      if (target instanceof BSubSpaceFile) {
         return (BSubSpaceFile)target;
      } else {
         BINavNode node = BOrd.toSpace(target);

         while (node != null && !(node instanceof BSubSpaceFile)) {
            node = node.getNavParent();
         }

         return (BSubSpaceFile)node;
      }
   }

   void autoLogoff() {
   }

   public final boolean confirmClose() {
      String title = UiLexicon.bajaui().getText("saveBeforeClose");
      int result = BSaveAllDialog.saveTabs(this, title);
      return result != 2;
   }

   public void hyperlink(File file, int line1, int col1, int line2, int col2) {
      BOrd fileOrd = BFileSystem.INSTANCE.localFileToOrd(file);
      BViewTab[] tabs = this.pane.views.getTabs();

      for (int i = 0; i < tabs.length; i++) {
         if (tabs[i].ord != null && tabs[i].ord.equals(fileOrd) && this.tab() != tabs[i]) {
            this.pane.views.selectLabelPane(tabs[i]);
         }
      }

      if (!this.tab().ord.equals(fileOrd)) {
         this.hyperlink(fileOrd);
      }

      Object active = this.getActiveView();
      if (active instanceof BTextFileEditor) {
         ((BTextFileEditor)active).highlight(line1, col1, line2, col2);
      }
   }

   public void hyperlink(HyperlinkInfo hyperlinkInfo) {
      this.doHyperlink(hyperlinkInfo);
   }

   final void doHyperlink(HyperlinkInfo hyperlinkInfo) {
      this.showStatus("");
      NHyperlinkInfo info = NHyperlinkInfo.make(hyperlinkInfo);
      if (info.getMode() == BHyperlinkMode.newShell) {
         WbMain.openFrame((BNiagaraWbFrame)this, info.getOrd(), this.profile.getType());
      } else {
         BViewTab tab = this.tab();
         if (info.getMode() == BHyperlinkMode.newTab) {
            tab = this.pane.views.addTab();
         } else if (!this.confirmHyperlink()) {
            return;
         }

         if (!this.profile.canHyperlink(info.getOrd())) {
            this.error(tab, info.getOrd(), new LocalizableException("bajaui", "errorPanel.ordNotAccessible"), true);
         } else {
            ShellManager shellManager = (ShellManager)this.widgetSupport(null);
            this.enterBusy();

            try {
               info.hyperlink(tab);
            } catch (Throwable var9) {
               this.error(tab, info.getOrd(), var9, true);
            } finally {
               this.exitBusy();
            }
         }
      }
   }

   public final boolean confirmHyperlink() {
      return this.confirmHyperlink(this.tab());
   }

   public final boolean confirmHyperlink(BViewTab tab) {
      if (tab.view != null) {
         if (tab.view.isModified()) {
            String msg = UiLexicon.bajaui().getText("confirmHyperlink.message", new Object[]{tab.getDisplayName()});
            int buttons = 14;
            int result = BDialog.confirm(this, null, msg, buttons);
            if (result == 2) {
               return false;
            }

            if (result == 4) {
               this.save(tab.view);
               if (tab.view.isModified()) {
                  return false;
               }
            }
         }

         if (!BrowserUtil.confirmBeforeUnload(tab.view)) {
            return false;
         }
      }

      return true;
   }

   public void syncTree() {
      this.syncTree(false);
   }

   public void syncTree(boolean silence) {
      if (this.getProfile().hasSideBar()) {
         try {
            BIWbSideBar[] bars = this.pane.sideBar.list();

            for (int i = 0; i < bars.length; i++) {
               if (bars[i] instanceof BNavSideBar) {
                  ((BNavSideBar)bars[i]).syncTree(silence);
                  return;
               }
            }
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      }
   }

   public void initShell() {
      ((ShellManager)this.widgetSupport(null)).activate();
   }

   public void shutdownShell() {
      for (BViewTab tab : this.pane.views.getTabs()) {
         BWbView view = tab.getView();
         if (view != null) {
            NHyperlinkInfo.clearCache(view);
         }

         tab.stopView(true);
      }

      ((ShellManager)this.widgetSupport(null)).deactivate();
   }

   public OrdTarget resolve(BOrd ord, BICredentials credentials) throws Exception {
      boolean newSession = false;
      WbAuthenticationClient authenticationClient = new WbAuthenticationClient(this);
      int i = 0;

      while (true) {
         try {
            OrdTarget target = ord.resolve(BLocalHost.INSTANCE, null, authenticationClient);
            if (newSession || authenticationClient.isNewSession()) {
               BWbProfile profile = this.getProfile();
               if (profile != null) {
                  BISession session = BOrd.toSession(target.get());
                  BOrd openOrd = profile.getOpenOrd(session, ord);
                  if (openOrd != null && !ord.equals(openOrd)) {
                     ord = openOrd;
                     target = openOrd.resolve(BLocalHost.INSTANCE, null, authenticationClient);
                  }
               }
            }

            return target;
         } catch (CancelledAuthenticationException var11) {
            throw var11;
         } catch (ChangeUserAuthenticationException var12) {
            AuthUtil.removeSavedUser(var12.getAuthenticationRealm().getAuthenticationRealmName());
         } catch (AuthenticationException var13) {
            label73: {
               newSession = true;
               AuthenticationRealm realm = var13.getAuthenticationRealm();
               LoginFailureCause loginFailureCause = var13.getLoginFailureCause();
               if (loginFailureCause != null) {
                  switch (loginFailureCause) {
                     case LOGIN_INTERFACE_NOT_SUPPORTED:
                        authenticationClient.setException(
                           new AuthenticationException(
                              var13.getAuthenticationRealm(), new GenericReportableAuthenticationException(LoginFailureCause.LOGIN_INTERFACE_NOT_SUPPORTED)
                           )
                        );
                        AuthUtil.removeSavedUser(var13.getAuthenticationRealm().getAuthenticationRealmName());
                        break label73;
                  }
               }

               if (credentials != null && var13.getAuthenticationRealm() != null) {
                  realm.setCredentials(credentials);
                  credentials = null;
               } else if (i == 0 && WbMain.kioskCredentials != null) {
                  realm.setCredentials(WbMain.kioskCredentials);
               } else if (i != 0 || !AuthUtil.resolveSSO(var13)) {
                  authenticationClient.setException(var13);
               }
            }
         } catch (Exception var14) {
            if (var14.getCause() != null && var14.getCause() instanceof IncompatibleVersionException) {
               BDialog.warning(
                  this,
                  UiLexicon.bajaui().getText("resolve.cannotHyperlinkN4wbToAXstation.title"),
                  UiLexicon.bajaui().getText("resolve.cannotHyperlinkN4wbToAXstation.msg", new Object[]{ord})
               );
            }

            throw var14;
         }

         i++;
      }
   }

   public void userActivity() {
      OrdTarget target = this.getActiveOrdTarget();
      BObject object = target == null ? null : target.get();
      BISession session = object == null ? null : BOrd.toSession(object);
      if (session instanceof BFoxSession) {
         ((BFoxSession)session).userActivity();
      }

      for (BWbShell.ActivityListener listener : this.activityListeners) {
         listener.activity();
      }
   }

   @Override
   public void addActivityListener(BWbShell.ActivityListener listener) {
      this.activityListeners.add(listener);
   }

   @Override
   public void removeActivityListener(BWbShell.ActivityListener listener) {
      this.activityListeners.remove(listener);
   }

   @Override
   public boolean notifyTimeout(BWidget widget, final BISession session) {
      BWidgetShell shell = widget.getShell();
      if (!(shell instanceof BNiagaraWbShell)) {
         for (BWidget parent = widget; parent != null; parent = parent.getParentWidget()) {
            if (parent instanceof BViewTab) {
               ((BNiagaraWbShell)shell).pane.views.selectLabelPane((BLabelPane)parent);
               break;
            }
         }
      }

      final BOptionDialog dialog = new BOptionDialog(
         widget,
         lex.getText("sessionTimeout.title"),
         new BLabel(lex.getText("sessionTimeout.message", new Object[]{((BFoxProxySession)session).getStationName()})),
         3,
         BDialog.INFO_ICON,
         null
      );
      dialog.setBoundsCenteredOnOwner();
      dialog.getOkButton().setText(lex.getText("sessionTimeout.continue"));
      dialog.getCancelButton().setText(lex.getText("sessionTimeout.logout"));
      Runnable runnable = new Runnable() {
         @Override
         public void run() {
            while (!dialog.isShowing()) {
               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var2) {
               }
            }

            while (dialog.isShowing()) {
               if (session instanceof BFoxSession && !session.isConnected()) {
                  Runnable close = new Runnable() {
                     @Override
                     public void run() {
                        dialog.close();
                     }
                  };
                  BWidget.invokeLater(close);
               }

               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var3) {
               }
            }
         }
      };
      Thread expirationWarningMonitor = new Thread(runnable);
      expirationWarningMonitor.setName("ExpirationWarningMonitor");
      expirationWarningMonitor.start();
      dialog.open();
      if (dialog.getResult() == 2) {
         session.disconnect();
         return false;
      } else {
         return true;
      }
   }

   public void save(BWbView view) {
      this.enterBusy();

      try {
         view.saveValue();
      } catch (Throwable var10) {
         Throwable cause = var10;
         String msg = UiLexicon.bajaui().getText("plugin.save.error");
         boolean suppressStackTrace = false;
         if (var10 instanceof CannotSaveException) {
            CannotSaveException cse = (CannotSaveException)var10;
            if (cse.isSilent()) {
               return;
            }

            msg = var10.getMessage();
            cause = var10.getCause();
            if (cause == null) {
               cause = var10;
            }

            suppressStackTrace = cse.suppressStackTrace();
         }

         if (!suppressStackTrace) {
            cause.printStackTrace();
         }

         String title = UiLexicon.bajaui().getText("dialog.error");
         BDialog.error(this, title, msg, suppressStackTrace ? null : var10);
         this.repaint();
         return;
      } finally {
         this.exitBusy();
      }

      this.commands.updateBogCommands();
      this.commands.save.setEnabled(false);
      this.repaint();
   }

   void error(BViewTab tab, BOrd ord, Throwable e, boolean logIt) {
      String msg = null;
      if (e instanceof UnresolvedException) {
         Throwable cause = ThrowableUtil.getCause(e);
         msg = this.exceptionToMessage(cause);
         if (msg == null) {
            msg = this.exceptionToMessage(e);
         }
      } else {
         msg = this.exceptionToMessage(e);
      }

      if (msg == null) {
         msg = UiLexicon.bajaui().getText("errorPanel.cannotHyperlink");
      }

      this.error(tab, ord, msg, e, logIt);
   }

   void error(BViewTab tab, BOrd ord, String msg, Throwable e, boolean logIt) {
      if (logIt) {
         log.log(Level.SEVERE, msg, e);
      }

      this.showStatus(msg);
      String tabName = BViewTabbedPane.toTabName(ord, null);
      BImage tabIcon = errorIcon;
      tab.fault = null;
      tab.setLabel(new BLabel(tabIcon, tabName));
      BWidget errorDisplay = null;
      BWbProfile profile = this.getProfile();
      if (profile != null) {
         errorDisplay = profile.makeErrorDisplay(this, msg, ord, e);
      }

      if (errorDisplay == null) {
         errorDisplay = new BErrorPanel(this, msg, ord, e);
      }

      tab.setContent(errorDisplay);
      tab.relayout();
   }

   private String exceptionToMessage(Throwable e) {
      UiLexicon lex = UiLexicon.bajaui();
      if (e instanceof LocalizableException) {
         return e.getMessage();
      } else if (e instanceof LocalizableRuntimeException) {
         return e.getMessage();
      } else if (e instanceof UnresolvedException) {
         return lex.getText("errorPanel.targetNotFound");
      } else if (e instanceof AuthenticationException) {
         return lex.getText("errorPanel.failedToAuthenticate");
      } else if (e instanceof PermissionException) {
         return lex.getText("errorPanel.noPermission");
      } else if (e instanceof LicenseException) {
         return lex.getText("errorPanel.license", new Object[]{e.getMessage()});
      } else if (e instanceof ConnectException && e.getCause() instanceof SSLHandshakeException) {
         return lex.getText("errorPanel.tlsError", new Object[]{e.getCause().getMessage()});
      } else if (e instanceof ConnectException) {
         return lex.getText("errorPanel.connect");
      } else if (e instanceof UnknownHostException) {
         return lex.getText("errorPanel.unknownHost", new Object[]{e.getMessage()});
      } else {
         return e instanceof SSLHandshakeException ? lex.getText("errorPanel.tlsError", new Object[]{e.getMessage()}) : null;
      }
   }

   public BConsole getConsole() {
      return this.pane.console;
   }

   public BConsole openConsole() {
      this.commands.console.setSelected(true);
      this.pane.console.appendBreak();
      this.relayout();
      return this.pane.console;
   }

   public BViewTab tab() {
      return this.pane.views.tab();
   }

   private static BFont incFontSize(BFont f) {
      return BFont.make(f.getName(), f.getSize() + 4.0, f.getStyle());
   }

   public static WbHistory getWbHistory() {
      return history;
   }

   static {
      if (BGeneralOptions.make().getFontSize() == BFontSize.large) {
         PalladiumWidgetTheme.plainText = incFontSize(PalladiumWidgetTheme.plainText);
         PalladiumWidgetTheme.boldText = incFontSize(PalladiumWidgetTheme.boldText);
         PalladiumWidgetTheme.largeFont = incFontSize(PalladiumWidgetTheme.largeFont);
         PalladiumWidgetTheme.largeBoldFont = incFontSize(PalladiumWidgetTheme.largeBoldFont);
         PalladiumWidgetTheme.fixedWidth = incFontSize(PalladiumWidgetTheme.fixedWidth);
         PalladiumWidgetTheme.fixedWidthBold = incFontSize(PalladiumWidgetTheme.fixedWidthBold);
      }

      log = Logger.getLogger("wb.shell");
      statusDefault = "";
      errorIcon = BImage.make("module://icons/x16/error.png");
      history = new WbHistory();
      lex = Lexicon.make("workbench");
   }

   static class Support {
      BNiagaraWbShell shell;
      BWbProfile profile;

      Support(BNiagaraWbShell shell) {
         this.shell = shell;
         this.profile = shell.profile;
      }
   }
}
