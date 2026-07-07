package com.tridium.template.ui;

import com.tridium.install.BDependency;
import com.tridium.install.BVersion;
import com.tridium.tagdictionary.util.TagDictionaryUtil;
import com.tridium.tagdictionary.util.TagDictionaryUtil.ComponentTagGroupChoices;
import com.tridium.template.BPasswordBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateState;
import com.tridium.template.TemplateConst;
import com.tridium.template.UpgradeUtil;
import com.tridium.template.api.TemplateType;
import com.tridium.template.application.ApplicationTemplateUtil;
import com.tridium.template.file.BINtplFile;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.DependencyUtil;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.manifest.TemplateManifest.Resource;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.template.ui.sidebar.BTemplateSideBar;
import com.tridium.ui.BOptionDialog;
import com.tridium.ui.theme.Theme;
import com.tridium.ui.util.LabelUtil;
import com.tridium.util.CompUtil;
import com.tridium.util.ObjectUtil;
import com.tridium.util.ObjectUtil.NameContainer;
import com.tridium.workbench.console.BConsole.HyperlinkInterceptor;
import com.tridium.workbench.fieldeditors.BFrozenEnumFE;
import com.tridium.workbench.file.BSubdirectoryDropDown;
import com.tridium.workbench.shell.BErrorPanel;
import com.tridium.workbench.shell.BNiagaraWbShell;
import com.tridium.workbench.util.BEditTagDialog;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.types.image.BPngFile;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.license.Feature;
import javax.baja.license.FeatureNotLicensedException;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.Array;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BMarker;
import javax.baja.sys.BObject;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.CopyHints;
import javax.baja.sys.Property;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.TagDictionaryService;
import javax.baja.tag.TagInfo;
import javax.baja.tagdictionary.BTagDictionaryService;
import javax.baja.tagdictionary.BTagGroupInfo;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextDropDown;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BButtonStyle;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BWidgetEvent;
import javax.baja.ui.event.WidgetSubscriber;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.menu.BIMenu;
import javax.baja.ui.menu.BIMenuBar;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BFlowPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.pane.BTabbedPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;
import javax.baja.util.LexiconText;
import javax.baja.util.Version;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;
import javax.baja.workbench.sidebar.BIWbSideBar;
import javax.baja.workbench.view.BWbComponentView;
import javax.baja.workbench.view.BWbView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"template:NtplFile", "template:NewNtplFromTemporary"},
      requiredPermissions = "r"
   )}
)
@NiagaraActions({@NiagaraAction(
      name = "templateModified"
   ), @NiagaraAction(
      name = "save"
   ), @NiagaraAction(
      name = "duplicate"
   ), @NiagaraAction(
      name = "cancel"
   )})
public class BTemplateView extends BWbComponentView implements HyperlinkInterceptor, TemplateConst {
   public static final Action templateModified = newAction(0, null);
   public static final Action save = newAction(0, null);
   public static final Action duplicate = newAction(0, null);
   public static final Action cancel = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BTemplateView.class);
   BEdgePane iconPan;
   static BImage openIcon = BImage.make("module://icons/x16/open.png");
   static ArrayList<BIMenu> menuList = new ArrayList<>();
   private static final BImage infoIcon = BImage.make("module://icons/x16/info.png");
   private static final BImage errorIcon = BImage.make("module://icons/x16/error.png");
   private static final String NO_ICON = "NO ICON SELECTED";
   private static final Lexicon lex = Lexicon.make("template");
   private static final Logger log = Logger.getLogger("ntpl");
   private BINtplFile ntplFile;
   private TemplateType templateType;
   private BComponent templateComp;
   private Property tcfgProp;
   private BTemplateConfig templateConfig;
   private TemplateManifest manifest;
   private BDataFile[] stationFiles = null;
   private String templateName;
   private String folderName;
   private BLabel banner;
   private boolean newTemplate = false;
   private boolean initModified = false;
   private boolean checkIo = false;
   private HashSet<String> outOfDateSubtemplates = null;
   private final ArrayList<String> dynamicPasswordDetected = new ArrayList<>();
   private boolean uuidAssigned = false;
   private boolean titleAssigned = false;
   boolean readOnly = true;
   BTabbedPane tp;
   BTemplateView.TabbedPaneSubscriber tabbedPaneSubscriber;
   BTemplateView.PropertyChangedSubscriber propertyChangedSubscriber;
   private BGridPane infoGridPane;
   private BGridPane nameGridPane;
   private BGridPane folderGridPane;
   private BGridPane versionGridPane;
   private BGridPane titleGridPane;
   private BLabel invalidName;
   private BLabel invalidFolder;
   private BLabel invalidVersion;
   private BLabel titleRequired;
   private BTextField nameField;
   private BSubdirectoryDropDown folderField;
   private BTextField titleField;
   private BTextField vendorField;
   private BFrozenEnumFE stateField;
   private BTextField versionField;
   private BTextField descriptionField;
   private BTextEditorPane infoField;
   private BPngFile pngFile = null;
   BOrd iconSrcOrd = null;
   BOrd imageOrd = null;
   private BLabel iconLabel;
   BTemplateBogEditor bogPane;
   BWidget infoPane;
   BTemplateConfigEditor settingsPane;
   BTemplateRelationEditor relationPane;
   BTemplateIOEditor ioPane;
   BTemplatePxEditor graphicsPane;
   BTemplateManager templateMgr;
   private BButton btnSave;
   private BButton btnDuplicate;
   private BButton btnCancel;
   private BPane buttonPane;

   public void templateModified() {
      this.invoke(templateModified, null, null);
   }

   public void save() {
      this.invoke(save, null, null);
   }

   public void duplicate() {
      this.invoke(duplicate, null, null);
   }

   public void cancel() {
      this.invoke(cancel, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void deactivated() {
      if (this.ntplFile != null) {
         TemplateViewState.save(this);
         if (this.bogPane != null) {
            this.bogPane.deactivated();
         }

         BIMenuBar w = (BIMenuBar)this.getShell().getMenuBar();

         for (BIMenu biMenu : menuList) {
            w.removeMenu(biMenu);
         }

         menuList.clear();
         if (!this.readOnly && this.ntplFile.isOpen()) {
            this.ntplFile.close();
         }
      }
   }

   public void prime() {
      if (this.newTemplate || this.initModified) {
         this.doTemplateModified();
      }
   }

   public void doLoadValue(BObject value, Context cx) {
      try {
         Feature feature = Sys.getLicenseManager().getFeature("tridium", "template");
         feature.check();
         this.ntplFile = (BINtplFile)value;
         this.readOnly = this.ntplFile.isReadOnly();
         this.setReadonly(this.readOnly);
         this.init();
         BEdgePane ep = new BEdgePane();
         this.banner = new BLabel(this.getBannerText(), BHalign.left);
         BFont fnt = Theme.label().getTextFont(this.banner);
         this.banner.setFont(BFont.make(fnt.getName(), fnt.getSize() * 1.1, 1));
         BEdgePane bp = new BEdgePane();
         bp.setCenter(this.banner);
         if (this.readOnly) {
            BLabel ro = new BLabel("ReadOnly", BFont.make(fnt.getName(), fnt.getSize() * 1.1, 1));
            ro.setForeground(BBrush.makeSolid(BColor.red));
            bp.setLeft(new BBorderPane(ro, 0.0, 10.0, 0.0, 0.0));
         }

         ep.setTop(new BBorderPane(bp, 5.0, 0.0, 7.0, 2.0));
         this.tp = new BTabbedPane();
         ep.setCenter(new BBorderPane(this.tp, 0.0, 0.0, 10.0, 0.0));
         this.buttonPane = this.buildButtonPane();
         ep.setBottom(this.buttonPane);
         this.infoPane = this.buildInfoPane();
         this.tp
            .addPane(
               new BLabel(BImage.make(BIcon.make("module://icons/x16/files/ntpl.png")), lex.getText("template.infoPane.title")),
               new BBorderPane(this.infoPane, 4.0, 4.0, 4.0, 4.0)
            );
         this.bogPane = new BTemplateBogEditor(this);
         this.tp
            .addPane(
               new BLabel(BImage.make(BIcon.std("compass.png")), lex.getText("template.bogPane.title")), new BBorderPane(this.bogPane, 0.0, 0.0, 0.0, 0.0)
            );
         this.settingsPane = new BTemplateConfigEditor(this, this.templateComp, this.templateConfig);
         this.tp
            .addPane(
               new BLabel(BImage.make(BIcon.std("propertySheet.png")), lex.getText("template.settingsPane.title")),
               new BBorderPane(this.settingsPane, 2.0, 2.0, 2.0, 2.0)
            );
         if (this.templateType != TemplateType.STATION && this.templateType != TemplateType.APPLICATION) {
            this.relationPane = new BTemplateRelationEditor(this, this.templateConfig);
            this.tp
               .addPane(
                  new BLabel(BImage.make(BIcon.std("match.png")), lex.getText("template.relatePane.title")),
                  new BBorderPane(this.relationPane, 4.0, 4.0, 4.0, 4.0)
               );
            this.ioPane = new BTemplateIOEditor(this, this.templateComp);
            this.tp
               .addPane(new BLabel(BImage.make(BIcon.std("match.png")), lex.getText("template.ioPane.title")), new BBorderPane(this.ioPane, 4.0, 4.0, 4.0, 4.0));
         } else {
            this.stationFiles = this.ntplFile.getStationFiles();
         }

         this.graphicsPane = new BTemplatePxEditor(this, this.templateComp, this.ntplFile.getPxFiles(), this.ntplFile.getPxImageFiles());
         this.tp
            .addPane(
               new BLabel(BImage.make(BIcon.std("view.png")), lex.getText("template.graphicsPane.title")),
               new BBorderPane(this.graphicsPane, 2.0, 2.0, 2.0, 2.0)
            );
         this.templateMgr = new BTemplateManager();
         BLabel templateMgrTabLabel = new BLabel(BImage.make(BIcon.std("files/ntpl.png")), lex.getText("template.templateMgr.title"));
         this.tp.addPane(templateMgrTabLabel, new BBorderPane(this.templateMgr, 2.0, 2.0, 2.0, 2.0));
         this.setContent(ep);
         this.tp.selectPane(this.infoPane.getParentWidget());
         if (!this.newTemplate && !this.initModified) {
            TemplateViewState.restore(this);
         }

         this.bogPane.doLoadValue(this.templateComp);
         this.templateMgr.setReadonly(this.readOnly);
         this.templateMgr.doLoadValue(this.getTemplateRoot(), cx);
         if (!this.readOnly) {
            this.outOfDateSubtemplates = this.templateMgr.hasOutOfDateTemplate();
            if (this.outOfDateSubtemplates != null && this.outOfDateSubtemplates.size() > 0) {
               BTemplateManager.UpgradeAll upgradeAllCmd = this.templateMgr.upgradeAllCmd;
               BTemplateManager.Commit commitCmd = this.templateMgr.commitCmd;
               if (upgradeAllCmd != null && commitCmd != null) {
                  upgradeAllCmd.doInvoke();
                  commitCmd.doInvoke(true);
                  this.templateMgr.doLoadValue(this.getTemplateRoot(), cx);
                  BLabel info = new BLabel(lex.getText("template.infoPane.outOfDateSubtemplate.message"));
                  this.addToInfoPane(infoIcon, info, BColor.red);
                  templateMgrTabLabel.setForeground(BBrush.makeSolid(BColor.red));
                  this.setInitModified(true);
                  this.templateMgr.setUpdatedStatus(this.outOfDateSubtemplates);
               }
            }
         }

         try {
            List<ComponentTagGroupChoices> tagGroupChoices = TagDictionaryUtil.listPotentialTagGroupsFromTags(this.templateComp);
            if (!tagGroupChoices.isEmpty()) {
               StringBuilder sb = new StringBuilder();
               sb.append(lex.get("template.infoPane.tagsToTagGroup"));
               BComponent currentComp = null;

               for (ComponentTagGroupChoices choice : tagGroupChoices) {
                  BComponent comp = choice.getComponent();
                  if (!comp.equals(currentComp)) {
                     sb.append("\r     ");
                     sb.append(comp.toPathString());
                     currentComp = comp;
                  }
               }

               sb.append("\r");
               sb.append(lex.get("template.infoPane.tagsToTagGroup.save"));
               BLabel info = new BLabel(sb.toString());
               this.addToInfoPane(infoIcon, info, BColor.blue);
               this.infoGridPane.add(null, new BLabel());
               BButton chooseButton = new BButton(lex.getText("template.infoPane.tagsToTagGroup.choose"));
               this.infoGridPane.add(null, chooseButton);
               this.linkTo(chooseButton, BButton.actionPerformed, save);
            }
         } catch (Exception var14) {
            if (log.isLoggable(Level.FINE)) {
               log.log(Level.WARNING, "ListTagGroupChoices threw an exception: " + var14, (Throwable)var14);
            } else {
               log.warning("ListTagGroupChoices threw an exception: " + var14);
            }
         }

         BWidget content = this.tp.getSelectedLabelPane().getContent();
         this.swapActiveView(this.getWbView(content));
         this.tabbedPaneSubscriber = new BTemplateView.TabbedPaneSubscriber();
         this.tabbedPaneSubscriber.subscribe(this.tp);
         this.propertyChangedSubscriber = new BTemplateView.PropertyChangedSubscriber();
         this.propertyChangedSubscriber.subscribe(this.templateComp, Integer.MAX_VALUE);
         this.btnSave.setEnabled(!this.readOnly && this.initModified);
      } catch (FeatureNotLicensedException var15) {
         String errorMessage = lex.getText("templateView.featureNotLicensed");
         BOrd ord = value instanceof BNtplFile ? ((BNtplFile)value).getAbsoluteOrd() : null;
         BPane epx = new BErrorPanel(this.getWbShell(), errorMessage, ord, var15);
         this.setContent(epx);
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.WARNING, errorMessage, (Throwable)var15);
         } else {
            log.warning(errorMessage);
         }
      } catch (Exception var16) {
         String exceptionMessage = var16.getCause() != null ? var16.getCause().getLocalizedMessage() : var16.getLocalizedMessage();
         String errorDisplayMessage = lex.getText("templateView.loadException", new Object[]{exceptionMessage});
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.WARNING, errorDisplayMessage, (Throwable)var16);
         } else {
            log.warning(errorDisplayMessage);
         }

         BOrd ordx = value instanceof BNtplFile ? ((BNtplFile)value).getNavOrd() : null;
         BPane epxx = new BErrorPanel(this.getWbShell(), errorDisplayMessage, ordx, var16);
         this.setContent(epxx);
      }
   }

   public void setInitModified(boolean value) {
      this.initModified = value;
   }

   public void doTemplateModified() {
      if (!this.readOnly) {
         this.setModified();
         this.invalidName.setVisible(!this.checkValidNameEntry());
         this.invalidFolder.setVisible(!this.checkValidFolderEntry());
         this.btnSave.setEnabled(this.checkValidPath());
         this.banner.setText(this.getBannerText());
         this.titleRequired.setVisible(this.titleField.getText().length() == 0);
         this.checkChangeState();
      }
   }

   private void checkChangeState() {
      try {
         if (this.manifest.state.equals(BTemplateState.ready) && this.stateField.saveValue().equals(BTemplateState.ready)) {
            this.stateField.loadValue(BTemplateState.draft);
            this.versionField.setEditable(false);
            this.invalidVersion.setVisible(false);
         } else if (this.stateField.saveValue().equals(BTemplateState.ready)) {
            this.versionField.setEditable(true);
            this.invalidVersion.setVisible(true);
         } else {
            this.versionField.setEditable(false);
            this.invalidVersion.setVisible(false);
         }
      } catch (Exception var2) {
         log.log(Level.WARNING, "Error checking template change state:" + var2.getLocalizedMessage(), (Throwable)var2);
      }
   }

   private String getBannerText() {
      if (this.templateType == TemplateType.APPLICATION) {
         return lex.getText("templateBanner.application", new Object[]{this.templateName, this.manifest.vendor, this.manifest.version});
      } else {
         return this.templateType == TemplateType.STATION
            ? lex.getText("templateBanner.station", new Object[]{this.templateName, this.manifest.vendor, this.manifest.version})
            : lex.getText("templateBanner.component", new Object[]{this.templateName, this.manifest.vendor, this.manifest.version});
      }
   }

   public boolean isApplicationTemplate() {
      return this.templateType == TemplateType.APPLICATION;
   }

   public BComponent getTemplateRoot() {
      return this.templateComp;
   }

   private void init() throws Exception {
      this.templateComp = this.ntplFile.getBaseComponent();
      this.manifest = this.ntplFile.getTemplateManifest();
      this.newTemplate = this.ntplFile.isNew();
      BImageFile image = this.ntplFile.getImageFile();
      if (image instanceof BPngFile) {
         this.pngFile = (BPngFile)image;
         this.imageOrd = image.getAbsoluteOrd();
      }

      this.templateConfig = BTemplateConfig.getOrCreateConfigForRoot(this.templateComp, this.manifest.isApplication);
      this.tcfgProp = this.templateConfig == null ? null : this.templateConfig.getPropertyInParent();
      if (this.templateConfig != null && this.tcfgProp != null) {
         this.templateType = NtplUtil.getTemplateType(this.templateComp, this.templateConfig);
         if (this.newTemplate) {
            String templateNameCandidate = SlotPath.unescape(this.ntplFile.getTemplateName());
            BDirectory templates = NtplUtil.getTemplateDirectory();
            this.templateName = this.getUniqueName(templateNameCandidate, templates);
            this.folderName = "";
            this.manifest.title = this.templateName;
            int count = this.templateComp.getLinks().length;
            count += this.templateComp.getKnobCount();
            this.checkIo = count > 0;
            BPasswordBinding[] passwordBindings = this.templateConfig.getPasswordBindings();

            for (BPasswordBinding binding : passwordBindings) {
               if (binding.getIsDynamic()) {
                  String s = " * " + binding.getPswOrd().toString();
                  this.dynamicPasswordDetected.add(s + '.' + binding.getPswSlot());
                  this.templateConfig.remove(binding);
               }
            }

            this.manifest.state = BTemplateState.DEFAULT;
            this.manifest.uID = BUuid.make();
         } else {
            FilePath templateFilePath = this.ntplFile.getFilePath();
            this.templateName = NtplUtil.templateNameFromFilePath(templateFilePath, this.templateType);
            if (this.manifest.title.isEmpty()) {
               this.manifest.title = this.templateName;
               this.titleAssigned = true;
            }

            this.folderName = NtplUtil.templateFolderFromFilePath(templateFilePath, this.templateType);
            if (this.templateConfig.getUID().equals(BUuid.DEFAULT)) {
               this.templateConfig.setUID(BUuid.make());
               this.manifest.uID = this.templateConfig.getUID();
               this.setInitModified(true);
               this.uuidAssigned = true;
            }
         }

         if (!this.templateConfig.getUID().equals(this.manifest.uID)) {
            this.manifest.uID = this.templateConfig.getUID();
         }
      } else {
         throw new Exception(lex.getText("templateView.cannotFindTemplateConfig"));
      }
   }

   public void handleComponentEvent(BComponentEvent e) {
      super.handleComponentEvent(e);
      switch (e.getId()) {
         case 5:
            return;
         default:
            this.bogPane.handleComponentEvent(e);
            this.doTemplateModified();
      }
   }

   private BPane buildButtonPane() {
      BFlowPane pane = new BFlowPane();
      pane.setAlign(BHalign.center);
      pane.add(null, this.makeSaveButton());
      pane.add(null, this.makeDuplicateButton());
      if (this.newTemplate) {
         pane.add(null, this.makeCancelButton());
      }

      return pane;
   }

   private BPane buildInfoPane() throws Exception {
      this.infoGridPane = new BGridPane(2);
      this.nameGridPane = new BGridPane(2);
      this.folderGridPane = new BGridPane(2);
      this.titleGridPane = new BGridPane(2);
      this.versionGridPane = new BGridPane(3);
      this.infoGridPane.setColumnGap(10.0);
      this.infoGridPane.setRowGap(6.0);
      this.infoGridPane.setHalign(BHalign.left);
      this.invalidName = new BLabel(lex.getText("templateView.invalidTemplateName"));
      this.invalidName.setForeground(BBrush.makeSolid(BColor.red));
      this.invalidName.setVisible(false);
      this.invalidFolder = new BLabel(lex.getText("templateView.invalidTemplateFolder"));
      this.invalidFolder.setForeground(BBrush.makeSolid(BColor.red));
      this.invalidFolder.setVisible(false);
      this.invalidVersion = new BLabel(lex.getText("templateView.versionChangeRequired", new Object[]{this.manifest.version}));
      this.invalidVersion.setForeground(BBrush.makeSolid(BColor.red));
      this.invalidVersion.setVisible(false);
      this.titleRequired = new BLabel(lex.getText("templateView.titleRequired"));
      this.titleRequired.setForeground(BBrush.makeSolid(BColor.red));
      this.titleRequired.setVisible(false);
      this.nameField = new BTextField(this.templateName, 25, this.newTemplate);
      this.nameGridPane.add(null, this.nameField);
      this.nameGridPane.add(null, this.invalidName);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.templateName"), this.nameGridPane);
      this.folderField = new BSubdirectoryDropDown(NtplUtil.makeTemplateDirectoryOrd(this.templateType), this.folderName, 25, this.newTemplate);
      this.folderGridPane.add(null, this.folderField);
      this.folderGridPane.add(null, this.invalidFolder);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.folderName"), this.folderGridPane);
      this.titleField = new BTextField(this.manifest.title, 25, !this.readOnly);
      this.titleGridPane.add(null, this.titleField);
      this.titleGridPane.add(null, this.titleRequired);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.templateTitle"), this.titleGridPane);
      this.vendorField = new BTextField(this.manifest.vendor, 25, !this.readOnly);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.vendor"), this.vendorField);
      this.versionField = new BTextField(this.manifest.version, 25, false);
      this.versionGridPane.add(null, this.versionField);
      this.versionGridPane.add(null, this.invalidVersion);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.version"), this.versionGridPane);
      this.stateField = new BFrozenEnumFE();
      this.stateField.loadValue(this.manifest.state);
      this.stateField.setReadonly(this.readOnly);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.state"), this.stateField);
      this.descriptionField = new BTextField(this.manifest.description, 25, !this.readOnly);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.description"), this.descriptionField);
      this.infoField = new BTextEditorPane(this.manifest.info, 5, 60, !this.readOnly);
      LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.info"), this.infoField);
      Resource res = this.manifest.getResource("image.png", "png");
      String s = "NO ICON SELECTED";
      if (res != null) {
         s = res.sourceOrd;
         this.iconSrcOrd = BOrd.make(s);
      } else {
         this.iconSrcOrd = null;
      }

      BTextField iconField = new BTextField(s, 40, false);
      if (this.getShell() != null) {
         BButton iconSel = new BButton(new BTemplateView.BrowseIcons(this.getShell(), iconField));
         iconSel.setButtonStyle(BButtonStyle.toolBar);
         iconSel.setVisible(!this.readOnly);
         this.iconLabel = new BLabel();
         this.iconPan = new BEdgePane();
         this.iconPan.setCenter(iconField);
         this.iconPan.setRight(iconSel);
         if (this.pngFile != null) {
            this.setIcon();
         }

         LabelUtil.addLabelWidgetPair(this.infoGridPane, LexiconText.make(TYPE, "templateView.icon"), this.iconPan);
      }

      this.invalidName.setVisible(!this.checkValidNameEntry());
      this.invalidFolder.setVisible(!this.checkValidFolderEntry());
      if (this.readOnly) {
         BLabel info = new BLabel(lex.getText("template.infoPane.readonly.message"));
         this.addToInfoPane(infoIcon, info, BColor.red);
         if (this.uuidAssigned) {
            BLabel info1 = new BLabel(lex.getText("template.infoPane.uuidAssigned.readonly.message"));
            this.addToInfoPane(infoIcon, info1, BColor.red);
            this.uuidAssigned = false;
         }
      } else {
         if (this.newTemplate && this.templateType == TemplateType.DEVICE) {
            BLabel info = new BLabel(lex.getText("template.infoPane.newDeviceTemplate.message"));
            this.addToInfoPane(infoIcon, info, BColor.red);
         }

         try {
            this.templateConfig.checkForValidTemplate();
         } catch (Exception var7) {
            BLabel info = new BLabel(var7.getLocalizedMessage());
            this.addToInfoPane(errorIcon, info, BColor.red);
         }

         if (this.uuidAssigned) {
            BLabel info = new BLabel(lex.getText("template.infoPane.uuidAssigned.message"));
            this.addToInfoPane(infoIcon, info, BColor.red);
            this.uuidAssigned = false;
         }

         if (this.titleAssigned) {
            BLabel info = new BLabel(lex.getText("template.infoPane.titleAssigned.message"));
            this.addToInfoPane(infoIcon, info, BColor.red);
            this.titleAssigned = false;
         }

         if (this.checkIo) {
            BLabel info = new BLabel(lex.getText("template.infoPane.checkIO.message"));
            this.addToInfoPane(infoIcon, info, BColor.red);
            this.checkIo = false;
         }

         if (this.outOfDateSubtemplates != null && this.outOfDateSubtemplates.size() > 0) {
            BLabel info = new BLabel(lex.getText("template.infoPane.outOfDateSubtemplate.message"));
            this.addToInfoPane(infoIcon, info, BColor.red);
         }

         if (this.templateConfig.getPasswordBindings().length > 0) {
            BLabel info = new BLabel(lex.getText("template.infoPane.checkConfig.message"));
            this.addToInfoPane(infoIcon, info, BColor.black);
         }

         if (!this.dynamicPasswordDetected.isEmpty()) {
            StringBuilder list = new StringBuilder();

            for (String s1 : this.dynamicPasswordDetected) {
               list.append("\n").append(s1);
            }

            BLabel info = new BLabel(lex.getText("template.infoPane.dynamicPsw.message") + list);
            this.addToInfoPane(infoIcon, info, BColor.red);
            this.dynamicPasswordDetected.clear();
         }
      }

      BEdgePane ip = new BEdgePane();
      ip.setTop(new BBorderPane(this.infoGridPane, 10.0, 10.0, 10.0, 10.0));
      this.linkTo(this.nameField, BTextField.textModified, templateModified);
      this.linkTo(this.folderField, BTextDropDown.valueModified, templateModified);
      this.linkTo(this.vendorField, BTextField.textModified, templateModified);
      this.linkTo(this.titleField, BTextField.textModified, templateModified);
      this.linkTo(this.versionField, BTextField.textModified, templateModified);
      this.linkTo(this.descriptionField, BTextField.textModified, templateModified);
      this.linkTo(this.infoField.getEditor(), BTextEditor.textModified, templateModified);
      this.linkTo(this.stateField, BFrozenEnumFE.pluginModified, templateModified);
      return ip;
   }

   private boolean checkValidPath() {
      return TemplateUiUtil.isValidTemplatePath(this.folderField.getText(), this.nameField.getText());
   }

   private boolean checkValidNameEntry() {
      return TemplateUiUtil.isValidTemplateNameEntry(this.nameField.getText());
   }

   private boolean checkValidFolderEntry() {
      return TemplateUiUtil.isValidTemplateFolder(this.folderField.getText());
   }

   private void setIcon() {
      if (this.imageOrd != null) {
         BImage iconImage = BImage.make(this.imageOrd);
         this.iconLabel.setImage(iconImage);
         this.banner.setImage(iconImage);
         this.iconPan.setLeft(this.iconLabel);
      } else {
         this.iconLabel.setImage(BImage.NULL);
         this.banner.setImage(BImage.NULL);
         this.iconPan.setLeft(new BNullWidget());
      }
   }

   public void addToInfoPane(BWidget widget) {
      this.infoGridPane.add(null, new BLabel(errorIcon));
      this.infoGridPane.add(null, widget);
   }

   public void addToInfoPane(BImage icon, BLabel label, BColor color) {
      this.infoGridPane.add(null, new BLabel(icon));
      label.setWordWrapEnabled(true);
      label.setHalign(BHalign.left);
      label.setForeground(BBrush.makeSolid(color));
      this.infoGridPane.add(null, label);
   }

   public boolean consoleHyperlink(File file, int line1, int col1, int line2, int col2) {
      return this.bogPane.consoleHyperlink(file, line1, col1, line2, col2);
   }

   public void doSave() {
      String nameEntry = this.nameField.getText();
      String folderEntry = this.folderField.getText();
      if (!this.isValidPathAndName(folderEntry, nameEntry)) {
         log.fine("doSave: invalid template or folder name " + TemplateUiUtil.templateFolderFromEntries(folderEntry, nameEntry));
      } else {
         if (this.newTemplate) {
            FilePath outFilePath = NtplUtil.buildTemplateFilePath(TemplateUiUtil.templateRelativePathFromEntries(folderEntry, nameEntry), this.templateType);
            File targetFile = BFileSystem.INSTANCE.pathToLocalFile(outFilePath);
            if (targetFile.exists()) {
               int proceed = BDialog.confirm(
                  this.getWbShell(),
                  lex.getText("commands.duplicate.dup.title"),
                  lex.getText("commands.duplicate.dup.message") + "\n\n" + outFilePath.getBody()
               );
               if (proceed != 4) {
                  return;
               }
            }
         }

         log.fine("doSave: template=" + TemplateUiUtil.templateRelativePathFromEntries(folderEntry, nameEntry));
         if (this.titleField.getText().length() > 0 && this.verifyVersionChange()) {
            if (this.prepForSave()) {
               FilePath savedFile = this.implSave(
                  TemplateUiUtil.templateRelativePathFromEntries(folderEntry, nameEntry),
                  this.titleField.getText(),
                  this.versionField.getText(),
                  this.templateComp,
                  this.ntplFile,
                  this.templateType,
                  this.newTemplate
               );
               Objects.requireNonNull(savedFile, "Unexpected error while saving the template");
               if (this.newTemplate) {
                  String msg = getSaveConfirmationMessage(this.templateType, savedFile);
                  BDialog.message(this.getWbShell(), lex.getText("templateCreated.title"), msg);
               }

               if (!this.readOnly && this.ntplFile.isOpen()) {
                  this.ntplFile.close();
               }

               this.clearModified();
               this.refreshTemplateSidebar();
               if (this.newTemplate) {
                  this.getWbShell().getBackCommand().invoke();
               } else {
                  this.getWbShell().getRefreshCommand().invoke();
               }
            }

            this.invalidVersion.setVisible(false);
         }
      }
   }

   protected BObject doSaveValue(BObject value, Context cx) throws Exception {
      this.doSave();
      return value;
   }

   public void doDuplicate() {
      BDuplicateTemplateInfo dupInfo = this.promptForDuplicateInfo(
         new BDuplicateTemplateInfo(
            this.nameField.getText() + "Dup", this.folderField.getText(), this.titleField.getText() + "Dup", this.versionField.getText(), this.templateType
         )
      );
      if (dupInfo != null) {
         boolean wasModified = this.isModified();
         if (this.prepForSave()) {
            CopyHints hints = new CopyHints();
            hints.defaultOnClone = false;
            hints.swizzleHandles = false;
            hints.keepHandles = true;
            BComponent spaceRoot = this.templateComp.getParent().newCopy(hints).asComponent();
            BComponentSpace dupSpace = new BComponentSpace("dup", this.templateComp.getSpace().getLexiconText(), BOrd.make("dup:"));
            dupSpace.setRootComponent(spaceRoot);
            BComponent saveComponent = spaceRoot.getChildComponents()[0];
            FilePath savedFile = this.implSave(
               TemplateUiUtil.templateRelativePathFromEntries(dupInfo.getFolderNameText(), dupInfo.getFileName()),
               dupInfo.getTitle(),
               dupInfo.getVersion(),
               saveComponent,
               this.ntplFile,
               this.templateType,
               true
            );
            Objects.requireNonNull(savedFile, "Unexpected error while duplicating the template");
            this.refreshTemplateSidebar();
            BDialog.message(this.getWbShell(), lex.getText("templateCreated.title"), getSaveConfirmationMessage(this.templateType, savedFile));
            this.clearModified();
            this.templateConfig.clearCacheValues();
            if (this.newTemplate) {
               this.getWbShell().getBackCommand().invoke();
            } else {
               this.getWbShell().getRefreshCommand().invoke();
               if (!wasModified) {
                  this.clearModified();
                  this.btnSave.setEnabled(false);
               }
            }
         } else if (!wasModified) {
            this.clearModified();
            this.btnSave.setEnabled(false);
         }
      }
   }

   private static String getSaveConfirmationMessage(TemplateType templateType, FilePath savedFile) {
      String msgPattern;
      if (templateType == TemplateType.APPLICATION) {
         msgPattern = lex.getText("applicationTemplateCreated.message");
      } else if (templateType == TemplateType.STATION) {
         msgPattern = lex.getText("stationTemplateCreated.message");
      } else {
         msgPattern = lex.getText("templateCreated.message");
      }

      return MessageFormat.format(msgPattern, savedFile.getBody());
   }

   private BDuplicateTemplateInfo promptForDuplicateInfo(BDuplicateTemplateInfo editComp) {
      BWbFieldEditor editor = BWbFieldEditor.makeFor(editComp);
      editor.loadValue(editComp);
      BGridPane gridPane = new BGridPane(1);
      gridPane.add(null, editor);
      BOptionDialog dlg = new BOptionDialog(this.getWbShell(), lex.getText("commands.duplicate.prompt.label"), gridPane, 3, null, null);
      dlg.computePreferredSize();
      dlg.setBoundsCenteredOnOwner();
      boolean validEntry = false;

      while (true) {
         while (true) {
            if (validEntry) {
               return editComp;
            }

            dlg.open();
            if (dlg.getResult() != 1) {
               return null;
            }

            try {
               editComp = (BDuplicateTemplateInfo)editor.saveValue();
               if (!this.isValidPathAndName(editComp.getFolderNameText(), editComp.getFileName())) {
                  editor.loadValue(editComp);
               } else if (this.isDuplicateFilename(editComp.getFolderNameText(), editComp.getFileName())) {
                  BDialog.error(
                     this.getWbShell(),
                     lex.getText("commands.duplicate.dup.title"),
                     lex.getText("commands.duplicate.dup.title") + " " + lex.getText("commands.duplicate.dup.newFilename")
                  );
                  editor.loadValue(editComp);
               } else {
                  if (this.isValidVersion(editComp.getVersion())) {
                     break;
                  }

                  BDialog.error(
                     this.getWbShell(),
                     lex.getText("commands.duplicate.version.invalid.title"),
                     lex.getText("commands.duplicate.version.invalid.message", new Object[]{editComp.getVersion()})
                  );
                  editor.loadValue(editComp);
               }
            } catch (Exception var7) {
               BDialog.error(this.getWbShell(), "Exception: " + var7.getLocalizedMessage());
            }
         }

         validEntry = true;
      }
   }

   public void doCancel() {
      if (this.newTemplate) {
         BLabel ruSure = new BLabel(lex.getText("commands.cancel.confirm"));
         double fontSize = BFont.DEFAULT.getSize() + 2.0;
         ruSure.setFont(BFont.make(BFont.DEFAULT, fontSize));
         int proceed = BDialog.confirm(this.getWbShell(), lex.getText("commands.cancel.label"), ruSure);
         if (proceed != 4) {
            return;
         }

         this.clearModified();
         this.getWbShell().getBackCommand().invoke();
      }

      if (!this.readOnly && this.ntplFile.isOpen()) {
         this.ntplFile.close();
      }
   }

   private boolean prepForSave() {
      BTagDictionaryService tdService = (BTagDictionaryService)this.templateComp.getTagDictionaryService();
      List<BComponent> entities = TagDictionaryUtil.getComponents(this.templateComp);
      List<BTagGroupInfo> tagGroups = TagDictionaryUtil.getTagGroups(tdService);
      ArrayList<ComponentTagGroupChoices> ctgChoices = new ArrayList<>();
      TagDictionaryUtil.listPotentialTagGroupsFromTags(entities, tagGroups, ctgChoices);
      int results = TmplUtil.selectTagGroupChoices(this, ctgChoices, lex);
      if (results == 1) {
         List<String> keepTags = new ArrayList<>();
         List<BTagGroupInfo> addTagGroup = new ArrayList<>();
         List<BTagGroupInfo> dontAddTagGroup = new ArrayList<>();

         for (int i = 0; i < ctgChoices.size(); i++) {
            ComponentTagGroupChoices ctgChoice = ctgChoices.get(i);
            ComponentTagGroupChoices nextCtgChoice = null;
            if (i + 1 < ctgChoices.size()) {
               nextCtgChoice = ctgChoices.get(i + 1);
            }

            BComponent thisComp = ctgChoice.getComponent();
            boolean isComplete = i + 1 == ctgChoices.size() || nextCtgChoice != null && !thisComp.equals(nextCtgChoice.getComponent());
            if (ctgChoice.isSelected()) {
               addTagGroup.add(ctgChoice.getTagGroupInfo());
            } else {
               dontAddTagGroup.add(ctgChoice.getTagGroupInfo());
               if (!ctgChoice.isRemoveTags()) {
                  for (TagInfo tagInfo : ctgChoice.getTagGroupInfo().getTagList()) {
                     keepTags.add(tagInfo.getTagId().getQName());
                  }
               }
            }

            if (isComplete) {
               BComponent currentComp = thisComp;

               for (BTagGroupInfo tagGroupInfo : addTagGroup) {
                  if (tagGroupInfo instanceof Entity) {
                     Optional<BOrd> ord = tagGroupInfo.getOrdToEntity();
                     if (ord.isPresent()) {
                        BOrd slotPathOrd = ord.get().get(thisComp).asComponent().getSlotPathOrd();
                        String addName = SlotPath.escape(tagGroupInfo.getGroupId().getQName());
                        if (thisComp.get(addName) == null) {
                           Property tgProp = thisComp.add(addName, slotPathOrd, BEditTagDialog.TAG_GROUP_FLAGS);
                           thisComp.setFacets(tgProp, BEditTagDialog.TAG_GROUP_FACETS);
                        }
                     }
                  }

                  for (TagInfo tagInfo : tagGroupInfo.getTagList()) {
                     String qName = tagInfo.getTagId().getQName();
                     if (!keepTags.contains(qName)) {
                        String removeName = SlotPath.escape(qName);
                        BValue tagValue = currentComp.get(removeName);
                        if (tagValue != null) {
                           if (tagValue.equivalent(tagInfo.getDefaultValue())) {
                              currentComp.remove(removeName);
                           } else {
                              keepTags.add(removeName);
                           }
                        }
                     }
                  }
               }

               for (BTagGroupInfo tagGroupInfo : dontAddTagGroup) {
                  for (TagInfo tagInfox : tagGroupInfo.getTagList()) {
                     String qName = tagInfox.getTagId().getQName();
                     if (!keepTags.contains(qName)) {
                        String removeName = SlotPath.escape(qName);
                        if (currentComp.get(removeName) != null) {
                           currentComp.remove(removeName);
                        }
                     }
                  }
               }

               keepTags = new ArrayList<>();
               addTagGroup = new ArrayList<>();
               dontAddTagGroup = new ArrayList<>();
            }
         }
      }

      boolean needsRelationHints = this.relationPane != null && !this.relationPane.hasValidHints();
      boolean needBindHints = this.ioPane != null && !this.ioPane.hasValidHints();
      this.outOfDateSubtemplates = this.templateMgr.hasOutOfDateTemplate();
      if (needsRelationHints || needBindHints) {
         int proceed = BDialog.confirm(this.getWbShell(), lex.getText("commands.save.needsHints.title"), lex.getText("commands.save.needsHints.message"));
         if (proceed != 4) {
            return false;
         }
      }

      this.settingsPane.save();
      this.bogPane.save();
      if (this.relationPane != null) {
         this.relationPane.save();
      }

      if (this.ioPane != null) {
         this.ioPane.save();
      }

      this.graphicsPane.save();
      this.templateConfig.updatePxEditBindings(this.graphicsPane.pxEditTargets);
      boolean isStationTemplate = this.templateComp.getParent().asComponent().getChildComponents()[0].getType().is(BStation.TYPE);
      if (!isStationTemplate) {
         TmplUtil.convertTagGroupRelationsToTags(this.templateComp);
      }

      TmplUtil.markComponentTags(this.templateComp);
      TagDictionaryService tagDictionaryService = this.templateComp.getTagDictionaryService();
      if (tagDictionaryService instanceof BTagDictionaryService) {
         Property tdsProp = ((BTagDictionaryService)tagDictionaryService).getPropertyInParent();
         BComponent tmplRoot = ((BTagDictionaryService)tagDictionaryService).getParent().asComponent();
         tmplRoot.setFlags(tdsProp, tmplRoot.getFlags(tdsProp) | 2);
      }

      return true;
   }

   private FilePath implSave(
      String saveNameAndPath, String saveTitle, String saveVersion, BComponent saveRoot, BINtplFile sourceNtpl, TemplateType templateType, boolean creatingNew
   ) {
      BTemplateConfig saveConfig = BTemplateConfig.getOrCreateConfigForRoot(saveRoot, templateType == TemplateType.APPLICATION);
      TemplateManifest sourceManifest = sourceNtpl.getTemplateManifest();
      BFacets f = saveRoot.getSlotFacets(this.tcfgProp);
      saveRoot.setFacets(this.tcfgProp, BFacets.makeRemove(f, "ntplCreation"));
      saveConfig.setTemplateName(TemplateUiUtil.templateNameFromNameEntry(saveNameAndPath));
      BVersion version = new BVersion(this.vendorField.getText(), saveVersion);
      version.setBajaVersionString(Sys.getBajaVersion().toString());
      saveConfig.setVersion(version);
      saveConfig.setVersionDate(BAbsTime.now());
      TemplateManifest locManifest = saveConfig.getManifest();
      locManifest.vendor = this.vendorField.getText();
      locManifest.title = saveTitle;
      locManifest.version = saveVersion;
      locManifest.description = this.descriptionField.getText();
      locManifest.info = this.infoField.getText();
      if (creatingNew) {
         locManifest.uID = BUuid.make();
         saveConfig.setUID(locManifest.uID);
      } else {
         locManifest.uID = sourceManifest.uID;
      }

      locManifest.buildVersion = Sys.getModuleForClass(BTemplateView.class).getVendorVersion(RuntimeProfile.rt).toString();
      CompUtil.setOrAdd(saveRoot, ROOT_TAG_NAME, BMarker.MARKER, 16389, null, null);
      CompUtil.setOrAdd(
         saveRoot,
         NTPL_FILE_TAG_NAME,
         BString.make(NtplUtil.buildTemplateFileName(TemplateUiUtil.templateNameFromNameEntry(saveNameAndPath), templateType)),
         16389,
         null,
         null
      );
      CompUtil.setOrAdd(saveRoot, VENDOR_TAG_NAME, BString.make(locManifest.vendor), 16389, null, null);
      CompUtil.setOrAdd(saveRoot, TITLE_TAG_NAME, BString.make(locManifest.title), 16389, null, null);
      CompUtil.setOrAdd(saveRoot, VERSION_TAG_NAME, BString.make(locManifest.version), 16389, null, null);
      CompUtil.setOrAdd(saveRoot, INFO_TAG_NAME, BString.make(locManifest.description), 16389, null, null);
      CompUtil.setOrAdd(saveRoot, UID_TAG_NAME, BString.make(locManifest.uID.toString()), 16389, null, null);
      if (this.iconSrcOrd != null && !this.iconSrcOrd.equals(BOrd.make("NO ICON SELECTED"))) {
         CompUtil.setOrAdd(saveRoot, ICON_TAG_NAME, this.iconSrcOrd, 16389, null, null);
      } else if (saveRoot.get(ICON_TAG_NAME) != null) {
         saveRoot.remove(ICON_TAG_NAME);
      }

      long signature = UpgradeUtil.getTemplateSignature(saveRoot);
      locManifest.bogSignature = Long.toHexString(signature);

      try {
         locManifest.state = BTemplateState.make(this.stateField.saveValue().toString().toLowerCase());
      } catch (Exception var20) {
         log.log(Level.WARNING, "Error setting template edit state:" + var20.getLocalizedMessage(), (Throwable)var20);
      }

      locManifest.resources = sourceManifest.resources;
      Hashtable<String, BDependency> hashtable = new Hashtable<>();

      for (BComponent root : this.findAllTemplateRoots(saveRoot, templateType == TemplateType.APPLICATION)) {
         DependencyUtil.getBogComponentDependencies(root, hashtable, BTemplateOptions.get().getUseMinorVersionOnDeployment());
      }

      DependencyUtil.getTemplatePxDependencies(this.graphicsPane.getPxRefs(), hashtable, BTemplateOptions.get().getUseMinorVersionOnDeployment());
      locManifest.addDependencies(hashtable.values().toArray(new BDependency[0]));

      for (TmplUtil.SubtemplateInfo stInfo : TmplUtil.listSubtemplates(saveRoot)) {
         locManifest.addSubtemplate(
            stInfo.getDeployName(), stInfo.getVendor(), stInfo.getVersion(), stInfo.getDeployOrd().toString(), stInfo.getNtplFileOrd().toString()
         );
      }

      locManifest.optional = sourceManifest.optional.copy();
      this.removeUnavailableOptionals(saveRoot, locManifest.optional);
      return NtplUtil.makeNtpl(
         saveConfig.getTemplateName(),
         TemplateUiUtil.templateFolderFromNameEntry(saveNameAndPath),
         templateType,
         saveRoot.getParent().asComponent(),
         saveConfig,
         this.graphicsPane.getPxRefs(),
         this.pngFile,
         sourceNtpl,
         creatingNew
      );
   }

   private void removeUnavailableOptionals(BObject saveRoot, Array<BOrd> current) {
      for (BOrd ord : current) {
         try {
            OrdTarget ex = ord.resolve(saveRoot);
         } catch (Exception var6) {
            current.remove(ord);
         }
      }
   }

   private void refreshTemplateSidebar() {
      BWbShell wbShell = this.getWbShell();
      if (wbShell instanceof BNiagaraWbShell) {
         BIWbSideBar[] bars = ((BNiagaraWbShell)wbShell).pane.getSideBar().list();

         for (BIWbSideBar bar : bars) {
            if (bar instanceof BTemplateSideBar) {
               ((BTemplateSideBar)bar).updateTree();
            }
         }
      }
   }

   private BComponent[] findAllTemplateRoots(BComponent templateComp, boolean isApplicationTemplate) {
      return isApplicationTemplate ? ApplicationTemplateUtil.findApplicationRoots((BStation)templateComp) : new BComponent[]{templateComp};
   }

   private boolean isValidVersion(String version) {
      String regex = "^\\d+(\\.?\\d+)*$";
      return version.matches(regex);
   }

   private boolean verifyVersionChange() {
      Version versionOld = new Version(this.manifest.version);
      if (!this.isValidVersion(this.versionField.getText())) {
         BDialog.error(
            this.getWbShell(),
            lex.getText("templateView.invalidVersionTitle"),
            lex.getText("templateView.invalidVersion", new Object[]{this.versionField.getText()})
         );
         return false;
      } else {
         Version versionNew = new Version(this.versionField.getText());
         int result = versionNew.compareTo(versionOld);
         if (result == 0) {
            try {
               if (this.stateField.saveValue().equals(BTemplateState.draft)) {
                  return true;
               }
            } catch (Exception var5) {
               log.log(Level.WARNING, "Error detecting template edit state:" + var5.getLocalizedMessage(), (Throwable)var5);
            }

            this.invalidVersion.setText(lex.getText("templateView.versionChangeRequired", new Object[]{this.manifest.version}));
            return false;
         } else if (result < 0) {
            this.invalidVersion.setText(lex.getText("templateView.versionChangeRequired", new Object[]{this.manifest.version}));
            return false;
         } else {
            this.invalidVersion.setVisible(false);
            return true;
         }
      }
   }

   private boolean isDuplicateFilename(String folderEntry, String nameEntry) {
      FilePath fullPath = NtplUtil.buildTemplateFilePath(TemplateUiUtil.templateRelativePathFromEntries(folderEntry, nameEntry), this.templateType);
      File targetFile = BFileSystem.INSTANCE.pathToLocalFile(fullPath);
      return targetFile.exists();
   }

   private boolean isValidPathAndName(String folderEntry, String nameEntry) {
      if (null == nameEntry || nameEntry.isEmpty()) {
         BDialog.error(this.getWbShell(), lex.getText("templateName.invalidNameTitle"), lex.getText("templateName.zeroLengthMessage"));
         return false;
      } else if (!TemplateUiUtil.isValidTemplateNameEntry(nameEntry)) {
         BDialog.error(
            this.getWbShell(),
            lex.getText("templateName.invalidNameTitle"),
            lex.getText("templateName.invalidCharacterMessage.filename", new Object[]{nameEntry})
         );
         return false;
      } else if (!TemplateUiUtil.isValidTemplateFolder(folderEntry)) {
         BDialog.error(
            this.getWbShell(),
            lex.getText("templateName.invalidFolderTitle"),
            lex.getText("templateName.invalidCharacterMessage.folderName", new Object[]{folderEntry})
         );
         return false;
      } else {
         return true;
      }
   }

   protected BButton makeCancelButton() {
      this.btnCancel = new BButton(new Command(this, lex, "commands.cancel"));
      this.linkTo(this.btnCancel, BButton.actionPerformed, cancel);
      return this.btnCancel;
   }

   protected BButton makeSaveButton() {
      this.btnSave = new BButton(new Command(this, lex, "commands.save"));
      this.linkTo(this.btnSave, BButton.actionPerformed, save);
      this.btnSave.setEnabled(false);
      return this.btnSave;
   }

   protected BButton makeDuplicateButton() {
      this.btnDuplicate = new BButton(new Command(this, lex, "commands.duplicate"));
      this.linkTo(this.btnDuplicate, BButton.actionPerformed, duplicate);
      this.btnDuplicate.setEnabled(true);
      return this.btnDuplicate;
   }

   private String getUniqueName(String templateName, BDirectory dir) {
      return ObjectUtil.generateUniqueName(templateName, new BTemplateView.NameContainerImpl(dir));
   }

   public TemplateManifest getManifest() {
      return this.manifest;
   }

   void swapActiveView(BWbView wbView) {
      if (this.getShell() != null) {
         BTransferWidget trans = wbView != null ? wbView.getTransferWidget() : null;
         this.setTransferWidget(trans);
         BIMenuBar w = (BIMenuBar)this.getShell().getMenuBar();

         for (BIMenu biMenu : menuList) {
            w.removeMenu(biMenu);
         }

         menuList.clear();
         if (wbView != null) {
            BIMenu[] menus = wbView.getViewMenus();
            if (menus != null) {
               for (int i = 0; i < menus.length; i++) {
                  w.addMenu("template" + i, menus[i]);
                  menuList.add(menus[i]);
               }
            }
         }
      }
   }

   private BWbView getWbView(BWidget content) {
      if (content instanceof BWbView) {
         return (BWbView)content;
      } else {
         BWbView[] a = (BWbView[])CompUtil.getDescendants(content, BWbView.class);
         return a.length > 0 ? a[0] : null;
      }
   }

   public class BrowseIcons extends Command {
      BTextField txt;

      BrowseIcons(BWidget owner, BTextField t) {
         super(owner, "");
         this.txt = t;
      }

      public String getText() {
         return null;
      }

      public BImage getIcon() {
         return BTemplateView.openIcon;
      }

      public CommandArtifact doInvoke() {
         BOrd ord = null;
         BTemplateView.this.imageOrd = null;
         BTemplateView.this.pngFile = null;

         try {
            ord = this.getOrd();
         } catch (Throwable var5) {
         }

         if (ord != null) {
            this.txt.setText(ord.encodeToString());
            this.txt.repaint();
            BObject o = ord.resolve().get();
            if (o instanceof BPngFile) {
               BTemplateView.this.pngFile = (BPngFile)o;
               BTemplateView.this.imageOrd = ord;
               Resource res = BTemplateView.this.manifest.getResource("image.png", "png");
               BTemplateView.this.iconSrcOrd = BTemplateView.this.imageOrd.relativizeToSession();
               String iconOrd = BTemplateView.this.iconSrcOrd.toString(null);
               if (res == null) {
                  BTemplateView.this.manifest.addResource("image.png", "png", iconOrd);
               } else {
                  res.sourceOrd = iconOrd;
               }
            }
         } else {
            BTemplateView.this.manifest.removeResource("image.png");
            this.txt.setText("");
         }

         BTemplateView.this.setIcon();
         BTemplateView.this.templateModified();
         return null;
      }

      BOrd getOrd() {
         BFileChooser c = BFileChooser.makeOpen(this.getShell());
         String t = this.txt.getText();
         if (t.indexOf(".") > 0) {
            t = t.substring(0, t.lastIndexOf("/"));
            c.setCurrentDirectory(BOrd.make(t));
         } else {
            c.setCurrentDirectory(BOrd.make("module://icons/x16/file.png"));
         }

         return c.show();
      }
   }

   private static class NameContainerImpl implements NameContainer {
      BDirectory dir;

      NameContainerImpl(BDirectory dir) {
         this.dir = dir;
      }

      public boolean contains(String name) {
         for (BIFile f : this.dir.listFiles()) {
            String fnWithExt = f.getFileName();
            int dot = fnWithExt.lastIndexOf(46);
            String fn = fnWithExt.substring(0, dot > 0 ? dot : fnWithExt.length());
            if (fn.equals(name)) {
               return true;
            }
         }

         return false;
      }
   }

   private class PropertyChangedSubscriber extends Subscriber {
      private PropertyChangedSubscriber() {
      }

      public void event(BComponentEvent event) {
         BTemplateView.this.doTemplateModified();
      }
   }

   private class TabbedPaneSubscriber extends WidgetSubscriber {
      private TabbedPaneSubscriber() {
      }

      public void modified(BWidgetEvent e) {
         if (e.getWidget().equals(BTemplateView.this.tp)) {
            BWidget content = BTemplateView.this.tp.getSelectedLabelPane().getContent();
            BTemplateView.this.swapActiveView(BTemplateView.this.getWbView(content));
         }
      }
   }
}
