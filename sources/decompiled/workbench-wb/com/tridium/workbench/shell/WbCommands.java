package com.tridium.workbench.shell;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.fox.sys.BFoxConnection;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.user.BUserChannel;
import com.tridium.gx.awt.ImageManager;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.ui.PxCache;
import com.tridium.ui.PxIncludeManager;
import com.tridium.ui.fx.BFxWidget;
import com.tridium.ui.fx.menu.BFxMenuBar;
import com.tridium.ui.fx.toolbar.BFxToolBar;
import com.tridium.ui.theme.Theme;
import com.tridium.util.BSessionInfo;
import com.tridium.util.ObjectUtil;
import com.tridium.workbench.auth.BCnxHandler;
import com.tridium.workbench.bookmark.BBookmarkMenu;
import com.tridium.workbench.file.BExportDialog;
import com.tridium.workbench.util.BFileSearch;
import com.tridium.workbench.util.BGotoFile;
import com.tridium.workbench.util.BSessionInfoDialog;
import com.tridium.workbench.util.TypeInfoSpec;
import com.tridium.workbench.web.browser.BWebWidget;
import java.lang.reflect.Method;
import java.util.Optional;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.file.BSubSpaceFile;
import javax.baja.gx.BImage;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.naming.BISession;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.BSession;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.space.BSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BActionMenuItem;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BHyperlinkMode;
import javax.baja.ui.BMenu;
import javax.baja.ui.BMenuBar;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BToggleButton;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.CommandEvent;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.BAbstractButton.MenuController;
import javax.baja.ui.menu.BIMenu;
import javax.baja.ui.menu.BIMenuBar;
import javax.baja.ui.menu.BIQuickSearch;
import javax.baja.ui.options.BMruTextDropDown;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.toolbar.BIToolBar;
import javax.baja.ui.util.UiLexicon;
import javax.baja.util.Lexicon;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.sidebar.BIWbSideBar;
import javax.baja.workbench.sidebar.BWbSideBar;
import javax.baja.workbench.tool.BWbNavNodeTool;
import javax.baja.workbench.tool.BWbTool;
import javax.baja.workbench.view.BWbView;

public class WbCommands extends BNiagaraWbShell.Support {
   public final boolean isKiosk;
   public final BToggleButton rightClickButton;
   public final Command newWindow = new WbCommands.NewWindowCommand();
   public final Command newTab = new WbCommands.NewTabCommand();
   public final Command closeTab = new WbCommands.CloseTabCommand(null);
   public final Command closeOtherTabs = new WbCommands.CloseOtherTabsCommand(null);
   public final Command nextTab = new WbCommands.NextTabCommand();
   public final Command prevTab = new WbCommands.PrevTabCommand();
   public final Command save = new WbCommands.SaveCommand();
   public final Command saveAll = new WbCommands.SaveAllCommand();
   public final Command saveBog = new WbCommands.SaveBogCommand();
   public final Command bogProtection = new WbCommands.BogProtectionCommand();
   public final Command open = new WbCommands.OpenMenuCommand();
   public final Command openOrd = new WbCommands.OpenOrdCommand();
   public final Command openFile = new WbCommands.OpenFileCommand();
   public final Command openDir = new WbCommands.OpenDirCommand();
   public final Command openQuery = new WbCommands.OpenQueryCommand();
   public final Command findStations = new WbCommands.FindStationsCommand();
   public final WbCommands.ExportCommand export = new WbCommands.ExportCommand();
   public final Command back = new WbCommands.BackCommand(null);
   public final Command forward = new WbCommands.ForwardCommand(null);
   public final Command upLevel = new WbCommands.UpLevelCommand(null);
   public final Command recentOrds = new WbCommands.RecentOrdsCommand();
   public final Command refresh = new WbCommands.RefreshCommand();
   public final Command refreshTabs = new WbCommands.RefreshTabsCommand();
   public final Command sessionInfo = new WbCommands.SessionInfoCommand();
   public final Command home = new WbCommands.HomeCommand();
   public final Command nonFipsRestart = new WbCommands.NonFipsRestartCommand();
   public final Command fipsRestart = new WbCommands.FipsRestartCommand();
   public final Command logoff = new WbCommands.LogoffCommand();
   public final Command close = new WbCommands.CloseCommand();
   public final Command exit = new WbCommands.ExitCommand();
   public final Command cut = new WbCommands.PluginCommand(0);
   public final Command copy = new WbCommands.PluginCommand(1);
   public final Command paste = new WbCommands.PluginCommand(2);
   public final Command pasteSpecial = new WbCommands.PluginCommand(11);
   public final Command duplicate = new WbCommands.PluginCommand(3);
   public final Command delete = new WbCommands.PluginCommand(4);
   public final Command rename = new WbCommands.PluginCommand(5);
   public BActionMenuItem undoMenu;
   public BActionMenuItem redoMenu;
   public BButton undoButton;
   public BButton redoButton;
   public final Command find = new WbCommands.PluginCommand(6);
   public final Command findNext = new WbCommands.PluginCommand(8);
   public final Command findPrev = new WbCommands.PluginCommand(7);
   public final Command replace = new WbCommands.PluginCommand(9);
   public final Command goTo = new WbCommands.PluginCommand(10);
   public final Command gotoFile = new WbCommands.GotoFileCommand();
   public final Command findFiles = new WbCommands.FindFilesCommand();
   public final Command replaceInFiles = new WbCommands.ReplaceInFilesCommand();
   public final Command consolePrev = new WbCommands.ConsolePrevCommand();
   public final Command consoleNext = new WbCommands.ConsoleNextCommand();
   public final Command consoleKill = new WbCommands.ConsoleKillCommand();
   public final Command options = new WbCommands.OptionsCommand();
   public final Command[] tools = this.buildToolCommands();
   public final Command sideBarMenu = new WbCommands.SideBarMenuCommand();
   public final ToggleCommand showSideBar = new WbCommands.ShowSideBarCommand();
   public final Command[] sideBars = this.buildSideBarCommands();
   public final Command activePlugin = new WbCommands.ActivePlugin();
   public final WbCommands.ConsoleGroup consoleGroup;
   public final WbCommands.ConsoleToggle hideConsole;
   public final WbCommands.ConsoleToggle console;
   public final ToggleCommand pathBarUsesNavFile = new WbCommands.PathBarUsesNavFileCommand();
   public final Command helpContents = new WbCommands.HelpContentsCommand();
   public final Command helpOnView = new WbCommands.HelpOnViewCommand();
   public final Command helpGuideOnTarget = new WbCommands.HelpGuideOnTargetCommand();
   public final Command helpBajadocOnTarget = new WbCommands.HelpBajadocOnTargetCommand();
   public final Command helpFindBajadoc = new WbCommands.HelpFindBajadocCommand();
   public final Command about = new WbCommands.AboutCommand();
   public final WbCommands.ModeSelectionGroup modeSelectionGroup;
   public final WbCommands.ModeSelectionCommand noneMode;
   public final WbCommands.ModeSelectionCommand miniMode;
   public final WbCommands.ModeSelectionCommand singleMode;
   public final WbCommands.ModeSelectionCommand doubleMode;
   public final WbCommands.ModeSelectionCommand masterMode;

   public WbCommands(BNiagaraWbShell shell) {
      super(shell);
      this.isKiosk = WbMain.isKiosk();
      this.rightClickButton = new BRightClickButton(shell);
      this.consoleGroup = new WbCommands.ConsoleGroup();
      this.consoleGroup.add(this.hideConsole = new WbCommands.ConsoleToggle("commands.hideConsole"));
      this.consoleGroup.add(this.console = new WbCommands.ConsoleToggle("commands.console"));
      this.modeSelectionGroup = new WbCommands.ModeSelectionGroup();
      this.modeSelectionGroup.add(this.noneMode = new WbCommands.ModeSelectionCommand(WbCommands.Mode.NONE));
      this.modeSelectionGroup.add(this.miniMode = new WbCommands.ModeSelectionCommand(WbCommands.Mode.MINI));
      this.modeSelectionGroup.add(this.singleMode = new WbCommands.ModeSelectionCommand(WbCommands.Mode.SINGLE));
      this.modeSelectionGroup.add(this.doubleMode = new WbCommands.ModeSelectionCommand(WbCommands.Mode.DOUBLE));
      this.modeSelectionGroup.add(this.masterMode = new WbCommands.ModeSelectionCommand(WbCommands.Mode.MASTER));
   }

   public BIMenuBar makeMenuBar() {
      BIMenuBar menuBar = this.getMenuBar();
      menuBar.setId("menu-bar-profile");
      menuBar.addMenu("file", this.buildFileMenu());
      menuBar.addMenu("edit", this.buildEditMenu());
      menuBar.addMenu("search", this.buildSearchMenu());
      menuBar.addMenu("bookmarks", this.buildBookmarkMenu());
      menuBar.addMenu("tools", this.buildToolsMenu());
      menuBar.addMenu("window", this.buildWindowMenu());
      menuBar.addMenu("help", this.buildHelpMenu());
      return menuBar;
   }

   private BIMenuBar getMenuBar() {
      return (BIMenuBar)(Theme.javaFx().isEnabled() ? new BFxMenuBar() : new BMenuBar());
   }

   public BMenu makeOpenMenu() {
      BOrd fox = BOrd.make("local:|fox:");
      BOrd foxs = BOrd.make("local:|foxs:");
      boolean hasFox = this.shell.profile.canHyperlink(fox) || this.shell.profile.canHyperlink(foxs);
      BMenu open = UiLexicon.bajaui().buildMenu("menu.open.label");
      open.addItem("openOrd", this.openOrd);
      open.addItem("openFile", this.openFile);
      open.addItem("openDir", this.openDir);
      if (hasFox) {
         open.addItem("openQuery", this.openQuery);
      }

      open.addSeparator();

      for (Command sessionOpen : BCnxHandler.getCnxCommands(this.shell, null)) {
         open.addItem(this.toSlotName(sessionOpen), sessionOpen);
      }

      if (hasFox) {
         open.addSeparator();
         open.addItem("findStations", this.findStations);
      }

      return open;
   }

   private BIMenu buildFileMenu() {
      BSubMenuItem openItem = new BSubMenuItem(this.makeOpenMenu());
      openItem.setText(UiLexicon.bajaui().getText("commands.open.label"));
      openItem.setImage(BImage.make("module://icons/x16/open.png"));
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.file.label");
      menu.addItem("open", openItem);
      menu.addSeparator();
      if (!this.isKiosk) {
         menu.addItem("newWindow", this.newWindow);
      }

      menu.addItem("newTab", this.newTab);
      menu.addItem("closeTab", this.closeTab);
      menu.addItem("closeOtherTabs", this.closeOtherTabs);
      menu.addItem("nextTab", this.nextTab);
      menu.addItem("prevTab", this.prevTab);
      menu.addSeparator();
      menu.addItem("save", this.save);
      menu.addItem("saveAll", this.saveAll);
      menu.addItem("saveBog", this.saveBog);
      menu.addItem("bogProtection", this.bogProtection);
      menu.addSeparator();
      menu.addItem("export", this.export);
      menu.addSeparator();
      menu.addItem("back", this.back);
      menu.addItem("forward", this.forward);
      menu.addItem("upLevel", this.upLevel);
      menu.addSeparator();
      menu.addItem("recentOrds", this.recentOrds);
      menu.addItem("home", this.home);
      menu.addItem("refresh", this.refresh);
      menu.addItem("refreshtabs", this.refreshTabs);
      menu.addItem("sessionInfo", this.sessionInfo);
      menu.addSeparator();
      if (SecurityInitializer.getInstance().isFips()) {
         menu.addItem("nonFipsRestart", this.nonFipsRestart);
      }

      try {
         Sys.getLicenseManager().checkFeature("tridium", "fips140-2");
         if (!SecurityInitializer.getInstance().isFips()) {
            menu.addItem("fipsRestart", this.fipsRestart);
         }
      } catch (FeatureNotLicensedException var4) {
      }

      menu.addItem("logoff", this.logoff);
      if (!this.isKiosk) {
         menu.addItem("close", this.close);
         menu.addItem("exit", this.exit);
      }

      return menu;
   }

   private BIMenu buildEditMenu() {
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.edit.label");
      menu.addItem("cut", this.cut);
      menu.addItem("copy", this.copy);
      menu.addItem("paste", this.paste);
      menu.addItem("pasteSpecial", this.pasteSpecial);
      menu.addItem("duplicate", this.duplicate);
      menu.addItem("delete", this.delete);
      menu.addItem("rename", this.rename);
      menu.addSeparator();
      menu.addItem("undo", this.shell.getUndoManager().getUndoCommand());
      menu.addItem("redo", this.shell.getUndoManager().getRedoCommand());
      this.undoMenu = (BActionMenuItem)menu.getItem("undo");
      this.redoMenu = (BActionMenuItem)menu.getItem("redo");
      return menu;
   }

   private BIMenu buildSearchMenu() {
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.search.label");
      menu.addItem("find", this.find);
      menu.addItem("findPrev", this.findNext);
      menu.addItem("findNext", this.findPrev);
      menu.addItem("replace", this.replace);
      menu.addItem("goto", this.goTo);
      menu.addSeparator();
      menu.addItem("gotoFile", this.gotoFile);
      menu.addItem("findFiles", this.findFiles);
      menu.addItem("replaceInFiles", this.replaceInFiles);
      menu.addSeparator();
      menu.addItem("consolePrev", this.consolePrev);
      menu.addItem("consoleNext", this.consoleNext);
      return menu;
   }

   private BIMenu buildBookmarkMenu() {
      return new BBookmarkMenu();
   }

   private Command[] buildToolCommands() {
      Array<Command> acc = new Array(Command.class);
      if (this.profile.hasTools()) {
         for (TypeInfo t : BWbTool.getInstalled()) {
            if (this.profile.hasTool(t)) {
               acc.add(new WbCommands.ToolCommand(t));
            }
         }
      }

      return (Command[])acc.trim();
   }

   private BIMenu buildToolsMenu() {
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.tools.label");
      menu.addItem("options", this.options);
      if (this.tools.length > 0) {
         menu.addSeparator();

         for (int i = 0; i < this.tools.length; i++) {
            menu.addItem("t" + i, this.tools[i]);
         }
      }

      return menu;
   }

   private Command[] buildSideBarCommands() {
      Array<Command> acc = new Array(Command.class);
      if (this.profile.hasSideBar()) {
         for (TypeInfo t : BWbSideBar.getInstalled()) {
            if (this.profile.hasSideBar(t)) {
               acc.add(new WbCommands.SideBarCommand(t));
            }
         }
      }

      return (Command[])acc.trim();
   }

   public BMenu buildSideBarMenu() {
      BMenu menu = UiLexicon.bajaui().buildMenu("menu.sidebar.label");
      menu.addItem("show", this.showSideBar);
      if (this.sideBars.length > 0) {
         menu.addSeparator();

         for (int i = 0; i < this.sideBars.length; i++) {
            menu.addItem("m" + i, this.sideBars[i]);
         }
      }

      return menu;
   }

   public BMenu buildModeSelectorMenu() {
      BMenu menu = UiLexicon.bajaui().buildMenu("menu.sidebar.label");
      menu.addItem("noneMode", this.noneMode);
      menu.addSeparator();
      menu.addItem("miniMode", this.miniMode);
      menu.addItem("singleMode", this.singleMode);
      menu.addItem("doubleMode", this.doubleMode);
      menu.addItem("masterMode", this.masterMode);
      this.singleMode.setSelected(true);
      return menu;
   }

   private BIMenu buildWindowMenu() {
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.window.label");
      if (this.profile.hasSideBar()) {
         menu.addItem("sidebars", new BSubMenuItem(this.buildSideBarMenu()));
         menu.addSeparator();
      }

      menu.addItem("pathBarUsesNavFile", this.pathBarUsesNavFile);
      menu.addItem("activePlugin", this.activePlugin);
      menu.addSeparator();
      menu.addItem("hideConsole", this.hideConsole);
      menu.addItem("console", this.console);
      menu.addItem("consoleKill", this.consoleKill);
      return menu;
   }

   private BIMenu buildHelpMenu() {
      BIMenu menu = UiLexicon.bajaui().buildMenu("menu.help.label");
      menu.addItem("contents", this.helpContents);
      menu.addSeparator();
      menu.addItem("onView", this.helpOnView);
      menu.addItem("guideOnTarget", this.helpGuideOnTarget);
      menu.addItem("bajadocOnTarget", this.helpBajadocOnTarget);
      menu.addSeparator();
      menu.addItem("findBajadoc", this.helpFindBajadoc);
      menu.addSeparator();
      menu.addItem("about", this.about);
      return menu;
   }

   private String toSlotName(Command command) {
      StringBuilder s = new StringBuilder();
      String[] toks = TextUtil.split(command.getKeyBase(), '.');

      for (int i = 0; i < toks.length; i++) {
         String tok = toks[i];
         if (i == 0) {
            s.append(tok);
         } else {
            s.append(TextUtil.capitalize(tok));
         }
      }

      return s.toString();
   }

   public BIToolBar makeToolBar() {
      BIToolBar bar = this.getToolBar();
      bar.setId("tool-bar-profile");
      bar.addButton("back", this.back).setMenuController(new WbCommands.BackMenuController());
      bar.addButton("forward", this.forward).setMenuController(new WbCommands.ForwardMenuController());
      bar.addButton("upLevel", this.upLevel).setMenuController(new WbCommands.UpLevelMenuController());
      bar.addSeparator();
      if (this.profile.hasSideBar()) {
         bar.addButton("sideBar", this.sideBarMenu).setMenuController(new WbCommands.SideBarMenuController());
      }

      bar.addButton("recentOrds", this.recentOrds);
      bar.addButton("home", this.home);
      bar.addButton("refresh", this.refresh);
      bar.addButton("refreshTabs", this.refreshTabs);
      bar.addButton("sessionInfo", this.sessionInfo);
      bar.addSeparator();
      bar.addButton("open", this.open).setMenuController(new WbCommands.OpenMenuController());
      bar.addButton("save", this.save);
      bar.addButton("saveBog", this.saveBog);
      bar.addButton("bogProtection", this.bogProtection);
      bar.addButton("export", this.export);
      bar.addSeparator();
      bar.addButton("cut", this.cut);
      bar.addButton("copy", this.copy);
      bar.addButton("paste", this.paste);
      bar.addButton("duplicate", this.duplicate);
      bar.addButton("delete", this.delete);
      bar.addSeparator();
      bar.addButton("undo", this.shell.getUndoManager().getUndoCommand());
      bar.addButton("redo", this.shell.getUndoManager().getRedoCommand());
      this.undoButton = (BButton)bar.asWidget().get("undo");
      this.redoButton = (BButton)bar.asWidget().get("redo");
      return bar;
   }

   private BIToolBar getToolBar() {
      return (BIToolBar)(Theme.javaFx().isEnabled() ? new BFxToolBar() : new BToolBar());
   }

   void updateTab(BViewTab tab) {
      this.back.setEnabled(tab.history.isBackEnabled());
      this.forward.setEnabled(tab.history.isForwardEnabled());
      this.upLevel.setEnabled(tab.hasParent);
      this.updateView(tab.view);
   }

   void updateView(BWbView view) {
      boolean vnn = view != null;
      this.cut.setEnabled(vnn && view.isCommandEnabled(0));
      this.copy.setEnabled(vnn && view.isCommandEnabled(1));
      this.paste.setEnabled(vnn && view.isCommandEnabled(2));
      this.pasteSpecial.setEnabled(vnn && view.isCommandEnabled(11));
      this.duplicate.setEnabled(vnn && view.isCommandEnabled(3));
      this.delete.setEnabled(vnn && view.isCommandEnabled(4));
      this.rename.setEnabled(vnn && view.isCommandEnabled(5));
      this.find.setEnabled(vnn && view.isCommandEnabled(6));
      this.findNext.setEnabled(vnn && view.isCommandEnabled(8));
      this.findPrev.setEnabled(vnn && view.isCommandEnabled(7));
      this.replace.setEnabled(vnn && view.isCommandEnabled(9));
      this.goTo.setEnabled(vnn && view.isCommandEnabled(10));
      this.consoleKill.setEnabled(vnn && this.shell.pane.console.inExec());
      this.helpOnView.setEnabled(vnn);
      this.helpGuideOnTarget.setEnabled(vnn);
      this.helpBajadocOnTarget.setEnabled(vnn);
      boolean connected = false;

      try {
         BISession session = this.shell.getActiveView().getCurrentValueSession();
         if (session != null && !(session instanceof BLocalHost) && session.isConnected()) {
            connected = true;
         }
      } catch (Exception var5) {
      }

      this.sessionInfo.setEnabled(vnn && connected);
      this.save.setEnabled(vnn && view.isModified());
      this.updateBogCommands();
   }

   public void trimCaches() {
      ImageManager.trimAll();
      PxCache.trimAll();
      PxIncludeManager.trimAll();
   }

   public void updateBogCommands() {
      this.saveBog.setEnabled(false);
      this.bogProtection.setEnabled(false);
      if (this.shell.tab().view != null) {
         BObject target = this.shell.tab().view.getCurrentValue();
         if (target instanceof BComponent) {
            BComponent component = (BComponent)target;
            BSpace space = component.getComponentSpace();
            if (space != null && space.getNavParent() instanceof BSubSpaceFile) {
               BSubSpaceFile file = (BSubSpaceFile)space.getNavParent();
               this.saveBog.setEnabled(file.isModified());
               if (space.getNavParent() instanceof BBogFile) {
                  this.bogProtection.setEnabled(true);
               }
            }
         }
      }
   }

   public BWidget getQuickSearch(BWbView view) {
      BIQuickSearch quickSearch = this.findQuickSearchAgent(view, view);
      if (quickSearch == null) {
         quickSearch = this.findQuickSearchAgent(this.profile, view);
      }

      return quickSearch != null ? quickSearch.asGlobalWidget(view, "menu-bar-profile-quick-search") : null;
   }

   private BIQuickSearch findQuickSearchAgent(BObject obj, BWbView view) {
      AgentList agents = obj.getAgents();
      if (agents.size() < 1) {
         return null;
      } else {
         agents = agents.filter(AgentFilter.is(BIQuickSearch.TYPE));
         if (agents.size() < 1) {
            return null;
         } else {
            AgentFilter filter = AgentFilter.is(BFxWidget.TYPE);
            if (!Theme.javaFx().isEnabled()) {
               filter = AgentFilter.not(filter);
            }

            agents = agents.filter(filter);
            int size = agents.size();
            if (size > 0) {
               AgentInfo quickSearchInfo = agents.getDefault();
               if (this.profile.hasQuickSearch(quickSearchInfo, view)) {
                  var quickSearch = (BIQuickSearch & BIQuickSearch)quickSearchInfo.getInstance();
                  if (quickSearch.isQuickSearchEnabled(view)) {
                     return quickSearch;
                  }
               }

               if (size > 1) {
                  for (int i = 0; i < size; i++) {
                     quickSearchInfo = agents.get(i);
                     if (this.profile.hasQuickSearch(quickSearchInfo, view)) {
                        var quickSearch = (BIQuickSearch & BIQuickSearch)quickSearchInfo.getInstance();
                        if (quickSearch.isQuickSearchEnabled(view)) {
                           return quickSearch;
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   WbCommands.CloseTabCommand makeCloseTab(BViewTab tab) {
      return new WbCommands.CloseTabCommand(tab);
   }

   WbCommands.CloseOtherTabsCommand makeCloseOtherTabs(BViewTab tab) {
      return new WbCommands.CloseOtherTabsCommand(tab);
   }

   public WbCommands.BackMenuController makeBackMenuController() {
      return new WbCommands.BackMenuController();
   }

   public WbCommands.ForwardMenuController makeForwardMenuController() {
      return new WbCommands.ForwardMenuController();
   }

   public static BOrd getAboutOrd() {
      return BOrd.make("workbench:/help/about.html");
   }

   public BOrd getHomeOrd() {
      BOrd ord = getAboutOrd();

      try {
         BWbView view = this.shell.getActiveView();
         BObject target = view.getCurrentValue();
         BISession session = BOrd.toSession(target);
         if (session instanceof BFoxSession) {
            BFoxSession foxSession = (BFoxSession)session;
            BFoxConnection conn = foxSession.getConnection();
            BUserChannel userChannel = conn.getChannels().getUserChannel();
            ord = userChannel.getHomePage();
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }

      return ord;
   }

   public class AboutCommand extends WbCommands.WbCommand {
      AboutCommand() {
         super("commands.help.about");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.hyperlink(BOrd.make("workbench:/help/about.html"));
         return null;
      }
   }

   public class ActivePlugin extends WbCommands.WbCommand {
      ActivePlugin() {
         super("commands.activePlugin");
      }

      public CommandArtifact doInvoke() {
         BWbView view = WbCommands.this.shell.getActiveView();
         if (view != null) {
            view.prime();
         }

         return null;
      }
   }

   public class BackCommand extends WbCommands.WbCommand {
      BOrd ord;

      BackCommand(WbHistory.Entry entry) {
         super("commands.back");
         if (entry != null) {
            this.ord = entry.ord;
            this.label = entry.name;
            this.icon = entry.icon;
            this.accelerator = null;
         }
      }

      public CommandArtifact doInvoke() throws Exception {
         BOrd ord = this.ord;
         if (ord == null) {
            ord = WbCommands.this.shell.tab().history.back();
         } else {
            BOrd test = WbCommands.this.shell.tab().history.back();

            while (test != null && test != ord) {
               test = WbCommands.this.shell.tab().history.back();
            }
         }

         if (ord != null) {
            WbCommands.this.shell.hyperlink(new NHyperlinkInfo(ord, BHyperlinkMode.replace, false));
         }

         return null;
      }
   }

   class BackMenuController implements MenuController {
      public boolean isMenuDistinct() {
         return true;
      }

      public BMenu getMenu(BAbstractButton b) {
         BMenu menu = new BMenu();
         WbHistory.Entry[] list = WbCommands.this.shell.tab().history.getBackHistory();

         for (int i = 0; i < list.length && i < 10; i++) {
            menu.add("back" + i, WbCommands.this.new BackCommand(list[i]));
         }

         return menu;
      }
   }

   public class BogProtectionCommand extends Command {
      BogProtectionCommand() {
         super(WbCommands.this.shell, UiLexicon.bajaui().module, "commands.bogProtection");
      }

      public CommandArtifact doInvoke() throws Exception {
         if (WbCommands.this.shell.tab().view == null) {
            return null;
         } else {
            BComponent target = (BComponent)WbCommands.this.shell.tab().view.getCurrentValue();
            if (target == null) {
               return null;
            } else {
               BSpace space = target.getComponentSpace();
               if (space.getNavParent() instanceof BBogFile) {
                  BBogFile bogFile = (BBogFile)space.getNavParent();
                  WbBogFilePassPhraseUtil.openDialog(this.getOwner(), bogFile, WbCommands.this::updateBogCommands);
               }

               return null;
            }
         }
      }
   }

   public class CloseCommand extends WbCommands.WbCommand {
      CloseCommand() {
         super("commands.close");
      }

      public CommandArtifact doInvoke() {
         WbMain.closeFrame((BNiagaraWbFrame)WbCommands.this.shell);
         return null;
      }
   }

   class CloseOtherTabsCommand extends WbCommands.WbCommand {
      BViewTab tabToKeep;

      CloseOtherTabsCommand(BViewTab tabToKeep) {
         super("commands.closeOtherTabs");
         this.tabToKeep = tabToKeep;
         if (tabToKeep != null) {
            this.accelerator = null;
         }
      }

      public CommandArtifact doInvoke(CommandEvent commandEvent) {
         BViewTab tabToKeep = this.tabToKeep;
         if (tabToKeep == null) {
            tabToKeep = WbCommands.this.shell.tab();
         }

         for (BViewTab tab : WbCommands.this.shell.pane.views.getTabs()) {
            if (tab != tabToKeep) {
               tab.closeTab(commandEvent.getInputEvent());
            }
         }

         return null;
      }
   }

   class CloseTabCommand extends WbCommands.WbCommand {
      BViewTab tab;

      CloseTabCommand(BViewTab tab) {
         super("commands.closeTab");
         this.tab = tab;
         this.setEnabled(tab != null);
         if (tab != null) {
            this.accelerator = null;
         }
      }

      public CommandArtifact doInvoke(CommandEvent event) {
         BViewTab tab = this.tab;
         if (tab == null) {
            tab = WbCommands.this.shell.tab();
         }

         tab.closeTab(event.getInputEvent());
         return null;
      }
   }

   public class ConsoleGroup extends ToggleCommandGroup<WbCommands.ConsoleToggle> {
      protected void selected(WbCommands.ConsoleToggle cmd) {
         if (WbCommands.this.shell.pane != null) {
            WbCommands.this.shell.pane.setConsoleVisible(cmd == WbCommands.this.console);
            if (cmd == WbCommands.this.console) {
               WbCommands.this.shell.pane.console.prime();
            } else {
               WbCommands.this.activePlugin.invoke();
            }
         }
      }
   }

   public class ConsoleKillCommand extends WbCommands.WbCommand {
      public ConsoleKillCommand() {
         super("commands.consoleKill");
      }

      public CommandArtifact doInvoke() {
         if (WbCommands.this.shell.pane != null) {
            WbCommands.this.shell.pane.console.kill();
         }

         return null;
      }
   }

   public class ConsoleNextCommand extends WbCommands.WbCommand {
      public ConsoleNextCommand() {
         super("commands.consoleNext");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.pane.console.next();
         return null;
      }
   }

   public class ConsolePrevCommand extends WbCommands.WbCommand {
      public ConsolePrevCommand() {
         super("commands.consolePrev");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.pane.console.prev();
         return null;
      }
   }

   public class ConsoleToggle extends WbCommands.WbToggleCommand {
      ConsoleToggle(String key) {
         super(key);
      }

      public CommandArtifact doInvoke() {
         if (this == WbCommands.this.console) {
            WbCommands.this.shell.pane.console.prime();
         }

         return null;
      }
   }

   public class ExitCommand extends WbCommands.WbCommand {
      ExitCommand() {
         super("commands.exit");
      }

      public CommandArtifact doInvoke() {
         WbMain.exit((BNiagaraWbFrame)WbCommands.this.shell);
         return null;
      }
   }

   class ExportCommand extends WbCommands.WbCommand {
      ExportCommand() {
         super("commands.export");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BExportDialog.invoke(WbCommands.this.shell, false);
      }

      public void updateEnabled() {
         try {
            this.setEnabled(BExportDialog.hasExporters(WbCommands.this.shell));
         } catch (Exception var2) {
            this.setEnabled(false);
         }
      }
   }

   public class FindFilesCommand extends WbCommands.WbCommand {
      public FindFilesCommand() {
         super("commands.findFiles");
      }

      public CommandArtifact doInvoke() {
         BFileSearch.findFiles(WbCommands.this.shell);
         return null;
      }
   }

   public class FindStationsCommand extends WbCommands.WbCommand {
      FindStationsCommand() {
         super("commands.findStations");
      }

      public CommandArtifact doInvoke() throws Exception {
         BStationFinder finder = new BStationFinder();
         int r = BDialog.open(WbCommands.this.shell, this.getLabel(), finder, 3);
         if (r == 2) {
            return null;
         } else {
            BOrd ord = finder.result();
            if (ord == null) {
               return null;
            } else {
               WbCommands.this.shell.hyperlink(ord);
               return null;
            }
         }
      }
   }

   public class FipsRestartCommand extends WbCommands.WbCommand {
      FipsRestartCommand() {
         super("commands.fipsRestart");
      }

      public CommandArtifact doInvoke() {
         WbMain.restart(true, (BNiagaraWbFrame)WbCommands.this.shell);
         return null;
      }
   }

   public class ForwardCommand extends WbCommands.WbCommand {
      BOrd ord;

      ForwardCommand(WbHistory.Entry entry) {
         super("commands.forward");
         if (entry != null) {
            this.ord = entry.ord;
            this.label = entry.name;
            this.icon = entry.icon;
            this.accelerator = null;
         }
      }

      public CommandArtifact doInvoke() throws Exception {
         BOrd ord = this.ord;
         if (ord == null) {
            ord = WbCommands.this.shell.tab().history.forward();
         } else {
            BOrd test = WbCommands.this.shell.tab().history.forward();

            while (test != null && test != ord) {
               test = WbCommands.this.shell.tab().history.forward();
            }
         }

         if (ord != null) {
            WbCommands.this.shell.hyperlink(new NHyperlinkInfo(ord, BHyperlinkMode.replace, false));
         }

         return null;
      }
   }

   class ForwardMenuController implements MenuController {
      public boolean isMenuDistinct() {
         return true;
      }

      public BMenu getMenu(BAbstractButton b) {
         BMenu menu = new BMenu();
         WbHistory.Entry[] list = WbCommands.this.shell.tab().history.getForwardHistory();

         for (int i = 0; i < list.length && i < 10; i++) {
            menu.add("forward" + i, WbCommands.this.new ForwardCommand(list[i]));
         }

         return menu;
      }
   }

   public class GotoFileCommand extends WbCommands.WbCommand {
      GotoFileCommand() {
         super("commands.gotoFile");
      }

      public CommandArtifact doInvoke() {
         BGotoFile.go(WbCommands.this.shell);
         return null;
      }
   }

   public class HelpBajadocOnTargetCommand extends WbCommands.WbCommand {
      HelpBajadocOnTargetCommand() {
         super("commands.help.bajadocOnTarget");
      }

      public CommandArtifact doInvoke() {
         BWbView view = WbCommands.this.shell.getActiveView();
         BObject target = view.getCurrentValue();
         if (target != null) {
            WbCommands.HelpUtil.openBajadocHelp(WbCommands.this.shell, target);
         }

         return null;
      }
   }

   public class HelpContentsCommand extends WbCommands.WbCommand {
      HelpContentsCommand() {
         super("commands.help.contents");
      }

      public CommandArtifact doInvoke() {
         WbMain.openHelp(WbCommands.this.shell, BOrd.make("workbench:/help/contents.html"));
         return null;
      }
   }

   public class HelpFindBajadocCommand extends WbCommands.WbCommand {
      private Method findBajadocOrd;

      HelpFindBajadocCommand() {
         super("commands.help.findBajadoc");
      }

      public CommandArtifact doInvoke() {
         try {
            Class<?> bajadocFinder = Sys.loadClass("help", "com.tridium.help.BajadocFinder");
            this.findBajadocOrd = bajadocFinder.getMethod("findBajadocOrd", BWidget.class, String.class);
            if (WbCommands.HelpUtil.helpSystemExists()) {
               this.find();
            } else {
               WbCommands.HelpUtil.loadHelp(WbCommands.this.shell);
               if (WbCommands.HelpUtil.helpSystemExists()) {
                  this.find();
               }
            }

            return null;
         } catch (Exception var2) {
            var2.printStackTrace();
            throw new BajaRuntimeException(var2);
         }
      }

      private void find() throws Exception {
         BMruTextDropDown field = new BMruTextDropDown("findBajadoc", 30);
         String selection = WbCommands.this.shell.getSelectedText();
         if (selection != null) {
            field.setText(selection);
         }

         BEdgePane edge = new BEdgePane();
         edge.setTop(field);
         int r = BDialog.open(WbCommands.this.shell, this.getLabel(), edge, 3, BDialog.QUESTION_ICON);
         if (r == 1) {
            String pattern = field.getTextAndSave();
            BOrd ord = this.findBajadocOrd(pattern);
            if (ord != null) {
               WbMain.openHelp(WbCommands.this.shell, ord);
            } else {
               WbCommands.this.shell.showStatus("Not found: " + pattern);
            }
         }
      }

      private BOrd findBajadocOrd(String pattern) throws Exception {
         return (BOrd)this.findBajadocOrd.invoke(null, WbCommands.this.shell, pattern);
      }
   }

   public class HelpGuideOnTargetCommand extends WbCommands.WbCommand {
      HelpGuideOnTargetCommand() {
         super("commands.help.guideOnTarget");
      }

      public CommandArtifact doInvoke() {
         BWbView view = WbCommands.this.shell.getActiveView();
         BObject target = view.getCurrentValue();
         if (target != null) {
            BOrd ord = ObjectUtil.getGuideHelpOrd(target);
            WbMain.openHelp(WbCommands.this.shell, ord);
         }

         return null;
      }
   }

   public class HelpOnViewCommand extends WbCommands.WbCommand {
      HelpOnViewCommand() {
         super("commands.help.onView");
      }

      public CommandArtifact doInvoke() {
         BWbView view = WbCommands.this.shell.getActiveView();
         BOrd ord = WbCommands.HelpUtil.getHelpOrdForTargetView(view);
         if (!ord.isNull()) {
            WbMain.openHelp(WbCommands.this.shell, ord);
         }

         return null;
      }
   }

   public static final class HelpUtil {
      private static Method helpSystemExists;
      private static Method loadHelp;

      private HelpUtil() {
      }

      public static BOrd getReferenceHelpOrd(BObject target) {
         try {
            String qname = target.getClass().getName();
            Class<?> bajadocIndex = Sys.loadClass("help", "com.tridium.help.BajadocIndex");
            Class<?> bajadocEntry = Sys.loadClass("help", "com.tridium.help.BajadocIndex$Entry");
            Method instance = bajadocIndex.getMethod("instance");
            Method getFirstEntry = bajadocIndex.getMethod("getFirstEntry", String.class, Optional.class);
            Method toOrd = bajadocEntry.getMethod("toOrd");
            Object index = instance.invoke(null);
            Object entry = getFirstEntry.invoke(index, qname, Optional.empty());
            return entry != null ? (BOrd)toOrd.invoke(entry) : BOrd.NULL;
         } catch (Exception var9) {
            var9.printStackTrace();
            return BOrd.NULL;
         }
      }

      public static void openBajadocHelp(BWbShell shell, BObject target) {
         try {
            if (!helpSystemExists()) {
               loadHelp(shell);
            }

            if (helpSystemExists()) {
               BOrd ord = getReferenceHelpOrd(target);
               if (!ord.isNull()) {
                  WbMain.openHelp(shell, ord);
               } else {
                  String targetType = target.getType().getTypeName();
                  String msg = UiLexicon.bajaui().getText("help.noDocumentation", new Object[]{targetType});
                  shell.showStatus(msg);
               }
            }
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }

      public static void loadHelp(BWbShell shell) throws Exception {
         loadHelp.invoke(null, shell);
      }

      public static boolean helpSystemExists() throws Exception {
         return (Boolean)helpSystemExists.invoke(null);
      }

      public static BOrd getHelpOrdForTargetView(BObject target) {
         BOrd ord = BOrd.NULL;
         if (target instanceof BWebWidget) {
            Optional<TypeInfo> typeInfo = ((BWebWidget)target).getTypeInfoFromJs();
            if (typeInfo.isPresent()) {
               ord = ObjectUtil.getGuideHelpOrd(typeInfo.get());
            }
         } else {
            ord = ObjectUtil.getGuideHelpOrd(target);
         }

         return ord;
      }

      static {
         try {
            Class<?> helpSystem = Sys.loadClass("help", "com.tridium.help.HelpSystem");
            helpSystemExists = helpSystem.getMethod("exists");
            Class<?> helpSideBar = Sys.loadClass("help", "com.tridium.help.ui.BHelpSideBar");
            loadHelp = helpSideBar.getMethod("loadHelp", BWidget.class);
         } catch (Exception var2) {
            var2.printStackTrace();
         }
      }
   }

   public class HomeCommand extends WbCommands.WbCommand {
      HomeCommand() {
         super("commands.home");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.hyperlink(WbCommands.this.profile.getHomeOrd());
         return null;
      }
   }

   public class LogoffCommand extends WbCommands.WbCommand {
      LogoffCommand() {
         super("commands.logoff");
      }

      public CommandArtifact doInvoke() {
         BWbApplication.autoLogoff();
         return null;
      }
   }

   public static enum Mode {
      NONE,
      MINI,
      SINGLE,
      DOUBLE,
      MASTER;
   }

   public class ModeSelectionCommand extends WbCommands.WbToggleCommand {
      private WbCommands.Mode mode;

      public ModeSelectionCommand(WbCommands.Mode mode) {
         super("commands.sideBarMode." + mode.toString().toLowerCase());
         this.mode = mode;
      }

      public WbCommands.Mode getMode() {
         return this.mode;
      }
   }

   public class ModeSelectionGroup extends ToggleCommandGroup<WbCommands.ModeSelectionCommand> {
      protected void selected(WbCommands.ModeSelectionCommand command) {
         super.selected(command);
         if (WbCommands.this.shell != null && WbCommands.this.shell.pane != null && WbCommands.this.shell.pane.sideBar != null) {
            WbCommands.this.shell.pane.sideBar.open(command.getMode());
         }
      }
   }

   public static class NavBajadocCommand extends Command {
      private final BObject target;

      public NavBajadocCommand(BWidget widget, String key, BObject target) {
         super(widget, UiLexicon.bajaui(), key);
         this.target = target;
      }

      public CommandArtifact doInvoke() {
         BWidgetShell shell = this.getShell();
         if (shell instanceof BWbShell) {
            WbCommands.HelpUtil.openBajadocHelp((BWbShell)shell, this.target);
         }

         return null;
      }
   }

   public static class NavHelpCommand extends Command {
      BOrd ord;

      public NavHelpCommand(BWidget widget, Lexicon lex, String key, BObject target) {
         super(widget, lex, key);
         this.ord = WbCommands.HelpUtil.getHelpOrdForTargetView(target);
      }

      public NavHelpCommand(BWidget widget, String key, BObject target) {
         this(widget, UiLexicon.bajaui(), key, target);
      }

      public CommandArtifact doInvoke() {
         if (!this.ord.isNull()) {
            BWidgetShell shell = this.getShell();
            if (shell instanceof BWbShell) {
               WbMain.openHelp((BWbShell)shell, this.ord);
            }
         }

         return null;
      }
   }

   class NewTabCommand extends WbCommands.WbCommand {
      NewTabCommand() {
         super("commands.newTab");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.hyperlink(new HyperlinkInfo(WbCommands.this.shell.getActiveOrd(), BHyperlinkMode.newTab));
         return null;
      }
   }

   class NewWindowCommand extends WbCommands.WbCommand {
      NewWindowCommand() {
         super("commands.newWindow");
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.hyperlink(new HyperlinkInfo(WbCommands.this.shell.getActiveOrd(), BHyperlinkMode.newShell));
         return null;
      }
   }

   class NextTabCommand extends WbCommands.WbCommand {
      NextTabCommand() {
         super("commands.nextTab");
         this.setEnabled(false);
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.pane.views.selectNextTab();
         return null;
      }
   }

   public class NonFipsRestartCommand extends WbCommands.WbCommand {
      NonFipsRestartCommand() {
         super("commands.nonFipsRestart");
      }

      public CommandArtifact doInvoke() {
         WbMain.restart(false, (BNiagaraWbFrame)WbCommands.this.shell);
         return null;
      }
   }

   public class OpenDirCommand extends WbCommands.WbCommand {
      OpenDirCommand() {
         super("commands.open.dir");
      }

      public CommandArtifact doInvoke() throws Exception {
         OpenUtil.openDir(WbCommands.this.shell);
         return null;
      }
   }

   public class OpenFileCommand extends WbCommands.WbCommand {
      OpenFileCommand() {
         super("commands.open.file");
      }

      public CommandArtifact doInvoke() throws Exception {
         OpenUtil.openFile(WbCommands.this.shell);
         return null;
      }
   }

   public class OpenMenuCommand extends WbCommands.WbToggleCommand {
      public OpenMenuCommand() {
         super("commands.open");
      }

      public CommandArtifact doInvoke() {
         return null;
      }
   }

   public class OpenMenuController implements MenuController {
      public boolean isMenuDistinct() {
         return false;
      }

      public BMenu getMenu(BAbstractButton b) {
         return WbCommands.this.makeOpenMenu();
      }
   }

   public class OpenOrdCommand extends WbCommands.WbCommand {
      OpenOrdCommand() {
         super("commands.open.ord");
      }

      public CommandArtifact doInvoke() throws Exception {
         OpenUtil.openOrd(WbCommands.this.shell);
         return null;
      }
   }

   public class OpenQueryCommand extends WbCommands.WbCommand {
      OpenQueryCommand() {
         super("commands.open.query");
      }

      public CommandArtifact doInvoke() throws Exception {
         OpenUtil.openQuery(WbCommands.this.shell);
         return null;
      }
   }

   public class OptionsCommand extends WbCommands.WbCommand {
      OptionsCommand() {
         super("commands.options");
      }

      public CommandArtifact doInvoke() {
         BOptionsEditor.openInDialog(WbCommands.this.shell);
         return null;
      }
   }

   public class PathBarUsesNavFileCommand extends WbCommands.WbToggleCommand {
      public PathBarUsesNavFileCommand() {
         super("commands.pathBarUsesNavFile");
         this.setSelected(true);
      }
   }

   public class PluginCommand extends WbCommands.WbCommand {
      private int id;

      public PluginCommand(int id) {
         super("commands." + BWbView.commandIdToString(id));
         this.id = id;
      }

      public CommandArtifact doInvoke() throws Exception {
         BWbView view = WbCommands.this.shell.getActiveView();
         return view != null ? view.invokeCommand(this.id) : null;
      }
   }

   class PrevTabCommand extends WbCommands.WbCommand {
      PrevTabCommand() {
         super("commands.prevTab");
         this.setEnabled(false);
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.pane.views.selectPrevTab();
         return null;
      }
   }

   public class RecentOrdsCommand extends WbCommands.WbCommand {
      RecentOrdsCommand() {
         super("commands.recentOrds");
      }

      public CommandArtifact doInvoke() throws Exception {
         WbHistory.Entry[] recent = BNiagaraWbShell.history.recentList();
         BRecentList list = new BRecentList();
         list.load(recent);
         int r = BDialog.open(WbCommands.this.shell, this.getLabel(), list, 3);
         if (r == 2) {
            return null;
         } else {
            HyperlinkInfo info = list.result();
            if (info == null) {
               return null;
            } else {
               WbCommands.this.shell.hyperlink(info);
               return null;
            }
         }
      }
   }

   public class RefreshCommand extends WbCommands.WbCommand {
      RefreshCommand() {
         super("commands.refresh");
      }

      public CommandArtifact doInvoke(CommandEvent event) throws Exception {
         if (event.isControlDown()) {
            WbCommands.this.trimCaches();
         }

         BWbView view = WbCommands.this.shell.getActiveView();
         if (view != null && event.isControlDown()) {
            NHyperlinkInfo.clearCache(view);
         }

         WbCommands.this.shell.hyperlink(new NHyperlinkInfo(WbCommands.this.shell.getActiveOrd(), BHyperlinkMode.replace, false));
         return null;
      }
   }

   public class RefreshTabsCommand extends WbCommands.WbCommand {
      public RefreshTabsCommand() {
         super("commands.refreshtabs");
      }

      public CommandArtifact doInvoke(CommandEvent event) throws Exception {
         if (event.isControlDown()) {
            WbCommands.this.trimCaches();
         }

         BViewTab[] tabs = WbCommands.this.shell.pane.views.getTabs();
         BViewTab activeTab = WbCommands.this.shell.tab();
         int id = -1;

         for (int i = 0; i < tabs.length; i++) {
            if (activeTab == tabs[i]) {
               id = i;
            }

            BOrd ord = tabs[i].getOrd();

            try {
               BISession session = BOrd.toSession(ord.get());
               if (session instanceof BFoxSession) {
                  ((BFoxSession)session).userActivity();
               }
            } catch (Exception var8) {
            }

            tabs[i].closeTab(event.getInputEvent());
            WbCommands.this.shell.hyperlink(new HyperlinkInfo(ord, BHyperlinkMode.newTab));
         }

         tabs = WbCommands.this.shell.pane.views.getTabs();
         if (id > -1) {
            WbCommands.this.shell.pane.views.selectLabelPane(tabs[id]);
         }

         return null;
      }
   }

   public class ReplaceInFilesCommand extends WbCommands.WbCommand {
      public ReplaceInFilesCommand() {
         super("commands.replaceInFiles");
      }

      public CommandArtifact doInvoke() {
         BFileSearch.replaceInFiles(WbCommands.this.shell);
         return null;
      }
   }

   class SaveAllCommand extends WbCommands.WbCommand {
      SaveAllCommand() {
         super("commands.saveAll");
      }

      public CommandArtifact doInvoke() {
         BSaveAllDialog.saveAll(WbCommands.this.shell, this.getLabel());
         return null;
      }
   }

   public class SaveBogCommand extends WbCommands.WbCommand {
      SaveBogCommand() {
         super("commands.saveBog");
      }

      public CommandArtifact doInvoke() throws Exception {
         if (WbCommands.this.shell.tab().view == null) {
            return null;
         } else {
            BComponent target = (BComponent)WbCommands.this.shell.tab().view.getCurrentValue();
            if (target == null) {
               return null;
            } else {
               BSpace space = target.getComponentSpace();
               if (space.getNavParent() instanceof BSubSpaceFile) {
                  BSubSpaceFile file = (BSubSpaceFile)space.getNavParent();
                  file.save();
                  WbCommands.this.updateBogCommands();
               }

               return null;
            }
         }
      }
   }

   public class SaveCommand extends WbCommands.WbCommand {
      SaveCommand() {
         super("commands.save");
      }

      public CommandArtifact doInvoke() {
         if (WbCommands.this.shell.tab().view != null) {
            WbCommands.this.shell.save(WbCommands.this.shell.tab().view);
         }

         return null;
      }
   }

   public class SessionInfoCommand extends WbCommands.WbCommand {
      SessionInfoCommand() {
         super("commands.sessionInfo");
      }

      public CommandArtifact doInvoke(CommandEvent event) throws Exception {
         try {
            BISession session = WbCommands.this.shell.getActiveView().getCurrentValueSession();
            if (session instanceof BSession) {
               BSession bsession = (BSession)session;
               BSessionInfo info = bsession.getSessionInfo();
               if (info != null) {
                  BSessionInfoDialog dlg = this.getDialog(info);
                  if (dlg != null) {
                     dlg.show(WbCommands.this.shell, info);
                  }
               }
            }
         } catch (Exception var6) {
         }

         return null;
      }

      private BSessionInfoDialog getDialog(BSessionInfo sessionInfo) {
         AgentList list = Sys.getRegistry().getSpecificAgents(sessionInfo.getType().getTypeInfo());
         if (list != null && list.size() != 0) {
            AgentInfo info = list.get(0);
            return (BSessionInfoDialog)info.getInstance();
         } else {
            return null;
         }
      }
   }

   public class ShowSideBarCommand extends WbCommands.WbToggleCommand {
      public ShowSideBarCommand() {
         super("commands.showSideBar");
      }

      public void setSelected(boolean sel) {
         super.setSelected(sel);
         WbCommands.this.shell.pane.setSideBarVisible(sel);
      }
   }

   public class SideBarCommand extends Command {
      TypeInfoSpec spec;

      SideBarCommand(TypeInfo info) {
         super(WbCommands.this.shell, "SideBar");
         this.spec = new TypeInfoSpec(info);
         this.label = this.spec.label;
         this.icon = this.spec.icon;
      }

      public CommandArtifact doInvoke() {
         WbCommands.this.shell.pane.sideBar.open((BIWbSideBar)this.spec.info.getInstance());
         return null;
      }
   }

   public class SideBarMenuCommand extends WbCommands.WbToggleCommand {
      public SideBarMenuCommand() {
         super("commands.sideBarMenu");
      }

      public CommandArtifact doInvoke() {
         return null;
      }
   }

   public class SideBarMenuController implements MenuController {
      public boolean isMenuDistinct() {
         return false;
      }

      public BMenu getMenu(BAbstractButton b) {
         return WbCommands.this.buildSideBarMenu();
      }
   }

   public class SideBarModeSelectorController implements MenuController {
      public boolean isMenuDistinct() {
         return false;
      }

      public BMenu getMenu(BAbstractButton b) {
         return WbCommands.this.buildModeSelectorMenu();
      }
   }

   public class ToolCommand extends Command {
      TypeInfoSpec spec;

      ToolCommand(TypeInfo info) {
         super(WbCommands.this.shell, "Tool");
         this.spec = new TypeInfoSpec(info);
         this.label = this.spec.label;
         this.icon = this.spec.icon;
      }

      public CommandArtifact doInvoke() throws Exception {
         BWbTool tool;
         if (this.spec.info.is(BWbNavNodeTool.TYPE)) {
            tool = BWbNavNodeTool.make(this.spec.info.toString());
         } else {
            tool = (BWbTool)this.spec.info.getInstance();
         }

         return tool.invoke(WbCommands.this.shell);
      }
   }

   public class UpLevelCommand extends WbCommands.WbCommand {
      BOrd ord;

      UpLevelCommand(BOrd ord) {
         super("commands.upLevel");
         this.ord = ord;
         if (ord != null) {
            this.label = WbHistory.toName(ord);
            this.icon = null;
            this.accelerator = null;
         }
      }

      public CommandArtifact doInvoke() throws Exception {
         BOrd ord = this.ord;
         if (ord == null) {
            ord = WbCommands.this.shell.tab().ord.getParent();
         }

         if (ord != null) {
            WbCommands.this.shell.hyperlink(ord);
         }

         return null;
      }
   }

   class UpLevelMenuController implements MenuController {
      public boolean isMenuDistinct() {
         return true;
      }

      public BMenu getMenu(BAbstractButton b) {
         BMenu menu = new BMenu();
         BViewTab tab = WbCommands.this.shell.tab();
         if (tab != null && tab.ord != null) {
            for (BOrd ord = tab.ord.getParent(); ord != null && !ord.isNull(); ord = ord.getParent()) {
               menu.add(null, WbCommands.this.new UpLevelCommand(ord));
            }
         }

         return menu;
      }
   }

   public class WbCommand extends Command {
      public WbCommand(String keyBase) {
         super(WbCommands.this.shell, UiLexicon.bajaui().module, keyBase);
      }
   }

   public class WbToggleCommand extends ToggleCommand {
      public WbToggleCommand(String keyBase) {
         super(WbCommands.this.shell, UiLexicon.bajaui().module, keyBase);
      }
   }
}
