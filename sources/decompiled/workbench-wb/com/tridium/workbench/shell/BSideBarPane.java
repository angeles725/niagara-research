package com.tridium.workbench.shell;

import com.tridium.gx.awt.AwtGraphics;
import com.tridium.ui.theme.JavaFxTheme;
import com.tridium.ui.theme.Theme;
import java.awt.Graphics2D;
import javax.baja.gx.Graphics;
import javax.baja.gx.IGeom;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BToolPane;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.sidebar.BIWbSideBar;
import javax.baja.workbench.sidebar.BWbSideBarManager;
import javax.baja.workbench.sidebar.IWbSideBarManager;

@NiagaraType
public class BSideBarPane extends BEdgePane {
   public static final Type TYPE = Sys.loadType(BSideBarPane.class);
   public static final String defaultPickle = "workbench:NavSideBar;|toolpane=10000";
   BNiagaraWbShell shell;
   private IWbSideBarManager manager;
   private static final BToolPane TOOLPANE = (BToolPane)BToolPane.TYPE.getInstance();

   public Type getType() {
      return TYPE;
   }

   public BSideBarPane() {
      this.setCenter(this.getManager().asWidget());
   }

   private IWbSideBarManager getManager() {
      return this.manager == null ? (this.manager = new BWbSideBarManager()) : this.manager;
   }

   public BIWbSideBar[] list() {
      return this.manager.listSideBars();
   }

   void initFromProfile(BWbProfile profile) {
      try {
         BIWbSideBar[] bars = profile.getDefaultSideBars();

         for (BIWbSideBar bar : bars) {
            this.open(bar);
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }

   BIWbSideBar open(BIWbSideBar bar) {
      bar = this.manager.openSideBar(bar);
      if (this.shell != null) {
         this.showSideBar().setSelected(true);
      }

      return bar;
   }

   void open(WbCommands.Mode mode) {
      this.manager.openMode(mode);
   }

   void closeAll() {
      this.manager.closeAllSideBars();
   }

   ToggleCommand showSideBar() {
      return this.shell.commands.showSideBar;
   }

   public String pickle() {
      return this.manager.serialize();
   }

   public void unpickle(String s) {
      this.manager.deserialize(s);
   }

   public void paint(Graphics g) {
      super.paint(g);
      JavaFxTheme theme = Theme.javaFx();
      if (theme.isEnabled()) {
         int borderRadius = (int)theme.getBorderRadius(TOOLPANE);
         if (borderRadius == 0) {
            return;
         }

         int diameter = 2 * borderRadius;
         IGeom rectBounds = new RectGeom(0.0, 0.0, borderRadius, borderRadius);
         g.setBrush(Theme.pathBar().getControlBackground());
         g.fill(rectBounds);
         g.setBrush(Theme.toolPane().getControlBackground(TOOLPANE));
         Graphics2D g2 = ((AwtGraphics)g).getAwtGraphics();
         g2.fillArc(0, 0, diameter, diameter, 90, 90);
      }
   }
}
