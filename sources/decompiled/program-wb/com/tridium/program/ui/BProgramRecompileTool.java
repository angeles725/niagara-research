package com.tridium.program.ui;

import com.tridium.bql.BSelect;
import com.tridium.bql.SelectQuery;
import com.tridium.bql.expression.BPath;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.program.BProgram;
import com.tridium.program.ui.signing.BCertificateNotSelectedDialog;
import com.tridium.program.ui.signing.BCodeSigningOptions;
import com.tridium.util.ObjectUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.baja.collection.BITable;
import javax.baja.collection.ColumnList;
import javax.baja.collection.TableCursor;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.query.util.Columns;
import javax.baja.sys.BIObject;
import javax.baja.sys.BObject;
import javax.baja.sys.BStation;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BProgressDialog;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.BProgressDialog.Worker;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.table.BTable;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.util.BTitlePane;
import javax.baja.util.Lexicon;
import javax.baja.workbench.view.BWbView;

@NiagaraType(
   agent = {@AgentOn(
      types = {"program:ProgramService"}
   )}
)
public class BProgramRecompileTool extends BWbView {
   public static final Type TYPE = Sys.loadType(BProgramRecompileTool.class);
   List<BProgram> programs;
   BTable table;
   BTitlePane titlePane;
   protected static Lexicon lex = Lexicon.make("program");
   Logger log = Logger.getLogger("program");

   public Type getType() {
      return TYPE;
   }

   public BProgramRecompileTool() {
      this.table = new BTable(new BProgramRecompileTool.ProgramTableModel());
      BGridPane buttons = new BGridPane(5);
      buttons.add(null, new BButton(new BProgramRecompileTool.RecompileCommand()));
      BEdgePane pane = new BEdgePane();
      this.titlePane = BTitlePane.makePane(this.getTypeDisplayName(null), this.table);
      pane.setCenter(this.titlePane);
      pane.setBottom(new BBorderPane(buttons, 5.0, 0.0, 0.0, 0.0));
      this.setContent(pane);
   }

   protected void doLoadValue(BObject value, Context cx) throws Exception {
      this.programs = new ArrayList<>();
      BStation station = ObjectUtil.getStation(value.asComponent().getComponentSpace());
      BOrd base = value.asComponent().getComponentSpace().getAbsoluteOrd();
      BOrd ord = BOrd.make(base, BOrd.make("slot:/|bql:select * from program:Program"));
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
      BITable<?> table = (BITable<?>)ord.resolve(value).get();
      ColumnList cols = table.getColumns();
      TableCursor<? extends BIObject> c = table.cursor();
      Throwable var13 = null;

      try {
         while (c.next()) {
            BOrd temp = BOrd.make(base.toString() + "|slot:" + c.cell(cols.get(0)));
            temp = BOrd.make(this.getWbShell().getActiveOrd(), temp).normalize();
            BObject obj = temp.resolve().get();
            if (obj instanceof BProgram) {
               ((BProgram)obj).lease(1);
               this.programs.add((BProgram)obj);
            }
         }
      } catch (Throwable var23) {
         var13 = var23;
         throw var23;
      } finally {
         if (c != null) {
            if (var13 != null) {
               try {
                  c.close();
               } catch (Throwable var22) {
                  var13.addSuppressed(var22);
               }
            } else {
               c.close();
            }
         }
      }

      this.titlePane.setCount(this.programs.size());
   }

   class ProgramTableModel extends TableModel {
      private final String[] cols = new String[]{
         BProgramRecompileTool.lex.get("recompile.columns.path"),
         BProgramRecompileTool.lex.get("recompile.columns.status"),
         BProgramRecompileTool.lex.get("recompile.columns.fault"),
         BProgramRecompileTool.lex.get("recompile.columns.signed")
      };

      public int getRowCount() {
         return BProgramRecompileTool.this.programs.size();
      }

      public int getColumnCount() {
         return this.cols.length;
      }

      public String getColumnName(int col) {
         return this.cols[col];
      }

      public Object getValueAt(int row, int col) {
         BProgram program = BProgramRecompileTool.this.programs.get(row);
         switch (col) {
            case 0:
               return program.getSlotPath();
            case 1:
               return program.getCode().getStatus();
            case 2:
               return program.getCode().getFaultCause();
            case 3:
               return program.getCode().getSignature() != null && program.getCode().getSignature().length() > 0;
            default:
               return null;
         }
      }
   }

   class RecompileCommand extends Command {
      public RecompileCommand() {
         super(BProgramRecompileTool.this, BProgramRecompileTool.lex.getText("recompile.recompileAll"));
      }

      public CommandArtifact doInvoke() throws Exception {
         BCodeSigningOptions options = BCodeSigningOptions.make();
         String alias = options.getSigningCert();
         char[] keyPassword = null;
         if ((alias == null || alias.isEmpty())
            && AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("program.requireSigning")))) {
            CoreCryptoManager ccm = AccessController.doPrivileged((PrivilegedAction<CoreCryptoManager>)(() -> {
               try {
                  return CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
               } catch (Exception var1x) {
                  return null;
               }
            }));
            String newAlias = BCertificateNotSelectedDialog.show(this.getOwner());
            if (newAlias != null && !newAlias.isEmpty()) {
               options.setSigningCert(newAlias);
               alias = newAlias;
            }
         }

         if (alias != null && !alias.isEmpty()) {
            keyPassword = AccessController.doPrivileged((PrivilegedAction<char[]>)(() -> {
               try {
                  return options.getKeyPassword(this.getOwner());
               } catch (Exception var3x) {
                  return new char[0];
               }
            }));
         }

         if (alias != null && !alias.isEmpty()) {
            try {
               Compiler.checkSigningKey(alias, (char[])keyPassword.clone());
            } catch (LocalizableException var6) {
               BDialog.error(this.getOwner(), var6.getLocalizedMessage());
               return null;
            }
         } else {
            BProgramRecompileTool.this.log.warning(BProgramRecompileTool.lex.getText("program.willNotSign"));
         }

         String tsaUrl = options.getTsaUrl();
         RecompileTool recompiler = new RecompileTool(alias, tsaUrl, keyPassword);
         BProgressDialog.open(
            this.getOwner(),
            BProgramRecompileTool.lex.getText("recompile.title"),
            BProgramRecompileTool.this.new RecompileWorker(BProgramRecompileTool.this.programs, recompiler)
         );
         return null;
      }
   }

   class RecompileWorker extends Worker {
      boolean canceled = false;
      RecompileTool recompiler;
      List<BProgram> programs;
      Map<SlotPath, Exception> errors = new HashMap<>();

      public RecompileWorker(List<BProgram> programs, RecompileTool recompiler) {
         this.programs = programs;
         this.recompiler = recompiler;
      }

      public void doRun() throws Exception {
         for (int i = 0; i < this.programs.size() && !this.canceled; i++) {
            int progress = (i + 1) * 100 / this.programs.size();
            this.updateProgress(progress, BProgramRecompileTool.lex.getText("recompile.progress", new Object[]{i + 1, this.programs.size()}));

            try {
               this.recompiler.recompile(this.programs.get(i));
            } catch (Exception var4) {
               this.errors.put(this.programs.get(i).getSlotPath(), var4);
            }
         }

         if (!this.canceled) {
            if (this.errors.isEmpty()) {
               BDialog.info(
                  BProgramRecompileTool.this,
                  BProgramRecompileTool.lex.getText("recompile.complete.title"),
                  BProgramRecompileTool.lex.getText("recompile.complete.description")
               );
            } else {
               StringBuilder message = new StringBuilder();

               for (Entry<SlotPath, Exception> entry : this.errors.entrySet()) {
                  message.append(entry.getKey() + ": \n\n");
                  message.append(entry.getValue().getMessage());
                  message.append("\n\n");
               }

               BDialog.error(
                  BProgramRecompileTool.this,
                  BProgramRecompileTool.lex.getText("recompile.complete.title"),
                  BProgramRecompileTool.lex.getText("recompile.complete.errors.description"),
                  message.toString()
               );
            }
         }
      }

      public void doCancel() throws Exception {
         this.canceled = true;
      }
   }
}
