package com.tridium.fox.encoding;

import com.tridium.fox.message.FoxMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.baja.io.ValueDocDecoder;
import javax.baja.io.ValueDocEncoder;
import javax.baja.io.ValueDocDecoder.BogDecoderPlugin;
import javax.baja.io.ValueDocEncoder.BogEncoderPlugin;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.xml.XException;

public final class BogCodec {
   public static void add(FoxMessage msg, String name, BValue object, Context context) throws IOException {
      ByteBuffer buf = encode(object, context);
      msg.add(name, "bog", buf.getBytes(), buf.getLength());
   }

   public static ByteBuffer encode(BValue object, Context context) throws IOException {
      ByteBuffer buf = new ByteBuffer(1024);
      BogEncoderPlugin plugin = new BogEncoderPlugin(buf.getOutputStream(), context);
      ValueDocEncoder out = new ValueDocEncoder(plugin, context);
      plugin.start("bog").attr("version", "1.0");
      if (object != null) {
         plugin.endAttr().newLine();
         out.setEncodeTransients(true);
         out.setEncodeComments(false);
         out.encode(object);
         plugin.end("bog").newLine();
      } else {
         plugin.end().newLine();
      }

      out.close();
      return buf;
   }

   static class Provider implements DecoderFactory.Provider {
      @Override
      public Object decode(byte[] buf, Object arg, Context cx) throws Exception {
         BogDecoderPlugin plugin = new BogDecoderPlugin(new ByteArrayInputStream(buf), cx);
         ValueDocDecoder in = new ValueDocDecoder(plugin, cx);
         in.next();
         if (!in.elem().name().equals("bog")) {
            throw new XException("Expected <bog>, not " + in.elem(), ((BogDecoderPlugin)in.getPlugin()).getXmlParser());
         } else {
            in.next();
            return in.type() == 2 ? null : in.decode();
         }
      }
   }
}
