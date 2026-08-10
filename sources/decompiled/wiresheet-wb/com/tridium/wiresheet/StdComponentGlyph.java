package com.tridium.wiresheet;

import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.WiresheetTheme;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BVector;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.RelationKnob;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BWsAnnotation;
import javax.baja.wiresheet.BWireSheet;

public class StdComponentGlyph extends ComponentGlyph {
   private static final Comparator<LinkGlyph> LINK_ORDER = (link1, link2) -> {
      StdComponentGlyph source1 = link1.getSourceComponentGlyph();
      StdComponentGlyph source2 = link2.getSourceComponentGlyph();
      int nullComparison = compareWithNullChecks(source1, source2);
      if (validComparisonResult(nullComparison)) {
         return nullComparison;
      } else {
         int result = compareNames(source1, source2);
         if (result == 0) {
            Slot[] slots = source1.component.getSlotsArray();
            SlotBarGlyph link1Bar = link1.getSourceSlotGlyph();
            SlotBarGlyph link2Bar = link2.getSourceSlotGlyph();
            int linkNullComparison = compareWithNullChecks(link1Bar, link2Bar);
            return validComparisonResult(linkNullComparison) ? linkNullComparison : indexOf(slots, link1Bar.slot) - indexOf(slots, link2Bar.slot);
         } else {
            return result;
         }
      }
   };
   final TitleBarGlyph titleBar;
   final FooterBarGlyph footerBar;
   private static final int SLOT_BARS_INITIAL_CAPACITY = 21;
   private HashMap<String, SlotBarGlyph> slotBars = new HashMap<>(21);
   private boolean autoCalcWixelWidth = false;
   private int selectionIndex = -1;
   private static final boolean AUTO_SIZE_WIRESHEET_COMPONENTS = AccessController.doPrivileged(
         (PrivilegedAction<String>)(() -> System.getProperty("nsedona.autoSizeWiresheetComponents", "true"))
      )
      .equals("true");

   public StdComponentGlyph(BWireSheetPane ws, BComponent component, BWsAnnotation anno) {
      super(ws, component, anno);
      this.add(this.titleBar = new TitleBarGlyph(ws, component));
      if (BWsOptions.make().getShowRelations()) {
         this.add(this.footerBar = new RelationFooterBarGlyph(ws, component));
      } else {
         this.add(this.footerBar = new FooterBarGlyph(ws, component));
      }

      Slot[] slots = this.getPotentialSlots(true);

      for (int i = 0; i < slots.length; i++) {
         this.addSlotBar(slots[i]);
      }

      try {
         if (AUTO_SIZE_WIRESHEET_COMPONENTS) {
            this.autoCalcWixelWidth = component.getType().is(Sys.getType("nsedona:SedonaComponent"));
         }
      } catch (Exception var6) {
      }
   }

   public Slot[] getPotentialSlots(boolean includeLinks) {
      ArrayList<Slot> list = new ArrayList<>();
      SlotCursor<Slot> c = this.component.loadSlots().getSlots();

      while (c.next()) {
         Slot slot = c.slot();
         if (!Flags.isHidden(this.component, slot)) {
            if (slot.isProperty()) {
               BObject v = c.get();
               if (v.isComponent() && !(v instanceof BVector) || !includeLinks && v instanceof BLink) {
                  continue;
               }
            }

            list.add(slot);
         }
      }

      return list.toArray(new Slot[0]);
   }

   public SlotBarGlyph addSlotBar(Slot slot) {
      synchronized (this.slotBars) {
         String key = slot.getName();
         if (slot.isProperty()) {
            Type type = slot.asProperty().getType();
            if (!type.is(BLink.TYPE) && type.is(BRelation.TYPE)) {
               key = ((BRelation)this.component.get(slot.asProperty())).getRelationId();
            }
         }

         SlotBarGlyph bar = this.slotBars.get(key);
         if (bar == null) {
            bar = SlotBarGlyph.make(this.ws, this, this.component, slot);
            if (bar != null) {
               this.slotBars.put(key, bar);
               this.add(bar);
            }
         }

         return bar;
      }
   }

   public SlotBarGlyph addSlotBar(RelationKnob knob) {
      synchronized (this.slotBars) {
         String key = knob.getRelationId();
         SlotBarGlyph bar = this.slotBars.get(key);
         if (bar == null) {
            bar = new RelationKnobBarGlyph(this.ws, this, this.component, key);
            if (bar != null) {
               this.slotBars.put(key, bar);
               this.add(bar);
            }
         }

         return bar;
      }
   }

   public SlotBarGlyph getVisibleSlotBar(String name) {
      synchronized (this.slotBars) {
         SlotBarGlyph bar = this.slotBars.get(name);
         if (bar != null) {
            bar.visible = true;
         }

         return bar;
      }
   }

   public SlotBarGlyph getVisibleRelationBar(RelationKnob rKnob) {
      synchronized (this.slotBars) {
         String name = SlotPath.escape(rKnob.getRelationId());
         SlotBarGlyph bar = this.slotBars.get(name);
         if (bar == null) {
            bar = new RelationKnobBarGlyph(this.ws, this, this.component, name);
            this.slotBars.put(name, bar);
            this.add(bar);
         }

         if (bar != null) {
            bar.visible = true;
         }

         return bar;
      }
   }

   public SlotBarGlyph getVisibleRelationBar(BRelation relation, boolean isRelationKnob) {
      synchronized (this.slotBars) {
         String name = relation.getRelationId();
         SlotBarGlyph bar = this.slotBars.get(name);
         if (bar == null) {
            if (isRelationKnob) {
               bar = new RelationKnobBarGlyph(this.ws, this, this.component, relation.getRelationId());
            } else {
               bar = new RelationBarGlyph(this.ws, this, this.component, relation.getPropertyInParent(), name);
            }

            this.slotBars.put(name, bar);
            this.add(bar);
         }

         if (bar != null) {
            bar.visible = true;
         }

         return bar;
      }
   }

   public SlotBarGlyph getVisibleRelationKnobBar(BRelation relation) {
      synchronized (this.slotBars) {
         String name = relation.getRelationId();
         SlotBarGlyph bar = this.slotBars.get(name);
         if (bar == null) {
            bar = new RelationBarGlyph(this.ws, this, this.component, relation.getPropertyInParentComponent(), name);
            this.slotBars.put(name, bar);
            this.add(bar);
         }

         if (bar != null) {
            bar.visible = true;
         }

         return bar;
      }
   }

   @Override
   public void loadLinks() {
      super.loadLinks();
      this.slotBars
         .values()
         .stream()
         .filter(bar -> bar instanceof PropertyBarGlyph)
         .map(bar -> (PropertyBarGlyph)bar)
         .filter(bar -> bar.isLink && bar.linkGlyph == null)
         .map(bar -> (BLink)this.component.get(bar.property))
         .map(link -> this.ws.controller.makeLinkGlyph(this, link))
         .sorted(LINK_ORDER)
         .forEach(this.ws.controller::addLinkGlyph);
   }

   @Override
   public void loadRelations() {
      super.loadRelations();
      if (BWsOptions.make().getShowRelations()) {
         HashMap<String, SlotBarGlyph> cloneSlotBars = new HashMap<>(this.slotBars);

         for (SlotBarGlyph bar : cloneSlotBars.values()) {
            if (bar instanceof RelationBarGlyph) {
               RelationBarGlyph propBar = (RelationBarGlyph)bar;
               if (propBar.isRelation && propBar.relationGlyph == null) {
                  BRelation relation = (BRelation)this.component.get(propBar.property);

                  for (BRelation bRelation : this.component.getComponentRelations()) {
                     if (bRelation.getRelationId().equals(relation.getRelationId())) {
                        propBar.relationGlyph = this.ws.controller.addRelationLinkGlyph(this, bRelation);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void loadKnobs() {
      super.loadKnobs();

      for (SlotBarGlyph bar : this.slotBars.values()) {
         if (bar != null && bar.slot != null) {
            Knob[] knobs = this.component.getKnobs(bar.slot);

            for (int i = 0; i < knobs.length; i++) {
               this.ws.controller.addKnobGlyph(this, knobs[i]);
            }
         }
      }
   }

   @Override
   public void loadRelationKnobs() {
      if (BWsOptions.make().getShowRelations()) {
         RelationKnob[] rKnobs = this.component.getRelationKnobs();

         for (int i = 0; i < rKnobs.length; i++) {
            if (rKnobs[i] != null && rKnobs[i].getRelationOrd() != null && !rKnobs[i].getRelationOrd().equals(BOrd.NULL)) {
               this.ws.controller.addRelationLinkGlyph(this, rKnobs[i]);
            }
         }
      }
   }

   @Override
   public void checkForSubscribe(BWireSheet view, Array<BComponent> toSub) {
      super.checkForSubscribe(view, toSub);

      for (SlotBarGlyph bar : this.slotBars.values()) {
         if (bar.visible && bar instanceof PropertyBarGlyph) {
            PropertyBarGlyph pbar = (PropertyBarGlyph)bar;
            if (pbar.isComponent) {
               BComponent kid = (BComponent)this.component.get(pbar.property);
               if (!view.isRegisteredForComponentEvents(kid)) {
                  toSub.add(kid);
               }
            }
         }
      }
   }

   @Override
   public void postSubscribe() {
      for (SlotBarGlyph bar : this.slotBars.values()) {
         if (bar.visible && bar instanceof PropertyBarGlyph) {
            PropertyBarGlyph pbar = (PropertyBarGlyph)bar;
            pbar.updateValueString();
         }
      }
   }

   @Override
   public void changed(String propName) {
      super.changed(propName);
      Object bar = this.slotBars.get(propName);
      if (bar instanceof PropertyBarGlyph) {
         ((PropertyBarGlyph)bar).updateValueString();
      }
   }

   @Override
   protected void doLayout(Glyph[] kids) {
      if (this.autoCalcWixelWidth) {
         this.autoCalculateWixelWidth();
      }

      int n = 2;
      this.titleBar.setWixelBounds(0, 0, this.ww, 2);

      for (int i = 2; i < kids.length; i++) {
         SlotBarGlyph bar = (SlotBarGlyph)kids[i];
         if (bar.visible) {
            bar.setWixelBounds(0, n++, this.ww, 1);
         }
      }

      this.footerBar.setWixelBounds(0, n, this.ww, 1);
      n++;
      if (this.p + this.ww > BWsOptions.make().getMaxWidth()) {
         this.p = BWsOptions.make().getMaxWidth() - this.ww;
      }

      if (this.q + n > BWsOptions.make().getMaxHeight()) {
         this.q = BWsOptions.make().getMaxHeight() - n;
      }

      this.setWixelBounds(this.p, this.q, this.ww, n);
   }

   @Override
   public void paint(Graphics g) {
      WiresheetTheme theme = Theme.wiresheet();
      int w = this.pw;
      int h = this.ph;
      g.setBrush(theme.glyph().getBackground(this));
      g.fillRect(1.0, 1.0, w - 2, h - 2);
      this.paintChildren(g);
      g.setBrush(theme.glyph().getOutline(this));
      g.strokeLine(2.0, 0.0, w - 3, 0.0);
      g.strokeLine(2.0, h - 1, w - 3, h - 1);
      g.strokeLine(0.0, 2.0, 0.0, h - 3);
      g.strokeLine(w - 1, 2.0, w - 1, h - 3);
      g.strokeLine(0.0, 2.0, 2.0, 0.0);
      g.strokeLine(w - 3, 0.0, w - 1, 2.0);
      g.strokeLine(0.0, h - 3, 2.0, h - 1);
      g.strokeLine(w - 1, h - 3, w - 3, h - 1);
      g.strokeLine(0.0, h - this.ws.grid.wixel, w, h - this.ws.grid.wixel);
   }

   @Override
   public ResizeZone getResizeAt(double x, double y) {
      int q = (int)(y / this.ws.grid.wixel);
      if (this.q == q || this.q + 1 == q) {
         if (ResizeZone.onLeft(this, x, y)) {
            return new ResizeZone(this, 4);
         }

         if (ResizeZone.onRight(this, x, y)) {
            return new ResizeZone(this, 8);
         }
      }

      return null;
   }

   @Override
   public void print(WsPrinting p) {
      double w = this.ww * p.wixel;
      double h = this.wh * p.wixel;
      Graphics g = p.graphics;
      g.setBrush(BColor.make(15658734));
      g.fillRect(0.5, 0.5, w - 1.0, h - 1.0);
      this.printChildren(p);
      g.setBrush(BColor.black.toBrush());
      g.strokeRect(0.0, 0.0, w, h);
   }

   @Override
   public BWsAnnotation toAnnotation() {
      return BWsAnnotation.make(this.p, this.q, this.ww);
   }

   private void autoCalculateWixelWidth() {
      int newW = (int)Math.floor((this.titleBar.calcWidth() + this.ws.grid.wixel / 2.0) / this.ws.grid.wixel);
      synchronized (this.slotBars) {
         for (SlotBarGlyph bar : this.slotBars.values()) {
            if (bar.visible) {
               int sw = (int)Math.floor((bar.calcWidth() + this.ws.grid.wixel / 2.0) / this.ws.grid.wixel);
               if (sw > newW) {
                  newW = sw;
               }
            }
         }
      }

      if (newW < BWsAnnotation.DEFAULT.wixelWidth) {
         newW = BWsAnnotation.DEFAULT.wixelWidth;
      }

      this.ww = newW;
   }

   @Override
   WsSelectedState saveSelectedState() {
      return new StdComponentGlyph.SelectedState(this.getSelectionKey(), this.selectionIndex);
   }

   @Override
   void restoreSelectedState(WsSelectedState state) {
      if (this.selectionIndex != -1) {
         this.ws.selection.unregisterStdSelectionIndex(this.selectionIndex);
      }

      this.selectionIndex = ((StdComponentGlyph.SelectedState)state).selectionIndex;
      this.ws.selection.registerStdSelectionIndex(this.selectionIndex);
   }

   @Override
   void setSelected(boolean selected) {
      super.setSelected(selected);
      if (selected) {
         if (this.selectionIndex == -1) {
            this.selectionIndex = this.ws.selection.registerNewStdSelectionIndex();
         }
      } else {
         this.ws.selection.unregisterStdSelectionIndex(this.selectionIndex);
         this.selectionIndex = -1;
      }
   }

   int getSelectionIndex() {
      return this.selectionIndex;
   }

   private static int indexOf(Slot[] slots, Slot slot) {
      int i = 0;

      for (int len = slots.length; i < len; i++) {
         if (slots[i] == slot) {
            return i;
         }
      }

      return -1;
   }

   private static int compareNames(StdComponentGlyph source1, StdComponentGlyph source2) {
      String name1 = source1.component.getName();
      String name2 = source2.component.getName();
      int nullComparison = compareWithNullChecks(name1, name2);
      return validComparisonResult(nullComparison) ? nullComparison : name1.compareTo(name2);
   }

   private static boolean validComparisonResult(int comparisonResult) {
      return comparisonResult >= -1 && comparisonResult <= 1;
   }

   private static int compareWithNullChecks(Object object1, Object object2) {
      if (object1 == null && object2 == null) {
         return 0;
      } else if (object1 == null) {
         return -1;
      } else {
         return object2 == null ? 1 : 2;
      }
   }

   private class SelectedState extends WsSelectedState {
      private int selectionIndex;

      protected SelectedState(Object key, int selectionIndex) {
         super(key);
         this.selectionIndex = selectionIndex;
      }

      @Override
      void cancelRestore() {
         if (this.selectionIndex > -1) {
            StdComponentGlyph.this.ws.selection.unregisterStdSelectionIndex(this.selectionIndex);
         }
      }
   }
}
