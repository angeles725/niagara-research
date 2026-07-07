package com.tridium.template.ui;

import com.tridium.px.editor.BPxEditorOptions;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.px.editor.util.EventUtil;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateService;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.file.EmbeddedPxScanner;
import com.tridium.template.file.EmbeddedPxSource;
import com.tridium.template.file.PxFileRef;
import com.tridium.ui.Binder;
import com.tridium.util.CompUtil;
import com.tridium.workbench.ord.RefFilter;
import com.tridium.workbench.propsheet.BPropertySheet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.baja.agent.BPxView;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.TextUtil;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxComponentEvent;
import javax.baja.px.editor.event.PxEvent;
import javax.baja.px.editor.event.PxLayerEvent;
import javax.baja.px.editor.event.PxListener;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BMenu;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BToggleButton;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.event.WidgetSubscriber;
import javax.baja.ui.list.BList;
import javax.baja.ui.list.ListRenderer;
import javax.baja.ui.list.ListRenderer.Item;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BTreePane;
import javax.baja.ui.px.BPxInclude;
import javax.baja.ui.tree.TreeController;
import javax.baja.ui.tree.TreeModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.ui.tree.TreeSelection;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditorBinding;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.DefaultNavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;

@NiagaraType
public class BTemplatePxEditor extends BEdgePane implements PxListener {
   public static final Type TYPE = Sys.loadType(BTemplatePxEditor.class);
   public static final RefFilter compFilter = new RefFilter() {
      public boolean accept(BObject parent, Slot slot) {
         if (slot != null && parent != null && slot.isProperty()) {
            Type slotType = parent.asComplex().get(slot.asProperty()).getType();
            if (slotType.is(BComponent.TYPE) && !slotType.is(BTemplateConfig.TYPE) && !slotType.is(BStatusValue.TYPE) && !slotType.is(BPxView.TYPE)) {
               int slotFlags = parent.asComplex().getFlags(slot);
               return (slotFlags & 3) == 0;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   };
   private static final Lexicon lex = Lexicon.make("template");
   private static final BModule module = Sys.getModuleForClass(BTemplatePxEditor.class);
   final List<PxFileRef> pxRefs;
   final Array<BImageFile> pxImages;
   ArrayList<SlotPath> pxEditTargets;
   private final List<PxFileRef> unusedPxRefs = new ArrayList<>();
   private final List<BImageFile> unusedImages = new ArrayList<>();
   private BSplitPane split;
   private BEdgePane rightPane = new BEdgePane();
   private BEdgePane pxEditPane = new BEdgePane();
   private Binder binder;
   private BComponent rootPxComp;
   private final BTemplatePxEditor.PxFileComboSubscriber pxFileComboSubscriber = new BTemplatePxEditor.PxFileComboSubscriber();
   private final BComponent tmplRoot;
   private BNavTree tree;
   private BListDropDown pxFileSelCombo;
   private BToggleButton grid;
   private BToggleButton snap;
   private BToggleButton showHatch;
   private BButton clearUnusedFilesBtn;
   private BToolBar toolBar;
   final BTemplateView view;
   private final BWidget owner;
   private BPxEditorOptions editorOptions;
   private BPxEditorPane editorPane;
   private BGridPane pxControlPane;
   private BPxEditor currentPxEditor;
   private final BTemplatePxEditor.EditorPxSource pxSource = new BTemplatePxEditor.EditorPxSource();
   private static final Pattern pxOrdPrefixPattern = Pattern.compile("^.+:\\^?((px/deploy/.+/)|(px/))?");
   private static final Pattern pxImagePrefixPattern = Pattern.compile("^.+:\\^?(.*/)*");

   public Type getType() {
      return TYPE;
   }

   public BTemplatePxEditor() {
      this.view = null;
      this.owner = null;
      this.tmplRoot = null;
      this.pxRefs = new ArrayList<>();
      this.pxImages = new Array(BImageFile.class);
   }

   public PxFileRef[] getPxRefs() {
      return this.pxRefs.toArray(new PxFileRef[0]);
   }

   public BPxEditor getCurrentPxEditor() {
      return this.currentPxEditor;
   }

   public BTemplatePxEditor(BTemplateView view, BComponent root, PxFileRef[] pxRefs, BImageFile[] pxImages) {
      this.view = view;
      this.owner = view.getShell();
      this.tmplRoot = root;
      this.pxRefs = new Array(pxRefs != null ? pxRefs : new PxFileRef[0]).list();
      this.pxImages = new Array(pxImages != null ? pxImages : new BImageFile[0]);
      DefaultNavTreeModel model = (DefaultNavTreeModel)(view.isApplicationTemplate()
         ? new ApplicationNavTreeModel(this.tmplRoot, ApplicationTemplateUtil.describeDefaultStation(null), view.getManifest())
         : new DefaultNavTreeModel(this.tmplRoot));
      this.tree = new BNavTree(model);
      this.tree.setController(new BTemplatePxEditor.Controller());
      this.tree.setSelection(new TreeSelection());
      this.tree.setMultipleSelection(true);
      model.setRootVisible(true);
      this.tree.setExpanded(model.getRoot(0), true);
      model.updateTree();
      this.split = new BSplitPane();
      this.split.setDividerPosition(20.0);
      this.split.setWidget1(this.buildLeftPane());
      this.split.setWidget2(this.buildRightPane());
      this.setCenter(this.split);
   }

   public void started() {
      this.updatePxSelCombo();
   }

   private BWidget buildLeftPane() {
      BEdgePane pane = new BEdgePane();
      pane.setCenter(new BBorderPane(new BTreePane(this.tree), BBorder.none, BInsets.make(0.0, 0.0, 0.0, 0.0)));
      return pane;
   }

   public BWidget buildRightPane() {
      this.rightPane = new BEdgePane();
      this.rightPane.setTop(this.makePxFileSelPane());
      this.noFileDisplay();
      return this.rightPane;
   }

   private void noFileDisplay() {
      BLabel noGraphicsSelected = new BLabel(lex.getText("template.pxEditor.noFileSelected"));
      BFont font = BFont.DEFAULT;
      noGraphicsSelected.setFont(BFont.make(font, font.getSize() * 2.0));
      this.rightPane.setCenter(new BBorderPane(noGraphicsSelected, 2.0, 2.0, 2.0, 2.0));
   }

   public BWidget makePxFileSelPane() {
      BBorderPane pxSelPane = new BBorderPane(new BNullWidget(), BInsets.make(10.0, 0.0, 1.0, 10.0));
      this.pxControlPane = new BGridPane();
      this.pxFileSelCombo = new BListDropDown();
      this.pxFileSelCombo.getList().setRenderer(new BTemplatePxEditor.BPxListRenderer());
      this.pxControlPane.setHalign(BHalign.left);
      this.pxControlPane.add("label?", new BLabel(lex.getText(lex.getText("template.pxPane.pxFileSelect"))));
      this.pxControlPane.add("pxFileSelect", this.pxFileSelCombo);
      this.clearUnusedFilesBtn = new BButton(new BTemplatePxEditor.PxClear(this));
      this.clearUnusedFilesBtn.setText("");
      this.unusedPxRefs.clear();
      this.unusedImages.clear();
      findUnusedPxRefsAndImages(this.pxSource, this.tmplRoot, this.pxRefs, this.pxImages, this.unusedPxRefs, this.unusedImages);
      this.clearUnusedFilesBtn.setEnabled(!this.unusedPxRefs.isEmpty() || !this.unusedImages.isEmpty());
      this.pxControlPane.setHalign(BHalign.center);
      this.pxControlPane.add("clearUnusedFiles", this.clearUnusedFilesBtn);
      this.pxControlPane.setColumnCount(30);
      this.pxControlPane.setRowGap(6.0);
      this.pxFileComboSubscriber.subscribe(this.pxFileSelCombo);
      this.pxFileComboSubscriber.subscribe(this.clearUnusedFilesBtn);
      this.setSubscribers();
      pxSelPane.setContent(this.pxControlPane);
      return pxSelPane;
   }

   private void addToolBar(BPxEditor pxEditor) {
      if (this.grid == null) {
         this.grid = new BToggleButton(BImage.make(BIcon.std("grid.png")), "", false);
         this.grid.setButtonStyle(BButtonStyle.toolBar);
         this.pxControlPane.add("cb?", this.grid);
      }

      if (this.snap == null) {
         this.snap = new BToggleButton(BImage.make(BIcon.std("snap.png")), "", false);
         this.snap.setButtonStyle(BButtonStyle.toolBar);
         this.pxControlPane.add("cb?", this.snap);
      }

      if (this.showHatch == null) {
         this.showHatch = new BToggleButton(BImage.make(BIcon.std("hash.png")), "", false);
         this.showHatch.setButtonStyle(BButtonStyle.toolBar);
         this.pxControlPane.add("cb?", this.showHatch);
      }

      this.toolBar = pxEditor.getViewToolBar();

      try {
         this.toolBar.remove("ToggleButton");
      } catch (Exception var3) {
      }

      if (this.pxControlPane.get("tb") != null) {
         this.pxControlPane.remove("tb");
      }

      this.pxControlPane.add("tb?", this.toolBar);
      this.setSubscribers();
   }

   private void setSubscribers() {
      if (this.grid != null) {
         this.pxFileComboSubscriber.subscribe(this.grid);
      }

      if (this.snap != null) {
         this.pxFileComboSubscriber.subscribe(this.snap);
      }

      if (this.showHatch != null) {
         this.pxFileComboSubscriber.subscribe(this.showHatch);
      }
   }

   void updatePxSelCombo() {
      BList list = this.pxFileSelCombo.getList();
      list.removeAllItems();

      for (PxFileRef pxRef : this.pxRefs) {
         list.addItem(pxRef);
      }

      if (list.getItemCount() > 0) {
         if (list.getSelectedIndex() < 0) {
            this.pxFileSelCombo.setSelectedIndex(0);
         }
      } else {
         this.noFileDisplay();
      }
   }

   public void selectPxSelCombo(PxFileRef file) {
      this.pxFileSelCombo.getList().setSelectedItem(file);
   }

   public void updateRightPane(PxFileRef pxFileRef) {
      this.save();
      BPxFile currentEditFile = pxFileRef.getPxFile();
      this.currentPxEditor = new BPxEditor();
      pxFileRef.setPxEditor(this.currentPxEditor);
      this.rightPane.setCenter(new BBorderPane(this.currentPxEditor, 2.0, 2.0, 2.0, 2.0));
      this.initBinder(this.currentPxEditor);
      this.currentPxEditor.loadValue(currentEditFile, Context.NULL);
      this.editorPane = (BPxEditorPane)this.currentPxEditor.getContent();
      this.editorOptions = this.editorPane.getOptions();
      this.addToolBar(this.currentPxEditor);
      this.pxControlPane.relayout();
      if (this.grid != null) {
         this.grid.setSelected(this.editorOptions.getShowGrid());
      }

      if (this.snap != null) {
         this.snap.setSelected(this.editorOptions.getUseSnap());
      }

      if (this.showHatch != null) {
         this.showHatch.setSelected(this.editorOptions.getShowHatch());
      }

      this.currentPxEditor.addPxListener(this);
      this.view.linkTo(this.currentPxEditor, BPropertySheet.pluginModified, BTemplateView.templateModified);
   }

   private void selectTreeNode() {
      this.selectTreeNode((PxFileRef)this.pxFileSelCombo.getSelectedItem());
   }

   private void selectTreeNode(PxFileRef pxFileRef) {
      if (pxFileRef != null) {
         TreeModel model = this.tree.getModel();
         String[] rootNames = this.tmplRoot.getSlotPath().getNames();
         BPxView[] pxViews = (BPxView[])CompUtil.getDescendants(this.tmplRoot, BPxView.class);
         TreeSelection selection = this.tree.getSelection();
         selection.deselectAll();
         this.rootPxComp = null;

         for (BPxView pxView : pxViews) {
            String ord = pxView.getPxFile().encodeToString();
            ord = ord.substring(ord.indexOf("px/") + 3);
            String pxOrd = pxFileRef.getPxOrd().encodeToString();
            pxOrd = pxOrd.substring(pxOrd.indexOf("px/") + 3);
            if (!ord.equals(pxOrd)) {
               model.getRoot(0).setExpanded(true);
            } else {
               String[] names = pxView.getParent().asComponent().getSlotPath().getNames();
               NavTreeNode node = (NavTreeNode)model.getRoot(0);

               for (int j = rootNames.length; j < names.length; j++) {
                  NavTreeNode temp = node.getChild(names[j], true);
                  if (temp == null) {
                     break;
                  }

                  node = temp;
               }

               TreeNode[] path = node.getPathFromRoot();
               this.tree.scrollPathToVisible(path);
               TreeNode n = path[path.length - 1];
               selection.select(n);
               if (this.rootPxComp == null) {
                  this.rootPxComp = ((NavTreeNode)n).getNavNode().asObject().asComponent();
               }
            }

            model.updateTree();
         }

         if (this.rootPxComp != null) {
            this.startBinder(this.rootPxComp);
         }
      }
   }

   private static BPxView getPxView(BComponent comp, String pxName) {
      BPxView[] pxViews = (BPxView[])comp.getChildren(BPxView.class);

      for (BPxView pxView : pxViews) {
         if (getFilename(pxView).equals(pxName)) {
            return pxView;
         }
      }

      return null;
   }

   private static String getFilename(BPxView view) {
      BOrd ord = view.getPxFile();
      String[] names = TextUtil.splitAndTrim(ord.toString(), '/');
      String fileName = names[names.length - 1];
      return TextUtil.splitAndTrim(fileName, '.')[0];
   }

   private PxFileRef getPxFileRef(String fileName) {
      for (PxFileRef pxRef : this.pxRefs) {
         if (pxRef.getPxName().equals(fileName)) {
            return pxRef;
         }
      }

      return null;
   }

   private void initBinder(BPxEditor pxEditor) {
      if ((this.binder = (Binder)pxEditor.fw(302)) == null) {
         this.binder = (Binder)pxEditor.fw(403);
      }
   }

   private void startBinder(BComponent rootPxComp) {
      this.editorPane.setPxRootComponent(rootPxComp);
      OrdTarget otgt = rootPxComp.getNavOrd().resolve();
      if (otgt != null) {
         this.binder.start(otgt, null);
      }
   }

   public void save() {
      for (PxFileRef pxRef : this.pxRefs) {
         BPxEditor pxEditor = (BPxEditor)pxRef.getPxEditor();
         if (pxEditor != null) {
            try {
               pxEditor.saveValue(pxRef.getPxFile(), null);
            } catch (Exception var5) {
               BTemplateService.logger.log(Level.SEVERE, "Unable to save PxRefs: " + var5.getLocalizedMessage(), (Throwable)var5);
            }
         }
      }

      this.pxEditTargets = this.findEditBindings();
   }

   private ArrayList<SlotPath> findEditBindings() {
      ArrayList<SlotPath> wrBindingList = new ArrayList<>();
      BPxView[] pxViews = (BPxView[])CompUtil.getDescendants(this.tmplRoot, BPxView.class);

      for (BPxView pxView : pxViews) {
         BComplex pxViewRoot = pxView.getParent();
         String[] filePathNames = pxView.getPxFile().toString().split("/");
         String pxName = filePathNames[filePathNames.length - 1];

         for (PxFileRef thisRef : this.pxRefs) {
            if (pxName.startsWith(thisRef.getPxName())) {
               BPxEditor pxEditor = (BPxEditor)thisRef.getPxEditor();
               if (pxEditor != null) {
                  SlotPath pxRootSlotPath = pxViewRoot.asComponent().getSlotPath();
                  Map<BOrd, List<BComponent>> bindings = findEditBindings(pxEditor);

                  for (BOrd bOrd : bindings.keySet()) {
                     String s = bOrd.toString();
                     String[] body = s.split("slot:");
                     if (s.startsWith("slot:")) {
                        SlotPath slotPath = body.length == 1 ? pxRootSlotPath : pxRootSlotPath.merge(new SlotPath(body[1]));
                        if (!wrBindingList.contains(slotPath)) {
                           wrBindingList.add(slotPath);
                        }
                     }
                  }
               }
            }
         }
      }

      return wrBindingList;
   }

   private static Map<BOrd, List<BComponent>> findEditBindings(BPxEditor pxEditor) {
      Map<BOrd, List<BComponent>> map = new TreeMap<>();
      findEditBindings(pxEditor.getWidget(), map);
      return map;
   }

   private static void findEditBindings(BComponent component, Map<BOrd, List<BComponent>> map) {
      if (component instanceof BWbFieldEditorBinding || component instanceof BPxInclude) {
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

         for (BComponent kid : kids) {
            findEditBindings(kid, map);
         }
      }
   }

   public void pxEvent(PxEvent event) {
      switch (event.getEventType()) {
         case 1:
            switch (((PxPropertyEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.relativizeOrds();
            }
         case 2:
         case 7:
         default:
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
                  this.relativizeOrds();
                  return;
               case 4:
               default:
                  return;
            }
         case 5:
         case 6:
            this.relativizeOrds();
            break;
         case 8:
            switch (((PxLayerEvent)event).getEventId()) {
               case 1:
               case 2:
                  this.relativizeOrds();
            }
      }
   }

   private void relativizeOrds() {
      if (this.editorPane != null) {
         this.editorPane.relativizeOrds();
      }
   }

   public CommandArtifact doSyncTree() {
      this.selectTreeNode();
      return null;
   }

   public CommandArtifact doNewView(BComponent c) {
      try {
         BTemplateNewPxViewDialog.invoke(this.owner, this, c);
      } catch (Exception var3) {
      }

      this.selectTreeNode((PxFileRef)this.pxFileSelCombo.getSelectedItem());
      return null;
   }

   public CommandArtifact toPxEditor(BComponent comp, String name) {
      String pxName = name + ".px";
      PxFileRef fileRef = this.getPxFileRef(pxName);
      this.selectPxSelCombo(fileRef);
      this.rootPxComp = comp;
      this.updateRightPane(fileRef);
      this.startBinder(this.rootPxComp);
      return null;
   }

   public CommandArtifact doRemove(BComponent comp, String pxName) {
      int confirm = BDialog.confirm(this, "Remove '" + pxName + "' view from " + comp.getName() + ". Are you sure?");
      if (confirm != 2 && confirm != 8) {
         BPxView pxView = getPxView(comp, pxName);
         if (pxView == null) {
            return null;
         } else {
            comp.remove(pxView);
            return this.doClearUnusedFiles(false);
         }
      } else {
         return null;
      }
   }

   public CommandArtifact doClearUnusedFiles(boolean informIfNone) {
      if (this.clearUnusedFiles(informIfNone)) {
         this.updatePxSelCombo();
         this.selectTreeNode();
         this.view.templateModified();
      }

      return null;
   }

   private boolean clearUnusedFiles(boolean informIfNone) {
      if (this.unusedPxRefs.isEmpty() && this.unusedImages.isEmpty()) {
         if (informIfNone) {
            BDialog.message(this, lex.getText("templatePxEditor.informNoFilesToClear"));
         }

         return false;
      } else {
         String unusedFilesMessage = getUnusedFilesMessage(this.unusedPxRefs, this.unusedImages);
         int confirm = BDialog.confirm(this, lex.getText("templatePxEditor.permissionToClear") + '\n' + unusedFilesMessage.toString());
         if (confirm != 4) {
            return false;
         } else {
            for (PxFileRef pxRef : this.unusedPxRefs) {
               BTemplateService.logger.log(Level.INFO, "Removing unused PX file from template: " + getPxName(pxRef.getPxOrd()));
               this.pxRefs.remove(pxRef);
            }

            for (BImageFile image : this.unusedImages) {
               BTemplateService.logger.log(Level.INFO, "Removing unused image file from template: " + image.getFileName());
               this.pxImages.remove(image);
            }

            return true;
         }
      }
   }

   private static String getUnusedFilesMessage(List<PxFileRef> unusedPxRefs, List<BImageFile> unusedImages) {
      StringBuilder unusedFiles = new StringBuilder();
      int itemCount = 0;

      for (PxFileRef unusedFile : unusedPxRefs) {
         if (itemCount >= 10) {
            break;
         }

         unusedFiles.append("  - ").append(unusedFile.getPxName()).append('\n');
         itemCount++;
      }

      for (BImageFile image : unusedImages) {
         if (itemCount >= 10) {
            break;
         }

         itemCount++;
         unusedFiles.append("  - ").append(image.getFileName()).append('\n');
      }

      int leftItemCount = unusedPxRefs.size() + unusedImages.size() - 10;
      if (itemCount >= 10 && leftItemCount > 0) {
         unusedFiles.append("     ").append(lex.getText("templatePxEditor.itemsLeftCount", new Object[]{leftItemCount}));
      }

      return unusedFiles.toString();
   }

   private static void findUnusedPxRefsAndImages(
      EmbeddedPxSource pxSource,
      BComponent root,
      List<PxFileRef> pxRefs,
      Array<BImageFile> pxImages,
      List<PxFileRef> unusedPxRefs,
      List<BImageFile> unusedImages
   ) {
      List<String> pxOrds = new ArrayList<>();
      List<String> imageOrds = new ArrayList<>();
      EmbeddedPxScanner.findPxAndImageOrds(pxSource, root, pxOrds, imageOrds);

      for (PxFileRef pxfRef : pxRefs) {
         String filePxName = getPxName(pxfRef.getPxOrd());
         if (!filePxName.isEmpty()) {
            boolean found = false;

            for (String pxOrd : pxOrds) {
               found = filePxName.equals(getPxName(pxOrd));
               if (found) {
                  break;
               }
            }

            if (!found) {
               unusedPxRefs.add(pxfRef);
            }
         }
      }

      for (BImageFile image : pxImages) {
         String imageName = image.getFileName();
         boolean found = false;

         for (String imageOrd : imageOrds) {
            String otherName = getImageName(imageOrd);
            found = imageName.equals(otherName);
            if (found) {
               break;
            }
         }

         if (!found) {
            unusedImages.add(image);
         }
      }
   }

   private static String getPxName(BOrd pxOrd) {
      return getPxName(pxOrd.toString(null));
   }

   private static String getPxName(String pxOrd) {
      return stripPrefix(pxOrdPrefixPattern, pxOrd);
   }

   private static String getImageName(String imageOrd) {
      return stripPrefix(pxImagePrefixPattern, imageOrd);
   }

   private static String stripPrefix(Pattern prefixPattern, String input) {
      Matcher pxOrdPrefixMatcher = prefixPattern.matcher(input);
      return pxOrdPrefixMatcher.find() ? input.substring(pxOrdPrefixMatcher.end()) : "";
   }

   private static class BPxListRenderer extends ListRenderer {
      private BPxListRenderer() {
      }

      public String getItemText(Item item) {
         PxFileRef fileRef = (PxFileRef)item.value;
         String displayName = fileRef.getPxOrd().encodeToString();
         return displayName.substring(displayName.indexOf("px/") + 3);
      }
   }

   class Controller extends TreeController {
      public BObject[] getSelectedObjects() {
         TreeNode[] nodes = this.getSelection().getNodes();
         BObject[] objs = new BObject[nodes.length];

         for (int i = 0; i < nodes.length; i++) {
            objs[i] = ((NavTreeNode)nodes[i]).getNavNode().asObject();
         }

         return objs;
      }

      private String[] getPxFileNames(BComponent comp) {
         BPxView[] pxViews = (BPxView[])comp.getChildren(BPxView.class);
         String[] fileNames = new String[pxViews.length];

         for (int i = 0; i < pxViews.length; i++) {
            fileNames[i] = BTemplatePxEditor.getFilename(pxViews[i]);
         }

         return fileNames;
      }

      protected void popup(BMouseEvent event, TreeNode node) {
         BMenu menu;
         if (node == null) {
            menu = new BMenu();
            menu.add("st", BTemplatePxEditor.this.new PxSyncTree());
         } else {
            BINavNode navNode = ((NavTreeNode)node).getNavNode();
            BComponent target = navNode.asObject().asComponent();
            String[] pxFileNames = this.getPxFileNames(navNode.asObject().asComponent());
            menu = new BMenu();
            menu.add("nv?", BTemplatePxEditor.this.new PxNewView(target));

            for (String pxFileName : pxFileNames) {
               menu.add("ed?", BTemplatePxEditor.this.new PxEditor(target, pxFileName));
               menu.add("del?", BTemplatePxEditor.this.new PxRemove(target, pxFileName));
            }
         }

         menu.removeConsecutiveSeparators();
         menu.open(event);
      }
   }

   private class EditorPxSource implements EmbeddedPxSource {
      private EditorPxSource() {
      }

      public BPxFile getPxFile(BOrd ord) {
         String searchName = BTemplatePxEditor.getPxName(ord);
         if (!searchName.isEmpty()) {
            for (PxFileRef pxRef : BTemplatePxEditor.this.pxRefs) {
               if (searchName.equals(BTemplatePxEditor.getPxName(pxRef.getPxOrd()))) {
                  return pxRef.getPxFile();
               }
            }
         }

         return null;
      }
   }

   class PxClear extends Command {
      PxClear(BWidget owner) {
         super(owner, BTemplatePxEditor.lex, "templatePxEditor.clearUnusedFiles");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplatePxEditor.this.doClearUnusedFiles(true);
      }
   }

   class PxEditor extends Command {
      String pxName;
      BComponent comp;

      PxEditor(BComponent comp, String pxName) {
         super(BTemplatePxEditor.this.owner, "Edit " + pxName);
         this.pxName = pxName;
         this.comp = comp;
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplatePxEditor.this.toPxEditor(this.comp, this.pxName);
      }

      public BImage getIcon() {
         return BImage.make(BIcon.std("edit.png"));
      }
   }

   class PxFileComboSubscriber extends WidgetSubscriber {
      public void actionPerformed(BWidgetEvent e) {
         BWidget widget = e.getWidget();
         if (widget.equals(BTemplatePxEditor.this.snap)) {
            BTemplatePxEditor.this.editorOptions.setUseSnap(BTemplatePxEditor.this.snap.isSelected());
         } else if (widget.equals(BTemplatePxEditor.this.showHatch)) {
            BTemplatePxEditor.this.editorOptions.setShowHatch(BTemplatePxEditor.this.showHatch.isSelected());
         } else if (widget.equals(BTemplatePxEditor.this.grid)) {
            BTemplatePxEditor.this.editorOptions.setShowGrid(BTemplatePxEditor.this.grid.isSelected());
         }

         BTemplatePxEditor.this.editorPane.forceRootLayout();
      }

      public void modified(BWidgetEvent e) {
         BWidget widget = e.getWidget();
         if (widget.equals(BTemplatePxEditor.this.pxFileSelCombo)) {
            PxFileRef pxRef = (PxFileRef)BTemplatePxEditor.this.pxFileSelCombo.getSelectedItem();
            BTemplatePxEditor.this.updateRightPane(pxRef);
            BTemplatePxEditor.this.selectTreeNode(pxRef);
         }
      }
   }

   class PxNewView extends Command {
      BComponent comp;

      PxNewView(BComponent comp) {
         super(BTemplatePxEditor.this.owner, "NewView");
         this.comp = comp;
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplatePxEditor.this.doNewView(this.comp);
      }

      public BImage getIcon() {
         return BImage.make(BIcon.make("module://icons/x16/views/newView.png"));
      }
   }

   class PxRemove extends Command {
      String pxName;
      BComponent comp;

      PxRemove(BComponent comp, String pxName) {
         super(BTemplatePxEditor.this.owner, "Remove " + pxName);
         this.comp = comp;
         this.pxName = pxName;
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplatePxEditor.this.doRemove(this.comp, this.pxName);
      }

      public BImage getIcon() {
         return BImage.make(BIcon.std("delete.png"));
      }
   }

   class PxSyncTree extends Command {
      PxSyncTree() {
         super(BTemplatePxEditor.this.owner, "SyncTree");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplatePxEditor.this.doSyncTree();
      }

      public BImage getIcon() {
         return BImage.make("module://icons/x16/sync.png");
      }
   }
}
