package com.tridium.template.ui.file;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.install.BDependency;
import com.tridium.install.BVersion;
import com.tridium.neql.component.BNeqlComponentQueryHandler;
import com.tridium.sys.Nre;
import com.tridium.sys.module.NModule;
import com.tridium.sys.tag.ComponentTags;
import com.tridium.sys.transfer.TransferResult;
import com.tridium.sys.transfer.TransferStrategy;
import com.tridium.tagdictionary.BNiagaraTagDictionary;
import com.tridium.tagdictionary.util.ImportUtil;
import com.tridium.tagdictionary.util.TagDictionaryUtil.ComponentTagGroupChoices;
import com.tridium.template.BPasswordBinding;
import com.tridium.template.BTemplateConfig;
import com.tridium.template.BTemplateService;
import com.tridium.template.TemplateConst;
import com.tridium.template.file.BMemoryFileSpace;
import com.tridium.template.file.BMemoryImageFile;
import com.tridium.template.file.BNtplFile;
import com.tridium.template.file.EmbeddedPxScanner;
import com.tridium.template.file.EmbeddedPxSource;
import com.tridium.template.file.ImageFileRef;
import com.tridium.template.file.MemoryPxFileRef;
import com.tridium.template.file.NtplUtil;
import com.tridium.template.file.PxFileRef;
import com.tridium.template.file.TemplateManager;
import com.tridium.template.file.TemplateManager.TemplateInfo;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.manifest.TemplateManifest.Subtemplate;
import com.tridium.template.ui.BSelectInputDialog;
import com.tridium.template.ui.BSelectOutputDialog;
import com.tridium.template.ui.BSelectTagGroupDialog;
import com.tridium.template.ui.BTemplateManager;
import com.tridium.util.CompUtil;
import com.tridium.util.LinkUtil;
import com.tridium.util.PasswordUtil;
import com.tridium.workbench.util.BEditTagDialog;
import com.tridium.workbench.util.TagUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.BAbstractPxView;
import javax.baja.agent.BPxView;
import javax.baja.converters.BIBooleanToSimple;
import javax.baja.converters.BIEnumToSimple;
import javax.baja.converters.BINumericToSimple;
import javax.baja.converters.BIStatusToSimple;
import javax.baja.data.BIDataValue;
import javax.baja.file.BAbstractFile;
import javax.baja.file.BDataFile;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.file.BMemoryFileStore;
import javax.baja.file.FilePath;
import javax.baja.file.types.image.BImageFile;
import javax.baja.file.types.log.BILogFile;
import javax.baja.file.types.text.BCsvFile;
import javax.baja.file.types.text.BJsonFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.fox.BFoxProxySession;
import javax.baja.gx.BColor;
import javax.baja.gx.BImage;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.neql.NeqlQuery;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.search.BResultsRequest;
import javax.baja.search.BSearchParams;
import javax.baja.search.BSearchResult;
import javax.baja.search.BSearchResultSet;
import javax.baja.search.BSearchService;
import javax.baja.search.BSearchTask;
import javax.baja.security.BPassword;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.space.Mark;
import javax.baja.sys.ActionInvokeException;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIObject;
import javax.baja.sys.BLink;
import javax.baja.sys.BMarker;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelation;
import javax.baja.sys.BSimple;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Flags;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Tag;
import javax.baja.tag.TagDictionary;
import javax.baja.tag.TagDictionaryService;
import javax.baja.tag.TagGroupInfo;
import javax.baja.tag.TagInfo;
import javax.baja.tag.Tags;
import javax.baja.tagdictionary.BTagDictionary;
import javax.baja.tagdictionary.BTagDictionaryService;
import javax.baja.tagdictionary.BTagGroupInfo;
import javax.baja.tagdictionary.BTagGroupInfoList;
import javax.baja.tagdictionary.BTagInfoList;
import javax.baja.ui.BBinding;
import javax.baja.ui.BPicture;
import javax.baja.ui.BWidget;
import javax.baja.ui.px.PxDecoder;
import javax.baja.ui.px.PxProperty;
import javax.baja.util.BFormat;
import javax.baja.util.BUnrestrictedFolder;
import javax.baja.util.BUuid;
import javax.baja.util.CloseableIterator;
import javax.baja.util.Lexicon;
import javax.baja.xml.XElem;
import javax.baja.xml.XException;
import javax.baja.xml.XParser;

public final class TmplUtil {
   public static final int INPUT = 0;
   public static final int OUTPUT = 10;
   public static final int RELATION = 20;
   public static final int NO_HINTS = 1;
   public static final int NONE_FOUND = 2;
   public static final int NONE_SELECTED = 3;
   public static final int ADDED_BINDING = 4;
   public static final int NOT_LINKED = 5;
   public static final int NOT_LINKED_TYPES = 6;
   public static final int NOT_LINKED_NO_OUT = 7;
   public static final int NOT_LINKED_NOT_COMP = 8;
   public static final int EXCEPTION = 99;
   public static final Logger log = Logger.getLogger("ntpl");
   private static final Lexicon lex = Lexicon.make("template");
   private static int ACE_DEPTH = 4;

   private TmplUtil() {
   }

   public static Object[] findMatchingObjects(
      BTemplateManager.IoSlotInfo slotInfo, BSearchService searchService, BTemplateService templateService, BComponent base, Lexicon lex
   ) {
      try {
         return findMatchingObjects(slotInfo.bindHints, slotInfo.slotPathScope, slotInfo.resultsMessages, searchService, templateService, base, lex);
      } catch (Exception var6) {
         slotInfo.results = 99;
         slotInfo.otherName = var6.getMessage();
         slotInfo.otherSlot = var6.getCause().toString();
         return null;
      }
   }

   private static SlotPath resolveSlotPathScope(String slotPathScope) {
      if (slotPathScope != null && slotPathScope.length() != 0) {
         try {
            String[] rawNames = slotPathScope.split("/");
            ArrayList<String> escapedNames = new ArrayList<>();

            for (String rawName : rawNames) {
               if (rawName.length() > 0) {
                  escapedNames.add(SlotPath.escape(rawName));
               }
            }

            if (escapedNames.size() == 0) {
               return SlotPath.EMPTY_SLOT_PATH;
            } else {
               String[] pathNames = new String[escapedNames.size()];
               escapedNames.toArray(pathNames);
               return new SlotPath("slot", pathNames);
            }
         } catch (Exception var7) {
            log.warning(String.format("Template slot path scope %s is invalid: %s", slotPathScope, var7.getLocalizedMessage()));
            return SlotPath.EMPTY_SLOT_PATH;
         }
      } else {
         return SlotPath.EMPTY_SLOT_PATH;
      }
   }

   public static Object[] findMatchingObjects(String bindHints, BSearchService searchService, BTemplateService templateService, BComponent base, Lexicon lex) {
      return findMatchingObjects(bindHints, "", new ArrayList<>(), searchService, templateService, base, lex);
   }

   public static Object[] findMatchingObjects(
      String bindHints,
      String slotPathScope,
      List<String> resultsMessages,
      BSearchService searchService,
      BTemplateService templateService,
      BComponent base,
      Lexicon lex
   ) {
      BSpace space = base.getSpace();
      boolean isOffline = space instanceof BBogSpace;
      BComponent rootComponent = ((BComponentSpace)space).getRootComponent();
      if (isOffline || searchService == null) {
         return findMatchingObjects(bindHints, templateService, base, lex);
      } else if (!searchService.getStatus().isValid()) {
         throw new RuntimeException(lex.getText("templateManager.searchService.notValid.exception"));
      } else {
         BIObject scope = rootComponent;
         if (slotPathScope != null && slotPathScope.length() > 0) {
            SlotPath escapedPath = resolveSlotPathScope(slotPathScope);
            scope = BOrd.make("station:|" + escapedPath);
         }

         BOrd searchTask;
         try {
            BSearchParams bSearchParams = new BSearchParams("neql:" + bindHints, scope);
            searchTask = searchService.search(bSearchParams).relativizeToSession();
         } catch (ActionInvokeException var29) {
            if (!isSlotPathUnresolved(var29)) {
               throw var29;
            }

            log.log(
               Level.WARNING,
               String.format("Slot path scope '%s' cannot be resolved for bind hints '%s', so this search was not scoped", slotPathScope, bindHints)
            );
            resultsMessages.add(lex.getText("templateManager.unscopedTemplateResult", new Object[]{slotPathScope, bindHints}));
            BSearchParams bSearchParamsx = new BSearchParams("neql:" + bindHints, rootComponent);
            searchTask = searchService.search(bSearchParamsx).relativizeToSession();
         }

         try {
            templateService.getComponentSpace().sync();
         } catch (Exception var28) {
            log.log(Level.WARNING, "Error syncing template service component space:" + var28.getLocalizedMessage(), (Throwable)var28);
         }

         BObject bObject = searchTask.get(templateService);
         BSearchTask task = null;
         if (bObject instanceof BSearchTask) {
            task = (BSearchTask)bObject;
         }

         BSearchResultSet results = null;
         boolean resultsComplete = false;
         int startItem = 0;
         Array<BComponent> foundObjs = new Array(BComponent.class);
         long maxTicks = Clock.ticks() + 60000L;

         while (!resultsComplete && Clock.ticks() < maxTicks) {
            resultsComplete = results != null && results.getResultsComplete();
            BResultsRequest resultsRequest = BResultsRequest.make(searchTask, startItem, 10);
            results = searchService.retrieveResults(resultsRequest);
            startItem += results.getResultCount();
            SlotCursor<Property> props = results.getResults().getProperties();

            while (props.next()) {
               BSearchResult result = (BSearchResult)props.get();
               BOrd ord = result.getOrd().relativizeToSession();
               BIObject object = ord.get(templateService);
               BSimple epoch = result.getEpoch();
               if (object instanceof BComponent) {
                  BComponent comp = (BComponent)object;
                  foundObjs.add(comp);
               }
            }

            try {
               Thread.sleep(10L);
            } catch (Exception var27) {
               log.log(Level.WARNING, "Error idling thread:" + var27.getLocalizedMessage(), (Throwable)var27);
            }
         }

         return foundObjs.trim();
      }
   }

   private static boolean isSlotPathUnresolved(ActionInvokeException aie) {
      return aie.getCause() instanceof UnresolvedException
         ? true
         : aie.getCause() != null && aie.getCause().getMessage() != null && aie.getCause().getMessage().contains("UnresolvedException");
   }

   private static Object[] findMatchingObjects(String bindHints, BTemplateService templateService, BComponent base, Lexicon lex) {
      BComponent rootComponent = ((BComponentSpace)base.getSpace()).getRootComponent();
      NeqlQuery ordQuery = new NeqlQuery(bindHints);
      BNeqlComponentQueryHandler queryHandler = new BNeqlComponentQueryHandler();
      OrdTarget scope = rootComponent.getSlotPathOrd().resolve(base);

      try {
         CloseableIterator<Entity> results = queryHandler.query(scope, ordQuery);
         Throwable var9 = null;

         Object[] var26;
         try {
            Array<BComponent> foundObjs = new Array(BComponent.class);

            while (results.hasNext()) {
               Entity result = (Entity)results.next();
               if (result instanceof BComponent) {
                  BComponent comp = (BComponent)result;
                  foundObjs.add(comp);
               }
            }

            var26 = foundObjs.trim();
         } catch (Throwable var22) {
            var9 = var22;
            throw var22;
         } finally {
            if (results != null) {
               if (var9 != null) {
                  try {
                     results.close();
                  } catch (Throwable var21) {
                     var9.addSuppressed(var21);
                  }
               } else {
                  results.close();
               }
            }
         }

         return var26;
      } catch (RuntimeException var24) {
         throw var24;
      } catch (Exception var25) {
         throw new BajaRuntimeException(var25);
      }
   }

   public static Object selectInputSource(BWidget owner, BTemplateManager.IoSlotInfo ioInfo, BTemplateConfig config, boolean allowRemember, Lexicon lex) {
      List<Object> selectedComps = selectComponents(owner, ioInfo, false, allowRemember);
      if (selectedComps.isEmpty()) {
         return null;
      } else {
         TmplUtil.TargetChoice choice = new TmplUtil.TargetChoice();
         choice.targetPoint = (BComponent)selectedComps.get(0);
         int ordinal = 0;
         ArrayList<String> outSlots = new ArrayList<>();
         outSlots.add("out");
         if (ioInfo.targetSlotHints != null && !ioInfo.targetSlotHints.isEmpty()) {
            for (String slotHint : TextUtil.split(ioInfo.targetSlotHints, ',')) {
               if (!outSlots.contains(slotHint)) {
                  Property outProperty = choice.targetPoint.getProperty(slotHint.trim());
                  if (outProperty != null && !Flags.isHidden(choice.targetPoint, outProperty)) {
                     ordinal = 1;
                     outSlots.add(slotHint.trim());
                     break;
                  }
               }
            }
         }

         BEnumRange outRange = BEnumRange.make(outSlots.toArray(new String[0]));
         choice.targetSlotEnum = BDynamicEnum.make(ordinal, outRange);
         choice.selected = true;
         ArrayList<TmplUtil.TargetChoice> targetChoices = new ArrayList<>();
         targetChoices.add(choice);
         ioInfo.lastSelected = targetChoices;
         return targetChoices;
      }
   }

   public static Object selectRelationSource(BWidget owner, BTemplateManager.IoSlotInfo ioInfo, BTemplateConfig config, boolean allowRemember, Lexicon lex) {
      List<Object> selectedComps = selectComponents(owner, ioInfo, true, allowRemember);
      if (selectedComps.isEmpty()) {
         return null;
      } else {
         return ioInfo.allowMultiple ? selectedComps : selectedComps.get(0);
      }
   }

   private static List<Object> selectComponents(BWidget owner, BTemplateManager.IoSlotInfo ioInfo, boolean isRelation, boolean allowRemember) {
      if (ioInfo.choices != null && ioInfo.choices.length != 0) {
         ArrayList<BComponent> components = new ArrayList<>();

         for (Object linkSource : ioInfo.choices) {
            if (linkSource instanceof BComponent) {
               components.add((BComponent)linkSource);
            }
         }

         String userInfo = ioInfo.userTip;
         if (!ioInfo.resultsMessages.isEmpty()) {
            userInfo = ioInfo.userTip + " - " + joinResponseMessages(ioInfo.resultsMessages);
         }

         BSelectInputDialog selectDialog = new BSelectInputDialog(
            owner, ioInfo.title, ioInfo.prompt, userInfo, ioInfo.isInput, isRelation, ioInfo.allowMultiple, components, allowRemember
         );
         ArrayList<Object> selectedComps = selectDialog.openDialog();
         ioInfo.dontAskAgain = selectDialog.getDontAskAgain();
         if (selectDialog.getResult() == 2) {
            ioInfo.abort = true;
            return Collections.emptyList();
         } else if (selectedComps != null && !selectedComps.isEmpty()) {
            if (isRelation) {
               if (ioInfo.allowMultiple) {
                  ioInfo.lastSelected = selectDialog.getRemember() ? selectedComps : null;
               }

               ioInfo.lastSelected = selectDialog.getRemember() ? selectedComps.get(0) : null;
            }

            return selectedComps;
         } else {
            ioInfo.results = isRelation ? 23 : 3;
            return Collections.emptyList();
         }
      } else {
         return Collections.emptyList();
      }
   }

   public static Object selectOutputTargets(BWidget owner, BTemplateManager.IoSlotInfo ioInfo, BTemplateConfig config, boolean isMultiple, Lexicon lex) {
      if (ioInfo.choices != null && ioInfo.choices.length != 0) {
         ArrayList<TmplUtil.TargetChoice> targetChoices = new ArrayList<>();

         for (Object linkSource : ioInfo.choices) {
            if (linkSource instanceof BComponent) {
               BComponent component = (BComponent)linkSource;
               TmplUtil.TargetChoice choice = new TmplUtil.TargetChoice();
               choice.targetPoint = component;
               choice.targetSlotEnum = getComponentLinkableInputs(component, true);
               if (ioInfo.targetSlotHints != null && !ioInfo.targetSlotHints.isEmpty()) {
                  for (String slotHint : TextUtil.split(ioInfo.targetSlotHints, ',')) {
                     try {
                        choice.targetSlotEnum = (BDynamicEnum)choice.targetSlotEnum.getRange().get(slotHint.trim());
                        break;
                     } catch (InvalidEnumException var18) {
                     }
                  }
               }

               targetChoices.add(choice);
            }
         }

         ArrayList<TmplUtil.TargetChoice> selectResults = null;

         try {
            String userInfo = ioInfo.userTip;
            if (!ioInfo.resultsMessages.isEmpty()) {
               userInfo = ioInfo.userTip + " - " + joinResponseMessages(ioInfo.resultsMessages);
            }

            BSelectOutputDialog selectDialog = new BSelectOutputDialog(
               owner, ioInfo.title, ioInfo.prompt, userInfo, ioInfo.isInput, ioInfo.allowMultiple, targetChoices, isMultiple
            );
            ArrayList<TmplUtil.TargetChoice> tempResults = selectDialog.openDialog();
            ioInfo.dontAskAgain = selectDialog.getDontAskAgain();
            if (selectDialog.getResult() == 2) {
               ioInfo.abort = true;
               return null;
            }

            if (tempResults != null) {
               ioInfo.lastSelected = selectDialog.getRemember() ? tempResults : null;
               return tempResults;
            }

            ioInfo.results = ioInfo.isInput ? 3 : 13;
         } catch (Exception var17) {
            log.log(Level.WARNING, "Error selecting results:" + var17.getLocalizedMessage(), (Throwable)var17);
         }

         return null;
      } else {
         return null;
      }
   }

   private static String joinResponseMessages(List<String> responseMessages) {
      StringJoiner responses = new StringJoiner("\n");

      for (String response : responseMessages) {
         responses.add(response);
      }

      return responses.toString();
   }

   public static int selectTagGroupChoices(BWidget owner, ArrayList<ComponentTagGroupChoices> compTagGroupChoices, Lexicon lex) {
      if (compTagGroupChoices != null && !compTagGroupChoices.isEmpty()) {
         StringBuilder infoSb = new StringBuilder();
         infoSb.append(lex.getText("selectTagGroupDialog.info1"));
         infoSb.append("\n");
         infoSb.append(lex.getText("selectTagGroupDialog.info2"));
         infoSb.append("\n");
         infoSb.append(lex.getText("selectTagGroupDialog.info3"));
         BSelectTagGroupDialog selectDialog = new BSelectTagGroupDialog(
            owner, lex.getText("selectTagGroupDialog.title"), "", infoSb.toString(), false, false, true, compTagGroupChoices, false
         );
         selectDialog.setIsDeploy(false);
         ArrayList<Object> selectedComps = selectDialog.openDialog();
         return selectDialog.getResult();
      } else {
         return 2;
      }
   }

   public static TagDictionaryService makeTagDictionaryService() {
      var service = (BComponent & TagDictionaryService)TagUtil.getTagDictionaryServiceFromPalettes();
      HashMap<String, TagDictionary> namespaceMap = new HashMap<>();
      if (service instanceof BTagDictionaryService) {
         BTagDictionaryService tdService = (BTagDictionaryService)service;

         for (TagDictionary tagDictionary : tdService.getTagDictionaries()) {
            if (!namespaceMap.containsKey(tagDictionary.getNamespace())) {
               namespaceMap.put(tagDictionary.getNamespace(), tagDictionary);
            }
         }

         for (TagDictionary tagDictionaryx : getTagDictionariesFromPalettes()) {
            checkAddDictionary(tagDictionaryx, service, namespaceMap);
         }

         for (TagDictionary tagDictionaryx : TagUtil.getUserTagDictionaries()) {
            checkAddDictionary(tagDictionaryx, service, namespaceMap);
         }

         BNiagaraTagDictionary niagaraStandardDictionary = (BNiagaraTagDictionary)tdService.getNiagara().getStandardDictionary();
         tdService.setNiagara((BNiagaraTagDictionary)niagaraStandardDictionary.newCopy());
      }

      return (TagDictionaryService)service;
   }

   static void checkAddDictionary(TagDictionary tagDictionary, BComponent service, HashMap<String, TagDictionary> namespaceMap) {
      String namespace = tagDictionary.getNamespace();
      if (!namespaceMap.containsKey(namespace)) {
         BComponent tagDictionaryComponent = (BComponent)tagDictionary;
         if (service.get(tagDictionaryComponent.getName()) == null) {
            if (tagDictionaryComponent instanceof BTagDictionary) {
               BTagDictionary alternateTagDictionary = ((BTagDictionary)tagDictionaryComponent).getStandardDictionary();
               BOrd importOrd = ((BTagDictionary)tagDictionaryComponent).getImportDictionaryOrd();
               BValue defaultImportOrd = tagDictionaryComponent.getProperty("importDictionaryOrd").getDefaultValue();
               if (alternateTagDictionary != null) {
                  tagDictionaryComponent = alternateTagDictionary;
               } else if (!defaultImportOrd.equals(importOrd)) {
                  try {
                     BIFile inFile = (BIFile)importOrd.resolve(tagDictionaryComponent).get();
                     if (inFile instanceof BJsonFile || inFile instanceof BCsvFile) {
                        ImportUtil.ImportTagDictionary((BTagDictionary)tagDictionaryComponent, importOrd);
                     }
                  } catch (UnresolvedException var11) {
                  }
               }

               BTagInfoList tagInfoList = ((BTagDictionary)tagDictionaryComponent).getTagDefinitions();
               BTagGroupInfoList tagGroupInfoList = ((BTagDictionary)tagDictionaryComponent).getTagGroupDefinitions();
               if (tagInfoList.iterator().hasNext() || tagGroupInfoList.iterator().hasNext()) {
                  Property addedTd = service.add(tagDictionaryComponent.getName(), tagDictionaryComponent.newCopy());
                  service.setDisplayName(addedTd, BFormat.make(tagDictionary.getDisplayName(null)), null);
                  namespaceMap.put(namespace, tagDictionary);
               }
            }
         }
      }
   }

   static Collection<TagDictionary> getTagDictionariesFromPalettes() {
      ArrayList<TagDictionary> result = new ArrayList<>();

      for (String moduleName : new String[]{"haystack", "brick"}) {
         try {
            Nre.getModuleManager().loadModule(moduleName, RuntimeProfile.rt);
         } catch (Exception var13) {
         }
      }

      for (NModule module : Nre.getModuleManager().getModules()) {
         if (!module.isTransient() && !module.isSynthetic()) {
            boolean hasPalette = false;

            try {
               hasPalette = module.bmodule().hasPalette();
            } catch (Exception var12) {
            }

            if (hasPalette) {
               BINavNode node = Sys.loadModule(module.getModuleName()).getNavChild("module.palette");
               BINavNode[] entries = node.getNavChildren();

               for (BINavNode entry : entries) {
                  if (entry instanceof TagDictionaryService) {
                     entry = entry.getNavChild("Niagara");
                  }

                  if (entry != null) {
                     if (entry.getType().is(BTagDictionary.TYPE)) {
                        if (result.contains(entry)) {
                           continue;
                        }

                        result.add((TagDictionary)entry);
                     }

                     if (entry instanceof BUnrestrictedFolder) {
                        addTagDictionariesFromPaletteFolder((BUnrestrictedFolder)entry, result);
                     }
                  }
               }
            }
         }
      }

      return result;
   }

   static void addTagDictionariesFromPaletteFolder(BUnrestrictedFolder folder, ArrayList<TagDictionary> result) {
      BINavNode[] entries = folder.getNavChildren();

      for (BINavNode entry : entries) {
         if (entry.getType().is(BTagDictionary.TYPE) && !result.contains(entry)) {
            result.add((TagDictionary)entry);
         }

         if (entry instanceof BUnrestrictedFolder) {
            addTagDictionariesFromPaletteFolder((BUnrestrictedFolder)entry, result);
         }
      }
   }

   private static BDynamicEnum getComponentLinkableInputs(BComponent component, boolean isInput) {
      ArrayList<String> slotList = new ArrayList<>();
      slotList.add(SlotPath.escape("----"));
      Property[] componentProperties = component.getPropertiesArray();

      for (Property slot : componentProperties) {
         if ((!Flags.isReadonly(component, slot) || isInput)
            && (Flags.isFanIn(component, slot) || !component.isLinkTarget(slot))
            && !Flags.isHidden(component, slot)) {
            slotList.add(slot.getName());
         }
      }

      BEnumRange slotRange = BEnumRange.make(slotList.toArray(new String[0]));
      return BDynamicEnum.make(0, slotRange);
   }

   public static TmplUtil.BindInfo formatBindResults(BTemplateManager.IoSlotInfo slotInfo, Lexicon lex) {
      TmplUtil.BindInfo info = null;
      switch (slotInfo.results) {
         case 1:
         case 11:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.noBindHints", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.bindHints}), BColor.red
            );
            break;
         case 2:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.noSourcesFound", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.bindHints}), BColor.red
            );
            break;
         case 3:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.input.notLinked.notSelected", new Object[]{slotInfo.rootName, slotInfo.slot.getName()}), BColor.red
            );
            break;
         case 4:
            info = new TmplUtil.BindInfo(
               lex.getText(
                  "templateManager.linkedTemplateInput", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.otherName, slotInfo.otherSlot}
               ),
               BColor.black
            );
            break;
         case 6:
            info = new TmplUtil.BindInfo(
               lex.getText(
                  "templateManager.input.notLinked.types", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.otherName, slotInfo.otherSlot}
               ),
               BColor.red
            );
            break;
         case 7:
            String slotName = "out";
            if (slotInfo.targetSlotHints != null && !slotInfo.targetSlotHints.isEmpty()) {
               slotName = slotInfo.targetSlotHints;
            }

            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.input.notLinked.noOut", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.otherName, slotName}),
               BColor.red
            );
            break;
         case 8:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.input.notLinked.notComponent", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.otherName}),
               BColor.red
            );
            break;
         case 12:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.noTargetsFound", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.bindHints}), BColor.red
            );
            break;
         case 13:
            info = new TmplUtil.BindInfo(lex.getText("templateManager.outputNotLinked", new Object[]{slotInfo.rootName, slotInfo.slot.getName()}), BColor.red);
            break;
         case 14:
            info = new TmplUtil.BindInfo(
               lex.getText(
                  "templateManager.linkedTemplateOutput", new Object[]{slotInfo.rootName, slotInfo.slot.getName(), slotInfo.otherName, slotInfo.otherSlot}
               ),
               BColor.black
            );
            break;
         case 15:
            info = new TmplUtil.BindInfo(lex.getText("templateManager.outputNotLinked", new Object[]{slotInfo.rootName, slotInfo.slot.getName()}), BColor.red);
            break;
         case 21:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.relateNoBindHints", new Object[]{slotInfo.rootName, slotInfo.relationInfo.getRelationId(), slotInfo.bindHints}),
               BColor.red
            );
            break;
         case 22:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.noRelationFound", new Object[]{slotInfo.rootName, slotInfo.relationInfo.getRelationId(), slotInfo.bindHints}),
               BColor.red
            );
            break;
         case 23:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.relate.notRelated.notSelected", new Object[]{slotInfo.rootName, slotInfo.relationInfo.getRelationId()}), BColor.red
            );
            break;
         case 24:
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.addedTemplateRelation", new Object[]{slotInfo.relationInfo.getRelationId(), slotInfo.otherSlot, slotInfo.otherName}),
               BColor.black
            );
            break;
         case 99:
            String exception = slotInfo.otherName != null ? slotInfo.otherName : slotInfo.otherSlot;
            String slorOrRelation = slotInfo.slot != null
               ? slotInfo.slot.getName()
               : (slotInfo.relationInfo != null ? slotInfo.relationInfo.getRelationId() : "?");
            info = new TmplUtil.BindInfo(
               lex.getText("templateManager.search.exception", new Object[]{slotInfo.rootName, slorOrRelation, slotInfo.bindHints, exception}), BColor.red
            );
      }

      return info;
   }

   private static BAbstractPxView getView(TypeInfo typInfo) {
      BPxView pv = new BPxView();
      if (typInfo != null) {
         pv.setMedia(typInfo.getTypeSpec());
      }

      return pv;
   }

   private static void getPxImageRefs(BWidget widgetRoot, ArrayList<ImageFileRef> imageRefs) {
      try {
         BComplex[] complexes = (BComplex[])CompUtil.getDescendants(widgetRoot, BComplex.class);

         for (int j = 0; j < complexes.length; j++) {
            SlotCursor<Property> cursor = complexes[j].getProperties();

            while (cursor.next()) {
               BValue bValue = cursor.get();
               Type typ = bValue.getType();
               if (typ.is(BPicture.TYPE)) {
                  addImage(((BPicture)bValue).getImage(), imageRefs);
               } else if (typ.is(BINumericToSimple.TYPE)) {
                  addImages(((BINumericToSimple)bValue).getMap().getValues(), imageRefs);
               } else if (typ.is(BIEnumToSimple.TYPE)) {
                  addImages(((BIEnumToSimple)bValue).getMap().getValues(), imageRefs);
               } else if (typ.is(BIBooleanToSimple.TYPE)) {
                  addImage(((BIBooleanToSimple)bValue).getTrueValue(), imageRefs);
                  addImage(((BIBooleanToSimple)bValue).getFalseValue(), imageRefs);
               } else if (typ.is(BIStatusToSimple.TYPE)) {
                  BIStatusToSimple ist = (BIStatusToSimple)bValue;
                  Property[] p = ist.getFrozenPropertiesArray();

                  for (int i = 0; i < p.length; i++) {
                     BValue v = ist.get(p[0]);
                     if (v instanceof BImage) {
                        addImage((BImage)v, imageRefs);
                     }
                  }
               } else if (typ.is(BImage.TYPE)) {
                  addImage((BImage)bValue, imageRefs);
               }
            }
         }
      } catch (Exception var11) {
         log.log(Level.WARNING, "Error getting PX image references:" + var11.getLocalizedMessage(), (Throwable)var11);
      }
   }

   private static void relativizeOrds(BComplex cplx, BWidget w) {
      if (cplx.isComponent()) {
         SlotPath slotPath = cplx.asComponent().getSlotPath();
         String[] baseNames = slotPath.getNames();
         BBinding[] blbs = (BBinding[])CompUtil.getDescendants(w, BBinding.class);

         for (int i = 0; i < blbs.length; i++) {
            BOrd ord = blbs[i].getOrd();
            BOrd newOrd = relativizeOrd(baseNames, ord);
            if (newOrd != null) {
               blbs[i].setOrd(newOrd);
            }
         }
      }
   }

   public static BImage[] getPxImageOrds(BComponent root) {
      BPxView[] pxViews = (BPxView[])CompUtil.getDescendants(root, BPxView.class);
      Array<BImage> imageArray = new Array(BImage.class);

      for (int i = 0; i < pxViews.length; i++) {
         BOrd pxFileOrd = pxViews[i].getPxFile();
         BIFile pxFile = (BIFile)pxFileOrd.resolve(root).get();
         PxDecoder decoder = null;

         try {
            decoder = new PxDecoder(pxFile);
            BWidget widgetRoot = decoder.decodeDocument();
            PxProperty[] props = decoder.getPxProperties();
            BImage[] descendantsOrds = (BImage[])CompUtil.getDescendants(widgetRoot, BImage.class);

            for (int j = 0; j < descendantsOrds.length; j++) {
               imageArray.add((BImage)descendantsOrds[j].newCopy());
            }
         } catch (Exception var11) {
            log.log(Level.WARNING, "Error getting PX image ORDs:" + var11.getLocalizedMessage(), (Throwable)var11);
         }
      }

      return (BImage[])imageArray.trim();
   }

   private static void addImages(BSimple[] sa, ArrayList<ImageFileRef> imageRefs) {
      for (int i = 0; i < sa.length; i++) {
         addImage(sa[i], imageRefs);
      }
   }

   private static void addImage(BSimple s, ArrayList<ImageFileRef> imageRefs) {
      if (s instanceof BImage) {
         addImage((BImage)s, imageRefs);
      }
   }

   private static void addImage(BImage im, ArrayList<ImageFileRef> imageRefs) {
      if (!im.getOrdList().toString().startsWith("module:")) {
         imageRefs.add(new ImageFileRef(im));
      }
   }

   public static BNtplFile createInMemoryNtpl(BComponent c) throws Exception {
      return createInMemoryNtplImpl(c, false);
   }

   public static BNtplFile createInMemoryApp(BComponent c) throws Exception {
      return createInMemoryNtplImpl(c, true);
   }

   private static BNtplFile createInMemoryNtplImpl(BComponent c, boolean createAsApp) throws Exception {
      String myNam = c.getName();
      BComponent templateRoot = null;
      TemplateManifest manifest = new TemplateManifest();
      manifest.newCreation = true;
      manifest.vendor = Sys.getBajaModule().getVendor(RuntimeProfile.rt);
      manifest.isStation = c.getType().is(BStation.TYPE);
      if (manifest.isStation) {
         manifest.isApplication = createAsApp;
         myNam = ((BStation)c).getStationName();
         templateRoot = c;
      }

      ArrayList<TmplUtil.LinkInfo> extLinks = listExternalLinks(c);
      ArrayList<TmplUtil.PasswordInfo> intPSWs = listInternalBPasswords(c);
      ArrayList<TmplUtil.TagGroupRelationInfo> tagGroupRelationInfos = listTagGroupRelations(c);
      BUnrestrictedFolder folder = new BUnrestrictedFolder();
      BComponentSpace space = new BComponentSpace(null, null, null);
      space.setRootComponent(folder);
      Mark mark = new Mark(c, myNam);
      BHistoryExt[] sourceHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(c, BHistoryExt.class);
      BComponent params = null;
      if (manifest.isStation || manifest.isApplication) {
         params = new BComponent();
         params.add("exactConfig", BBoolean.TRUE);
      }

      TransferResult transferResult = TransferStrategy.make(16, mark, folder, params, null).transfer();
      BComponent copiedRoot = folder.get(transferResult.getInsertNames()[0]).asComponent();
      BHistoryExt[] copiedHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(folder, BHistoryExt.class);
      HashMap<String, Boolean> historyMap = new HashMap<>();

      for (BHistoryExt ext : sourceHistoryExtensions) {
         CompUtil.slotPathFromAncestor(c, ext).ifPresent(slotPath -> {
            Boolean var10000 = historyMap.put(slotPath.toString(), ext.getEnabled());
         });
      }

      if (c instanceof BHistoryExt) {
         CompUtil.slotPathFromAncestor(c, c).ifPresent(slotPath -> {
            Boolean var10000 = historyMap.put(slotPath.toString(), ((BHistoryExt)c).getEnabled());
         });
      }

      for (BHistoryExt ext : copiedHistoryExtensions) {
         Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(copiedRoot, ext);
         boolean enabled = historyMap.get(slotPath.isPresent() ? slotPath.get().toString() : "");
         ext.setEnabled(enabled);
      }

      if (!manifest.isStation) {
         try {
            templateRoot = folder.getChildComponents()[0];
            BTemplateConfig.removeConfigFromRoot(templateRoot);
            folder.setDisplayName(templateRoot.getPropertyInParent(), BFormat.DEFAULT, null);
            addExternalLinks(templateRoot, extLinks);
            addPasswordConfig(templateRoot, intPSWs);
            addTagGroupTags(templateRoot, tagGroupRelationInfos);
         } catch (Exception var32) {
            log.log(Level.WARNING, "Error creating in-memory NTPL file:" + var32.getLocalizedMessage(), (Throwable)var32);
         }
      }

      PasswordUtil.forceClearReversiblePasswords(folder);
      BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore(myNam + ".bog");
      BBogFile bogFile = new BBogFile(fs);
      fs.setFile(bogFile);
      ValueDocEncoder encoder = new ValueDocEncoder(bogFile.getOutputStream());
      encoder.setZipped(true);
      encoder.setEncodeComments(true);
      encoder.encodeDocument(folder);
      encoder.close();

      for (TmplUtil.SubtemplateInfo stInfo : listSubtemplates(templateRoot)) {
         manifest.addSubtemplate(
            stInfo.getDeployName(), stInfo.getVendor(), stInfo.getVersion(), stInfo.getDeployOrd().toString(), stInfo.getNtplFileOrd().toString()
         );
      }

      PxFileRef[] pxRefs = null;
      BImageFile[] imageFiles = null;
      BDataFile[] stationFiles = null;
      if (!manifest.isStation && !manifest.isApplication) {
         ArrayList<String> pxOrds = new ArrayList<>();
         ArrayList<String> imageOrds = new ArrayList<>();
         findPxAndImageOrds(c, pxOrds, imageOrds);
         ArrayList<PxFileRef> pxFileRefArray = new ArrayList<>();
         ArrayList<BImageFile> imageFileArray = new ArrayList<>();
         loadPxFilesIntoMemory(c, pxOrds, pxFileRefArray);
         loadImageFilesIntoMemory(c, imageOrds, imageFileArray);
         pxRefs = pxFileRefArray.toArray(new PxFileRef[0]);
         imageFiles = imageFileArray.toArray(new BImageFile[0]);

         for (PxFileRef ref : pxRefs) {
            manifest.addResource(ref.getPxName(), "px", ((MemoryPxFileRef)ref).getFileSystemOrd().encodeToString());
         }

         for (BImageFile image : imageFiles) {
            manifest.addResource(image.getFileName(), "image", ((BMemoryImageFile)image).getLocalImageOrd().encodeToString());
         }
      } else {
         ArrayList<BDataFile> stationFileList = new ArrayList<>();
         addInMemoryStationFiles(stationFileList, c, "^", manifest);
         if (!stationFileList.isEmpty()) {
            stationFiles = stationFileList.toArray(new BDataFile[0]);
         }
      }

      fs = BMemoryFileSpace.INSTANCE.makeMemoryStore(myNam + (createAsApp ? ".napl" : ".ntpl"));
      BWbDeployableNtplFile nf = new BWbDeployableNtplFile(fs, manifest, bogFile, pxRefs, imageFiles, stationFiles);
      fs.setFile(nf);
      return nf;
   }

   private static void findPxAndImageOrds(BComponent c, ArrayList<String> pxOrds, ArrayList<String> imageOrds) {
      EmbeddedPxScanner.findPxAndImageOrds(new TmplUtil.LocalPxSource(c), c, pxOrds, imageOrds);
   }

   private static void loadPxFilesIntoMemory(BComponent root, List<String> ords, List<PxFileRef> pxFileRefArray) throws IOException {
      Set<String> resourceFileNames = new HashSet<>();

      for (String ord : ords) {
         BIFile file;
         try {
            file = (BIFile)BOrd.make(ord).resolve(root).get();
         } catch (UnresolvedException var43) {
            log.log(Level.WARNING, "Missing px file: " + ord, (Throwable)var43);
            continue;
         }

         String resourceFileName = addUniqueFileNameToSet(file.getFileName(), resourceFileNames);
         BMemoryFileStore store = BMemoryFileSpace.INSTANCE.makeMemoryStore("px/" + resourceFileName);
         BPxFile newFile = new BPxFile(store);
         MemoryPxFileRef ref = new MemoryPxFileRef(newFile, file.getOrdInSpace(), newFile.getAbsoluteOrd(), newFile.getFileName());

         try (
            InputStream in = file.getInputStream();
            OutputStream out = newFile.getOutputStream();
         ) {
            FileUtil.pipe(in, out);
         }

         pxFileRefArray.add(ref);
      }
   }

   private static void loadImageFilesIntoMemory(BComponent root, List<String> ords, List<BImageFile> imageFiles) throws IOException {
      Set<String> resourceFileNames = new HashSet<>();

      for (String ord : ords) {
         BIFile file;
         try {
            file = (BIFile)BOrd.make(ord).resolve(root).get();
         } catch (UnresolvedException var42) {
            log.log(Level.WARNING, "Missing image file: " + ord, (Throwable)var42);
            continue;
         }

         String resourceFileName = addUniqueFileNameToSet(file.getFileName(), resourceFileNames);
         BMemoryFileStore store = BMemoryFileSpace.INSTANCE.makeMemoryStore("images/" + resourceFileName);
         BMemoryImageFile newFile = new BMemoryImageFile(store, file.getOrdInSpace());

         try (
            InputStream in = file.getInputStream();
            OutputStream out = newFile.getOutputStream();
         ) {
            FileUtil.pipe(in, out);
         }

         imageFiles.add(newFile);
      }
   }

   private static String addUniqueFileNameToSet(String fileName, Set<String> fileNames) {
      String uniqueFileName = fileName;
      if (fileNames.contains(fileName)) {
         int suffix = 1;
         String namePart = null;
         String extPart = null;
         int extStart = fileName.lastIndexOf(46);
         if (extStart > 0) {
            namePart = fileName.substring(0, extStart);
            extPart = fileName.substring(extStart + 1);
         }

         do {
            if (namePart == null) {
               uniqueFileName = fileName + suffix++;
            } else {
               uniqueFileName = namePart + suffix++ + "." + extPart;
            }
         } while (fileNames.contains(uniqueFileName));
      }

      fileNames.add(uniqueFileName);
      return uniqueFileName;
   }

   private static void addInMemoryStationFiles(ArrayList<BDataFile> stationFileList, BComponent rootComponent, String baseFilePath, TemplateManifest manifest) {
      BOrd fileOrd = BOrd.make(rootComponent.getOrdInHost().toString() + "|file:" + baseFilePath);
      if (fileOrd != null) {
         try {
            fileOrd.resolve(rootComponent).get();
            addInMemoryStationFiles(stationFileList, rootComponent, fileOrd, manifest);
         } catch (Exception var6) {
         }
      }
   }

   private static void addInMemoryStationFiles(ArrayList<BDataFile> stationFiles, BComponent rootComponent, BOrd ord, TemplateManifest manifest) throws FileAlreadyExistsException {
      BObject bObj = ord.resolve(rootComponent).get();
      if (bObj instanceof BDirectory) {
         BDirectory dir = (BDirectory)bObj;
         BINavNode[] children = dir.getNavChildren();
         if (children != null && children.length > 0) {
            for (BINavNode n : children) {
               if (n instanceof BAbstractFile) {
                  addInMemoryStationFiles(stationFiles, rootComponent, ((BAbstractFile)n).getAbsoluteOrd(), manifest);
               }
            }
         }
      } else if (bObj instanceof BDataFile && !(bObj instanceof BILogFile)) {
         BDataFile dFile = (BDataFile)bObj;
         String dFileOrd = dFile.getOrdInSpace().encodeToString();
         String dFileOrdTruncated = dFileOrd.replace("file:^", "shared/");
         String fname = dFile.getFileName();
         BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore(dFileOrdTruncated);
         BDataFile newF = new BDataFile(fs);
         fs.setFile(newF);
         NtplUtil.copyFile(dFile, newF);
         stationFiles.add(newF);
         manifest.addResource(fname, "data", dFileOrd);
      }
   }

   public static ArrayList<TmplUtil.SubtemplateInfo> listSubtemplates(BComponent root) {
      BTemplateConfig[] subTmplCfg = (BTemplateConfig[])CompUtil.getDescendants(root, BTemplateConfig.class);
      ArrayList<TmplUtil.SubtemplateInfo> stAl = new ArrayList<>();
      TemplateManager tmInstance = new TemplateManager();
      tmInstance.initTemplateMap();

      for (BTemplateConfig tc : subTmplCfg) {
         BComplex tcParent = tc.getParent();
         if (tcParent != root) {
            BComponent stRoot = tcParent.asComponent();
            BUuid uID = tc.getManifest().uID;
            Optional<BIDataValue> optVendor = stRoot.tags().get(TemplateConst.TEMPLATE_VENDOR_TAG_ID);
            Optional<BIDataValue> optVersion = stRoot.tags().get(TemplateConst.TEMPLATE_VERSION_TAG_ID);
            String tVersion = optVersion.isPresent() ? optVersion.get().toString() : "";
            if (optVendor.isPresent()) {
               String tVendor = optVendor.get().toString();
               TemplateInfo templateInfo = null;
               if (uID != null) {
                  templateInfo = tmInstance.getTemplate(uID, tVendor);
               } else {
                  templateInfo = tmInstance.getTemplate(tc.getTemplateName(), tVendor);
               }

               if (templateInfo == null) {
                  System.out.println("Template doesn't exist in WB: " + tVendor + ":" + uID);
               } else {
                  BOrd ntpFileOrd = templateInfo.getNtpFileOrd();
                  stAl.add(new TmplUtil.SubtemplateInfo(stRoot.getName(), tVendor, tVersion, stRoot.getSlotPathOrd(), ntpFileOrd));
               }
            }
         }
      }

      return stAl;
   }

   public static ArrayList<TmplUtil.PasswordInfo> listInternalBPasswords(BComponent root) {
      BOrd rootOrd = root.getSlotPathOrd();
      ArrayList<TmplUtil.PasswordInfo> intPSWs = new ArrayList<>();
      ArrayList<BComponent> componentList = new ArrayList<>();
      listComponents(componentList, root);
      String[] namesRoot = root.getSlotPath().getNames();

      for (BComponent comp : componentList) {
         if (!(comp instanceof BTemplateConfig)) {
            for (Property property : comp.getProperties()) {
               if (!comp.get(property).isComponent()) {
                  TmplUtil.PasswordOrdProp ordProp = findPasswordProp(comp, property, namesRoot);
                  if (ordProp != null) {
                     if (ordProp.parentProperty == null) {
                        intPSWs.add(new TmplUtil.PasswordInfo(ordProp.parent, ordProp.property.getName(), ordProp.isDynamic));
                     } else {
                        intPSWs.add(new TmplUtil.PasswordInfo(ordProp.parent, ordProp.parentProperty.getName(), ordProp.property.getName(), ordProp.isDynamic));
                     }
                  }
               }
            }
         }
      }

      return intPSWs;
   }

   public static TmplUtil.PasswordOrdProp findPasswordProp(BComponent parentComp, Property property, String[] namesRoot) {
      BValue value = parentComp.get(property);
      if (value.getType().is(BPassword.TYPE)) {
         return new TmplUtil.PasswordOrdProp(relativizeOrd(namesRoot, parentComp.getSlotPathOrd()), property, property.isDynamic());
      } else {
         if (value.getType().is(BComplex.TYPE)) {
            for (Property complexProp : value.asComplex().getProperties()) {
               BValue complexPropValue = value.asComplex().get(complexProp);
               if (complexPropValue.getType().is(BPassword.TYPE)) {
                  return new TmplUtil.PasswordOrdProp(relativizeOrd(namesRoot, parentComp.getSlotPathOrd()), property, complexProp, complexProp.isDynamic());
               }
            }
         }

         return null;
      }
   }

   public static boolean hasPasswordProp(BComplex complex) {
      for (Property complexProp : complex.getProperties()) {
         BValue complexPropValue = complex.get(complexProp);
         if (complexPropValue.getType().is(BPassword.TYPE)) {
            return true;
         }
      }

      return false;
   }

   public static ArrayList<TmplUtil.LinkInfo> listExternalLinks(BComponent root) {
      BOrd rootOrd = root.getSlotPathOrd();
      ArrayList<TmplUtil.LinkInfo> extLinks = new ArrayList<>();
      ArrayList<BComponent> componentList = new ArrayList<>();
      listComponents(componentList, root);
      HashMap<Object, BComponent> handleMap = new HashMap<>();

      for (BComponent component : componentList) {
         handleMap.put(component.getHandleOrd(), component);
      }

      String[] namesRoot = root.getSlotPath().getNames();

      for (BComponent comp : componentList) {
         for (BLink link : comp.getLinks()) {
            if (!LinkUtil.isCompositeLink(link)) {
               String ord = link.getSourceOrd().toString();
               if (ord.startsWith("h:") && !handleMap.containsKey(link.getSourceOrd())) {
                  if (link.getParent().equals(root)) {
                     int flags = root.getFlags(root.getSlot(link.getTargetSlotName()));
                     if ((flags & 4096) == 0) {
                        continue;
                     }
                  }

                  try {
                     BComponent linkSource = link.getSourceOrd().resolve(root).get().asComponent();
                     String bindHints = getBindHints(linkSource);
                     BOrd bOrdComp = relativizeOrd(namesRoot, comp.getSlotPathOrd());
                     extLinks.add(new TmplUtil.LinkInfo(bOrdComp, link.getTargetSlotName(), true, bindHints));
                  } catch (Exception var24) {
                     log.log(Level.WARNING, "LinkSource error", (Throwable)var24);
                  }
               }
            }
         }

         for (Knob knob : comp.getKnobs()) {
            BOrd targetOrd = knob.getTargetOrd();
            String ord = targetOrd.toString();
            if (ord.startsWith("h:") && !handleMap.containsKey(targetOrd)) {
               BComponent targetComp = targetOrd.resolve(root).get().asComponent();
               targetComp.lease();
               Slot targetSlot = targetComp.getSlot(knob.getTargetSlotName());
               BLink[] links = targetComp.getLinks(targetSlot);

               for (BLink linkx : links) {
                  if (!LinkUtil.isCompositeLink(linkx)
                     && !comp.getHandleOrd().equals(root.getHandleOrd())
                     && linkx.getSourceSlotName().equals(knob.getSourceSlotName())
                     && linkx.getSourceOrd().equals(comp.getHandleOrd())) {
                     String bindHints = getBindHints(targetComp);
                     BOrd slotPathOrd = comp.getSlotPathOrd();
                     if (slotPathOrd != null) {
                        BOrd bOrdComp = relativizeOrd(namesRoot, slotPathOrd);
                        extLinks.add(new TmplUtil.LinkInfo(bOrdComp, linkx.getSourceSlotName(), false, bindHints));
                     }
                  }
               }
            }
         }
      }

      return extLinks;
   }

   public static ArrayList<TmplUtil.TagGroupRelationInfo> listTagGroupRelations(BComponent root) {
      ArrayList<TmplUtil.TagGroupRelationInfo> tagGroupInfo = new ArrayList<>();
      ArrayList<BComponent> componentList = new ArrayList<>();
      String[] namesRoot = root.getSlotPath().getNames();
      listComponents(componentList, root);

      for (BComponent comp : componentList) {
         BRelation[] relations = (BRelation[])comp.getChildren(BRelation.class);

         for (BRelation relation : relations) {
            if (relation.getId().equals(BNiagaraTagDictionary.TAG_GROUP_RELATION)) {
               Entity endpoint = relation.getEndpoint();
               if (endpoint != null && endpoint instanceof BTagGroupInfo) {
                  BTagGroupInfo tgi = (BTagGroupInfo)endpoint;
                  String tagGroupId = tgi.getGroupId().getQName();
                  tgi.lease(2);
                  Iterator<TagInfo> tags = tgi.getTags();
                  ArrayList<Tag> groupTags = new ArrayList<>();

                  while (tags.hasNext()) {
                     TagInfo tagInfo = tags.next();
                     groupTags.add(tagInfo.makeTag());
                  }

                  if (groupTags.size() > 0) {
                     String slotName = relation.getPropertyInParent().getName();
                     BOrd bOrdComp = relativizeOrd(namesRoot, comp.getSlotPathOrd());
                     tagGroupInfo.add(new TmplUtil.TagGroupRelationInfo(bOrdComp, tagGroupId, slotName, groupTags));
                  }
               }
            }
         }
      }

      return tagGroupInfo;
   }

   public static void markComponentTags(BComponent comp) {
      for (BComponent c : (BComponent[])CompUtil.getDescendants(comp, BComponent.class)) {
         for (Tag tag : new ComponentTags(c)) {
            Property p = c.getProperty(SlotPath.escape(tag.getId().getQName()));
            c.setFlags(p, c.getFlags(p) | -2147483648);
            if (c.getType().is(BTemplateConfig.TYPE)) {
               int tagFlags = c.getFlags(p);
               if ((tagFlags & 16384) != 0) {
                  tagFlags &= -16385;
                  c.setFlags(p, tagFlags);
               }
            }
         }
      }
   }

   public static void setTagDictionaryServiceForTemplateComponentSpace(BComponent root) {
      BComponentSpace space = Objects.requireNonNull(root.getComponentSpace());
      BComponent templateRoot = Objects.requireNonNull(root.getParent().getParentComponent());
      BTagDictionaryService[] tagDictionaryServices = (BTagDictionaryService[])templateRoot.getChildren(BTagDictionaryService.class);
      if (tagDictionaryServices != null && tagDictionaryServices.length > 0) {
         space.setTagDictionaryService(tagDictionaryServices[0]);
      } else {
         BTagDictionaryService tagDictionaryService = (BTagDictionaryService)makeTagDictionaryService();
         if (tagDictionaryService != null) {
            Property mtdsProp = templateRoot.add("mockTagDictionaryService", tagDictionaryService.newCopy(), 0, null);
            tagDictionaryService = (BTagDictionaryService)templateRoot.get(mtdsProp);
            space.setTagDictionaryService(tagDictionaryService);
         }
      }
   }

   public static void convertTagsToTagGroupRelations(BComponent comp) {
      BTagDictionaryService tdService = (BTagDictionaryService)comp.getTagDictionaryService();
      BTagGroupInfo[] tagGroups = (BTagGroupInfo[])CompUtil.getDescendants(tdService, BTagGroupInfo.class);
      convertTagsToTagGroupRelationsForComponent(tagGroups, comp);

      for (BComponent c : (BComponent[])CompUtil.getDescendants(comp, BComponent.class)) {
         convertTagsToTagGroupRelationsForComponent(tagGroups, c);
      }
   }

   private static void convertTagsToTagGroupRelationsForComponent(BTagGroupInfo[] tagGroups, BComponent c) {
      for (Tag tag : new ComponentTags(c)) {
         Property p = c.getProperty(SlotPath.escape(tag.getId().getQName()));
         if (c.getSlotFacets(p).getb("tg__", false)) {
            for (BTagGroupInfo tagGroup : tagGroups) {
               if (tagGroup.getGroupId().equals(tag.getId())) {
                  BRelation addRelation = new BRelation(BNiagaraTagDictionary.TAG_GROUP_RELATION, tagGroup.getSlotPathOrd());
                  if (!hasRelation(addRelation, c)) {
                     c.remove(p);
                     c.add("r?", addRelation, 1);
                     break;
                  }
               }
            }
         }
      }
   }

   public static void convertTagGroupRelationsToTags(BComponent comp) {
      for (BRelation relation : (BRelation[])CompUtil.getDescendants(comp, BRelation.class)) {
         if (!(relation instanceof BLink) && relation.getId().equals(BNiagaraTagDictionary.TAG_GROUP_RELATION)) {
            Entity entity = relation.getEndpoint();
            BComponent parent = relation.getParent().asComponent();
            parent.remove(relation);
            if (entity instanceof BTagGroupInfo) {
               BTagGroupInfo tagGroupInfo = (BTagGroupInfo)entity;
               String name = SlotPath.escape(tagGroupInfo.getGroupId().getQName());
               CompUtil.setOrAdd(parent, name, BMarker.MARKER, 16384, BEditTagDialog.TAG_GROUP_FACETS, null);
            }
         }
      }
   }

   private static boolean hasRelation(BRelation relation, BComponent component) {
      boolean hasRelation = false;

      for (BRelation existingRelation : (BRelation[])component.getChildren(BRelation.class)) {
         if (existingRelation.getId().equals(relation.getId()) && existingRelation.getEndpointOrd().equals(relation.getEndpointOrd())) {
            hasRelation = true;
            break;
         }
      }

      return hasRelation;
   }

   private static String getBindHints(BComponent comp) {
      comp.lease();
      String bindHints = "";

      for (Tag tag : new ComponentTags(comp).getAll()) {
         if (tag.getValue().getType().is(BMarker.TYPE)) {
            String tagId = tag.getId().toString();
            bindHints = bindHints + (bindHints.isEmpty() ? tagId : " and " + tagId);
         }
      }

      for (Relation groupRelation : comp.relations().getAll(Id.newId("n:tagGroup"), 2)) {
         BObject bObject = null;
         BOrd tgOrd = groupRelation.getEndpointOrd();
         if (tgOrd != null && !tgOrd.isNull()) {
            bObject = tgOrd.resolve(comp).get();
         } else {
            bObject = (BObject)groupRelation.getEndpoint();
         }

         if (bObject != null && bObject instanceof BTagGroupInfo) {
            BTagGroupInfo tagGroup = (BTagGroupInfo)bObject;
            tagGroup.lease(3);
            Iterator<TagInfo> tags = tagGroup.getTags();

            while (tags.hasNext()) {
               Tag tagx = tags.next().makeTag();
               if (tagx.getValue() instanceof BMarker) {
                  bindHints = bindHints + (bindHints.isEmpty() ? tagx.getId().toString() : " and " + tagx.getId().toString());
               }
            }
         }
      }

      return bindHints;
   }

   private static void listComponents(ArrayList<BComponent> list, BComponent comp) {
      list.add(comp);
      SlotCursor<Property> cursor = comp.getProperties();

      while (cursor.nextComponent()) {
         listComponents(list, cursor.get().asComponent());
      }
   }

   private static void addPasswordConfig(BComponent root, ArrayList<TmplUtil.PasswordInfo> intPSWs) {
      if (!intPSWs.isEmpty()) {
         BTemplateConfig templateConfig = BTemplateConfig.createConfigForRoot(root);

         for (TmplUtil.PasswordInfo passwordInfo : intPSWs) {
            BComponent pswComp = passwordInfo.getParentOrd().resolve(root).get().asComponent();
            String[] names = pswComp.getSlotPath().getNames();
            BOrd pswCompOrd = pswComp.getHandleOrd();
            if (passwordInfo.isDynamic()) {
               pswCompOrd = pswComp.getSlotPathOrd();
            }

            BPasswordBinding binding = null;
            if (passwordInfo.getParentSlotName() != null) {
               binding = new BPasswordBinding(pswCompOrd, passwordInfo.getParentSlotName(), passwordInfo.getSlotName(), passwordInfo.isDynamic());
            } else {
               binding = new BPasswordBinding(pswCompOrd, passwordInfo.getSlotName(), passwordInfo.isDynamic());
            }

            templateConfig.add(null, binding, 5, BFacets.NULL, null);
         }
      }
   }

   private static void addTagGroupTags(BComponent root, ArrayList<TmplUtil.TagGroupRelationInfo> tagGroupRelationInfos) {
      for (TmplUtil.TagGroupRelationInfo tagGroupRelationInfo : tagGroupRelationInfos) {
         BComponent tagGroupComp = tagGroupRelationInfo.getSourceOrd().resolve(root).get().asComponent();
         Slot slot = tagGroupComp.getSlot(tagGroupRelationInfo.getRelationSlotName());
         if (slot != null) {
            tagGroupComp.remove(tagGroupRelationInfo.getRelationSlotName());
         }
      }

      BComponent templateRoot = root.getParent().getParentComponent();

      for (TmplUtil.TagGroupRelationInfo tagGroupRelationInfox : tagGroupRelationInfos) {
         BComponent tagGroupComp = tagGroupRelationInfox.getSourceOrd().resolve(root).get().asComponent();
         String tagGroupTagName = SlotPath.escape(tagGroupRelationInfox.getTagGroupId());
         BTagDictionaryService tdService = null;
         if (templateRoot != null) {
            try {
               tdService = (BTagDictionaryService)templateRoot.get("mockTagDictionaryService");
            } catch (Exception var18) {
            }

            if (tdService == null) {
               tdService = (BTagDictionaryService)makeTagDictionaryService();
               if (tdService != null) {
                  Property mtdsProp = templateRoot.add("mockTagDictionaryService", tdService.newCopy(), 0, null);
                  tdService = (BTagDictionaryService)templateRoot.get(mtdsProp);
               }
            }
         }

         if (tdService != null) {
            Id tgId = Id.newId(SlotPath.unescape(tagGroupTagName));
            Optional<TagDictionary> tdOptional = tdService.getTagDictionary(tgId.getDictionary());
            if (tdOptional.isPresent()) {
               BTagDictionary tagDictionary = (BTagDictionary)tdOptional.get();
               BTagGroupInfoList tagGroupInfos = tagDictionary.getTagGroupDefinitions();
               Optional<TagGroupInfo> tagGroupInfo = tagGroupInfos.getTagGroup(tgId);
               if (tagGroupInfo.isPresent()) {
                  BTagGroupInfo tgi = (BTagGroupInfo)tagGroupInfo.get();
                  BOrd slotPathOrd = tgi.getSlotPathOrd();
                  BRelation tgRelation = new BRelation(Id.newId("n:tagGroup"), slotPathOrd);
                  String name = SlotPath.escape(tgi.getGroupId().getQName());
                  Property var17 = tagGroupComp.add(name, tgRelation, 1, BEditTagDialog.TAG_GROUP_FACETS, null);
               }
            }
         }
      }
   }

   private static void addExternalLinks(BComponent root, ArrayList<TmplUtil.LinkInfo> extLinks) {
      for (TmplUtil.LinkInfo linkInfo : extLinks) {
         BComponent linkComp = linkInfo.getOrd().resolve(root).get().asComponent();
         addCompositeSlot(linkInfo, linkComp, root);
      }
   }

   private static void addCompositeSlot(TmplUtil.LinkInfo linkInfo, BComponent linkComp, BComponent root) {
      Slot childSlot = linkComp.getSlot(linkInfo.getSlotName());
      int childFlags = childSlot.getDefaultFlags();
      linkComp.setFlags(childSlot, childFlags);
      BValue value = null;
      BFacets facets = linkComp.getSlotFacets(childSlot);
      if (facets == null) {
         facets = BFacets.NULL;
      }

      if (!childSlot.isAction() && !childSlot.isTopic()) {
         value = linkComp.get((Property)childSlot).newCopy();
         int flags = linkInfo.isInput() ? 4104 : 4105;
         flags |= childFlags;
         flags &= -3;
         String addName = linkComp.getName() + '_' + linkInfo.getSlotName();
         Property prop = root.getProperty(addName);
         if (prop == null) {
            root.add(addName, value, flags, facets, null);
            if (!linkInfo.isInput()) {
               BLink link = new BLink(linkComp.getHandleOrd(), linkInfo.getSlotName(), addName, true);
               link.tags().set(BNiagaraTagDictionary.BIND_HINTS, BString.make(linkInfo.bindHints));
               root.add(null, link, 4096, BFacets.NULL, null);
               log.info("External link detected from " + addName);
            } else {
               BLink link = new BLink(root.getHandleOrd(), addName, linkInfo.getSlotName(), true);
               link.tags().set(BNiagaraTagDictionary.BIND_HINTS, BString.make(linkInfo.bindHints));
               linkComp.add(null, link, 4096, BFacets.NULL, null);
               link.activate();
               log.info("External link detected to " + addName);
            }
         } else {
            BLink[] links = root.getLinks(prop);
            if (links != null && links.length > 0) {
               checkAddBindHints(links[0], linkInfo.bindHints);
            }

            Knob[] knobs = root.getKnobs(prop);
            if (knobs != null && knobs.length > 0) {
               BLink link = knobs[0].getLink();
               checkAddBindHints(link, linkInfo.bindHints);
            }
         }
      } else {
         String msg = childSlot.isAction() ? "action" : "topic";
         log.info("External link to " + msg + " not supported.");
      }
   }

   private static void checkAddBindHints(BLink link, String bindHints) {
      Tags tags = link.tags();
      StringBuilder sb = new StringBuilder();
      Optional<BIDataValue> optBindHints = tags.get(BNiagaraTagDictionary.BIND_HINTS);
      boolean addBindHints = !optBindHints.isPresent();
      if (optBindHints.isPresent()) {
         String curBindHints = ((BString)optBindHints.get()).getString();
         addBindHints = true;
         tags.removeAll(BNiagaraTagDictionary.BIND_HINTS);
         if (!curBindHints.isEmpty() && !curBindHints.contains(bindHints)) {
            if (curBindHints.startsWith("(")) {
               sb.append(curBindHints).append(" or (").append(bindHints).append(')');
            } else {
               sb.append('(').append(curBindHints).append(") or (").append(bindHints).append(")");
            }

            bindHints = sb.toString();
         } else {
            bindHints = curBindHints;
         }
      }

      if (addBindHints) {
         tags.set(BNiagaraTagDictionary.BIND_HINTS, BString.make(bindHints));
      }
   }

   public static BOrd relativizeOrd(String[] baseNames, BOrd oldOrd) {
      if (oldOrd.isNull()) {
         return null;
      } else {
         OrdQuery[] q;
         try {
            q = oldOrd.parse();
         } catch (Throwable var9) {
            return null;
         }

         if (q.length == 0) {
            return null;
         } else {
            int n = 0;
            if (q[n].getScheme().equals("station")) {
               n++;
               if (q.length == 1) {
                  return null;
               }
            }

            if (!q[n].getScheme().equals("slot")) {
               return null;
            } else {
               SlotPath path = (SlotPath)q[n];
               String[] names = path.getNames();
               if (names.length != 0 && baseNames.length != 0) {
                  if (!names[0].equals(baseNames[0])) {
                     return null;
                  } else {
                     String rel = relative(baseNames, names);
                     Array<OrdQuery> queries = new Array(OrdQuery.class);
                     queries.add(new SlotPath("slot", rel));

                     for (int i = n + 1; i < q.length; i++) {
                        queries.add(q[i]);
                     }

                     return BOrd.make((OrdQuery[])queries.trim());
                  }
               } else {
                  return null;
               }
            }
         }
      }
   }

   private static String relative(String[] from, String[] to) {
      if (!from[0].equals(to[0])) {
         throw new IllegalStateException();
      } else {
         StringBuilder sb = new StringBuilder();
         int a = 1;

         while (a < to.length && a < from.length && from[a].equals(to[a])) {
            a++;
         }

         int b = from.length - a;

         for (int i = 0; i < b; i++) {
            if (sb.length() > 0) {
               sb.append("/");
            }

            sb.append("..");
         }

         for (int i = a; i < to.length; i++) {
            if (sb.length() > 0) {
               sb.append("/");
            }

            sb.append(to[i]);
         }

         return sb.toString();
      }
   }

   public static BOrd[] getPxViewOrds(BComponent root) {
      Array<BOrd> array = new Array(BOrd.class);
      appendPxViews(root, array);
      return (BOrd[])array.trim();
   }

   private static void appendPxViews(BComponent root, Array<BOrd> array) {
      BPxView[] pxViews = (BPxView[])root.getChildren(BPxView.class);

      for (int i = 0; i < pxViews.length; i++) {
         array.add((BOrd)pxViews[i].getPxFile().newCopy());
      }

      BComponent[] childComps = root.getChildComponents();

      for (int j = 0; j < childComps.length; j++) {
         appendPxViews(childComps[j], array);
      }
   }

   public static BIFile makeFile(BDirectory dir, String fileName) {
      FilePath filePath = dir.getFilePath();

      try {
         return BFileSystem.INSTANCE.makeFile(filePath.merge(fileName));
      } catch (Exception var4) {
         log.log(Level.WARNING, "Cannot create file:" + var4.getLocalizedMessage(), (Throwable)var4);
         return null;
      }
   }

   public static PxFileRef makeNewPx(String fname) {
      BMemoryFileStore fs = BMemoryFileSpace.INSTANCE.makeMemoryStore("px/" + fname);
      BPxFile newF = new BPxFile(fs);
      fs.setFile(newF);
      return new MemoryPxFileRef(newF, BOrd.make("file:^px/" + fname), newF.getAbsoluteOrd(), fname);
   }

   public static List<Map<String, BVersion>> checkModuleDependencies(
      BNtplFile file, HashMap<String, BVersion> nreModules, boolean checkSubtemplates, boolean checkPxFiles
   ) {
      ArrayList<BNtplFile> ntplFiles = new ArrayList<>();
      Map<String, BVersion> modulesMissing = new LinkedHashMap<>();
      Map<String, BVersion> pxModulesMissing = new LinkedHashMap<>();
      Map<String, BVersion> versionsMissing = new LinkedHashMap<>();
      ArrayList<String> pxModules = new ArrayList<>();
      ntplFiles.add(file);
      if (checkSubtemplates) {
         Object[] subs = file.getTemplateManifest().subtemplates.trim();

         for (Object obj : subs) {
            if (obj instanceof Subtemplate) {
               String ord = ((Subtemplate)obj).ntplFileOrd;
               BNtplFile local = (BNtplFile)BOrd.make(ord).get();
               if (!ntplFiles.contains(local)) {
                  ntplFiles.add(local);
               }
            }
         }
      }

      for (BNtplFile ntplFile : ntplFiles) {
         boolean closeNtplFile = !ntplFile.isOpen();

         BDependency[] bDeps;
         PxFileRef[] pxFileRefs;
         try {
            bDeps = ntplFile.getTemplateManifest().getDependencies();
            pxFileRefs = ntplFile.getPxFiles();
         } finally {
            if (closeNtplFile) {
               ntplFile.close();
            }
         }

         if (checkPxFiles) {
            for (PxFileRef pxFileRef : pxFileRefs) {
               try {
                  BPxFile pxFile = pxFileRef.getPxFile();
                  String[] modules = getModulesFromPxFile(pxFile);
                  pxModules.addAll(Arrays.asList(modules));
               } catch (Exception var22) {
                  log.log(Level.WARNING, "Error checking module dependencies:" + var22.getLocalizedMessage(), (Throwable)var22);
               }
            }
         }

         for (BDependency dependency : bDeps) {
            if (nreModules.get(dependency.getPartName()) == null) {
               modulesMissing.put(dependency.getPartName(), BVersion.ZERO);
            } else if (nreModules.get(dependency.getPartName()).compareTo(new BVersion(dependency.getVersion().getVendorVersion().toString())) < 0) {
               versionsMissing.put(dependency.getPartName(), dependency.getVersion());
            }
         }

         if (checkPxFiles) {
            for (String s : pxModules) {
               String wb = s + "-wb";
               String rt = s + "-rt";
               if (nreModules.get(wb) == null && nreModules.get(rt) == null && nreModules.get(s) == null) {
                  pxModulesMissing.put(s, BVersion.ZERO);
               }
            }
         }
      }

      ArrayList<Map<String, BVersion>> dependencyCheckResults = new ArrayList<>();
      dependencyCheckResults.add(modulesMissing);
      dependencyCheckResults.add(versionsMissing);
      dependencyCheckResults.add(pxModulesMissing);
      return dependencyCheckResults;
   }

   public static boolean stationHasAce(BStation station) {
      BISession session = station.getSession();
      if (session instanceof BFoxProxySession) {
         Optional<Boolean> useService = Optional.empty();

         try {
            useService = ((BFoxProxySession)session).rpc(BOrd.make("type:template:TemplateService"), "mustUseServiceToMakeTemplate", new Object[0]);
         } catch (Exception var4) {
         }

         if (useService.isPresent()) {
            return useService.get();
         }
      }

      station.lease(ACE_DEPTH);
      return CompUtil.hasDescendant(station, "ace:AceNetwork");
   }

   private static String[] getModulesFromPxFile(BPxFile pxFile) {
      String[] modules = new String[]{""};

      try {
         XParser parser = XParser.make(pxFile.getInputStream());
         XElem root = parser.parse();
         XElem elem = root.elem("import");
         if (elem == null) {
            throw new XException("Missing <import> element", root);
         }

         XElem[] moduleElems = elem.elems("module");
         modules = new String[moduleElems.length];

         for (int i = 0; i < moduleElems.length; i++) {
            modules[i] = moduleElems[i].get("name");
         }
      } catch (Exception var7) {
         log.warning(var7.getLocalizedMessage());
      }

      return modules;
   }

   public static class BindInfo {
      String info;
      BColor forground;

      public BindInfo(String info, BColor forground) {
         this.info = info;
         this.forground = forground;
      }

      public String getInfo() {
         return this.info;
      }

      public BColor getForground() {
         return this.forground;
      }
   }

   public static class LinkInfo {
      private BOrd ord;
      private String slotName;
      private boolean isInput;
      private String bindHints;

      public LinkInfo() {
      }

      public LinkInfo(BOrd ord, String slotName, boolean isInput, String bindHints) {
         this.ord = ord;
         this.slotName = slotName;
         this.isInput = isInput;
         this.bindHints = bindHints;
      }

      private static TmplUtil.LinkInfo make(BOrd ord, String slotName, boolean isInput, String bindHints) {
         return new TmplUtil.LinkInfo(ord, slotName, isInput, bindHints);
      }

      public void setOrd(BOrd ord) {
         this.ord = ord;
      }

      public void setSlotName(String slotName) {
         this.slotName = slotName;
      }

      public void setIsInput(boolean isInput) {
         this.isInput = isInput;
      }

      public BOrd getOrd() {
         return this.ord;
      }

      public String getSlotName() {
         return this.slotName;
      }

      public boolean isInput() {
         return this.isInput;
      }
   }

   private static class LocalPxSource implements EmbeddedPxSource {
      private final BComponent root;

      LocalPxSource(BComponent root) {
         this.root = root;
      }

      public BPxFile getPxFile(BOrd ord) {
         OrdTarget target = ord.resolve(this.root);
         return (BPxFile)target.get();
      }
   }

   public static class PasswordInfo {
      private BOrd parentOrd;
      private String slotName;
      private String parentSlotName;
      private boolean isDynamic;

      public PasswordInfo() {
      }

      public PasswordInfo(BOrd parentOrd, String slotName, boolean isDynamic) {
         this.parentOrd = parentOrd;
         this.slotName = slotName;
         this.parentSlotName = "";
         this.isDynamic = isDynamic;
      }

      public PasswordInfo(BOrd parentOrd, String parentSlotName, String slotName, boolean isDynamic) {
         this.parentOrd = parentOrd;
         this.slotName = slotName;
         this.parentSlotName = parentSlotName;
         this.isDynamic = isDynamic;
      }

      public void setParentOrd(BOrd parentOrd) {
         this.parentOrd = parentOrd;
      }

      public void setSlotName(String slotName) {
         this.slotName = slotName;
      }

      public void setIsDynamic(boolean isDynamic) {
         this.isDynamic = isDynamic;
      }

      public BOrd getParentOrd() {
         return this.parentOrd;
      }

      public String getSlotName() {
         return this.slotName;
      }

      public String getParentSlotName() {
         return this.parentSlotName;
      }

      public boolean isDynamic() {
         return this.isDynamic;
      }
   }

   private static class PasswordOrdProp {
      BOrd parent;
      Property property;
      Property parentProperty;
      boolean isDynamic;

      PasswordOrdProp(BOrd parent, Property property, boolean isDynamic) {
         this.parent = parent;
         this.property = property;
         this.parentProperty = null;
         this.isDynamic = isDynamic;
      }

      PasswordOrdProp(BOrd parent, Property parentProperty, Property property, boolean isDynamic) {
         this.parent = parent;
         this.property = property;
         this.parentProperty = parentProperty;
         this.isDynamic = isDynamic;
      }
   }

   public static class SubtemplateInfo {
      private String deployName;
      private String vendor;
      private String version;
      private BOrd deployOrd;
      private BOrd ntplFileOrd;

      public SubtemplateInfo() {
      }

      public SubtemplateInfo(String deployName, String vendor, String version, BOrd deployOrd, BOrd ntplFileOrd) {
         this.deployName = deployName;
         this.vendor = vendor;
         this.version = version;
         this.deployOrd = deployOrd;
         this.ntplFileOrd = ntplFileOrd;
      }

      public void setDeployName(String deployName) {
         this.deployName = deployName;
      }

      public void setVendor(String vendor) {
         this.vendor = vendor;
      }

      public void setVersion(String version) {
         this.version = version;
      }

      public void setDeployOrd(BOrd deployOrd) {
         this.deployOrd = deployOrd;
      }

      public void setNtplFileOrd(BOrd ntplFileOrd) {
         this.ntplFileOrd = ntplFileOrd;
      }

      public String getDeployName() {
         return this.deployName;
      }

      public String getVendor() {
         return this.vendor;
      }

      public String getVersion() {
         return this.version;
      }

      public BOrd getDeployOrd() {
         return this.deployOrd;
      }

      public BOrd getNtplFileOrd() {
         return this.ntplFileOrd;
      }
   }

   public static class TagGroupRelationInfo {
      private String tagGroupId = "";
      private BOrd sourceOrd = BOrd.DEFAULT;
      private String relationSlotName = "";
      private ArrayList<Tag> tagGroupTags = new ArrayList<>();

      public TagGroupRelationInfo() {
      }

      public TagGroupRelationInfo(BOrd sourceOrd, String tagGroupId, String relationSlotName, ArrayList<Tag> tagGroupTags) {
         this.sourceOrd = sourceOrd;
         this.tagGroupId = tagGroupId;
         this.relationSlotName = relationSlotName;
         this.tagGroupTags = tagGroupTags;
      }

      public void setSourceOrd(BOrd ord) {
         this.sourceOrd = ord;
      }

      public void setRelationSlotName(String relationSlotName) {
         this.relationSlotName = relationSlotName;
      }

      public void setTagGroupTags(ArrayList<Tag> tagGroupTags) {
         this.tagGroupTags = tagGroupTags;
      }

      public void setTagGroupId(String tagGroupId) {
         this.tagGroupId = tagGroupId;
      }

      public BOrd getSourceOrd() {
         return this.sourceOrd;
      }

      public String getRelationSlotName() {
         return this.relationSlotName;
      }

      public String getTagGroupId() {
         return this.tagGroupId;
      }

      public ArrayList<Tag> getTagGroupTags() {
         return this.tagGroupTags;
      }

      public String getTagGroupList() {
         StringBuilder sb = new StringBuilder();
         int i = 0;

         for (Tag tag : this.tagGroupTags) {
            if (i > 0) {
               sb.append(", ");
            }

            sb.append(tag.getId().getQName());
            i++;
         }

         return sb.toString();
      }
   }

   public static class TargetChoice implements Comparable<TmplUtil.TargetChoice> {
      public BComponent targetPoint;
      public BDynamicEnum targetSlotEnum;
      public boolean selected;

      public String getSelectString() {
         return SlotPath.unescape(this.targetPoint.getSlotPath().toString());
      }

      public int compareTo(TmplUtil.TargetChoice targetChoice) {
         return this.getSelectString().compareTo(targetChoice.getSelectString());
      }
   }
}
