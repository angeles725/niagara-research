package com.tridium.template.ui;

import com.tridium.excel.ui.ExcelUiUtils;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.UpgradeUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.ui.BDialog;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.util.BFormat;
import javax.baja.util.BNameMap;

public class UpdateConfigs extends BulkDeploy {
   private static final int DEPTH_FROM_STATION_TO_APPLICATION_SERVICE_TEMPLATE_CONFIG = 4;

   public UpdateConfigs(BWidget owner, String label) {
      super(owner, label);
   }

   @Override
   public CommandArtifact doInvoke() throws Exception {
      if (ExcelUiUtils.informIfNoExcelSupportIsInstalledLocally(this.getOwner())) {
         return null;
      } else {
         LOG.log(this.IMPORT_LOG_LEVEL, "Update Configurations");
         BComponent root = this.initBulkDeploy();
         if (root == null) {
            return null;
         } else {
            BulkDeployWorkbook deployWorkbook = this.getDeployWorkbook();
            if (deployWorkbook == null) {
               return null;
            } else {
               List<BulkDeployUtil.DeployedWorksheet> deployedWorksheets = BulkDeployUtil.loadDeployedWorksheets(deployWorkbook);

               for (BulkDeployUtil.DeployedWorksheet deployedWorksheet : deployedWorksheets) {
                  for (BulkDeployUtil.DeployedRoot deployedRoot : deployedWorksheet.deployedRoots) {
                     BComponent deployedTemplateComponent = this.findTemplateInstance(deployedWorksheet, deployedRoot, root);
                     if (deployedTemplateComponent != null && !(deployedTemplateComponent instanceof BStation)) {
                        UpgradeUtil.deleteTemplateInputLinks(deployedTemplateComponent, deployedWorksheet.inputDefs);
                        UpgradeUtil.deleteTemplateOutputLinks(deployedTemplateComponent, deployedWorksheet.outputDefs);
                        UpgradeUtil.deleteTemplateInputRelations(deployedTemplateComponent, deployedWorksheet.relationDefs);
                        UpgradeUtil.deleteTemplateOutputRelations(deployedTemplateComponent, deployedWorksheet.relationDefs);
                     }
                  }
               }

               HashMap<BComponent, BNameMap> displayNames = new HashMap<>();
               int deployedRootCount = 0;
               int updatedRootCount = 0;
               StringBuilder errorDetailMessage = new StringBuilder();

               for (BulkDeployUtil.DeployedWorksheet deployedWorksheet : deployedWorksheets) {
                  for (BulkDeployUtil.DeployedRoot deployedRootx : deployedWorksheet.deployedRoots) {
                     deployedRootCount++;
                     BComponent deployedTemplateComponent = this.findTemplateInstance(deployedWorksheet, deployedRootx, root);
                     if (deployedTemplateComponent == null) {
                        errorDetailMessage.append('\n');
                        errorDetailMessage.append(deployedRootx.getParentComponentSlotPath());
                        errorDetailMessage.append('/');
                        errorDetailMessage.append(deployedRootx.getDeployName());
                     } else {
                        updatedRootCount++;
                        deployedRootx.setDeployedTemplate(deployedTemplateComponent);

                        for (String response : this.deployUtil.updateConfigurations(deployedWorksheet, deployedRootx)) {
                           errorDetailMessage.append('\n');
                           errorDetailMessage.append(deployedRootx.getParentComponentSlotPath() + '/' + deployedRootx.getDeployName() + ": " + response);
                        }

                        if (!(deployedTemplateComponent instanceof BStation)) {
                           BulkDeployUtil.setComponentPosition(deployedTemplateComponent, deployedRootx.getPosition());
                           HashMap<String, BFormat> componentDisplayNames = new HashMap<>();
                           BComponent deployedRootComponent = this.resolveDeployComponent(deployedWorksheet, deployedRootx, root);
                           if (deployedRootx.getDisplayName() != null && !deployedRootx.getDisplayName().isEmpty()) {
                              String escapedName = SlotPath.escape(deployedRootx.getDeployName());

                              try {
                                 componentDisplayNames.put(
                                    deployedRootComponent.getProperty(escapedName).getName(), BFormat.make(deployedRootx.getDisplayName())
                                 );
                              } catch (Exception var18) {
                                 LOG.log(
                                    Level.WARNING,
                                    LEX.getText(
                                       "bulkDeploy.excelImport.displayNameError", new Object[]{deployedRootx.getDisplayName(), deployedRootx.getDeployName()}
                                    ),
                                    (Throwable)var18
                                 );
                              }
                           }

                           if (!componentDisplayNames.isEmpty()) {
                              BNameMap nameMap = BNameMap.make(componentDisplayNames);
                              if (displayNames.containsKey(deployedRootComponent)) {
                                 BNameMap mergedNameMap = BNameMap.make(displayNames.get(deployedRootComponent), nameMap);
                                 displayNames.put(deployedRootComponent, mergedNameMap);
                              } else {
                                 displayNames.put(deployedRootComponent, nameMap);
                              }
                           }
                        }
                     }
                  }
               }

               if (!displayNames.isEmpty()) {
                  displayNames.forEach((key, value) -> {
                     LOG.log(this.IMPORT_LOG_LEVEL, String.format("Component: %s, Updated display name: %s", key.getName(), value.encodeToString()));
                     this.deployUtil.updateTemplateDisplayNames(key, value);
                  });
               }

               String details = errorDetailMessage.toString();
               if (deployedRootCount == updatedRootCount) {
                  switch (deployedRootCount) {
                     case 0:
                        BDialog.error(this.getOwner(), LEX.getText("bulkDeploy.excelImport.title"), LEX.getText("bulkDeploy.excelImport.configsUpdatedForNone"));
                        break;
                     case 1:
                        if (details.isEmpty()) {
                           BDialog.message(
                              this.getOwner(), LEX.getText("bulkDeploy.excelImport.title"), LEX.getText("bulkDeploy.excelImport.configsUpdatedForOne")
                           );
                        } else {
                           BTextEditorPane resultsPane = new BTextEditorPane(
                              LEX.getText("bulkDeploy.excelImport.configsUpdatedForOneDetails", new Object[]{details}), 12, 100, false
                           );
                           BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.title"), resultsPane);
                        }
                        break;
                     default:
                        if (details.isEmpty()) {
                           BDialog.message(
                              this.getOwner(),
                              LEX.getText("bulkDeploy.excelImport.title"),
                              LEX.getText("bulkDeploy.excelImport.configsUpdatedForMany", new Object[]{deployedRootCount})
                           );
                        } else {
                           BTextEditorPane resultsPane = new BTextEditorPane(
                              LEX.getText("bulkDeploy.excelImport.configsUpdatedForManyDetails", new Object[]{deployedRootCount, details}), 12, 100, false
                           );
                           BDialog.message(this.getOwner(), LEX.getText("bulkDeploy.excelImport.title"), resultsPane);
                        }
                  }
               } else if (updatedRootCount == 0) {
                  if (deployedRootCount == 1) {
                     BDialog.error(
                        this.getOwner(), LEX.getText("bulkDeploy.excelImport.title"), LEX.getText("bulkDeploy.excelImport.ErrorConfigsUpdatedForNoneOfOne")
                     );
                  } else {
                     BDialog.error(
                        this.getOwner(),
                        LEX.getText("bulkDeploy.excelImport.title"),
                        LEX.getText("bulkDeploy.excelImport.ErrorConfigsUpdatedForNoneOfMany", new Object[]{deployedRootCount})
                     );
                  }
               } else if (details.isEmpty()) {
                  BDialog.warning(
                     this.getOwner(),
                     LEX.getText("bulkDeploy.excelImport.title"),
                     LEX.getText("bulkDeploy.excelImport.ErrorConfigsUpdatedForSome", new Object[]{updatedRootCount, deployedRootCount})
                  );
               } else {
                  BDialog.warning(
                     this.getOwner(),
                     LEX.getText("bulkDeploy.excelImport.title"),
                     LEX.getText("bulkDeploy.excelImport.ErrorConfigsUpdatedDetails", new Object[]{updatedRootCount, deployedRootCount, details})
                  );
               }

               return null;
            }
         }
      }
   }

   private BComponent findTemplateInstance(BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot, BComponent root) {
      BComponent deployedTemplateComponent = null;
      BComponent deployedRootComponent = this.resolveDeployComponent(deployedWorksheet, deployedRoot, root);
      if (deployedRootComponent instanceof BStation) {
         deployedTemplateComponent = deployedRootComponent;
      } else if (deployedRootComponent != null) {
         String escapedName = SlotPath.escape(deployedRoot.getDeployName());

         try {
            deployedTemplateComponent = deployedRootComponent.get(escapedName).asComponent();
         } catch (Exception var8) {
         }
      }

      if (deployedTemplateComponent == null) {
         LOG.log(
            Level.WARNING, LEX.getText("bulkDeploy.excelImport.templateNotResolvedError", new Object[]{deployedRoot.getDeployName(), deployedWorksheet.title})
         );
      } else {
         deployedTemplateComponent.lease(4);
         BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedTemplateComponent);
         if (templateConfig == null) {
            LOG.log(Level.WARNING, LEX.getText("bulkDeploy.excelImport.invalidTemplateError", new Object[]{deployedTemplateComponent.getName()}));
            deployedTemplateComponent = null;
         } else if (!Objects.equals(templateConfig.getUID(), deployedWorksheet.uid)) {
            LOG.log(Level.WARNING, LEX.getText("bulkDeploy.excelImport.invalidTemplateId", new Object[]{deployedTemplateComponent.getName()}));
            deployedTemplateComponent = null;
         }
      }

      return deployedTemplateComponent;
   }

   protected BComponent resolveDeployComponent(BulkDeployUtil.DeployedWorksheet deployedWorksheet, BulkDeployUtil.DeployedRoot deployedRoot, BComponent root) {
      BComponent deployedRootComponent = null;
      String var5 = deployedWorksheet.templateType;
      switch (var5) {
         case "Application":
            deployedRootComponent = root;
            break;
         case "Device":
         case "Component":
         default:
            BOrd deployRootOrd = BOrd.make(root.getSlotPathOrd() + deployedRoot.getParentComponentSlotPath());
            LOG.log(this.IMPORT_LOG_LEVEL, "deployRootOrd = " + deployRootOrd);

            try {
               deployedRootComponent = deployRootOrd.resolve(root).getComponent();
            } catch (UnresolvedException var9) {
            }
      }

      if (deployedRootComponent == null) {
         LOG.log(Level.WARNING, LEX.getText("bulkDeploy.excelImport.rootNotResolvedError", new Object[]{deployedRoot.getDeployName(), deployedWorksheet.title}));
      }

      return deployedRootComponent;
   }
}
