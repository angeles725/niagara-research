package javax.baja.xml;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class XParserEventGenerator {
   public static final int ANY_EVENT = -1;
   private static final Logger logger = Logger.getLogger(XParserEventGenerator.class.getName());
   private final List<XParserEventGenerator.EventListenerRegistration> registrations = new LinkedList<>();
   private final XElemLocation location = new XElemLocation();
   private final XParser parser;
   private int currentPosition = 0;
   private int insertPosition = 0;

   public XParserEventGenerator(XParser parser) {
      this.parser = parser;
   }

   public void addListener(int eventId, XParserEventListener listener) {
      this.addListener(eventId, null, listener);
   }

   public void addListener(XParserEventListener listener) {
      this.addListener(-1, null, listener);
   }

   public void addListener(XPath path, XParserEventListener listener) {
      this.addListener(-1, path, listener);
   }

   public void addListener(int eventId, XPath path, XParserEventListener listener) {
      XParserEventGenerator.EventListenerRegistration registration = new XParserEventGenerator.EventListenerRegistration(eventId, path, listener, this.location);
      logger.fine(() -> String.format("Inserting listener @ position %d [%s]", this.insertPosition, registration));
      this.registrations.add(this.insertPosition, registration);
      this.insertPosition++;
   }

   public void removeListener(XParserEventListener listener) {
      int index = 0;
      int priorElementsRemoved = 0;

      for (Iterator<XParserEventGenerator.EventListenerRegistration> i = this.registrations.iterator(); i.hasNext(); index++) {
         XParserEventGenerator.EventListenerRegistration registration = i.next();
         if (registration.listener.equals(listener)) {
            i.remove();
            int finalIndex = index;
            logger.fine(() -> String.format("Removing listener @ position %d [%s]", finalIndex, registration));
            if (index <= this.currentPosition) {
               logger.fine(() -> String.format("Element was before currentPosition %d", this.currentPosition));
               priorElementsRemoved++;
            }
         }
      }

      this.currentPosition -= priorElementsRemoved;
      this.insertPosition -= priorElementsRemoved;
      logger.fine(() -> String.format("After remove: currentPosition = %d, insertPosition = %d", this.currentPosition, this.insertPosition));
   }

   private void dispatchEvent(XParserEvent event) {
      logger.fine(() -> String.format("Starting dispatch loop, registered listeners = %d", this.registrations.size()));

      for (this.currentPosition = 0; this.currentPosition < this.registrations.size(); this.currentPosition++) {
         this.insertPosition = this.currentPosition + 1;
         logger.fine(() -> String.format("Calling listener @ position %d [%s]", this.currentPosition, this.registrations.get(this.currentPosition)));
         this.registrations.get(this.currentPosition).processEvent(event);
      }
   }

   public void run() {
      try {
         XParserEventGenerator.XParserEventImpl event = new XParserEventGenerator.XParserEventImpl();
         this.location.clear();
         int nodeType = this.parser.next();
         boolean selfClosing = false;

         while (nodeType != -1) {
            switch (nodeType) {
               case -1:
               case 0:
               default:
                  break;
               case 1:
                  selfClosing = this.parser.emptyElem();
                  XElem elem = this.parser.elem().copy();
                  this.location.addElement(elem);
                  event.setAll(1, this.location, elem, selfClosing);
                  this.dispatchEvent(event);
                  break;
               case 2:
                  event.setAll(2, this.location, this.location.get(this.location.size() - 1), selfClosing);
                  this.dispatchEvent(event);
                  this.location.removeElement();
                  selfClosing = false;
                  break;
               case 3:
                  XText text = this.parser.text();
                  event.setAll(3, this.location, text, false);
                  this.dispatchEvent(event);
            }

            nodeType = this.parser.next();
         }
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private static class EventListenerRegistration {
      private final int eventId;
      private final XParserEventListener listener;
      private XPathMatcher matcher;

      public EventListenerRegistration(int eventId, XPath path, XParserEventListener listener, XElemLocation location) {
         this.eventId = eventId;
         this.listener = listener;
         if (path != null) {
            this.matcher = path.getMatcher(location);
         }
      }

      public void processEvent(XParserEvent event) {
         if ((this.eventId == -1 || this.eventId == event.getEventId()) && (this.matcher == null || this.matcher.matches())) {
            this.listener.handleEvent(event);
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (o != null && this.getClass() == o.getClass()) {
            XParserEventGenerator.EventListenerRegistration that = (XParserEventGenerator.EventListenerRegistration)o;
            if (this.eventId != that.eventId) {
               return false;
            } else if (this.listener != null ? this.listener.equals(that.listener) : that.listener == null) {
               return this.matcher != null ? this.matcher.equals(that.matcher) : that.matcher == null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = this.eventId;
         result = 31 * result + (this.listener != null ? this.listener.hashCode() : 0);
         return 31 * result + (this.matcher != null ? this.matcher.hashCode() : 0);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("EventListenerRegistration{");
         sb.append("eventId=").append(this.eventId);
         sb.append(", listener=").append(this.listener);
         sb.append(", matcher=").append(this.matcher);
         sb.append('}');
         return sb.toString();
      }
   }

   private static class XParserEventImpl implements XParserEvent {
      private XContent content;
      private XElemLocation location;
      private int eventId;
      private boolean selfClosing;

      private XParserEventImpl() {
      }

      public void clear() {
         this.setAll(0, null, null, false);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         }

         if (o != null && this.getClass() == o.getClass()) {
            XParserEventGenerator.XParserEventImpl that = (XParserEventGenerator.XParserEventImpl)o;
            if (this.eventId != that.eventId) {
               return false;
            } else if (this.selfClosing != that.selfClosing) {
               return false;
            } else if (this.location != null ? this.location.equals(that.location) : that.location == null) {
               return this.content != null ? this.content.equals(that.content) : that.content == null;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      @Override
      public XContent getContent() {
         return this.content;
      }

      @Override
      public XElemLocation getLocation() {
         return this.location;
      }

      @Override
      public int getEventId() {
         return this.eventId;
      }

      @Override
      public int hashCode() {
         int result = this.eventId;
         result = 31 * result + (this.location != null ? this.location.hashCode() : 0);
         result = 31 * result + (this.content != null ? this.content.hashCode() : 0);
         return 31 * result + (this.selfClosing ? 1 : 0);
      }

      @Override
      public boolean isSelfClosing() {
         return this.selfClosing;
      }

      public void setAll(int eventId, XElemLocation location, XContent content, boolean selfClosing) {
         this.eventId = eventId;
         this.location = location;
         this.content = content;
         this.selfClosing = selfClosing;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("XParserEventImpl{");
         sb.append("eventId=").append(this.eventId);
         sb.append(", location=").append(this.location);
         sb.append(", content=").append(this.content);
         sb.append(", selfClosing=").append(this.selfClosing);
         sb.append('}');
         return sb.toString();
      }
   }
}
