package com.tridium.excel.impl;

import com.tridium.excel.Comment;
import com.tridium.excel.RichTextString;

public class CommentImpl implements Comment {
   final org.apache.poi.ss.usermodel.Comment comment;

   CommentImpl(org.apache.poi.ss.usermodel.Comment comment) {
      this.comment = comment;
   }

   public void setAuthor(String author) {
      this.comment.setAuthor(author);
   }

   public void setString(RichTextString string) {
      this.comment.setString(((RichTextStringImpl)string).string);
   }

   @Override
   public String toString() {
      return this.comment.toString();
   }
}
