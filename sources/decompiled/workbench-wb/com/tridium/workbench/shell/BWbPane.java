package com.tridium.workbench.shell;

import com.tridium.ui.fx.BFxContentPane;
import com.tridium.ui.theme.JavaFxTheme;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.custom.nss.StyleUtils;
import com.tridium.workbench.console.BConsole;
import javax.baja.gx.BInsets;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BAbstractButton;
import javax.baja.ui.BBorder;
import javax.baja.ui.BMenuBar;
import javax.baja.ui.BMenuItem;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BOrientation;
import javax.baja.ui.menu.BIMenu;
import javax.baja.ui.menu.BIMenuBar;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.toolbar.BIToolBar;
import javax.baja.workbench.BWbLocatorBar;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.BWbStatusBar;
import javax.baja.workbench.sidebar.BIWbSideBar;
import javax.baja.workbench.view.BWbView;

@NiagaraType
public class BWbPane extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BWbPane.class);
   private static final String[] noMenus = new String[0];
   final BNiagaraWbShell shell;
   final BWbProfile profile;
   private BIMenuBar menuBar;
   private String[] mergedMenus = noMenus;
   private BIToolBar toolBar;
   private BScrollingToolbarPane toolBarPane;
   private Property mergedToolBar;
   private BWbLocatorBar locatorBar;
   BViewTabbedPane views;
   BSideBarPane sideBar;
   BBorderPane sideBarBp;
   BConsole console;
   private BSplitPane hSplitPane;
   private BSplitPane vSplitPane;
   private BBorderPane hSplitPaneBp;
   private BFxContentPane content;
   private BWbStatusBar statusBar;

   public Type getType() {
      return TYPE;
   }

   public BWbPane() {
      throw new IllegalStateException();
   }

   public BWbPane(BNiagaraWbShell shell) {
      this.shell = shell;
      this.profile = shell.profile;
      this.setTop(new BEdgePane());
      this.buildMenuBar();
      this.buildToolBar();
      this.buildLocatorBar();
      this.buildContent();
      this.buildStatusBar();
   }

   public String getStyleId() {
      return "main-background-pane";
   }

   public void update(BViewTab tab) {
      try {
         this.updateMenuBar(tab);
         this.updateQuickSearch(tab);
         this.updateToolBar(tab);
         this.updateLocatorBar(tab);
         this.updateSideBars(tab);
         this.updateStatusBar(tab);
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public boolean hasMenu(String menuPath) {
      if (this.menuBar == null) {
         return false;
      } else {
         String[] names = TextUtil.split(menuPath, '|');
         BComponent p = this.menuBar.asWidget();

         for (String name : names) {
            BComponent c = (BComponent)p.get(name);
            if (c instanceof BSubMenuItem) {
               c = ((BSubMenuItem)c).getMenu();
            }

            if (c == null) {
               return false;
            }

            p = c;
         }

         return true;
      }
   }

   BWidget getMenuBar() {
      return this.menuBar.asWidget();
   }

   private void buildMenuBar() {
      this.menuBar = this.profile.makeMenuBar();
      if (this.menuBar != null) {
         BIMenu[] menus = this.menuBar.getMenus();

         for (BIMenu menu : menus) {
            menu.removeConsecutiveSeparators();
         }

         BEdgePane pane = new BEdgePane();
         pane.setCenter(this.menuBar.asWidget());
         BEdgePane top = (BEdgePane)this.getTop();
         top.setTop(pane);
      }
   }

   private void updateMenuBar(BViewTab tab) {
      if (this.menuBar != null) {
         for (String menuName : this.mergedMenus) {
            unregisterFromCommands(this.menuBar.removeMenu(menuName).asWidget());
         }

         this.mergedMenus = noMenus;
         BIMenu[] menus = null;

         try {
            if (tab.view != null) {
               menus = tab.view.getViewMenus();
            }
         } catch (Exception var6) {
            var6.printStackTrace();
         }

         if (menus == null) {
            this.mergedMenus = noMenus;
         } else {
            BIMenu help = this.menuBar.removeMenu("help");
            this.mergedMenus = new String[menus.length];

            for (int i = 0; i < menus.length; i++) {
               String menuName = "merge" + i;
               this.menuBar.addMenu(this.mergedMenus[i] = menuName, menus[i]);
            }

            if (help != null) {
               this.menuBar.addMenu("help", help);
            }
         }
      }
   }

   private void updateQuickSearch(BViewTab tab) {
      if (!this.shell.isAppletOrWebStart() && this.menuBar != null) {
         BWidget quickSearch = null;
         BWbView view = tab.view;

         try {
            if (tab.view != null) {
               quickSearch = this.profile.getQuickSearch(view);
            }
         } catch (Exception var7) {
            var7.printStackTrace();
         }

         if (quickSearch == null) {
            try {
               this.menuBar.removeQuickSearch();
            } catch (UnsupportedOperationException var6) {
               BWidget var8 = new BNullWidget();
               this.updateQuickSearchInPane(var8);
            }
         } else {
            try {
               this.menuBar.addQuickSearch(quickSearch);
            } catch (UnsupportedOperationException var5) {
               this.updateQuickSearchInPane(quickSearch);
            }
         }
      }
   }

   private void updateQuickSearchInPane(BWidget quickSearch) {
      BEdgePane top = (BEdgePane)((BEdgePane)this.getTop()).getTop();
      BWidget right = top.getRight();
      if (quickSearch != right && (!right.isNull() || !quickSearch.isNull())) {
         top.setRight(quickSearch);
      }
   }

   private void buildToolBar() {
      this.toolBar = this.profile.makeToolBar();
      if (this.toolBar != null) {
         this.toolBar.removeConsecutiveSeparators();
         BEdgePane pane = new BEdgePane();
         if (this.toolBar instanceof BToolBar) {
            this.toolBarPane = new BScrollingToolbarPane(10, this.toolBar);
            StyleUtils.addStyleClass(pane, "toolbar-container");
            if (this.menuBar instanceof BMenuBar) {
               BSeparator sep = new BSeparator(BOrientation.horizontal);
               sep.setStyleClasses("below-menu-bar");
               pane.setTop(sep);
            }

            pane.setCenter(this.toolBarPane);
         } else {
            pane.setCenter(this.toolBar.asWidget());
         }

         BEdgePane top = (BEdgePane)this.getTop();
         top.setCenter(pane);
      }
   }

   private void updateToolBar(BViewTab tab) {
      if (this.toolBar != null) {
         BIToolBar viewToolBar = getViewToolBar(tab);
         if (this.mergedToolBar != null) {
            if (this.toolBarPane != null) {
               unregisterFromCommands((BWidget)this.toolBarPane.get(this.mergedToolBar));
               this.toolBarPane.removeMergedToolbar(this.mergedToolBar);
            } else {
               this.toolBar.asWidget().remove(this.mergedToolBar);
            }

            this.mergedToolBar = null;
         }

         if (viewToolBar != null) {
            if (this.toolBarPane != null) {
               this.mergedToolBar = this.toolBarPane.addMergedToolbar(viewToolBar);
               this.toolBarPane.relayout();
            } else {
               this.mergedToolBar = this.toolBar.asWidget().add("merged", viewToolBar.asWidget());
            }
         }

         this.shell.commands.export.updateEnabled();
      }
   }

   private static BIToolBar getViewToolBar(BViewTab tab) {
      try {
         if (tab.view != null) {
            return tab.view.getViewToolBar();
         }
      } catch (Exception var2) {
         var2.printStackTrace();
      }

      return null;
   }

   private void buildLocatorBar() {
      this.locatorBar = this.profile.makeLocatorBar();
      if (this.locatorBar != null) {
         BBorderPane locatorPane = new BBorderPane(this.locatorBar, 3.0, 0.0, 1.0, 0.0);
         locatorPane.setBorder(BBorder.none);
         BEdgePane top = (BEdgePane)this.getTop();
         top.setBottom(locatorPane);
      }
   }

   private void updateLocatorBar(BViewTab tab) {
      try {
         if (this.locatorBar == null) {
            return;
         }

         if (this.locatorBar instanceof BNiagaraWbLocatorBar) {
            ((BNiagaraWbLocatorBar)this.locatorBar).update(tab);
         } else {
            this.locatorBar.activeViewChanged();
         }
      } catch (Throwable var3) {
         var3.printStackTrace();
      }
   }

   public double getSplitPosition() {
      return this.hSplitPane.getDividerPosition();
   }

   public void setSplitPosition(double pos) {
      if (this.sideBar != null) {
         this.hSplitPane.setDividerPosition(pos);
      }
   }

   public void setSideBarVisible(boolean visible) {
      if (this.sideBar != null) {
         this.sideBarBp.setVisible(visible);
         JavaFxTheme theme = Theme.javaFx();
         if (theme.isEnabled() && this.hSplitPaneBp != null) {
            double borderRadius = theme.getBorderRadius(this);
            this.hSplitPaneBp.setPadding(visible ? BInsets.make(0.0, borderRadius, 0.0, 0.0) : BInsets.make(0.0, borderRadius, 0.0, borderRadius));
         }

         this.hSplitPane.relayout();
      }
   }

   void setConsoleVisible(boolean visible) {
      if (this.console != null) {
         this.console.setVisible(visible);
         JavaFxTheme theme = Theme.javaFx();
         if (theme.isEnabled() && this.content != null) {
            this.content.setPadding(visible ? BInsets.make(0.0, 0.0, theme.getBorderRadius(this), 0.0) : BInsets.DEFAULT);
         }

         this.vSplitPane.relayout();
      }
   }

   private void updateSideBars(BViewTab tab) {
      if (this.sideBar != null) {
         for (BIWbSideBar bar : this.sideBar.list()) {
            try {
               bar.activeViewChanged();
            } catch (Throwable var7) {
               var7.printStackTrace();
            }
         }
      }
   }

   private void buildContent() {
      JavaFxTheme fxTheme = Theme.javaFx();
      boolean javaFxEnabled = fxTheme.isEnabled();
      double borderRadius = fxTheme.getBorderRadius(this);
      this.views = new BViewTabbedPane(this.shell, this);
      this.hSplitPane = new BSplitPane(BOrientation.horizontal, 30.0);
      this.hSplitPane.setDividerWidth(Theme.splitPane().getDividerWidth(this.hSplitPane));
      if (this.profile.hasSideBar()) {
         this.sideBar = this.shell.makeSideBar();
         this.sideBar.shell = this.shell;
         this.sideBarBp = new BBorderPane(this.sideBar, BInsets.make(0.0, 0.0, javaFxEnabled ? borderRadius : 0.0, 0.0));
         this.sideBarBp.setVisible(false);
         this.hSplitPane.setWidget1(this.sideBarBp);
      } else {
         this.hSplitPane.getWidget1().setVisible(false);
      }

      BWidget widget2;
      if (javaFxEnabled) {
         widget2 = new BFxContentPane(this.views, BInsets.make(0.0, 0.0, 0.0, borderRadius), "view-profile");
      } else {
         BBorderPane borderPane = new BBorderPane(this.views, BInsets.DEFAULT);
         borderPane.setStyleId("main-content-view");
         borderPane.setBorder(BBorder.make(BBorder.solid, Theme.borderPane().getControlForeground(borderPane)));
         borderPane.setFill(Theme.borderPane().getControlBackground(borderPane));
         widget2 = borderPane;
      }

      this.hSplitPane.setWidget2(widget2);
      this.hSplitPaneBp = new BBorderPane(this.hSplitPane, BInsets.make(0.0, borderRadius, 0.0, borderRadius));
      this.console = new BConsole(this.shell);
      this.console.setVisible(false);
      this.vSplitPane = new BSplitPane(BOrientation.vertical, 85.0);
      StyleUtils.addStyleClass(this.vSplitPane, "console-top");
      this.vSplitPane.setDividerWidth(Theme.splitPane().getDividerWidth(this.vSplitPane));
      this.vSplitPane.setWidget1(this.hSplitPaneBp);
      this.vSplitPane.setWidget2(this.console);
      BWidget centerWidget = (BWidget)(javaFxEnabled ? (this.content = new BFxContentPane(this.vSplitPane, "content-profile")) : this.vSplitPane);
      boolean paddingRequired = this.locatorBar == null;
      centerWidget = (BWidget)(paddingRequired ? new BBorderPane(centerWidget, BInsets.make(borderRadius, 0.0, borderRadius, 0.0)) : centerWidget);
      this.setCenter(centerWidget);
   }

   public void showStatus(String msg) {
      if (this.statusBar != null) {
         this.statusBar.showStatus(msg);
         this.statusBar.repaint();
      }
   }

   private void buildStatusBar() {
      this.statusBar = this.profile.makeStatusBar();
      if (this.statusBar != null) {
         this.setBottom(this.statusBar);
      }
   }

   private void updateStatusBar(BViewTab tab) {
      if (this.statusBar != null) {
         BWidget statusBarSupplement = null;
         if (tab.view != null) {
            statusBarSupplement = tab.view.getViewStatusBarSupplement();
         }

         if (statusBarSupplement == null) {
            statusBarSupplement = new BNullWidget();
         }

         this.statusBar.setSupplement(statusBarSupplement);
      }
   }

   static void unregisterFromCommands(BWidget w) {
      if (w instanceof BAbstractButton) {
         ((BAbstractButton)w).setCommand(null, false, false);
      } else if (w instanceof BMenuItem) {
         ((BMenuItem)w).setCommand(null);
      }

      for (BWidget kid : w.getChildWidgets()) {
         unregisterFromCommands(kid);
      }
   }

   public BSideBarPane getSideBar() {
      return this.sideBar;
   }
}
