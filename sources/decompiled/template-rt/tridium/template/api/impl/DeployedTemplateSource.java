package com.tridium.template.api.impl;

import com.tridium.template.BTemplateConfig;
import com.tridium.template.api.TemplateSourceType;
import com.tridium.template.api.TemplateValueSource;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.Subscriber;

public abstract class DeployedTemplateSource extends TemplateSourceWithConfig {
   Subscriber subscriber = new Subscriber() {
      public void event(BComponentEvent event) {
      }
   };
   private BComponent root;
   private BTemplateConfig config;

   protected DeployedTemplateSource(BComponent root, BTemplateConfig config) {
      this.root = root;
      this.subscriber.subscribe(root);
      this.config = config;
      this.subscriber.subscribe(config);
   }

   @Override
   protected TemplateValueSource getValueSource() {
      return TemplateValueSource.LOCAL_VALUE;
   }

   @Override
   protected BTemplateConfig getConfig() {
      return this.config;
   }

   @Override
   protected BComponent getBase() {
      return this.root;
   }

   @Override
   public TemplateSourceType getSourceType() {
      return TemplateSourceType.DEPLOYED;
   }

   @Override
   public void close() {
      this.subscriber.unsubscribeAll();
      this.subscriber = null;
      this.config = null;
      this.root = null;
      super.close();
   }
}
