package com.tridium.template.api.impl;

import com.tridium.template.api.TemplateScope;
import com.tridium.util.CompUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.baja.file.BIDirectory;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.SlotPath;
import javax.baja.sys.BComponent;

public abstract class NewInternalTemplateSource extends NewTemplateSource {
   public NewInternalTemplateSource(BComponent sourceComponent, BIDirectory sourceHomeDir, BIDirectory sourceProtectedHomeDir) {
      super(sourceComponent, sourceHomeDir, sourceProtectedHomeDir);
   }

   @Override
   protected List<BComponent> getInstallRoots() {
      return Collections.singletonList(this.getBase());
   }

   @Override
   public TemplateScope getTemplateScope() {
      return TemplateScope.INTERNAL;
   }

   @Override
   protected void doPostCopyCleanup(BComponent source, BComponent copy) {
      log.finer("Resetting history extension enable properties in new template root...");
      BHistoryExt[] sourceHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(source, BHistoryExt.class);
      BHistoryExt[] copiedHistoryExtensions = (BHistoryExt[])CompUtil.getDescendants(copy, BHistoryExt.class);
      HashMap<String, Boolean> historyMap = new HashMap<>();

      for (BHistoryExt ext : sourceHistoryExtensions) {
         CompUtil.slotPathFromAncestor(source, ext).ifPresent(slotPathx -> {
            Boolean var10000 = historyMap.put(slotPathx.toString(), ext.getEnabled());
         });
      }

      if (source instanceof BHistoryExt && copy instanceof BHistoryExt) {
         ((BHistoryExt)copy).setEnabled(((BHistoryExt)source).getEnabled());
      }

      for (BHistoryExt ext : copiedHistoryExtensions) {
         Optional<SlotPath> slotPath = CompUtil.slotPathFromAncestor(copy, ext);
         Boolean enabled = historyMap.get(slotPath.isPresent() ? slotPath.get().toString() : "");
         if (enabled != null) {
            ext.setEnabled(enabled);
         }
      }
   }

   @Override
   protected String getDefaultStationDirectoryName() {
      return "templates";
   }
}
