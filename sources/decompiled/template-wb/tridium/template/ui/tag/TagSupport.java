package com.tridium.template.ui.tag;

import com.tridium.file.types.bog.BBogFile;
import com.tridium.file.types.bog.BBogSpace;
import com.tridium.sys.tag.ComponentTags;
import com.tridium.tagdictionary.BNiagaraTagDictionary;
import com.tridium.template.TemplateConst;
import com.tridium.template.ui.BTemplateIOEditor;
import com.tridium.util.CompUtil;
import java.util.HashMap;
import javax.baja.file.BDirectory;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.naming.NullOrdException;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SyntaxException;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BMarker;
import javax.baja.sys.BModule;
import javax.baja.sys.Sys;
import javax.baja.tag.Tag;
import javax.baja.tag.TagInfo;
import javax.baja.tagdictionary.BTagDictionary;
import javax.baja.tagdictionary.BTagDictionaryService;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.list.BList;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.util.Lexicon;

public interface TagSupport {
   Lexicon lex = Lexicon.make("template");

   static void updateTagDictionaryTable(BTable tagTable, BTagDictionary dictn, boolean markersOnly, String filter) {
      BTemplateIOEditor.TagTableModel model = (BTemplateIOEditor.TagTableModel)tagTable.getModel();
      model.removeAll();
      if (dictn != null) {
         for (TagInfo tagInfo : dictn.getTagDefinitions()) {
            if (!markersOnly || tagInfo.getDefaultValue() instanceof BMarker) {
               Tag tag = new Tag(tagInfo.getTagId(), tagInfo.getDefaultValue());
               if (filter != null && filter.length() != 0) {
                  if (tagInfo.getTagId().getName().equalsIgnoreCase(filter)) {
                     model.add(tag);
                     break;
                  }

                  if (tagInfo.getTagId().getName().toLowerCase().startsWith(filter.toLowerCase())) {
                     model.add(tag);
                  }
               } else {
                  model.add(tag);
               }
            }
         }
      }
   }

   static BWidget makeTagDictionarySelPane(BListDropDown dictnCombo, BTextField tagEntry, Lexicon lex) {
      BBorderPane dictnPane = new BBorderPane(new BNullWidget(), BInsets.make(1.0, 0.0, 1.0, 1.0));
      BGridPane dictnSelPane = new BGridPane();
      dictnSelPane.setHalign(BHalign.left);
      dictnSelPane.add("label?", new BLabel(lex.getText("tag.dictionary.select")));
      dictnSelPane.add("tagDictnSelect", dictnCombo);
      dictnSelPane.setColumnCount(4);
      dictnSelPane.setRowGap(6.0);
      dictnSelPane.add(null, new BLabel(BImage.make("module://icons/x16/filter.png"), ""));
      dictnSelPane.add(null, tagEntry);
      dictnPane.setContent(dictnSelPane);
      return dictnPane;
   }

   static BListDropDown makeDictionarySelect(HashMap<String, Object> map) {
      BListDropDown dictnSel = new BListDropDown();
      BList list = dictnSel.getList();
      HashMap<String, Object> nsMap = new HashMap<>();
      TypeInfo[] types = Sys.getRegistry().getTypes(BTagDictionary.TYPE.getTypeInfo());

      for (int i = 0; i < types.length; i++) {
         BModule module = Sys.loadModule(types[i].getModuleName());
         BINavNode node = module.getNavChild("module.palette");
         BINavNode[] entries = node.getNavChildren();

         for (int e = 0; e < entries.length; e++) {
            BINavNode thisEntry = entries[e];
            if (thisEntry instanceof BTagDictionaryService) {
               thisEntry = thisEntry.getNavChild("Niagara");
               if (thisEntry instanceof BNiagaraTagDictionary) {
                  BNiagaraTagDictionary tagDictionary = (BNiagaraTagDictionary)thisEntry;
                  if (!tagDictionary.getTagDefinitions().iterator().hasNext()) {
                     tagDictionary.importFromBog(BNiagaraTagDictionary.importContext);
                     tagDictionary.loadSlots();
                  }
               }
            }

            if (thisEntry != null && thisEntry instanceof BTagDictionary) {
               BTagDictionary tagDictionary = (BTagDictionary)thisEntry;
               if (tagDictionary.getTagDefinitions().iterator().hasNext()) {
                  String nameKey = tagDictionary.getDisplayName(null);
                  String nameSpace = tagDictionary.getNamespace();
                  if (!nsMap.containsKey(nameSpace) && !map.containsKey(nameKey)) {
                     nsMap.put(nameSpace, thisEntry);
                     map.put(nameKey, thisEntry);
                     list.addItem(nameKey);
                  }
               }
            }
         }
      }

      BDirectory tdDirectory = getTagDictionaryDirectory();

      for (BIFile file : tdDirectory.listFiles()) {
         if ("bog".equals(file.getExtension())) {
            BBogFile bFile = (BBogFile)file;
            BBogSpace bogSpace = (BBogSpace)bFile.open();
            BComponent rootComponent = bogSpace.getRootComponent().getChildComponents()[0];
            if (rootComponent instanceof BTagDictionary) {
               BTagDictionary td = (BTagDictionary)rootComponent;
               String nameKey = td.getDisplayName(null);
               String nameSpace = td.getNamespace();
               if (!nsMap.containsKey(nameSpace) && !map.containsKey(nameKey)) {
                  nsMap.put(nameSpace, td);
                  map.put(nameKey, td);
                  list.addItem(nameKey);
               }
            }
         }
      }

      if (list.getItemCount() > 0) {
         list.setSelectedIndex(0);
      }

      return dictnSel;
   }

   static BDirectory getTagDictionaryDirectory() {
      try {
         return (BDirectory)getTagDictionaryDirectoryOrd().resolve().get();
      } catch (Exception var1) {
         return null;
      }
   }

   static BOrd getTagDictionaryDirectoryOrd() throws Exception {
      BOrd tdOrd = BOrd.make("local:|file:~tagDictionaries");

      try {
         tdOrd.resolve();
      } catch (UnresolvedException var3) {
         BDirectory fDir = BFileSystem.INSTANCE.getUserHome();
         fDir.getFileSpace().makeDir(fDir.getFilePath().merge("tagDictionaries"));
      }

      return tdOrd;
   }

   static String isNeqlPredicateValid(String predicate, String name) {
      if (predicate.isEmpty()) {
         return lex.getText("query.empty", new Object[]{name});
      } else {
         String neql = "neql:" + predicate;
         BOrd newOrdValue = BOrd.make(neql);

         try {
            OrdQuery[] queryList = newOrdValue.parse();

            for (OrdQuery query : queryList) {
               String scheme = query.getScheme();
               if (scheme != "neql") {
                  return lex.getText("query.invalidScheme", new Object[]{name, scheme});
               }
            }

            return "";
         } catch (NullOrdException var10) {
            return lex.getText("query.empty", new Object[]{name});
         } catch (SyntaxException var11) {
            return lex.getText("query.syntax.error", new Object[]{name, predicate, var11.getCause().getLocalizedMessage()});
         }
      }
   }

   static boolean hasTemplateAncestor(BComplex parent) {
      while (parent != null && parent.isComponent()) {
         if (parent.asComponent().tags().contains(TemplateConst.TEMPLATE_ROOT_TAG_ID)) {
            return true;
         }

         parent = parent.getParent();
      }

      return false;
   }

   static boolean isSubtemplate(BComplex parent) {
      int count = 0;

      while (parent != null && parent.isComponent()) {
         if (parent.asComponent().tags().contains(TemplateConst.TEMPLATE_ROOT_TAG_ID)) {
            if (++count > 1) {
               return true;
            }
         }

         parent = parent.getParent();
      }

      return false;
   }

   static void convertMultiTags(BComponent root) {
      convertComponentMultiTags(root);

      for (BComponent comp : (BComponent[])CompUtil.getDescendants(root, BComponent.class)) {
         convertComponentMultiTags(comp);
      }
   }

   static void convertComponentMultiTags(BComponent comp) {
      ComponentTags tags = new ComponentTags(comp);

      for (Tag tag : tags) {
         if (tags.isMulti(tag.getId())) {
            tags.removeAll(tag.getId());
            tags.set(tag);
         }
      }
   }
}
