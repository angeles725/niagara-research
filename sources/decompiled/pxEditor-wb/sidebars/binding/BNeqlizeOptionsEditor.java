package com.tridium.px.editor.sidebars.binding;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import com.tridium.px.editor.BPxEditorOptions;
import com.tridium.tagdictionary.neqlize.BNeqlizeMode;
import java.util.Map;
import java.util.Optional;
import javax.baja.fox.BFoxProxySession;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BCheckBox;
import javax.baja.ui.BDialog;
import javax.baja.ui.BDropDown;
import javax.baja.ui.BLabel;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.ToggleCommand;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.list.BList;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BTextEditorPane;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraAction(
   name = "changeNeqlizeMode",
   flags = 20
)
public class BNeqlizeOptionsEditor extends BDialog {
   public static final Action changeNeqlizeMode = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BNeqlizeOptionsEditor.class);
   private static final int EDITOR_ROWS = 4;
   private static final int EDITOR_COLUMNS = 40;
   private static final int TAG_RELATION_COLUMNS = 3;
   private static final int CONVERSION_MODE_COLUMNS = 2;
   private static final Lexicon LEX = Lexicon.make("pxEditor");
   private static final BOrd NEQLIZE_RPC_ORD = BOrd.make("type:tagdictionary:NeqlizeRpc");
   private static final String SERVICE_TAGS_RELATIONS_RPC_METHOD = "getServiceExcludedTagsRelations";
   private final BPxEditorOptions editedOptions;
   private final BGridPane tagRelationsPane = new BGridPane(3);
   private final BGridPane conversionModePane = new BGridPane(2);
   private final BListDropDown neqlizeMode = new BListDropDown();
   private final BTextEditorPane serviceExcludedRelations = new BTextEditorPane(4, 40);
   private final BTextEditorPane serviceExcludedTags = new BTextEditorPane(4, 40);
   private final BTextEditorPane userExcludedRelations = new BTextEditorPane(4, 40);
   private final BTextEditorPane userExcludedTags = new BTextEditorPane(4, 40);

   public void changeNeqlizeMode() {
      this.invoke(changeNeqlizeMode, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   private BNeqlizeOptionsEditor(BPxEditorOptions options, BWidget owner) {
      this.editedOptions = options;
      this.makeConversionModeGridPane();
      this.makeTagsRelationsGridPane(owner);
      BGridPane mainPane = new BGridPane(1);
      mainPane.setRowGap(10.0);
      mainPane.add(null, this.conversionModePane);
      mainPane.add(null, this.tagRelationsPane);
      this.setContent(mainPane);
   }

   public void init(BFoxProxySession session) throws Exception {
      Optional<Map<String, Object>> rpcResult = this.getServiceTagsRelationsFromRPC(session);
      if (rpcResult.isPresent()) {
         JSONObject tagsRelations = new JSONObject(rpcResult.get());
         this.serviceExcludedRelations.setText(JSONUtil.getString(tagsRelations, "relations"));
         this.serviceExcludedTags.setText(JSONUtil.getString(tagsRelations, "tags"));
      }
   }

   protected Optional<Map<String, Object>> getServiceTagsRelationsFromRPC(BFoxProxySession session) throws Exception {
      return session.rpc(NEQLIZE_RPC_ORD, "getServiceExcludedTagsRelations", new Object[0]);
   }

   private void makeConversionModeGridPane() {
      this.conversionModePane.add(null, new BLabel(LEX.getText("neqlizeOptionsEditor.neqlizeMode.label")));
      BList modeList = this.neqlizeMode.getList();
      modeList.insertItem(0, LEX.getText("neqlizeOptionsEditor.traverseIfPossibleCommand.label"));
      modeList.insertItem(1, LEX.getText("neqlizeOptionsEditor.traverseOnlyCommand.label"));
      modeList.insertItem(2, LEX.getText("neqlizeOptionsEditor.selectOnlyCommand.label"));
      this.neqlizeMode.setSelectedIndex(this.editedOptions.getNeqlizeMode().getOrdinal());
      this.linkTo(this.neqlizeMode, BDropDown.valueModified, changeNeqlizeMode);
      this.conversionModePane.add(null, this.neqlizeMode);
      this.conversionModePane.setColumnGap(10.0);
      this.conversionModePane.setRowGap(10.0);
      this.conversionModePane.setHalign(BHalign.left);
   }

   private void makeTagsRelationsGridPane(BWidget owner) {
      this.tagRelationsPane.setColumnGap(10.0);
      this.tagRelationsPane.setRowGap(10.0);
      this.tagRelationsPane.add(null, new BLabel(""));
      this.tagRelationsPane.add(null, new BLabel(LEX.getText("neqlizeOptionsEditor.excludedRelations.label")));
      this.tagRelationsPane.add(null, new BLabel(LEX.getText("neqlizeOptionsEditor.excludedTags.label")));
      this.tagRelationsPane.add(null, new BLabel(LEX.getText("neqlizeOptionsEditor.service.label")));
      this.serviceExcludedRelations.getEditor().setEditable(false);
      this.tagRelationsPane.add(null, this.serviceExcludedRelations);
      this.serviceExcludedTags.getEditor().setEditable(false);
      this.tagRelationsPane.add(null, this.serviceExcludedTags);
      this.tagRelationsPane.add(null, new BLabel(""));
      BCheckBox useServiceRelationsBox = new BCheckBox(new BNeqlizeOptionsEditor.UseServiceRelationsCommand(owner));
      useServiceRelationsBox.setText(LEX.getText("neqlizeOptionsEditor.useServiceExcludedRelations.label"));
      useServiceRelationsBox.setSelected(this.editedOptions.getUseServiceExcludedRelations());
      this.tagRelationsPane.add(null, useServiceRelationsBox);
      BCheckBox useServiceTagsBox = new BCheckBox(new BNeqlizeOptionsEditor.UseServiceTagsCommand(owner));
      useServiceTagsBox.setText(LEX.getText("neqlizeOptionsEditor.useServiceExcludedTags.label"));
      useServiceTagsBox.setSelected(this.editedOptions.getUseServiceExcludedTags());
      this.tagRelationsPane.add(null, useServiceTagsBox);
      this.tagRelationsPane.add(null, new BLabel(LEX.getText("neqlizeOptionsEditor.user.label")));
      this.userExcludedRelations.setText(this.editedOptions.getNeqlizeExcludedRelations());
      this.tagRelationsPane.add(null, this.userExcludedRelations);
      this.userExcludedTags.setText(this.editedOptions.getNeqlizeExcludedTags());
      this.tagRelationsPane.add(null, this.userExcludedTags);
   }

   public void doChangeNeqlizeMode() {
      this.editedOptions.setNeqlizeMode(BNeqlizeMode.make(this.neqlizeMode.getSelectedIndex()));
   }

   public static void open(BWidget owner, BPxEditorOptions options, BFoxProxySession session) throws Exception {
      BPxEditorOptions editedOptions = (BPxEditorOptions)options.newCopy();
      BNeqlizeOptionsEditor editor = new BNeqlizeOptionsEditor(editedOptions, owner);
      editor.init(session);
      if (1 == BDialog.open(owner, LEX.getText("neqlizeOptionsEditor.title"), new BBorderPane(editor), 3)) {
         options.setNeqlizeMode(editedOptions.getNeqlizeMode());
         options.setUseServiceExcludedRelations(editedOptions.getUseServiceExcludedRelations());
         options.setUseServiceExcludedTags(editedOptions.getUseServiceExcludedTags());
         options.setNeqlizeExcludedRelations(editor.userExcludedRelations.getText());
         options.setNeqlizeExcludedTags(editor.userExcludedTags.getText());
         options.save();
      }
   }

   private class UseServiceRelationsCommand extends ToggleCommand {
      public UseServiceRelationsCommand(BWidget owner) {
         super(owner, BNeqlizeOptionsEditor.TYPE.getModule(), "neqlizeOptionsEditor.useServiceExcludedRelations");
      }

      public CommandArtifact doInvoke() {
         BNeqlizeOptionsEditor.this.editedOptions.setUseServiceExcludedRelations(this.isSelected());
         return null;
      }
   }

   private class UseServiceTagsCommand extends ToggleCommand {
      public UseServiceTagsCommand(BWidget owner) {
         super(owner, BNeqlizeOptionsEditor.TYPE.getModule(), "neqlizeOptionsEditor.useServiceExcludedTags");
      }

      public CommandArtifact doInvoke() {
         BNeqlizeOptionsEditor.this.editedOptions.setUseServiceExcludedTags(this.isSelected());
         return null;
      }
   }
}
