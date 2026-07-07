package com.tridium.template.ui;

import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.template.ui.tag.TagSupport;
import com.tridium.util.CompUtil;
import com.tridium.wiresheet.BWireSheetPane;
import com.tridium.wiresheet.WsController;
import com.tridium.wiresheet.states.NormalState;
import com.tridium.workbench.console.BConsole.HyperlinkInterceptor;
import com.tridium.workbench.propsheet.BPropertyEntry;
import com.tridium.workbench.propsheet.BPropertySheet;
import com.tridium.workbench.shell.WbCommands.RefreshCommand;
import com.tridium.workbench.shell.WbCommands.SaveCommand;
import com.tridium.workbench.slotsheet.BSlotSheet;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import javax.baja.driver.BDevice;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BActionMenuItem;
import javax.baja.ui.BButton;
import javax.baja.ui.BLabel;
import javax.baja.ui.BMenu;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BSplitPane;
import javax.baja.ui.pane.BTreePane;
import javax.baja.ui.tree.TreeNode;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;
import javax.baja.wiresheet.BWireSheet;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.DefaultNavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeController;
import javax.baja.workbench.nav.tree.NavTreeSubject;
import javax.baja.workbench.view.BWbComponentView;
import javax.baja.workbench.view.BWbComponentView.Attachable;

@NiagaraType
public class BTemplateBogEditor extends BEdgePane implements Attachable {
   public static final Type TYPE = Sys.loadType(BTemplateBogEditor.class);
   private static final Lexicon lex = Lexicon.make("template");
   private static BTypeSpec PROGRAM_TYPE = BTypeSpec.make("program:Program");
   private static final BIcon TEMPLATE_BADGE_ICON = BIcon.std("badges/nodeLibrary.png");
   private final BWidget owner;
   private BTemplateView view;
   private BComponent root;
   BNavTree tree;
   private BTreePane treePane;
   private BSplitPane split;
   private BPropertySheet propSheet;
   private BWireSheet wireSheet;
   private BSlotSheet slotSheet;
   private BWbComponentView programEditor;
   public ArrayList<BTemplateBogEditor.ProgramObjInfo> programObjInfos;
   boolean attached = false;
   BObject selObj;
   private BTemplateBogEditor.ProgramEditorSubscriber subscriber;
   private static final int PROP = 1;
   private static final int WIRE = 2;
   private static final int SLOT = 3;
   private static final int PROGRAM = 4;
   int selPane = 1;
   BOrd bogSelOrd = null;
   int programSelIndex = 0;

   public Type getType() {
      return TYPE;
   }

   public BTemplateBogEditor(BTemplateView view) {
      this.view = view;
      this.owner = view.getShell();
   }

   void doLoadValue(BComponent root) {
      BComponentSpace space = Objects.requireNonNull(root.getComponentSpace());
      if (space.getRootComponent() == null) {
         space.setRootComponent(this.view.getTemplateRoot());
      }

      TmplUtil.setTagDictionaryServiceForTemplateComponentSpace(root);
      TmplUtil.convertTagsToTagGroupRelations(root);
      this.root = root;
      TagSupport.convertMultiTags(root);
      BValue bValue = root.get("icon");
      if (bValue == null) {
         BIcon rootIcon = root.getIcon();
         BIcon tmplIcon = BIcon.make(rootIcon, TEMPLATE_BADGE_ICON);
         root.add("icon", tmplIcon, 5);
      }

      this.selObj = root;
      if (root instanceof BDevice) {
         ((BDevice)root).setEnabled(false);
      }

      DefaultNavTreeModel model = (DefaultNavTreeModel)(this.view.isApplicationTemplate()
         ? new ApplicationNavTreeModel(root, ApplicationTemplateUtil.describeDefaultStation(null), this.view.getManifest())
         : new DefaultNavTreeModel(root));
      this.tree = new BNavTree(model);
      model.setRootVisible(true);
      this.tree.setMultipleSelection(true);
      this.tree.setController(new BTemplateBogEditor.TreeController());
      this.tree.setExpanded(model.getRoot(0), true);
      this.treePane = new BTreePane(this.tree);
      this.propSheet = new BPropertySheet();
      this.wireSheet = new BWireSheet();
      BWireSheetPane wsPane = this.wireSheet.getWireSheetPane();
      WsController wsc = new BTemplateBogEditor.TemplateWsController(wsPane);
      wsPane.setController(wsc);
      this.slotSheet = new BSlotSheet();
      this.subscriber = new BTemplateBogEditor.ProgramEditorSubscriber();
      this.programObjInfos = this.initProgramEditors();
      this.split = new BSplitPane();
      this.split.setDividerPosition(30.0);
      this.split.setWidget1(this.treePane);
      this.setCenter(this.split);

      try {
         if (this.bogSelOrd != null) {
            this.selObj = this.bogSelOrd.resolve(root).getComponent();
         }
      } catch (Throwable var13) {
         this.selPane = 1;
         var13.printStackTrace();
      }

      switch (this.selPane) {
         case 2:
            this.split.setWidget2(this.wireSheet);
            this.wireSheet.loadValue(this.selObj);
            break;
         case 3:
            this.split.setWidget2(this.slotSheet);
            this.slotSheet.loadValue(this.selObj);
            break;
         case 4:
            if (this.programObjInfos.size() > 0) {
               if (this.programSelIndex >= this.programObjInfos.size()) {
                  this.programSelIndex = 0;
               }

               this.programEditor = this.programObjInfos.get(this.programSelIndex).getProgramEditor();
               this.split.setWidget2(this.programEditor);
               this.selObj = this.programObjInfos.get(this.programSelIndex).getProgObjOrd().resolve(root).getComponent();
               this.doSyncTree();
               break;
            }
         default:
            this.selPane = 1;
            this.split.setWidget2(this.propSheet);
            this.propSheet.loadValue(this.selObj);
      }

      this.disablePropSheetHyperlink();
      this.hideButtons();
      if (this.programObjInfos != null) {
         for (BTemplateBogEditor.ProgramObjInfo programObjInfo : this.programObjInfos) {
            if (programObjInfo.getOutOfDate()) {
               BGridPane gridPane = new BGridPane(2);
               BLabel info = programObjInfo.getProgObjStatus();
               String s = programObjInfo.getProgObjOrd().toString();
               BTextField ordFe = new BTextField(s, 45, false);
               gridPane.add(null, info);
               gridPane.add(null, ordFe);
               this.view.addToInfoPane(gridPane);
            }
         }
      }

      this.view.linkTo(this.propSheet, BPropertySheet.pluginModified, BTemplateView.templateModified);
      this.view.linkTo(this.wireSheet, BWireSheet.pluginModified, BTemplateView.templateModified);
      this.view.linkTo(this.slotSheet, BSlotSheet.pluginModified, BTemplateView.templateModified);
      this.view.linkTo(this.tree, BNavTree.treeModified, BTemplateView.templateModified);
      this.view.attach(this);
   }

   public void deactivated() {
      if (this.propSheet != null && this.propSheet.getWbShell() != null) {
         this.propSheet.deactivated();
      }

      if (this.slotSheet != null) {
         this.slotSheet.deactivated();
      }

      if (this.wireSheet != null) {
         this.wireSheet.deactivated();
      }

      if (this.root != null) {
         TmplUtil.convertTagGroupRelationsToTags(this.root);
      }
   }

   private ArrayList<BTemplateBogEditor.ProgramObjInfo> initProgramEditors() {
      BObject[] progObjs = (BObject[])CompUtil.getDescendants(this.root, PROGRAM_TYPE.getInstance().getClass());
      ArrayList<BTemplateBogEditor.ProgramObjInfo> programObjInfos = new ArrayList<>();

      for (int i = 0; i < progObjs.length; i++) {
         BWbComponentView instance = (BWbComponentView)BTypeSpec.make("program:ProgramEditor").getInstance();
         instance.loadValue(progObjs[i]);
         BTemplateBogEditor.ProgramObjInfo programObjInfo = new BTemplateBogEditor.ProgramObjInfo(instance, progObjs[i].asComponent().getSlotPathOrd());
         this.updateProgramObjStatus(programObjInfo);
         programObjInfos.add(programObjInfo);
         this.subscriber.subscribe(instance);
         this.subscriber.subscribe(progObjs[i].asComponent());
      }

      return programObjInfos;
   }

   public CommandArtifact toPropertysheet() {
      this.selObj = this.tree.getSelectedObject();
      this.propSheet.loadValue(this.selObj);
      this.disablePropSheetHyperlink();
      this.split.setWidget2(this.propSheet);
      this.hideButtons();
      this.selPane = 1;
      this.view.swapActiveView(null);
      return null;
   }

   public CommandArtifact toWiresheet() {
      this.selObj = this.tree.getSelectedObject();
      this.wireSheet.loadValue(this.selObj, null);
      this.split.setWidget2(this.wireSheet);
      this.selPane = 2;
      BWireSheetPane wireSheetPane = this.wireSheet.getWireSheetPane();
      if (this.selObj.isComponent()) {
         if (TagSupport.isSubtemplate(this.selObj.asComponent())) {
            wireSheetPane.controller.transition(new TemplateWsState(wireSheetPane));
         } else {
            wireSheetPane.controller.transition(new NormalState(wireSheetPane));
         }
      }

      this.view.swapActiveView(this.wireSheet);
      return null;
   }

   public CommandArtifact toSlotsheet() {
      this.selObj = this.tree.getSelectedObject();
      this.slotSheet.loadValue(this.selObj, null);
      this.split.setWidget2(this.slotSheet);
      this.selPane = 3;
      if (this.selObj.isComponent() && TagSupport.hasTemplateAncestor(this.selObj.asComponent())) {
         this.slotSheet.setReadonly(true);
      }

      this.view.swapActiveView(null);
      return null;
   }

   public CommandArtifact toProgramEditor() {
      this.selObj = this.tree.getSelectedObject();
      BOrd programSelOrd = this.selObj.asComponent().getSlotPathOrd();
      this.programSelIndex = this.getProgramObjectIndex(programSelOrd);
      if (this.programSelIndex >= 0) {
         this.programEditor = this.programObjInfos.get(this.programSelIndex).getProgramEditor();
      } else {
         System.out.println("programObject not found: rebuilding ");
         this.programObjInfos = this.initProgramEditors();
         this.programSelIndex = this.getProgramObjectIndex(programSelOrd);
         if (this.programSelIndex < 0) {
            System.out.println("**** programObject not found ****");
            return null;
         }

         this.programEditor = this.programObjInfos.get(this.programSelIndex).getProgramEditor();
      }

      this.split.setWidget2(this.programEditor);
      this.selPane = 4;
      this.view.swapActiveView(this.programEditor);
      return null;
   }

   private int getProgramObjectIndex(BOrd progObjOrd) {
      for (int i = 0; i < this.programObjInfos.size(); i++) {
         if (this.programObjInfos.get(i).getProgObjOrd().equals(progObjOrd)) {
            return i;
         }
      }

      return -1;
   }

   public CommandArtifact doSyncTree() {
      if (this.selObj == null) {
         return null;
      } else {
         this.selObj.asComponent().getSlotPathOrd();

         try {
            this.tree.expandToNavNode((BINavNode)this.selObj);
         } catch (Exception var4) {
            this.selObj = this.root;

            try {
               this.selPane = 1;
               this.doLoadValue(this.root);
               this.tree.expandToNavNode((BINavNode)this.selObj);
            } catch (Exception var3) {
               var3.printStackTrace();
            }
         }

         return null;
      }
   }

   public void save() {
      try {
         if (this.root.get("wsAnnotation") != null) {
            this.root.remove("wsAnnotation");
         }

         this.propSheet.saveValue(this.propSheet.getCurrentValue(), null);

         for (BTemplateBogEditor.ProgramObjInfo poi : this.programObjInfos) {
            this.updateProgramObjStatus(poi);
            if (poi.getModified()) {
               poi.getProgramEditor().saveValue();
            }

            poi.setModified(false);
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public boolean isApplicationTemplate() {
      return this.view.isApplicationTemplate();
   }

   public TemplateManifest getManifest() {
      return this.view.getManifest();
   }

   private void updateProgramObjectStatus() {
      for (BTemplateBogEditor.ProgramObjInfo poi : this.programObjInfos) {
         this.updateProgramObjStatus(poi);
      }
   }

   private void updateProgramObjStatus(BTemplateBogEditor.ProgramObjInfo poi) {
      BWbComponentView programEditor = poi.getProgramEditor();
      BWidget content = programEditor.getContent();
      if (content instanceof BEdgePane) {
         BWidget top = ((BEdgePane)content).getTop();
         BWidget status = ((BBorderPane)top).getContent();
         if (status instanceof BLabel) {
            poi.setProgObjStatus(new BLabel(((BLabel)status).getText()));
            BImage statusImage = ((BLabel)status).getImage();
            poi.setOutOfDate(statusImage.toString().contains("circleGold.png"));
         }
      }
   }

   private void disablePropSheetHyperlink() {
      BPropertyEntry[] a = (BPropertyEntry[])CompUtil.getDescendants(this.propSheet, BPropertyEntry.class);

      for (int i = 0; i < a.length; i++) {
         a[i].disableHyperlink(true);
      }
   }

   private void hideButtons() {
      BButton[] a = (BButton[])CompUtil.getDescendants(this.propSheet, BButton.class);

      for (int i = 0; i < a.length; i++) {
         Command command = a[i].getCommand();
         if (command != null && (command.getClass().equals(SaveCommand.class) || command.getClass().equals(RefreshCommand.class))) {
            a[i].setVisible(false);
         }
      }
   }

   public boolean consoleHyperlink(File file, int line1, int col1, int line2, int col2) {
      if (this.selPane == 4 && this.programEditor instanceof HyperlinkInterceptor) {
         HyperlinkInterceptor interceptor = (HyperlinkInterceptor)this.programEditor;
         return interceptor.consoleHyperlink(file, line1, col1, line2, col2);
      } else {
         return false;
      }
   }

   public void attached(BWbComponentView editor) {
      this.attached = true;
   }

   public void detached(BWbComponentView editor) {
      this.attached = false;
   }

   public void handleComponentEvent(BComponentEvent event) {
      if (this.attached) {
         if (event.getId() != 3) {
            switch (this.selPane) {
               case 1:
                  this.propSheet.handleComponentEvent(event);
                  break;
               case 2:
                  this.wireSheet.handleComponentEvent(event);
                  break;
               case 3:
                  this.slotSheet.handleComponentEvent(event);
                  break;
               case 4:
                  this.programEditor.handleComponentEvent(event);
            }
         }
      }
   }

   class ProgramEditor extends Command {
      ProgramEditor() {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "programEditor");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplateBogEditor.this.toProgramEditor();
      }
   }

   class ProgramEditorSubscriber extends Subscriber {
      public void event(BComponentEvent event) {
         if (BTemplateBogEditor.this.programEditor == null) {
            BTemplateBogEditor.this.programEditor = BTemplateBogEditor.this.programObjInfos.get(BTemplateBogEditor.this.programSelIndex).getProgramEditor();
         }

         BTemplateBogEditor.this.programEditor.handleComponentEvent(event);
         switch (event.getId()) {
            case 4:
            default:
               break;
            case 5:
               if (!event.getSlotName().equals("pluginModified")) {
                  break;
               }
            case 0:
            case 1:
            case 2:
            case 3:
               BTemplateBogEditor.this.view.doTemplateModified();
               BTemplateBogEditor.ProgramObjInfo programObjInfo = BTemplateBogEditor.this.programObjInfos.get(BTemplateBogEditor.this.programSelIndex);
               BTemplateBogEditor.this.updateProgramObjStatus(programObjInfo);
               programObjInfo.setModified(true);
         }
      }
   }

   class ProgramObjInfo {
      boolean isModified = false;
      boolean outOfDate = false;
      BLabel progObjStatus;
      BOrd progObjOrd;
      BWbComponentView programEditor;

      ProgramObjInfo(BWbComponentView programEditor, BOrd progObjOrd) {
         this.isModified = false;
         this.programEditor = programEditor;
         this.progObjOrd = progObjOrd;
      }

      boolean getModified() {
         return this.isModified;
      }

      void setModified(boolean isModified) {
         this.isModified = isModified;
      }

      boolean getOutOfDate() {
         return this.outOfDate;
      }

      void setOutOfDate(boolean outOfDate) {
         this.outOfDate = outOfDate;
      }

      BLabel getProgObjStatus() {
         return this.progObjStatus;
      }

      void setProgObjStatus(BLabel progObjStatus) {
         this.progObjStatus = progObjStatus;
      }

      BWbComponentView getProgramEditor() {
         return this.programEditor;
      }

      void setProgramEditor(BWbComponentView programEditor) {
         this.programEditor = programEditor;
      }

      BOrd getProgObjOrd() {
         return this.progObjOrd;
      }

      void setProgObjOrd(BOrd progObjOrd) {
         this.progObjOrd = progObjOrd;
      }

      int getProgramObjIndex(BOrd progObjOrd) {
         for (int i = 0; i < BTemplateBogEditor.this.programObjInfos.size(); i++) {
            if (BTemplateBogEditor.this.programObjInfos.get(i).getProgObjOrd().equals(progObjOrd)) {
               return i;
            }
         }

         return -1;
      }
   }

   class PropertySheet extends Command {
      PropertySheet() {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "propSheet");
      }

      PropertySheet(boolean enable) {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "propSheet");
         this.setEnabled(enable);
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplateBogEditor.this.toPropertysheet();
      }
   }

   class SlotSheet extends Command {
      SlotSheet() {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "slotSheet");
      }

      SlotSheet(boolean enable) {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "slotSheet");
         this.setEnabled(enable);
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplateBogEditor.this.toSlotsheet();
      }
   }

   class SyncTree extends Command {
      SyncTree() {
         super(BTemplateBogEditor.this.owner, "SyncTree");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplateBogEditor.this.doSyncTree();
      }

      public BImage getIcon() {
         return BImage.make("module://icons/x16/sync.png");
      }
   }

   public class TemplateWsController extends WsController {
      public TemplateWsController(BWireSheetPane ws) {
         super(ws);
      }

      public void mousePressed(BMouseEvent event) {
         if (event.getClickCount() <= 1) {
            super.mousePressed(event);
         }
      }
   }

   class TreeController extends NavTreeController {
      public TreeController() {
      }

      protected BMenu makePopup(NavTreeSubject subject) {
         BMenu menu = super.makePopup(subject);
         if (menu == null) {
            menu = new BMenu();
            menu.add("st", BTemplateBogEditor.this.new SyncTree());
         } else {
            menu.addSeparatorToFront();
            Object[] objects = subject.get();
            boolean hasTemplateAncestor = false;
            if (objects.length > 0 && objects[0] instanceof BComponent) {
               BComponent component = (BComponent)objects[0];
               if (component.getType().is(BTemplateBogEditor.PROGRAM_TYPE.getResolvedType())) {
                  menu.addItemToFront("pe", new BActionMenuItem(BTemplateBogEditor.this.new ProgramEditor()));
               }
            }

            menu.addItemToFront("sl", new BActionMenuItem(BTemplateBogEditor.this.new SlotSheet(!hasTemplateAncestor)));
            menu.addItemToFront("ws", new BActionMenuItem(BTemplateBogEditor.this.new WireSheet()));
            menu.addItemToFront("ps", new BActionMenuItem(BTemplateBogEditor.this.new PropertySheet(!hasTemplateAncestor)));
         }

         return menu;
      }

      public void nodeDoubleClicked(BMouseEvent event, TreeNode node) {
         Object subject = node.getSubject();
         BTemplateBogEditor.this.toPropertysheet();
      }
   }

   class WireSheet extends Command {
      WireSheet() {
         super(BTemplateBogEditor.this.owner, BTemplateBogEditor.lex, "wireSheet");
      }

      public CommandArtifact doInvoke() throws Exception {
         return BTemplateBogEditor.this.toWiresheet();
      }
   }
}
