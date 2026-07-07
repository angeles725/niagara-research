package com.tridium.px.editor.sidebars.binding;

import com.tridium.file.types.bog.BBogSpace;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.commands.GotoOrd;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.workbench.fieldeditors.BOrdFE;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.baja.file.types.text.BPxFile;
import javax.baja.gx.BImage;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.px.editor.BDrawingTool;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.BPxSideBar;
import javax.baja.px.editor.event.PxComponentEvent;
import javax.baja.px.editor.event.PxEditorEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.px.editor.event.PxSelectionEvent;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.ListController;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.px.BPxInclude;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;

@NiagaraType
@NiagaraAction(
   name = "selectionModified",
   parameterType = "BWidgetEvent",
   defaultValue = "new BWidgetEvent()"
)
public class BBoundOrds extends BPxSideBar implements PxListener {
   public static final Action selectionModified = newAction(0, new BWidgetEvent(), null);
   public static final Type TYPE = Sys.loadType(BBoundOrds.class);
   private static final Version MIN_NEQLIZE_VER = new Version("4.9");
   private static final Logger LOGGER = Logger.getLogger("pxEditor");
   private static final Lexicon LEX = Lexicon.make("pxEditor");
   private static final String DESC = LEX.getText("boundOrds.label");
   private static final BImage ICON = BImage.make(LEX.getText("boundOrds.icon"));
   private static final BOrd[] EMPTY_ORD_ARRAY = new BOrd[0];
   private static final ChangeOrds.Entry[] EMPTY_CHANGE_ORDS_ENTRY_ARRAY = new ChangeOrds.Entry[0];
   private final BPxEditor editor;
   private final BPxEditorPane editorPane;
   private final BList list = new BList();
   private Map<BOrd, List<BComponent>> ords;
   private final BBoundOrds.Relativize relativize;
   private final BBoundOrds.Replace replace;
   private final BBoundOrds.Neqlize neqlize;

   public void selectionModified(BWidgetEvent parameter) {
      this.invoke(selectionModified, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBoundOrds(BPxEditor editor) {
      super(editor);
      this.editor = editor;
      this.editorPane = (BPxEditorPane)editor.getContent();
      editor.addPxListener(this);
      this.list.setMultipleSelection(true);
      this.list.setController(new BBoundOrds.Controller());
      BToolBar toolbar = new BToolBar();
      toolbar.add(null, this.neqlize = new BBoundOrds.Neqlize());
      toolbar.add(null, this.relativize = new BBoundOrds.Relativize());
      toolbar.add(null, this.replace = new BBoundOrds.Replace());
      BEdgePane edge = new BEdgePane();
      edge.setRight(toolbar);
      BEdgePane contents = new BEdgePane();
      contents.setTop(edge);
      contents.setCenter(this.list);
      this.setContent(contents);
      this.linkTo("lnkSelModified", this.list, BList.selectionModified, selectionModified);
   }

   public void doSelectionModified(BWidgetEvent event) {
      List<BWidget> arr = new ArrayList<>();
      int[] items = this.list.getSelection().getItems();

      for (int i = 0; i < items.length; i++) {
         BOrd ord = (BOrd)this.list.getItem(items[i]);
         List<BComponent> comps = this.ords.get(ord);

         for (int j = 0; j < comps.size(); j++) {
            BComponent c = comps.get(j);

            while (!(c instanceof BWidget)) {
               c = (BComponent)c.getParent();
            }

            arr.add((BWidget)c);
         }
      }

      BWidget[] widgets = arr.toArray(new BWidget[0]);
      this.editor.getSelection().setWidgets(widgets);
      this.editor.firePxEvent(new PxSelectionEvent(widgets));
   }

   @Override
   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 0:
            PxEditorEvent ee = (PxEditorEvent)event;
            switch (ee.getEventId()) {
               case 0:
                  this.updateOrds();
                  this.checkReadonly();
                  return;
               case 3:
                  BDrawingTool tool = (BDrawingTool)ee.getEventValue();
                  this.setEnabled(tool.isNormal());
                  return;
               default:
                  return;
            }
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.updateOrds();
                  return;
               default:
                  return;
            }
         case 2:
            this.checkReadonly();
            break;
         case 3:
         case 4:
            switch (EventUtil.getEventType((PxComponentEvent)event)) {
               case 1:
               case 2:
               case 3:
               case 5:
               case 6:
               case 7:
               case 8:
               case 9:
                  this.updateOrds();
                  return;
               case 4:
               default:
                  return;
            }
         case 5:
         case 6:
            this.updateOrds();
         case 7:
         default:
            break;
         case 8:
            switch (((PxLayerEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.checkReadonly();
            }
      }
   }

   private void updateOrds() {
      this.remove("lnkSelModified");
      this.list.getSelection().deselectAll();
      this.list.removeAllItems();
      this.ords = this.ords();

      for (BOrd ord : this.ords.keySet()) {
         this.list.addItem(ord);
      }

      this.relayout();
      this.linkTo("lnkSelModified", this.list, BList.selectionModified, selectionModified);
   }

   private void checkReadonly() {
      this.relativize.setEnabled(true);
      this.replace.setEnabled(true);
      if (!this.editor.isReadonly() && this.editorPane.getLayerManager().allNormal(this.editor.getSelection().getWidgets())) {
         this.relativize.setEnabled(!this.editor.isFileBased() || this.editorPane.getPxRootComponent() != null);
      } else {
         this.relativize.setEnabled(false);
         this.replace.setEnabled(false);
      }

      BComponent baseComponent = this.getBaseComponent();
      if (!this.editor.isFileBased() && !this.editor.isReadonly() && baseComponent != null && !(baseComponent.getComponentSpace() instanceof BBogSpace)) {
         BISession currentSession = baseComponent.getSession();
         Version remoteVersion = (Version)((BObject)currentSession).fw(404, "tagdictionary", null, null, null);
         this.neqlize.setEnabled(remoteVersion != null && remoteVersion.compareTo(MIN_NEQLIZE_VER) >= 0);
      } else {
         this.neqlize.setEnabled(false);
      }
   }

   private BComponent getBaseComponent() {
      BComponent baseComponent = null;
      BObject baseObject = this.editor.getCurrentValue();
      if (baseObject instanceof BComponent) {
         baseComponent = (BComponent)baseObject;
      }

      return baseComponent;
   }

   public CommandArtifact relativizeORds() {
      try {
         return new BBoundOrds.Relativize(false).doInvoke();
      } catch (Exception var2) {
         return null;
      }
   }

   private Map<BOrd, List<BComponent>> ords() {
      Map<BOrd, List<BComponent>> map = new TreeMap<>();
      this.findOrds(this.editor.getWidget(), map);
      return map;
   }

   private void findOrds(BComponent component, Map<BOrd, List<BComponent>> map) {
      if (component instanceof BBinding || component instanceof BPxInclude) {
         SlotCursor<Property> c = component.getProperties();

         while (c.next(BOrd.class)) {
            BOrd ord = (BOrd)c.get();
            if (!ord.equals(BOrd.NULL)) {
               List<BComponent> comps = map.get(ord);
               if (comps == null) {
                  comps = new ArrayList<>();
                  map.put(ord, comps);
               }

               comps.add(component);
            }
         }
      }

      if (!(component instanceof BPxInclude)) {
         BComponent[] kids = component.getChildComponents();

         for (int i = 0; i < kids.length; i++) {
            this.findOrds(kids[i], map);
         }
      }
   }

   @Override
   public BImage getSideBarIcon() {
      return ICON;
   }

   @Override
   public String getSideBarDescription() {
      return DESC;
   }

   class Controller extends ListController {
      protected void itemPressed(BMouseEvent event, int index) {
         super.itemPressed(event, index);
         BOrd ord = (BOrd)BBoundOrds.this.list.getItem(index);
         if (event.isButton1Down()) {
            if (event.getClickCount() == 2 && !BBoundOrds.this.editor.isReadonly()) {
               BOrdFE fe = new BOrdFE();
               fe.loadValue(ord);
               int r = BDialog.open(BBoundOrds.this.editor, BBoundOrds.LEX.getText("boundOrds.edit"), fe, 3);
               if (r == 1) {
                  try {
                     BOrd newOrd = (BOrd)fe.saveValue();
                     List<ChangeOrds.Entry> entries = new ArrayList<>();
                     this.swapOrd(ord, newOrd, BBoundOrds.this.editor.getWidget(), entries);
                     ChangeOrds.Entry[] e = entries.toArray(new ChangeOrds.Entry[0]);
                     if (e.length > 0) {
                        new ChangeOrds(BBoundOrds.this.editor, e).invoke();
                     }
                  } catch (Exception var9) {
                     throw new BajaRuntimeException(var9);
                  }
               }
            }
         } else if (event.isButton3Down()) {
            BMenu menu = new BMenu();
            menu.add(null, new GotoOrd(BBoundOrds.this, ord));
            menu.open(BBoundOrds.this, event.getX(), event.getY());
         }
      }

      private void swapOrd(BOrd from, BOrd to, BComponent component, List<ChangeOrds.Entry> entries) {
         if (component instanceof BBinding || component instanceof BPxInclude) {
            SlotCursor<Property> c = component.getProperties();

            while (c.next(BOrd.class)) {
               BOrd ord = (BOrd)c.get();
               if (ord.equals(from)) {
                  entries.add(new ChangeOrds.Entry(component, c.property(), ord, to));
               }
            }
         }

         if (!(component instanceof BPxInclude)) {
            BComponent[] kids = component.getChildComponents();

            for (int i = 0; i < kids.length; i++) {
               this.swapOrd(from, to, kids[i], entries);
            }
         }
      }
   }

   class Neqlize extends Command {
      Neqlize() {
         super(BBoundOrds.this.editor, BBoundOrds.LEX, "boundOrds.neqlize");
      }

      public CommandArtifact doInvoke() throws Exception {
         BComponent baseComponent = BBoundOrds.this.getBaseComponent();
         if (baseComponent == null) {
            BDialog.error(
               this.getOwner(), BBoundOrds.LEX.getText("boundOrds.noTargetComponent.title"), BBoundOrds.LEX.getText("boundOrds.noTargetComponent.message")
            );
            return null;
         } else {
            BISession componentSession = baseComponent.getSession();
            if (!(componentSession instanceof BFoxSession)) {
               BDialog.error(this.getOwner(), BBoundOrds.LEX.getText("boundOrds.noFoxSession.title"), BBoundOrds.LEX.getText("boundOrds.noFoxSession.message"));
               return null;
            } else {
               BOrd[] before = BBoundOrds.this.ords().keySet().toArray(BBoundOrds.EMPTY_ORD_ARRAY);

               try {
                  BNeqlizeOrds dialog = new BNeqlizeOrds(BBoundOrds.this.editor, before, baseComponent);
                  dialog.init();
                  if (!dialog.refresh(true)) {
                     return null;
                  } else {
                     String title = BBoundOrds.LEX.getText("boundOrds.neqlize.label");
                     int r = BDialog.open(BBoundOrds.this.editor, title, dialog, 3);
                     if (r != 1) {
                        return null;
                     } else {
                        List<ChangeOrds.Entry> entries = new ArrayList<>();
                        this.replaceInOrds(dialog.selected(), dialog, BBoundOrds.this.editor.getWidget(), entries);
                        ChangeOrds.Entry[] e = entries.toArray(BBoundOrds.EMPTY_CHANGE_ORDS_ENTRY_ARRAY);
                        return e.length > 0 ? new ChangeOrds(BBoundOrds.this.editor, e).doInvoke() : null;
                     }
                  }
               } catch (Exception var9) {
                  throw new BajaRuntimeException(BBoundOrds.LEX.getText("boundOrds.conversionFailed"), var9);
               }
            }
         }
      }

      private void replaceInOrds(Set<BOrd> selectedOrds, BNeqlizeOrds dialog, BComponent component, List<ChangeOrds.Entry> entries) {
         if (component instanceof BBinding || component instanceof BPxInclude) {
            SlotCursor<Property> c = component.getProperties();

            while (c.next(BOrd.class)) {
               BOrd oldOrd = (BOrd)c.get();
               if (selectedOrds.contains(oldOrd)) {
                  BNeqlizeOrds.NeqlizeData data = dialog.getNeqlizeData(oldOrd);
                  BOrd newOrd = BNeqlizeOrds.makeAfterOrd(data, true);
                  entries.add(new ChangeOrds.Entry(component, c.property(), oldOrd, newOrd));
               }
            }
         }

         if (!(component instanceof BPxInclude)) {
            BComponent[] kids = component.getChildComponents();

            for (BComponent kid : kids) {
               this.replaceInOrds(selectedOrds, dialog, kid, entries);
            }
         }
      }
   }

   class Relativize extends Command {
      boolean promptEnable = true;

      Relativize() {
         super(BBoundOrds.this.editor, BBoundOrds.LEX, "boundOrds.relativize");
         this.promptEnable = true;
      }

      Relativize(boolean promptEnable) {
         super(BBoundOrds.this.editor, BBoundOrds.LEX, "boundOrds.relativize");
         this.promptEnable = promptEnable;
      }

      public CommandArtifact doInvoke() throws Exception {
         BObject currentValue = BBoundOrds.this.editor.getCurrentValue();
         BComponent pxRootComponent = BBoundOrds.this.editorPane.getPxRootComponent();
         BComponent comp;
         if (currentValue instanceof BPxFile && pxRootComponent != null) {
            comp = pxRootComponent;
         } else {
            comp = (BComponent)currentValue;
         }

         String[] baseNames = comp.getSlotPath().getNames();
         BOrd[] before = BBoundOrds.this.ords().keySet().toArray(new BOrd[0]);
         BRelativizeOrds dialog = new BRelativizeOrds(BBoundOrds.this.editor, baseNames, before);
         dialog.init();
         if (this.promptEnable) {
            String title = BBoundOrds.LEX.getText("boundOrds.relativize.label");
            int r = BDialog.open(BBoundOrds.this.editor, title, dialog, 3);
            if (r != 1) {
               return null;
            }
         }

         List<ChangeOrds.Entry> entries = new ArrayList<>();
         this.relativizeComponent(dialog.selected(), baseNames, BBoundOrds.this.editor.getWidget(), entries);
         ChangeOrds.Entry[] e = entries.toArray(new ChangeOrds.Entry[0]);
         return e.length > 0 ? new ChangeOrds(BBoundOrds.this.editor, e).doInvoke() : null;
      }

      private void relativizeComponent(Set<BOrd> selectedOrds, String[] baseNames, BComponent component, List<ChangeOrds.Entry> entries) {
         if (component instanceof BBinding || component instanceof BPxInclude) {
            SlotCursor<Property> c = component.getProperties();

            while (c.next(BOrd.class)) {
               BOrd oldOrd = (BOrd)c.get();
               if (selectedOrds.contains(oldOrd)) {
                  BOrd newOrd = BRelativizeOrds.relativizeOrd(baseNames, oldOrd);
                  if (newOrd != null) {
                     entries.add(new ChangeOrds.Entry(component, c.property(), oldOrd, newOrd));
                  }
               }
            }
         }

         if (!(component instanceof BPxInclude)) {
            BComponent[] kids = component.getChildComponents();

            for (int i = 0; i < kids.length; i++) {
               this.relativizeComponent(selectedOrds, baseNames, kids[i], entries);
            }
         }
      }
   }

   class Replace extends Command {
      Replace() {
         super(BBoundOrds.this.editor, BBoundOrds.LEX, "boundOrds.replace");
      }

      public CommandArtifact doInvoke() throws Exception {
         BOrd[] before = BBoundOrds.this.ords().keySet().toArray(new BOrd[0]);
         BReplaceOrds dialog = new BReplaceOrds(BBoundOrds.this.editor, before);
         dialog.init();
         String title = BBoundOrds.LEX.getText("boundOrds.replace.label");
         int r = BDialog.open(BBoundOrds.this.editor, title, dialog, 3);
         if (r != 1) {
            return null;
         } else {
            String from = dialog.from.getTextAndSave();
            String to = dialog.to.getTextAndSave();
            if (from.isEmpty()) {
               return null;
            } else {
               List<ChangeOrds.Entry> entries = new ArrayList<>();
               this.replaceInOrds(dialog.selected(), from, to, BBoundOrds.this.editor.getWidget(), entries);
               ChangeOrds.Entry[] e = entries.toArray(new ChangeOrds.Entry[0]);
               return e.length > 0 ? new ChangeOrds(BBoundOrds.this.editor, e).doInvoke() : null;
            }
         }
      }

      private void replaceInOrds(Set<BOrd> selectedOrds, String fromStr, String toStr, BComponent component, List<ChangeOrds.Entry> entries) {
         if (component instanceof BBinding || component instanceof BPxInclude) {
            SlotCursor<Property> c = component.getProperties();

            while (c.next(BOrd.class)) {
               BOrd oldOrd = (BOrd)c.get();
               if (selectedOrds.contains(oldOrd)) {
                  String str = oldOrd.toString();
                  if (str.contains(fromStr)) {
                     entries.add(new ChangeOrds.Entry(component, c.property(), oldOrd, BOrd.make(TextUtil.replace(str, fromStr, toStr))));
                  }
               }
            }
         }

         if (!(component instanceof BPxInclude)) {
            BComponent[] kids = component.getChildComponents();

            for (int i = 0; i < kids.length; i++) {
               this.replaceInOrds(selectedOrds, fromStr, toStr, kids[i], entries);
            }
         }
      }
   }
}
