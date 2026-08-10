package com.tridium.program.ui.module;

import com.tridium.program.BProgram;
import com.tridium.program.ProgramBase;
import com.tridium.program.module.BProgramModule;
import com.tridium.program.ui.BProgramEditor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.baja.gx.BImage;
import javax.baja.naming.SlotPath;
import javax.baja.sys.Action;
import javax.baja.sys.BDouble;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLong;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.ui.table.TableModel;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;

public abstract class ValidateModel extends TableModel {
   protected static final Lexicon lex = Lexicon.make(BuildHelper.class);
   public static final BImage okProgramIcon = BImage.make(new BProgram().getIcon());
   public static final BImage errProgramIcon = BImage.make(BIcon.make(new BProgram().getIcon(), BIcon.std("badges/error.png")));
   public static final BImage warnProgramIcon = BImage.make(BIcon.make(new BProgram().getIcon(), BIcon.std("badges/warning.png")));
   protected static final BImage errIcon = BImage.make(BIcon.std("stop.png"));
   protected static final BImage warnIcon = BImage.make(BIcon.std("warning.png"));
   protected List<String> errs = new ArrayList<>();
   protected List<String> warns = new ArrayList<>();

   public static ValidateModel make(BProgram program) {
      ValidateModel m = new ValidateModel.ValidateProgram(program);
      m.validate();
      return m;
   }

   public static ValidateModel make(BProgramModule module) {
      ValidateModel m = new ValidateModel.ValidateModule(module);
      m.validate();
      return m;
   }

   public BImage getProgramStatusIcon() {
      if (this.getErrCount() > 0) {
         return errProgramIcon;
      } else {
         return this.getWarnCount() > 0 ? warnProgramIcon : okProgramIcon;
      }
   }

   public final int getErrCount() {
      return this.errs.size();
   }

   public final int getWarnCount() {
      return this.warns.size();
   }

   protected abstract void validate();

   public void err(String msg) {
      if (!this.errs.contains(msg)) {
         this.errs.add(msg);
      }
   }

   public void warn(String msg) {
      if (!this.warns.contains(msg)) {
         this.warns.add(msg);
      }
   }

   public int getColumnCount() {
      return 1;
   }

   public int getRowCount() {
      return this.errs.size() + this.warns.size();
   }

   public Object getValueAt(int row, int col) {
      int numErrs = this.getErrCount();
      return row < numErrs ? this.errs.get(row) : this.warns.get(row - numErrs);
   }

   public BImage getRowIcon(int row) {
      return row < this.getErrCount() ? errIcon : warnIcon;
   }

   private static class ValidateModule extends ValidateModel {
      BProgramModule module;

      public ValidateModule(BProgramModule module) {
         this.module = module;
      }

      public String getColumnName(int col) {
         return lex.getText("vmodule.col");
      }

      @Override
      protected void validate() {
         this.checkName();
         this.checkVendor();
         this.checkVersion();
         if (this.module.listPrograms().length == 0) {
            this.warn(lex.getText("vmodule.warn.noPrograms"));
         }
      }

      private void checkName() {
         if (!SlotPath.isValidName(this.module.getModuleName())) {
            this.err(lex.getText("vmodule.err.name", new Object[]{this.module.getModuleName()}));
         }
      }

      private void checkVendor() {
         if (this.module.getVendor().length() == 0) {
            this.err(lex.getText("vmodule.err.vendor"));
         }
      }

      private void checkVersion() {
         try {
            Version ver = new Version(this.module.getVendorVersion());
            if (ver.toString().length() == 0) {
               this.err(lex.getText("vmodule.err.verRequired"));
            }
         } catch (Exception var2) {
            this.err(lex.getText("vmodule.err.version", new Object[]{this.module.getVendorVersion()}));
         }
      }
   }

   private static class ValidateProgram extends ValidateModel {
      private static final BProgramEditor pe = new BProgramEditor();
      BProgram program;

      public ValidateProgram(BProgram program) {
         this.program = program;
      }

      public String getColumnName(int col) {
         return lex.getText("vprogram.col");
      }

      @Override
      protected void validate() {
         this.checkCompileStatus();
         this.checkName();
         this.checkLongAndDoubleSlots();
         this.checkPropertySlots();
         this.checkActionDelegates();
         this.checkReflection();
      }

      @Override
      public void err(String msg) {
         super.err("[" + BuildHelper.toTypeName(this.program) + "] " + msg);
      }

      @Override
      public void warn(String msg) {
         super.warn("[" + BuildHelper.toTypeName(this.program) + "] " + msg);
      }

      private void checkCompileStatus() {
         pe.loadValue(this.program);
         int cksum = this.program.getCode().getChecksum();
         if (cksum == 0 || cksum != pe.computeChecksum(this.program.getCode().getSource())) {
            this.err(lex.getText("vprogram.err.outofdate"));
         }
      }

      private void checkName() {
         if (BuildHelper.toComponentName(this.program) == null) {
            this.err(lex.getText("vprogram.err.name"));
         }
      }

      private void checkLongAndDoubleSlots() {
         Property[] props = this.program.getPropertiesArray();

         for (Property prop : props) {
            BValue v = prop.getDefaultValue();
            if (v instanceof BDouble) {
               this.err(lex.getText("vprogram.err.double", new Object[]{prop.getName()}));
            } else if (v instanceof BLong) {
               this.err(lex.getText("vprogram.err.long", new Object[]{prop.getName()}));
            }
         }
      }

      private void checkPropertySlots() {
         Property[] props = this.program.getPropertiesArray();

         for (Property prop : props) {
            if (ReflectUtil.getInstance().getPropertyStatus(prop) != 0) {
               this.err(lex.getText("vprogram.err.getset", new Object[]{prop.getName()}));
            }
         }
      }

      private void checkActionDelegates() {
         Class<?> progClass = null;

         try {
            progClass = this.program.getCode().newInstance().getClass();
         } catch (Exception var13) {
            return;
         }

         Action[] actions = this.program.getActionsArray();

         for (Action action : actions) {
            ComponentWriter.CompAction ca = new ComponentWriter.CompAction(action);
            if (!"onExecute".equals(ca.getOn())) {
               String paramType = ca.paramType() == null ? "" : ca.paramType();
               String signature = ca.getOn() + "(" + paramType + ")";
               Object[] errParams = new Object[]{ca.name(), signature};

               try {
                  Method m = progClass.getDeclaredMethod(ca.getOn(), ca.paramTypeArray());
                  if ((m.getModifiers() & 1) == 0) {
                     this.err(lex.getText("vprogram.err.delegate.public", errParams));
                  }
               } catch (NoSuchMethodException var12) {
                  this.err(lex.getText("vprogram.err.delegate", errParams));
               }
            }
         }
      }

      private void checkReflection() {
         ProgramBase base = this.program.getCode().newProgramInstance();
         Method[] methods = base.getClass().getDeclaredMethods();
         ReflectUtil ref = ReflectUtil.getInstance();

         for (Method method : methods) {
            switch (ref.getOverrideStatus(method)) {
               case 1:
                  if (!"toString".equals(method.getName())) {
                     this.warn(lex.getText("vprogram.warn.override", new Object[]{this.toMethodDef(method)}));
                  }
                  break;
               case 2:
                  this.err(lex.getText("vprogram.err.override", new Object[]{this.toMethodDef(method)}));
            }
         }
      }

      private String toMethodDef(Method m) {
         StringBuilder sb = new StringBuilder().append(ReflectUtil.getInstance().getDeclaringClass(m).getName()).append(".").append(m.getName()).append("(");
         Class<?>[] params = m.getParameterTypes();

         for (int i = 0; i < params.length; i++) {
            if (i > 0) {
               sb.append(",");
            }

            sb.append(params[i].getName());
         }

         sb.append(")");
         return sb.toString();
      }
   }
}
