package com.tridium.bacnet.stack.network;

import java.util.List;

public class DataAttributes {
   private List<DataAttribute> attributes;
   private List<SecurityParameter> securityParameters;

   public DataAttributes(List<DataAttribute> attributes) {
      this.attributes = attributes;
      this.securityParameters = null;
   }

   public List<DataAttribute> getAttributes() {
      return this.attributes;
   }

   public void setAttributes(List<DataAttribute> attributes) {
      this.attributes = attributes;
   }

   public List<SecurityParameter> getSecurityParameters() {
      return this.securityParameters;
   }

   public void setSecurityParameters(List<SecurityParameter> securityParameters) {
      this.securityParameters = securityParameters;
   }
}
