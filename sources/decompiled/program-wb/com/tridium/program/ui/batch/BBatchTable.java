package com.tridium.program.ui.batch;

import com.tridium.script.PropertyField;
import com.tridium.script.ScriptField;
import com.tridium.script.ScriptUtil;
import com.tridium.workbench.user.BPermissionsMapFE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.baja.gx.BImage;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BMenu;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableController;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.table.TableSelection;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferFormat;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;

@NiagaraType
public class BBatchTable extends BTable {
   public static final Type TYPE = Sys.loadType(BBatchTable.class);
   private Lexicon lex = Lexicon.make("program");
   BBatchEditor editor;
   BBatchTable.Model model;

   public Type getType() {
      return TYPE;
   }

   public BBatchTable(BBatchEditor editor) {
      this.editor = editor;
      this.setModel(this.model = new BBatchTable.Model());
      this.setSelection(new BBatchTable.Selection());
      this.setController(new BBatchTable.Controller());
      this.setPasteEnabled(true);
   }

   public void refresh() {
      BatchCommands.lease(this.model.kids.toArray(new BComponent[0]));
      this.relayout();
   }

   public void setModel(TableModel m) {
      super.setModel(m);
      if (m instanceof BBatchTable.Model) {
         this.model = (BBatchTable.Model)m;
      }
   }

   public int dragOver(TransferContext cx) {
      Mark mark = (Mark)cx.getEnvelope().getData(TransferFormat.mark);
      BObject[] obj = mark.getValues();

      for (BObject anObj : obj) {
         if (anObj instanceof BComponent) {
            return 16;
         }
      }

      return 0;
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      Mark mark = (Mark)cx.getEnvelope().getData(TransferFormat.mark);
      BObject[] obj = mark.getValues();
      List<BComponent> toLease = new ArrayList<>();

      for (BObject anObj : obj) {
         if (anObj instanceof BComponent && !this.model.kids.contains(anObj)) {
            toLease.add((BComponent)anObj);
            this.model.kids.add((BComponent)anObj);
         }
      }

      BatchCommands.lease(toLease.toArray(new BComponent[0]));
      this.sizeColumnsToFit();
      this.relayout();
      this.editor.commands.updateCommands();
      return null;
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      return this.drop(cx);
   }

   class Controller extends TableController {
      public void keyPressed(BKeyEvent event) {
         if (event.getKeyCode() == 127) {
            event.consume();
            BBatchTable.this.editor.commands.clear.invoke();
         } else {
            super.keyPressed(event);
         }
      }

      protected void cellPopup(BMouseEvent event, int row, int column) {
         BMenu menu = BBatchTable.this.editor.commands.buildMenu();
         menu.open(this.getTable(), event.getX(), event.getY());
      }

      protected void backgroundPopup(BMouseEvent event) {
         BMenu menu = BBatchTable.this.editor.commands.buildMenu();
         menu.open(this.getTable(), event.getX(), event.getY());
      }
   }

   class Model extends TableModel {
      List<String> cols = new ArrayList<>();
      List<BComponent> kids = new ArrayList<>();
      Map<BComponent, BImage> hash = new HashMap<>();

      private Model() {
         this.cols.add(BBatchTable.this.lex.getText("batchEditor.object"));
      }

      public int getColumnCount() {
         return this.cols.size();
      }

      public String getColumnName(int col) {
         return this.cols.get(col);
      }

      public int getRowCount() {
         return this.kids.size();
      }

      public Object getValueAt(int row, int col) {
         BComponent c = this.kids.get(row);
         if (col == 0) {
            return SlotPath.unescape(c.getSlotPath().getBody());
         } else {
            Object obj = c.get(this.cols.get(col));
            return obj == null ? "n/a" : String.valueOf(obj);
         }
      }

      public BImage getRowIcon(int row) {
         BImage icon = this.hash.get(this.kids.get(row));
         if (icon == null) {
            BComponent obj = this.kids.get(row);
            this.hash.put(obj, icon = BImage.make(obj.getIcon()));
         }

         return icon;
      }

      public String[] getAllColumns() {
         return this.getAllColumns(true);
      }

      public String[] getAllColumns(boolean includeFrozen) {
         List<String> list = new ArrayList<>();

         for (BComponent kid : this.kids) {
            ScriptField[] fields = ScriptUtil.scriptFields(kid);

            for (ScriptField field : fields) {
               if (field.isProperty()
                  && !list.contains(field.scriptName())
                  && (field.scriptFlags(kid) & 1) == 0
                  && (field.scriptType().getModifiers() & 1024) == 0) {
                  boolean frozen = field.isProperty() && ((PropertyField)field).getProperty().isFrozen();
                  if (includeFrozen || !frozen) {
                     list.add(field.scriptName());
                  }
               }
            }
         }

         String[] array = list.toArray(new String[0]);
         Arrays.sort((Object[])array);
         return array;
      }

      public Class<?> getColumnType(String name) {
         for (BComponent kid : this.kids) {
            ScriptField[] fields = ScriptUtil.scriptFields(kid);

            for (ScriptField field : fields) {
               if (name.equals(field.scriptName())) {
                  return field.scriptType();
               }
            }
         }

         return null;
      }

      public BWbFieldEditor getColumnEditor(String name) {
         for (BComponent kid : this.kids) {
            ScriptField[] fields = ScriptUtil.scriptFields(kid);

            for (ScriptField field : fields) {
               if (name.equals(field.scriptName())) {
                  Context cx = null;
                  BObject defaultValue = null;
                  if (kid != null) {
                     Slot slot = kid.getSlot(field.scriptName());
                     if (slot != null && slot.isProperty()) {
                        cx = slot.asProperty().getFacets();
                        defaultValue = slot.asProperty().getDefaultValue();
                     }
                  }

                  if (defaultValue == null) {
                     defaultValue = Sys.getType(field.scriptType()).getInstance();
                  }

                  BWbFieldEditor fe = BWbFieldEditor.makeFor(defaultValue, cx);
                  if (fe instanceof BPermissionsMapFE && (cx == null || cx.getFacets().isNull())) {
                     cx = BBatchTable.this.editor.getCurrentContext();
                  }

                  fe.loadValue(defaultValue, cx);
                  return fe;
               }
            }
         }

         return null;
      }

      public BBatchTable.Model makeCopy() {
         List<BComponent> toLease = new ArrayList<>();
         BBatchTable.Model m = BBatchTable.this.new Model();

         for (int i = 1; i < this.cols.size(); i++) {
            m.cols.add(this.cols.get(i));
         }

         for (int i = 0; i < this.kids.size(); i++) {
            BComponent c = this.kids.get(i);
            if (c.isMounted()) {
               toLease.add(c);
               m.kids.add(c);
            }
         }

         BatchCommands.lease(toLease.toArray(new BComponent[0]));
         return m;
      }
   }

   class Selection extends TableSelection {
      public void updateTable() {
         super.updateTable();
         BBatchTable.this.editor.commands.updateCommands();
      }
   }
}
