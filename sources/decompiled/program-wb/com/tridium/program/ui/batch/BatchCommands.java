package com.tridium.program.ui.batch;

import com.tridium.bql.BSelect;
import com.tridium.bql.SelectQuery;
import com.tridium.bql.expression.BPath;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.program.batch.BAddSlotBatchRoutine;
import com.tridium.program.batch.BAddTagBatchRoutine;
import com.tridium.program.batch.BBatchRoutine;
import com.tridium.program.batch.BEditSlotBatchRoutine;
import com.tridium.program.batch.BRemoveSlotBatchRoutine;
import com.tridium.program.batch.BSlotFlagsBatchRoutine;
import com.tridium.workbench.bql.builder.BBqlQueryBuilder;
import com.tridium.workbench.util.BEditTagDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.baja.collection.BITable;
import javax.baja.collection.ColumnList;
import javax.baja.collection.TableCursor;
import javax.baja.gx.BInsets;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdQuery;
import javax.baja.query.util.Columns;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIObject;
import javax.baja.sys.BModule;
import javax.baja.sys.BObject;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.ui.BBorder;
import javax.baja.ui.BDialog;
import javax.baja.ui.BMenu;
import javax.baja.ui.BSeparator;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.commands.PasteCommand;
import javax.baja.ui.list.BCheckList;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BConstrainedPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.ui.text.BTextEditor;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;

public class BatchCommands {
   private static final Version MIN_REMOTE_BATCH_ROUTINE_VERSION = new Version("4.4.73.24.2");
   private static final Logger log = Logger.getLogger("program");
   private static BModule module = Sys.getModuleForClass(BBatchEditor.class);
   private static Lexicon lex = Lexicon.make("program");
   private BBatchEditor editor;
   private BBqlQueryBuilder builder;
   BatchCommands.FindObjects findObjects;
   BatchCommands.Clear clear;
   BatchCommands.ClearAll clearAll;
   BatchCommands.SelectColumns selectCols;
   BatchCommands.Rename rename;
   BatchCommands.SlotAdd slotAdd;
   BatchCommands.TagAdd tagAdd;
   BatchCommands.SlotEdit slotEdit;
   BatchCommands.SlotRename slotRename;
   BatchCommands.SlotRemove slotRemove;
   BatchCommands.SlotFlags slotFlags;
   BatchCommands.Hyperlink hyperlink;

   public BatchCommands(BBatchEditor editor) {
      this.editor = editor;
      this.findObjects = new BatchCommands.FindObjects();
      this.clear = new BatchCommands.Clear();
      this.clearAll = new BatchCommands.ClearAll();
      this.selectCols = new BatchCommands.SelectColumns();
      this.rename = new BatchCommands.Rename();
      this.slotAdd = new BatchCommands.SlotAdd();
      this.tagAdd = new BatchCommands.TagAdd();
      this.slotEdit = new BatchCommands.SlotEdit();
      this.slotRename = new BatchCommands.SlotRename();
      this.slotRemove = new BatchCommands.SlotRemove();
      this.slotFlags = new BatchCommands.SlotFlags();
      this.hyperlink = new BatchCommands.Hyperlink();
   }

   public void updateCommands() {
      boolean a = this.editor.table.getModel().getRowCount() > 0;
      boolean b = this.editor.table.getSelection().getRows().length > 0;
      boolean c = this.editor.table.getSelection().getRows().length == 1;
      this.findObjects.setEnabled(true);
      this.clear.setEnabled(a && b);
      this.clearAll.setEnabled(a);
      this.selectCols.setEnabled(a);
      this.rename.setEnabled(a);
      this.slotAdd.setEnabled(a);
      this.tagAdd.setEnabled(a);
      this.slotEdit.setEnabled(a);
      this.slotRename.setEnabled(a);
      this.slotRemove.setEnabled(a);
      this.slotFlags.setEnabled(a);
      this.hyperlink.setEnabled(a && c);
   }

   public BMenu buildMenu() {
      BMenu menu = new BMenu(lex.getText("batchEditor"));
      menu.add(null, this.findObjects);
      menu.add(null, this.clear);
      menu.add(null, this.clearAll);
      menu.add(null, this.selectCols);
      menu.add(null, new BSeparator());
      menu.add(null, new PasteCommand(this.editor.table));
      menu.add(null, new BSeparator());
      menu.add(null, this.rename);
      menu.add(null, this.slotAdd);
      menu.add(null, this.slotEdit);
      menu.add(null, this.slotRename);
      menu.add(null, this.slotRemove);
      menu.add(null, this.slotFlags);
      menu.add(null, new BSeparator());
      menu.add(null, this.hyperlink);
      return menu;
   }

   private void runBatchRoutine(BBatchRoutine routine) {
      this.editor.enterBusy();
      this.editor.disableCommands();
      BOrd[] ords = new BOrd[this.editor.table.model.kids.size()];

      for (int i = 0; i < ords.length; i++) {
         ords[i] = this.editor.table.model.kids.get(i).getHandleOrd();
      }

      routine.setTargets(BOrdList.make(ords));

      try {
         BFoxSession session = (BFoxSession)this.editor.getCurrentValueSession();
         Version remoteVersion = (Version)session.fw(404, "program", "rt", null, null);
         String results;
         if (remoteVersion != null && remoteVersion.compareTo(MIN_REMOTE_BATCH_ROUTINE_VERSION) > -1) {
            results = this.editor.service.runBatchRoutine(routine).getString();
         } else {
            log.fine(lex.getText("batchEditor.runClientSide", new Object[]{remoteVersion, MIN_REMOTE_BATCH_ROUTINE_VERSION}));
            Subscriber subscriber = Subscriber.make(event -> {});

            try {
               BComponent[] components = this.editor.table.model.kids.toArray(new BComponent[0]);
               subscriber.subscribe(components, 0, null);
               results = this.editor.service.doRunBatchRoutine(routine, null).getString();
            } finally {
               subscriber.unsubscribeAll();
            }
         }

         this.editor.exitBusy();
         this.editor.enableCommands();
         BTextEditor out = new BTextEditor(results, false);
         BTextEditorPane pane = new BTextEditorPane(out, 20, 100);
         BDialog.open(this.editor, "BatchEditor Results", pane, 1);
      } catch (Exception var12) {
         this.editor.exitBusy();
         this.editor.enableCommands();
         BDialog.error(this.editor, "Error", "Commit Failed", var12);
      }
   }

   static void lease(BComponent[] comps) {
      try {
         BComponent.lease(comps, 0, 60000L);
      } catch (Throwable var8) {
         System.out.println("Error leasing Components...");
         var8.printStackTrace();

         for (BComponent comp : comps) {
            try {
               comp.lease(0, 60000L);
            } catch (Throwable var7) {
            }
         }
      }
   }

   abstract class BatchCommand extends Command {
      public BatchCommand(String lex) {
         super(BatchCommands.this.editor, BatchCommands.module, "batchEditor.commands." + lex);
      }
   }

   class Clear extends BatchCommands.BatchCommand {
      public Clear() {
         super("clear");
      }

      public CommandArtifact doInvoke() {
         int rowCount = BatchCommands.this.editor.table.model.kids.size();
         int[] rows = BatchCommands.this.editor.table.getSelection().getRows();
         BatchCommands.this.editor.table.getSelection().deselectAll();

         for (int i = 0; i < rows.length; i++) {
            BatchCommands.this.editor.table.getSelection().deselectAll();
            BatchCommands.this.editor.table.model.kids.remove(rows[i] - i);
         }

         BatchCommands.this.updateCommands();
         BatchCommands.this.editor.table.sizeColumnsToFit();
         BatchCommands.this.editor.table.relayout();
         BatchCommands.this.editor.updateObjectCount(rowCount - rows.length);
         BatchCommands.this.editor.updateModel(BatchCommands.this.editor.table.model);
         return null;
      }
   }

   class ClearAll extends BatchCommands.BatchCommand {
      public ClearAll() {
         super("clearAll");
      }

      public CommandArtifact doInvoke() {
         while (!BatchCommands.this.editor.table.model.kids.isEmpty()) {
            BatchCommands.this.editor.table.model.kids.remove(0);
         }

         BatchCommands.this.updateCommands();
         BatchCommands.this.editor.table.sizeColumnsToFit();
         BatchCommands.this.editor.table.relayout();
         BatchCommands.this.editor.updateObjectCount(0);
         BatchCommands.this.editor.updateModel(BatchCommands.this.editor.table.model);
         return null;
      }
   }

   class FindObjects extends BatchCommands.BatchCommand {
      public FindObjects() {
         super("findObjects");
      }

      public CommandArtifact doInvoke() {
         if (BatchCommands.this.builder == null) {
            BatchCommands.this.builder = new BBqlQueryBuilder(BatchCommands.this.editor.service, BOrd.NULL, false);
         }

         BOrd ord = BatchCommands.this.builder.open(BatchCommands.this.editor);
         if (ord != null && !ord.isNull()) {
            OrdQuery[] query = ord.parse();
            SelectQuery q = (SelectQuery)query[query.length - 1];
            BSelect s = q.getSelect();
            BPath path = new BPath("toPathString");
            if (s.hasProjection() && s.getProjection().isDistinct()) {
               s.select(Columns.distinctProjection().add(Columns.make(path)));
            } else {
               s.select(Columns.projection(Columns.make(path)));
            }

            query[query.length - 1] = new SelectQuery(s);
            ord = BOrd.make(query);
            BITable<?> table = (BITable<?>)ord.resolve(BatchCommands.this.editor.service).get();
            ColumnList cols = table.getColumns();
            List<BComponent> toLease = new ArrayList<>();
            TableCursor<? extends BIObject> c = table.cursor();
            Throwable var10 = null;

            try {
               while (c.next()) {
                  BOrd temp = BOrd.make("station:|slot:" + c.cell(cols.get(0)));
                  temp = BOrd.make(BatchCommands.this.editor.getWbShell().getActiveOrd(), temp).normalize();
                  BObject obj = temp.resolve().get();
                  if (obj instanceof BComponent && !BatchCommands.this.editor.table.model.kids.contains(obj)) {
                     toLease.add((BComponent)obj);
                     BatchCommands.this.editor.table.model.kids.add((BComponent)obj);
                  }
               }
            } catch (Throwable var20) {
               var10 = var20;
               throw var20;
            } finally {
               if (c != null) {
                  if (var10 != null) {
                     try {
                        c.close();
                     } catch (Throwable var19) {
                        var10.addSuppressed(var19);
                     }
                  } else {
                     c.close();
                  }
               }
            }

            BatchCommands.lease(toLease.toArray(new BComponent[0]));
            BatchCommands.this.editor.table.sizeColumnsToFit();
            BatchCommands.this.editor.table.relayout();
            BatchCommands.this.editor.updateObjectCount(BatchCommands.this.editor.table.model.kids.size());
            BatchCommands.this.editor.updateModel(BatchCommands.this.editor.table.model);
            BatchCommands.this.updateCommands();
         }

         return null;
      }
   }

   class Hyperlink extends BatchCommands.BatchCommand {
      public Hyperlink() {
         super("hyperlink");
      }

      public CommandArtifact doInvoke() {
         int[] rows = BatchCommands.this.editor.table.getSelection().getRows();
         BComponent c = BatchCommands.this.editor.table.model.kids.get(rows[0]);
         BOrd ord = BOrd.make("station:|" + c.getSlotPath());
         BatchCommands.this.editor.getWbShell().hyperlink(ord);
         return null;
      }
   }

   class Rename extends BatchCommands.BatchCommand {
      public Rename() {
         super("rename");
      }

      public CommandArtifact doInvoke() {
         BBatchRoutine routine = BRenameDialog.open(BatchCommands.this.editor, false);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class SelectColumns extends BatchCommands.BatchCommand {
      public SelectColumns() {
         super("selectColumns");
      }

      public CommandArtifact doInvoke() {
         BBatchTable.Model model = BatchCommands.this.editor.table.model;
         BCheckList list = new BCheckList();
         String[] cols = model.getAllColumns();

         for (String col : cols) {
            list.getModel().addItem(col);
         }

         for (int i = 1; i < model.cols.size(); i++) {
            int index = list.indexOfItem(model.cols.get(i));
            if (index >= 0) {
               list.getSelection().select(index);
            }
         }

         BConstrainedPane pane = new BConstrainedPane(new BBorderPane(list, BBorder.inset, BInsets.make(0.0, 0.0, 0.0, 0.0)));
         pane.setMinWidth(300.0);
         pane.setMaxWidth(300.0);
         pane.setMinHeight(300.0);
         pane.setMaxHeight(400.0);
         if (1 == BDialog.open(BatchCommands.this.editor, BatchCommands.lex.getText("batchEditor.commands.selectColumns.label"), pane, 3)) {
            String path = BatchCommands.this.editor.table.model.cols.get(0);
            model.cols.clear();
            model.cols.add(path);
            int[] items = list.getSelection().getItems();

            for (int item : items) {
               model.cols.add((String)list.getItem(item));
            }

            BatchCommands.this.editor.table.sizeColumnsToFit();
            BatchCommands.this.editor.table.relayout();
         }

         return null;
      }
   }

   class SlotAdd extends BatchCommands.BatchCommand {
      public SlotAdd() {
         super("slotAdd");
      }

      public CommandArtifact doInvoke() {
         BAddSlotBatchRoutine routine = BAddDialog.open(BatchCommands.this.editor);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class SlotEdit extends BatchCommands.BatchCommand {
      public SlotEdit() {
         super("slotEdit");
      }

      public CommandArtifact doInvoke() {
         BEditSlotBatchRoutine routine = BSetDialog.open(BatchCommands.this.editor);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class SlotFlags extends BatchCommands.BatchCommand {
      public SlotFlags() {
         super("slotFlags");
      }

      public CommandArtifact doInvoke() {
         BSlotFlagsBatchRoutine routine = BSetFlagsDialog.open(BatchCommands.this.editor);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class SlotRemove extends BatchCommands.BatchCommand {
      public SlotRemove() {
         super("slotRemove");
      }

      public CommandArtifact doInvoke() {
         BRemoveSlotBatchRoutine routine = BRemoveDialog.open(BatchCommands.this.editor);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class SlotRename extends BatchCommands.BatchCommand {
      public SlotRename() {
         super("slotRename");
      }

      public CommandArtifact doInvoke() {
         BBatchRoutine routine = BRenameDialog.open(BatchCommands.this.editor, true);
         if (routine != null) {
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }

   class TagAdd extends BatchCommands.BatchCommand {
      public TagAdd() {
         super("tagAdd");
      }

      public CommandArtifact doInvoke() {
         BComponent tags = BEditTagDialog.open(BatchCommands.this.editor.getWbShell(), "AddTag", BatchCommands.this.editor.service, true);
         if (tags != null) {
            BAddTagBatchRoutine routine = BAddTagBatchRoutine.make(tags);
            BatchCommands.this.runBatchRoutine(routine);
         }

         return null;
      }
   }
}
