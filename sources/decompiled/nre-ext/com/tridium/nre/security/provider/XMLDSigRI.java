package com.tridium.nre.security.provider;

import com.tridium.nre.util.tuple.Pair;
import java.security.AccessController;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.Provider.Service;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class XMLDSigRI extends Provider {
   private static final String NAME = "TridiumXMLDSig";
   private static final double VERSION = 1.0;
   private static final String INFO = "TridiumXMLDSig (DOM XMLSignatureFactory; DOM KeyInfoFactory; C14N 1.0, Enveloped)";
   private static final long serialVersionUID = 6115102542958026879L;

   public XMLDSigRI(Provider provider) {
      super("TridiumXMLDSig", 1.0, "TridiumXMLDSig (DOM XMLSignatureFactory; DOM KeyInfoFactory; C14N 1.0, Enveloped)");
      if ("XMLDSig".equals(provider.getName())) {
         AccessController.doPrivileged(
            () -> {
               HashMap<String, String> mechanismTypeMap = new HashMap<>();
               mechanismTypeMap.put("MechanismType", "DOM");
               Service xmlSignatureFactory = provider.getService("XMLSignatureFactory", "DOM");
               Service keyInfoFactory = provider.getService("KeyInfoFactory", "DOM");
               Service envelopedTransform = provider.getService("TransformService", "http://www.w3.org/2000/09/xmldsig#enveloped-signature");
               Service c14nTransform = provider.getService("TransformService", "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
               this.putService(new XMLDSigRI.ProviderService(xmlSignatureFactory, this, "XMLSignatureFactory", "DOM", xmlSignatureFactory.getClassName()));
               this.putService(new XMLDSigRI.ProviderService(keyInfoFactory, this, "KeyInfoFactory", "DOM", keyInfoFactory.getClassName()));
               this.putService(
                  new XMLDSigRI.ProviderService(
                     envelopedTransform,
                     this,
                     "TransformService",
                     "http://www.w3.org/2000/09/xmldsig#enveloped-signature",
                     envelopedTransform.getClassName(),
                     new String[]{"ENVELOPED"},
                     mechanismTypeMap
                  )
               );
               this.putService(
                  new XMLDSigRI.ProviderService(
                     c14nTransform,
                     this,
                     "TransformService",
                     "http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                     c14nTransform.getClassName(),
                     new String[]{"INCLUSIVE"},
                     mechanismTypeMap
                  )
               );
               return null;
            }
         );
      }
   }

   private static final class ProviderService extends Service {
      Map<Pair<String, String>, Service> serviceMap = new HashMap<>();

      ProviderService(Service service, Provider provider, String type, String algorithm, String className) {
         this(service, provider, type, algorithm, className, null, null);
      }

      ProviderService(Service service, Provider provider, String type, String algorithm, String className, String[] aliases, Map<String, String> attributes) {
         super(provider, type, algorithm, className, aliases == null ? null : Arrays.asList(aliases), attributes);
         Pair<String, String> pair = new Pair<>(type, algorithm);
         this.serviceMap.put(pair, service);
      }

      @Override
      public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
         String type = this.getType();
         if (constructorParameter != null) {
            throw new InvalidParameterException("constructorParameter not used with " + type + " engines");
         }

         String algorithm = this.getAlgorithm();
         Pair<String, String> pair = new Pair<>(type, algorithm);
         Service service = this.serviceMap.get(pair);
         if (service != null) {
            try {
               return service.newInstance(constructorParameter);
            } catch (Exception e) {
               throw new NoSuchAlgorithmException("Error constructing " + type + " for " + algorithm + " using TridiumXMLDSig", e);
            }
         } else {
            throw new ProviderException("No impl for " + type + ' ' + algorithm + " using TridiumXMLDSig");
         }
      }
   }
}
