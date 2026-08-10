package com.tridium.wiresheet;

import com.tridium.ui.BRoundedPopup;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.WiresheetTheme;
import com.tridium.ui.theme.custom.nss.StyleUtils;
import com.tridium.wiresheet.commands.PinSlotsCommand;
import com.tridium.workbench.shell.WbMain;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.gx.BImage;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.nre.util.Array;
import javax.baja.status.BStatus;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.HyperlinkInfo;
import javax.baja.ui.Subject;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.util.BWsAnnotation;
import javax.baja.wiresheet.BWireSheet;
import javax.baja.workbench.BWbEditor;
import javax.baja.workbench.BWbProfile;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.fieldeditor.BWbFieldEditor.ProfileFilter;
import javax.baja.workbench.nav.menu.NavMenuUtil;
import javax.baja.workbench.popup.BIPopupEditor;

public abstract class ComponentGlyph extends Glyph {
   public final BComponent component;
   public final Object handle;
   static AgentFilter PopupAgentFilter = AgentFilter.is(BIPopupEditor.TYPE);

   public ComponentGlyph(BWireSheetPane ws, BComponent component, BWsAnnotation anno) {
      super(ws);
      this.component = component;
      this.handle = component.getHandle();
      this.setWixelBounds(anno.p, anno.q, anno.wixelWidth, anno.wixelHeight);
   }

   @Override
   public boolean isSelectable() {
      return true;
   }

   @Override
   public final Object getSelectionKey() {
      return "Comp:" + this.handle;
   }

   @Override
   public void popup(double mx, double my) {
      boolean std = this instanceof StdComponentGlyph;
      Subject subject = this.ws.controller.selection.getSubject();
      BMenu menu = NavMenuUtil.makeMenu(this.ws.getCanvas(), subject);
      this.remove(menu, "new");
      if (std) {
         if (subject.size() == 1) {
            menu.add(null, new BSeparator());
            menu.add("pinSlots", new PinSlotsCommand(this.ws, (StdComponentGlyph)this));
         }
      } else {
         this.remove(menu, "actions");
         this.remove(menu, "linkFrom");
         this.remove(menu, "linkTo");
         this.remove(menu, "reorder");
         this.remove(menu, "composite");
      }

      menu = this.ws.getView().makeComponentPopup(menu, this.component);
      if (menu != null) {
         menu.removeConsecutiveSeparators();
         menu.open(this.ws.getCanvas(), mx, my);
      }
   }

   private void remove(BMenu menu, String name) {
      Property prop = menu.getProperty(name);
      if (prop != null) {
         menu.remove(prop);
      }
   }

   @Override
   public void doubleClicked(BMouseEvent event) {
      AgentList agents = this.component.getAgents();
      AgentList popupAgents = agents.filter(getAgentFilter(this.ws.getWbShell()));
      if (popupAgents.size() > 0) {
         this.displayPopup(event.getX(), event.getY(), popupAgents);
      } else {
         BWbShell shell = this.ws.getWbShell();
         if (shell != null) {
            shell.hyperlink(new HyperlinkInfo(this.component.getNavOrd(), event));
         }
      }
   }

   private void displayPopup(double x, double y, AgentList popupAgents) {
      AgentInfo agentInfo = popupAgents.get(0);
      BIPopupEditor pe = (BIPopupEditor)agentInfo.getInstance();
      BWbEditor editor = pe.getEditor();
      editor.loadValue(this.component);
      BRoundedPopup window = new BRoundedPopup(this.ws.getWbShell(), editor, this.component.getDisplayName(null), 15.0F, 15.0F, 3);
      Point screen = this.ws.translateToScreen(new Point(x, y));
      RectGeom rect = new RectGeom(screen.x, screen.y, this.pw, this.ph);
      window.setBoundsCenteredOn(rect);
      window.open();
      int result = window.getResult();
      if (result != 2) {
         try {
            editor.saveValue();
         } catch (CannotSaveException var14) {
            var14.printStackTrace();
         } catch (Exception var15) {
            var15.printStackTrace();
         }
      }
   }

   public void loadLinks() {
   }

   public void loadRelations() {
   }

   public void loadKnobs() {
   }

   public void loadRelationKnobs() {
   }

   public void checkForSubscribe(BWireSheet view, Array<BComponent> toSub) {
      if (!view.isRegisteredForComponentEvents(this.component)) {
         toSub.add(this.component);
      }
   }

   public void postSubscribe() {
   }

   public void changed(String propName) {
   }

   @Override
   protected void fillGrid() {
      this.ws.grid.setComponent(this.absP(), this.absQ(), this.ww, this.wh);
   }

   @Override
   public RectGeom getGeom() {
      return new RectGeom(this.p, this.q, this.ww, this.wh);
   }

   @Override
   public void paint(Graphics g) {
      super.paint(g);
      SlotCursor<Property> properties = this.component.getProperties();

      while (properties.next()) {
         Property p = properties.property();
         Type type = p.getType();
         if (type.is(BStatus.TYPE)) {
            BStatus value = (BStatus)this.component.get(p);
            if (value.isFault()) {
               this.paintFaultIndicator(g);
               break;
            }
         }
      }
   }

   protected void paintFaultIndicator(Graphics g) {
      BImage errorIcon = BImage.make(BIcon.std("warning.png"));
      RectGeom geom = this.getGeom();
      double x = geom.x + geom.width - errorIcon.getWidth();
      double y = geom.y + geom.height - errorIcon.getHeight();
      g.drawImage(errorIcon, x, y);
   }

   @Override
   public String getStyleClasses() {
      String styleClasses = super.getStyleClasses();
      if (this.component.isPendingMove()) {
         styleClasses = StyleUtils.addStyleClass(styleClasses, "cut");
      } else {
         styleClasses = StyleUtils.removeStyleClass(styleClasses, "cut");
      }

      return styleClasses;
   }

   @Override
   public void paintSelection(Graphics g) {
      int w = this.pw;
      int h = this.ph;
      WiresheetTheme theme = Theme.wiresheet();
      g.setBrush(theme.glyph().getSelectionForeground(this));
      g.strokeLine(-1.0, 2.0, -1.0, h - 3);
      g.strokeLine(w, 2.0, w, h - 3);
      g.strokeLine(2.0, -1.0, w - 3, -1.0);
      g.strokeLine(2.0, h, w - 3, h);
      g.strokeLine(0.0, 1.0, 1.0, 0.0);
      g.strokeLine(0.0, h - 2, 1.0, h - 1);
      g.strokeLine(w - 2, 0.0, w - 1, 1.0);
      g.strokeLine(w - 1, h - 2, w - 2, h - 1);
      g.fillRect(-3.0, this.ws.grid.wixel - 2, 5.0, 5.0);
      g.fillRect(w - 2, this.ws.grid.wixel - 2, 5.0, 5.0);
      g.setBrush(theme.glyph().getSelectionBackground(this));
      g.strokeRect(-4.0, this.ws.grid.wixel - 3, 6.0, 6.0);
      g.strokeRect(w - 3, this.ws.grid.wixel - 3, 6.0, 6.0);
   }

   public abstract BWsAnnotation toAnnotation();

   public static AgentFilter getAgentFilter(BWbShell shell) {
      BWbProfile profile = null;
      if (shell != null) {
         profile = shell.getProfile();
      } else {
         try {
            profile = BWbProfile.make(null, WbMain.defaultProfileType);
         } catch (Exception var3) {
            return PopupAgentFilter;
         }
      }

      return AgentFilter.and(new ProfileFilter(profile), PopupAgentFilter);
   }
}
