package com.tridium.fox.sys.broker;

import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.nre.platform.RuntimeProfile;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Lexicon;

public class SyntheticTypeInfo implements TypeInfo {
   private final Type type;
   Lexicon lexicon;

   public SyntheticTypeInfo(Type type) {
      this.type = type;
      this.lexicon = Lexicon.make("baja");
   }

   public AgentInfo getAgentInfo() {
      return null;
   }

   public String getDisplayName(Context cx) {
      return this.type.getTypeSpec().getTypeName();
   }

   public BIcon getIcon(Context cx) {
      return null;
   }

   public BObject getInstance() {
      return this.type.getInstance();
   }

   public TypeInfo[] getInterfaces() {
      return new TypeInfo[0];
   }

   public Lexicon getLexicon(Context cx) {
      return this.lexicon;
   }

   public String getModuleName() {
      return this.type.getTypeSpec().getModuleName();
   }

   public RuntimeProfile getRuntimeProfile() {
      return this.type.getRuntimeProfile();
   }

   public TypeInfo getSuperType() {
      return this.type.getSuperType().getTypeInfo();
   }

   public String getTypeClassName() {
      return this.type.getTypeSpec().getResolvedType().getTypeClass().getName();
   }

   public String getTypeName() {
      return this.type.getTypeName();
   }

   public BTypeSpec getTypeSpec() {
      return this.type.getTypeSpec();
   }

   public boolean is(TypeInfo typeInfo) {
      return typeInfo == this || this.type.getSuperType().is(typeInfo);
   }

   public boolean is(Type type) {
      return type.getTypeInfo() == this || this.type.getSuperType().is(type);
   }

   public boolean isAbstract() {
      return false;
   }

   public boolean isFinal() {
      return false;
   }

   public boolean isInterface() {
      return false;
   }

   public boolean isTransient() {
      return true;
   }

   public AgentList getAgents() {
      return BComponent.TYPE.getTypeInfo().getAgents();
   }
}
