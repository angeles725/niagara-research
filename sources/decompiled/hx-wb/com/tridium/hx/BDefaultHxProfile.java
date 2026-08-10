package com.tridium.hx;

import javax.baja.agent.AgentInfo;
import javax.baja.hx.BHxProfile;
import javax.baja.hx.BHxView;
import javax.baja.hx.HxOp;
import javax.baja.io.HtmlWriter;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraSingleton
public class BDefaultHxProfile extends BHxProfile {
   public static final BDefaultHxProfile INSTANCE = new BDefaultHxProfile();
   public static final Type TYPE = Sys.loadType(BDefaultHxProfile.class);

   @Override
   public Type getType() {
      return TYPE;
   }

   protected BDefaultHxProfile() {
   }

   @Override
   public boolean hasView(BObject target, AgentInfo agentInfo) {
      String s = agentInfo.getAgentType().toString();
      return !s.startsWith("demoAppliance:");
   }

   @Override
   public void doBody(BHxView view, HxOp op) throws Exception {
      HtmlWriter out = op.getHtmlWriter();
      boolean fullScreen = this.isFullScreen(view, op);
      if (!fullScreen) {
         out.w("<div style='padding: 0px;'>");
         BHxPathBar.INSTANCE.write(this.makePathBarOp(op));
         out.w("</div>");
      }

      out.w("<div class='hx-outer-content");
      if (fullScreen) {
         out.w(" hx-outer-fullscreen");
      }

      out.w("'");
      boolean requiresStatic = this.requiresStatic(view, op);
      if (requiresStatic) {
         out.w(" style='position: static;'");
      }

      out.w(">");
      out.w("<div class='hx-inner-content'");
      if (requiresStatic) {
         out.w(" style='position: static;'");
      }

      out.w(">");
      view.write(op);
      out.w("</div>");
      out.w("</div>");
      this.displayError(op);
   }

   private boolean requiresStatic(BHxView view, HxOp op) {
      return !(view instanceof BHxWebWidget);
   }

   @Override
   public void updateDocument(BHxView view, HxOp op) throws Exception {
      BHxPathBar.INSTANCE.update(this.makePathBarOp(op));
      view.update(op);
   }

   @Override
   public boolean processDocument(BHxView view, HxOp op) throws Exception {
      return BHxPathBar.INSTANCE.process(this.makePathBarOp(op)) ? true : view.process(op);
   }

   @Override
   public void saveDocument(BHxView view, HxOp op) throws Exception {
      BHxPathBar.INSTANCE.save(this.makePathBarOp(op));
      view.save(op);
   }

   protected HxOp makePathBarOp(HxOp op) {
      return op.make("pathbar", op);
   }
}
