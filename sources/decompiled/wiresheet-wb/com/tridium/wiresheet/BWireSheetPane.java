package com.tridium.wiresheet;

import com.tridium.ui.theme.Theme;
import java.util.HashMap;
import javax.baja.gx.Graphics;
import javax.baja.gx.RectGeom;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.wiresheet.BWireSheet;
import javax.baja.workbench.BWbShell;

@NiagaraType
@NiagaraProperty(
   name = "thumbnail",
   type = "BThumbnail",
   defaultValue = "new BThumbnail()"
)
public class BWireSheetPane extends BEdgePane implements WsConst {
   public static final Property thumbnail = newProperty(0, new BThumbnail(), null);
   public static final Type TYPE = Sys.loadType(BWireSheetPane.class);
   static HashMap<BOrd, RectGeom> scrollMemory = new HashMap<>();
   public WsController controller;
   public final WixelGrid grid = new WixelGrid(this);
   public final WsSelection selection = new WsSelection(this);
   public final WsCommands commands = new WsCommands(this);
   boolean initialized;
   Context context;
   private BComponent value;
   private RectGeom lastViewport;
   private BOrd ord;
   private static boolean lastShowRelations;
   private static boolean lastShowLinks;

   public BThumbnail getThumbnail() {
      return (BThumbnail)this.get(thumbnail);
   }

   public void setThumbnail(BThumbnail v) {
      this.set(thumbnail, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BWireSheetPane get(BWidget x) {
      if (x instanceof BWireSheetPane) {
         return (BWireSheetPane)x;
      } else {
         BWidget[] kids = x.getChildWidgets();

         for (int i = 0; i < kids.length; i++) {
            BWireSheetPane result = get(kids[i]);
            if (result != null) {
               return result;
            }
         }

         return null;
      }
   }

   public BScrollPane getScrollPane() {
      return (BScrollPane)this.getCenter();
   }

   public BWsCanvas getCanvas() {
      return (BWsCanvas)this.getScrollPane().getContent();
   }

   public BWireSheetPane() {
      this.setCenter(new BScrollPane(new BWsCanvas()));
      this.controller = new WsController(this);
      this.getScrollPane().getHscrollBar().setUnitIncrement(this.grid.wixel);
      this.getScrollPane().getVscrollBar().setUnitIncrement(this.grid.wixel);
      this.getScrollPane().getHscrollBar().setBlockIncrement(this.grid.wixel * 10);
      this.getScrollPane().getVscrollBar().setBlockIncrement(this.grid.wixel * 10);
      this.getScrollPane().getHscrollBar().setSnapToUnitIncrement(true);
      this.getScrollPane().getVscrollBar().setSnapToUnitIncrement(true);
   }

   public void setController(WsController c) {
      this.controller = c;
   }

   private BWbShell findShell(BComponent comp) {
      if (comp == null) {
         return null;
      } else {
         if (comp instanceof BWidget) {
            BWidgetShell shell = ((BWidget)comp).getShell();
            if (shell != null && shell instanceof BWbShell) {
               return (BWbShell)shell;
            }
         }

         return this.findShell((BComponent)comp.getParent());
      }
   }

   public final BWbShell getWbShell() {
      try {
         return this.findShell(this);
      } catch (Exception var2) {
         return null;
      }
   }

   public final BWireSheet getView() {
      BComplex parent = this.getParent();

      while (parent != null && !(parent instanceof BWireSheet)) {
         parent = parent.getParent();
      }

      return (BWireSheet)parent;
   }

   public void deactivated() {
      RectGeom viewport = this.getScrollPane().getViewport();
      scrollMemory.put(this.ord, viewport);
   }

   public void doLoadValue(BObject value, Context cx) {
      this.value = (BComponent)value;
      this.context = cx;
      this.init();
   }

   public void started() {
      this.init();
   }

   private void init() {
      if (this.value != null) {
         BWireSheet ws = this.getView();
         if (ws != null) {
            BComponent container = this.value;
            ws.registerForComponentEvents(container, 1);
            this.controller.updateTransferStates();
            this.commands.init();
            this.controller.load(this.value.asComponent());
            lastShowRelations = BWsOptions.make().getShowRelations();
            lastShowLinks = BWsOptions.make().getShowLinks();
            this.initialized = true;
            BWbShell shell = this.getWbShell();
            OrdTarget activeOrdTarget = shell != null ? shell.getActiveOrdTarget() : null;
            String sourceHandle = activeOrdTarget != null ? activeOrdTarget.getViewParameter("sHandle", "") : "";
            String sourceSlot = activeOrdTarget != null ? activeOrdTarget.getViewParameter("sSlot", "") : "";
            String targetHandle = activeOrdTarget != null ? activeOrdTarget.getViewParameter("tHandle", "") : "";
            String targetSlot = activeOrdTarget != null ? activeOrdTarget.getViewParameter("tSlot", "") : "";
            if (!sourceHandle.isEmpty() && !sourceSlot.isEmpty() && !targetHandle.isEmpty() && !targetSlot.isEmpty()) {
               BWidget.invokeLater(() -> {
                  this.relayoutSync();
                  this.getCanvas().rootGlyph.linkLayer.selectLink(sourceHandle, sourceSlot, targetHandle, targetSlot);
               });
            } else if (shell != null) {
               this.ord = shell.getActiveOrd();
               RectGeom scrollTo = scrollMemory.get(this.ord);
               if (scrollTo != null) {
                  this.getScrollPane().scrollToVisible(scrollTo);
               }
            }
         }
      }
   }

   public void doLayout(BWidget[] kids) {
      RectGeom rect = this.getCanvas().getRootGlyph().getGeom();
      int prefWixelWidth = 120;
      int prefWixelHeight = 120;
      if (rect != null) {
         prefWixelWidth = Math.max(120, (int)rect.x + (int)rect.width + 60);
         prefWixelHeight = Math.max(120, (int)rect.y + (int)rect.height + 60);
         if (prefWixelWidth > this.getMaxWidth()) {
            prefWixelWidth = this.getMaxWidth();
         }

         if (prefWixelHeight > this.getMaxHeight()) {
            prefWixelHeight = this.getMaxHeight();
         }
      }

      if (prefWixelWidth != this.grid.getWixelWidth() || prefWixelHeight != this.grid.getWixelHeight()) {
         this.getCanvas().getRootGlyph().setWixelBounds(0, 0, prefWixelWidth, prefWixelHeight);
         this.grid.setWixelSize(prefWixelWidth, prefWixelHeight);
         this.controller.zoom(0);
      }

      this.getCanvas().setPreferredSize(prefWixelWidth * this.grid.wixel, prefWixelHeight * this.grid.wixel);
      this.getCanvas().relayout();
      double w = this.getWidth();
      double h = this.getHeight();
      this.getScrollPane().setBounds(0.0, 0.0, w, h);
      double xoff = BWsOptions.make().getThumbnailX();
      double yoff = BWsOptions.make().getThumbnailY();
      double sb = Theme.scrollBar().getFixedWidth();
      this.getThumbnail().computePreferredSize();
      double tw = this.getThumbnail().getPreferredWidth();
      double th = this.getThumbnail().getPreferredHeight();
      double tx = xoff >= 0.0 ? xoff : w - tw - sb + xoff;
      double ty = yoff >= 0.0 ? yoff : h - th - sb + yoff;
      if (tx + tw > w - sb - 5.0) {
         tx = w - sb - 5.0 - tw;
      }

      if (ty + th > h - sb - 5.0) {
         ty = h - sb - 5.0 - th;
      }

      if (tx < 5.0) {
         tx = 5.0;
      }

      if (ty < 5.0) {
         ty = 5.0;
      }

      this.getThumbnail().setBounds(tx, ty, tw, th);
   }

   protected int getMaxWidth() {
      return BWsOptions.make().getMaxWidth();
   }

   protected int getMaxHeight() {
      return BWsOptions.make().getMaxHeight();
   }

   public void paint(Graphics g) {
      if (this.initialized) {
         if (lastShowRelations != BWsOptions.make().getShowRelations()) {
            lastShowRelations = !lastShowRelations;
            this.getWbShell().getRefreshCommand().invoke();
         }

         if (lastShowLinks != BWsOptions.make().getShowLinks()) {
            lastShowLinks = !lastShowLinks;
            this.getWbShell().getRefreshCommand().invoke();
         }
      }

      this.paintChild(g, this.getScrollPane());
      if (this.initialized && BWsOptions.make().getShowThumbnail()) {
         RectGeom viewport = this.getScrollPane().getViewport();
         if (!viewport.equals(this.lastViewport)) {
            this.getThumbnail().repaintBuffer();
         }

         this.lastViewport = viewport;
         this.paintChild(g, this.getThumbnail());
      }
   }
}
