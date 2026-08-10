package com.tridium.wiresheet;

import com.tridium.ui.UiEnv;
import com.tridium.wiresheet.states.NormalState;
import com.tridium.workbench.commands.PasteSpecialCommand;
import com.tridium.workbench.nav.BComponentMenuAgent;
import com.tridium.workbench.transfer.NTransferContext;
import com.tridium.workbench.transfer.TransferArtifact;
import com.tridium.workbench.transfer.TransferUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import javax.baja.gx.BFont;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.naming.BOrd;
import javax.baja.nre.util.Array;
import javax.baja.space.Mark;
import javax.baja.sync.Transaction;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.RelationKnob;
import javax.baja.sys.SlotCursor;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.CommandEvent;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.commands.CopyCommand;
import javax.baja.ui.commands.CutCommand;
import javax.baja.ui.commands.DeleteCommand;
import javax.baja.ui.commands.DuplicateCommand;
import javax.baja.ui.commands.PasteCommand;
import javax.baja.ui.commands.RenameCommand;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BMouseWheelEvent;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.util.BWsAnnotation;
import javax.baja.util.BWsTextBlock;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.commands.ComponentCompositeCommand;
import javax.baja.workbench.commands.ComponentReorderCommand;

public class WsController implements WsConst {
   public final BWireSheetPane ws;
   public final WsSelection selection;
   public BComponent container;
   public Point dropPoint;
   private WsState state;
   private HashMap<String, ComponentGlyph> byName = new HashMap<>();
   private HashMap<Object, ComponentGlyph> byHandle = new HashMap<>();
   private int auto = -2;
   private boolean reload;
   int id;
   static int nextId;

   public WsController(BWireSheetPane ws) {
      this.id = nextId++;
      this.ws = ws;
      this.selection = ws.selection;
      this.state = new NormalState(ws, this);
   }

   public WsState getState() {
      return this.state;
   }

   public void transition(WsState newState) {
      this.state = newState;
   }

   public ComponentGlyph[] getAllComponentGlyphs() {
      Glyph[] g = this.ws.getCanvas().getRootGlyph().componentLayer.getChildGlyphs();
      ComponentGlyph[] r = new ComponentGlyph[g.length];
      System.arraycopy(g, 0, r, 0, g.length);
      return r;
   }

   public void load(BComponent container) {
      this.container = container;
      WsSelectedState[] states = this.selection.preLoad();
      this.ws.getCanvas().preLoad();
      this.byName.clear();
      this.byHandle.clear();
      this.auto = -2;
      this.dropPoint = null;
      ArrayList<ComponentGlyph> glyphs = new ArrayList<>();
      SlotCursor<Property> c = container.getProperties();

      while (c.nextComponent()) {
         BComponent kid = c.get().asComponent();
         if (!Flags.isHidden(container, c.property()) && !(kid instanceof BVector)) {
            glyphs.add(this.addComponentGlyph(kid));
         }
      }

      Array<BComponent> toSub = new Array(BComponent.class);

      for (int i = 0; i < glyphs.size(); i++) {
         ComponentGlyph glyph = glyphs.get(i);
         glyph.checkForSubscribe(this.ws.getView(), toSub);
      }

      if (toSub.size() > 0) {
         this.ws.getView().registerForComponentEvents((BComponent[])toSub.trim(), 0);
      }

      for (int i = 0; i < glyphs.size(); i++) {
         ComponentGlyph glyph = glyphs.get(i);
         glyph.loadLinks();
         glyph.loadRelations();
      }

      for (int i = 0; i < glyphs.size(); i++) {
         ComponentGlyph glyph = glyphs.get(i);
         glyph.loadKnobs();
         glyph.loadRelationKnobs();
      }

      for (int i = 0; i < glyphs.size(); i++) {
         ComponentGlyph glyph = glyphs.get(i);
         glyph.postSubscribe();
      }

      this.ws.getCanvas().postLoad();
      this.selection.postLoad(states);
      this.ws.getThumbnail().postLoad();
   }

   public void reload() {
      this.reload = true;
      this.ws.getCanvas().relayout();
   }

   public void doLayout() {
      int numReloads = 0;

      while (this.reload) {
         this.reload = false;
         if (numReloads++ > 10) {
            System.out.println("ERROR: WsController recursive reload!");
            break;
         }

         this.load(this.container);
      }
   }

   public void saveAnnotations(ComponentGlyph[] glyphs) throws Exception {
      HashMap<BComponent, ComponentGlyph> toSave = new HashMap<>();

      for (int i = 0; i < glyphs.length; i++) {
         toSave.put(glyphs[i].component, glyphs[i]);
      }

      ComponentGlyph[] all = this.getAllComponentGlyphs();

      for (int i = 0; i < all.length; i++) {
         if (all[i].component.getProperty("wsAnnotation") == null) {
            toSave.put(all[i].component, all[i]);
         }
      }

      Context tx = Transaction.start(this.container, null);
      Iterator<ComponentGlyph> it = toSave.values().iterator();

      while (it.hasNext()) {
         this.saveAnnotation(it.next(), tx);
      }

      Transaction.end(this.container, tx);
   }

   private void saveAnnotation(ComponentGlyph glyph, Context tx) throws Exception {
      BComponent comp = glyph.component;
      BWsAnnotation anno = glyph.toAnnotation();
      Property prop = comp.getProperty("wsAnnotation");
      if (prop == null) {
         comp.add("wsAnnotation", anno, 1, tx);
      } else {
         BWsAnnotation old = (BWsAnnotation)comp.get(prop);
         if (!old.equals(anno)) {
            comp.set(prop, anno, tx);
         }
      }
   }

   public void zoom(int distance) {
      RectGeom viewport = this.ws.getScrollPane().getViewport();
      double wx = (viewport.width / 2.0 + viewport.x) / this.ws.grid.wixel;
      double wy = (viewport.height / 2.0 + viewport.y) / this.ws.grid.wixel;
      int newWixel = this.ws.grid.wixel + distance;
      if (newWixel < 2) {
         newWixel = 2;
      }

      if (newWixel > 20) {
         newWixel = 20;
      }

      if (this.ws.grid.wixel != newWixel) {
         this.ws.grid.wixel = newWixel;
         this.ws.doLayout(this.ws.getChildWidgets());
         this.ws.getScrollPane().getContent().repaint();
         this.ws.getThumbnail().repaintBuffer();
      }

      RectGeom orig = this.ws.getScrollPane().getViewport();
      if (orig.width > 0.0) {
         double ww = orig.width / this.ws.grid.wixel;
         double wh = orig.height / this.ws.grid.wixel;
         double p = wx - ww / 2.0;
         double q = wy - wh / 2.0;
         viewport = new RectGeom(p * this.ws.grid.wixel, q * this.ws.grid.wixel, orig.width, orig.height);
         this.ws.getScrollPane().scrollToVisible(viewport);
      }
   }

   public void zoomReset() {
      this.zoom(12 - this.ws.grid.wixel);
   }

   public ComponentGlyph addComponentGlyph(BComponent comp) {
      if (comp instanceof BVector) {
         throw new IllegalStateException();
      } else {
         synchronized (this.byName) {
            ComponentGlyph glyph = this.byName.get(comp.getName());
            if (glyph != null) {
               return glyph;
            } else {
               BWsAnnotation anno = null;
               BObject annoRaw = comp.get("wsAnnotation");
               if (annoRaw instanceof BWsAnnotation) {
                  anno = (BWsAnnotation)annoRaw;
               } else {
                  anno = this.nextAutoAnnotation();
               }

               int p = anno.p;
               int q = anno.q;
               int w = anno.wixelWidth;
               int h = anno.wixelHeight;
               if (w < 2) {
                  w = 2;
               }

               if (p + w > BWsOptions.make().getMaxWidth()) {
                  p = BWsOptions.make().getMaxWidth() - w;
               }

               if (q + h > BWsOptions.make().getMaxHeight()) {
                  q = BWsOptions.make().getMaxHeight() - h;
               }

               if (p < 0) {
                  p = 0;
               }

               if (q < 0) {
                  q = 0;
               }

               anno = BWsAnnotation.make(p, q, w, h);
               if (comp instanceof BWsTextBlock) {
                  glyph = new TextBlockGlyph(this.ws, comp, anno);
               } else {
                  glyph = new StdComponentGlyph(this.ws, comp, anno);
               }

               this.byName.put(comp.getName(), glyph);
               this.byHandle.put(glyph.handle, glyph);
               this.ws.getCanvas().getRootGlyph().componentLayer.addToTop(glyph);
               return glyph;
            }
         }
      }
   }

   public LinkGlyph addLinkGlyph(LinkGlyph glyph) {
      this.ws.getCanvas().getRootGlyph().linkLayer.add(glyph);
      return glyph;
   }

   LinkGlyph makeLinkGlyph(StdComponentGlyph target, BLink link) {
      StdComponentGlyph source = this.toStdComponentGlyph(link.getSourceOrd());
      return (LinkGlyph)(source == null ? new LinkStubGlyph(this.ws, link, target) : new LinkSnakeGlyph(this.ws, link, target, source));
   }

   public LinkGlyph addRelationLinkGlyph(StdComponentGlyph source, BRelation relation) {
      BOrd relationOrd = relation.getEndpointOrd();
      StdComponentGlyph target = this.toStdComponentGlyph(relationOrd);
      if (target != null) {
         return null;
      } else {
         LinkGlyph glyph = new RelationStubGlyph(this.ws, relation, source);
         this.ws.getCanvas().getRootGlyph().linkLayer.add(glyph);
         return glyph;
      }
   }

   public LinkGlyph addRelationLinkGlyph(StdComponentGlyph target, RelationKnob knob) {
      BRelation relation = null;
      BOrd relationOrd = knob.getRelationOrd();
      BComponent relationSource = relationOrd.resolve(target.component).get().asComponent();
      relationSource.lease();

      for (BRelation bRelation : (BRelation[])relationSource.getChildren(BRelation.class)) {
         if (!(bRelation instanceof BLink)) {
            BObject relationEndpoint = bRelation.getEndpointOrd().resolve(target.component).get();
            if (relationEndpoint.equals(target.component) && bRelation.getId().toString().equals(knob.getRelationId())) {
               relation = bRelation;
               break;
            }
         }
      }

      StdComponentGlyph source = this.toStdComponentGlyph(relationOrd);
      LinkGlyph glyph;
      if (source != null && relation != null) {
         glyph = new LinkSnakeGlyph(this.ws, relation, target, source);
      } else {
         glyph = new RelationKnobGlyph(this.ws, knob, target);
      }

      this.ws.getCanvas().getRootGlyph().linkLayer.add(glyph);
      return glyph;
   }

   public LinkGlyph addKnobGlyph(ComponentGlyph source, Knob knob) {
      if (!(source instanceof StdComponentGlyph)) {
         return null;
      } else {
         StdComponentGlyph target = this.toStdComponentGlyph(knob.getTargetOrd());
         if (target != null) {
            return null;
         } else {
            LinkGlyph glyph = new LinkKnobGlyph(this.ws, knob, (StdComponentGlyph)source);
            this.ws.getCanvas().getRootGlyph().linkLayer.add(glyph);
            return glyph;
         }
      }
   }

   public LinkGlyph addRelationKnobGlyph(ComponentGlyph target, RelationKnob rKnob) {
      if (!(target instanceof StdComponentGlyph)) {
         return null;
      } else {
         StdComponentGlyph source = this.toStdComponentGlyph(rKnob.getRelationOrd());
         if (source != null) {
            return null;
         } else {
            LinkGlyph glyph = new RelationKnobGlyph(this.ws, rKnob, (StdComponentGlyph)target);
            this.ws.getCanvas().getRootGlyph().linkLayer.add(glyph);
            return glyph;
         }
      }
   }

   public ComponentGlyph toComponentGlyph(BOrd ord) {
      String s = String.valueOf(ord);
      if (s.startsWith("h:")) {
         String handle = s.substring(2).trim();
         return this.byHandle.get(handle);
      } else {
         if (s.startsWith("station:") || s.startsWith("slot:")) {
            try {
               BObject obj = ord.get(this.ws.controller.container);
               if (obj.isComponent()) {
                  return this.byHandle.get(obj.asComponent().getHandle());
               }
            } catch (Exception var4) {
            }
         }

         return null;
      }
   }

   public StdComponentGlyph toStdComponentGlyph(BOrd ord) {
      ComponentGlyph glyph = this.toComponentGlyph(ord);
      return glyph instanceof StdComponentGlyph ? (StdComponentGlyph)glyph : null;
   }

   protected BWsAnnotation nextAutoAnnotation() {
      this.auto += 4;
      if (this.auto > this.ws.grid.getWixelWidth() - 8 || this.auto > this.ws.grid.getWixelHeight() - 8) {
         this.auto = 2;
      }

      return BWsAnnotation.make(this.auto, this.auto, 8);
   }

   public void doBackgroundPopup(double mx, double my) {
      this.dropPoint = new Point(mx, my);
      BMenu menu = new BMenu();
      int p = (int)(mx / this.ws.grid.wixel);
      int q = (int)(my / this.ws.grid.wixel);
      BWsAnnotation anno = BWsAnnotation.make(p, q);
      BWsCanvas canvas = this.ws.getCanvas();
      BMenu newMenu = BComponentMenuAgent.makeNewMenu(this.ws, this.container, anno);
      if (newMenu != null) {
         menu.add("new", new BSubMenuItem(newMenu));
         menu.add("sep0", new BSeparator());
      }

      menu.add("cut", new CutCommand(canvas));
      menu.add("copy", new CopyCommand(canvas));
      menu.add("paste", new PasteCommand(canvas));
      menu.add("pasteSpecial", new PasteSpecialCommand(canvas, this.container));
      menu.add("duplicate", new DuplicateCommand(canvas));
      menu.add("sep1", new BSeparator());
      menu.add("delete", new DeleteCommand(canvas));
      if (BWsOptions.make().getShowRelations() || BWsOptions.make().getShowLinks()) {
         menu.add("deleteRelationss", this.ws.commands.deleteRelations);
         menu.add("editRelation", this.ws.commands.editRelation);
         menu.add("editRelationTags", this.ws.commands.editRelationTags);
      }

      menu.add("rename", new RenameCommand(canvas));
      menu.add("sep2", new BSeparator());
      if (this.addLinkSelection(menu, p, q)) {
         menu.add("sepLinkSep1", new BSeparator());
      }

      menu.add("arrange", this.ws.commands.arrange);
      menu.add("selectAll", this.ws.commands.selectAll);
      menu.add("sep3", new BSeparator());
      menu.add("reorder", new ComponentReorderCommand(this.ws, this.container));
      menu.add("composite", new ComponentCompositeCommand(this.ws, this.container));
      menu = this.ws.getView().makeBackgroundPopup(menu);
      if (menu != null) {
         menu.removeConsecutiveSeparators();
         menu.open(canvas, mx, my);
      }
   }

   public boolean addLinkSelection(BMenu menu, int p, int q) {
      LinkGlyph[] linkGlyphs = this.ws.getCanvas().rootGlyph.linkLayer.getLinkGlyphsAt(p, q);
      if (linkGlyphs.length <= 1) {
         return false;
      } else {
         menu.add("sepLinkSep0", new BSeparator());
         boolean isRelation = linkGlyphs[0].isRelation();
         BMenu linkSelMenu = new BMenu();

         for (int i = 0; i < linkGlyphs.length; i++) {
            linkSelMenu.add("linkSel?", new WsController.LinkSelectCommand(linkGlyphs[i]));
         }

         String selectionText = isRelation ? lex.get("relationSelection") : lex.get("linkSelection");
         menu.add("linkMenu", new BSubMenuItem(selectionText, linkSelMenu));
         return true;
      }
   }

   public void keyPressed(BKeyEvent event) {
      if (this.processInput()) {
         this.state.keyPressed(event);
      }
   }

   public void keyReleased(BKeyEvent event) {
      if (this.processInput()) {
         this.state.keyReleased(event);
      }
   }

   public void keyTyped(BKeyEvent event) {
      if (this.processInput()) {
         this.state.keyTyped(event);
      }
   }

   public void mousePressed(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mousePressed(event);
      }
   }

   public void mouseReleased(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mouseReleased(event);
      }
   }

   public void mouseEntered(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mouseEntered(event);
      }
   }

   public void mouseExited(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mouseExited(event);
      }
   }

   public void mouseMoved(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mouseMoved(event);
      }
   }

   public void mouseDragged(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mouseDragged(event);
      }
   }

   public void mousePulsed(BMouseEvent event) {
      if (this.processInput()) {
         this.state.mousePulsed(event);
      }
   }

   public void mouseWheel(BMouseWheelEvent event) {
      if (this.processInput()) {
         this.state.mouseWheel(event);
      }
   }

   boolean processInput() {
      return this.ws.getShell() != null && this.ws.initialized;
   }

   public boolean pulseViewport(double xCanvas, double yCanvas) {
      return this.ws.getScrollPane().pulseViewport(new Point(xCanvas, yCanvas), this.ws.grid.wixel * 2);
   }

   public void handleComponentEvent(BComponentEvent event) {
      if (this.container != null) {
         int id = event.getId();
         BComponent src = event.getSourceComponent();
         Object h = src.getHandle();
         String slotName = event.getSlotName();
         if (this.container.getHandle().equals(h)) {
            switch (id) {
               case 1:
               case 3:
                  this.reload();
                  break;
               case 2:
                  ComponentGlyph glyph = this.byName.get(slotName);
                  if (glyph != null) {
                     this.ws.grid.set(glyph.p, glyph.q, glyph.ww, glyph.wh, 0);
                     this.ws.grid.clearLinks();
                  }

                  this.reload();
            }
         } else {
            ComponentGlyph glyph = this.byHandle.get(h);
            if (glyph != null) {
               switch (id) {
                  case 0:
                     if (slotName.equals("wsAnnotation")) {
                        this.reload();
                     } else {
                        glyph.changed(slotName);
                     }
                     break;
                  case 1:
                  case 3:
                  case 4:
                  case 8:
                  case 9:
                  case 17:
                  case 18:
                     this.reload();
                     break;
                  case 2:
                  case 6:
                     this.ws.grid.set(glyph.p, glyph.q, glyph.ww, glyph.wh, 0);
                     this.ws.grid.clearLinks();
                     this.reload();
                  case 5:
                  case 7:
                  case 10:
                  case 11:
                  case 12:
                  case 13:
                  case 14:
                  case 15:
                  case 16:
               }
            } else {
               BComponent parent = (BComponent)src.getParent();
               if (parent != null) {
                  glyph = this.byHandle.get(parent.getHandle());
               }

               if (glyph != null) {
                  switch (id) {
                     case 0:
                        glyph.changed(src.getPropertyInParent().getName());
                  }
               }
            }
         }
      }
   }

   public void updateTransferStates() {
      boolean hasComponents = false;
      boolean hasLinks = false;
      Glyph[] selectedGlyphs = this.selection.get();

      for (int i = 0; i < selectedGlyphs.length; i++) {
         Glyph sel = selectedGlyphs[i];
         if (sel instanceof ComponentGlyph) {
            hasComponents = true;
         }

         if (sel instanceof LinkGlyph) {
            hasLinks = true;
         }
      }

      BWsCanvas canvas = this.ws.getCanvas();
      canvas.setCutEnabled(hasComponents);
      canvas.setCopyEnabled(hasComponents);
      canvas.setPasteEnabled(true);
      canvas.setPasteSpecialEnabled(true);
      canvas.setDuplicateEnabled(hasComponents);
      canvas.setDeleteEnabled(hasComponents);
      canvas.setRenameEnabled(hasComponents);
      if (this.ws.commands != null) {
         if (this.ws.commands.deleteRelations != null) {
            this.ws.commands.deleteRelations.setEnabled(hasLinks);
         }

         if (this.ws.commands.editRelation != null) {
            this.ws.commands.editRelation.setEnabled(this.ws.selection.getRelations().length == 1);
         }

         if (this.ws.commands.editRelationTags != null) {
            this.ws.commands.editRelationTags.setEnabled(this.ws.selection.getRelations().length == 1);
         }
      }
   }

   public TransferContext makeTransferContext(Context src, int action, TransferEnvelope envelope) {
      NTransferContext cx = new NTransferContext(src, action, envelope);
      cx.params = this.getTransferParameters(false);
      return cx;
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      CommandArtifact art = null;
      art = TransferUtil.insert(this.ws.getCanvas(), cx, this.container, this.getTransferParameters(true));
      if (art instanceof TransferArtifact) {
         this.completeTransfer((TransferArtifact)art);
      }

      return art;
   }

   public void completeTransfer(TransferArtifact artifact) {
      if (artifact != null) {
         BWbShell shell = this.ws.getWbShell();
         if (shell != null) {
            shell.enterBusy();
         }

         try {
            String[] names = artifact.getInsertNames();
            this.selection.unselectAll();
            ComponentGlyph[] glyphs = new ComponentGlyph[names.length];
            synchronized (this.byName) {
               for (int i = 0; i < names.length; i++) {
                  BObject value = this.container.get(names[i]);
                  if (value != null && value.isComponent() && !(value instanceof BVector)) {
                     glyphs[i] = this.addComponentGlyph(value.asComponent());
                     this.selection.select(glyphs[i]);
                  }
               }
            }

            for (int ix = 0; ix < glyphs.length; ix++) {
               glyphs[ix].loadLinks();
               glyphs[ix].loadRelations();
               glyphs[ix].loadKnobs();
               glyphs[ix].loadRelationKnobs();
            }
         } catch (Exception var14) {
            var14.printStackTrace();
         } finally {
            if (shell != null) {
               shell.exitBusy();
            }
         }
      }
   }

   public BComponent getTransferParameters(boolean clear) {
      BComponent params = new BComponent();
      if (this.dropPoint != null) {
         params.add("origin", BWsAnnotation.make((int)this.dropPoint.x / this.ws.grid.wixel, (int)this.dropPoint.y / this.ws.grid.wixel, 2));
         if (clear) {
            this.dropPoint = null;
         }
      }

      return params;
   }

   public Mark getSelectionAsMark() {
      ComponentGlyph[] glyphs = this.selection.getComponentGlyphs();
      if (glyphs.length == 0) {
         return null;
      } else {
         BComponent[] comp = new BComponent[glyphs.length];

         for (int i = 0; i < comp.length; i++) {
            comp[i] = glyphs[i].component;
         }

         Arrays.sort(comp, new WsController.SlotOrder(this.container));
         return new Mark(comp);
      }
   }

   public static BFont getFont(BFont font, int size) {
      return UiEnv.get().isMicroEdition() ? BFont.make(font.getName(), Math.max(10, size), font.getStyle()) : BFont.make(font.getName(), size, font.getStyle());
   }

   private class LinkSelectCommand extends ToggleCommand {
      private LinkGlyph linkGlyph;

      private LinkSelectCommand(LinkGlyph linkGlyph) {
         super(WsController.this.ws.getCanvas(), linkGlyph.getStatusMessage());
         this.linkGlyph = linkGlyph;
      }

      public CommandArtifact doInvoke(CommandEvent event) throws Exception {
         if (event == null || !event.isControlDown()) {
            WsController.this.ws.selection.unselectAll();
            WsController.this.ws.selection.select(this.linkGlyph);
         } else if (this.linkGlyph.isSelected()) {
            WsController.this.ws.selection.unselect(this.linkGlyph);
         } else {
            WsController.this.ws.selection.select(this.linkGlyph);
         }

         return null;
      }

      public synchronized boolean isSelected() {
         return this.linkGlyph.isSelected();
      }
   }

   static class SlotOrder implements Comparator<BComponent> {
      HashMap<String, Integer> lookup = new HashMap<>();

      public SlotOrder(BComponent parent) {
         Property[] props = parent.getPropertiesArray();

         for (int i = 0; i < props.length; i++) {
            this.lookup.put(props[i].getName(), i);
         }
      }

      public int compare(BComponent obj1, BComponent obj2) {
         String na = obj1.getName();
         String nb = obj2.getName();
         int a = this.lookup.get(na);
         int b = this.lookup.get(nb);
         if (a < b) {
            return -1;
         } else {
            return a == b ? 0 : 1;
         }
      }
   }
}
