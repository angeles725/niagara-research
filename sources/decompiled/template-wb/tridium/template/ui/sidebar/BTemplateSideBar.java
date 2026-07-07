package com.tridium.template.ui.sidebar;

import com.tridium.sys.module.BModuleFile;
import com.tridium.sys.module.DefaultModulesFileManager;
import com.tridium.template.TemplateConst;
import com.tridium.template.api.TemplateType;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.file.TemplateManager;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.manifest.TemplateManifest.Subtemplate;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.template.ui.file.ExportConfigsCommand;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.custom.nss.StyleUtils;
import com.tridium.util.ThrowableUtil;
import com.tridium.workbench.shell.WbMain;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nav.BINavNode;
import javax.baja.nav.NavEvent;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BMenu;
import javax.baja.ui.BWidget;
import javax.baja.ui.BWidgetShell;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.bookmark.BBookmark;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.list.DefaultListModel;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.pane.BTreePane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.tree.TreeNode;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.DefaultNavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeController;
import javax.baja.workbench.nav.tree.NavTreeModel;
import javax.baja.workbench.nav.tree.NavTreeNode;
import javax.baja.workbench.nav.tree.NavTreeSubject;
import javax.baja.workbench.sidebar.BWbSideBar;
import javax.baja.workbench.util.BNotifyPane;

@NiagaraType
@NiagaraAction(
   name = "updateTree"
)
public class BTemplateSideBar extends BWbSideBar {
   public static final Action updateTree = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BTemplateSideBar.class);
   private static final BImage errorIcon = BImage.make("module://icons/x32/error.png");
   private static final BIcon jarIcon = BIcon.make("module://icons/x16/modules.png");
   static final Lexicon lex = Lexicon.make("template");
   public static final Logger log = Logger.getLogger("ntpl");
   private static final String SLOT_NAME_SEPARATOR = ".";
   private BListDropDown selTree = new BListDropDown();
   private final BNavTree tree;
   BEdgePane content;
   BTreePane treePane;
   BButton bModule;
   BOrd moduleOrd = BOrd.NULL;
   BTemplateSideBar.TmplSideBarModel modModel = null;
   private boolean init = false;
   private boolean inError = false;

   public void updateTree() {
      this.invoke(updateTree, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BTemplateSideBar() {
      this.selTree = new BListDropDown();
      this.selTree.getList().setModel(new BTemplateSideBar.NavListModel());
      this.linkTo(null, this.selTree, BListDropDown.listActionPerformed, updateTree);
      this.tree = new BTemplateSideBarNavTree();
      this.tree.setMultipleSelection(true);
      this.tree.setController(new BTemplateSideBar.TplNavController());
      this.bModule = new BButton(new BTemplateSideBar.MakeModule(this), false, true);
      this.bModule.setButtonStyle(BButtonStyle.toolBar);
      this.bModule.setFocusTraversable(false);
      BGridPane grid = new BGridPane(3);
      grid.setColumnAlign(BHalign.fill);
      grid.setRowAlign(BValign.fill);
      grid.add(null, this.bModule);
      BEdgePane top = new BEdgePane();
      top.setLeft(grid);
      top.setCenter(new BBorderPane(this.selTree, 0.0, 0.0, 0.0, 5.0));
      BBorderPane topPane = new BBorderPane(top, 5.0, 5.0, 5.0, 5.0);
      StyleUtils.addStyleClass(topPane, "sidebar-header");
      this.content = new BEdgePane();
      this.content.setTop(topPane);
      this.treePane = new BTreePane(this.tree);
      this.tree.setStyleId("template-sidebar");
      StyleUtils.addStyleClass(this.tree, "sidebar-body");
      StyleUtils.addStyleClass(this.treePane, "sidebar-scroll-pane");
      this.content.setCenter(this.treePane);
      this.setContent(this.content);
   }

   public void started() throws Exception {
      super.started();
      if (!this.init) {
         this.loadSelTree();
         this.doUpdateTree();
         this.init = true;
      }
   }

   private void loadSelTree() throws Exception {
      this.selTree.getList().removeAllItems();
      BDirectory modDir = BFileSystem.INSTANCE.makeDir(TemplateConst.TEMPLATE_MODULE_DIR);
      this.moduleOrd = modDir.getAbsoluteOrd();
      BBookmark moduleBookmark = new BBookmark(lex.getText("templateSideBar.moduleMenu"), modDir.getNavOrd(), jarIcon);
      this.selTree.getList().addItem(new BTemplateSideBar.NavWrapper(moduleBookmark, true));
      BBookmark templateBookmark = new BBookmark(
         lex.getText("templateSideBar.templateMenu"), NtplUtil.getTemplateDirectoryOrd(), NtplUtil.getTemplateDirectory().getNavIcon()
      );
      this.selTree.getList().addItem(new BTemplateSideBar.NavWrapper(templateBookmark, false));
      this.selTree.getList().setSelectedIndex(1);
   }

   private void displayError(String name, Exception e) {
      this.inError = true;
      BGridPane a = new BGridPane(1);
      a.setRowGap(1.0);
      a.add(null, new BLabel(lex.getText("templateSideBar.couldNotFind"), Theme.widget().getBoldText()));
      a.add(null, new BLabel(name));
      BGridPane b = new BGridPane(2);
      b.add(null, new BLabel(errorIcon, ""));
      b.add(null, a);
      BGridPane c = new BGridPane(1);
      c.setRowGap(5.0);
      c.setColumnAlign(BHalign.center);
      c.add(null, b);
      c.add(null, new BButton(new BTemplateSideBar.Details(this, name, e)));
      this.content.setCenter(c);
   }

   public void doUpdateTree() {
      this.inError = false;
      BTemplateSideBar.NavWrapper w = (BTemplateSideBar.NavWrapper)this.selTree.getSelectedItem();

      try {
         BOrd ord = w.bookmark.getBookmarkOrd();
         BObject obj = ord.resolve().get();
         w.bookmark.setIcon(obj.getIcon());
         BTemplateSideBar.TmplSideBarModel model = new BTemplateSideBar.TmplSideBarModel((BINavNode)obj);
         if (w.isModule) {
            this.modModel = model;
         }

         this.tree.setModel(model);
         this.tree.relayout();
         if (!(this.content.getCenter() instanceof BTreePane)) {
            this.content.setCenter(this.treePane);
         }

         this.bModule.setEnabled(!w.isModule);
      } catch (Exception var5) {
         this.displayError(w.bookmark.getText(), var5);
      }
   }

   public void doTreeSelectionModified() {
      this.repaint();
   }

   public void activeViewChanged() {
      if (this.inError) {
         this.doUpdateTree();
      }
   }

   private void remove(BComponent c, String name) {
      if (c != null) {
         Property p = c.getProperty(name);
         if (p != null) {
            c.remove(p);
         }
      }
   }

   public static Collection<BWbDeployableNtplFile> collectTemplates(BObject[] selectedObjects) {
      Collection<BWbDeployableNtplFile> templates = new TreeSet<>();

      for (BObject selectedObject : selectedObjects) {
         if (selectedObject instanceof BIFile) {
            collectTemplates((BIFile)selectedObject, templates);
         }
      }

      return templates;
   }

   private static void collectTemplates(BIFile selectedFile, Collection<BWbDeployableNtplFile> templates) {
      if (selectedFile instanceof BWbDeployableNtplFile) {
         templates.add((BWbDeployableNtplFile)selectedFile);
      } else if (selectedFile instanceof BDirectory) {
         for (BIFile childFile : ((BDirectory)selectedFile).listFiles()) {
            collectTemplates(childFile, templates);
         }
      }
   }

   public static BOrd makeTemplateModule(
      Collection<BWbDeployableNtplFile> templates, String name, String description, String vendor, String version, String fileName
   ) throws IOException {
      FilePath path = TemplateConst.TEMPLATE_MODULE_DIR;
      BDirectory modDir = BFileSystem.INSTANCE.makeDir(path);
      BIFile biFile = modDir.getFileSpace().makeFile(path.merge(fileName));
      BOrd result = biFile.getOrdInSession();

      try (ZipOutputStream zout = new ZipOutputStream(biFile.getOutputStream())) {
         ZipEntry zmod = new ZipEntry("META-INF/module.xml");
         zout.putNextEntry(zmod);
         writeModuleXml(zout, name, description, vendor, version);
         zout.closeEntry();

         for (BWbDeployableNtplFile template : templates) {
            FilePath relativePath = NtplUtil.getTemplateRelativePath(template.getFilePath(), TemplateType.COMPONENT);
            ZipEntry zntp = new ZipEntry(relativePath.isAbsolute() ? relativePath.getName() : relativePath.getBody());
            zout.putNextEntry(zntp);

            try (InputStream templateStream = template.getInputStream()) {
               BajaFileUtil.pipe(templateStream, zout);
            }

            zout.closeEntry();
         }
      } finally {
         ((BModuleFile)biFile).close();
         TemplateManager.INSTANCE.modsChanged();
      }

      return result;
   }

   private Hashtable<String, ArrayList<String>> makeSubtemplateMap(BObject[] files) throws Exception {
      Hashtable<String, ArrayList<String>> hashtable = new Hashtable<>();
      FilePath path = TemplateConst.TEMPLATE_DIR;
      BDirectory tnplDir = BFileSystem.INSTANCE.makeDir(path);
      BIFile[] ntplFiles = tnplDir.listFiles();

      for (BObject file : files) {
         if (file instanceof BWbDeployableNtplFile) {
            BWbDeployableNtplFile ntplFile = (BWbDeployableNtplFile)file;
            TemplateManifest manifest = ntplFile.getTemplateManifest();
            String templateName = manifest.vendor + '/' + ntplFile.getFileName();
            ArrayList<String> subtemplateList = new ArrayList<>();

            for (BIFile otherFile : ntplFiles) {
               if (file != otherFile && otherFile instanceof BWbDeployableNtplFile) {
                  BWbDeployableNtplFile otherNtplFile = (BWbDeployableNtplFile)otherFile;
                  TemplateManifest stm = otherNtplFile.getTemplateManifest();

                  for (Subtemplate st : stm.subtemplates) {
                     if (st != null) {
                        String stFileOrd = st.ntplFileOrd;
                        if (stFileOrd.contains(ntplFile.getNavName())) {
                           String ordInHost = otherNtplFile.getOrdInHost().toString(null);
                           if (!subtemplateList.contains(ordInHost)) {
                              subtemplateList.add(ordInHost);
                           }
                        }
                     }
                  }
               }
            }

            hashtable.put(templateName, subtemplateList);
         }
      }

      return hashtable;
   }

   private void notifySuccess(String fileName) {
      BNotifyPane pane = new BNotifyPane();
      BGridPane notifyGrid = new BGridPane(1);
      BLabel title = new BLabel(lex.getText("templateSideBar.makeModule"));
      StyleUtils.addStyleClass(title, "strong");
      notifyGrid.add(null, title);
      BLabel status = new BLabel(BImage.make("module://icons/x16/job/success.png"), "success");
      notifyGrid.add(null, status);
      String detailText = lex.getText("templateSideBar.notify.success", new Object[]{fileName});
      BLabel msg = new BLabel(BImage.make(BIcon.make(lex.getText("templateSideBar.notify.icon"))), detailText);
      notifyGrid.add(null, msg);
      pane.setCenter(notifyGrid);
      pane.open();
   }

   private boolean isValidModuleName(String name) {
      String err;
      if (name.isEmpty()) {
         err = lex.getText("templateSideBar.makeModule.noName");
      } else {
         if (SlotPath.isValidName(name)) {
            return true;
         }

         err = lex.getText("templateSideBar.makeModule.invalidName", new Object[]{name});
      }

      BDialog.error(this.getWbShell(), lex.getText("templateName.invalidNameTitle"), err);
      return false;
   }

   private static void writeModuleXml(ZipOutputStream zout, String moduleName, String description, String vendor, String vendorVersion) throws IOException {
      String buf = "<module\n  name = \""
         + moduleName
         + "\"\n  bajaVersion = \"0\"\n  description = \""
         + description
         + "\"\n  vendor = \""
         + vendor
         + "\"\n  vendorVersion = \""
         + vendorVersion
         + "\"\n  templateJar=\"true\"\n>\n\n<dependencies>\n</dependencies>\n\n<dirs/>\n\n<types/>\n\n</module>\n";
      PrintWriter pw = new PrintWriter(zout);
      pw.write(buf);
      pw.flush();
      zout.flush();
   }

   private static boolean navNodeContainsTemplate(BINavNode navNode) {
      return navNodesContainTemplate(navNode.getNavChildren());
   }

   private static boolean navNodesContainTemplate(BINavNode[] navNodes) {
      for (BINavNode navChild : navNodes) {
         if (navChild instanceof BDirectory && navNodeContainsTemplate(navChild)) {
            return true;
         }

         if (navChild.getNavName().endsWith(".ntpl")) {
            return true;
         }
      }

      return false;
   }

   static class Details extends Command {
      private final Exception e;

      public Details(BWidget owner, String name, Exception e) {
         super(owner, BTemplateSideBar.lex.getText("NavSideBar.details"));
         this.e = e;
      }

      public CommandArtifact doInvoke() {
         BTextEditor msg = new BTextEditor(ThrowableUtil.dumpToString(this.e), false);
         BTextEditorPane pane = new BTextEditorPane(msg, 25, 80);
         BDialog.open(this.getOwner(), BTemplateSideBar.lex.getText("NavSideBar.details"), pane, 1);
         return null;
      }
   }

   class FindUsages extends Command {
      public FindUsages(BWidget owner) {
         super(owner, BTemplateSideBar.lex, "commands.showUsedBy");
      }

      public CommandArtifact doInvoke() {
         try {
            BObject[] sel = BTemplateSideBar.this.tree.getSelectedObjects();
            if (sel.length == 0) {
               return null;
            }

            Hashtable<String, ArrayList<String>> subtemplateMap = BTemplateSideBar.this.makeSubtemplateMap(sel);
            Enumeration<String> stEnum = subtemplateMap.keys();
            StringBuilder sb = new StringBuilder();

            while (stEnum.hasMoreElements()) {
               String stName = stEnum.nextElement();
               if (subtemplateMap.size() > 0 && subtemplateMap.get(stName).size() > 0) {
                  String[] split = stName.split("[.]");
                  sb.append(BTemplateSideBar.lex.getText("templateSidebar.usedBy", new Object[]{split[0]}));
                  sb.append("\n");

                  for (String usedInTmpl : subtemplateMap.get(stName)) {
                     try {
                        BObject bObject = BOrd.make(usedInTmpl).get();
                        if (bObject instanceof BWbDeployableNtplFile) {
                           String[] split1 = ((BWbDeployableNtplFile)bObject).getFileName().split("[.]");
                           usedInTmpl = split1[0];
                        }
                     } catch (Exception var11) {
                     }

                     sb.append("             ").append(usedInTmpl).append("\n");
                  }
               }
            }

            if (sb.toString().isEmpty()) {
               sb.append(BTemplateSideBar.lex.getText("templateSideBar.notSubtemplate"));
            }

            BDialog.message(this.getOwner(), BTemplateSideBar.lex.getText("templateSidebar.usage"), sb.toString());
         } catch (Exception var12) {
            var12.printStackTrace();
         }

         return null;
      }
   }

   class MakeModule extends Command {
      public MakeModule(BWidget owner) {
         super(owner, BTemplateSideBar.lex, "commands.makeModule");
      }

      public CommandArtifact doInvoke() {
         try {
            Collection<BWbDeployableNtplFile> selectedTemplates = BTemplateSideBar.collectTemplates(BTemplateSideBar.this.tree.getSelectedObjects());
            if (selectedTemplates.isEmpty()) {
               BDialog.info(
                  this.getOwner(),
                  BTemplateSideBar.lex.getText("templateSideBar.makeModule"),
                  BString.make(BTemplateSideBar.lex.getText("templateSideBar.makeModule.noFilesSelected"))
               );
               return null;
            }

            BComponent cmp = new BComponent();
            cmp.add("moduleName", BString.make("myModule"));
            cmp.add("description", BString.make(""));
            cmp.add("vendor", BString.make(""));
            cmp.add("vendorVersion", BString.make("1.0"));
            BObject result = BWbFieldEditor.dialog(this.getOwner(), BTemplateSideBar.lex.getText("templateSideBar.moduleInfo.title"), cmp);
            if (result == null) {
               return null;
            }

            cmp = result.asComponent();
            String moduleName = ((BString)cmp.get("moduleName")).getString();
            String description = ((BString)cmp.get("description")).getString();
            String vendor = ((BString)cmp.get("vendor")).getString();
            String vendorVersion = ((BString)cmp.get("vendorVersion")).getString();
            if (!BTemplateSideBar.this.isValidModuleName(moduleName)) {
               return null;
            }

            String fileName = moduleName + ".jar";
            FilePath path = TemplateConst.TEMPLATE_MODULE_DIR;
            BDirectory modDir = BFileSystem.INSTANCE.makeDir(path);
            boolean fileExists = modDir.getNavChild(fileName) != null;
            if (fileExists) {
               Optional<DefaultModulesFileManager> fileManagerOptional = DefaultModulesFileManager.get();
               if (!fileManagerOptional.isPresent()) {
                  BDialog.error(
                     BTemplateSideBar.this.getWbShell(),
                     BTemplateSideBar.lex.getText("templateSideBar.makeModule.noFileManager.title"),
                     BTemplateSideBar.lex.getText("templateSideBar.makeModule.noFileManager.message")
                  );
                  return null;
               }

               DefaultModulesFileManager fm = fileManagerOptional.get();
               if (fm.findDependency(moduleName) != null) {
                  BDialog.error(
                     BTemplateSideBar.this.getWbShell(),
                     BTemplateSideBar.lex.getText("templateName.invalidNameTitle"),
                     BTemplateSideBar.lex.getText("templateSideBar.makeModule.moduleFile")
                  );
                  return null;
               }

               int rc = BDialog.confirm(
                  BTemplateSideBar.this.getWbShell(),
                  BTemplateSideBar.lex.getText("templateSideBar.makeModule.duplicate.title"),
                  BTemplateSideBar.lex.getText("templateSideBar.makeModule.duplicate")
               );
               if (rc != 4) {
                  return null;
               }
            }

            BTemplateSideBar.makeTemplateModule(selectedTemplates, moduleName, description, vendor, vendorVersion, fileName);
            if (BTemplateSideBar.this.modModel != null) {
               if (fileExists) {
                  NavEvent event = NavEvent.makeReplaced(BTemplateSideBar.this.moduleOrd, fileName, null);
                  BTemplateSideBar.this.modModel.replaced(event);
               } else {
                  NavEvent event = NavEvent.makeAdded(BTemplateSideBar.this.moduleOrd, fileName, null);
                  BTemplateSideBar.this.modModel.added(event);
               }
            }

            BTemplateSideBar.this.notifySuccess(fileName);
         } catch (Exception var15) {
            BTemplateSideBar.log.log(Level.WARNING, "Error creating module:" + var15.getLocalizedMessage(), (Throwable)var15);
         }

         return null;
      }
   }

   static class NavListModel extends DefaultListModel {
      public BImage getItemIcon(int index) {
         BTemplateSideBar.NavWrapper w = (BTemplateSideBar.NavWrapper)this.getItem(index);
         return BImage.make(w.bookmark.getIcon());
      }
   }

   static class NavWrapper {
      public BBookmark bookmark;
      public boolean isModule;

      public NavWrapper(BBookmark bookmark, boolean isModule) {
         this.bookmark = bookmark;
         this.isModule = isModule;
      }

      @Override
      public String toString() {
         return this.bookmark.getText();
      }
   }

   static class TmplNavTreeNode extends NavTreeNode {
      public TmplNavTreeNode(NavTreeModel model, NavTreeNode parent, BINavNode navNode) {
         super(model, parent, navNode);
      }

      public String getText() {
         BINavNode navNode = this.getNavNode();
         if (navNode instanceof BModuleFile) {
            String nn = navNode.getNavName();
            return nn.substring(0, nn.indexOf("."));
         } else if (navNode instanceof BDirectory) {
            return navNode.getNavName();
         } else if (navNode instanceof BWbDeployableNtplFile) {
            BWbDeployableNtplFile file = (BWbDeployableNtplFile)this.getNavNode();
            String title = file.getTemplateManifest().title;
            String version = file.getTemplateManifest().version;
            if (title != null && title.length() != 0) {
               return title + '-' + version;
            } else {
               String t = super.getText();
               return this.getFileBase(t);
            }
         } else {
            return "";
         }
      }

      private String getFileBase(String fileName) {
         return fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
      }

      public int getChildCount() {
         return this.getChildren().length;
      }

      public TreeNode getChild(int index) {
         return this.getChildren()[index];
      }

      public NavTreeNode[] getChildren() {
         if (this.children == null) {
            BWidgetShell shell = this.getShell();
            if (shell != null) {
               shell.enterBusy();
            }

            try {
               this.children = this.buildChildren();
            } catch (Throwable var6) {
               this.children = new NavTreeNode[0];
               BTemplateSideBar.log.log(Level.WARNING, "Cannot expand tree:" + var6.getLocalizedMessage(), var6);
               BDialog.error(this.getTree(), BDialog.TITLE_ERROR, BTemplateSideBar.lex.getText("templateSideBar.cannotExpandTree"), var6);
            } finally {
               if (shell != null) {
                  shell.exitBusy();
               }
            }
         }

         return this.children;
      }
   }

   public class TmplSideBarModel extends DefaultNavTreeModel {
      private final BTemplateSideBar.TmplNavTreeNode root;

      public TmplSideBarModel(BINavNode node) {
         super(node);
         this.root = new BTemplateSideBar.TmplNavTreeNode(this, null, node);
      }

      protected void added(NavEvent event) {
         super.added(event);
      }

      protected void replaced(NavEvent event) {
         if (!event.getFacets().getb("close", false)) {
            super.replaced(event);
         }
      }

      protected void removed(NavEvent event) {
         BTemplateSideBar.NavWrapper w = (BTemplateSideBar.NavWrapper)BTemplateSideBar.this.selTree.getSelectedItem();
         BOrd activeOrd = w.bookmark.getBookmarkOrd();
         if (WbMain.isRemoved(event, activeOrd)) {
            BTemplateSideBar.this.displayError(w.bookmark.getText(), new Exception("Parent node removed"));
         } else {
            TreeNode[] tnodA = BTemplateSideBar.this.tree.getSelection().getNodes();

            for (TreeNode tnod : tnodA) {
               if (((BTemplateSideBar.TmplNavTreeNode)tnod).getNavNode().getNavName().equals(event.getOldChildName())) {
                  BTemplateSideBar.this.tree.getSelection().deselect(tnod);
               }
            }

            super.removed(event);
         }
      }

      public int getRootCount() {
         return this.root.getChildCount();
      }

      public TreeNode getRoot(int index) {
         return this.root.getChild(index);
      }

      public NavTreeNode makeNavTreeNode(NavTreeNode parent, BINavNode navNode) {
         if (navNode instanceof BIFile && navNode.getNavName().endsWith(".lock")) {
            return null;
         } else if (navNode.getNavName().endsWith(".ntpl")) {
            return new BTemplateSideBar.TmplNavTreeNode(this, parent, navNode);
         } else {
            try {
               if (navNode instanceof BModuleFile) {
                  BINavNode[] navKids = navNode.getNavChildren();
                  ((BModuleFile)navNode).close();
                  if (BTemplateSideBar.navNodesContainTemplate(navKids)) {
                     return new BTemplateSideBar.TmplNavTreeNode(this, parent, navNode);
                  }
               } else if (navNode instanceof BDirectory && BTemplateSideBar.navNodeContainsTemplate(navNode)) {
                  return new BTemplateSideBar.TmplNavTreeNode(this, parent, navNode);
               }
            } catch (Throwable var4) {
            }

            return null;
         }
      }
   }

   class TplNavController extends NavTreeController {
      protected BMenu makePopup(NavTreeSubject subject) {
         BMenu menu = super.makePopup(subject);
         TreeNode[] nodes = subject.getNodes();
         boolean modfiles = false;
         boolean tplFiles = false;
         boolean tplInMod = false;

         for (TreeNode node : nodes) {
            NavTreeNode navNode = (NavTreeNode)node;
            if (navNode.getNavNode() instanceof BModuleFile) {
               modfiles = true;
            } else {
               if (!(navNode.getNavNode() instanceof BWbDeployableNtplFile)) {
                  return null;
               }

               tplFiles = true;
               if (((NavTreeNode)navNode.getParent()).getNavNode() instanceof BModuleFile) {
                  tplInMod = true;
               }
            }
         }

         if (modfiles && tplFiles) {
            return null;
         } else {
            BTemplateSideBar.this.remove(menu, "cut");
            BTemplateSideBar.this.remove(menu, "paste");
            BTemplateSideBar.this.remove(menu, "duplicate");
            BTemplateSideBar.this.remove(menu, "rename");
            BTemplateSideBar.this.remove(menu, "exportConfigs");
            if (modfiles || nodes.length > 1) {
               BTemplateSideBar.this.remove(menu, "copy");
            }

            if (tplInMod) {
               BTemplateSideBar.this.remove(menu, "delete");
               menu.addItem("exportConfigs", new ExportConfigsCommand(BTemplateSideBar.this.tree));
            }

            if (tplFiles && !tplInMod) {
               menu.addSeparator();
               menu.addItem("makeTemplateModule", BTemplateSideBar.this.new MakeModule(BTemplateSideBar.this.tree));
               menu.addItem("findUsages", BTemplateSideBar.this.new FindUsages(BTemplateSideBar.this.tree));
               menu.addItem("exportConfigs", new ExportConfigsCommand(BTemplateSideBar.this.tree));
            }

            return menu;
         }
      }
   }
}
