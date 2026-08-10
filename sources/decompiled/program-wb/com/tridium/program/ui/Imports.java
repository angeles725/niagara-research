package com.tridium.program.ui;

import com.tridium.program.BProgramCode;
import com.tridium.sys.Nre;
import com.tridium.sys.module.ModuleManager;
import com.tridium.sys.module.NModule;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.baja.gx.BImage;
import javax.baja.nre.util.SortUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.ui.BMenu;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.util.UiLexicon;

public class Imports extends TableModel {
   static final int PREDEFINED = 0;
   static final int USER_DEFINED = 1;
   static final int BY_PROPERTY = 2;
   static Imports.Import[] predefined = new Imports.Import[]{
      new Imports.Import("java", "java.util", 0),
      new Imports.Import("nre", "javax.baja.nre.util", 0),
      new Imports.Import("baja", "javax.baja.sys", 0),
      new Imports.Import("baja", "javax.baja.status", 0),
      new Imports.Import("baja", "javax.baja.util", 0),
      new Imports.Import("program-rt", "com.tridium.program", 0)
   };
   static UiLexicon lex = UiLexicon.bajaui();
   static String lexModule = lex.getText("programEditor.module");
   static String lexPackage = lex.getText("programEditor.package");
   static String lexDefinition = lex.getText("programEditor.definition");
   static BImage importIcon = BImage.make("module://icons/x16/module.png");
   final BProgramEditor editor;
   BTable table;
   private List<Imports.Import> list = new ArrayList<>();
   private Map<Imports.Import, Imports.Import> map = new HashMap<>();

   Imports(BProgramEditor editor) {
      this.editor = editor;
      this.table = new BTable(this);
      this.table.setController(new Imports.Controller());
   }

   public Imports.Import[] getAll() {
      return this.list.toArray(new Imports.Import[0]);
   }

   public String[] getDependencies() {
      Hashtable<String, String> depends = new Hashtable<>(13);
      depends.put("baja", "baja");
      depends.put("program-rt", "program-rt");

      for (Imports.Import x : this.list) {
         if (!x.modulePartName.equals("java")) {
            depends.put(x.modulePartName, x.modulePartName);
         }
      }

      String[] result = new String[depends.size()];
      Enumeration<String> e = depends.keys();

      for (int i = 0; e.hasMoreElements(); i++) {
         result[i] = e.nextElement();
      }

      return result;
   }

   public void updateImports() {
      List<Imports.Import> newList = new ArrayList<>();
      Map<Imports.Import, Imports.Import> newMap = new HashMap<>();

      for (Imports.Import aPredefined : predefined) {
         newList.add(aPredefined);
         newMap.put(aPredefined, aPredefined);
      }

      for (Imports.Import x : this.list) {
         if (x.definition == 1 && newMap.get(x) == null) {
            newList.add(x);
            newMap.put(x, x);
         }
      }

      ModuleManager moduleManager = AccessController.doPrivileged((PrivilegedAction<ModuleManager>)(() -> Nre.getModuleManager()));
      SlotCursor<Property> c = this.editor.program.getProperties();

      while (c.next()) {
         if (c.property().isDynamic()) {
            Class<?> cls = c.get().getClass();
            String pack = TextUtil.getPackageName(cls);
            NModule module = moduleManager.getModuleForClass(cls);
            Imports.Import xx = new Imports.Import(module.getModulePartName(), pack, 2);
            if (newMap.get(xx) == null) {
               newList.add(xx);
               newMap.put(xx, xx);
            }
         }
      }

      this.list = newList;
      this.map = newMap;
      this.updateTable();
   }

   void add(Imports.Import x) {
      Imports.Import dup = this.map.get(x);
      if (dup != null) {
         if (dup.definition > x.definition) {
            dup.definition = x.definition;
         }
      } else {
         this.list.add(x);
         this.map.put(x, x);
      }

      this.updateTable();
      this.editor.setModified();
   }

   void remove(Imports.Import x) {
      this.list.remove(x);
      this.map.remove(x);
      this.updateTable();
      this.editor.setModified();
   }

   void removeSelection() {
      int[] rows = this.table.getSelection().getRows();
      if (rows != null && rows.length != 0) {
         boolean changed = false;
         Imports.Import[] temp = this.getAll();

         for (int row : rows) {
            if (temp[row].definition == 1) {
               temp[row] = null;
               changed = true;
            }
         }

         if (changed) {
            this.list.clear();
            this.map.clear();

            for (Imports.Import aTemp : temp) {
               if (aTemp != null) {
                  this.list.add(aTemp);
                  this.map.put(aTemp, aTemp);
               }
            }

            this.updateTable();
            this.editor.setModified();
         }
      }
   }

   public void save(BProgramCode code) {
      StringBuilder udi = new StringBuilder();

      for (Imports.Import x : this.list) {
         if (x.definition == 1) {
            if (udi.length() > 0) {
               udi.append(';');
            }

            udi.append(x.modulePartName).append(':').append(x.packageName);
         }
      }

      code.setUserDefinedImports(udi.toString());
      StringBuilder d = new StringBuilder();
      String[] depends = this.getDependencies();

      for (String depend : depends) {
         if (d.length() > 0) {
            d.append(';');
         }

         d.append(depend);
      }

      code.setDependencies(d.toString());
   }

   public void load(BProgramCode code) {
      this.list.clear();
      this.map.clear();
      String s = code.getUserDefinedImports();
      StringTokenizer st = new StringTokenizer(s, ";");

      while (st.hasMoreTokens()) {
         String tok = st.nextToken();
         int colon = tok.indexOf(58);
         String m = tok.substring(0, colon);
         String p = tok.substring(colon + 1);
         this.add(new Imports.Import(m, p, 1));
      }

      this.updateImports();
   }

   public int getRowCount() {
      return this.list.size();
   }

   public int getColumnCount() {
      return 3;
   }

   public String getColumnName(int col) {
      switch (col) {
         case 0:
            return lexModule;
         case 1:
            return lexPackage;
         case 2:
            return lexDefinition;
         default:
            return "???";
      }
   }

   public Object getValueAt(int row, int col) {
      Imports.Import i = this.list.get(row);
      switch (col) {
         case 0:
            return i.modulePartName;
         case 1:
            return i.packageName;
         case 2:
            return i.definitionToString();
         default:
            return "???";
      }
   }

   public BImage getRowIcon(int row) {
      return importIcon;
   }

   public boolean isColumnSortable(int col) {
      return true;
   }

   public void sortByColumn(int col, boolean ascending) {
      Imports.Import[] values = this.list.toArray(new Imports.Import[0]);
      Object[] keys = new Object[values.length];

      for (int i = 0; i < keys.length; i++) {
         keys[i] = this.getValueAt(i, col).toString();
      }

      SortUtil.sort(keys, values, ascending);
      List<Imports.Import> temp = new ArrayList<>();
      Collections.addAll(temp, values);
      this.list = temp;
   }

   class Controller extends TableController {
      public void cellPopup(BMouseEvent event, int row, int column) {
         this.popup(event);
      }

      public void backgroundPopup(BMouseEvent event) {
         this.popup(event);
      }

      public void popup(BMouseEvent event) {
         BMenu menu = new BMenu();
         menu.add(null, Imports.this.editor.commands.importType);
         menu.add(null, Imports.this.editor.commands.importPackage);
         menu.add(null, Imports.this.editor.commands.removeImport);
         menu.open(Imports.this.table, event.getX(), event.getY());
      }
   }

   static class Import {
      public final String modulePartName;
      public final String packageName;
      public int definition;

      public Import(String m, String p, int d) {
         this.modulePartName = m;
         this.packageName = p;
         this.definition = d;
      }

      @Override
      public int hashCode() {
         return this.modulePartName.hashCode() ^ this.packageName.hashCode();
      }

      @Override
      public boolean equals(Object o) {
         if (!(o instanceof Imports.Import)) {
            return false;
         } else {
            Imports.Import x = (Imports.Import)o;
            return this.modulePartName.equals(x.modulePartName) && this.packageName.equals(x.packageName);
         }
      }

      public String definitionToString() {
         switch (this.definition) {
            case 0:
               return "Predefined";
            case 1:
               return "User Defined";
            case 2:
               return "By Property";
            default:
               return "???";
         }
      }
   }
}
