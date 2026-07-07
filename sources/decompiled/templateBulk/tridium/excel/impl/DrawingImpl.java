package com.tridium.excel.impl;

import com.tridium.excel.ClientAnchor;
import com.tridium.excel.Comment;
import org.apache.poi.ss.usermodel.Drawing;

public class DrawingImpl implements com.tridium.excel.Drawing {
   final Drawing<?> drawing;

   DrawingImpl(Drawing<?> drawing) {
      this.drawing = drawing;
   }

   public Comment createCellComment(ClientAnchor anchor) {
      org.apache.poi.ss.usermodel.Comment comment = this.drawing.createCellComment(((ClientAnchorImpl)anchor).anchor);
      return comment == null ? null : new CommentImpl(comment);
   }

   @Override
   public String toString() {
      return this.drawing.toString();
   }
}
