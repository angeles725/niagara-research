package com.tridium.nre.security.policy;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.baja.xml.XElem;

public abstract class XmlPolicyParser<T> {
   public static final String PERMISSIONS_ELEM = "permissions";
   public static final String TYPE_ATTR = "type";

   protected final Map<NiagaraPolicy.PolicyType, Set<T>> doParseAll(ICodeSourceInfo codeSource, XElem permissionsBlock) throws ParsingException {
      Map<NiagaraPolicy.PolicyType, Set<T>> map = new EnumMap<>(NiagaraPolicy.PolicyType.class);

      for (NiagaraPolicy.PolicyType type : NiagaraPolicy.PolicyType.values()) {
         Set<T> permissions = this.doParse(codeSource, permissionsBlock, type);
         if (!permissions.isEmpty()) {
            map.put(type, permissions);
         }
      }

      return map;
   }

   protected final Set<T> doParse(ICodeSourceInfo codeSource, XElem permissionsBlock, NiagaraPolicy.PolicyType type) throws ParsingException {
      if (!"permissions".equals(permissionsBlock.name())) {
         throw new ParsingException(String.format("%s: Unexpected XML block %s", codeSource.getName(), permissionsBlock.name()));
      }

      Set<T> permissions = new HashSet<>();

      for (XElem permissionsElem : permissionsBlock.elems(this.getPermissionsGroupName())) {
         String types = permissionsElem.get("type", null);
         if (types == null) {
            throw new ParsingException("Missing type attribute");
         }

         for (String policyType : types.split(",")) {
            if (policyType.equalsIgnoreCase(NiagaraPolicy.PolicyType.ALL.toString()) || policyType.equalsIgnoreCase(type.name())) {
               permissions.addAll(this.doParseElement(codeSource, permissionsElem));
            }
         }
      }

      return permissions;
   }

   protected abstract Set<T> doParseElement(ICodeSourceInfo var1, XElem var2) throws ParsingException;

   protected abstract String getPermissionsGroupName();
}
