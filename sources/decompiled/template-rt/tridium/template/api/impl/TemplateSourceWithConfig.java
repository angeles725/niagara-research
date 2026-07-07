package com.tridium.template.api.impl;

import com.tridium.template.BConfigBinding;
import com.tridium.template.BTemplateConfig;
import java.util.stream.IntStream;
import javax.baja.naming.BOrd;
import javax.baja.naming.UnresolvedException;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;

public abstract class TemplateSourceWithConfig extends TemplateSourceWithBase {
   protected abstract BTemplateConfig getConfig();

   @Override
   public String getVersion() {
      return this.getConfig().getVersion().getVendorVersionString();
   }

   @Override
   public String getUid() {
      return this.getConfig().getUID().toString(null);
   }

   @Override
   public String getTitle() {
      return this.getConfig().getTemplateName();
   }

   @Override
   public IntStream getPropertyKeyStream() {
      return IntStream.range(0, this.getConfig().getConfigBindings().length).filter(this::propertyHasTarget);
   }

   @Override
   public String getPropertyPath(int propertyKey) {
      BConfigBinding binding = this.getPropertyBinding(propertyKey);
      if (binding == null) {
         return null;
      } else {
         BOrd ord = binding.getTargetOrd();
         String targetSlot = binding.getTargetSlot();

         try {
            BComponent targetParent = (BComponent)ord.resolve(this.getBase()).get();
            String basePath = this.getBase().getSlotPath().getBody();
            String path = targetParent.getSlotPath().getBody().substring(basePath.length()) + '/' + targetSlot;
            if (path.startsWith("/")) {
               path = path.substring(1);
            }

            return path;
         } catch (UnresolvedException var8) {
            return null;
         }
      }
   }

   @Override
   public String getPropertyName(int propertyKey) {
      BConfigBinding binding = this.getPropertyBinding(propertyKey);
      return binding == null ? null : binding.getSourceSlot();
   }

   @Override
   public String getPropertyUserTip(int propertyKey) {
      BConfigBinding binding = this.getPropertyBinding(propertyKey);
      return binding == null ? null : binding.getUserTip();
   }

   @Override
   protected BValue getPropertyValue(int propertyKey) {
      BConfigBinding binding = this.getPropertyBinding(propertyKey);
      return binding == null ? null : this.getConfig().get(binding.getSourceSlot());
   }

   private boolean propertyHasTarget(int propertyKey) {
      BConfigBinding binding = this.getPropertyBinding(propertyKey);
      if (binding == null) {
         return false;
      } else {
         BOrd targetOrd = binding.getTargetOrd();

         try {
            BComponent targetParent = (BComponent)targetOrd.resolve(this.getBase()).get();
            String targetSlotOrAttribute = binding.getTargetSlot();
            return !isSlotName(targetSlotOrAttribute) || targetParent.get(targetSlotOrAttribute) != null;
         } catch (UnresolvedException var6) {
            return false;
         }
      }
   }

   private BConfigBinding getPropertyBinding(int propertyKey) {
      BConfigBinding[] bindings = this.getConfig().getConfigBindings();
      return propertyKey >= 0 && propertyKey <= bindings.length ? bindings[propertyKey] : null;
   }

   private static boolean isSlotName(String slotOrAttributeName) {
      return !slotOrAttributeName.startsWith("#");
   }
}
