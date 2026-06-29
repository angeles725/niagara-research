package com.tridium.lonworks.util.selfdoc;

import com.tridium.lonworks.util.NameUtil;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

public class NodeSelfDoc {
   private boolean debug;
   public String selfDocString;
   public int minorVersion = 0;
   public NodeSelfDoc.LonMarkObj[] objects = null;
   public String description;
   public NodeSelfDoc.SuperCat[] cats = null;
   private boolean valid = false;
   private boolean compressSuperCats = false;

   public NodeSelfDoc(String selfDoc) {
      this(selfDoc, false);
   }

   public NodeSelfDoc(String selfDoc, boolean debug) {
      this.debug = debug;
      this.selfDocString = selfDoc;
      if (selfDoc.length() != 0) {
         if (selfDoc.indexOf(64) >= 1) {
            if (selfDoc.startsWith("&")) {
               selfDoc = selfDoc.substring(1);
            }

            try {
               this.parseNodeSelfDocumentation(selfDoc);
            } catch (Throwable var4) {
               System.out.println("Unable to parse node self doc \"" + selfDoc + "\":" + var4);
            }
         }
      }
   }

   public void compressSuperCats() {
      this.compressSuperCats = true;
   }

   public NodeSelfDoc.LonMarkObj[] getObjects() {
      return this.objects;
   }

   public NodeSelfDoc.LonMarkObj getObject(int index) {
      return this.objects != null && index < this.objects.length ? this.objects[index] : null;
   }

   public int getObjectType(int index) {
      return this.objects != null && index < this.objects.length ? this.objects[index].getType() : -1;
   }

   public boolean hasLonObjects() {
      return this.objects != null;
   }

   public NodeSelfDoc.SuperCat[] getSuperCats() {
      if (!this.hasLonObjects()) {
         return null;
      } else {
         if (this.cats == null) {
            this.assignSuperCat();
         }

         return this.cats;
      }
   }

   public boolean hasNodeObject() {
      if (this.objects != null) {
         for (int i = 0; i < this.objects.length; i++) {
            if (this.objects[i] != null && this.objects[i].getType() == 0) {
               return true;
            }
         }
      }

      return false;
   }

   public int getNodeObjectIndex() {
      if (this.objects != null) {
         for (int i = 0; i < this.objects.length; i++) {
            if (this.objects[i] != null && this.objects[i].getType() == 0) {
               return i;
            }
         }
      }

      return -1;
   }

   public int getSuperCat(int objectIndex) {
      if (!this.hasLonObjects()) {
         return -1;
      } else {
         if (this.cats == null) {
            this.assignSuperCat();
         }

         return objectIndex >= 0 && objectIndex < this.objects.length ? this.objects[objectIndex].catIndex : -1;
      }
   }

   public String[] getSuperCatNames() {
      if (!this.hasLonObjects()) {
         return null;
      } else {
         if (this.cats == null) {
            this.assignSuperCat();
         }

         String[] catNames = new String[this.cats.length];

         for (int j = 0; j < this.cats.length; j++) {
            NodeSelfDoc.SuperCat cat = this.cats[j];
            if (cat.getName() != null && this.makeLegal(cat.getName()).length() > 0) {
               catNames[j] = NameUtil.toJavaName(this.makeLegal(cat.getName()), true);
            } else {
               String name = LonMarkObjectUtil.getObjectName(cat.getType());
               int typeCount = cat.getTypeCount();
               if (typeCount != 0) {
                  name = name + typeCount;
               }

               cat.setName(name);
               catNames[j] = name;
            }
         }

         MakeUniqueSupercatName uniquenessTool = new MakeUniqueSupercatName();

         for (int i = 0; i < this.cats.length; i++) {
            catNames[i] = uniquenessTool.getUniqueScatName(catNames[i]);
         }

         return catNames;
      }
   }

   public boolean isValid() {
      return this.valid;
   }

   private String makeLegal(String correct) {
      StringBuilder sendBack = new StringBuilder();
      int len = correct.length();

      for (int i = 0; i < len; i++) {
         char check = correct.charAt(i);
         if (i == 0 && Character.isJavaIdentifierStart(check) || i > 0 && Character.isJavaIdentifierPart(check)) {
            sendBack.append(check);
         }
      }

      return sendBack.toString();
   }

   private void assignSuperCat() {
      int objCnt = this.objects.length;
      HashMap<Integer, NodeSelfDoc.SuperCat> catHash = new HashMap<>(objCnt);
      Vector<NodeSelfDoc.SuperCat> catsVect = new Vector<>();
      int catCnt = 0;

      for (int i = 0; i < objCnt; i++) {
         NodeSelfDoc.LonMarkObj obj = this.objects[i];
         int type = obj.type;
         NodeSelfDoc.SuperCat cat;
         if (!this.compressSuperCats || (cat = catHash.get(type)) == null) {
            int typeCount = 0;
            if (!this.compressSuperCats) {
               NodeSelfDoc.SuperCat prev = catHash.get(type);
               if (prev != null) {
                  typeCount = prev.typeCount + 1;
               }
            }

            cat = new NodeSelfDoc.SuperCat(type, catCnt++, typeCount);
            cat.name = obj.name;
            catHash.put(type, cat);
            catsVect.addElement(cat);
         }

         obj.catIndex = cat.index;
         obj.catMember = cat.count++;
      }

      this.cats = new NodeSelfDoc.SuperCat[catsVect.size()];
      catsVect.copyInto(this.cats);
   }

   private void parseNodeSelfDocumentation(String line) {
      if (this.debug) {
         System.out.println("\nParsing self doc =>" + line);
      }

      int posAmp = line.indexOf(64);
      this.minorVersion = Integer.parseInt(line.substring(line.indexOf(46) + 1, posAmp));
      String data = line.substring(posAmp + 1, line.length());
      int suffixIndex = data.indexOf(59);
      if (suffixIndex != -1) {
         this.description = data.substring(suffixIndex);
         data = data.substring(0, suffixIndex);
      }

      StringTokenizer st = new StringTokenizer(data, ",");
      Vector<NodeSelfDoc.LonMarkObj> objectList = new Vector<>();
      int objectIndex = 0;

      while (st.hasMoreTokens()) {
         int arrayCount = 0;
         NodeSelfDoc.LonMarkObj object = new NodeSelfDoc.LonMarkObj();
         String token = st.nextToken().trim();
         object.type = this.getInteger(token);
         token = this.removeInteger(token);
         if (token.length() > 0 && token.startsWith("[")) {
            token = token.substring(1);
            arrayCount = this.getInteger(token);
            token = this.removeInteger(token);
            if (token.startsWith("]")) {
               token = token.length() > 1 ? token.substring(1) : "";
            }
         }

         if (this.debug) {
            System.out.println("token=" + token);
         }

         String groupPrefix = null;
         if (token.length() > 0) {
            if (arrayCount > 1) {
               groupPrefix = token;
               object.name = token + "1";
            } else {
               object.name = token;
            }
         } else {
            object.name = null;
         }

         if (this.debug) {
            System.out.println("\ntype " + object.type + " arrayCount = " + arrayCount + " name = " + object.name + " objectIndex = " + objectIndex);
         }

         object.index = objectIndex++;
         objectList.addElement(object);

         while (arrayCount > 1) {
            NodeSelfDoc.LonMarkObj obj = new NodeSelfDoc.LonMarkObj();
            obj.type = object.type;
            if (groupPrefix != null) {
               obj.name = groupPrefix + (objectIndex - object.index + 1);
            }

            if (this.debug) {
               System.out
                  .println("\ntype " + obj.type + " arrayCount = " + arrayCount + " name = " + obj.name + " objectIndex = " + objectIndex + " groupPrefix=");
            }

            obj.index = objectIndex++;
            objectList.addElement(obj);
            arrayCount--;
         }
      }

      this.objects = new NodeSelfDoc.LonMarkObj[objectList.size()];
      objectList.copyInto(this.objects);
      this.valid = true;
   }

   private int getInteger(String s) {
      int i = 0;

      while (i < s.length() && Character.isDigit(s.charAt(i))) {
         i++;
      }

      return Integer.parseInt(s.substring(0, i));
   }

   private String removeInteger(String s) {
      int i = 0;

      while (i < s.length() && Character.isDigit(s.charAt(i))) {
         i++;
      }

      if (s.length() > i) {
         s = s.substring(i);
      } else {
         s = "";
      }

      return s;
   }

   public static class LonMarkObj {
      public int type;
      public String name;
      public int index;
      public int catIndex;
      public int catMember;

      public int getType() {
         return this.type;
      }

      public int getIndex() {
         return this.index;
      }

      public int getCatIndex() {
         return this.catIndex;
      }

      public int getCatMember() {
         return this.catMember;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return "type; " + this.type + " index: " + this.index + " name: " + this.name;
      }
   }

   public static class SuperCat {
      public String name;
      public int type;
      public int count = 0;
      public int index;
      public int typeCount;

      public SuperCat(int type, int index, int typeCount) {
         this.type = type;
         this.index = index;
         this.typeCount = typeCount;
      }

      public int getType() {
         return this.type;
      }

      public int getTypeCount() {
         return this.typeCount;
      }

      public String getName() {
         return this.name;
      }

      public void setName(String name) {
         this.name = name;
      }
   }
}
