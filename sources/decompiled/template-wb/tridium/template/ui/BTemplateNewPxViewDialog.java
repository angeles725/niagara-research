package com.tridium.template.ui;

import com.tridium.px.editor.BPxEditorOptions;
import com.tridium.px.editor.BPxEditorPane;
import com.tridium.sys.transfer.CompToComp;
import com.tridium.template.file.PxFileRef;
import com.tridium.template.manifest.TemplateManifest;
import com.tridium.template.ui.file.TmplUtil;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.util.TypeInfoSpec;
import javax.baja.agent.BAbstractPxView;
import javax.baja.agent.BPxView;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.file.FilePath;
import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.space.BSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.ToggleCommandGroup;
import javax.baja.ui.commands.ReflectCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.file.BFileChooser;
import javax.baja.ui.options.BMruTextDropDown;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.px.BPxMedia;
import javax.baja.ui.text.BTextEditor;
import javax.baja.ui.wizard.BWizardHeader;
import javax.baja.util.Lexicon;
import javax.baja.workbench.fieldeditor.BWbFieldEditor;
import javax.baja.xml.XWriter;

@NiagaraType
@NiagaraAction(
   name = "viewNameChanged"
)
public class BTemplateNewPxViewDialog extends BEdgePane {
   public static final Action viewNameChanged = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BTemplateNewPxViewDialog.class);
   BTemplatePxEditor templatePxEditor;
   static Lexicon lex = Lexicon.make("pxEditor");
   static final BImage headerIcon = BPxEditorPane.icon("newPxView.iconHeader");
   static final BImage browseIcon = BPxEditorPane.icon("newPxView.iconBrowse");
   static final String lexBrowse = BPxEditorPane.text("newPxView.browse");
   static final String lexCancel = BPxEditorPane.text("newPxView.cancel");
   static final String lexDescription = BPxEditorPane.text("newPxView.description");
   static final String lexExporter = BPxEditorPane.text("newPxView.dynamic");
   static final String lexFileAlreadyExists = BPxEditorPane.text("newPxView.fileAlreadyExists");
   static final String lexMedia = BPxEditorPane.text("newPxView.media");
   static final String lexOverwrite = BPxEditorPane.text("newPxView.overwrite");
   static final String lexPxFile = BPxEditorPane.text("newPxView.pxFile");
   static final String lexSource = BPxEditorPane.text("newPxView.source");
   static final String lexReuse = BPxEditorPane.text("newPxView.reuse");
   static final String lexReuseOrOverwrite = BPxEditorPane.text("newPxView.reuseOrOverwrite");
   static final String lexSaveError = BPxEditorPane.text("newPxView.saveError");
   static final String lexTitle = BPxEditorPane.text("newPxView.title");
   static final String lexViewIcon = BPxEditorPane.text("newPxView.viewIcon");
   static final String lexViewName = BPxEditorPane.text("newPxView.viewName");
   static BFileChooser openChooser;
   static BFileChooser saveChooser;
   BComponent component;
   BWidget owner;
   BDialog existingDialog;
   boolean reuse = false;
   boolean cancelled = false;
   BMruTextDropDown name;
   BWbFieldEditor icon;
   BListDropDown media;
   boolean useFile;
   BMruTextDropDown file;
   BButton browse;
   BPxEditorOptions pxEditorOptions;

   public void viewNameChanged() {
      this.invoke(viewNameChanged, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static CommandArtifact invoke(BWidget owner, BTemplatePxEditor templatePxEditor, BComponent component) throws Exception {
      BTemplateNewPxViewDialog dialog = new BTemplateNewPxViewDialog(templatePxEditor, component);
      dialog.owner = owner;
      String pxViewName = null;

      do {
         int r = BDialog.open(owner, lexTitle, dialog, 3);
         if (r != 1) {
            return null;
         }

         pxViewName = dialog.save();
      } while (pxViewName == null);

      String view = "view:" + pxViewName;
      if (!dialog.reuse && dialog.useFile) {
         view = view + "/editor";
      }

      return null;
   }

   public BTemplateNewPxViewDialog(BTemplatePxEditor templatePxEditor, BComponent component) {
      this.component = component;
      this.name = this.buildName();
      this.icon = this.buildIcon();
      this.pxEditorOptions = BPxEditorOptions.make();
      this.media = this.buildMedia();
      this.templatePxEditor = templatePxEditor;
      ToggleCommand cmdPxFile = new ToggleCommand(this, lexPxFile) {
         public CommandArtifact doInvoke() throws Exception {
            if (this.isSelected()) {
               BTemplateNewPxViewDialog.this.useFile = true;
               BTemplateNewPxViewDialog.this.file.setEnabled(true);
            }

            return null;
         }
      };
      ToggleCommand cmdDynamic = new ToggleCommand(this, lexExporter) {
         public CommandArtifact doInvoke() throws Exception {
            if (this.isSelected()) {
               BTemplateNewPxViewDialog.this.useFile = false;
               BTemplateNewPxViewDialog.this.file.setEnabled(false);
            }

            return null;
         }
      };
      ToggleCommandGroup<ToggleCommand> grp = new ToggleCommandGroup();
      grp.add(cmdPxFile);
      grp.add(cmdDynamic);
      BGridPane source = new BGridPane(2);
      source.add(null, this.buildFile(), 1);
      this.useFile = true;
      this.file.setEnabled(false);
      BGridPane pairs = new BGridPane(2);
      pairs.setColumnGap(10.0);
      pairs.add(null, new BLabel(lexViewName));
      pairs.add(null, this.name);
      pairs.add(null, new BLabel(lexViewIcon));
      pairs.add(null, this.icon);
      pairs.add(null, new BLabel(lexMedia));
      pairs.add(null, this.media);
      pairs.add(null, new BLabel(lexSource));
      pairs.add(null, source, 1);
      this.setTop(new BWizardHeader(headerIcon, lexTitle, lexDescription));
      this.setCenter(new BBorderPane(pairs));
      this.linkTo(this.name.getEditor(), BTextEditor.textModified, viewNameChanged);
   }

   public BMruTextDropDown buildName() {
      BMruTextDropDown field = new BMruTextDropDown("newPxViewName", 20);
      String name = field.getText();
      if (name.length() == 0) {
         name = "Graphic";
      }

      name = CompToComp.getUniqueName(this.component, name);
      field.setText(name);
      return field;
   }

   public BWbFieldEditor buildIcon() {
      BIcon icon = new BPxView().getIcon();
      BWbFieldEditor field = BWbFieldEditor.makeFor(icon);
      field.loadValue(icon);
      return field;
   }

   public BListDropDown buildMedia() {
      BListDropDown field = new BListDropDown();
      TypeInfo[] types = Sys.getRegistry().getConcreteTypes(BPxMedia.TYPE.getTypeInfo());
      int def = 0;

      for (int i = 0; i < types.length; i++) {
         TypeInfoSpec m = new TypeInfoSpec(types[i]);
         if (m.info.getTypeName().equals(this.pxEditorOptions.getPxMediaTypeName())) {
            def = i;
         }

         field.getList().addItem(m.icon, m);
      }

      field.setSelectedIndex(def);
      return field;
   }

   public BWidget buildFile() {
      this.file = this.buildMru("pxViewFile");
      this.file.setEnabled(false);
      BGridPane a = new BGridPane(2);
      a.add(null, this.file, 1);
      return a;
   }

   public BMruTextDropDown buildMru(String mru) {
      BMruTextDropDown field = new BMruTextDropDown(mru, 60);
      String filename = this.name.getText() + ".px";
      if (filename.length() > 3 && FilePath.isValidName(filename)) {
         field.setText("file:^px/" + filename);
      }

      return field;
   }

   public String save() throws Exception {
      String slotName = SlotPath.escape(this.name.getTextAndSave());
      if (this.component.get(slotName) != null) {
         String text = lex.getText("newPxView.slotAlreadyExists", new Object[]{slotName});
         BDialog.error(this.owner, text);
         return null;
      } else {
         TypeInfoSpec m = (TypeInfoSpec)this.media.getSelectedItem();
         BAbstractPxView view = null;
         if (this.useFile) {
            view = new BPxView();
            BOrd ord = this.doSaveFile(m);
            ((BPxView)view).setPxFile(ord);
         }

         view.setIcon((BIcon)this.icon.saveValue());
         view.setMedia(m.info.getTypeSpec());
         String selectedMediaTypeName = m.info.getTypeName();
         if (!selectedMediaTypeName.equals(this.pxEditorOptions.getPxMediaTypeName())) {
            this.pxEditorOptions.setPxMediaTypeName(selectedMediaTypeName);
            this.pxEditorOptions.save();
         }

         this.component.add(slotName, view, 256);
         this.component.lease();
         return slotName;
      }
   }

   public BOrd doSaveFile(TypeInfoSpec m) throws Exception {
      String fileText = this.file.getText();
      String[] parsedText = TextUtil.splitAndTrim(fileText, '/');
      String fileName = parsedText[parsedText.length - 1];
      boolean fileExist = this.fileExist(fileName);
      if (fileExist) {
         this.existingDialog = this.makeExistingDialog();
         this.existingDialog.setBoundsCenteredOnOwner();
         this.existingDialog.open();
         if (this.cancelled) {
            return null;
         }
      } else {
         System.out.println("Create new file here......................");
         PxFileRef pxFileRef = TmplUtil.makeNewPx(fileName);
         this.writeNewFile(pxFileRef.getPxFile(), m.info);
         this.templatePxEditor.pxRefs.add(pxFileRef);
         this.templatePxEditor.updatePxSelCombo();
         this.templatePxEditor.selectPxSelCombo(pxFileRef);
         this.templatePxEditor.updateRightPane(pxFileRef);
         TemplateManifest manifest = this.templatePxEditor.view.getManifest();
         manifest.addResource(fileName, "px", fileText);
         System.out.println("Manifest = " + manifest);
      }

      this.file.getTextAndSave();
      return BOrd.make(fileText);
   }

   private boolean fileExist(String pxName) {
      for (PxFileRef pxRef : this.templatePxEditor.pxRefs) {
         if (pxRef.getPxName().equals(pxName)) {
            return true;
         }
      }

      return false;
   }

   BDialog makeExistingDialog() {
      BGridPane actions = new BGridPane(3);
      actions.setUniformColumnWidth(true);
      actions.setColumnAlign(BHalign.fill);
      actions.add(null, new BButton(new ReflectCommand(this, lexReuse, "reuse")));
      actions.add(null, new BButton(new ReflectCommand(this, lexCancel, "cancel")));
      BLabel desc = new BLabel(lexReuseOrOverwrite);
      desc.setHalign(BHalign.left);
      BGridPane text = new BGridPane(1);
      text.add(null, new BLabel(lexFileAlreadyExists, Theme.widget().getBoldText()));
      text.add(null, desc);
      BGridPane top = new BGridPane(2);
      top.setColumnGap(10.0);
      top.add(null, new BLabel(BImage.make("module://icons/x32/warning.png")));
      top.add(null, text);
      BGridPane content = new BGridPane(1);
      content.add(null, new BBorderPane(top, 0.0, 10.0, 10.0, 10.0));
      content.add(null, actions);
      this.reuse = false;
      this.cancelled = false;
      return new BDialog(this.owner, lexFileAlreadyExists, true, new BBorderPane(content));
   }

   void writeNewFile(BIFile newFile, TypeInfo media) throws Exception {
      try {
         BPxMedia pxMedia = (BPxMedia)media.getTypeSpec().getResolvedType().getInstance();
         BIFile source = (BIFile)pxMedia.getPxFileOrd().get();
         BajaFileUtil.pipe(source, newFile);
      } catch (UnresolvedException var5) {
         XWriter out = new XWriter(newFile.getOutputStream());
         out.prolog();
         out.w("<px version='1.0' media='").w(media).w("'>\n");
         out.w("<import>\n");
         out.w(" <module name='bajaui'/>\n");
         out.w("</import>\n");
         out.w("<content>\n");
         out.w("  <ScrollPane>\n");
         out.w("    <CanvasPane name=\"content\" viewSize=\"500,400\"/>\n");
         out.w("  </ScrollPane>\n");
         out.w("</content>\n");
         out.w("</px>");
         out.close();
      }
   }

   public void reuse() {
      this.existingDialog.close();
      this.reuse = true;
   }

   public void cancel() {
      this.existingDialog.close();
      this.cancelled = true;
   }

   public void browse() {
      BFileChooser chooser = BFileChooser.makeSave(this.owner);
      chooser.setBoundsCenteredOnScreen();
      chooser.setCreateFileOnSave(false);
      chooser.setConfirmOverwrite(false);

      try {
         BOrd ord = BOrd.make(this.file.getText());
         OrdQuery[] q = ord.parse();
         FilePath path = (FilePath)q[q.length - 1];
         String name = path.getName();
         q[q.length - 1] = path.getParent();
         BOrd dir = BOrd.make(q);
         chooser.setDefaultFileName(name);
         chooser.setCurrentDirectory(BOrd.make(this.component.getAbsoluteOrd(), dir));
         BObject obj = BOrd.make(this.component.getAbsoluteOrd(), "file:~").get(this.component);
         BSpace space = BOrd.toSpace(obj);
         if (space != null) {
            chooser.setSpaces(new BSpace[]{space});
         }
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      BOrd result = chooser.show();
      if (result != null) {
         result = result.relativizeToSession();
         this.file.setText(result.toString());
      }
   }

   public void doViewNameChanged() {
      String filename = this.name.getText() + ".px";
      if (filename.length() > 3 && FilePath.isValidName(filename)) {
         this.file.setText("file:^px/" + filename);
      }
   }
}
