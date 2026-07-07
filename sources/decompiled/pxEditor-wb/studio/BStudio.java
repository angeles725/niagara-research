package com.tridium.px.editor.studio;

import com.tridium.gx.util.GeomUtil;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.commands.Delete;
import com.tridium.px.editor.commands.EditPropertiesContext;
import com.tridium.px.editor.commands.Insert;
import com.tridium.px.editor.commands.InsertDynamic;
import com.tridium.px.editor.commands.InsertFrozen;
import com.tridium.px.editor.commands.Rename;
import com.tridium.px.editor.studio.artisans.Artisan;
import com.tridium.px.editor.studio.commands.MoveWidget;
import com.tridium.px.editor.studio.commands.PreferredSize;
import com.tridium.px.editor.studio.commands.Select;
import com.tridium.px.editor.studio.painters.DefaultPainter;
import com.tridium.px.editor.studio.painters.Painter;
import com.tridium.px.editor.studio.trackers.Tracker;
import com.tridium.px.editor.studio.trackers.UnpressedTracker;
import com.tridium.px.editor.util.MenuBuilder;
import com.tridium.px.editor.util.Reflector;
import com.tridium.px.editor.util.SelectedWidgets;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.gx.BSize;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BIPxTransferWidget;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.factory.WidgetInserter;
import javax.baja.space.Mark;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BSubMenuItem;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.commands.CompoundCommand;
import javax.baja.ui.commands.CopyCommand;
import javax.baja.ui.commands.CutCommand;
import javax.baja.ui.commands.DeleteCommand;
import javax.baja.ui.commands.DuplicateCommand;
import javax.baja.ui.commands.PasteCommand;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BCanvasPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BLabelPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.pane.BTabbedPane;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.ui.util.UiLexicon;

@NiagaraType
public class BStudio extends BTransferWidget implements BIPxTransferWidget, TreeStudio, CommandStudio, TrackerStudio, RootStudio, PainterStudio {
   public static final Type TYPE = Sys.loadType(BStudio.class);
   private static final int DRAG_LINE = 50;
   private static final BBrush DRAG_BRUSH = BColor.lime.toBrush();
   private BPxEditor editor;
   private BPxEditorPane editorPane;
   private Artisan artisan;
   private SelectedWidgets selected;
   private boolean isShiftDown;
   private Painter painter;
   private Tracker tracker;
   private boolean duplicating;
   private BWidget duplParent;
   private RectGeom[] duplBounds;
   private double popupX;
   private double popupY;
   private Point dragPoint;
   private RectGeom dragRect;
   private BImage bufferImage = null;

   public Type getType() {
      return TYPE;
   }

   public BStudio(BPxEditor editor, BPxEditorPane editorPane) {
      this.editor = editor;
      this.editorPane = editorPane;
      this.artisan = Artisan.instance();
      this.selected = editorPane.getSelectedWidgets();
      this.tracker = new UnpressedTracker(editorPane, this);
      this.painter = new DefaultPainter(this);
   }

   public void computePreferredSize() {
      this.editorPane.getRootContainer().computePreferredSize();
      this.setPreferredSize(this.editorPane.getRootContainer().getPreferredWidth(), this.editorPane.getRootContainer().getPreferredHeight());
   }

   public void doLayout(BWidget[] kids) {
      this.editorPane.getRootContainer().setBounds(0.0, 0.0, this.getWidth(), this.getHeight());
      this.editorPane.forceRootLayout();
   }

   public void paint(Graphics g) {
      this.editorPane.getRootShellMgr().layoutWidgets();
      this.painter.doPaint(g);
   }

   public void mouseMoved(BMouseEvent event) {
      double x = event.getX();
      double y = event.getY();
      BWidget widget = this.rootDescendant(new Point(x, y));
      BCanvasPane canvas = Reflector.canvas(widget);
      if (canvas == null) {
         this.editorPane.updateStatus("");
      } else {
         Point pnt = this.toViewbox(x, y, canvas);
         BSize s = canvas.getViewSize();
         if (new RectGeom(0.0, 0.0, s.width, s.height).contains(pnt.x, pnt.y)) {
            this.editorPane.updateStatus((int)pnt.x + ", " + (int)pnt.y);
         } else {
            this.editorPane.updateStatus("");
         }
      }

      this.tracker = this.tracker.mouseMoved(event);
   }

   public void mousePressed(BMouseEvent event) {
      this.requestFocus();
      this.tracker = this.tracker.mousePressed(event);
   }

   public void mouseDragged(BMouseEvent event) {
      this.tracker = this.tracker.mouseDragged(event);
   }

   public void mouseReleased(BMouseEvent event) {
      this.tracker = this.tracker.mouseReleased(event);
   }

   public void mouseEntered(BMouseEvent event) {
      this.tracker = this.tracker.mouseEntered(event);
   }

   public void mouseExited(BMouseEvent event) {
      this.tracker = this.tracker.mouseExited(event);
   }

   public void focusGained(BFocusEvent event) {
      this.repaint();
      this.editor.setTransferWidget(this);
   }

   public void keyPressed(BKeyEvent event) {
      this.tracker = this.tracker.keyPressed(event);
   }

   public void keyReleased(BKeyEvent event) {
      this.tracker = this.tracker.keyReleased(event);
   }

   public void keyTyped(BKeyEvent event) {
      this.tracker = this.tracker.keyTyped(event);
   }

   public CommandArtifact doDuplicate() throws Exception {
      this.duplicating = true;
      return super.doDuplicate();
   }

   public TransferEnvelope getTransferData() throws Exception {
      if (this.duplicating) {
         this.duplParent = this.selected.get(0).getParentWidget();
         BWidget[] w = this.selected.getWidgets();
         this.duplBounds = new RectGeom[w.length];

         for (int i = 0; i < w.length; i++) {
            this.duplBounds[i] = this.artisan.bounds(w[i]);
         }
      }

      return this.selected.envelope();
   }

   public CommandArtifact removeTransferData(TransferContext cx) throws Exception {
      return this.doDelete();
   }

   public CommandArtifact doDelete() throws Exception {
      return this.selected.size() > 0 && this.editorPane.getLayerManager().allNormal(this.selected.getWidgets())
         ? new Delete(this.editorPane).doInvoke()
         : null;
   }

   public int dragOver(TransferContext cx) {
      if (!this.preventEdits() && this.editor.getController().allowDrop(this, cx)) {
         BWidget parent = this.findDroppable(this.rootDescendant(new Point(cx.getX(), cx.getY())));
         if (!this.preventEdits() && parent != null && cx.getEnvelope().supports(TransferFormat.mark)) {
            this.dragPoint = new Point(cx.getX(), cx.getY());
            if (parent instanceof BCanvasPane) {
               BCanvasPane canvas = (BCanvasPane)parent;
               Point pta = this.toViewbox(cx.getX(), cx.getY(), canvas);
               Point ptb = this.snap(pta.x, pta.y);
               this.dragPoint = this.fromViewbox(ptb.x, ptb.y, canvas);
            }

            this.checkDrag();
            return 16;
         } else {
            this.dragPoint = null;
            this.checkDrag();
            return 0;
         }
      } else {
         return 0;
      }
   }

   public void dragExit(TransferContext cx) {
      this.dragPoint = null;
      this.checkDrag();
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
      BWidget parent = this.duplicating ? this.duplParent : this.selected.get(0);
      if (!Reflector.isDroppable(parent)) {
         throw new IllegalStateException();
      } else {
         if (!Reflector.isFreeFormPane(parent)) {
            objects = new BObject[]{objects[0]};
         }

         WidgetInserter ins = this.editor.getController().getWidgetInserter(this, objects);
         if (ins == null) {
            return null;
         } else if (ins.getWidgets() == null) {
            return null;
         } else if (ins.getWidgets().length == 0) {
            return null;
         } else {
            Insert insert = this.makeInsertCommand(parent, ins.getWidgets());
            this.popupX = 0.0;
            this.popupY = 0.0;
            this.duplicating = false;
            return this.makeInsertArtifact(insert, ins.getAuxillaryCommand());
         }
      }
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      if (!this.preventEdits() && this.editor.getController().allowDrop(this, cx)) {
         BObject[] objects = ((Mark)cx.getEnvelope().getData(TransferFormat.mark)).getValues();
         this.dragPoint = null;
         this.checkDrag();
         BWidget parent = this.findDroppable(this.rootDescendant(new Point(cx.getX(), cx.getY())));
         if (parent == null) {
            throw new IllegalStateException();
         } else {
            if (!Reflector.isFreeFormPane(parent)) {
               objects = new BObject[]{objects[0]};
            }

            WidgetInserter ins = this.editor.getController().getWidgetInserter(this, objects);
            if (ins == null) {
               return null;
            } else if (ins.getWidgets() == null) {
               return null;
            } else if (ins.getWidgets().length == 0) {
               return null;
            } else {
               Insert insert = this.makeDropCommand(cx, parent, ins.getWidgets(), ins.getColumnCount());
               return this.makeInsertArtifact(insert, ins.getAuxillaryCommand());
            }
         }
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   public BMenu getDefaultPopupMenu(BMouseEvent event) {
      BMenu menu = new BMenu();
      this.populatePopupMenu(menu);
      return menu;
   }

   public void populatePopupMenu(BMenu menu) {
      BCanvasPane canvas = this.getCurrentCanvas();
      BPane freeForm = this.getCurrentFreeForm();
      BSubMenuItem newSubMenu = new BSubMenuItem(UiLexicon.bajaui().getText("menu.new.label"));
      CutCommand cut = new CutCommand(this);
      CopyCommand copy = new CopyCommand(this);
      PasteCommand paste = new PasteCommand(this);
      DuplicateCommand duplicate = new DuplicateCommand(this);
      DeleteCommand del = new DeleteCommand(this);
      EditPropertiesContext edit = new EditPropertiesContext(this.editor);
      PreferredSize prefSize = new PreferredSize(this.editorPane, freeForm);
      if (this.selected.size() == 0) {
         cut.setEnabled(false);
         copy.setEnabled(false);
         paste.setEnabled(false);
         newSubMenu.setEnabled(false);
         duplicate.setEnabled(false);
         del.setEnabled(false);
         edit.setEnabled(false);
         prefSize.setEnabled(false);
      } else {
         if (this.selected.size() != 1 || !Reflector.isDroppable(this.selected.get(0))) {
            paste.setEnabled(false);
            newSubMenu.setEnabled(false);
         }

         if (!Reflector.isFreeFormPane(this.selected.get(0).getParentWidget())) {
            duplicate.setEnabled(false);
         }
      }

      boolean readOnly = this.preventEdits();
      if (cut.isEnabled()) {
         cut.setEnabled(!readOnly);
      }

      if (paste.isEnabled()) {
         paste.setEnabled(!readOnly);
      }

      if (newSubMenu.isEnabled()) {
         newSubMenu.setEnabled(!readOnly);
      }

      if (duplicate.isEnabled()) {
         duplicate.setEnabled(!readOnly);
      }

      if (del.isEnabled()) {
         del.setEnabled(!readOnly);
      }

      if (edit.isEnabled()) {
         edit.setEnabled(!readOnly);
      }

      if (prefSize.isEnabled()) {
         prefSize.setEnabled(!readOnly);
      }

      if (newSubMenu.isEnabled()) {
         BMenu newMenu = MenuBuilder.newMenu(this.editorPane, this);
         if (newMenu == null) {
            newSubMenu.setEnabled(false);
         } else {
            newSubMenu.setMenu(newMenu);
         }
      }

      BSubMenuItem align = new BSubMenuItem(BPxEditorPane.text("commands.align"), MenuBuilder.alignMenu(this.editorPane, this.alignable(canvas)));
      BSubMenuItem distribute = new BSubMenuItem(
         BPxEditorPane.text("commands.distribute"), MenuBuilder.distributeMenu(this.editorPane, this.distributable(canvas))
      );
      BSubMenuItem reorg = new BSubMenuItem(BPxEditorPane.text("commands.reorder"), MenuBuilder.reorgMenu(this.editorPane, this.reorgable(freeForm)));
      BSubMenuItem select = new BSubMenuItem(BPxEditorPane.text("commands.select"), this.selectMenu(freeForm));
      BSubMenuItem border = new BSubMenuItem(BPxEditorPane.text("commands.border"), MenuBuilder.borderMenu(this.editorPane, this.selected));
      menu.add(null, newSubMenu);
      menu.add(null, new BSeparator());
      menu.add(null, cut);
      menu.add(null, copy);
      menu.add(null, paste);
      menu.add(null, duplicate);
      menu.add(null, del);
      menu.add(null, new BSeparator());
      menu.add(null, edit);
      menu.add(null, align);
      menu.add(null, distribute);
      menu.add(null, reorg);
      menu.add(null, select);
      menu.add(null, border);
      menu.add(null, prefSize);
      if (this.editorPane.getOptions().getPreserveIdentities()) {
         Rename rename = new Rename(this.editorPane);
         if (this.selected.size() != 1 || !Reflector.isFreeFormPane(this.selected.get(0).getParentWidget())) {
            rename.setEnabled(false);
         }

         if (rename.isEnabled()) {
            rename.setEnabled(!this.preventEdits());
         }

         menu.add(null, rename);
      }
   }

   private CommandArtifact makeInsertArtifact(Insert insert, Command auxCommand) throws Exception {
      if (insert == null) {
         return null;
      } else if (!insert.checkMedia()) {
         return null;
      } else {
         return auxCommand == null ? insert.doInvoke() : new CompoundCommand(this.editor, "compound", new Command[]{insert, auxCommand}).doInvoke();
      }
   }

   private Insert makeInsertCommand(BWidget parent, BWidget[] widgets) {
      if (!Reflector.isFreeFormPane(parent)) {
         if (parent instanceof BBorderPane) {
            MoveWidget.toZero(widgets, this.artisan);
            return new InsertFrozen(this.editor, this.editorPane, parent, widgets[0], "content");
         } else if (parent instanceof BEdgePane) {
            MoveWidget.toZero(widgets, this.artisan);
            return new InsertFrozen(this.editor, this.editorPane, parent, widgets[0], "center");
         } else {
            throw new IllegalStateException();
         }
      } else {
         if (parent instanceof BCanvasPane) {
            BCanvasPane canvas = (BCanvasPane)parent;
            if (this.duplicating) {
               MoveWidget.toZero(widgets, this.artisan);

               for (int i = 0; i < widgets.length; i++) {
                  this.artisan.move(widgets[i], this.duplBounds[i].x + 10.0, this.duplBounds[i].y + 10.0);
               }
            } else {
               RectGeom bnd = this.artisan.bounds(widgets[0]);

               for (int i = 1; i < widgets.length; i++) {
                  bnd = RectGeom.bounds(this.artisan.bounds(widgets[i]), bnd, bnd);
               }

               Point pta = this.toViewbox(this.popupX, this.popupY, canvas);
               Point ptb = this.snap(pta.x, pta.y);
               MoveWidget.shiftWidgets(widgets, this.artisan, ptb.x - bnd.x, ptb.y - bnd.y);
            }
         } else if (parent instanceof BTabbedPane) {
            MoveWidget.toZero(widgets, this.artisan);
            widgets = this.makeTabs(widgets);
         }

         return new InsertDynamic(this.editor, this.editorPane, parent, widgets);
      }
   }

   private Insert makeDropCommand(TransferContext cx, BWidget parent, BWidget[] widgets, int columnCount) {
      if (Reflector.isFreeFormPane(parent)) {
         if (parent instanceof BCanvasPane) {
            BCanvasPane canvas = (BCanvasPane)parent;
            Point pta = this.toViewbox(cx.getX(), cx.getY(), canvas);
            Point ptb = this.snap(pta.x, pta.y);
            this.dropWidgetsInCanvas(ptb.x, ptb.y, widgets, columnCount);
         } else if (parent instanceof BTabbedPane) {
            widgets = this.makeTabs(widgets);
         }

         return new InsertDynamic(this.editor, this.editorPane, parent, widgets);
      } else if (parent instanceof BBorderPane) {
         return new InsertFrozen(this.editor, this.editorPane, parent, widgets[0], "content");
      } else if (parent instanceof BEdgePane) {
         return new InsertFrozen(this.editor, this.editorPane, parent, widgets[0], "center");
      } else {
         throw new IllegalStateException();
      }
   }

   private BMenu selectMenu(BPane freeForm) {
      BMenu menu = new BMenu();
      BWidget[] stack = this.rootDescendants(new Point(this.popupX, this.popupY));
      int idx = this.isSelected(this.selected, stack);
      menu.add(null, new Select(this.editorPane, stack, idx, true, false));
      menu.add(null, new Select(this.editorPane, stack, idx, true, true));
      menu.add(null, new Select(this.editorPane, stack, idx, false, true));
      menu.add(null, new Select(this.editorPane, stack, idx, false, false));
      menu.setEnabled(this.selectable(freeForm, stack, idx));
      return menu;
   }

   private int isSelected(SelectedWidgets selected, BWidget[] stack) {
      for (int i = 0; i < stack.length; i++) {
         if (selected.isSelected(stack[i])) {
            return i;
         }
      }

      return -1;
   }

   private boolean selectable(BPane freeForm, BWidget[] stack, int idx) {
      return !this.preventEdits() && freeForm != null && freeForm instanceof BCanvasPane && stack != null && stack.length > 1 && idx != -1;
   }

   @Override
   public void dropWidgetsInCanvas(double anchorX, double anchorY, BWidget[] widgets, int columnCount) {
      if (columnCount == 1) {
         double dy = 0.0;

         for (int i = 0; i < widgets.length; i++) {
            this.artisan.move(widgets[i], anchorX, anchorY + dy);
            dy += widgets[i].getLayout().getHeight();
         }
      } else {
         double dx = 0.0;
         double dy = 0.0;
         double curY = 0.0;

         for (int i = 0; i < widgets.length; i++) {
            this.artisan.move(widgets[i], anchorX + dx, anchorY + dy);
            curY = (int)Math.max(widgets[i].getLayout().getHeight(), curY);
            if (i % columnCount == columnCount - 1) {
               dx = 0.0;
               dy += curY;
               curY = 0.0;
            } else {
               dx += widgets[i].getLayout().getWidth();
            }
         }
      }
   }

   @Override
   public BLabelPane[] makeTabs(BWidget[] widgets) {
      BLabelPane[] tabs = new BLabelPane[widgets.length];

      for (int i = 0; i < widgets.length; i++) {
         BWidget w = widgets[i];
         if (w instanceof BLabelPane) {
            BLabelPane lp = (BLabelPane)w;
            w = this.editor.cloneWidget(lp.getContent());
         }

         String name = Reflector.displayName(w, false);
         tabs[i] = new BLabelPane(name, w);
      }

      return tabs;
   }

   @Override
   public boolean alignable(BCanvasPane canvas) {
      return !this.preventEdits() && canvas != null && this.selected.size() > 1;
   }

   @Override
   public boolean distributable(BCanvasPane canvas) {
      return !this.preventEdits() && canvas != null && this.selected.size() > 2;
   }

   @Override
   public boolean reorgable(BPane freeForm) {
      return !this.preventEdits() && freeForm != null && this.selected.size() > 0;
   }

   @Override
   public BPane getCurrentFreeForm() {
      int n = this.selected.size();
      if (n == 0) {
         return null;
      } else {
         BWidget parent = this.selected.get(0).getParentWidget();
         return Reflector.isFreeFormPane(parent) ? (BPane)parent : null;
      }
   }

   @Override
   public Point translateToRoot(BWidget widget, Point pt) {
      return widget.translateToAncestor(this.editorPane.getRootContainer(), pt);
   }

   @Override
   public Point translateFromRoot(BWidget widget, Point pt) {
      return widget.translateFromAncestor(this.editorPane.getRootContainer(), pt);
   }

   @Override
   public BWidget rootDescendant(Point pnt) {
      return this.descendantAt(this.editorPane.getRootContainer(), pnt);
   }

   @Override
   public BWidget[] rootDescendants(Point pnt) {
      Point temp = new Point(pnt);
      BWidget widget = this.descendantAt(this.editorPane.getRootContainer(), pnt);
      if (widget == null) {
         return null;
      } else {
         BWidget parent = widget.getParentWidget();
         if (!(parent instanceof BCanvasPane)) {
            return new BWidget[]{widget};
         } else {
            BCanvasPane canvas = (BCanvasPane)parent;
            BWidget[] kids = canvas.childrenAt(this.translateFromRoot(canvas, temp));
            return this.editorPane.getLayerManager().trimToNormal(kids);
         }
      }
   }

   private BWidget descendantAt(BWidget widget, Point pnt) {
      if (!this.editorPane.getLayerManager().isNormal(widget)) {
         return null;
      } else if (Reflector.isLeaf(widget)) {
         return widget;
      } else {
         BWidget child = widget.childAt(pnt);
         if (child != null) {
            BWidget d = this.descendantAt(child, widget.translateToChild(child, pnt));
            if (d == null) {
               return this.editorPane.getLayerManager().isNormal(child) ? child : widget;
            } else {
               return d;
            }
         } else {
            return null;
         }
      }
   }

   @Override
   public Point toViewbox(double x, double y, BCanvasPane canvas) {
      Point p = this.translateFromRoot(canvas, new Point(x, y));
      return canvas.getScaleTransform().getInverse().transform(p, null);
   }

   @Override
   public Point fromViewbox(double x, double y, BCanvasPane canvas) {
      Point z = new Point(x, y);
      Point p = canvas.getScaleTransform().transform(z, null);
      return this.translateToRoot(canvas, p);
   }

   @Override
   public void selectWidgets(RectGeom rubberBand, BCanvasPane parent) {
      BWidget[] children = parent.getChildWidgets();

      for (int i = 0; i < children.length; i++) {
         if (GeomUtil.contains(rubberBand, this.artisan.bounds(children[i])) && this.editorPane.getLayerManager().isNormal(children[i])) {
            this.selected.select(children[i]);
         }
      }
   }

   @Override
   public BCanvasPane getCurrentCanvas() {
      int n = this.selected.size();
      if (n == 0) {
         return null;
      } else {
         BWidget parent = this.selected.get(0).getParentWidget();
         return parent instanceof BCanvasPane ? (BCanvasPane)parent : null;
      }
   }

   @Override
   public Point snap(double x, double y) {
      if (this.editorPane.getOptions().getUseSnap() && !this.isShiftDown()) {
         int snap = this.editorPane.getOptions().getSnapSize();
         if (snap < 1) {
            snap = 1;
         }

         int ix = (int)x;
         int iy = (int)y;
         ix = (ix + snap / 2) / snap * snap;
         iy = (iy + snap / 2) / snap * snap;
         return new Point(ix, iy);
      } else {
         return new Point(x, y);
      }
   }

   @Override
   public void showPopupMenu(BMouseEvent event) {
      this.popupX = event.getX();
      this.popupY = event.getY();
      BMenu menu = this.editor.getController().getPopupMenu(this, event);
      if (menu != null) {
         menu.open(this, event.getX(), event.getY());
      }
   }

   @Override
   public void buffer() {
      try {
         if (this.bufferImage != null) {
            this.bufferImage.dispose();
         }
      } catch (Throwable var2) {
      }

      this.bufferImage = BImage.make(this.getWidth(), this.getHeight());
      Graphics g = this.bufferImage.getGraphics();
      g.clip(0.0, 0.0, this.getWidth(), this.getHeight());
      this.paintPage(g);
      this.paintSelected(g);
   }

   @Override
   public void unbuffer() {
      try {
         if (this.bufferImage != null) {
            this.bufferImage.dispose();
         }
      } catch (Throwable var2) {
      }

      this.bufferImage = null;
   }

   @Override
   public void paintBuffer(Graphics g) {
      g.drawImage(this.bufferImage, 0.0, 0.0);
   }

   @Override
   public void paintPage(Graphics g) {
      this.editorPane.getRootContainer().paint(g);
   }

   @Override
   public void paintSelected(Graphics g) {
      for (int i = 0; i < this.selected.size(); i++) {
         Artisan.instance().paintSelected(this, this.selected.get(i), g);
      }
   }

   @Override
   public void paintDrag(Graphics g) {
      Point pnt = this.dragPoint;
      if (pnt != null) {
         g.setBrush(DRAG_BRUSH);
         g.strokeLine(pnt.x, pnt.y, pnt.x + 50.0, pnt.y);
         g.strokeLine(pnt.x, pnt.y, pnt.x, pnt.y + 50.0);
      }
   }

   private void checkDrag() {
      RectGeom dirty = this.dragRect;
      if (this.dragPoint == null) {
         dirty = this.dragRect;
         this.dragRect = null;
      } else {
         RectGeom newDirty = new RectGeom(this.dragPoint.x, this.dragPoint.y, 52.0, 52.0);
         RectGeom oldDirty = this.dragRect == null ? newDirty : this.dragRect;
         dirty = RectGeom.bounds(newDirty, oldDirty, new RectGeom());
         this.dragRect = newDirty;
      }

      if (dirty != null) {
         this.repaint(dirty.x, dirty.y, dirty.width, dirty.height);
      }
   }

   private BWidget findDroppable(BWidget w) {
      while (w != null) {
         if (Reflector.isDroppable(w)) {
            return w;
         }

         w = w.getParentWidget();
      }

      return null;
   }

   private boolean preventEdits() {
      return this.editor.isReadonly() || !this.editorPane.getLayerManager().allNormal(this.selected.getWidgets());
   }

   @Override
   public Painter getPainter() {
      return this.painter;
   }

   public Tracker getTracker() {
      return this.tracker;
   }

   @Override
   public void setPainter(Painter painter) {
      this.painter = painter;
   }

   public void setTracker(Tracker tracker) {
      this.tracker = tracker;
   }

   public boolean isShiftDown() {
      return this.isShiftDown;
   }

   @Override
   public void setShiftDown(boolean val) {
      this.isShiftDown = val;
   }
}
