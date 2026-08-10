package com.tridium.program.ui;

import com.tridium.program.BProgram;
import com.tridium.program.BProgramCode;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BAction;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLink;
import javax.baja.sys.BLong;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BTopic;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.util.BWsAnnotation;

public class SourceWriter extends PrintWriter {
   public BProgramEditor editor;
   public BProgram program;
   public BProgramCode code;
   public Imports.Import[] imports;
   private Property[] accessProps;
   private int line;
   public int sourceLine;

   public static char[] generateToCharArray(BProgramEditor editor, String className) {
      CharArrayWriter out = new CharArrayWriter();
      new SourceWriter(out, editor).generate(className);
      return out.toCharArray();
   }

   public SourceWriter(Writer out, BProgramEditor editor) {
      super(out);
      this.init(editor);
   }

   public SourceWriter(OutputStream out, BProgramEditor editor) {
      super(out);
      this.init(editor);
   }

   private void init(BProgramEditor editor) {
      this.editor = editor;
      this.program = editor.program;
      this.code = editor.code;
      this.sourceLine = 0;
      this.imports = editor.imports.getAll();
      this.accessProps = this.getAccessProperties();
   }

   private Property[] getAccessProperties() {
      List<Property> list = new ArrayList<>();
      SlotCursor<Property> c = this.program.getProperties();

      while (c.next()) {
         Property prop = c.property();
         if (!prop.isFrozen()) {
            BObject v = c.get();
            if (!(v instanceof BWsAnnotation) && !(v instanceof BLink) && !(v instanceof BAction) && !(v instanceof BTopic)) {
               list.add(prop);
            }
         }
      }

      return list.toArray(new Property[0]);
   }

   public void generate(String className) {
      this.generateHeader(className);
      this.generateGetters();
      this.generateSetters();
      this.generateSource();
      this.generateFooter();
      this.editor.sourceLine = this.sourceLine;
      this.flush();
   }

   private void generateHeader(String className) {
      this.w("/* Auto-generated ProgramImpl Code */");
      this.w("");

      for (Imports.Import anImport : this.imports) {
         this.x("import ");
         this.x(TextUtil.pad(anImport.packageName + ".*;", 25));
         this.x(" /* ");
         this.x(anImport.modulePartName);
         this.x(" ");
         this.x(anImport.definitionToString());
         this.w("*/");
      }

      this.w("");
      this.w("public class " + className);
      this.w("  extends com.tridium.program.ProgramBase");
      this.w("{");
   }

   private void generateGetters() {
      if (this.accessProps.length != 0) {
         this.header("Getters");

         for (Property prop : this.accessProps) {
            String name = prop.getName();
            BObject v = this.program.get(prop);
            if (v.getType() == BBoolean.TYPE) {
               this.x("  public boolean get");
               this.x(capitalize(name));
               this.x("() { return getBoolean(\"");
               this.x(name);
               this.x("\"); }");
            } else if (v.getType() == BInteger.TYPE) {
               this.x("  public int get");
               this.x(capitalize(name));
               this.x("() { return getInt(\"");
               this.x(name);
               this.x("\"); }");
            } else if (v.getType() == BFloat.TYPE) {
               this.x("  public float get");
               this.x(capitalize(name));
               this.x("() { return getFloat(\"");
               this.x(name);
               this.x("\"); }");
            } else if (v.getType() == BString.TYPE) {
               this.x("  public String get");
               this.x(capitalize(name));
               this.x("() { return getString(\"");
               this.x(name);
               this.x("\"); }");
            } else if (v.getType() == BDouble.TYPE) {
               this.x("  public double get");
               this.x(capitalize(name));
               this.x("() { return getDouble(\"");
               this.x(name);
               this.x("\"); }");
            } else if (v.getType() == BLong.TYPE) {
               this.x("  public long get");
               this.x(capitalize(name));
               this.x("() { return getLong(\"");
               this.x(name);
               this.x("\"); }");
            } else {
               String cls = TextUtil.getClassName(v.getClass());
               this.x("  public ");
               this.x(cls);
               this.x(" get");
               this.x(capitalize(name));
               this.x("() { return (");
               this.x(cls);
               this.x(")get(\"");
               this.x(name);
               this.x("\"); }");
            }

            this.w("");
         }
      }
   }

   private void generateSetters() {
      if (this.accessProps.length != 0) {
         this.header("Setters");

         for (Property prop : this.accessProps) {
            String name = prop.getName();
            BObject v = this.program.get(prop);
            if (v.getType() == BBoolean.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(boolean v) { setBoolean(\"");
               this.x(name);
               this.x("\", v); }");
            } else if (v.getType() == BInteger.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(int v) { setInt(\"");
               this.x(name);
               this.x("\", v); }");
            } else if (v.getType() == BFloat.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(float v) { setFloat(\"");
               this.x(name);
               this.x("\", v); }");
            } else if (v.getType() == BString.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(String v) { setString(\"");
               this.x(name);
               this.x("\", v); }");
            } else if (v.getType() == BDouble.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(double v) { setDouble(\"");
               this.x(name);
               this.x("\", v); }");
            } else if (v.getType() == BLong.TYPE) {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(long v) { setLong(\"");
               this.x(name);
               this.x("\", v); }");
            } else {
               this.x("  public void set");
               this.x(capitalize(name));
               this.x("(");
               this.x(v.getClass().getName());
               this.x(" v) { set(\"");
               this.x(name);
               this.x("\", v); }");
            }

            this.w("");
         }
      }
   }

   private void generateSource() {
      this.header("Program Source");
      this.sourceLine = this.line;
      this.writeCode(this.code.getSource(), 2);
   }

   private void generateFooter() {
      this.w("}");
   }

   static String capitalize(String s) {
      return TextUtil.capitalize(s);
   }

   static String decapitalize(String s) {
      return TextUtil.decapitalize(s);
   }

   private void header(String s) {
      this.w("");
      this.w("////////////////////////////////////////////////////////////////");
      this.w("// " + s);
      this.w("////////////////////////////////////////////////////////////////");
      this.w("");
   }

   private void writeCode(String code, int indent) {
      try (BufferedReader r = new BufferedReader(new StringReader(code))) {
         String line;
         while ((line = r.readLine()) != null) {
            this.x(TextUtil.getSpaces(indent));
            this.w(line);
         }
      } catch (IOException var17) {
         throw new IllegalStateException();
      }
   }

   private void x(String s) {
      this.print(s);
   }

   private void w(String s) {
      this.print(s);
      this.print('\n');
      this.line++;
   }
}
