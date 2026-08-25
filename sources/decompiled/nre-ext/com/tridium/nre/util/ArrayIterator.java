package com.tridium.nre.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ArrayIterator<E> implements ListIterator<E> {
   private E[] array;
   private int first = 0;
   private int size = 0;
   private int carat = 0;

   public ArrayIterator(E[] objects) {
      this(objects, 0, objects.length);
   }

   public ArrayIterator(E[] objects, int first, int size) {
      if (objects == null) {
         throw new NullPointerException();
      }

      if (first >= 0 && size <= objects.length) {
         this.array = objects;
         this.first = first;
         this.size = size;
         this.carat = first;
      } else {
         throw new IllegalArgumentException();
      }
   }

   @Override
   public void add(E o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean hasNext() {
      return this.carat < this.first + this.size;
   }

   @Override
   public boolean hasPrevious() {
      return this.carat > this.first;
   }

   @Override
   public E next() throws NoSuchElementException {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      } else {
         return this.array[this.carat++];
      }
   }

   @Override
   public int nextIndex() {
      return this.carat - this.first;
   }

   @Override
   public E previous() throws NoSuchElementException {
      if (!this.hasPrevious()) {
         throw new NoSuchElementException();
      } else {
         return this.array[--this.carat];
      }
   }

   @Override
   public int previousIndex() {
      return this.carat == this.first ? -1 : Math.min(this.first + this.size - 1, this.carat - this.first - 1);
   }

   @Override
   public void remove() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void set(Object o) {
      throw new UnsupportedOperationException();
   }
}
