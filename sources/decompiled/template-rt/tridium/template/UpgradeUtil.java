package com.tridium.template;

import com.tridium.platform.BPlatformService;
import com.tridium.sys.engine.LocalKnob;
import com.tridium.sys.engine.LocalRelationKnob;
import com.tridium.sys.tag.ComponentTags;
import com.tridium.sys.transfer.DeployToComp;
import com.tridium.sys.transfer.ReplacingContext;
import com.tridium.template.application.ProgressTracker;
import com.tridium.template.file.BINtplFile;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.util.CompUtil;
import com.tridium.util.LinkUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javax.baja.agent.BPxView;
import javax.baja.file.BIDeployable;
import javax.baja.file.FilePath;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.security.BPassword;
import javax.baja.space.Mark;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Flags;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.RelationKnob;
import javax.baja.sys.Slot;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Tag;
import javax.baja.tag.TagGroupInfo;
import javax.baja.tag.TagInfo;
import javax.baja.util.BFormat;
import javax.baja.util.BWsAnnotation;
import javax.baja.util.Lexicon;

public class UpgradeUtil implements TemplateConst {
   public static Lexicon lex = Lexicon.make("template");
   private static Type[] EXCLUDE_TYPES = new Type[]{BRelation.TYPE, BHistoryConfig.TYPE, BPlatformService.TYPE};
   private static UpgradeUtil.TypeProperties[] SAVE_TYPE_PROPERTIES = new UpgradeUtil.TypeProperties[]{
      new UpgradeUtil.TypeProperties(BHistoryExt.TYPE, new String[]{BHistoryExt.historyName.getName()})
   };
   private static Level LOG_LEVEL = Level.FINE;
   private static final Logger LOG = Logger.getLogger("template");

   public static void upgradeTemplates(List<BTemplateConfig> configs, List<Boolean> redeployValues, ProgressTracker progress) {
      Set<UpgradeUtil.RelationSpec> relationSpecs = new HashSet<>();
      BComponent base = null;
      progress.constrain(80);
      progress.divide(configs.size());

      try {
         for (BTemplateConfig config : configs) {
            try {
               BComponent upgradedRoot = upgradeTemplates(config, progress, relationSpecs);
               base = base == null && upgradedRoot != null ? upgradedRoot.getParent().asComponent() : base;
            } catch (Exception var17) {
            }

            progress.cycle();
         }
      } finally {
         progress.conquer();
         progress.fulfill();
      }

      progress.constrain(100);

      try {
         if (base != null) {
            rebuildRelations(base, relationSpecs);
         }
      } finally {
         progress.fulfill();
      }
   }

   public static void upgradeTemplates(BTemplateConfig tmplConfig) throws Exception {
      upgradeTemplates(tmplConfig, new ProgressTracker(), null);
   }

   private static BComponent upgradeTemplates(BTemplateConfig tmplConfig, ProgressTracker progress, Set<UpgradeUtil.RelationSpec> relationSpecs) throws Exception {
      progress.constrain(80);

      BComponent l1Root;
      try {
         l1Root = upgradeTemplate(tmplConfig, progress, relationSpecs);
      } finally {
         progress.fulfill();
      }

      progress.constrain(100);

      try {
         if (l1Root != null) {
            upgradeSubtemplate(l1Root, progress, relationSpecs);
         }
      } finally {
         progress.fulfill();
      }

      return l1Root;
   }

   private static void upgradeSubtemplate(BComponent root, ProgressTracker progress, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      ArrayList<BComponent> newRoots = new ArrayList<>();
      progress.constrain(80);

      try {
         BTemplateConfig[] descendants = (BTemplateConfig[])CompUtil.getDescendants(root, BTemplateConfig.class);

         for (BTemplateConfig tmplConfig : descendants) {
            if (tmplConfig.getParent() != root && tmplConfig.isMounted()) {
               try {
                  BComponent stRoot = BTemplateConfig.getRootForConfig(tmplConfig);
                  if (stRoot != null) {
                     BINtplFile ntplFile = getDeployedNtplFile(tmplConfig);
                     if (ntplFile != null) {
                        boolean closeNtplFile = !ntplFile.isOpen();

                        try {
                           String ntplSignature = ntplFile.getTemplateManifest().bogSignature;
                           long stRootSignature = getTemplateSignature(stRoot);
                           if (Long.toHexString(stRootSignature).equals(ntplSignature)) {
                              if (BTemplateService.logger.isLoggable(Level.FINE)) {
                                 BTemplateService.logger
                                    .fine(
                                       " Upgrade Subtemplate: No need to update "
                                          + tmplConfig.getTemplateName()
                                          + " at "
                                          + tmplConfig.getSlotPathOrd().encodeToString()
                                    );
                              }
                              continue;
                           }
                        } finally {
                           if (closeNtplFile) {
                              ntplFile.close();
                           }
                        }
                     }
                  }

                  BComponent addedRoot = upgradeTemplate(tmplConfig, progress, relationSpecs);
                  if (addedRoot != null) {
                     newRoots.add(addedRoot);
                  }
               } catch (Exception var42) {
                  BTemplateService.logger.log(Level.WARNING, "Error upgrading subtemplate: " + var42.getLocalizedMessage(), (Throwable)var42);
               }
            }
         }
      } finally {
         progress.fulfill();
      }

      progress.constrain(100);

      try {
         progress.divide(newRoots.size());

         try {
            for (BComponent newRoot : newRoots) {
               upgradeSubtemplate(newRoot, progress, relationSpecs);
               progress.cycle();
            }
         } finally {
            progress.conquer();
         }
      } finally {
         progress.fulfill();
      }
   }

   private static BComponent upgradeTemplate(BTemplateConfig tmplConfig, ProgressTracker progress, Set<UpgradeUtil.RelationSpec> relationSpecs) throws Exception {
      BComponent deployedRoot = tmplConfig.getParent().asComponent();
      progress.message("job.upgrading", deployedRoot.getSlotPath().toString());
      BComponent rootParent = deployedRoot.getParent().asComponent();
      BValue bValue = tmplConfig.get("ntplFile");
      if (bValue == null) {
         TemplateManifest manifest = tmplConfig.getManifest();
         FilePath ntplFilePath = new FilePath("^template/" + manifest.vendor).merge(tmplConfig.getTemplateName() + ".ntpl");
         BOrd ntplOrd = BOrd.make(ntplFilePath);
         bValue = tmplConfig.get(tmplConfig.add("ntplFile", ntplOrd, 5));
      }

      if (!(bValue instanceof BOrd)) {
         throw new RuntimeException("NtplFile not defined for: " + rootParent.getSlotPath());
      } else {
         BObject deployable = ((BOrd)bValue).get(deployedRoot);
         if (deployable != null && deployable.getType().is(BIDeployable.TYPE)) {
            return upgrade(deployedRoot, deployable, progress, relationSpecs);
         } else {
            throw new RuntimeException("NtplFile not found for: " + rootParent.getSlotPath());
         }
      }
   }

   public static BComponent upgrade(BComponent deployedRoot, BObject ntplFile) throws Exception {
      return upgrade(deployedRoot, ntplFile, new ProgressTracker(), null);
   }

   private static BComponent upgrade(BComponent deployedRoot, BObject ntplFile, ProgressTracker progress, Set<UpgradeUtil.RelationSpec> relationSpecs) throws Exception {
      String deployName = deployedRoot.getName();
      BComponent rootParent = deployedRoot.getParent().asComponent();
      if (!rootParent.isMounted()) {
         return null;
      } else {
         UpgradeUtil.TemplateSaveData templateSavedData = saveTemplateData(deployedRoot);
         if (relationSpecs != null) {
            collectRelationSpecs(deployedRoot, templateSavedData, relationSpecs);
         }

         progress.message("job.dataSaved");
         Property deployedRootProperty = deployedRoot.getPropertyInParent();
         BFormat displayName = rootParent.getDisplayNameFormat(deployedRootProperty);

         try {
            ReplacingContext replacingContext = new ReplacingContext(DeployToComp.NoPostLink);
            Mark mark = new Mark(ntplFile, deployName);
            rootParent.remove(deployedRootProperty, replacingContext);
            progress.message("job.removed");
            BComponent params = new BComponent();
            params.add("exact", BBoolean.TRUE);
            mark.copyTo(rootParent, params, replacingContext);
         } catch (Exception var16) {
            BTemplateService.logger.log(Level.WARNING, "Error upgrading template: " + var16.getLocalizedMessage(), (Throwable)var16);
            throw var16;
         }

         progress.message("job.installed");
         BComponent newDeployRoot = rootParent.get(deployName).asComponent();
         if (displayName != null) {
            rootParent.setDisplayName(rootParent.getProperty(newDeployRoot.getName()), displayName, null);
         }

         BHistoryExt[] historyExtensions = (BHistoryExt[])CompUtil.getDescendants(newDeployRoot, BHistoryExt.class);
         HashMap<String, Boolean> historyExtMap = new HashMap<>();

         for (BHistoryExt ext : historyExtensions) {
            historyExtMap.put(ext.getSlotPathOrd().encodeToString(), ext.getEnabled());
         }

         restoreTemplateSaveData(templateSavedData, newDeployRoot, relationSpecs != null);
         restoreHistoryExtensions(historyExtMap, newDeployRoot);
         progress.message("job.dataRestored");
         return newDeployRoot;
      }
   }

   private static void restoreHistoryExtensions(HashMap<String, Boolean> historyMap, BComponent upgradedTemplate) {
      BHistoryExt[] staleHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(upgradedTemplate, BHistoryExt.class);

      for (int i = 0; i < staleHistoryExtensions.length; i++) {
         boolean enabled = historyMap.get(staleHistoryExtensions[i].getSlotPathOrd().encodeToString());
         staleHistoryExtensions[i].setEnabled(enabled);
      }
   }

   public static UpgradeUtil.TemplateSaveData saveTemplateData(BComponent deployedRoot) {
      HashMap<Object, BComponent> handleMap = getComponentHandleMap(deployedRoot);
      UpgradeUtil.TemplateSaveData templateSavedData = new UpgradeUtil.TemplateSaveData();
      BWsAnnotation[] wsAnnotations = (BWsAnnotation[])deployedRoot.getChildren(BWsAnnotation.class);
      if (wsAnnotations != null && wsAnnotations.length > 0) {
         templateSavedData.location = wsAnnotations[0];
      }

      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(deployedRoot);
      templateSavedData.rootProperties = getRootProperties(deployedRoot);
      templateSavedData.pxEditSaveValues = getPxEditValues(deployedRoot, templateConfig);
      templateSavedData.passwords = getPasswords(templateConfig);
      templateSavedData.configProperties = getConfigProperties(templateConfig);
      templateSavedData.inputLinks = getInputLinks(deployedRoot, templateConfig);
      templateSavedData.outputKnobs = getOutputKnobs(deployedRoot, templateConfig);
      templateSavedData.externalLinks = getExternalLinks(deployedRoot, handleMap);
      templateSavedData.externalKnobs = getExternalKnobs(deployedRoot, handleMap);
      templateSavedData.relationInfos = templateConfig.getRelationInfos();
      templateSavedData.outputRelations = getOutputRelations(deployedRoot);
      templateSavedData.inputRelations = getInputRelations(deployedRoot);
      templateSavedData.externalOutputRelations = getExternalRelations(deployedRoot, handleMap);
      templateSavedData.externalInputRelations = getExternalRelationKnobs(deployedRoot, handleMap);
      templateSavedData.savedTags = getComponentTags(deployedRoot);
      templateSavedData.savedPropertyValues = getPropertyValues(deployedRoot);
      return templateSavedData;
   }

   public static void collectRelationSpecs(BComponent root, UpgradeUtil.TemplateSaveData saveData, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      collectInputLinks(root, saveData.inputLinks, relationSpecs);
      collectOutputKnobs(root, saveData.outputKnobs, relationSpecs);
      collectExternalLinks(root, saveData.externalLinks, relationSpecs);
      collectExternalKnobs(saveData.externalKnobs, relationSpecs);
      collectOutputRelations(root, saveData.outputRelations, saveData.relationInfos, relationSpecs);
      collectInputRelations(root, saveData.inputRelations, relationSpecs);
      collectExternalOutputRelations(root, saveData.externalOutputRelations, relationSpecs);
      collectExternalInputRelations(root, saveData.externalInputRelations, relationSpecs);
   }

   private static void collectInputLinks(BComponent target, Hashtable<String, BLink> inputLinks, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      for (BLink link : inputLinks.values()) {
         try {
            collectLink(target, target.getSlotPath(), link, relationSpecs);
         } catch (Exception var6) {
         }
      }
   }

   private static void collectOutputKnobs(
      BComponent source, Hashtable<String, UpgradeUtil.LinkTarget[]> outputKnobs, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (Entry<String, UpgradeUtil.LinkTarget[]> outputKnobEntry : outputKnobs.entrySet()) {
         try {
            collectKnob(Objects.requireNonNull(source.getSlotPath()), outputKnobEntry.getKey(), Arrays.asList(outputKnobEntry.getValue()), relationSpecs);
         } catch (Exception var6) {
         }
      }
   }

   private static void collectExternalLinks(BComponent base, Hashtable<BOrd, ArrayList<BLink>> externalLinks, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      for (Entry<BOrd, ArrayList<BLink>> externalLinkEntry : externalLinks.entrySet()) {
         try {
            SlotPath targetPath = (SlotPath)externalLinkEntry.getKey().parse()[0];

            for (BLink link : externalLinkEntry.getValue()) {
               collectLink(base, targetPath, link, relationSpecs);
            }
         } catch (Exception var8) {
         }
      }
   }

   private static void collectExternalKnobs(
      Hashtable<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> externalKnobs, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (Entry<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> externalKnobEntry : externalKnobs.entrySet()) {
         try {
            SlotPath sourcePath = (SlotPath)externalKnobEntry.getKey().parse()[0];

            for (Entry<String, ArrayList<UpgradeUtil.LinkTarget>> linkTargetsEntry : externalKnobEntry.getValue().entrySet()) {
               collectKnob(sourcePath, linkTargetsEntry.getKey(), linkTargetsEntry.getValue(), relationSpecs);
            }
         } catch (Exception var7) {
         }
      }
   }

   private static void collectLink(BComponent base, SlotPath targetPath, BLink link, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      try {
         SlotPath sourcePath = Objects.requireNonNull(link.getSourceOrd().resolve(base).get().asComponent().getSlotPath());
         relationSpecs.add(UpgradeUtil.RelationSpec.makeLinkSpec(sourcePath, link.getSourceSlotName(), targetPath, link.getTargetSlotName()));
      } catch (Exception var5) {
      }
   }

   private static void collectKnob(
      SlotPath sourcePath, String sourceName, List<UpgradeUtil.LinkTarget> linkTargets, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (UpgradeUtil.LinkTarget linkTarget : linkTargets) {
         try {
            SlotPath targetPath = Objects.requireNonNull(linkTarget.target.getSlotPath());
            relationSpecs.add(UpgradeUtil.RelationSpec.makeLinkSpec(sourcePath, sourceName, targetPath, linkTarget.slot));
         } catch (Exception var7) {
         }
      }
   }

   private static void collectOutputRelations(
      BComponent target, BRelation[] relations, ArrayList<BRelationInfo> oldRelationInfos, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (BRelation relation : relations) {
         if (!(relation instanceof BLink)) {
            boolean wasInOldTemplate = false;

            for (BRelationInfo oldRelationInfo : oldRelationInfos) {
               if (!oldRelationInfo.getInbound() && relation.getRelationId().equals(oldRelationInfo.relationId)) {
                  wasInOldTemplate = true;
                  break;
               }
            }

            try {
               collectOutputRelation(target, relation, wasInOldTemplate, Objects.requireNonNull(target.getSlotPath()), relationSpecs);
            } catch (Exception var11) {
            }
         }
      }
   }

   private static void collectInputRelations(BComponent endpoint, Hashtable<String, BComponent> relations, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      for (Entry<String, BComponent> relationsEntry : relations.entrySet()) {
         String relationId = relationsEntry.getKey();
         BComponent target = relationsEntry.getValue();

         try {
            collectInputRelation(relationId, Objects.requireNonNull(endpoint.getSlotPath()), target, relationSpecs);
         } catch (Exception var8) {
         }
      }
   }

   private static void collectExternalOutputRelations(
      BComponent base, Hashtable<BOrd, ArrayList<BRelation>> externalOutputRelations, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (Entry<BOrd, ArrayList<BRelation>> relationEntry : externalOutputRelations.entrySet()) {
         try {
            SlotPath targetPath = Objects.requireNonNull(relationEntry.getKey().resolve(base).get().asComponent().getSlotPath());

            for (BRelation relation : relationEntry.getValue()) {
               collectOutputRelation(base, relation, false, targetPath, relationSpecs);
            }
         } catch (Exception var8) {
         }
      }
   }

   private static void collectExternalInputRelations(
      BComponent base, Hashtable<BOrd, Hashtable<String, ArrayList<BComponent>>> externalInputRelations, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      for (Entry<BOrd, Hashtable<String, ArrayList<BComponent>>> relationsEntry : externalInputRelations.entrySet()) {
         try {
            SlotPath endpointPath = Objects.requireNonNull(relationsEntry.getKey().resolve(base).get().asComponent().getSlotPath());

            for (Entry<String, ArrayList<BComponent>> relationEntry : relationsEntry.getValue().entrySet()) {
               String relationId = relationEntry.getKey();

               for (BComponent target : relationEntry.getValue()) {
                  collectInputRelation(relationId, endpointPath, target, relationSpecs);
               }
            }
         } catch (Exception var11) {
         }
      }
   }

   private static void collectOutputRelation(
      BComponent base, BRelation relation, boolean wasInOldTemplate, SlotPath targetPath, Set<UpgradeUtil.RelationSpec> relationSpecs
   ) {
      try {
         SlotPath endpointPath = Objects.requireNonNull(relation.getEndpointOrd().resolve(base).get().asComponent().getSlotPath());
         relationSpecs.add(UpgradeUtil.RelationSpec.makeRelationSpec(relation.getRelationId(), wasInOldTemplate, endpointPath, targetPath));
      } catch (Exception var6) {
      }
   }

   private static void collectInputRelation(String relationId, SlotPath endpointPath, BComponent target, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      try {
         relationSpecs.add(UpgradeUtil.RelationSpec.makeRelationSpec(relationId, false, endpointPath, Objects.requireNonNull(target.getSlotPath())));
      } catch (Exception var5) {
      }
   }

   private static void restoreTemplateSaveData(UpgradeUtil.TemplateSaveData savedData, BComponent newDeployedRoot, boolean ignoreRelations) {
      newDeployedRoot.lease(Integer.MAX_VALUE);
      BTemplateConfig newTemplateConfig = BTemplateConfig.getConfigForRoot(newDeployedRoot);
      if (savedData.location != null) {
         BValue wsAnnotation = newDeployedRoot.get("wsAnnotation");
         if (wsAnnotation != null) {
            newDeployedRoot.set("wsAnnotation", savedData.location, null);
         } else {
            newDeployedRoot.add("wsAnnotation", savedData.location);
         }
      }

      restoreRootProperties(newDeployedRoot, savedData.rootProperties);
      restorePasswords(newTemplateConfig, savedData.passwords);
      restoreConfigProperties(newDeployedRoot, savedData.configProperties);
      if (!ignoreRelations) {
         restoreInputLinks(newDeployedRoot, savedData.inputLinks);
         restoreOutputLinks(newDeployedRoot, savedData.outputKnobs);
         restoreOutputRelations(newDeployedRoot, savedData.relationInfos, savedData.outputRelations);
         restoreInputRelations(newDeployedRoot, savedData.inputRelations);
      }

      restorePxEditValues(newDeployedRoot, savedData.pxEditSaveValues);
      restoreComponentTags(newDeployedRoot, savedData.savedTags);
      if (!ignoreRelations) {
         restoreExternalLinks(newDeployedRoot, savedData.externalLinks);
         restoreExternalKnobs(newDeployedRoot, savedData.externalKnobs);
         restoreExternalRelations(newDeployedRoot, savedData.externalOutputRelations);
         restoreExternalRelationKnobs(newDeployedRoot, savedData.externalInputRelations);
      }

      restorePropertyValues(newDeployedRoot, savedData.savedPropertyValues);
   }

   public static void rebuildRelations(BComponent base, Set<UpgradeUtil.RelationSpec> relationSpecs) {
      for (UpgradeUtil.RelationSpec relationSpec : relationSpecs) {
         if (relationSpec.isLink) {
            rebuildLink(base, relationSpec);
         } else {
            rebuildRelation(base, relationSpec);
         }
      }
   }

   private static void rebuildLink(BComponent base, UpgradeUtil.RelationSpec relationSpec) {
      assert relationSpec.isLink;

      try {
         BOrd sourceOrd = BOrd.make(relationSpec.sourcePath);
         BComponent source = sourceOrd.resolve(base).get().asComponent();
         Slot sourceSlot = source.getSlot(relationSpec.sourceName);
         BOrd targetOrd = BOrd.make(relationSpec.targetPath);
         BComponent target = targetOrd.resolve(base).get().asComponent();
         Slot targetSlot = target.getSlot(relationSpec.targetName);
         BLink link = target.makeLink(source, sourceSlot, targetSlot, null);
         target.add(null, link);
      } catch (Exception var9) {
      }
   }

   private static void rebuildRelation(BComponent base, UpgradeUtil.RelationSpec relationSpec) {
      assert !relationSpec.isLink;

      try {
         BOrd targetOrd = BOrd.make(relationSpec.targetPath);
         BComponent target = targetOrd.resolve(base).get().asComponent();
         boolean ignore = false;
         if (relationSpec.wasInOldTemplate) {
            ignore = true;
            BTemplateConfig config = BTemplateConfig.getConfigForRoot(target);
            if (config != null) {
               for (BRelationInfo info : config.getRelationInfos()) {
                  if (!info.getInbound() && info.relationId.equals(relationSpec.relationId)) {
                     ignore = false;
                     break;
                  }
               }
            }
         }

         if (!ignore) {
            BOrd endpointOrd = BOrd.make(relationSpec.sourcePath);
            BComponent endpoint = endpointOrd.resolve(base).get().asComponent();
            BRelation relation = target.makeRelation(Id.newId(relationSpec.relationId), endpoint, null);
            target.add(null, relation);
         }
      } catch (Exception var8) {
      }
   }

   private static void restoreConfigProperties(BComponent root, Hashtable<String, BValue> configProps) {
      BTemplateConfig tc = BTemplateConfig.getConfigForRoot(root);
      if (tc != null) {
         for (Property property : tc.getProperties()) {
            if ((tc.getFlags(property) & 5) == 0) {
               BValue bValue = configProps.get(property.getName());
               if (bValue != null) {
                  tc.set(property, bValue);
               }
            }
         }
      }
   }

   private static void restoreOutputRelations(BComponent root, ArrayList<BRelationInfo> oldRelationInfos, BRelation[] relations) {
      BTemplateConfig newTc = BTemplateConfig.getConfigForRoot(root);
      if (newTc != null) {
         ArrayList<BRelationInfo> newRelationInfos = newTc.getRelationInfos();

         for (BRelation relation : relations) {
            if (!(relation instanceof BLink)) {
               boolean wasInOldTemplateRelation = false;
               boolean isInNewTemplateRelation = false;

               for (BRelationInfo oldRelationInfo : oldRelationInfos) {
                  if (!oldRelationInfo.getInbound() && relation.getRelationId().equals(oldRelationInfo.getRelationId())) {
                     wasInOldTemplateRelation = true;
                     break;
                  }
               }

               for (BRelationInfo newRelationInfo : newRelationInfos) {
                  if (!newRelationInfo.getInbound() && relation.getRelationId().equals(newRelationInfo.getRelationId())) {
                     isInNewTemplateRelation = true;
                     break;
                  }
               }

               if (!wasInOldTemplateRelation || isInNewTemplateRelation) {
                  root.add(null, relation.newCopy(true));
               }
            }
         }
      }
   }

   private static void restoreInputRelations(BComponent root, Hashtable<String, BComponent> inputRelations) {
      Enumeration<String> relationIds = inputRelations.keys();

      while (relationIds.hasMoreElements()) {
         String relationId = relationIds.nextElement();
         BComponent source = inputRelations.get(relationId);
         BRelation relation = source.makeRelation(Id.newId(relationId), root, null);
         source.add(null, relation);
      }
   }

   private static void restoreInputLinks(BComponent root, Hashtable<String, BLink> inputLinks) {
      BTemplateConfig tc = BTemplateConfig.getConfigForRoot(root);
      if (tc != null) {
         for (Slot slot : tc.getInputSlots()) {
            BLink link = inputLinks.get(slot.getName());
            if (link != null) {
               root.add(null, link);
            }
         }
      }
   }

   private static void restoreOutputLinks(BComponent root, Hashtable<String, UpgradeUtil.LinkTarget[]> outputLinks) {
      BTemplateConfig tc = BTemplateConfig.getConfigForRoot(root);
      if (tc != null) {
         for (Slot slot : tc.getOutputSlots()) {
            UpgradeUtil.LinkTarget[] linkTargets = outputLinks.get(slot.getName());
            if (linkTargets != null && linkTargets.length != 0) {
               for (UpgradeUtil.LinkTarget linkTarget : linkTargets) {
                  if (linkTarget != null) {
                     String targetSlotName = linkTarget.slot;
                     BComponent target = linkTarget.target;
                     Slot targetSlot = target.getSlot(targetSlotName);
                     BLink link = target.makeLink(root, slot, targetSlot, null);
                     target.add(null, link);
                  }
               }
            }
         }
      }
   }

   private static void restoreExternalLinks(BComponent root, Hashtable<BOrd, ArrayList<BLink>> extLinks) {
      Enumeration<BOrd> keys = extLinks.keys();

      while (keys.hasMoreElements()) {
         try {
            BOrd ord = keys.nextElement();
            BComponent target = ord.resolve(root).get().asComponent();

            for (BLink link : extLinks.get(ord)) {
               target.add(null, link);
            }
         } catch (Exception var8) {
            BTemplateService.logger.log(Level.WARNING, "Error restoring external links: " + var8.getLocalizedMessage(), (Throwable)var8);
         }
      }
   }

   private static void restoreExternalKnobs(BComponent root, Hashtable<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> extKnobs) {
      Enumeration<BOrd> keys = extKnobs.keys();

      while (keys.hasMoreElements()) {
         try {
            BOrd ord = keys.nextElement();
            BComponent source = ord.resolve(root).get().asComponent();
            Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>> slotKnobs = extKnobs.get(ord);
            Enumeration<String> slots = slotKnobs.keys();

            while (slots.hasMoreElements()) {
               String slotName = slots.nextElement();
               Slot sourceSlot = source.getSlot(slotName);

               for (UpgradeUtil.LinkTarget linkTarget : slotKnobs.get(slotName)) {
                  if (linkTarget != null) {
                     String targetSlotName = linkTarget.slot;
                     BComponent target = linkTarget.target;
                     Slot targetSlot = target.getSlot(targetSlotName);
                     BLink link = target.makeLink(source, sourceSlot, targetSlot, null);
                     target.add(null, link);
                  }
               }
            }
         } catch (Exception var15) {
            BTemplateService.logger.log(Level.WARNING, "Error restoring external knobs: " + var15.getLocalizedMessage(), (Throwable)var15);
         }
      }
   }

   private static void restoreExternalRelations(BComponent root, Hashtable<BOrd, ArrayList<BRelation>> extRelations) {
      Enumeration<BOrd> keys = extRelations.keys();

      while (keys.hasMoreElements()) {
         try {
            BOrd ord = keys.nextElement();
            BComponent target = ord.resolve(root).get().asComponent();

            for (BRelation relation : extRelations.get(ord)) {
               target.add(null, relation);
            }
         } catch (Exception var8) {
            BTemplateService.logger.log(Level.WARNING, "Error restoring external relations: " + var8.getLocalizedMessage(), (Throwable)var8);
         }
      }
   }

   private static void restoreExternalRelationKnobs(BComponent root, Hashtable<BOrd, Hashtable<String, ArrayList<BComponent>>> extRelKnobs) {
      Enumeration<BOrd> keys = extRelKnobs.keys();

      while (keys.hasMoreElements()) {
         try {
            BOrd ord = keys.nextElement();
            BComponent relEp = ord.resolve(root).get().asComponent();
            Hashtable<String, ArrayList<BComponent>> relIdComps = extRelKnobs.get(ord);
            Enumeration<String> ids = relIdComps.keys();

            while (ids.hasMoreElements()) {
               String relId = ids.nextElement();

               for (BComponent relComp : relIdComps.get(relId)) {
                  if (relComp != null) {
                     BRelation rel = relComp.makeRelation(Id.newId(relId), relEp, null);
                     relComp.add(null, rel);
                  }
               }
            }
         } catch (Exception var11) {
            BTemplateService.logger.log(Level.WARNING, "Error restoring external relation knobs: " + var11.getLocalizedMessage(), (Throwable)var11);
         }
      }
   }

   private static Hashtable<String, BLink> getInputLinks(BComponent templateRoot, BTemplateConfig templateConfig) {
      Hashtable<String, BLink> inputSources = new Hashtable<>();

      for (Slot slot : templateConfig.getInputSlots()) {
         for (BLink link : templateRoot.getLinks(slot)) {
            inputSources.put(slot.getName(), (BLink)link.newCopy());
         }
      }

      return inputSources;
   }

   private static Hashtable<String, UpgradeUtil.LinkTarget[]> getOutputKnobs(BComponent templateRoot, BTemplateConfig templateConfig) {
      Hashtable<String, UpgradeUtil.LinkTarget[]> outputTargets = new Hashtable<>();

      for (Slot slot : templateConfig.getOutputSlots()) {
         Knob[] knobs = templateRoot.getKnobs(slot);
         UpgradeUtil.LinkTarget[] targets = new UpgradeUtil.LinkTarget[knobs.length];

         for (int i = 0; i < knobs.length; i++) {
            String targetSlot = knobs[i].getTargetSlotName();
            if (knobs[i] instanceof LocalKnob) {
               BComponent targetComponent = knobs[i].getTargetComponent();
               targets[i] = new UpgradeUtil.LinkTarget(targetSlot, targetComponent);
            } else {
               BComponent targetComponent = knobs[i].getTargetOrd().resolve(templateRoot).get().asComponent();
               targets[i] = new UpgradeUtil.LinkTarget(targetSlot, targetComponent);
            }
         }

         outputTargets.put(slot.getName(), targets);
      }

      return outputTargets;
   }

   private static BRelation[] getOutputRelations(BComponent templateRoot) {
      ArrayList<BRelation> outputRelations = new ArrayList<>();
      BRelation[] componentRelations = templateRoot.getComponentRelations();

      for (BRelation relation : componentRelations) {
         if (!(relation instanceof BLink)) {
            Entity endpoint = relation.getEndpoint();
            if (endpoint instanceof BComponent && !((BComponent)endpoint).isDescendentOf(templateRoot)) {
               outputRelations.add(relation);
            }
         }
      }

      return outputRelations.toArray(new BRelation[0]);
   }

   private static Hashtable<String, BComponent> getInputRelations(BComponent templateRoot) {
      Hashtable<String, BComponent> inputRelations = new Hashtable<>();

      for (RelationKnob relationKnob : templateRoot.getRelationKnobs()) {
         BComponent source = relationKnob instanceof LocalRelationKnob
            ? relationKnob.getRelation().getParent().asComponent()
            : relationKnob.getRelationOrd().resolve(templateRoot).get().asComponent();
         if (!source.isDescendentOf(templateRoot)) {
            String relationId = relationKnob.getRelationId();
            inputRelations.put(relationId, source);
         }
      }

      return inputRelations;
   }

   private static Hashtable<String, BValue> getPxEditValues(BComponent root, BTemplateConfig templateConfig) {
      Hashtable<String, BValue> editTargets = new Hashtable<>();
      getPxEditValues(templateConfig, editTargets);

      for (BTemplateConfig tc : (BTemplateConfig[])CompUtil.getDescendants(root, BTemplateConfig.class)) {
         if (tc != templateConfig) {
            getPxEditValues(tc, editTargets);
         }
      }

      return editTargets;
   }

   private static void getPxEditValues(BTemplateConfig templateConfig, Hashtable<String, BValue> editTargets) {
      BComponent deployRoot = templateConfig.getParent().asComponent();
      SlotPath rootSlotPath = deployRoot.getSlotPath();
      if (rootSlotPath != null) {
         String[] rootSlotPathNames = rootSlotPath.getNames();

         for (BString slotPath : (BString[])templateConfig.getPxEditBindings().getChildren(BString.class)) {
            try {
               String[] split = slotPath.toString().split("slot:");
               SlotPath editSlotPath = new SlotPath(split[1]);
               String[] editSlotPathNames = editSlotPath.getNames();
               ArrayList<String> mergeNames = new ArrayList<>();
               Collections.addAll(mergeNames, rootSlotPathNames);
               mergeNames.addAll(Arrays.asList(editSlotPathNames).subList(1, editSlotPathNames.length));
               String[] pathNames = new String[mergeNames.size()];
               mergeNames.toArray(pathNames);
               editSlotPath = new SlotPath("slot", pathNames);
               BOrd targetOrd = BOrd.make(editSlotPath);
               BObject curValue = targetOrd.resolve(deployRoot).get();
               editTargets.put(editSlotPath.getBody(), curValue.asValue());
            } catch (Exception var16) {
               BTemplateService.logger.log(Level.WARNING, "Error resolving PX edit values: " + var16.getLocalizedMessage(), (Throwable)var16);
            }
         }
      }
   }

   private static Hashtable<String, BValue> getConfigProperties(BTemplateConfig templateConfig) {
      Hashtable<String, BValue> configProps = new Hashtable<>();

      for (Property property : templateConfig.getProperties()) {
         if ((templateConfig.getFlags(property) & 5) == 0) {
            configProps.put(property.getName(), templateConfig.get(property).newCopy(true));
         }
      }

      return configProps;
   }

   private static Hashtable<String, BValue> getRootProperties(BComponent templateRoot) {
      Hashtable<String, BValue> rootProps = new Hashtable<>();

      for (Property property : templateRoot.getProperties()) {
         if ((templateRoot.getFlags(property) & 2) == 0 && !property.getName().startsWith("ntpl$3a")) {
            BValue curVal = templateRoot.get(property);
            if (!(curVal instanceof BPxView) && !(curVal instanceof BRelation) && !(curVal instanceof BComponent)) {
               rootProps.put(property.getName(), curVal);
            }
         }
      }

      return rootProps;
   }

   private static Hashtable<BOrd, ArrayList<Tag>> getComponentTags(BComponent templateRoot) {
      Hashtable<BOrd, ArrayList<Tag>> compTags = new Hashtable<>();
      BComponent[] comps = (BComponent[])CompUtil.getDescendants(templateRoot, BComponent.class);

      for (BComponent comp : comps) {
         ArrayList<Tag> tags = new ArrayList<>();

         for (Tag tag : new ComponentTags(comp)) {
            if (!tag.getId().getDictionary().equals("ntpl")) {
               String qName = SlotPath.escape(tag.getId().getQName());
               Property prop = comp.getProperty(qName);
               if (!Flags.isUserDefined4(comp, prop)) {
                  tags.add(tag);
               }
            }
         }

         for (BRelation relation : (BRelation[])comp.getChildren(BRelation.class)) {
            if (relation.getRelationId().equals("n:tagGroup")) {
               BComplex parent = relation.getParent();
               Property pip = relation.getPropertyInParent();
               if (!Flags.isUserDefined4(parent, pip)) {
                  Entity endpoint = relation.getEndpoint();
                  if (endpoint != null && endpoint instanceof TagGroupInfo) {
                     TagGroupInfo tgi = (TagGroupInfo)endpoint;
                     if (tgi instanceof BComponent) {
                        ((BComponent)tgi).lease(2);
                     }

                     Iterator<TagInfo> tgiTags = tgi.getTags();

                     while (tgiTags.hasNext()) {
                        TagInfo tagInfo = tgiTags.next();
                        tags.add(tagInfo.makeTag());
                     }
                  }
               }
            }
         }

         if (tags.size() > 0) {
            BOrd ord = comp.getSlotPathOrd();
            if (ord != null) {
               compTags.put(ord, tags);
            }
         }
      }

      return compTags;
   }

   private static Hashtable<BOrd, ArrayList<BLink>> getExternalLinks(BComponent root, HashMap<Object, BComponent> handleMap) {
      Hashtable<BOrd, ArrayList<BLink>> compExtLinks = new Hashtable<>();
      BComponent[] components = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent comp : components) {
         ArrayList<BLink> extLinks = new ArrayList<>();

         for (BLink link : comp.getLinks()) {
            if (!LinkUtil.isCompositeLink(link)) {
               String ord = link.getSourceOrd().toString();
               if (ord.startsWith("h:") && !handleMap.containsKey(link.getSourceOrd())) {
                  BComplex linkParent = link.getParent();
                  Property linkPIP = link.getPropertyInParent();
                  if (!Flags.isTransient(linkParent, linkPIP)) {
                     extLinks.add((BLink)link.newCopy());
                  }
               }
            }
         }

         if (extLinks.size() > 0) {
            compExtLinks.put(comp.getSlotPathOrd(), extLinks);
         }
      }

      return compExtLinks;
   }

   private static Hashtable<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> getExternalKnobs(BComponent root, HashMap<Object, BComponent> handleMap) {
      Hashtable<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> outputTargets = new Hashtable<>();
      BComponent[] components = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent comp : components) {
         try {
            ArrayList<String> processed = new ArrayList<>();
            Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>> compLinks = new Hashtable<>();

            for (Knob knob : comp.getKnobs()) {
               String sourceSlot = knob.getSourceSlotName();
               if (!processed.contains(sourceSlot)) {
                  processed.add(sourceSlot);
                  ArrayList<UpgradeUtil.LinkTarget> extLinks = new ArrayList<>();
                  Knob[] slotKnobs = comp.getKnobs(comp.getSlot(sourceSlot));

                  for (Knob slotKnob : slotKnobs) {
                     BOrd targetOrd = slotKnob.getTargetOrd();
                     String ord = targetOrd.toString();
                     if (ord.startsWith("h:") && !handleMap.containsKey(targetOrd)) {
                        String targetSlot = slotKnob.getTargetSlotName();
                        if (slotKnob instanceof LocalKnob) {
                           BComponent targetComponent = slotKnob.getTargetComponent();
                           extLinks.add(new UpgradeUtil.LinkTarget(targetSlot, targetComponent));
                        } else {
                           BComponent targetComponent = slotKnob.getTargetOrd().resolve(root).get().asComponent();
                           extLinks.add(new UpgradeUtil.LinkTarget(targetSlot, targetComponent));
                        }
                     }
                  }

                  if (extLinks.size() > 0) {
                     compLinks.put(sourceSlot, extLinks);
                  }
               }
            }

            if (compLinks.size() > 0) {
               outputTargets.put(comp.getSlotPathOrd(), compLinks);
            }
         } catch (Exception var25) {
            BTemplateService.logger.log(Level.WARNING, "Error resolving external knobs: " + var25.getLocalizedMessage(), (Throwable)var25);
         }
      }

      return outputTargets;
   }

   private static Hashtable<BOrd, ArrayList<BRelation>> getExternalRelations(BComponent root, HashMap<Object, BComponent> handleMap) {
      Hashtable<BOrd, ArrayList<BRelation>> compExtRelation = new Hashtable<>();
      BComponent[] components = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent comp : components) {
         ArrayList<BRelation> extRelations = new ArrayList<>();

         for (BRelation relation : comp.getComponentRelations()) {
            if (!(relation instanceof BLink)) {
               String ord = relation.getEndpointOrd().toString();
               if (ord.startsWith("h:") && !handleMap.containsKey(relation.getSourceOrd())) {
                  Property pip = relation.getPropertyInParent();
                  if (!Flags.isUserDefined4(comp, pip) && !Flags.isTransient(comp, pip)) {
                     extRelations.add((BRelation)relation.newCopy());
                  }
               }
            }
         }

         if (extRelations.size() > 0) {
            compExtRelation.put(comp.getSlotPathOrd(), extRelations);
         }
      }

      return compExtRelation;
   }

   private static Hashtable<BOrd, Hashtable<String, ArrayList<BComponent>>> getExternalRelationKnobs(BComponent root, HashMap<Object, BComponent> handleMap) {
      Hashtable<BOrd, Hashtable<String, ArrayList<BComponent>>> inputRelations = new Hashtable<>();
      BComponent[] components = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent comp : components) {
         try {
            ArrayList<String> idList = new ArrayList<>();
            Hashtable<String, ArrayList<BComponent>> compRelations = new Hashtable<>();
            RelationKnob[] relationKnobs = comp.getRelationKnobs();

            for (RelationKnob rKnob : relationKnobs) {
               String id = rKnob.getRelationId();
               if (!idList.contains(id)) {
                  idList.add(id);
               }
            }

            if (idList.size() != 0) {
               for (String id : idList) {
                  ArrayList<BComponent> extRelationComps = new ArrayList<>();

                  for (RelationKnob relationKnob : relationKnobs) {
                     if (relationKnob.getRelationId().equals(id) && !handleMap.containsKey(relationKnob.getRelationOrd())) {
                        BComponent relationComp = relationKnob.getRelationComponent();
                        extRelationComps.add(relationComp);
                     }
                  }

                  if (extRelationComps.size() > 0) {
                     compRelations.put(id, extRelationComps);
                  }
               }

               inputRelations.put(comp.getSlotPathOrd(), compRelations);
            }
         } catch (Exception var19) {
            BTemplateService.logger.log(Level.WARNING, "Error resolving external relation knobs: " + var19.getLocalizedMessage(), (Throwable)var19);
         }
      }

      return inputRelations;
   }

   private static void restoreComponentTags(BComponent root, Hashtable<BOrd, ArrayList<Tag>> compTags) {
      BComponent[] comps = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent comp : comps) {
         BOrd slotPathOrd = comp.getSlotPathOrd();
         if (slotPathOrd != null) {
            ArrayList<Tag> tags = compTags.get(slotPathOrd);
            if (tags != null) {
               for (Tag tag : tags) {
                  if (tag != null) {
                     try {
                        new ComponentTags(comp).set(tag);
                     } catch (Exception var12) {
                     }
                  }
               }
            }
         }
      }
   }

   private static void restorePxEditValues(BComponent root, Hashtable<String, BValue> pxEditValues) {
      Enumeration<String> slotPathBodys = pxEditValues.keys();

      while (slotPathBodys.hasMoreElements()) {
         try {
            String body = slotPathBodys.nextElement();
            BValue savedValue = pxEditValues.get(body);
            SlotPath slotPath = new SlotPath("slot", body);
            BObject targetValue = BOrd.make(slotPath).resolve(root).get();
            if (savedValue.isSimple()) {
               SlotPath parentPath = slotPath.getParent();
               String propStr = slotPath.nameAt(slotPath.depth() - 1);
               BComplex parent = BOrd.make(parentPath).resolve(root).get().asComplex();
               parent.set(propStr, savedValue);
            } else if (savedValue.isComplex()) {
               Property prop = targetValue.asComplex().getPropertyInParent();
               BComplex parent = targetValue.asComplex().getParent();
               parent.set(prop, savedValue.newCopy());
            }
         } catch (Exception var10) {
         }
      }
   }

   private static void restoreRootProperties(BComponent root, Hashtable<String, BValue> rootProps) {
      for (Property property : root.getProperties()) {
         BValue bValue = rootProps.get(property.getName());
         if (bValue != null) {
            root.set(property, bValue.newCopy(true));
         }
      }
   }

   private static Hashtable<String, BPassword> getPasswords(BTemplateConfig templateConfig) {
      Hashtable<String, BPassword> passwords = new Hashtable<>();
      BPasswordBinding[] pswBindings = (BPasswordBinding[])templateConfig.getChildren(BPasswordBinding.class);

      for (BPasswordBinding pswBinding : pswBindings) {
         Optional<BComponent> target = pswBinding.getTarget();
         if (target.isPresent()) {
            BComponent targetComp = target.get();
            targetComp.lease();
            BValue bValue = targetComp.get(pswBinding.getPswSlot());
            if (bValue instanceof BPassword) {
               passwords.put(pswBinding.getName(), (BPassword)bValue);
            }
         }
      }

      return passwords;
   }

   private static void restorePasswords(BTemplateConfig templateConfig, Hashtable<String, BPassword> passwords) {
      BPasswordBinding[] pswBindings = (BPasswordBinding[])templateConfig.getChildren(BPasswordBinding.class);

      for (BPasswordBinding pswBinding : pswBindings) {
         BPassword savedPsw = passwords.get(pswBinding.getName());
         if (savedPsw != null) {
            Optional<BComponent> target = pswBinding.getTarget();
            if (target.isPresent()) {
               BComponent targetComp = target.get();
               String pswSlot = pswBinding.getPswSlot();
               targetComp.lease();
               BValue bValue = targetComp.get(pswSlot);
               if (bValue instanceof BPassword) {
                  targetComp.set(pswSlot, savedPsw);
               }
            }
         }
      }
   }

   private static Hashtable<BOrd, ArrayList<UpgradeUtil.PropertyValue>> getPropertyValues(BComponent root) {
      Hashtable<BOrd, ArrayList<UpgradeUtil.PropertyValue>> typePropertyValues = new Hashtable<>();

      for (UpgradeUtil.TypeProperties saveTypeProperty : SAVE_TYPE_PROPERTIES) {
         for (Object o : CompUtil.getDescendants(root, saveTypeProperty.type.getTypeClass())) {
            ArrayList<UpgradeUtil.PropertyValue> values = new ArrayList<>();
            if (o instanceof BComponent) {
               BComponent comp = (BComponent)o;

               for (String property : saveTypeProperty.properties) {
                  BValue bValue = comp.get(property);
                  if (bValue != null) {
                     values.add(new UpgradeUtil.PropertyValue(property, bValue));
                  }
               }
            }

            if (values.size() > 0) {
               typePropertyValues.put(((BComponent)o).getSlotPathOrd(), values);
            }
         }
      }

      return typePropertyValues;
   }

   private static void restorePropertyValues(BComponent root, Hashtable<BOrd, ArrayList<UpgradeUtil.PropertyValue>> propertyValues) {
      for (BOrd ord : propertyValues.keySet()) {
         try {
            BObject bObject = ord.resolve(root).get();
            if (bObject != null && bObject.isComponent()) {
               BComponent comp = bObject.asComponent();

               for (UpgradeUtil.PropertyValue propertyValue : propertyValues.get(ord)) {
                  comp.set(propertyValue.property, propertyValue.value);
               }
            }
         } catch (Exception var8) {
         }
      }
   }

   public static BINtplFile getDeployedNtplFile(BTemplateConfig tmplConfig) throws Exception {
      return getDeployedNtplFile(tmplConfig, "^template/", "ntpl");
   }

   public static BINtplFile getDeployedNtplFile(BTemplateConfig tmplConfig, String filepathBase, String extension) {
      BValue bValue = tmplConfig.get("ntplFile");
      if (bValue == null) {
         TemplateManifest manifest = tmplConfig.getManifest();
         FilePath ntplFilePath = new FilePath(filepathBase + manifest.vendor).merge(tmplConfig.getTemplateName() + '.' + extension);
         BOrd ntplOrd = BOrd.make(ntplFilePath);
         bValue = tmplConfig.get(tmplConfig.add("ntplFile", ntplOrd, 5));
      }

      if (!(bValue instanceof BOrd)) {
         throw new RuntimeException("NtplFile not defined for: " + tmplConfig.getSlotPath());
      } else {
         BObject deployable = ((BOrd)bValue).get(tmplConfig);
         if (deployable != null && deployable.getType().is(BINtplFile.TYPE)) {
            return (BINtplFile)deployable;
         } else {
            throw new RuntimeException("NtplFile not found for: " + tmplConfig.getSlotPath());
         }
      }
   }

   public static void deleteTemplateInputRelations(BComponent template, List<String[]> templateDefs) {
      for (RelationKnob relationKnob : template.getRelationKnobs()) {
         String relationId = relationKnob.getRelationId();
         if (isValidTemplateRelation(relationId, "Inbound", templateDefs)) {
            if (relationKnob instanceof LocalRelationKnob) {
               BComponent source = relationKnob.getRelation().getParent().asComponent();
               Optional<Relation> relationToRemove = source.relations().get(Id.newId(relationId));
               relationToRemove.ifPresent(relation -> {
                  LOG.log(LOG_LEVEL, String.format("Deleting LocalRelationKnob input relation %s ", relation.getId().getQName()));
                  source.relations().remove(relation);
               });
            } else {
               BOrd relationOrd = relationKnob.getRelationOrd();
               BComponent relationComponent = relationOrd.resolve(template).get().asComponent();
               Optional<Relation> relationToRemove = relationComponent.relations().get(Id.newId(relationId));
               relationToRemove.ifPresent(relation -> {
                  LOG.log(LOG_LEVEL, String.format("Deleting ProxyRelationKnob input relation %s ", relation.getId().getQName()));
                  relationComponent.relations().remove(relation);
               });
            }
         }
      }
   }

   public static void deleteTemplateOutputRelations(BComponent template, List<String[]> templateDefs) {
      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(template);
      if (templateConfig != null) {
         List<BRelationInfo> relationInfos = templateConfig.getRelationInfos();

         for (BRelation relation : Arrays.asList(template.getComponentRelations())) {
            if (!(relation instanceof BLink)) {
               for (BRelationInfo relationInfo : relationInfos) {
                  if (!relationInfo.getInbound()
                     && relation.getRelationId().equals(relationInfo.getRelationId())
                     && isValidTemplateRelation(relationInfo.getRelationId(), "Outbound", templateDefs)) {
                     LOG.log(LOG_LEVEL, String.format("Deleting output relation %s ", relation.getId().getQName()));
                     template.relations().remove(relation);
                     break;
                  }
               }
            }
         }
      }
   }

   public static void deleteTemplateInputLinks(BComponent template, List<String[]> inputDefs) {
      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(template);
      if (templateConfig != null) {
         for (Slot slot : templateConfig.getInputSlots()) {
            if (isValidTemplateSlot(slot, inputDefs)) {
               for (BLink link : template.getLinks(slot)) {
                  if (link != null) {
                     LOG.log(LOG_LEVEL, String.format("Deleting input link %s <-> %s", link.getSourceSlotName(), link.getTargetSlotName()));
                     template.remove(link);
                  }
               }
            }
         }
      }
   }

   public static void deleteTemplateOutputLinks(BComponent template, List<String[]> outputDefs) {
      BTemplateConfig templateConfig = BTemplateConfig.getConfigForRoot(template);
      if (templateConfig != null) {
         for (Slot slot : templateConfig.getOutputSlots()) {
            if (isValidTemplateSlot(slot, outputDefs)) {
               Knob[] knobs = template.getKnobs(slot);

               for (Knob knob : knobs) {
                  String targetSlotName = knob.getTargetSlotName();
                  BComponent targetComponent;
                  if (knob instanceof LocalKnob) {
                     targetComponent = knob.getTargetComponent();
                  } else {
                     targetComponent = knob.getTargetOrd().resolve(template).get().asComponent();
                  }

                  Slot targetSlot = targetComponent.getSlot(targetSlotName);

                  for (BLink link : targetComponent.getLinks(targetSlot)) {
                     if (link.getSourceSlotName().contentEquals(slot.getName())) {
                        LOG.log(LOG_LEVEL, String.format("Deleting output link %s <-> %s", link.getSourceSlotName(), link.getTargetSlotName()));
                        targetComponent.remove(link);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isValidTemplateRelation(String relationId, String direction, List<String[]> templateDefs) {
      for (String[] relationDef : templateDefs) {
         if (relationDef.length >= 4 && relationDef[1] != null && relationDef[3] != null) {
            String id = relationDef[1];
            String dir = relationDef[3];
            if (id.contentEquals(relationId) && dir.contentEquals(direction)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean isValidTemplateSlot(Slot slot, List<String[]> templateDefs) {
      for (String[] slotDefs : templateDefs) {
         String slotName = slotDefs[0];
         if (slot.getName().contentEquals(slotName)) {
            return true;
         }
      }

      return false;
   }

   public static long getTemplateSignature(BComponent root) {
      HashMap<Object, BComponent> handleMap = getComponentHandleMap(root);
      CRC32 crc = new CRC32();
      int internalLinkCount = 0;
      int complexCount = 0;
      ArrayList<String> names = new ArrayList<>();

      for (BComplex bComplex : (BComplex[])CompUtil.getDescendants(root, BComplex.class)) {
         if (!isSubtemplateComp(root, bComplex)) {
            BComplex parent = bComplex.getParent();
            Property pip = bComplex.getPropertyInParent();
            if (pip != null && !pip.isFrozen()) {
               int flags = parent.getFlags(pip);
               int FLAGS = 81922;
               if ((flags & FLAGS) == 0) {
                  if (bComplex.getType().is(BLink.TYPE)) {
                     BLink link = (BLink)bComplex;
                     String ord = link.getSourceOrd().toString();
                     if (ord.startsWith("h:") && handleMap.containsKey(link.getSourceOrd())) {
                        internalLinkCount++;
                     }
                  } else if (!isExcludeType(bComplex)) {
                     complexCount++;
                     String s = bComplex.getName() + "_" + bComplex.getType().getDisplayName(null);
                     BTemplateService.logger.finest(root.getSlotPath() + " name =" + s);
                     names.add(s);
                  }
               }
            }
         }
      }

      Collections.sort(names);

      for (String name : names) {
         crc.update(name.getBytes());
      }

      BTemplateService.logger.finest(root.getSlotPath() + " internalLinkCount=" + internalLinkCount);
      BTemplateService.logger.finest(root.getSlotPath() + " complexCount     =" + complexCount);
      crc.update(internalLinkCount);
      return crc.getValue();
   }

   private static boolean isExcludeType(BComplex complex) {
      BComplex parent = complex.getParent();
      Type parentType = parent == null ? null : parent.getType();

      for (Type excludeType : EXCLUDE_TYPES) {
         if (excludeType.is(complex.getType())) {
            return true;
         }

         if (parentType != null && excludeType.is(parentType)) {
            return true;
         }
      }

      return false;
   }

   private static HashMap<Object, BComponent> getComponentHandleMap(BComponent root) {
      HashMap<Object, BComponent> handleMap = new HashMap<>();
      handleMap.put(root.getHandleOrd(), root);
      BComponent[] components = (BComponent[])CompUtil.getDescendants(root, BComponent.class);

      for (BComponent component : components) {
         handleMap.put(component.getHandleOrd(), component);
      }

      return handleMap;
   }

   private static boolean isSubtemplateComp(BComponent root, BComplex child) {
      BComponent comp = getParentTemplate(child);
      return comp != null && comp != root;
   }

   public static BComponent getParentTemplate(BComplex child) {
      for (BComplex parent = child.getParent(); parent != null; parent = parent.getParent()) {
         if (parent.isComponent() && parent.asComponent().get(ROOT_TAG_NAME) != null) {
            return parent.asComponent();
         }
      }

      return null;
   }

   private static class LinkTarget {
      String slot;
      BComponent target;

      LinkTarget(String slot, BComponent target) {
         this.slot = slot;
         this.target = target;
      }
   }

   private static class PropertyValue {
      String property;
      BValue value;

      PropertyValue(String property, BValue value) {
         this.property = property;
         this.value = value;
      }
   }

   public static class RelationSpec {
      final boolean isLink;
      final String relationId;
      final boolean wasInOldTemplate;
      final SlotPath sourcePath;
      final String sourceName;
      final SlotPath targetPath;
      final String targetName;

      public static UpgradeUtil.RelationSpec makeLinkSpec(SlotPath sourcePath, String sourceName, SlotPath targetPath, String targetName) {
         return new UpgradeUtil.RelationSpec(true, "n:dataLink", false, sourcePath, sourceName, targetPath, targetName);
      }

      public static UpgradeUtil.RelationSpec makeRelationSpec(String relationId, boolean wasInOldTemplate, SlotPath endpointPath, SlotPath targetPath) {
         return new UpgradeUtil.RelationSpec(false, relationId, wasInOldTemplate, endpointPath, "", targetPath, "");
      }

      private RelationSpec(
         boolean isLink, String relationId, boolean wasInOldTemplate, SlotPath sourcePath, String sourceName, SlotPath targetPath, String targetName
      ) {
         this.isLink = isLink;
         this.relationId = relationId;
         this.wasInOldTemplate = wasInOldTemplate;
         this.sourcePath = Objects.requireNonNull(sourcePath);
         this.sourceName = Objects.requireNonNull(sourceName);
         this.targetPath = Objects.requireNonNull(targetPath);
         this.targetName = Objects.requireNonNull(targetName);
      }

      @Override
      public int hashCode() {
         return Objects.hash(
            this.isLink, this.relationId, this.wasInOldTemplate, this.sourcePath.toString(), this.sourceName, this.targetPath.toString(), this.targetName
         );
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof UpgradeUtil.RelationSpec
            && this.isLink == ((UpgradeUtil.RelationSpec)o).isLink
            && this.relationId.equals(((UpgradeUtil.RelationSpec)o).relationId)
            && this.wasInOldTemplate == ((UpgradeUtil.RelationSpec)o).wasInOldTemplate
            && this.sourcePath.toString().equals(((UpgradeUtil.RelationSpec)o).sourcePath.toString())
            && this.sourceName.equals(((UpgradeUtil.RelationSpec)o).sourceName)
            && this.targetPath.toString().equals(((UpgradeUtil.RelationSpec)o).targetPath.toString())
            && this.targetName.equals(((UpgradeUtil.RelationSpec)o).targetName);
      }

      @Override
      public String toString() {
         return this.isLink
            ? this.sourcePath + "." + this.sourceName + "->" + this.targetPath + "." + this.targetName
            : this.relationId + ":" + this.sourcePath + "->" + this.targetPath;
      }
   }

   public static class TemplateSaveData {
      BWsAnnotation location = null;
      Hashtable<String, BValue> rootProperties = null;
      Hashtable<String, BValue> pxEditSaveValues = null;
      Hashtable<String, BPassword> passwords = null;
      Hashtable<String, BValue> configProperties = null;
      Hashtable<String, BLink> inputLinks = null;
      Hashtable<BOrd, ArrayList<BLink>> externalLinks = null;
      Hashtable<String, UpgradeUtil.LinkTarget[]> outputKnobs = null;
      Hashtable<BOrd, Hashtable<String, ArrayList<UpgradeUtil.LinkTarget>>> externalKnobs = null;
      ArrayList<BRelationInfo> relationInfos = null;
      BRelation[] outputRelations = null;
      Hashtable<String, BComponent> inputRelations = null;
      Hashtable<BOrd, ArrayList<BRelation>> externalOutputRelations = null;
      Hashtable<BOrd, Hashtable<String, ArrayList<BComponent>>> externalInputRelations = null;
      Hashtable<BOrd, ArrayList<Tag>> savedTags = null;
      Hashtable<BOrd, ArrayList<UpgradeUtil.PropertyValue>> savedPropertyValues = null;
   }

   private static class TypeProperties {
      Type type;
      String[] properties;

      TypeProperties(Type type, String[] properties) {
         this.type = type;
         this.properties = properties;
      }
   }
}
