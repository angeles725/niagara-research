package com.tridium.template.ui;

import com.tridium.data.BDataRow;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.install.BVersion;
import com.tridium.sys.transfer.DeployToComp;
import com.tridium.tagdictionary.BNiagaraTagDictionary;
import com.tridium.template.BPasswordBinding;
import com.tridium.template.BRelationInfo;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateInfo;
import com.tridium.template.BTemplateService;
import com.tridium.template.TemplateConst;
import com.tridium.template.UpgradeUtil;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.TemplateManager;
import com.tridium.template.file.TemplateManager.TemplateInfo;
import com.tridium.template.job.BUpgradeTemplateJob;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.BWbDeployableNtplFile;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.ui.BOptionDialog;
import com.tridium.ui.theme.Theme;
import com.tridium.util.CompUtil;
import com.tridium.util.LinkUtil;
import com.tridium.workbench.job.BJobBar;
import com.tridium.workbench.util.WbViewEventWorker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.collection.BITable;
import javax.baja.collection.ColumnList;
import javax.baja.collection.TableCursor;
import javax.baja.control.BControlPoint;
import javax.baja.data.BIDataTable;
import javax.baja.data.BIDataValue;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.job.BJob;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.SortUtil;
import javax.baja.search.BSearchService;
import javax.baja.security.BPassword;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BIService;
import javax.baja.sys.BLink;
import javax.baja.sys.BMarker;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BStation;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Tag;
import javax.baja.tag.TagDictionaryService;
import javax.baja.tag.Tags;
import javax.baja.ui.BBorder;
import javax.baja.ui.BButton;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BMenu;
import javax.baja.ui.BMenuItem;
import javax.baja.ui.BTextField;
import javax.baja.ui.BToolBar;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BExpandablePane;
import javax.baja.ui.pane.BFlowPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableCellRenderer;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.ui.table.TableCellRenderer.Cell;
import javax.baja.ui.util.BTitlePane;
import javax.baja.util.BServiceContainer;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;
import javax.baja.workbench.BWbShell;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;
import javax.baja.workbench.mgr.folder.BFolderManager;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"template:TemplateService"},
      requiredPermissions = "r"
   )}
)
@NiagaraAction(
   name = "updateCommands",
   flags = 4
)
public class BTemplateManager extends BWbComponentView implements TemplateConst {
   public static final Action updateCommands = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BTemplateManager.class);
   private static final String[] INPUT_SLOTS = new String[]{
      "in2", "in3", "in4", "in5", "in6", "in7", "in9", "in10", "in11", "in12", "in13", "in14", "in15", "in16", "fallback"
   };
   private static final BImage DEFAULT_IMAGE = BImage.make("module://icons/x16/files/ntpl.png");
   private static final Lexicon lex = Lexicon.make("template");
   public static final Logger log = Logger.getLogger("template.manager");
   private static String NTPL_FILE_TAG_NAME = SlotPath.escape(TEMPLATE_FILE_TAG_ID.getQName());
   private static String ROOT_TAG_NAME = SlotPath.escape(TEMPLATE_ROOT_TAG_ID.getQName());
   private static String VENDOR_TAG_NAME = SlotPath.escape(TEMPLATE_VENDOR_TAG_ID.getQName());
   private static String VERSION_TAG_NAME = SlotPath.escape(TEMPLATE_VERSION_TAG_ID.getQName());
   private static String INFO_TAG_NAME = SlotPath.escape(TEMPLATE_DESCRIPTION_TAG_ID.getQName());
   private static String UP_TO_DATE = "Up to Date";
   private static String OUT_OF_DATE = "Out of Date";
   private static String OLDER_VERSION_AVAILABLE = "Older version available";
   private static String NOT_AVAILABLE = "not available";
   private static String UNKNOWN = "Unknown";
   private static String UPGRADE = "Upgrade";
   private static String DOWNGRADE = "Downgrade";
   private static String REDEPLOY = "Redeploy";
   private static String UPDATED = "Updated";
   private static String[] VERSION_STATUS = new String[]{
      UP_TO_DATE, OUT_OF_DATE, OLDER_VERSION_AVAILABLE, NOT_AVAILABLE, UNKNOWN, UPGRADE, DOWNGRADE, REDEPLOY, UPDATED
   };
   private static int UP_TO_DATE_SEL = BTemplateInfo.UP_TO_DATE_SEL;
   private static int OUT_OF_DATE_SEL = BTemplateInfo.OUT_OF_DATE_SEL;
   private static int OLDER_VERSION_AVAILABLE_SEL = BTemplateInfo.OLDER_VERSION_AVAILABLE_SEL;
   private static int NOT_AVAILABLE_SEL = BTemplateInfo.NOT_AVAILABLE_SEL;
   private static int UNKNOWN_SEL = BTemplateInfo.UNKNOWN_SEL;
   private static int UPGRADE_SEL = BTemplateInfo.UPGRADE_SEL;
   private static int DOWNGRADE_SEL = BTemplateInfo.DOWNGRADE_SEL;
   private static int REDEPLOY_SEL = BTemplateInfo.REDEPLOY_SEL;
   private static int UPDATED_SEL = BTemplateInfo.UPDATED_SEL;
   public static final Id SLOT_PATH_SCOPE = Id.newId("n", "slotPathScope");
   private boolean isOffline = false;
   private int versionState = NOT_AVAILABLE_SEL;
   private BTemplateConfig templateConfig;
   private BComponent root;
   private Object owner;
   private BTemplateManager manager;
   private BTemplateService templateService;
   private BComponent target;
   private boolean isTemplateEditor = false;
   private BSearchService searchService;
   private BTable table;
   private BTemplateManager.Untemplate untemplateCmd;
   public BTemplateManager.UpgradeAll upgradeAllCmd;
   private BulkDeploy bulkDeployCmd;
   private UpdateConfigs updateConfigsCmd;
   private BTemplateManager.Upgrade upgradeCmd;
   private BTemplateManager.Downgrade downgradeCmd;
   private BTemplateManager.Redeploy redeployCmd;
   private BButton upDownGradeButton;
   private BButton untemplateButton;
   private BTemplateManager.Reset resetCmd;
   public BTemplateManager.Commit commitCmd;
   private IntHashMap nameMap;
   private BBorderPane topPane;
   private BJobBar jobBar;
   private BTitlePane tablePane;
   private BFlowPane buttonPane;
   private Array<TmplUtil.BindInfo> bindInfo = new Array(TmplUtil.BindInfo.class);
   private boolean abort = false;
   private boolean isDeploy = false;
   private Consumer<String> statusListener = s -> {};
   private BTemplateManager.IoSlotInfo[] inputInfo;
   private BTemplateManager.IoSlotInfo[] outputInfo;
   private BTemplateManager.IoSlotInfo[] relationInfo;
   private Hashtable<String, Object> cache;
   private Hashtable<Object, TemplateInfo> availTemplates;
   private BJob job;
   private boolean commitInProgress = false;
   private boolean jobInProgress = false;
   private BVector vector;
   private Version remoteVersion;
   private boolean isRemotePre4_3 = false;

   public void updateCommands() {
      this.invoke(updateCommands, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BTemplateManager() {
      this.manager = this;
      this.owner = this;
      BEdgePane content = new BEdgePane();
      this.table = new BTable();
      this.table.setController(new BTemplateManager.Controller());
      this.table.setModel(new BTemplateManager.Model());
      this.table.setCellRenderer(new BTemplateManager.CellRenderer());
      this.tablePane = BTitlePane.makePane(this.getTypeDisplayName(null), this.table);
      content.setCenter(this.tablePane);
      content.setBottom(new BBorderPane(this.makeButtonPane(), 4.0, 0.0, 4.0, 0.0));
      this.jobBar = new BJobBar();
      this.attach(this.jobBar);
      BBorderPane jobPane = new BBorderPane(this.jobBar, BBorder.none, BInsets.make(0.0, 5.0, 0.0, 5.0));
      jobPane.setFill(Theme.widget().getWindowBackground());
      jobPane = new BBorderPane(jobPane, BBorder.none, BInsets.DEFAULT);
      content.setTop(new BBorderPane(jobPane, BInsets.make(0.0, 0.0, 5.0, 0.0)));
      this.setContent(content);
      this.linkTo(this.table, BTable.selectionModified, updateCommands);
      this.linkTo(this.table, BTable.tableModified, updateCommands);
      this.nameMap = new IntHashMap();
      this.nameMap.put(0, "templateManager.root");
      this.nameMap.put(1, "templateManager.name");
      this.nameMap.put(2, "templateManager.vendor");
      this.nameMap.put(3, "templateManager.version");
   }

   private BWidget makeButtonPane() {
      this.untemplateCmd = new BTemplateManager.Untemplate(this.table);
      this.upgradeAllCmd = new BTemplateManager.UpgradeAll(this.table);
      this.upgradeCmd = new BTemplateManager.Upgrade(this.table);
      this.downgradeCmd = new BTemplateManager.Downgrade(this.table);
      this.redeployCmd = new BTemplateManager.Redeploy(this.table);
      this.resetCmd = new BTemplateManager.Reset(this.table);
      this.commitCmd = new BTemplateManager.Commit(this.table);
      this.bulkDeployCmd = new BTemplateManager.ComponentBulkDeploy();
      this.updateConfigsCmd = new UpdateConfigs(this, lex.get("templateManager.updateConfigs"));
      this.upDownGradeButton = new BButton(this.upgradeCmd, true, false);
      this.untemplateButton = new BButton(this.untemplateCmd, true, false);
      this.buttonPane = new BFlowPane(BHalign.center);
      this.buttonPane.add(null, this.untemplateButton);
      this.buttonPane.add(null, new BButton(this.upgradeAllCmd, true, false));
      this.buttonPane.add("b2", this.upDownGradeButton);
      this.buttonPane.add(null, new BButton(this.resetCmd, true, false));
      this.buttonPane.add(null, new BButton(this.commitCmd, true, false));
      this.buttonPane.add("bulkDeploy", new BButton(this.bulkDeployCmd, true, false));
      this.buttonPane.add("updateConfigs", new BButton(this.updateConfigsCmd, true, false));
      return this.buttonPane;
   }

   public void setTemplateConfig(BTemplateConfig templateConfig) {
      this.templateConfig = templateConfig;
      this.root = templateConfig.getParent().asComponent();
   }

   public void setTemplateService(BTemplateService templateService) {
      this.templateService = templateService;
   }

   public void setOwner(Object owner) {
      this.owner = owner;
   }

   public void bindInput(Slot slot, BTemplateConfig templateConfig, Hashtable<String, Object> cache) throws Exception {
      BTemplateManager.Bind bind = new BTemplateManager.Bind(slot, templateConfig, true, true, cache);
      bind.doInvoke();
   }

   public static boolean processPostDeploy(BWidget owner, BComponent[] deployRoots, Consumer<String> statusListener, Context cx) {
      if (deployRoots.length == 0) {
         return true;
      } else {
         BTemplateManager tm = new BTemplateManager();
         tm.setIsDeploy(true);
         tm.setStatusListener(statusListener);
         BTemplateService tService = tm.getTemplateService(deployRoots[0]);
         BSearchService searchService = tm.getSearchService(deployRoots[0]);
         if (searchService != null) {
            searchService.lease();
            if (!searchService.getStatus().isValid()) {
               BDialog.warning(owner, "Warning", lex.getText("templateManager.searchService.notValid.exception"));
               return true;
            }

            tm.setTemplateService(tService);
         }

         try {
            tm.setOwner(owner);
            boolean first = true;
            BTemplateConfig reuseTemplateConfig = null;

            for (BComponent deployRoot : deployRoots) {
               BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployRoot);
               tm.setTemplateConfig(templateConfig);
               if (cx == null || !Context.includes(cx, DeployToComp.NoPostLink)) {
                  tm.queryForIoChoices(first);
                  first = false;
                  tm.promptUser(deployRoots.length > 1);
                  if (cx == null || !Context.includes(cx, DeployToComp.NoPostConfigEdit)) {
                     if (reuseTemplateConfig == null) {
                        boolean reuse = editConfigProperties(owner, templateConfig, deployRoots.length > 1);
                        if (reuse) {
                           reuseTemplateConfig = templateConfig;
                        }
                     } else {
                        copyTemplateConfigProperties(reuseTemplateConfig, templateConfig);
                     }
                  }

                  tm.checkPasswords();
                  tm.formatBindInfos();
                  if (tService != null) {
                     BVector vector = new BVector();

                     for (BTemplateConfig subTemplateConfig : (BTemplateConfig[])CompUtil.getDescendants(deployRoot, BTemplateConfig.class)) {
                        BComponent stRoot = BTemplateConfig.getRootForConfig(subTemplateConfig);
                        if (stRoot != deployRoot) {
                           stRoot.lease();
                           long stRootSignature = UpgradeUtil.getTemplateSignature(stRoot);
                           String ntplSignature = tm.getTemplateFileSignature(subTemplateConfig);
                           if (Long.toHexString(stRootSignature).equals(ntplSignature)) {
                              if (log.isLoggable(Level.FINE)) {
                                 log.fine(
                                    " Post Deploy: No need to update "
                                       + subTemplateConfig.getTemplateName()
                                       + " at "
                                       + subTemplateConfig.getSlotPathOrd().encodeToString()
                                 );
                              }
                           } else {
                              vector.add("v?", subTemplateConfig.getSlotPathOrd());
                           }
                        }
                     }

                     if (vector.getPropertyCount() > 0) {
                        tService.submitUpgradeJob(vector);
                     }
                  }
               }

               templateConfig.setDeployed(true);
            }
         } catch (Exception var24) {
            if (tm.getAbort()) {
               try {
                  for (BComponent deployRoot : deployRoots) {
                     BComplex parent = deployRoot.getParent();
                     if (parent != null && parent.isComponent()) {
                        deployRoot.getParent().asComponent().remove(deployRoot);
                     }
                  }
               } catch (Exception var23) {
               }

               return true;
            }

            log.log(Level.WARNING, "Error updating template after deployment", (Throwable)var24);
            BDialog.error(owner, var24.getLocalizedMessage());
         }

         tm.displayBindInfo();
         return false;
      }
   }

   private String getTemplateFileSignature(BTemplateConfig config) {
      TemplateManager tmInstance = TemplateManager.INSTANCE;
      TemplateInfo templateInfo = tmInstance.getTemplate(config.getUID(), config.getManifest().vendor);
      String signature = "";
      if (templateInfo == null) {
         return signature;
      } else {
         BNtplFile ntplFile = templateInfo.getNtplFile();
         Throwable var6 = null;

         String manifest;
         try {
            if (ntplFile != null) {
               TemplateManifest manifestx = ntplFile.getTemplateManifest();
               return manifestx.bogSignature;
            }

            manifest = signature;
         } catch (Throwable var17) {
            var6 = var17;
            throw var17;
         } finally {
            if (ntplFile != null) {
               if (var6 != null) {
                  try {
                     ntplFile.close();
                  } catch (Throwable var16) {
                     var6.addSuppressed(var16);
                  }
               } else {
                  ntplFile.close();
               }
            }
         }

         return manifest;
      }
   }

   private void checkPasswords() {
      BTemplateConfig tc = this.templateConfig;
      BPasswordBinding[] pswBindings = (BPasswordBinding[])tc.getChildren(BPasswordBinding.class);
      BGridPane grid = new BGridPane(1);
      boolean hasDefaultPasswords = false;

      for (BPasswordBinding pswBinding : pswBindings) {
         Optional<BComponent> target = pswBinding.getTarget();
         if (target.isPresent()) {
            BComponent targetComp = target.get();
            targetComp.lease();
            BValue bValue = targetComp.get(pswBinding.getPswSlot());
            if (bValue instanceof BPassword && ((BPassword)bValue).isDefault()) {
               hasDefaultPasswords = true;
               grid.add(null, new BLabel(targetComp.getSlotPath() + " has a default password."));
            }
         }
      }

      if (hasDefaultPasswords) {
         BBorderPane pane = new BBorderPane(grid, 10.0, 10.0, 10.0, 10.0);
         BDialog.open((BWidget)this.owner, lex.getText("Default Password"), pane, 1);
      }
   }

   private static void copyTemplateConfigProperties(BTemplateConfig from, BTemplateConfig to) {
      for (Property prop : from.getProperties()) {
         int flags = from.getFlags(prop);
         if ((flags & 5) == 0) {
            String propName = prop.getName();
            if (!to.get(propName).equivalent(from.get(prop))) {
               to.set(propName, from.get(prop).newCopy(true));
            }
         }
      }
   }

   private static boolean editConfigProperties(BWidget owner, BTemplateConfig tc, boolean isMultiple) {
      if (owner != null && !(owner instanceof BFolderManager) && tc.getDynamicPropertiesArray().length != 0) {
         BComponent root = BTemplateConfig.getRootForConfig(tc);
         if (root == null) {
            return false;
         } else {
            BCheckBox reuseCb = new BCheckBox("Use values for all.");
            int state = 0;

            while (state < 2) {
               try {
                  if (tc.hasConfigProperties()) {
                     BWbFieldEditor editor = new BTemplateConfigFE();
                     editor.loadValue(tc);

                     for (BExpandablePane pane : (BExpandablePane[])CompUtil.getDescendants(editor, BExpandablePane.class)) {
                        pane.setExpanded(true);
                     }

                     BGridPane gridPane = new BGridPane(1);
                     BScrollPane scrollPane = new BScrollPane(new BBorderPane(gridPane, 3.0, Theme.scrollBar().getFixedWidth() + 3.0, 3.0, 3.0));
                     gridPane.add(null, editor);
                     if (isMultiple) {
                        gridPane.add(null, reuseCb);
                     }

                     BOptionDialog dlg = new BOptionDialog(
                        owner, lex.getText("templateManager.configDialog.title") + ": " + root.getNavName(), scrollPane, 3, null, null
                     );
                     dlg.computePreferredSize();
                     dlg.setBoundsCenteredOnOwner();
                     boolean validEntry = false;

                     while (!validEntry) {
                        dlg.open();
                        if (dlg.getResult() != 1) {
                           return false;
                        }

                        try {
                           tc = (BTemplateConfig)editor.saveValue();
                        } catch (Exception var12) {
                           BDialog.error(owner, "Exception: " + var12.getLocalizedMessage());
                           continue;
                        }

                        validEntry = true;
                     }
                  }
               } catch (Exception var13) {
                  BDialog.error(owner, var13.getLocalizedMessage());
                  state = 1;
                  continue;
               }

               if (tc == null) {
                  return false;
               }

               state = 2;
            }

            return reuseCb.getSelected();
         }
      } else {
         return false;
      }
   }

   private static BWbFieldEditor makeConfigEditor(BTemplateConfig tc) {
      SlotCursor<Property> c = tc.getProperties();

      while (c.next()) {
         Property p = c.property();
         if (p.isFrozen()) {
         }
      }

      return BWbFieldEditor.makeFor(tc);
   }

   private void queryForIoChoices(boolean isFirst) {
      try {
         Slot[] slots = this.templateConfig.getInputSlots();
         this.inputInfo = this.initIoSlotInfos(this.inputInfo, this.templateConfig.getInputSlots(), true, this.templateConfig);
         this.outputInfo = this.initIoSlotInfos(this.outputInfo, this.templateConfig.getOutputSlots(), false, this.templateConfig);
         this.relationInfo = this.initRelateInfos(this.relationInfo, this.templateConfig);
         if (isFirst) {
            for (BTemplateManager.IoSlotInfo ioSlotInfo : this.inputInfo) {
               if (ioSlotInfo.bindHints != null && !ioSlotInfo.bindHints.isEmpty()) {
                  this.statusListener.accept(lex.getText("deploy.searchForInputComp") + " " + ioSlotInfo.bindHints);
                  ioSlotInfo.choices = TmplUtil.findMatchingObjects(ioSlotInfo, this.searchService, this.templateService, this.templateConfig, lex);
                  if ((ioSlotInfo.choices == null || ioSlotInfo.choices.length == 0) && ioSlotInfo.results == 0) {
                     ioSlotInfo.results = 2;
                  }
               } else {
                  ioSlotInfo.results = 1;
               }
            }

            for (BTemplateManager.IoSlotInfo ioSlotInfox : this.outputInfo) {
               if (ioSlotInfox.bindHints != null && !ioSlotInfox.bindHints.isEmpty()) {
                  this.statusListener.accept(lex.getText("deploy.searchForOutputComp") + " " + ioSlotInfox.bindHints);
                  ioSlotInfox.choices = TmplUtil.findMatchingObjects(ioSlotInfox, this.searchService, this.templateService, this.templateConfig, lex);
                  if ((ioSlotInfox.choices == null || ioSlotInfox.choices.length == 0) && ioSlotInfox.results == 0) {
                     ioSlotInfox.results = 12;
                  }
               } else {
                  ioSlotInfox.results = 11;
               }
            }

            for (BTemplateManager.IoSlotInfo relateInfo : this.relationInfo) {
               if (relateInfo.bindHints != null && !relateInfo.bindHints.isEmpty()) {
                  this.statusListener.accept(lex.getText("deploy.searchForRelatedComp") + " " + relateInfo.bindHints);
                  relateInfo.choices = TmplUtil.findMatchingObjects(relateInfo, this.searchService, this.templateService, this.templateConfig, lex);
                  if ((relateInfo.choices == null || relateInfo.choices.length == 0) && relateInfo.results == 0) {
                     relateInfo.results = 22;
                  }
               } else {
                  relateInfo.results = 21;
               }
            }
         }
      } catch (Exception var7) {
         log.log(Level.WARNING, "Error resolving I/O choices:" + var7.getLocalizedMessage(), (Throwable)var7);
      }

      this.statusListener.accept("postDeploy");
   }

   private void promptUser(boolean isMultiple) {
      this.promptRelations(isMultiple);
      this.promptInputs(isMultiple);
      this.promptOutputs(isMultiple);
   }

   private void promptInputs(boolean isMultiple) {
      for (BTemplateManager.IoSlotInfo ioSlotInfo : this.inputInfo) {
         if (ioSlotInfo.choices != null && ioSlotInfo.choices.length > 0) {
            Object selectInput = ioSlotInfo.lastSelected;
            if (selectInput == null && !ioSlotInfo.dontAskAgain) {
               selectInput = TmplUtil.selectInputSource((BWidget)this.owner, ioSlotInfo, this.templateConfig, isMultiple, lex);
            }

            this.abort = ioSlotInfo.abort;
            if (this.abort) {
               throw new RuntimeException(lex.getText("templateManager.deploy.aborted"));
            }

            if (selectInput != null) {
               this.linkSource(selectInput, this.root, ioSlotInfo);
            } else {
               ioSlotInfo.results = 3;
            }
         }
      }
   }

   private void promptOutputs(boolean isMultiple) {
      for (BTemplateManager.IoSlotInfo ioSlotInfo : this.outputInfo) {
         if (ioSlotInfo.choices != null && ioSlotInfo.choices.length > 0) {
            Object targets = null;
            if (!ioSlotInfo.dontAskAgain) {
               targets = TmplUtil.selectOutputTargets((BWidget)this.owner, ioSlotInfo, this.templateConfig, isMultiple, lex);
            }

            this.abort = ioSlotInfo.abort;
            if (this.abort) {
               throw new RuntimeException(lex.getText("templateManager.deploy.aborted"));
            }

            if (targets != null) {
               this.linkTarget(targets, this.root, ioSlotInfo);
            } else {
               ioSlotInfo.results = 13;
            }
         }
      }
   }

   private void promptRelations(boolean isMultiple) {
      for (BTemplateManager.IoSlotInfo relateInfo : this.relationInfo) {
         if (relateInfo.choices != null && relateInfo.choices.length > 0) {
            Object selectRelations = relateInfo.lastSelected;
            if (selectRelations == null && !relateInfo.dontAskAgain) {
               selectRelations = TmplUtil.selectRelationSource((BWidget)this.owner, relateInfo, this.templateConfig, isMultiple, lex);
            }

            this.abort = relateInfo.abort;
            if (this.abort) {
               throw new RuntimeException(lex.getText("templateManager.deploy.aborted"));
            }

            if (selectRelations != null) {
               this.relateSource(selectRelations, this.root, relateInfo);
            } else {
               relateInfo.results = 23;
            }
         }
      }
   }

   private BTemplateManager.IoSlotInfo changeTargetInfoInIoSlotInfo(BTemplateManager.IoSlotInfo ioSlotInfo, TmplUtil.TargetChoice targetChoice) {
      String targetSlotName = targetChoice.targetSlotEnum.getTag();
      Slot targetSlot = targetChoice.targetPoint.getSlot(targetSlotName);
      ioSlotInfo.otherName = targetChoice.targetPoint.getSlotPath().toString();
      ioSlotInfo.otherSlot = targetSlot.getName();
      return ioSlotInfo;
   }

   private BTemplateManager.IoSlotInfo changeEndpointInfoInIoSlotInfo(BTemplateManager.IoSlotInfo ioSlotInfo, BComponent extEndpoint) {
      ioSlotInfo.otherName = extEndpoint.getSlotPath().toString();
      ioSlotInfo.otherSlot = ioSlotInfo.relationInfo.getInbound() ? "from" : "to";
      return ioSlotInfo;
   }

   private void formatBindInfos() {
      for (BTemplateManager.IoSlotInfo ioSlotInfo : this.inputInfo) {
         this.bindInfo.add(TmplUtil.formatBindResults(ioSlotInfo, lex));
      }

      for (BTemplateManager.IoSlotInfo ioSlotInfo : this.outputInfo) {
         if (ioSlotInfo.linkedTargets.isEmpty()) {
            this.bindInfo.add(TmplUtil.formatBindResults(ioSlotInfo, lex));
         } else {
            for (TmplUtil.TargetChoice targetChoice : ioSlotInfo.linkedTargets) {
               this.bindInfo.add(TmplUtil.formatBindResults(this.changeTargetInfoInIoSlotInfo(ioSlotInfo, targetChoice), lex));
            }
         }
      }

      for (BTemplateManager.IoSlotInfo ioSlotInfox : this.relationInfo) {
         if (ioSlotInfox.relatedEndpoints.isEmpty()) {
            this.bindInfo.add(TmplUtil.formatBindResults(ioSlotInfox, lex));
         } else {
            for (BComponent extEndpoint : ioSlotInfox.relatedEndpoints) {
               this.bindInfo.add(TmplUtil.formatBindResults(this.changeEndpointInfoInIoSlotInfo(ioSlotInfox, extEndpoint), lex));
            }
         }
      }
   }

   private BTemplateManager.IoSlotInfo[] initIoSlotInfos(BTemplateManager.IoSlotInfo[] lastInfos, Slot[] slots, boolean isInput, BTemplateConfig config) {
      BTemplateManager.IoSlotInfo[] infos = new BTemplateManager.IoSlotInfo[slots.length];

      for (int i = 0; i < slots.length; i++) {
         infos[i] = new BTemplateManager.IoSlotInfo(slots[i], isInput, this.templateConfig);
         if (lastInfos != null) {
            infos[i].choices = lastInfos[i].choices;
            infos[i].lastSelected = lastInfos[i].lastSelected;
            infos[i].linkedTargets = lastInfos[i].linkedTargets;
            infos[i].dontAskAgain = lastInfos[i].dontAskAgain;
         }
      }

      return infos;
   }

   private BTemplateManager.IoSlotInfo[] initRelateInfos(BTemplateManager.IoSlotInfo[] lastRelateInfo, BTemplateConfig config) {
      ArrayList<BRelationInfo> infos = this.templateConfig.getRelationInfos();
      this.relationInfo = new BTemplateManager.IoSlotInfo[infos.size()];
      int i = 0;

      for (BRelationInfo info : infos) {
         this.relationInfo[i] = new BTemplateManager.IoSlotInfo(info, this.templateConfig);
         if (lastRelateInfo != null) {
            this.relationInfo[i].choices = lastRelateInfo[i].choices;
            this.relationInfo[i].lastSelected = lastRelateInfo[i].lastSelected;
            this.relationInfo[i].relatedEndpoints = lastRelateInfo[i].relatedEndpoints;
            this.relationInfo[i].dontAskAgain = lastRelateInfo[i].dontAskAgain;
         }

         i++;
      }

      return this.relationInfo;
   }

   public void bindOutput(Slot slot, BTemplateConfig templateConfig) throws Exception {
      BTemplateManager.Bind bind = new BTemplateManager.Bind(slot, templateConfig, false, true);
      bind.doInvoke();
   }

   private void bindRelations(BRelationInfo relationInfo, BTemplateConfig templateConfig, Hashtable<String, Object> cache) throws Exception {
      BTemplateManager.Bind relate = new BTemplateManager.Bind(relationInfo, templateConfig, cache);
      relate.doInvoke();
   }

   private Array<TmplUtil.BindInfo> getBindInfo() {
      return this.bindInfo;
   }

   private boolean getAbort() {
      return this.abort;
   }

   void setIsDeploy(boolean isDeploy) {
      this.isDeploy = isDeploy;
   }

   private void setStatusListener(Consumer<String> statusListener) {
      this.statusListener = statusListener;
   }

   private void displayBindInfo() {
      Object[] bindInfos = this.getBindInfo().trim();
      BScrollPane scrollPane = new BScrollPane();
      BGridPane pane = new BGridPane(1);
      int l = 0;
      if (bindInfos.length > 0) {
         for (Object bindInfo : bindInfos) {
            if (bindInfo instanceof TmplUtil.BindInfo) {
               BLabel infoLabel = new BLabel(++l + ".  " + ((TmplUtil.BindInfo)bindInfo).getInfo(), BHalign.left);
               infoLabel.setForeground(BBrush.makeSolid(((TmplUtil.BindInfo)bindInfo).getForground()));
               pane.add(null, infoLabel);
            }
         }

         pane.computePreferredSize();
         scrollPane.setContent(pane);
         int var10 = BDialog.info((BWidget)this.owner, lex.getText("templateManager.inputBindingResults"), scrollPane);
      }
   }

   public BMenu[] getViewMenus() {
      BMenu menu = new BMenu(lex.getText("templateManager.label"));
      return new BMenu[]{menu};
   }

   public BToolBar getViewToolBar() {
      return new BToolBar();
   }

   HashSet<String> hasOutOfDateTemplate() {
      BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
      int count = model.getRowCount();
      HashSet<String> outOfDateSet = new HashSet<>();

      for (int i = 0; i < count; i++) {
         BTemplateInfo templateInfo = model.get(i);
         if (templateInfo.getState() != 0) {
            outOfDateSet.add(templateInfo.getTemplateName());
         }
      }

      return outOfDateSet;
   }

   void setUpdatedStatus(HashSet<String> updatedSet) {
      BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
      int count = model.getRowCount();

      for (int i = 0; i < count; i++) {
         BTemplateInfo templateInfo = model.get(i);
         if (updatedSet.contains(templateInfo.getTemplateName())) {
            templateInfo.setStatus("Updated");
         }
      }
   }

   protected void doLoadValue(BObject value, Context context) throws Exception {
      this.remoteVersion = (Version)value.fw(404, "template", null, null, null);
      this.isRemotePre4_3 = this.remoteVersion.major() == 4 && this.remoteVersion.minor() < 3;
      if (value instanceof BTemplateService) {
         if (!((BTemplateService)value).getStatus().isValid()) {
            throw new RuntimeException("TemplateService is disabled or not licensed.");
         }

         this.updateTable((BTemplateService)value);
         this.target = value.asComponent();
         this.isTemplateEditor = false;
         this.doUpdateCommands();
      } else if (value instanceof BComponent) {
         this.updateTable(value.asComponent());
         this.target = value.asComponent();
         this.isTemplateEditor = true;
         if (!this.isReadonly()) {
            this.doUpdateCommands();
         }
      }
   }

   private void updateTable(BComponent target) {
      target.lease(2);
      if (this.isRemotePre4_3) {
         if (target instanceof BTemplateService) {
            this.templateService = (BTemplateService)target;
            this.update42Table(this.templateService);
         }
      } else {
         BSpace space = target.getSpace();
         this.isOffline = space instanceof BBogSpace;
         if (target instanceof BTemplateService) {
            this.templateService = (BTemplateService)target;
         }

         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         model.initTable();
         if (!this.isOffline) {
            this.initOnlineTable(this.templateService, model);
         } else {
            this.initOfflineTable(target, model);
         }

         model.initAvailTemplates();
         if (!this.isReadonly()) {
            model.initVersionStatus();
         }

         model.sortByColumn(0, true);
         model.updateTable(true);
         this.tablePane.tableModified();
      }
   }

   private void update42Table(BTemplateService target) {
      this.templateService = target;
      BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
      model.initTable();
      BOrd authority = ((BWbShell)this.getShell()).getActiveOrd();
      BOrd bqlOrd = BOrd.make(authority, this.query()).normalize();
      BITable<?> itable = (BITable<?>)bqlOrd.resolve(target).get();
      ColumnList columns = itable.getColumns();
      TableCursor<?> cursor = itable.cursor();
      Throwable var8 = null;

      try {
         while (cursor.next()) {
            BDataRow dataRow = (BDataRow)cursor.get();
            String slotPath = dataRow.cell(columns.get(0)).toString();
            BObject bObject = BOrd.make(slotPath).resolve(target).get();
            if (bObject instanceof BTemplateConfig) {
               ((BTemplateConfig)bObject).loadSlots();
               ((BTemplateConfig)bObject).clearCacheValues();
               model.add(BTemplateInfo.make((BTemplateConfig)bObject));
            }
         }
      } catch (Throwable var19) {
         var8 = var19;
         throw var19;
      } finally {
         if (cursor != null) {
            if (var8 != null) {
               try {
                  cursor.close();
               } catch (Throwable var18) {
                  var8.addSuppressed(var18);
               }
            } else {
               cursor.close();
            }
         }
      }

      this.table.relayout();
      this.tablePane.tableModified();
   }

   private void initOfflineTable(BComponent target, BTemplateManager.Model model) {
      BComponentSpace componentSpace = target.getComponentSpace();
      BComponent rootComponent = componentSpace.getRootComponent();
      boolean isStation = rootComponent instanceof BStation;
      BTemplateConfig[] templateConfigs = (BTemplateConfig[])CompUtil.getDescendants(rootComponent, BTemplateConfig.class);
      TagDictionaryService tagDictionaryService = componentSpace.getTagDictionaryService();
      componentSpace.setTagDictionaryService(tagDictionaryService);
      if (this.templateService != null) {
         this.templateService.removeAll();

         for (BTemplateConfig tc : templateConfigs) {
            this.templateService.register(tc);
         }

         this.initOnlineTable(this.templateService, model);
      } else {
         for (BTemplateConfig tc : templateConfigs) {
            if (!tc.getPropertyInParent().isFrozen()) {
               BTemplateInfo templateInfo = BTemplateInfo.make(tc);
               BComponent tRoot = tc.getParent().asComponent();
               tRoot.loadSlots();
               BComponent tRootParent = tRoot.getParent().asComponent();
               tRootParent.loadSlots();
               tc.loadSlots();
               tc.clearCacheValues();
               if (isStation || tRoot.getParent().getParent() != null) {
                  model.add(templateInfo);
               }
            }
         }
      }
   }

   private void initOnlineTable(BTemplateService target, BTemplateManager.Model model) {
      if (!this.isRemotePre4_3) {
         for (BTemplateInfo bTemplateInfo : (BTemplateInfo[])target.getChildren(BTemplateInfo.class)) {
            bTemplateInfo.loadSlots();
            OrdTarget ordTarget = null;
            BTemplateConfig tc = null;

            try {
               ordTarget = bTemplateInfo.getLocationOrd().resolve(target);
               tc = (BTemplateConfig)ordTarget.get();
            } catch (Exception var10) {
            }

            if (tc != null) {
               BComponent tRoot = tc.getParent().asComponent();
               tRoot.getParent().asComponent().loadSlots();
               tc.loadSlots();
               tc.clearCacheValues();
               model.add(bTemplateInfo);
            }
         }
      }
   }

   private String query() {
      return "station:|slot:/|bql:select slotPath from template:TemplateConfig";
   }

   private BSearchService getSearchService(BComponent base) {
      if (this.searchService != null) {
         return this.searchService;
      } else {
         BIService service = this.getService(BSearchService.TYPE, base);
         if (service == null) {
            return null;
         } else {
            this.searchService = (BSearchService)service;
            return this.searchService;
         }
      }
   }

   private BTemplateService getTemplateService(BComponent base) {
      if (this.templateService != null) {
         return this.templateService;
      } else {
         BIService service = this.getService(BTemplateService.TYPE, base);
         if (service == null) {
            return null;
         } else {
            this.templateService = (BTemplateService)service;
            return this.templateService;
         }
      }
   }

   private BIService getService(Type serviceType, BComponent base) {
      BComponentSpace space = base.getComponentSpace();
      if (space == null) {
         return null;
      } else {
         BComponent root = space.getRootComponent();
         BServiceContainer[] serviceContainer = (BServiceContainer[])root.getChildren(BServiceContainer.class);
         if (serviceContainer != null && serviceContainer.length != 0) {
            BIService[] services = (BIService[])serviceContainer[0].getChildren(serviceType.getTypeClass());
            return services != null && services.length != 0 ? services[0] : null;
         } else {
            return null;
         }
      }
   }

   public void doUpdateCommands() {
      boolean hasSelections = this.table.getSelection().getRow() != -1;
      TableModel model = this.table.getModel();
      int count = model.getRowCount();
      boolean upgradeAllEnable = false;
      boolean commitEnable = false;

      for (int i = 0; i < count; i++) {
         BTemplateInfo tRow = (BTemplateInfo)model.getSubject(i);
         tRow.lease();
         int versionState = tRow.getState();
         if (versionState == OUT_OF_DATE_SEL) {
            upgradeAllEnable = true;
         } else if (versionState == UPGRADE_SEL || versionState == DOWNGRADE_SEL || versionState == REDEPLOY_SEL) {
            commitEnable = true;
         }
      }

      boolean upgradeEnable = false;
      boolean downgradeEnable = false;
      boolean untemplateEnable = false;
      int[] tRows = this.table.getSelection().getRows();

      for (int ix : tRows) {
         untemplateEnable = true;
         BTemplateInfo tRow = (BTemplateInfo)model.getSubject(ix);
         if (tRow.isSubtemplate()) {
            untemplateEnable = false;
            break;
         }
      }

      upgradeEnable = this.checkTemplateInfoState(tRows, model, OUT_OF_DATE_SEL);
      downgradeEnable = this.checkTemplateInfoState(tRows, model, OLDER_VERSION_AVAILABLE_SEL);
      boolean var18 = this.checkTemplateInfoState(tRows, model, UP_TO_DATE_SEL);
      BButton b2 = (BButton)this.buttonPane.get("b2");
      if (downgradeEnable) {
         b2.setCommand(this.downgradeCmd, true, false);
         this.downgradeCmd.setEnabled(!this.isRemotePre4_3);
      } else if (var18) {
         b2.setCommand(this.redeployCmd, true, false);
         this.redeployCmd.setEnabled(!this.isRemotePre4_3);
      } else {
         b2.setCommand(this.upgradeCmd, true, false);
         this.upgradeCmd.setEnabled(!this.isRemotePre4_3 && upgradeEnable);
      }

      this.upgradeAllCmd.setEnabled(!this.isRemotePre4_3 && !this.commitInProgress && upgradeAllEnable);
      this.commitCmd.setEnabled(!this.isRemotePre4_3 && !this.commitInProgress && commitEnable);
      this.resetCmd.setEnabled(!this.isRemotePre4_3 && !this.commitInProgress && commitEnable);
      this.untemplateCmd.setEnabled(!this.isRemotePre4_3 && !this.commitInProgress && untemplateEnable);
      if (this.isTemplateEditor && this.buttonPane.get("bulkDeploy") != null) {
         this.bulkDeployCmd.setEnabled(false);
         this.buttonPane.remove("bulkDeploy");
         this.updateConfigsCmd.setEnabled(false);
         this.buttonPane.remove("updateConfigs");
      }
   }

   private boolean checkTemplateInfoState(int[] selectedRows, TableModel model, int state) {
      if (selectedRows.length == 0) {
         return false;
      } else {
         for (int i : selectedRows) {
            BTemplateInfo tRow = (BTemplateInfo)model.getSubject(i);
            if (tRow.getState() != state) {
               return false;
            }
         }

         return true;
      }
   }

   private void linkSource(Object source, BComponent target, BTemplateManager.IoSlotInfo slotInfo) {
      if (source != null) {
         if (source instanceof ArrayList && ((ArrayList)source).size() > 0) {
            Slot slot = slotInfo.slot;
            Type targetType = target.get(slot.asProperty()).getType();
            TmplUtil.TargetChoice sourceChoice = (TmplUtil.TargetChoice)((ArrayList)source).get(0);
            String sourceSlotName = "out";
            if (sourceChoice.targetSlotEnum.getOrdinal() != 0) {
               sourceSlotName = sourceChoice.targetSlotEnum.getTag();
            }

            BComponent srcComp = sourceChoice.targetPoint;
            srcComp.loadSlots();
            slotInfo.otherName = srcComp.getSlotPath().toString();
            Property outProp = srcComp.getProperty(sourceSlotName);
            if (outProp != null) {
               Type srcType = outProp.getType();
               LinkCheck linkCheck = target.checkLink(srcComp, outProp, slot, null);
               if (linkCheck.isValid()) {
                  BLink link = target.makeLink(srcComp, outProp, slot, null);
                  target.add(null, link);
                  slotInfo.otherSlot = outProp.getName();
                  slotInfo.results = 4;
               } else {
                  slotInfo.otherName = srcType.toString();
                  slotInfo.otherSlot = targetType.toString();
                  slotInfo.results = 6;
               }
            } else {
               slotInfo.results = 7;
            }
         } else {
            slotInfo.otherName = source.getClass().getName();
            slotInfo.results = 8;
         }
      }
   }

   private void relateSource(Object extEndpoint, BComponent localEndpoint, BTemplateManager.IoSlotInfo slotInfo) {
      if (extEndpoint instanceof ArrayList) {
         for (BComponent endpoint : (ArrayList)extEndpoint) {
            this.relateEndpoint(endpoint, localEndpoint, slotInfo);
         }
      }

      if (extEndpoint instanceof BComponent) {
         this.relateEndpoint((BComponent)extEndpoint, localEndpoint, slotInfo);
      }

      localEndpoint.lease();
   }

   private void relateEndpoint(BComponent extEndpoint, BComponent localEndpoint, BTemplateManager.IoSlotInfo slotInfo) {
      BRelationInfo relateInfo = slotInfo.relationInfo;
      Id relationId = Id.newId(relateInfo.getRelationId());
      BOrd endPointOrd = extEndpoint.getHandleOrd();
      BComponent relationOwner = localEndpoint;
      if (relateInfo.getInbound()) {
         endPointOrd = localEndpoint.getHandleOrd();
         relationOwner = extEndpoint;
      }

      BRelation newRelation = new BRelation(relationId, endPointOrd);
      relationOwner.relations().add(newRelation);
      relationOwner.lease();
      slotInfo.otherName = extEndpoint.getSlotPath().toString();
      slotInfo.otherSlot = relateInfo.getInbound() ? "from" : "to";
      slotInfo.relatedEndpoints.add(extEndpoint);
      slotInfo.results = 24;
   }

   private void linkTarget(Object targets, BComponent parent, BTemplateManager.IoSlotInfo slotInfo) {
      if (targets instanceof ArrayList) {
         Slot slot = slotInfo.slot;
         boolean linked = false;

         for (TmplUtil.TargetChoice targetChoice : (ArrayList)targets) {
            if (targetChoice.targetSlotEnum.getOrdinal() != 0) {
               String targetSlotName = targetChoice.targetSlotEnum.getTag();
               Slot targetSlot = targetChoice.targetPoint.getSlot(targetSlotName);
               BLink link = new BLink(parent.getHandleOrd(), slot.getName(), targetSlot.getName(), true);
               targetChoice.targetPoint.add(null, link);
               targetChoice.targetPoint.lease(1);
               slotInfo.otherName = targetChoice.targetPoint.getSlotPath().toString();
               slotInfo.otherSlot = targetSlot.getName();
               slotInfo.linkedTargets.add(targetChoice);
               slotInfo.results = 14;
               linked = true;
            }
         }

         if (!linked) {
            slotInfo.results = 15;
         }
      }
   }

   private boolean resolveInputBinding(BComponent root) {
      BSearchService searchService = this.getSearchService(root);
      return false;
   }

   private BMenu popupMenu(int row, int column) {
      BMenu menu = new BMenu();
      TableSelection selection = this.table.getSelection();
      int rowCount = selection.getRowCount();
      if (rowCount > 1) {
         menu.add(null, new BTemplateManager.BindSelection(this.table));
         return menu;
      } else {
         BTemplateConfig config = ((BTemplateManager.Model)this.table.getModel()).get(row).getTemplateConfig();
         Slot[] unlinkedInputSlots = config.getUnlinkedInputs();
         Slot[] unlinkedOutputSlots = config.getUnlinkedOutputs();
         ArrayList<BRelationInfo> unrelatedInfos = config.getUnboundRelationInfos();
         ArrayList<BPasswordBinding> defaultPasswords = config.getPasswordInfos();
         boolean needsBindings = unlinkedInputSlots.length + unlinkedOutputSlots.length + unrelatedInfos.size() > 0;
         switch (column) {
            case 0:
               if (!this.isTemplateEditor) {
                  menu.add(null, new BTemplateManager.Goto(row, this.table));
                  menu.add(null, new BTemplateManager.GotoComponent(row, this.table));
               }

               if (needsBindings) {
                  menu.add(null, new BTemplateManager.BindSelection(this.table));
               }
               break;
            case 1:
            case 2:
            case 3:
            default:
               if (!this.isTemplateEditor) {
                  menu.add(null, new BTemplateManager.Goto(row, this.table));
               }
               break;
            case 4:
               if (unlinkedInputSlots.length != 0) {
                  for (Slot slot : unlinkedInputSlots) {
                     menu.add(slot.getName(), new BTemplateManager.Bind(slot, config, true, true));
                  }
               }
               break;
            case 5:
               if (unlinkedOutputSlots.length != 0) {
                  for (Slot slot : unlinkedOutputSlots) {
                     menu.add(slot.getName(), new BTemplateManager.Bind(slot, config, false, true));
                  }
               }
               break;
            case 6:
               if (!unrelatedInfos.isEmpty()) {
                  for (BRelationInfo relateInfo : unrelatedInfos) {
                     BTemplateManager.Bind bindCmd = new BTemplateManager.Bind(relateInfo, config, null);
                     bindCmd.setDisplayBindInfo(true);
                     BMenuItem menuItem = menu.add(SlotPath.escape(relateInfo.getRelationId()), bindCmd);
                     menuItem.setText(relateInfo.getRelationId());
                  }
               }
               break;
            case 7:
               for (BPasswordBinding passwordBinding : defaultPasswords) {
                  if (passwordBinding.getTarget().isPresent()) {
                     menu.add(null, new BTemplateManager.GotoPassword(passwordBinding));
                  }
               }
         }

         return menu;
      }
   }

   private void showInputInfo(BTemplateConfig config) {
      BComponent parent = config.getParent().asComponent();
      Slot[] inputSlots = config.getInputSlots();
      BTemplateManager.IoSlotInfo[] slotInfos = new BTemplateManager.IoSlotInfo[inputSlots.length];
      if (slotInfos.length != 0) {
         for (int i = 0; i < inputSlots.length; i++) {
            slotInfos[i] = new BTemplateManager.IoSlotInfo();
            slotInfos[i].slot = inputSlots[i];
            slotInfos[i].tags = config.getInputSlotTags(inputSlots[i]);
         }

         BFont boldFont = Theme.label().getBoldText();
         boldFont = BFont.make(boldFont, 1);
         BGridPane pane = new BGridPane(2);

         for (BTemplateManager.IoSlotInfo slotInfo : slotInfos) {
            String slotName = slotInfo.slot.getName();
            String linkInfo = lex.getText("templateManager.notLinked");
            String hints = lex.getText("templateManager.bindHintsNotFound");
            BTextField linkField = new BTextField(linkInfo, 40, false);
            BTextField hintsField = new BTextField(hints, 40, false);
            BLabel slotLabel = new BLabel(slotName, BHalign.left);
            slotLabel.setFont(boldFont);
            pane.add(null, slotLabel);
            pane.add(null, new BLabel());
            pane.add(null, new BLabel("     " + lex.getText("templateManager.linkSource"), BHalign.right));
            pane.add(null, linkField);
            pane.add(null, new BLabel("     " + lex.getText("templateManager.bindHints"), BHalign.right));
            pane.add(null, hintsField);
            BLink[] links = parent.getLinks(slotInfo.slot);

            for (BLink link : parent.getLinks(slotInfo.slot)) {
               SlotPath slotPath = link.getEndpointOrd().resolve(config).get().asComponent().getSlotPath();
               linkField.setText(slotPath.toString() + "." + link.getSourceSlotName());
            }

            for (Tag tag : slotInfo.tags) {
               if (tag.getId().getQName().equals("n:bindHints")) {
                  hintsField.setText(tag.getValue().toString());
               }
            }
         }

         int info = BDialog.info((BWidget)this.owner, lex.getText("templateManager.inputInfo"), pane);
      }
   }

   private void showOutputInfo(BTemplateConfig config) {
      BComponent parent = config.getParent().asComponent();
      Slot[] outputSlots = config.getOutputSlots();
      BTemplateManager.IoSlotInfo[] slotInfos = new BTemplateManager.IoSlotInfo[outputSlots.length];
      if (slotInfos.length != 0) {
         for (int i = 0; i < outputSlots.length; i++) {
            slotInfos[i] = new BTemplateManager.IoSlotInfo();
            slotInfos[i].slot = outputSlots[i];
            slotInfos[i].tags = config.getOutputSlotTags(outputSlots[i]);
         }

         BFont boldFont = Theme.label().getBoldText();
         boldFont = BFont.make(boldFont, 1);
         BGridPane pane = new BGridPane(2);

         for (BTemplateManager.IoSlotInfo slotInfo : slotInfos) {
            String slotName = slotInfo.slot.getName();
            BLabel slotLabel = new BLabel(slotName, BHalign.left);
            slotLabel.setFont(boldFont);
            pane.add(null, slotLabel);
            pane.add(null, new BLabel());

            for (Tag tag : slotInfo.tags) {
               pane.add(null, new BLabel("     " + tag.getId().getQName(), BHalign.right));
               if (tag.getValue().getType().is(BMarker.TYPE)) {
                  pane.add(null, new BLabel("Marker"));
               } else {
                  pane.add(null, new BTextField(tag.getValue().toString(), 40, false));
               }
            }

            Knob[] knobs = parent.getKnobs(slotInfo.slot);
            if (knobs.length == 0) {
               String linkInfo = lex.getText("templateManager.notLinked");
               BTextField linkField = new BTextField(linkInfo, 40, false);
               pane.add(null, new BLabel("     " + lex.getText("templateManager.linkTarget"), BHalign.left));
               pane.add(null, linkField);
            } else {
               int j = 1;

               for (Knob knob : knobs) {
                  BOrd targetOrd = knob.getTargetOrd();
                  String targetSlot = knob.getTargetSlotName();
                  SlotPath slotPath = targetOrd.resolve(config).get().asComponent().getSlotPath();
                  String linkInfo = slotPath.toString() + "." + targetSlot;
                  BTextField linkField = new BTextField(linkInfo, 40, false);
                  pane.add(null, new BLabel("     " + lex.getText("templateManager.linkTarget") + j++, BHalign.right));
                  pane.add(null, linkField);
               }
            }
         }

         int info = BDialog.info((BWidget)this.owner, lex.getText("templateManager.outputInfo"), pane);
      }
   }

   private void showRelationInfo(BTemplateConfig config) {
      BComponent parent = config.getParent().asComponent();
      ArrayList<BRelationInfo> relationInfos = config.getRelationInfos();
      if (relationInfos.size() != 0) {
         BFont boldFont = Theme.label().getBoldText();
         boldFont = BFont.make(boldFont, 1);
         BGridPane pane = new BGridPane(2);

         for (BRelationInfo relationInfo : relationInfos) {
            BLabel slotLabel = new BLabel(relationInfo.getRelationId(), BHalign.left);
            slotLabel.setFont(boldFont);
            pane.add(null, slotLabel);
            pane.add(null, new BLabel());
            pane.add(null, new BLabel("     direction", BHalign.right));
            pane.add(null, new BTextField(relationInfo.getInbound() ? "Inbound" : "Outbound", 40, false));
            pane.add(null, new BLabel("     relateHints", BHalign.right));
            pane.add(null, new BTextField(relationInfo.getRelateHints(), 40, false));
            int direction = relationInfo.getInbound() ? 1 : 2;
            Collection<Relation> collection = parent.relations().getAll(Id.newId(relationInfo.getRelationId()), direction);
            if (collection.isEmpty()) {
               String relateInfo = lex.getText("templateManager.notRelated");
               BTextField relateField = new BTextField(relateInfo, 40, false);
               pane.add(null, new BLabel("     " + lex.getText("templateManager.relatedComponent"), BHalign.left));
               pane.add(null, relateField);
            } else {
               int j = 1;

               for (Relation relation : collection) {
                  BOrd targetOrd = ((BComponent)relation.getEndpoint()).getHandleOrd();
                  SlotPath slotPath = targetOrd.resolve(config).get().asComponent().getSlotPath();
                  String linkInfo = slotPath.toString();
                  BTextField linkField = new BTextField(linkInfo, 40, false);
                  pane.add(null, new BLabel("     " + lex.getText("templateManager.relatedComponent") + j++, BHalign.right));
                  pane.add(null, linkField);
               }
            }
         }

         int info = BDialog.info((BWidget)this.owner, lex.getText("templateManager.relateInfo"), pane);
      }
   }

   private static BDynamicEnum getLinkableInputs(BControlPoint point) {
      ArrayList<String> aList = new ArrayList<>();
      aList.add(SlotPath.escape("----"));

      for (String inputSlot : INPUT_SLOTS) {
         Slot slot = point.getSlot(inputSlot);
         if (!Flags.isReadonly(point, slot) && (Flags.isFanIn(point, slot) || !point.isLinkTarget(slot))) {
            aList.add(inputSlot);
         }
      }

      String[] inTags = aList.toArray(new String[0]);
      BEnumRange inRange = BEnumRange.make(inTags);
      return BDynamicEnum.make(0, inRange);
   }

   public void handleComponentEvent(BComponentEvent event) {
      super.handleComponentEvent(event);
      if (this.job == event.getSourceComponent()) {
         final BJob job = (BJob)event.getSourceComponent();
         this.jobBar.handleComponentEvent(event);
         if (job.getJobState().isComplete() && this.jobInProgress) {
            WbViewEventWorker.getInstance().invokeLater(new Runnable() {
               @Override
               public void run() {
                  if (BTemplateManager.this.isRegisteredForComponentEvents(job)) {
                     BTemplateManager.this.registerForComponentEvents(job, Integer.MAX_VALUE);
                  }

                  BTemplateManager.this.jobComplete(job);
               }
            });
         }
      }
   }

   private void jobComplete(BJob job) {
      BUpgradeTemplateJob uJob = (BUpgradeTemplateJob)job;
      Hashtable<Object, BComponent> rootParents = new Hashtable<>();

      for (BOrd bOrd : (BOrd[])this.vector.getChildren(BOrd.class)) {
         try {
            BComponent tc = bOrd.resolve(this.templateService).get().asComponent();
            BComponent root = tc.getParent().asComponent();
            BComponent rootParent = root.getParent().asComponent();
            if (!rootParents.containsKey(rootParent.getHandle())) {
               rootParent.lease(2);
               rootParents.put(rootParent.getHandle(), rootParent);
            }
         } catch (Exception var12) {
         }
      }

      this.templateService.lease(Integer.MAX_VALUE);

      try {
         this.doLoadValue(this.templateService, null);
         this.jobInProgress = false;
      } catch (Exception var11) {
         log.log(Level.WARNING, "Error completing job:" + job.toString(), (Throwable)var11);
      }
   }

   private class BEditComp extends BComponent {
      public String getTypeDisplayName(Context cx) {
         return "";
      }
   }

   public class Bind extends Command {
      BTemplateManager.IoSlotInfo slotInfo;
      BTemplateConfig config;
      boolean displayBindInfo = false;
      Hashtable<String, Object> cache;

      public Bind(Slot slot, BTemplateConfig config, boolean isInput, boolean displayBindInfo, Hashtable<String, Object> cache) {
         super(BTemplateManager.this, slot.getName());
         this.slotInfo = new BTemplateManager.IoSlotInfo(slot, isInput, config);
         this.config = config;
         this.displayBindInfo = displayBindInfo;
         this.cache = cache;
      }

      public Bind(Slot slot, BTemplateConfig config, boolean isInput, boolean displayBindInfo) {
         super(BTemplateManager.this, slot.getName());
         this.slotInfo = new BTemplateManager.IoSlotInfo(slot, isInput, config);
         this.config = config;
         this.displayBindInfo = displayBindInfo;
      }

      public Bind(BRelationInfo relateInfo, BTemplateConfig config, Hashtable<String, Object> cache) {
         super(BTemplateManager.this, SlotPath.escape(relateInfo.getRelationId()));
         this.slotInfo = new BTemplateManager.IoSlotInfo(relateInfo, config);
         this.config = config;
         this.cache = cache;
      }

      public void setDisplayBindInfo(boolean value) {
         this.displayBindInfo = value;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.this.abort = false;
         if (this.displayBindInfo) {
            BTemplateManager.this.bindInfo.clear();
         }

         BComponent parent = this.config.getParent().asComponent();
         BTemplateManager.this.getSearchService(parent);
         if (this.slotInfo.relationInfo != null) {
            String relateHints = this.slotInfo.bindHints;
            if (relateHints != null && !relateHints.isEmpty()) {
               BTemplateManager.this.statusListener.accept(BTemplateManager.lex.getText("deploy.searchForRelatedComp") + " " + relateHints);
               String cacheKey = this.config.getUID() + relateHints;
               Object source = null;
               if (this.cache != null && this.cache.containsKey(cacheKey)) {
                  source = this.cache.get(cacheKey);
                  BTemplateManager.this.relateSource(source, parent, this.slotInfo);
               } else {
                  this.slotInfo.choices = TmplUtil.findMatchingObjects(
                     this.slotInfo, BTemplateManager.this.searchService, BTemplateManager.this.templateService, this.config, BTemplateManager.lex
                  );
                  if (this.slotInfo.choices != null && this.slotInfo.choices.length > 0) {
                     source = TmplUtil.selectRelationSource((BWidget)BTemplateManager.this.owner, this.slotInfo, this.config, true, BTemplateManager.lex);
                     BTemplateManager.this.abort = this.slotInfo.abort;
                     if (!BTemplateManager.this.abort && source != null) {
                        BTemplateManager.this.relateSource(source, parent, this.slotInfo);
                        if (this.slotInfo.lastSelected != null && this.cache != null) {
                           this.cache.put(cacheKey, this.slotInfo.lastSelected);
                        }
                     } else {
                        this.slotInfo.results = 23;
                     }
                  } else {
                     this.slotInfo.results = 22;
                  }
               }
            } else {
               this.slotInfo.results = 21;
            }
         } else if (this.slotInfo.bindHints.isEmpty()) {
            this.slotInfo.results = this.slotInfo.isInput ? 1 : 11;
         } else if (this.slotInfo.isInput) {
            BTemplateManager.this.statusListener.accept(BTemplateManager.lex.getText("deploy.searchForInputComp") + " " + this.slotInfo.bindHints);
            String cacheKey = this.config.getUID() + this.slotInfo.bindHints;
            Object selectInput = null;
            if (this.cache != null && this.cache.containsKey(cacheKey)) {
               selectInput = this.cache.get(cacheKey);
               BTemplateManager.this.linkSource(selectInput, parent, this.slotInfo);
            } else {
               this.slotInfo.choices = TmplUtil.findMatchingObjects(
                  this.slotInfo, BTemplateManager.this.searchService, BTemplateManager.this.templateService, this.config, BTemplateManager.lex
               );
               if (this.slotInfo.choices != null && this.slotInfo.choices.length > 0) {
                  selectInput = TmplUtil.selectInputSource((BWidget)BTemplateManager.this.owner, this.slotInfo, this.config, true, BTemplateManager.lex);
                  BTemplateManager.this.abort = this.slotInfo.abort;
                  if (!BTemplateManager.this.abort && selectInput != null) {
                     BTemplateManager.this.linkSource(selectInput, parent, this.slotInfo);
                     if (this.slotInfo.lastSelected != null && this.cache != null) {
                        this.cache.put(cacheKey, this.slotInfo.lastSelected);
                     }
                  } else {
                     this.slotInfo.results = 3;
                  }
               } else {
                  this.slotInfo.results = 2;
               }
            }
         } else {
            BTemplateManager.this.statusListener.accept(BTemplateManager.lex.getText("deploy.searchForInputComp") + " " + this.slotInfo.bindHints);
            this.slotInfo.choices = TmplUtil.findMatchingObjects(
               this.slotInfo, BTemplateManager.this.searchService, BTemplateManager.this.templateService, this.config, BTemplateManager.lex
            );
            if (this.slotInfo.choices != null && this.slotInfo.choices.length > 0) {
               Object targets = TmplUtil.selectOutputTargets((BWidget)BTemplateManager.this.owner, this.slotInfo, this.config, true, BTemplateManager.lex);
               BTemplateManager.this.abort = this.slotInfo.abort;
               if (!BTemplateManager.this.abort && targets != null) {
                  BTemplateManager.this.linkTarget(targets, parent, this.slotInfo);
               } else {
                  this.slotInfo.results = 13;
               }
            } else {
               this.slotInfo.results = 12;
            }
         }

         if (BTemplateManager.this.abort && BTemplateManager.this.isDeploy) {
            throw new RuntimeException(BTemplateManager.lex.getText("templateManager.deploy.aborted"));
         } else if (BTemplateManager.this.abort) {
            return null;
         } else {
            if (this.config != null) {
               this.config.clearCacheValues();
            }

            TmplUtil.BindInfo results = TmplUtil.formatBindResults(this.slotInfo, BTemplateManager.lex);
            if (results != null) {
               BTemplateManager.this.bindInfo.add(results);
            }

            if (this.displayBindInfo) {
               BTemplateManager.this.displayBindInfo();
            }

            parent.lease();
            return null;
         }
      }
   }

   public class BindSelection extends Command {
      BTable table;

      public BindSelection(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.bindSelection"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.this.bindInfo.clear();
         BTemplateManager.this.abort = false;
         BTemplateManager.this.cache = new Hashtable<>();
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         TableSelection selection = this.table.getSelection();

         try {
            for (int i : selection.getRows()) {
               if (BTemplateManager.this.abort) {
                  break;
               }

               BTemplateConfig config = ((BTemplateInfo)model.getSubject(i)).getTemplateConfig();
               BComponent parent = config.getParent().asComponent();
               BTemplateManager.this.getSearchService(parent);

               for (Slot slot : config.getUnlinkedInputs()) {
                  BTemplateManager.Bind bindCommand = BTemplateManager.this.new Bind(slot, config, true, false, BTemplateManager.this.cache);
                  bindCommand.doInvoke();
                  if (BTemplateManager.this.abort) {
                     break;
                  }
               }

               if (!BTemplateManager.this.abort) {
                  for (Slot slotx : config.getUnlinkedOutputs()) {
                     BTemplateManager.Bind bindCommand = BTemplateManager.this.new Bind(slotx, config, false, false);
                     bindCommand.doInvoke();
                     if (BTemplateManager.this.abort) {
                        break;
                     }
                  }
               }

               if (!BTemplateManager.this.abort) {
                  for (BRelationInfo relateInfo : config.getUnboundRelationInfos()) {
                     BTemplateManager.this.bindRelations(relateInfo, config, BTemplateManager.this.cache);
                     if (BTemplateManager.this.abort) {
                        break;
                     }
                  }
               }

               config.clearCacheValues();
               parent.lease(1);
            }
         } catch (Exception var14) {
            BTemplateManager.log.log(Level.WARNING, "Error running command :" + this.toString(), (Throwable)var14);
         }

         BTemplateManager.this.displayBindInfo();
         return null;
      }
   }

   private class CellRenderer extends TableCellRenderer {
      BIDataTable<?> lastTable = null;
      int statusCol = -1;

      private CellRenderer() {
      }

      public BBrush getForeground(Cell cell) {
         try {
            if (BTemplateManager.this.jobInProgress) {
               return super.getForeground(cell);
            } else {
               BTemplateConfig config = ((BTemplateManager.Model)BTemplateManager.this.table.getModel()).get(cell.row).getTemplateConfig();
               BBrush fg = super.getForeground(cell);
               if (cell.column == 4) {
                  if (config.getUnlinkedInputs().length > 0) {
                     return ((BColor)BStatus.fault.getForegroundColor(null)).toBrush();
                  }
               } else if (cell.column == 5) {
                  if (config.getUnlinkedOutputs().length > 0) {
                     return ((BColor)BStatus.fault.getForegroundColor(null)).toBrush();
                  }
               } else if (cell.column == 6) {
                  if (config.getUnboundRelationInfos().size() > 0) {
                     return ((BColor)BStatus.fault.getForegroundColor(null)).toBrush();
                  }
               } else if (cell.column == 7) {
                  if (config.getPasswordInfos().size() > 0) {
                     return ((BColor)BStatus.fault.getForegroundColor(null)).toBrush();
                  }
               } else {
                  if (cell.column == 9) {
                     BTemplateInfo rowSubject = null;

                     try {
                        rowSubject = ((BTemplateManager.Model)BTemplateManager.this.table.getModel()).get(cell.row);
                     } catch (Exception var6) {
                        return super.getForeground(cell);
                     }

                     if (rowSubject.getModifiedState() < 0) {
                        return super.getForeground(cell);
                     }

                     return ((BColor)BStatus.alarm.getBackgroundColor(null)).toBrush();
                  }

                  if (cell.column == 10) {
                     if (!cell.value.equals(BTemplateManager.OUT_OF_DATE) && !cell.value.equals(BTemplateManager.UPDATED)) {
                        return BColor.blue.toBrush();
                     }

                     return BColor.red.toBrush();
                  }
               }

               return fg;
            }
         } catch (Exception var7) {
            return super.getBackground(cell);
         }
      }

      public BBrush getBackground(Cell cell) {
         try {
            if (BTemplateManager.this.jobInProgress) {
               return super.getBackground(cell);
            } else {
               BTemplateConfig config = ((BTemplateManager.Model)BTemplateManager.this.table.getModel()).get(cell.row).getTemplateConfig();
               BBrush bg = super.getBackground(cell);
               if (cell.column == 4) {
                  if (config.getUnlinkedInputs().length > 0) {
                     return ((BColor)BStatus.fault.getBackgroundColor(null)).toBrush();
                  }
               } else if (cell.column == 5) {
                  if (config.getUnlinkedOutputs().length > 0) {
                     return ((BColor)BStatus.fault.getBackgroundColor(null)).toBrush();
                  }
               } else if (cell.column == 6) {
                  if (config.getUnboundRelationInfos().size() > 0) {
                     return ((BColor)BStatus.fault.getBackgroundColor(null)).toBrush();
                  }
               } else if (cell.column == 7 && config.getPasswordInfos().size() > 0) {
                  return ((BColor)BStatus.fault.getBackgroundColor(null)).toBrush();
               }

               return bg;
            }
         } catch (Exception var4) {
            return super.getBackground(cell);
         }
      }
   }

   public class Commit extends Command {
      BTable table;

      public Commit(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.commit"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         return this.doInvoke(false);
      }

      public CommandArtifact doInvoke(boolean noPrompt) throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();

         try {
            int confirm = 4;
            if (!noPrompt) {
               confirm = BDialog.confirm(
                  BTemplateManager.this.manager,
                  BTemplateManager.lex.getText("TemplateManager.commitTemplate.title"),
                  BTemplateManager.lex.getText("TemplateManager.commitTemplate.message")
               );
            }

            if (confirm == 4) {
               BTemplateManager.this.commitInProgress = true;
               BTemplateManager.this.doUpdateCommands();
               int rowCount = model.getRowCount();
               BTemplateManager.this.vector = new BVector();

               for (int i = 0; i < rowCount; i++) {
                  BTemplateInfo subject = (BTemplateInfo)model.getSubject(i);
                  if (subject.getState() == BTemplateManager.UPGRADE_SEL
                     || subject.getState() == BTemplateManager.DOWNGRADE_SEL
                     || subject.getState() == BTemplateManager.REDEPLOY_SEL) {
                     BTemplateConfig config = subject.getTemplateConfig();
                     BOrd slotPathOrd = subject.getSlotPathOrd();
                     BComponent deployedRoot = config.getParent().asComponent();
                     deployedRoot.loadSlots();
                     TemplateInfo templateInfo = BTemplateManager.this.availTemplates.get(config.getTemplateId());
                     BNtplFile ntplFile = templateInfo.getNtplFile();
                     BWbDeployableNtplFile deployableNtplFile;
                     if (ntplFile instanceof BWbDeployableNtplFile) {
                        deployableNtplFile = (BWbDeployableNtplFile)ntplFile;
                     } else {
                        deployableNtplFile = BWbDeployableNtplFile.make(ntplFile);
                     }

                     if (!BTemplateManager.this.isTemplateEditor && UpdateUtil.updateNtplFile(deployableNtplFile, deployedRoot)) {
                        BTemplateManager.this.vector.add("v?", slotPathOrd);
                     } else {
                        UpgradeUtil.upgrade(deployedRoot, ntplFile);
                        subject.setState(BTemplateManager.UPGRADE_SEL);
                     }
                  }
               }

               if (!BTemplateManager.this.isOffline) {
                  BOrd ordToJob = BTemplateManager.this.templateService.submitUpgradeJob(BTemplateManager.this.vector);
                  BTemplateManager.this.jobInProgress = true;
                  BTemplateManager.this.templateService.getComponentSpace().sync();
                  BTemplateManager.this.job = (BJob)ordToJob.get(BTemplateManager.this.templateService);
                  BTemplateManager.this.registerForComponentEvents(BTemplateManager.this.job);
                  BTemplateManager.this.jobBar.load(BTemplateManager.this.job);
                  BTemplateManager.this.commitInProgress = false;
                  return null;
               }

               for (BOrd bOrd : (BOrd[])BTemplateManager.this.vector.getChildren(BOrd.class)) {
                  OrdTarget ordTarget = bOrd.resolve(BTemplateManager.this.target);
                  if (ordTarget != null) {
                     BObject bObject = ordTarget.get();
                     if (bObject instanceof BTemplateConfig) {
                        BTemplateManager.this.templateConfig = (BTemplateConfig)bObject;
                     } else if (bObject instanceof BTemplateInfo) {
                        BTemplateInfo tmplInfo = (BTemplateInfo)bObject;
                        BTemplateManager.this.templateConfig = tmplInfo.getTemplateConfig();
                     }
                  }

                  if (BTemplateManager.this.templateConfig != null) {
                     UpgradeUtil.upgradeTemplates(BTemplateManager.this.templateConfig);
                  }
               }

               BTemplateManager.this.doLoadValue(BTemplateManager.this.target, null);
            }
         } catch (Exception var13) {
            BTemplateManager.log.log(Level.WARNING, "Error running command :" + this.toString(), (Throwable)var13);
         }

         BTemplateManager.this.commitInProgress = false;
         return null;
      }
   }

   public class ComponentBulkDeploy extends BulkDeploy {
      public ComponentBulkDeploy() {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.bulkDeploy"));
      }

      @Override
      public CommandArtifact doInvoke() throws Exception {
         if (BTemplateManager.this.isTemplateEditor) {
            return null;
         } else {
            this.doDeploy();
            BTemplateManager.this.doLoadValue(BTemplateManager.this.templateService, null);
            return null;
         }
      }
   }

   private class Controller extends TableController {
      private Controller() {
      }

      protected void cellDoubleClicked(BMouseEvent event, int row, int column) {
         BTemplateConfig config = ((BTemplateManager.Model)this.getModel()).get(row).getTemplateConfig();
         switch (column) {
            case 0:
               if (BTemplateManager.this.target instanceof BTemplateService) {
                  BTemplateManager.Goto cmd = BTemplateManager.this.new Goto(row, BTemplateManager.this.table);
                  cmd.invoke();
               }
            case 1:
            case 2:
            case 3:
            default:
               break;
            case 4:
               BTemplateManager.this.showInputInfo(config);
               break;
            case 5:
               BTemplateManager.this.showOutputInfo(config);
               break;
            case 6:
               BTemplateManager.this.showRelationInfo(config);
         }
      }

      protected void cellPopup(BMouseEvent event, int row, int column) {
         BMenu menu = BTemplateManager.this.popupMenu(row, column);
         if (menu.getItemCount() > 0) {
            menu.open(this.getTable(), event.getX(), event.getY());
         }
      }
   }

   public class Downgrade extends Command {
      BTable table;

      public Downgrade(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.downgrade"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();

         for (int i : this.table.getSelection().getRows()) {
            BTemplateInfo templateRow = model.get(i);
            if (templateRow.getStatus().equals(BTemplateManager.OLDER_VERSION_AVAILABLE)) {
               String rowVersion = templateRow.getAvailVersion();
               if (rowVersion == null || rowVersion.isEmpty()) {
                  rowVersion = model.getLatestVersion(templateRow.getTemplateConfig());
               }

               templateRow.setStatus("Downgrade to " + rowVersion);
               templateRow.setState(BTemplateManager.DOWNGRADE_SEL);

               for (Relation relation : templateRow.relations().getAll(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID)) {
                  Entity endpoint = relation.getEndpoint();
                  if (endpoint instanceof BTemplateInfo) {
                     BTemplateInfo subTmpl = (BTemplateInfo)endpoint;
                     String subTmplVersion = subTmpl.getAvailVersion();
                     if (subTmplVersion == null || subTmplVersion.isEmpty()) {
                        subTmplVersion = model.getLatestVersion(subTmpl.getTemplateConfig());
                     }

                     subTmpl.setState(BTemplateManager.DOWNGRADE_SEL);
                     subTmpl.setStatus("Downgrade to " + subTmplVersion);
                  }
               }
            }
         }

         BTemplateManager.this.doUpdateCommands();
         return null;
      }
   }

   public class Goto extends Command {
      int row;
      BTable table;

      public Goto(int row, BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.goto"));
         this.row = row;
         this.table = table;
      }

      public BOrd getTargetOrd() {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         BTemplateInfo templateInfo = model.get(this.row);
         return templateInfo.getTemplateRoot().getHandleOrd();
      }

      public CommandArtifact doInvoke() {
         BOrd targetOrd = this.getTargetOrd();
         if (targetOrd == null) {
            return null;
         } else {
            if ((BWbShell)BTemplateManager.this.getShell() != null) {
               BOrd authority = ((BWbShell)BTemplateManager.this.getShell()).getActiveOrd();
               BOrd hyperlinkOrd = BOrd.make(authority, targetOrd).normalize();
               ((BWbShell)this.getShell()).hyperlink(hyperlinkOrd);
            }

            return null;
         }
      }
   }

   public class GotoComponent extends Command {
      int row;
      BTable table;

      public GotoComponent(int row, BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.gotoComponent"));
         this.row = row;
         this.table = table;
      }

      public BOrd getTargetOrd() {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         BComponent root = model.get(this.row).getTemplateParent();
         return BOrd.make("slot:" + root.toPathString());
      }

      public CommandArtifact doInvoke() {
         BOrd targetOrd = this.getTargetOrd();
         if (targetOrd == null) {
            return null;
         } else {
            BOrd authority = ((BWbShell)BTemplateManager.this.getShell()).getActiveOrd();
            BOrd hyperlinkOrd = BOrd.make(authority, targetOrd).normalize();
            ((BWbShell)this.getShell()).hyperlink(hyperlinkOrd);
            return null;
         }
      }
   }

   private class GotoPassword extends Command {
      BPasswordBinding pswBinding;

      public GotoPassword(BPasswordBinding pswBinding) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.gotoPsw"));
         this.pswBinding = pswBinding;
      }

      BOrd getTargetOrd() {
         return this.pswBinding.getPswOrd();
      }

      public CommandArtifact doInvoke() {
         BOrd targetOrd = this.getTargetOrd();
         if (targetOrd == null) {
            return null;
         } else {
            BOrd authority = ((BWbShell)BTemplateManager.this.getShell()).getActiveOrd();
            BOrd hyperlinkOrd = BOrd.make(authority, targetOrd).normalize();
            ((BWbShell)this.getShell()).hyperlink(hyperlinkOrd);
            return null;
         }
      }
   }

   public static class IoSlotInfo {
      public Slot slot;
      public BRelationInfo relationInfo;
      public Tags tags;
      public boolean isInput;
      public boolean allowMultiple;
      public Object[] choices;
      public Object lastSelected;
      public ArrayList<TmplUtil.TargetChoice> linkedTargets;
      public ArrayList<BComponent> relatedEndpoints;
      public String bindHints;
      public String userTip;
      public String targetSlotHints;
      public String slotPathScope;
      public String title;
      public String prompt;
      public String rootName;
      public String otherName;
      public String otherSlot;
      public boolean abort;
      public boolean dontAskAgain;
      public int results;
      public List<String> resultsMessages;

      IoSlotInfo() {
      }

      IoSlotInfo(Slot slot, boolean isInput, BTemplateConfig config) {
         this.slot = slot;
         Tags slotTags = isInput ? config.getInputSlotTags(slot) : config.getOutputSlotTags(slot);
         Optional<BIDataValue> optBindHints = slotTags.get(BNiagaraTagDictionary.BIND_HINTS);
         Optional<BIDataValue> optUserTip = slotTags.get(BNiagaraTagDictionary.USER_TIP);
         Optional<BIDataValue> optTargetSlotHint = slotTags.get(BNiagaraTagDictionary.TARGET_SLOT_HINT);
         Optional<BIDataValue> optSlotPathScope = slotTags.get(BTemplateManager.SLOT_PATH_SCOPE);
         this.bindHints = optBindHints.isPresent() ? optBindHints.get().toString() : "";
         this.userTip = optUserTip.isPresent() ? optUserTip.get().toString() : "";
         this.targetSlotHints = optTargetSlotHint.isPresent() ? optTargetSlotHint.get().toString() : "";
         this.slotPathScope = optSlotPathScope.isPresent() ? optSlotPathScope.get().toString() : "";
         this.rootName = config.getParent().getName();
         this.linkedTargets = new ArrayList<>();
         this.title = isInput
            ? BTemplateManager.lex.getText("templateManager.input.multipleSources.title")
            : BTemplateManager.lex.getText("templateManager.output.select.title");
         this.isInput = isInput;
         this.allowMultiple = !isInput;
         if (slot != null) {
            this.prompt = this.rootName + "." + slot.getName();
         }

         this.abort = false;
         this.dontAskAgain = false;
         this.resultsMessages = new ArrayList<>();
      }

      IoSlotInfo(BRelationInfo relateInfo, BTemplateConfig config) {
         this.relationInfo = relateInfo;
         this.bindHints = relateInfo.getRelateHints();
         this.slotPathScope = relateInfo.getSlotPathScope();
         this.userTip = relateInfo.getUserTip();
         this.isInput = relateInfo.getInbound();
         this.allowMultiple = true;
         this.rootName = config.getParent().getName();
         this.relatedEndpoints = new ArrayList<>();
         this.prompt = this.rootName + " <" + (this.isInput ? "Input " : "Output ") + relateInfo.getRelationId() + ">";
         this.title = BTemplateManager.lex.getText("templateManager.relation.select.title");
         this.abort = false;
         this.dontAskAgain = false;
         this.resultsMessages = new ArrayList<>();
      }
   }

   public class Model extends TableModel {
      final ArrayList<BTemplateInfo> deployedTemplates = new ArrayList<>();

      private void initTable() {
         synchronized (this.deployedTemplates) {
            this.deployedTemplates.clear();
         }

         if (BTemplateManager.this.availTemplates == null) {
            BTemplateManager.this.availTemplates = new Hashtable<>();
         } else {
            BTemplateManager.this.availTemplates.clear();
         }
      }

      private void add(BTemplateInfo tmplInfo) {
         synchronized (this.deployedTemplates) {
            this.deployedTemplates.add(tmplInfo);
         }
      }

      private BTemplateInfo get(int row) {
         synchronized (this.deployedTemplates) {
            return this.deployedTemplates.get(row);
         }
      }

      public BHalign getColumnAlignment(int col) {
         switch (col) {
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
               return BHalign.center;
            case 8:
            default:
               return BHalign.left;
         }
      }

      public boolean isColumnSortable(int col) {
         return col == 0;
      }

      public synchronized void sortByColumn(int col, boolean ascending) {
         synchronized (this.deployedTemplates) {
            if (col == 0 && this.deployedTemplates.size() != 0) {
               String[] names = new String[this.getRowCount()];

               for (int r = 0; r < names.length; r++) {
                  BTemplateInfo templateInfo = this.get(r);
                  names[r] = templateInfo.getTemplateRoot().getSlotPath().getBody();
               }

               BTemplateInfo[] current = this.deployedTemplates.toArray(new BTemplateInfo[0]);
               BTemplateInfo[] temp = new BTemplateInfo[this.deployedTemplates.size()];
               System.arraycopy(current, 0, temp, 0, names.length);
               SortUtil.sort(names, temp, ascending);
               this.deployedTemplates.clear();
               this.deployedTemplates.addAll(Arrays.asList(temp));
            }
         }
      }

      public int getRowCount() {
         synchronized (this.deployedTemplates) {
            return this.deployedTemplates.size();
         }
      }

      public int getColumnCount() {
         return 11;
      }

      public String getColumnName(int col) {
         switch (col) {
            case 0:
               return BTemplateManager.lex.getText("templateManager.root");
            case 1:
               return BTemplateManager.lex.getText("templateManager.name");
            case 2:
               return BTemplateManager.lex.getText("templateManager.vendor");
            case 3:
               return BTemplateManager.lex.getText("templateManager.version");
            case 4:
               return BTemplateManager.lex.getText("templateManager.unboundInputs");
            case 5:
               return BTemplateManager.lex.getText("templateManager.unboundOutputs");
            case 6:
               return BTemplateManager.lex.getText("templateManager.unboundRelations");
            case 7:
               return BTemplateManager.lex.getText("templateManager.passwords");
            case 8:
               return BTemplateManager.lex.getText("templateManager.availVersion");
            case 9:
               return BTemplateManager.lex.getText("templateManager.modified");
            default:
               return "";
         }
      }

      public Object getValueAt(int row, int col) {
         BTemplateInfo rowSubject = null;

         try {
            rowSubject = this.get(row);
            BTemplateConfig tmplConfig = rowSubject.getTemplateConfig();
            BComponent root = tmplConfig.getParent().asComponent();
            root.lease();
            switch (col) {
               case 0:
                  if (BTemplateManager.this.jobInProgress) {
                     return "-";
                  } else {
                     String path = root.getSlotPath().getBody();
                     int depth = rowSubject.getSubtemplateDepth();
                     if (depth == 0) {
                        return path;
                     } else {
                        int index = path.lastIndexOf(47);
                        if (index < 0) {
                           return path;
                        }

                        StringBuilder sb = new StringBuilder();

                        for (int i = 0; i < depth; i++) {
                           sb.append("  ");
                        }

                        sb.append("..");
                        sb.append(path.substring(index));
                        return sb.toString();
                     }
                  }
               case 1:
                  return tmplConfig.getTemplateName();
               case 2: {
                  Optional<BIDataValue> valueOptional = root.tags().get(TemplateConst.TEMPLATE_VENDOR_TAG_ID);
                  return valueOptional.isPresent() ? valueOptional.get() : "";
               }
               case 3: {
                  Optional<BIDataValue> valueOptional = root.tags().get(TemplateConst.TEMPLATE_VERSION_TAG_ID);
                  return valueOptional.isPresent() ? valueOptional.get() : "";
               }
               case 4:
                  return tmplConfig.getInputSlots().length;
               case 5:
                  return tmplConfig.getOutputSlots().length;
               case 6:
                  return tmplConfig.getRelationInfos().size();
               case 7:
                  return tmplConfig.getPasswordInfos().size();
               case 8:
                  String rowVersion = rowSubject.getAvailVersion();
                  if (rowVersion == null || rowVersion.isEmpty()) {
                     rowVersion = this.getLatestVersion(tmplConfig);
                  }

                  return rowSubject.getVendor() + " " + rowVersion;
               case 9:
                  int modifiedState = rowSubject.getModifiedState();
                  if (modifiedState < 0) {
                     return "-";
                  } else {
                     if (modifiedState == 0) {
                        return "";
                     }

                     return "X";
                  }
               case 10:
                  return rowSubject.getStatus();
               default:
                  return "";
            }
         } catch (Exception var13) {
            return "-";
         }
      }

      private String getLatestVersion(BTemplateConfig tmplConfig) {
         TemplateInfo templateInfo = BTemplateManager.this.availTemplates.get(tmplConfig.getUID());
         if (templateInfo == null) {
            return "";
         } else {
            BVersion latestVersion = new BVersion(templateInfo.getVendor(), templateInfo.getVersion());
            return latestVersion.getVendorVersionString();
         }
      }

      private int getAvailState(BTemplateInfo templateInfo) {
         BTemplateConfig tmplConfig = templateInfo.getTemplateConfig();
         BVersion installedVersion = tmplConfig.getVersion();
         BVersion selectedVersion = new BVersion(templateInfo.getVendor(), templateInfo.getAvailVersion());
         TemplateInfo selectedInfo = BTemplateManager.this.availTemplates.get(tmplConfig.getTemplateId());
         if (selectedInfo == null) {
            return BTemplateManager.NOT_AVAILABLE_SEL;
         } else {
            int results = selectedVersion.compareTo(installedVersion);
            if (results > 0) {
               return BTemplateManager.OUT_OF_DATE_SEL;
            } else if (results == 0) {
               BAbsTime lastModified = selectedInfo.getLastModified();
               BAbsTime versionDate = tmplConfig.getVersionDate();
               if (lastModified != null && versionDate != null) {
                  long delta = lastModified.getMillis() - versionDate.getMillis();
                  if (delta > 1000L) {
                     return BTemplateManager.OUT_OF_DATE_SEL;
                  }
               }

               return BTemplateManager.UP_TO_DATE_SEL;
            } else {
               return results < 0 ? BTemplateManager.OLDER_VERSION_AVAILABLE_SEL : BTemplateManager.UNKNOWN_SEL;
            }
         }
      }

      public Object getSubject(int row) {
         return this.deployedTemplates.get(row);
      }

      public BImage getRowIcon(int row) {
         try {
            if (BTemplateManager.this.jobInProgress) {
               return super.getRowIcon(row);
            } else {
               BComponent root = this.get(row).getTemplateConfig().getParent().asComponent();
               Optional<BIDataValue> valueOptional = root.tags().get(TemplateConst.TEMPLATE_ICON_TAG_ID);
               return valueOptional.isPresent() ? BImage.make((BOrd)valueOptional.get()) : BTemplateManager.DEFAULT_IMAGE;
            }
         } catch (Exception var4) {
            return super.getRowIcon(row);
         }
      }

      public boolean isOutOfDate(int row) {
         return this.getValueAt(row, 9).equals(BTemplateManager.OUT_OF_DATE);
      }

      public void initAvailTemplates() {
         TemplateManager tmInstance = new TemplateManager();
         tmInstance.initTemplateMap();

         for (int row = 0; row < this.deployedTemplates.size(); row++) {
            BTemplateConfig tc = this.deployedTemplates.get(row).getTemplateConfig();
            BComponent parent = tc.getParent().asComponent();
            parent.lease();
            Optional<BIDataValue> optVendor = parent.tags().get(TemplateConst.TEMPLATE_VENDOR_TAG_ID);
            Optional<BIDataValue> optVersion = parent.tags().get(TemplateConst.TEMPLATE_VERSION_TAG_ID);
            Optional<BIDataValue> optUID = parent.tags().get(TemplateConst.TEMPLATE_UID_TAG_ID);
            Object key = tc.getTemplateId();
            if (!BTemplateManager.this.availTemplates.contains(key) && optVendor.isPresent()) {
               String tVendor = optVendor.get().toString();
               TemplateInfo templateInfo = null;
               if (key instanceof String) {
                  templateInfo = tmInstance.getTemplate((String)key, tVendor);
               } else if (key instanceof BUuid) {
                  templateInfo = tmInstance.getTemplate((BUuid)key, tVendor);
               }

               if (templateInfo != null && !BTemplateManager.this.availTemplates.contains(key)) {
                  BTemplateManager.this.availTemplates.put(key, templateInfo);
               }
            }
         }
      }

      public void initVersionStatus() {
         for (BTemplateInfo templateRow : this.deployedTemplates) {
            BTemplateConfig tc = templateRow.getTemplateConfig();
            templateRow.setAvailVersion(this.getLatestVersion(tc));
            templateRow.setState(this.getAvailState(templateRow));
            templateRow.setStatus(BTemplateManager.VERSION_STATUS[templateRow.getState()]);
         }
      }
   }

   public class Redeploy extends Command {
      BTable table;

      public Redeploy(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.redeploy"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();

         for (int i : this.table.getSelection().getRows()) {
            BTemplateInfo templateRow = model.get(i);
            templateRow.lease();
            if (templateRow.getStatus().equals(BTemplateManager.UP_TO_DATE)) {
               String rowVersion = templateRow.getAvailVersion();
               if (rowVersion == null || rowVersion.isEmpty()) {
                  rowVersion = model.getLatestVersion(templateRow.getTemplateConfig());
               }

               templateRow.setStatus("Redeploy " + rowVersion);
               templateRow.setState(BTemplateManager.REDEPLOY_SEL);

               for (Relation relation : templateRow.relations().getAll(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID)) {
                  if (!relation.isInbound()) {
                     Entity endpoint = relation.getEndpoint();
                     if (endpoint instanceof BTemplateInfo) {
                        BTemplateInfo subTmpl = (BTemplateInfo)endpoint;
                        String subTmplVersion = subTmpl.getAvailVersion();
                        if (subTmplVersion == null || subTmplVersion.isEmpty()) {
                           subTmplVersion = model.getLatestVersion(subTmpl.getTemplateConfig());
                        }

                        subTmpl.setStatus("Redeploy " + subTmplVersion);
                        subTmpl.lease();
                     }
                  }
               }
            }
         }

         BTemplateManager.this.doUpdateCommands();
         return null;
      }
   }

   public class Reset extends Command {
      BTable table;

      public Reset(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.reset"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         model.initVersionStatus();
         BTemplateManager.this.doUpdateCommands();
         BTemplateManager.this.tablePane.tableModified();
         return null;
      }
   }

   public class Untemplate extends Command {
      BTable table;

      public Untemplate(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.untemplate"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BGridPane gridPane = new BGridPane(1);
         gridPane.setHalign(BHalign.right);
         BCheckBox rcsCb = new BCheckBox(BTemplateManager.lex.getText("templateManager.removeCompositeSlots"));
         rcsCb.setHalign(BHalign.right);
         BLabel warning = new BLabel(BTemplateManager.lex.getText("templateManager.untemplateWarning"));
         warning.setHalign(BHalign.left);
         gridPane.add(null, warning);
         gridPane.add(null, new BLabel());
         gridPane.add(null, rcsCb);
         rcsCb.setSelected(true);
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         BOptionDialog dlg = new BOptionDialog(
            BTemplateManager.this.manager,
            BTemplateManager.lex.getText("templateManager.untemplate"),
            gridPane,
            12,
            BImage.make("module://icons/x32/warning.png"),
            null
         );
         dlg.computePreferredSize();
         dlg.setBoundsCenteredOnOwner();
         dlg.open();
         if (dlg.getResult() == 4) {
            for (int i : this.table.getSelection().getRows()) {
               BTemplateInfo templateRow = model.get(i);
               BTemplateConfig templateConfig = templateRow.getTemplateConfig();
               BComponent root = templateConfig.getParent().asComponent();
               this.untemplate(templateConfig, rcsCb.isSelected());

               for (BTemplateConfig subtemplateConfig : (BTemplateConfig[])CompUtil.getDescendants(root, BTemplateConfig.class)) {
                  this.untemplate(subtemplateConfig, rcsCb.isSelected());
               }
            }

            BTemplateManager.this.updateTable(BTemplateManager.this.target);
            BTemplateManager.this.doUpdateCommands();
         }

         return null;
      }

      void untemplate(BTemplateConfig templateConfig, boolean removeCompositeSlots) {
         BComponent root = templateConfig.getParent().asComponent();
         BTemplateManager.log.info("Un-template " + root.getSlotPath());
         root.lease();
         root.remove(templateConfig);
         if (root.getSlot("icon") != null) {
            root.remove("icon");
         }

         if (root.getSlot(BTemplateManager.ROOT_TAG_NAME) != null) {
            root.remove(BTemplateManager.ROOT_TAG_NAME);
         }

         if (root.getSlot(BTemplateManager.NTPL_FILE_TAG_NAME) != null) {
            root.remove(BTemplateManager.NTPL_FILE_TAG_NAME);
         }

         if (root.getSlot(BTemplateManager.VENDOR_TAG_NAME) != null) {
            root.remove(BTemplateManager.VENDOR_TAG_NAME);
         }

         if (root.getSlot(BTemplateManager.VERSION_TAG_NAME) != null) {
            root.remove(BTemplateManager.VERSION_TAG_NAME);
         }

         if (root.getSlot(BTemplateManager.INFO_TAG_NAME) != null) {
            root.remove(BTemplateManager.INFO_TAG_NAME);
         }

         root.getParent().loadSlots();
         if (removeCompositeSlots) {
            for (BLink link : root.getLinks()) {
               if (LinkUtil.isCompositeLink(link)) {
                  String targetSlot = link.getTargetSlotName();
                  String sourceSlot = link.getSourceSlotName();
                  boolean compositeSlotRemoved = false;

                  for (Knob knob : root.getKnobs(root.getSlot(targetSlot))) {
                     BComponent targetComponent = knob.getTargetOrd().resolve(root).get().asComponent();
                     String knobTargetSlot = knob.getTargetSlotName();
                     root.remove(targetSlot);
                     BTemplateManager.log.fine("removing linked output composite slot: " + targetSlot);
                     compositeSlotRemoved = true;
                     BLink newLink = new BLink(link.getSourceOrd(), sourceSlot, knobTargetSlot, true);
                     targetComponent.add("l?", newLink);
                     BTemplateManager.log.fine("added link: " + targetComponent.getSlotPath() + "." + knobTargetSlot);
                  }

                  if (!compositeSlotRemoved) {
                     root.remove(targetSlot);
                     BTemplateManager.log.fine("removed unlinked output composite slot: " + targetSlot);
                  }
               }
            }

            for (Knob knob : root.getKnobs()) {
               String targetSlotName = knob.getTargetSlotName();
               String sourceSlotName = knob.getSourceSlotName();
               Slot sourceSlot = root.getSlot(sourceSlotName);
               if (Flags.isComposite(root, sourceSlot)) {
                  boolean compositeSlotRemoved = false;
                  BComponent targetComponent = knob.getTargetOrd().resolve(root).get().asComponent();

                  for (BLink extLink : root.getLinks(sourceSlot)) {
                     String linkSourceSlot = extLink.getSourceSlotName();
                     root.remove(sourceSlot.asProperty());
                     BTemplateManager.log.fine("removing linked input composite slot: " + sourceSlotName);
                     compositeSlotRemoved = true;
                     BLink newLink = new BLink(extLink.getSourceOrd(), linkSourceSlot, targetSlotName, true);
                     targetComponent.add("l?", newLink);
                     BTemplateManager.log.fine("added link to: " + targetComponent.getSlotPath() + '.' + targetSlotName);
                  }

                  if (!compositeSlotRemoved) {
                     root.remove(sourceSlot.asProperty());
                     BTemplateManager.log.fine("removing unlinked input composite slot: " + sourceSlotName);
                  }
               }
            }
         }
      }
   }

   public class Upgrade extends Command {
      BTable table;

      public Upgrade(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.update"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();

         for (int i : this.table.getSelection().getRows()) {
            BTemplateInfo templateRow = model.get(i);
            if (templateRow.getState() == BTemplateManager.OUT_OF_DATE_SEL || templateRow.getState() == BTemplateManager.UPGRADE_SEL) {
               String rowVersion = templateRow.getAvailVersion();
               if (rowVersion == null || rowVersion.isEmpty()) {
                  rowVersion = model.getLatestVersion(templateRow.getTemplateConfig());
               }

               templateRow.setStatus("Upgrade to " + rowVersion);
               templateRow.setState(BTemplateManager.UPGRADE_SEL);

               for (Relation relation : templateRow.relations().getAll(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID)) {
                  if (!relation.isInbound()) {
                     Entity endpoint = relation.getEndpoint();
                     if (endpoint instanceof BTemplateInfo) {
                        BTemplateInfo subTmpl = (BTemplateInfo)endpoint;
                        String subTmplVersion = subTmpl.getAvailVersion();
                        if (subTmplVersion == null || subTmplVersion.isEmpty()) {
                           subTmplVersion = model.getLatestVersion(subTmpl.getTemplateConfig());
                        }

                        subTmpl.setStatus("Upgrade to " + subTmplVersion);
                     }
                  }
               }
            }
         }

         BTemplateManager.this.doUpdateCommands();
         return null;
      }
   }

   public class UpgradeAll extends Command {
      BTable table;

      public UpgradeAll(BTable table) {
         super(BTemplateManager.this, BTemplateManager.lex.get("templateManager.updateAll"));
         this.table = table;
      }

      public CommandArtifact doInvoke() throws Exception {
         BTemplateManager.Model model = (BTemplateManager.Model)this.table.getModel();
         int rowCount = model.getRowCount();

         for (int i = 0; i < rowCount; i++) {
            BTemplateInfo templateRow = model.get(i);
            if (templateRow.getState() == BTemplateManager.OUT_OF_DATE_SEL || templateRow.getState() == BTemplateManager.UPGRADE_SEL) {
               String rowVersion = templateRow.getAvailVersion();
               if (rowVersion == null || rowVersion.isEmpty()) {
                  rowVersion = model.getLatestVersion(templateRow.getTemplateConfig());
               }

               templateRow.setStatus("Upgrade to " + rowVersion);
               templateRow.setState(BTemplateManager.UPGRADE_SEL);

               for (Relation relation : templateRow.relations().getAll(TemplateConst.TEMPLATE_SUBTEMPLATE_TAG_ID, 2)) {
                  Entity endpoint = relation.getEndpoint();
                  if (endpoint instanceof BTemplateInfo) {
                     BTemplateInfo subTmpl = (BTemplateInfo)endpoint;
                     String subTmplVersion = subTmpl.getAvailVersion();
                     if (subTmplVersion == null || subTmplVersion.isEmpty()) {
                        subTmplVersion = model.getLatestVersion(subTmpl.getTemplateConfig());
                     }

                     subTmpl.setStatus("Upgrade to " + subTmplVersion);
                  }
               }
            }
         }

         BTemplateManager.this.doUpdateCommands();
         return null;
      }
   }
}
