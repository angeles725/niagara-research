package com.tridium.template;

import com.tridium.file.types.bog.BBogSpace;
import com.tridium.install.BVersion;
import com.tridium.tagdictionary.util.TagDictionaryUtil;
import com.tridium.template.manifest.TemplateManifest;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import javax.baja.agent.AgentList;
import javax.baja.data.BIDataValue;
import javax.baja.file.FilePath;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.space.BComponentSpace;
import javax.baja.space.BSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BComponentEventMask;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BLink;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BStation;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Relations;
import javax.baja.tag.Tags;
import javax.baja.units.BUnit;
import javax.baja.util.BFormat;
import javax.baja.util.BNameMap;
import javax.baja.util.BServiceContainer;
import javax.baja.util.BUuid;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "templateName",
      type = "String",
      defaultValue = "Template",
      flags = 1
   ), @NiagaraProperty(
      name = "uID",
      type = "BUuid",
      defaultValue = "BUuid.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "version",
      type = "BVersion",
      defaultValue = "new BVersion()",
      flags = 1
   ), @NiagaraProperty(
      name = "versionDate",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 1
   ), @NiagaraProperty(
      name = "deployed",
      type = "boolean",
      defaultValue = "false",
      flags = 7
   ), @NiagaraProperty(
      name = "propagated",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "pxEditBindings",
      type = "BVector",
      defaultValue = "new BVector()",
      flags = 5
   )})
@NiagaraAction(
   name = "lateSubscribe",
   flags = 4
)
public class BTemplateConfig extends BComponent {
   public static final Property templateName = newProperty(1, "Template", null);
   public static final Property uID = newProperty(1, BUuid.DEFAULT, null);
   public static final Property version = newProperty(1, new BVersion(), null);
   public static final Property versionDate = newProperty(1, BAbsTime.NULL, null);
   public static final Property deployed = newProperty(7, false, null);
   public static final Property propagated = newProperty(5, false, null);
   public static final Property pxEditBindings = newProperty(5, new BVector(), null);
   public static final Action lateSubscribe = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BTemplateConfig.class);
   private BTemplateService templateService;
   private static Lexicon lex = Lexicon.make("template");
   public static final String ATTRIBUTE_PREFIX = "#";
   public static final String NAME_ATTRIBUTE = "#Name";
   public static final String DISPLAY_NAME_ATTRIBUTE = "#DisplayName";
   private Ticket subscribedTicket;
   private static int COMPOSITE_READONLY = 4097;
   private BTemplateConfig.TargetSubscriber subscriber;
   private TemplateManifest manifest = null;
   private Array<Slot> inputSlots = null;
   private Array<Slot> outputSlots = null;
   private ArrayList<BRelationInfo> relations = null;
   private ArrayList<BPasswordBinding> passwords = null;
   private Array<Slot> unLinkedInputSlots = null;
   private Array<Slot> unLinkedOutputSlots = null;
   private ArrayList<BRelationInfo> unboundRelations = null;

   public String getTemplateName() {
      return this.getString(templateName);
   }

   public void setTemplateName(String v) {
      this.setString(templateName, v, null);
   }

   public BUuid getUID() {
      return (BUuid)this.get(uID);
   }

   public void setUID(BUuid v) {
      this.set(uID, v, null);
   }

   public BVersion getVersion() {
      return (BVersion)this.get(version);
   }

   public void setVersion(BVersion v) {
      this.set(version, v, null);
   }

   public BAbsTime getVersionDate() {
      return (BAbsTime)this.get(versionDate);
   }

   public void setVersionDate(BAbsTime v) {
      this.set(versionDate, v, null);
   }

   public boolean getDeployed() {
      return this.getBoolean(deployed);
   }

   public void setDeployed(boolean v) {
      this.setBoolean(deployed, v, null);
   }

   public boolean getPropagated() {
      return this.getBoolean(propagated);
   }

   public void setPropagated(boolean v) {
      this.setBoolean(propagated, v, null);
   }

   public BVector getPxEditBindings() {
      return (BVector)this.get(pxEditBindings);
   }

   public void setPxEditBindings(BVector v) {
      this.set(pxEditBindings, v, null);
   }

   public void lateSubscribe() {
      this.invoke(lateSubscribe, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Object getTemplateId() {
      return this.getUID().equals(BUuid.DEFAULT) ? this.getTemplateName() : this.getUID();
   }

   public void started() throws Exception {
      if (this.isRunning()) {
         try {
            BTemplateService service = this.getTemplateService();
            if (service != null) {
               service.register(this);
            }

            this.convertToBindings();
         } catch (ServiceNotFoundException var2) {
         }
      }
   }

   public void stopped() throws Exception {
      try {
         BTemplateService service = this.getTemplateService();
         if (service != null) {
            service.unregister(this);
         }
      } catch (ServiceNotFoundException var2) {
      }
   }

   public void atSteadyState() {
      this.propagateConfigurationIfNeeded();
      if (this.isRunning()) {
         BOrd ntplOrd = BOrd.NULL;

         try {
            if (this.getVersion().getVendor().isEmpty()) {
               BComponent root = this.getParent().asComponent();
               Optional<BIDataValue> optVendor = root.tags().get(TemplateConst.TEMPLATE_VENDOR_TAG_ID);
               Optional<BIDataValue> optVersion = root.tags().get(TemplateConst.TEMPLATE_VERSION_TAG_ID);
               if (optVendor.isPresent() && optVersion.isPresent()) {
                  this.setVersion(new BVersion(optVendor.get().toString(), optVersion.get().toString()));
               }

               TemplateManifest manifest = this.getManifest();
               if (!manifest.vendor.isEmpty()) {
                  FilePath ntplFilePath = new FilePath("^template/" + manifest.vendor).merge(this.getTemplateName() + ".ntpl");
                  ntplOrd = BOrd.make(ntplFilePath);
                  ntplOrd.resolve();
                  this.get(this.add("ntplFile", ntplOrd, 5));
               }
            }
         } catch (UnresolvedException var7) {
            BTemplateService.logger.warning(lex.getText("deploy.templateFileNotResolved", new Object[]{ntplOrd.toString()}));
         }
      }
   }

   private void propagateConfigurationIfNeeded() {
      if (!this.getPropagated()) {
         this.propagateConfiguration();
      }
   }

   public void propagateConfiguration() {
      this.setPropagated(true);
      BConfigBinding[] bindings = this.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         Property sourceProperty = this.getProperty(binding.getSourceSlot());
         String targetSlotOrAttribute = binding.getTargetSlot();

         try {
            BComplex bComplex = binding.getTargetOrd().resolve(this).get().asComplex();
            this.changeBoundPropertyOrAttribute(sourceProperty, bComplex, targetSlotOrAttribute, null);
         } catch (Exception var9) {
            if (BTemplateService.logger.isLoggable(Level.FINE)) {
               BTemplateService.logger.log(Level.FINE, "Cannot resolve Template configBinding: " + binding.getTargetOrd(), (Throwable)var9);
            } else {
               BTemplateService.logger.warning("Cannot resolve Template configBinding: " + binding.getTargetOrd());
            }
         }
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (cx == null || !cx.equals(Context.decoding)) {
         BSpace space = this.getSpace();
         if (space != null) {
            if (!(space instanceof BComponentSpace) || space instanceof BBogSpace || this.isRunning()) {
               if (p.equals(deployed) && this.isRunning()) {
                  BComplex parent = this.getParent();
                  if (parent instanceof BComponent) {
                     BComponent deployRoot = parent.asComponent();
                     TagDictionaryUtil.updateTagGroupRelations(deployRoot, BTemplateService.logger);
                  }
               } else {
                  BConfigBinding[] bindings = this.getConfigBindings();

                  for (BConfigBinding binding : bindings) {
                     if (binding.getSourceSlot().equals(p.getName())) {
                        String targetSlotOrAttribute = binding.getTargetSlot();
                        BComplex targetObject = binding.getTargetOrd().resolve(this).get().asComplex();
                        this.changeBoundPropertyOrAttribute(p, targetObject, targetSlotOrAttribute, cx);
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private void changeBoundPropertyOrAttribute(Property newValueProperty, BComplex targetObject, String slotOrAttribute, Context cx) {
      if (slotOrAttribute.startsWith("#")) {
         changeAttribute(slotOrAttribute, targetObject, this.get(newValueProperty), cx);
      } else {
         this.changeBoundProperty(newValueProperty, targetObject, slotOrAttribute);
      }
   }

   private void changeBoundProperty(Property newValueProperty, BComplex targetObject, String slotName) {
      BValue changedValue = this.get(newValueProperty);
      BValue newValue = null;
      if (changedValue instanceof BPassword) {
         newValue = this.extractPassword(newValueProperty, (BPassword)changedValue);
      } else if (changedValue instanceof BUsernameAndPassword) {
         newValue = this.extractUsernameAndPassword(newValueProperty, (BUsernameAndPassword)changedValue);
      } else if (!targetObject.get(slotName).equivalent(changedValue)) {
         newValue = changedValue.newCopy();
      }

      if (newValue != null) {
         targetObject.set(slotName, newValue);
      }
   }

   private BPassword extractPassword(Property passwordProperty, BPassword passwordValue) {
      BPassword extractedPassword = null;
      if (!passwordValue.isDefault()) {
         extractedPassword = BPassword.make(AccessController.doPrivileged(passwordValue::getValue));
         this.set(passwordProperty, BPassword.DEFAULT);
      }

      return extractedPassword;
   }

   private BUsernameAndPassword extractUsernameAndPassword(Property usernameAndPasswordProperty, BUsernameAndPassword usernameAndPasswordValue) {
      BUsernameAndPassword extracted = null;
      if (!usernameAndPasswordValue.getPassword().isDefault()) {
         String changedUsername = usernameAndPasswordValue.getUsername();
         BPassword changedPassword = usernameAndPasswordValue.getPassword();
         BPassword transferablePassword = BPassword.make(AccessController.doPrivileged(changedPassword::getValue));
         extracted = new BUsernameAndPassword(changedUsername, transferablePassword);
         this.set(usernameAndPasswordProperty, new BUsernameAndPassword(changedUsername, BPassword.DEFAULT));
      }

      return extracted;
   }

   private static void changeAttribute(String attributeId, BComplex targetObject, BValue newValue, Context cx) {
      switch (attributeId) {
         case "#Name":
            changeNameAttribute(targetObject, newValue.toString());
            break;
         case "#DisplayName":
            changeDisplayNameAttribute(targetObject, BFormat.make(newValue.toString()), cx);
      }
   }

   private static void changeNameAttribute(BComplex targetObject, String newName) {
      Property targetProperty = targetObject.getPropertyInParent();
      if (targetProperty != null && targetProperty.isDynamic()) {
         String newTargetSlotName = SlotPath.escape(newName);
         if (!targetProperty.getName().equals(newTargetSlotName)) {
            BComponent targetParent = targetObject.getParent().asComponent();
            targetParent.rename(targetProperty, newTargetSlotName);
         }
      }
   }

   private static void changeDisplayNameAttribute(BComplex targetObject, BFormat newDisplayNameFormat, Context cx) {
      Property targetProperty = targetObject.getPropertyInParent();
      if (targetProperty != null) {
         BComponent targetParent = targetObject.getParent().asComponent();
         if (!Objects.equals(targetParent.getDisplayNameFormat(targetProperty), newDisplayNameFormat)) {
            targetParent.setDisplayName(targetProperty, newDisplayNameFormat, cx);
         }
      }
   }

   public void added(Property property, Context context) {
      super.added(property, context);
      Optional<BConfigBinding> optional = this.getConfigBinding(property);
      if (optional.isPresent()) {
         BConfigBinding binding = optional.get();
         Optional<BComplex> complexOptional = binding.getTarget();
         if (complexOptional.isPresent()) {
            BComplex targetComplex = complexOptional.get();
            String targetSlotOrAttribute = binding.getTargetSlot();
            this.changeBoundPropertyOrAttribute(property, targetComplex, targetSlotOrAttribute, context);
         }
      }
   }

   public void updatePxEditBindings(ArrayList<SlotPath> pxEditTarget) {
      BVector v = new BVector();

      for (SlotPath slotPath : pxEditTarget) {
         v.add("pxe?", BString.make(slotPath.toString()), null);
      }

      this.setPxEditBindings(v);
   }

   private Optional<BConfigBinding> getConfigBinding(BComponent targetComp, String targetSlotOrAttribute) {
      BConfigBinding[] bindings = this.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         if (binding.getTargetOrd().equals(targetComp.getHandleOrd()) && binding.getTargetSlot().equals(targetSlotOrAttribute)) {
            return Optional.of(binding);
         }
      }

      return Optional.empty();
   }

   public Optional<BConfigBinding> getConfigBinding(Slot slot) {
      BConfigBinding[] bindings = this.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         if (binding.getSourceSlot().equals(slot.getName())) {
            return Optional.of(binding);
         }
      }

      return Optional.empty();
   }

   public void subscribed() {
      if (this.isRunning()) {
         if (this.subscribedTicket != null) {
            this.subscribedTicket.cancel();
         }

         this.subscribedTicket = Clock.schedule(this, BRelTime.make(500L), lateSubscribe, null);
      } else {
         this.doLateSubscribe();
      }
   }

   public void doLateSubscribe() {
      BConfigBinding[] bindings = this.getConfigBindings();

      for (BConfigBinding binding : bindings) {
         Optional<BComplex> possibleBindingTarget = binding.getTarget();
         if (possibleBindingTarget.isPresent()) {
            BComplex bindingTarget = possibleBindingTarget.get();
            String targetSlotOrAttribute = binding.getTargetSlot();
            boolean doSubscribe = true;
            boolean doSubscribeToParent = false;
            BValue targetValue = null;
            if (targetSlotOrAttribute.startsWith("#")) {
               switch (targetSlotOrAttribute) {
                  case "#Name":
                     targetValue = BString.make(bindingTarget.getName());
                     doSubscribe = false;
                     doSubscribeToParent = true;
                     break;
                  case "#DisplayName":
                     BFormat targetValueFormat = bindingTarget.getParent().asComponent().getDisplayNameFormat(bindingTarget.getPropertyInParent());
                     targetValue = targetValueFormat == null ? BString.DEFAULT : BString.make(targetValueFormat.toString(null));
                     doSubscribeToParent = true;
               }
            } else {
               targetValue = bindingTarget.get(targetSlotOrAttribute);
               if (targetValue instanceof BPassword) {
                  targetValue = BPassword.DEFAULT;
                  doSubscribe = false;
               } else if (targetValue instanceof BUsernameAndPassword) {
                  targetValue = new BUsernameAndPassword(((BUsernameAndPassword)targetValue).getUsername(), BPassword.DEFAULT);
               } else if (targetValue != null) {
                  targetValue = targetValue.newCopy();
               }
            }

            if (targetValue != null) {
               String sourceSlot = binding.getSourceSlot();
               BValue sourceValue = this.get(sourceSlot);
               if (sourceValue != null && !sourceValue.equivalent(targetValue)) {
                  this.set(sourceSlot, targetValue);
               }
            }

            if (doSubscribe && bindingTarget.isComponent()) {
               this.subscribeTo(bindingTarget.asComponent());
            }

            if (doSubscribeToParent) {
               BComplex bindingTargetParent = bindingTarget.getParent();
               if (bindingTargetParent != null && bindingTargetParent.isComponent()) {
                  this.subscribeTo(bindingTargetParent.asComponent());
               }
            }
         }
      }
   }

   private void subscribeTo(BComponent targetComponent) {
      if (targetComponent != null) {
         if (this.subscriber == null) {
            this.subscriber = new BTemplateConfig.TargetSubscriber(this);
         }

         if (!this.subscriber.isSubscribed(targetComponent)) {
            this.subscriber.subscribe(targetComponent);
         }
      }
   }

   public void unsubscribed() {
      if (this.subscriber != null) {
         BConfigBinding[] bindings = this.getConfigBindings();

         for (BConfigBinding binding : bindings) {
            Optional<BComplex> optionalTarget = binding.getTarget();
            if (optionalTarget.isPresent()) {
               BComplex target = optionalTarget.get();
               if (target.isComponent() && this.subscriber.isSubscribed(target.asComponent())) {
                  this.subscriber.unsubscribe(target.asComponent());
               }
            }
         }
      }
   }

   public BTemplateService getTemplateService() {
      if (this.templateService == null) {
         this.templateService = (BTemplateService)Sys.getService(BTemplateService.TYPE);
      }

      return this.templateService;
   }

   public TemplateManifest getManifest() {
      if (this.manifest == null) {
         this.manifest = this.generateManifest();
      }

      return this.manifest;
   }

   public boolean hasConfigProperties() {
      return this.getConfigBindings().length > 0;
   }

   public BPasswordBinding[] getPasswordBindings() {
      return (BPasswordBinding[])this.getChildren(BPasswordBinding.class);
   }

   public BConfigBinding[] getConfigBindings() {
      return (BConfigBinding[])this.getChildren(BConfigBinding.class);
   }

   public void checkForValidTemplate() throws Exception {
      for (Slot slot : this.getInputSlots()) {
         if (this.getInputSlotTags(slot) == null) {
            throw new Exception(lex.getText("inputCompositeLinkNotFound", new Object[]{slot.getName()}));
         }
      }

      for (Slot slotx : this.getOutputSlots()) {
         if (this.getOutputSlotTags(slotx) == null) {
            throw new Exception(lex.getText("outputCompositeLinkNotFound", new Object[]{slotx.getName()}));
         }
      }
   }

   public void clearCacheValues() {
      this.manifest = null;
      this.inputSlots = null;
      this.outputSlots = null;
      this.relations = null;
      this.unboundRelations = null;
      this.unLinkedInputSlots = null;
      this.unLinkedOutputSlots = null;
      this.passwords = null;
   }

   public Slot[] getUnlinkedInputs() {
      if (this.unLinkedInputSlots != null) {
         return (Slot[])this.unLinkedInputSlots.trim();
      } else {
         this.unLinkedInputSlots = new Array(Slot.class);
         BComponent parent = this.getParent().asComponent();

         for (Slot slot : this.getInputSlots()) {
            if (parent.getLinks(slot).length == 0) {
               this.unLinkedInputSlots.add(slot);
            }
         }

         return (Slot[])this.unLinkedInputSlots.trim();
      }
   }

   public Slot[] getInputSlots() {
      if (this.inputSlots != null) {
         return (Slot[])this.inputSlots.trim();
      } else {
         this.inputSlots = new Array(Slot.class);
         BComponent parent = this.getParent().asComponent();
         SlotCursor<Property> sc = parent.getProperties();

         while (sc.next()) {
            Property p = sc.property();
            if (isInputSlot(parent, p)) {
               this.inputSlots.add(p);
            }
         }

         return (Slot[])this.inputSlots.trim();
      }
   }

   public Slot[] getUnlinkedOutputs() {
      if (this.unLinkedOutputSlots != null) {
         return (Slot[])this.unLinkedOutputSlots.trim();
      } else {
         this.unLinkedOutputSlots = new Array(Slot.class);
         BComponent parent = this.getParent().asComponent();

         for (Slot slot : this.getOutputSlots()) {
            if (parent.getKnobs(slot).length == 0) {
               this.unLinkedOutputSlots.add(slot);
            }
         }

         return (Slot[])this.unLinkedOutputSlots.trim();
      }
   }

   public Slot[] getOutputSlots() {
      if (this.outputSlots != null) {
         return (Slot[])this.outputSlots.trim();
      } else {
         this.outputSlots = new Array(Slot.class);
         BComponent parent = this.getParent().asComponent();
         SlotCursor<Property> sc = parent.getProperties();

         while (sc.next()) {
            Property p = sc.property();
            if (isOutputSlot(parent, p)) {
               this.outputSlots.add(p);
            }
         }

         return (Slot[])this.outputSlots.trim();
      }
   }

   public Tags getInputSlotTags(Slot slot) {
      BComponent parent = this.getParent().asComponent();
      parent.lease();
      Knob[] knobs = parent.getKnobs(slot);
      if (knobs != null && knobs.length > 0) {
         BOrd targetOrd = knobs[0].getTargetOrd();
         OrdTarget ordTarget = targetOrd.resolve(this);
         BComponent target = ordTarget.get().asComponent();
         Slot targetSlot = target.getProperty(knobs[0].getTargetSlotName());
         BLink[] links = target.getLinks(targetSlot);
         return links[0].tags();
      } else {
         return null;
      }
   }

   public Tags getOutputSlotTags(Slot slot) {
      BComponent parent = this.getParent().asComponent();
      BLink[] links = parent.getLinks(slot);
      return links != null && links.length > 0 ? links[0].tags() : null;
   }

   public ArrayList<BPasswordBinding> getPasswordInfos() {
      if (this.passwords != null) {
         return this.passwords;
      } else {
         this.passwords = new ArrayList<>();

         for (BPasswordBinding password : (BPasswordBinding[])this.getChildren(BPasswordBinding.class)) {
            if (password.isPasswordDefault()) {
               this.passwords.add(password);
            }
         }

         return this.passwords;
      }
   }

   public ArrayList<BRelationInfo> getRelationInfos() {
      if (this.relations == null) {
         this.relations = new ArrayList<>();
         Collections.addAll(this.relations, this.getChildren(BRelationInfo.class));
      }

      return this.relations;
   }

   public ArrayList<BRelationInfo> getUnboundRelationInfos() {
      if (this.unboundRelations != null) {
         return this.unboundRelations;
      } else {
         this.unboundRelations = new ArrayList<>();
         BComponent parent = this.getParent().asComponent();
         parent.lease();
         Relations existingRelations = parent.relations();

         for (BRelationInfo relationInfo : this.getRelationInfos()) {
            Optional<Relation> optional = existingRelations.get(Id.newId(relationInfo.relationId), relationInfo.inbound ? 1 : 2);
            if (!optional.isPresent()) {
               this.unboundRelations
                  .add(
                     BRelationInfo.make(
                        relationInfo.inbound, relationInfo.relationId, relationInfo.relateHints, relationInfo.userTip, relationInfo.slotPathScope
                     )
                  );
            }
         }

         return this.unboundRelations;
      }
   }

   private TemplateManifest generateManifest() {
      BVersion versonPropValue = this.getVersion();
      boolean isVersionPropNull = versonPropValue.equivalent(BVersion.ZERO);
      String vendor = versonPropValue.getVendor();
      String version = isVersionPropNull ? "1.0" : versonPropValue.getVendorVersion().toString();
      TemplateManifest manifest = new TemplateManifest();
      if (vendor.isEmpty()) {
         manifest.vendor = "Tridium";
      } else {
         manifest.vendor = vendor;
      }

      if (!version.isEmpty()) {
         manifest.version = version;
      }

      manifest.uID = this.getUID();
      manifest.state = BTemplateState.DEFAULT;
      manifest.isApplication = this.getPropertyInParent().isFrozen();
      manifest.isStation = manifest.isApplication || this.getParent() instanceof BStation;
      SlotCursor<Property> sc = this.getProperties();
      sc.next();

      while (sc.next()) {
         manifest.settings.add(this.makeValue(this, sc.property(), sc.get(), "cfg"));
      }

      manifest.uID = this.getUID();
      if (this.getVersion() != null) {
         manifest.version = this.getVersion().getVendorVersionString();
      }

      BComponent parent = this.getParent().asComponent();
      sc = parent.getProperties();

      while (sc.next()) {
         Property p = sc.property();
         BValue v = sc.get();
         if ((parent.getFlags(p) & 32768) != 0) {
            manifest.links.add(this.makeValue(parent, p, v, "out"));
         } else if ((parent.getFlags(p) & 4096) != 0) {
            Knob[] knobs = parent.getKnobs(p);
            if (knobs.length > 0) {
               manifest.links.add(this.makeValue(parent, p, v, "in"));
            }
         }
      }

      return manifest;
   }

   private TemplateManifest.Value makeValue(BComplex parent, Property p, BValue v, String dir) {
      TemplateManifest.Value value = new TemplateManifest.Value();
      value.name = p.getName();
      value.required = true;
      value.direction = dir;
      BFacets f = parent.getSlotFacets(p);
      if (v instanceof BINumeric) {
         value.type = "num";
         if (f != null) {
            BUnit u = (BUnit)f.get("units");
            if (u != null) {
               value.hasUnit = true;
               value.unit = u.getUnitName();
            }
         }
      } else if (v instanceof BIBoolean) {
         value.type = "bool";
      } else {
         value.type = "str";
      }

      return value;
   }

   public AgentList getAgents(Context cx) {
      AgentList agents = super.getAgents(cx);
      agents.remove("template:TemplateMenuAgent");
      return agents;
   }

   private static boolean isInputSlot(BComponent object, Slot slot) {
      if (object.get(slot.asProperty()) instanceof BLink) {
         return false;
      } else {
         int flags = object.getFlags(slot);
         return (flags & COMPOSITE_READONLY) == 4096;
      }
   }

   private static boolean isOutputSlot(BComponent object, Slot slot) {
      if (object.get(slot.asProperty()) instanceof BLink) {
         return false;
      } else {
         int flags = object.getFlags(slot);
         return (flags & COMPOSITE_READONLY) == COMPOSITE_READONLY;
      }
   }

   public void convertToBindings() {
      Knob[] knobs = this.getKnobs();
      if (knobs.length > 0) {
         BComponent parent = this.getParent().asComponent();

         for (Knob knob : knobs) {
            BComponent targetComp = (BComponent)knob.getTargetOrd().resolve(this).get();
            String a = targetComp.getSlotPath().toString();
            String b = parent.getSlotPath().toString();
            if (a.startsWith(b)) {
               String source = knob.getSourceSlotName();
               String target = knob.getTargetSlotName();
               Slot targetSlot = knob.getTargetSlot();
               if (this.getProperty(source) == null) {
                  targetComp.setFlags(targetSlot, targetComp.getFlags(targetSlot) & -2);
                  targetComp.remove(knob.getLink().getName());
               } else {
                  BConfigBinding configBinding = new BConfigBinding(targetComp.getHandleOrd(), source, target);
                  this.add(null, configBinding, 4);
                  targetComp.setFlags(targetSlot, targetComp.getFlags(targetSlot) & -2 | -2147483648);
                  targetComp.remove(knob.getLink().getName());
                  targetComp.set(target, this.get(source).newCopy());
               }
            }
         }
      }
   }

   public static BTemplateConfig createConfigForRoot(BComponent root) {
      return createConfigForRoot(root, false);
   }

   public static BTemplateConfig createConfigForApplication(BStation root) {
      return createConfigForRoot(root, true);
   }

   public static BTemplateConfig createConfigForRoot(BComponent root, boolean isApplication) {
      BTemplateConfig config = getConfigForRoot(root, isApplication);
      if (config != null) {
         return null;
      } else {
         if (!isApplication) {
            config = new BTemplateConfig();
            root.add("templateConfig", config, 0, BFacets.make("ntplCreation", true), null);
         } else if (root instanceof BStation) {
            BStation stationRoot = (BStation)root;
            BApplicationService appService = new BApplicationService();
            stationRoot.getServices().add("ApplicationService", appService);
            config = appService.getConfiguration();
         }

         return config;
      }
   }

   public static BTemplateConfig getConfigForRoot(BComponent root) {
      return getConfigForRoot(root, true);
   }

   public static BTemplateConfig getConfigForRoot(BComponent root, boolean isApplication) {
      BTemplateConfig[] tcs = (BTemplateConfig[])root.getChildren(BTemplateConfig.class);
      if (tcs != null && tcs.length > 0) {
         return tcs[0];
      } else if (!(root instanceof BStation)) {
         return null;
      } else {
         return isApplication ? getConfigForApplication((BStation)root) : null;
      }
   }

   public static BTemplateConfig getConfigForApplication(BStation station) {
      station.lease();
      BServiceContainer services = station.getServices();
      services.lease();
      BApplicationService[] applicationServices = (BApplicationService[])services.getChildren(BApplicationService.class);
      if (applicationServices != null && applicationServices.length != 0 && applicationServices[0] != null) {
         BApplicationService applicationService = applicationServices[0];
         applicationService.lease();
         return applicationService.getConfiguration();
      } else {
         return null;
      }
   }

   public static BTemplateConfig getOrCreateConfigForRoot(BComponent root) {
      return getOrCreateConfigForRoot(root, false);
   }

   public static BTemplateConfig getOrCreateConfigForApplication(BStation station) {
      return getOrCreateConfigForRoot(station, true);
   }

   public static BTemplateConfig getOrCreateConfigForRoot(BComponent root, boolean isApplication) {
      BTemplateConfig config = getConfigForRoot(root, isApplication);
      if (config != null) {
         Property configProp = config.getPropertyInParent();
         if (configProp.isDynamic() && isApplication || configProp.isFrozen() && !isApplication) {
            config = null;
         }
      } else {
         config = createConfigForRoot(root, isApplication);
      }

      return config;
   }

   public static void removeConfigFromRoot(BComponent root) {
      BTemplateConfig config = getConfigForRoot(root);
      if (config != null) {
         if (config.getPropertyInParent().isDynamic()) {
            root.remove(config);
         } else if (root instanceof BStation) {
            ((BStation)root).getServices().remove(config.getParent());
         }
      }
   }

   public static BComponent getRootForConfig(BTemplateConfig config) {
      if (config == null) {
         return null;
      } else {
         BComplex parent = config.getParent();
         if (parent == null) {
            return null;
         } else if (config.getPropertyInParent().isDynamic()) {
            return parent.asComponent();
         } else {
            BComplex services = parent.getParent();
            if (services == null) {
               return null;
            } else {
               BComplex root = services.getParent();
               return root == null ? null : root.asComponent();
            }
         }
      }
   }

   private static class TargetSubscriber extends Subscriber {
      BTemplateConfig templateConfig;

      TargetSubscriber(BTemplateConfig templateConfig) {
         this.templateConfig = templateConfig;
         this.setMask(BComponentEventMask.make(1));
      }

      public void event(BComponentEvent event) {
         if (event.getId() == 0) {
            BComponent eventComponent = event.getSourceComponent();
            String eventSlotName = event.getSlot().getName();
            if (eventSlotName.equals("displayNames")) {
               BNameMap newNameMap = (BNameMap)event.getValue();
               this.refreshDisplayNameConfig(eventComponent, newNameMap);
            } else {
               Optional<BConfigBinding> optional = this.templateConfig.getConfigBinding(eventComponent, eventSlotName);
               if (optional.isPresent()) {
                  BValue eventValue = event.getValue();
                  Object var12;
                  if (eventValue instanceof BPassword) {
                     var12 = BPassword.DEFAULT;
                  } else if (eventValue instanceof BUsernameAndPassword) {
                     var12 = new BUsernameAndPassword(((BUsernameAndPassword)eventValue).getUsername(), BPassword.DEFAULT);
                  } else {
                     var12 = eventValue.newCopy();
                  }

                  String sourceSlot = optional.get().getSourceSlot();
                  if (!this.templateConfig.get(sourceSlot).equivalent(var12)) {
                     this.templateConfig.set(sourceSlot, (BValue)var12);
                  }
               }
            }
         } else if (event.getId() == 3) {
            BComponent eventComponent = event.getSourceComponent();
            Property eventProperty = (Property)event.getSlot();
            if (eventProperty != null) {
               BValue renamedPropertyValue = eventComponent.get(eventProperty);
               if (renamedPropertyValue != null && renamedPropertyValue.isComponent()) {
                  Optional<BConfigBinding> optional = this.templateConfig.getConfigBinding(renamedPropertyValue.asComponent(), "#Name");
                  if (optional.isPresent()) {
                     BString newName = BString.make(eventProperty.getName());
                     String sourceSlot = optional.get().getSourceSlot();
                     if (!this.templateConfig.get(sourceSlot).equivalent(newName)) {
                        this.templateConfig.set(sourceSlot, newName);
                     }
                  }
               }
            }

            this.refreshDisplayNameConfig(eventComponent);
         }
      }

      private void refreshDisplayNameConfig(BComponent parentComponent) {
         this.refreshDisplayNameConfig(parentComponent, (BNameMap)parentComponent.get("displayNames"));
      }

      private void refreshDisplayNameConfig(BComponent parentComponent, BNameMap newNameMap) {
         for (Property property : parentComponent.getPropertiesArray()) {
            BValue displayNameTarget = parentComponent.get(property);
            if (displayNameTarget.isComponent()) {
               Optional<BConfigBinding> optional = this.templateConfig.getConfigBinding(displayNameTarget.asComponent(), "#DisplayName");
               if (optional.isPresent()) {
                  BString newDisplayNameValue = BString.DEFAULT;
                  if (newNameMap != null) {
                     BFormat newDisplayNameFormat = newNameMap.get(property.getName());
                     if (newDisplayNameFormat != null) {
                        newDisplayNameValue = BString.make(newDisplayNameFormat.toString(null));
                     }
                  }

                  String sourceSlot = optional.get().getSourceSlot();
                  if (!this.templateConfig.get(sourceSlot).equivalent(newDisplayNameValue)) {
                     this.templateConfig.set(sourceSlot, newDisplayNameValue);
                  }
               }
            }
         }
      }
   }
}
