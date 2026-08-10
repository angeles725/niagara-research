package com.tridium.workbench.wizard;

import com.tridium.ui.wizard.step.BWizardButtonMode;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BButton;
import javax.baja.ui.BDialog;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BSeparator;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BOrientation;
import javax.baja.ui.pane.BBorderPane;
import javax.baja.ui.pane.BEdgePane;
import javax.baja.ui.pane.BGridPane;
import javax.baja.ui.pane.BPane;
import javax.baja.ui.util.UiLexicon;
import javax.baja.workbench.CannotSaveException;
import javax.baja.workbench.view.BWbComponentView;

@NiagaraType
public class BWizardView extends BWbComponentView {
   public static final Type TYPE = Sys.loadType(BWizardView.class);
   private BWizardView.WizardViewModel model;
   protected BEdgePane content = new BEdgePane();
   protected BPane wizardContentPane;
   protected BPane buttonPaneFooter;
   protected BButton back;
   protected BButton next;
   protected BButton finish;
   protected BButton cancel;
   private BWizardView.BackCommand backCommand = new BWizardView.BackCommand();
   private BWizardView.NextCommand nextCommand = new BWizardView.NextCommand();
   private BWizardView.FinishCommand finishCommand = new BWizardView.FinishCommand();
   private BWizardView.CancelCommand cancelCommand = new BWizardView.CancelCommand();
   private static final UiLexicon uiLex = UiLexicon.bajaui();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BWizardView() {
      this.content.setCenter(this.wizardContentPane = this.makeWizardContentPane());
      this.content.setBottom(this.buttonPaneFooter = this.makeButtonPaneFooter());
      this.setContent(this.content);
   }

   public void init(BWizardView.WizardViewModel model) {
      this.model = model;
      if (this.model == null) {
         throw new NullPointerException("Wizard View Model can't be null!");
      } else {
         this.model.view = this;
         this.model.init();
      }
   }

   @Override
   protected void doLoadValue(BObject value, Context cx) throws Exception {
      if (this.model == null) {
         this.init(this.makeViewModel());
      }
   }

   @Override
   protected BObject doSaveValue(BObject value, Context cx) throws CannotSaveException, Exception {
      return this.model.saveValue(value, cx);
   }

   @Override
   public void deactivated() {
      this.model.deactivated();
   }

   public void setWizardContent(BWidget widget) {
      this.wizardContentPane.set("content", widget);
      this.relayout();
   }

   public BWidget getWizardContent() {
      return (BWidget)this.wizardContentPane.get("content");
   }

   public void setWizardHeader(BWidget widget) {
      this.content.setTop(widget);
      this.relayout();
   }

   public BWidget getWizardHeader() {
      return this.content.getTop();
   }

   public BWidget getWizardFooter() {
      return this.content.getBottom();
   }

   public void setWizardFooter(BWidget widget) {
      this.content.setBottom(widget);
   }

   public final Command getBackCommand() {
      return this.backCommand;
   }

   public void setBackVisible(boolean visible) {
      this.back.setVisible(visible);
      this.relayout();
   }

   public boolean isBackVisible() {
      return this.back.isVisible();
   }

   public void setBackEnabled(boolean enabled) {
      this.back.setEnabled(enabled);
   }

   public void setBackAsDefault() {
      this.setDefaultButton(this.back);
      this.back.requestFocus();
   }

   public final Command getNextCommand() {
      return this.nextCommand;
   }

   public void setNextVisible(boolean visible) {
      this.next.setVisible(visible);
      this.relayout();
   }

   public boolean isNextVisible() {
      return this.next.isVisible();
   }

   public void setNextEnabled(boolean enabled) {
      this.next.setEnabled(enabled);
   }

   public void setNextAsDefault() {
      this.setDefaultButton(this.next);
      this.next.requestFocus();
   }

   public final Command getCancelCommand() {
      return this.cancelCommand;
   }

   public void setCancelVisible(boolean visible) {
      this.cancel.setVisible(visible);
      this.relayout();
   }

   public boolean isCancelVisible() {
      return this.cancel.isVisible();
   }

   public void setCancelEnabled(boolean enabled) {
      this.cancel.setEnabled(enabled);
   }

   public void setCancelAsDefault() {
      this.setDefaultButton(this.cancel);
   }

   public final Command getFinishCommand() {
      return this.finishCommand;
   }

   public void setFinishVisible(boolean visible) {
      this.finish.setVisible(visible);
      this.relayout();
   }

   public boolean isFinishVisible() {
      return this.finish.isVisible();
   }

   public void setFinishEnabled(boolean enabled) {
      this.finish.setEnabled(enabled);
   }

   public void setFinishAsDefault() {
      this.setDefaultButton(this.finish);
      this.finish.requestFocus();
   }

   public void convertCancelToClose() {
      this.cancel.setText(UiLexicon.bajaui().get("commands.close.label"));
      this.cancel.setImage(BImage.make(UiLexicon.bajaui().get("commands.close.icon")));
      this.relayout();
   }

   public void setDefaultButton(BButton button) {
      this.getShell().setDefaultButton(button);
   }

   public void handleError(Throwable throwable) {
      BDialog.error(this, BDialog.TITLE_ERROR, throwable.getMessage(), throwable);
   }

   public final BWizardView.WizardViewModel getModel() {
      return this.model;
   }

   protected BWizardView.WizardViewModel makeViewModel() {
      return this.model;
   }

   protected BButton makeBackButton() {
      return new BButton(this.backCommand);
   }

   protected BButton makeNextButton() {
      return new BButton(this.nextCommand);
   }

   protected BButton makeCancelButton() {
      return new BButton(this.cancelCommand);
   }

   protected BButton makeFinishButton() {
      return new BButton(this.finishCommand);
   }

   protected BPane makeButtonPane() {
      BGridPane b = new BGridPane(4);
      b.setColumnAlign(BHalign.fill);
      b.setHalign(BHalign.right);
      b.add("back", this.back = this.makeBackButton());
      b.add("next", this.next = this.makeNextButton());
      b.add("finish", this.finish = this.makeFinishButton());
      b.add("cancel", this.cancel = this.makeCancelButton());
      return b;
   }

   protected BPane makeButtonPaneFooter() {
      BEdgePane buttons = new BEdgePane();
      buttons.setTop(new BSeparator(BOrientation.horizontal));
      buttons.setCenter(new BBorderPane(this.makeButtonPane()));
      return buttons;
   }

   protected BPane makeWizardContentPane() {
      return new BBorderPane(new BNullWidget(), BInsets.DEFAULT);
   }

   private class BackCommand extends Command {
      private BackCommand() {
         super(BWizardView.this, BWizardView.uiLex.module, "wizard.back");
      }

      public CommandArtifact doInvoke() {
         BWizardView.this.model.back();
         return null;
      }
   }

   private class CancelCommand extends Command {
      private CancelCommand() {
         super(BWizardView.this, BWizardView.uiLex.module, "wizard.cancel");
      }

      public CommandArtifact doInvoke() {
         BWizardView.this.model.cancel();
         return null;
      }
   }

   private class FinishCommand extends Command {
      private FinishCommand() {
         super(BWizardView.this, BWizardView.uiLex.module, "wizard.finish");
      }

      public CommandArtifact doInvoke() {
         BWizardView.this.model.finish();
         return null;
      }
   }

   private class NextCommand extends Command {
      private NextCommand() {
         super(BWizardView.this, BWizardView.uiLex.module, "wizard.next");
      }

      public CommandArtifact doInvoke() {
         BWizardView.this.model.next();
         return null;
      }
   }

   public abstract static class WizardViewModel {
      private BWizardView view;

      public final BWizardView getView() {
         return this.view;
      }

      public abstract void init();

      public abstract BObject saveValue(BObject var1, Context var2) throws CannotSaveException, Exception;

      public abstract void deactivated();

      public abstract void back();

      public abstract void next();

      public abstract void finish();

      public abstract void cancel();

      public void update(BWizardButtonMode mode) {
         this.view.setBackEnabled(mode.canBack());
         this.view.setNextEnabled(mode.canNext());
         this.view.setFinishEnabled(mode.canFinish());
         this.view.setCancelEnabled(mode.canCancel());
      }
   }
}
