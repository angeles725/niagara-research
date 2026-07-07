package com.tridium.svg.batik;

import com.tridium.gx.awt.BImageDecoder;
import com.tridium.gx.awt.ImageAnimator;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.baja.agent.BIAgent;
import javax.baja.gx.BTransform.Scale;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import org.apache.batik.anim.dom.AnimatedAttributeListener;
import org.apache.batik.anim.dom.AnimatedLiveAttributeValue;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGOMAnimationElement;
import org.apache.batik.anim.dom.SVGOMDocument;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.SVGAnimationEngine;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.css.engine.CSSNavigableDocumentListener;
import org.apache.batik.css.engine.CSSStylableElement;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@NiagaraType(
   agent = {@AgentOn(
      types = {"file:SvgFile"},
      requiredPermissions = "r"
   )}
)
public class BSvgDecoder extends BImageDecoder implements BIAgent {
   public static final Type TYPE = Sys.loadType(BSvgDecoder.class);
   private static final RenderingHints HINTS = new RenderingHints(null);
   private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
   private BufferedImage image;
   private ImageAnimator animator;
   private static final String ENABLE_SVG_HYPERLINKING_SYSPROP = "niagara.svg.hyperlinking.enabled";
   private static final boolean ENABLE_SVG_HYPERLINKING = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.svg.hyperlinking.enabled"))
   );

   public Type getType() {
      return TYPE;
   }

   public void decode(BOrd ord, byte[] bytes, Scale scale) {
      try {
         InputStream inputStream = new ByteArrayInputStream(bytes);
         String uri = BatikOrdUtils.toBatikUrl(ord);
         SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
         SVGOMDocument document = (SVGOMDocument)f.createDocument(uri, inputStream);
         UserAgent userAgent = new UserAgentAdapter();
         BridgeContext bridgeContext = AccessController.doPrivileged(
            (PrivilegedAction<BridgeContext>)(() -> new BridgeContext(userAgent, new DocumentLoader(userAgent)) {
               @Override
               public SVGAnimationEngine getAnimationEngine() {
                  if (this.animationEngine == null) {
                     this.animationEngine = new SVGAnimationEngine(this.document, this) {
                        protected float tick(float time, boolean hyperlinking) {
                           return BSvgDecoder.ENABLE_SVG_HYPERLINKING ? super.tick(time, hyperlinking) : super.tick(time, false);
                        }
                     };
                     this.setAnimationLimitingMode();
                  }

                  return this.animationEngine;
               }
            })
         );
         bridgeContext.setDynamicState(2);
         GVTBuilder builder = new GVTBuilder();
         GraphicsNode gvtRoot = builder.build(bridgeContext, document);
         this.image = initImage(bridgeContext, scale);
         if (scale != null) {
            AffineTransform trans = new AffineTransform();
            trans.scale(scale.getX(), scale.getY());
            gvtRoot.setTransform(trans);
         }

         this.image = initImage(bridgeContext, scale);
         renderImage(this.image, gvtRoot);
         if (hasAnimationElement(document)) {
            this.animator = new BSvgDecoder.SvgAnimator(document, bridgeContext, gvtRoot, scale);
         } else {
            this.animator = null;
         }
      } catch (Exception var13) {
         throw new BajaRuntimeException(var13);
      }
   }

   public Image getImage() {
      return this.image;
   }

   public ImageAnimator getAnimator() {
      return this.animator;
   }

   private static BufferedImage initImage(BridgeContext bridgeContext, Scale scale) {
      Dimension2D dim = bridgeContext.getDocumentSize();
      return scale == null
         ? new BufferedImage((int)(dim.getWidth() + 0.5), (int)(dim.getHeight() + 0.5), 2)
         : new BufferedImage((int)(scale.getX() * dim.getWidth() + 0.5), (int)(scale.getY() * dim.getHeight() + 0.5), 2);
   }

   private static void clearBackground(Graphics2D g2d, int width, int height) {
      Graphics2D newG = (Graphics2D)g2d.create();

      try {
         newG.setBackground(TRANSPARENT);
         newG.clearRect(0, 0, width, height);
      } finally {
         newG.dispose();
      }
   }

   private static void renderImage(BufferedImage image, GraphicsNode gvtRoot) {
      Graphics2D g2d = (Graphics2D)image.getGraphics();
      g2d.addRenderingHints(HINTS);
      clearBackground(g2d, image.getWidth(), image.getHeight());
      gvtRoot.paint(g2d);
   }

   private static boolean hasAnimationElement(Node node) {
      if (node instanceof SVGOMAnimationElement) {
         return true;
      } else {
         NodeList children = node.getChildNodes();

         for (int i = 0; i < children.getLength(); i++) {
            if (hasAnimationElement(children.item(i))) {
               return true;
            }
         }

         return false;
      }
   }

   static {
      HINTS.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      HINTS.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      HINTS.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      ParsedURL.registerHandler(new OrdParsedURLProtocolHandler());
      ImageTagRegistry.getRegistry().register(new OrdRegistryEntry());
   }

   private static class SvgAnimator implements ImageAnimator, AnimatedAttributeListener, CSSNavigableDocumentListener {
      private final BridgeContext bridgeContext;
      private final GraphicsNode gvtRoot;
      private long baseMillis;
      private final BufferedImage[] animImages;
      private int curImage;
      private boolean changed = false;

      private SvgAnimator(SVGOMDocument document, BridgeContext bridgeContext, GraphicsNode gvtRoot, Scale scale) {
         this.bridgeContext = bridgeContext;
         this.gvtRoot = gvtRoot;
         this.baseMillis = System.currentTimeMillis();
         this.animImages = new BufferedImage[2];
         this.curImage = 0;
         this.animImages[0] = BSvgDecoder.initImage(bridgeContext, scale);
         this.animImages[1] = BSvgDecoder.initImage(bridgeContext, scale);
         BSvgDecoder.renderImage(this.animImages[this.curImage], gvtRoot);
         document.addAnimatedAttributeListener(this);
         document.addCSSNavigableDocumentListener(this);
         SVGAnimationEngine engine = bridgeContext.getAnimationEngine();
         engine.start(0L);
         engine.setCurrentTime(0.0F);
      }

      public boolean animate() {
         this.changed = false;
         float elapsed = (float)(System.currentTimeMillis() - this.baseMillis) / 1000.0F;
         this.bridgeContext.getAnimationEngine().setCurrentTime(elapsed);
         if (this.changed) {
            int otherImage = (this.curImage + 1) % 2;
            BSvgDecoder.renderImage(this.animImages[otherImage], this.gvtRoot);
            this.curImage = otherImage;
         }

         return this.changed;
      }

      public Image getAnimatedImage() {
         return this.animImages[this.curImage];
      }

      public void animatedAttributeChanged(Element e, AnimatedLiveAttributeValue alav) {
         this.changed = true;
      }

      public void otherAnimationChanged(Element e, String type) {
         this.changed = true;
      }

      public void nodeInserted(Node newNode) {
         this.changed = true;
      }

      public void nodeToBeRemoved(Node oldNode) {
         this.changed = true;
      }

      public void subtreeModified(Node rootOfModifications) {
         this.changed = true;
      }

      public void characterDataModified(Node text) {
         this.changed = true;
      }

      public void attrModified(Element e, Attr attr, short attrChange, String prevValue, String newValue) {
         this.changed = true;
      }

      public void overrideStyleTextChanged(CSSStylableElement e, String text) {
         this.changed = true;
      }

      public void overrideStylePropertyRemoved(CSSStylableElement e, String name) {
         this.changed = true;
      }

      public void overrideStylePropertyChanged(CSSStylableElement e, String name, String val, String prio) {
         this.changed = true;
      }
   }
}
