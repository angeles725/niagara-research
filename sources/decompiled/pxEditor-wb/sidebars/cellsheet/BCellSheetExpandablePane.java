package com.tridium.px.editor.sidebars.cellsheet;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.BBrush;
import javax.baja.gx.Graphics;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.pane.BPane;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "summary",
      type = "BWidget",
      defaultValue = "new BNullWidget()"
   ), @NiagaraProperty(
      name = "expansion",
      type = "BWidget",
      defaultValue = "new BNullWidget()"
   ), @NiagaraProperty(
      name = "expanderHalign",
      type = "BHalign",
      defaultValue = "BHalign.right"
   ), @NiagaraProperty(
      name = "summaryBackground",
      type = "BBrush",
      defaultValue = "BBrush.NULL"
   )})
@NiagaraTopic(
   name = "expanderEvent",
   eventType = "BWidgetEvent"
)
public class BCellSheetExpandablePane extends BPane {
   public static final Property summary = newProperty(0, new BNullWidget(), null);
   public static final Property expansion = newProperty(0, new BNullWidget(), null);
   public static final Property expanderHalign = newProperty(0, BHalign.right, null);
   public static final Property summaryBackground = newProperty(0, BBrush.NULL, null);
   public static final Topic expanderEvent = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BCellSheetExpandablePane.class);
   private static final double GAP = 2.0;
   private static final double SIZE = 17.0;
   protected boolean isExpanded = false;
   protected boolean expanderVisible = true;
   protected RectGeom button = new RectGeom();
   private boolean overButton;

   public BWidget getSummary() {
      return (BWidget)this.get(summary);
   }

   public void setSummary(BWidget v) {
      this.set(summary, v, null);
   }

   public BWidget getExpansion() {
      return (BWidget)this.get(expansion);
   }

   public void setExpansion(BWidget v) {
      this.set(expansion, v, null);
   }

   public BHalign getExpanderHalign() {
      return (BHalign)this.get(expanderHalign);
   }

   public void setExpanderHalign(BHalign v) {
      this.set(expanderHalign, v, null);
   }

   public BBrush getSummaryBackground() {
      return (BBrush)this.get(summaryBackground);
   }

   public void setSummaryBackground(BBrush v) {
      this.set(summaryBackground, v, null);
   }

   public void fireExpanderEvent(BWidgetEvent event) {
      this.fire(expanderEvent, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BCellSheetExpandablePane(BWidget summary, BWidget expansion, BHalign align) {
      this.setSummary(summary);
      this.setExpansion(expansion);
      this.setExpanderHalign(align);
   }

   public BCellSheetExpandablePane(BWidget summary, BWidget expansion) {
      this.setSummary(summary);
      this.setExpansion(expansion);
   }

   public BCellSheetExpandablePane() {
   }

   public boolean isExpanded() {
      return this.isExpanded;
   }

   public void setExpanded(boolean isExpanded) {
      if (this.isExpanded != isExpanded) {
         this.isExpanded = isExpanded;
         if (isExpanded) {
            this.relayoutSync();
            this.scrollToVisible(new RectGeom(0.0, 0.0, this.getWidth(), this.getHeight()));
         } else {
            this.relayout();
         }
      }
   }

   public void setExpanderVisible(boolean expanderVisible) {
      if (this.expanderVisible != expanderVisible) {
         this.expanderVisible = expanderVisible;
         this.relayout();
      }
   }

   public void computePreferredSize() {
      BWidget summary = this.getSummary();
      summary.computePreferredSize();
      double pw = summary.getPreferredWidth();
      double ph = summary.getPreferredHeight();
      if (this.expanderVisible) {
         pw += 19.0;
         ph = Math.max(ph, 17.0);
      }

      if (this.isExpanded) {
         BWidget expansion = this.getExpansion();
         expansion.computePreferredSize();
         pw = Math.max(pw, expansion.getPreferredWidth());
         ph += expansion.getPreferredHeight();
      }

      this.setPreferredSize(pw, ph);
   }

   public void doLayout(BWidget[] kids) {
      double w = this.getWidth();
      double h = this.getHeight();
      BWidget summary = this.getSummary();
      summary.computePreferredSize();
      double sw = summary.getPreferredWidth();
      double sh = summary.getPreferredHeight();
      double seh = Math.max(sh, 17.0);
      double gap = summary.isNull() ? 0.0 : 2.0;
      if (this.expanderVisible) {
         if (this.getExpanderHalign() == BHalign.right) {
            summary.setBounds(0.0, (seh - sh) / 2.0, Math.min(sw, w - gap - 17.0), sh);
            this.button.set(sw + gap, (seh - 17.0) / 2.0, 17.0, Math.max(sh, 19.0));
         } else {
            this.button.set(0.0, (seh - 17.0) / 2.0, 17.0, Math.max(sh, 19.0));
            summary.setBounds(17.0 + gap, (seh - sh) / 2.0, Math.min(sw, w - gap - 17.0), sh);
         }
      } else {
         summary.setBounds(0.0, (seh - sh) / 2.0, sw, sh);
         this.button.set(0.0, 0.0, 0.0, 0.0);
      }

      BWidget expansion = this.getExpansion();
      if (this.isExpanded) {
         expansion.setBounds(0.0, seh, w, h - seh);
      } else {
         expansion.setBounds(0.0, 0.0, 0.0, 0.0);
      }
   }

   public void paint(Graphics g) {
      BBrush bg = this.getSummaryBackground();
      if (!bg.isNull()) {
         g.setBrush(bg);
         g.fillRect(0.0, 0.0, this.getWidth(), Math.max(this.getSummary().getHeight(), 17.0));
      }

      this.paintChild(g, this.getSummary());
      if (this.isExpanded) {
         this.paintChild(g, this.getExpansion());
      }

      if (this.expanderVisible) {
         Theme.expandablePane().paintButton(g, this, this.button, this.isExpanded, this.overButton);
      }
   }

   public void mouseReleased(BMouseEvent event) {
      if (this.button.contains(event.getX(), event.getY())) {
         this.setExpanded(!this.isExpanded);
         this.fireExpanderEvent(new BWidgetEvent(2, this));
      }
   }

   public void mouseEntered(BMouseEvent event) {
      super.mouseEntered(event);
      this.checkOverButton(event);
   }

   public void mouseExited(BMouseEvent event) {
      this.overButton = false;
      this.repaint();
   }

   public void mouseMoved(BMouseEvent event) {
      super.mouseMoved(event);
      this.checkOverButton(event);
   }

   private void checkOverButton(BMouseEvent event) {
      double mx = event.getX();
      double my = event.getY();
      boolean nowOverButton = this.button.contains(mx, my);
      if (nowOverButton != this.overButton) {
         this.overButton = nowOverButton;
         this.repaint();
      }
   }
}
