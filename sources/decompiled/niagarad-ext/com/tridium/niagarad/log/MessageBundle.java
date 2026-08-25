package com.tridium.niagarad.log;

import javax.baja.nre.util.TextUtil;
import javax.baja.xml.XWriter;

public class MessageBundle {
   private final String lexiconModule;
   private final String lexiconKey;
   String nonLocalizedMessage;
   private MessageBundle.LexiconArgument lexiconArgs;

   public MessageBundle(String pLexiconModule, String pLexiconKey, String pNonLocalizedMessageFormat) {
      this.lexiconArgs = null;
      this.lexiconModule = pLexiconModule;
      this.lexiconKey = pLexiconKey;
      this.nonLocalizedMessage = pNonLocalizedMessageFormat;
   }

   public MessageBundle(String pLexiconModule, String pLexiconKey, String pLexiconArg, String pNonLocalizedMessageFormat) {
      this(pLexiconModule, pLexiconKey, pNonLocalizedMessageFormat);
      this.lexiconArgs = new MessageBundle.LexiconArgument(pLexiconArg);
   }

   public MessageBundle(String pLexiconModule, String pLexiconKey, int pLexiconArg, String pLonLocalizedMessageFormat) {
      this(pLexiconModule, pLexiconKey, String.valueOf(pLexiconArg), pLonLocalizedMessageFormat);
   }

   public MessageBundle(String pNonLocalizedMessageString) {
      this.lexiconArgs = null;
      this.lexiconModule = null;
      this.lexiconKey = null;
      this.nonLocalizedMessage = pNonLocalizedMessageString;
   }

   public MessageBundle(MessageBundle toCopy) {
      if (toCopy.lexiconArgs == null) {
         this.lexiconArgs = null;
      } else {
         MessageBundle.LexiconArgument itemToCopy = toCopy.lexiconArgs;
         MessageBundle.LexiconArgument newItem = new MessageBundle.LexiconArgument(itemToCopy.text);

         for (this.lexiconArgs = newItem; itemToCopy.next != null; newItem = newItem.next) {
            itemToCopy = itemToCopy.next;
            newItem.next = new MessageBundle.LexiconArgument(itemToCopy.text);
         }
      }

      this.lexiconKey = toCopy.lexiconKey;
      this.lexiconModule = toCopy.lexiconModule;
      this.nonLocalizedMessage = toCopy.nonLocalizedMessage;
   }

   public void addLexiconArgument(String arg) {
      MessageBundle.LexiconArgument tail = this.lexiconArgs;
      if (tail == null) {
         this.lexiconArgs = new MessageBundle.LexiconArgument(arg);
      } else {
         while (tail.next != null) {
            tail = tail.next;
         }

         tail.next = new MessageBundle.LexiconArgument(arg);
      }
   }

   public void addLexiconArgument(int arg) {
      this.addLexiconArgument(String.valueOf(arg));
   }

   public void appendXML(XWriter out) {
      out.w("<message>\n");
      if (this.nonLocalizedMessage == null) {
         out.w("<nonlocalized ").attr("text", "").w("/>\n");
      } else {
         this.nonLocalizedMessage = TextUtil.replace(this.nonLocalizedMessage, "\n", " ");
         this.nonLocalizedMessage = TextUtil.replace(this.nonLocalizedMessage, "\r", " ");
         this.nonLocalizedMessage = this.nonLocalizedMessage.trim();
         out.w("<nonlocalized ").attr("text", this.nonLocalizedMessage).w("/>\n");
      }

      if (this.lexiconModule != null && this.lexiconKey != null) {
         out.w("<localized").w(' ').attr("module", this.lexiconModule).w(' ').attr("key", this.lexiconKey);
         if (this.lexiconArgs == null) {
            out.w("/>\n");
         } else {
            out.w(">\n");

            for (MessageBundle.LexiconArgument printMe = this.lexiconArgs; printMe != null; printMe = printMe.next) {
               out.w("<lexArg ").attr("value", printMe.text).w("/>\n");
            }

            out.w("</localized>\n");
         }
      }

      out.w("</message>\n");
   }

   public String getNonLocalizedMessage() {
      return this.nonLocalizedMessage;
   }

   private static class LexiconArgument {
      public String text;
      public MessageBundle.LexiconArgument next;

      public LexiconArgument(String argText) {
         this.text = argText == null ? "null" : argText;
         this.next = null;
      }
   }
}
