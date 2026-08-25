package javax.baja.nre.util;

import com.tridium.nre.util.ArrayIterator;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class Array<T> implements Iterable<T> {
   T[] array;
   int size;
   Class<T> ofClass;

   @Deprecated
   public Array() {
      this((Class<T>)Object.class, 10);
   }

   public Array(Class<T> ofClass) {
      this(ofClass, 10);
   }

   public Array(Class<T> ofClass, int capacity) {
      this.ofClass = ofClass;
      this.array = (T[])((Object[])java.lang.reflect.Array.newInstance(ofClass, capacity));
   }

   public Array(T[] array) {
      this(array, array.length);
   }

   public Array(T[] array, int size) {
      this.ofClass = (Class<T>)array.getClass().getComponentType();
      this.array = array;
      this.size = size;
   }

   public Array(Class<T> ofClass, Collection<T> c) {
      this.ofClass = ofClass;
      this.array = (T[])c.toArray((T[])((Object[])java.lang.reflect.Array.newInstance(ofClass, c.size())));
      this.size = this.array.length;
   }

   @Deprecated
   public Array(Collection<T> c) {
      this((Class<T>)Object.class, c);
   }

   public final Class<T> ofClass() {
      return this.ofClass;
   }

   public final T get(int index) {
      if (index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return this.array[index];
      }
   }

   public final int indexOf(T item) {
      return this.indexOf(item, 0);
   }

   public final int indexOf(T item, int fromIndex) {
      for (int i = fromIndex; i < this.size; i++) {
         if (item == null ? this.array[i] == null : item.equals(this.array[i])) {
            return i;
         }
      }

      return -1;
   }

   public final int lastIndexOf(T item) {
      return this.lastIndexOf(item, this.size - 1);
   }

   public final int lastIndexOf(T item, int fromIndex) {
      for (int i = fromIndex; i >= 0; i--) {
         if (item == null ? this.array[i] == null : item.equals(this.array[i])) {
            return i;
         }
      }

      return -1;
   }

   public final boolean contains(T item) {
      return this.indexOf(item) >= 0;
   }

   public final int size() {
      return this.size;
   }

   public final boolean isEmpty() {
      return this.size == 0;
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof Array) {
         Array<T> a = (Array<T>)o;
         if (this.size == a.size && this.ofClass() == a.ofClass()) {
            for (int i = 0; i < this.size; i++) {
               T mi = this.array[i];
               T ai = a.array[i];
               if (mi == null) {
                  if (ai != null) {
                     return false;
                  }
               } else {
                  if (ai == null) {
                     return false;
                  }

                  if (!mi.equals(ai)) {
                     return false;
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = Arrays.hashCode(this.array);
      return 31 * result + this.size;
   }

   @Override
   public String toString() {
      StringBuilder s = new StringBuilder();
      s.append('{');

      for (int i = 0; i < this.size; i++) {
         if (i > 0) {
            s.append(',');
         }

         s.append(this.array[i]);
      }

      s.append('}');
      return s.toString();
   }

   public final <E extends T> boolean add(E item) {
      this.grow(this.size + 1);
      this.array[this.size] = (T)item;
      this.size++;
      return true;
   }

   public final <E extends T> void add(int index, E item) {
      if (index > this.size) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      this.grow(this.size + 1);
      System.arraycopy(this.array, index, this.array, index + 1, this.size - index);
      this.array[index] = (T)item;
      this.size++;
   }

   public final <E extends T> void addAll(E[] array) {
      this.addAll(array, array.length);
   }

   public final void addAll(Array<? extends T> array) {
      this.addAll(array.array(), array.size());
   }

   public final <E extends T> void addAll(E[] array, int size) {
      for (int i = 0; i < size; i++) {
         this.add(array[i]);
      }
   }

   public final void addAll(Collection<? extends T> c) {
      c.forEach(this::add);
   }

   public final <E extends T> T set(int index, E item) {
      if (index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      T old = this.array[index];
      this.array[index] = (T)item;
      return old;
   }

   public final T remove(int index) {
      if (index >= this.size) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      T item = this.array[index];
      if (index < this.array.length) {
         System.arraycopy(this.array, index + 1, this.array, index, this.array.length - index - 1);
      }

      this.array[this.size - 1] = null;
      this.size--;
      return item;
   }

   public final boolean remove(T item) {
      int index = this.indexOf(item);
      if (index < 0) {
         return false;
      }

      this.remove(index);
      return true;
   }

   public Array<T> removeAll(T[] items) {
      return this.shrink(items, true);
   }

   public final void removeRange(int fromIndex, int toIndex) {
      int numMoved = this.size - toIndex;
      System.arraycopy(this.array, toIndex, this.array, fromIndex, numMoved);
      int newSize = this.size - (toIndex - fromIndex);

      while (this.size != newSize) {
         this.array[--this.size] = null;
      }
   }

   public Array<T> slice(int fromIndex, int toIndex) {
      int len = toIndex - fromIndex;
      T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass, len));
      System.arraycopy(this.array, fromIndex, temp, 0, len);
      return new Array<>(temp);
   }

   public Array<T> intersection(T[] items) {
      return this.shrink(items, false);
   }

   private Array<T> shrink(T[] items, boolean remove) {
      int len = remove ? this.size : 0;
      boolean[] keep = new boolean[this.size];

      for (int i = 0; i < this.size; i++) {
         keep[i] = remove;
      }

      for (T item : items) {
         int idx = this.indexOf(item);
         if (idx != -1) {
            if (remove) {
               len--;
            } else {
               len++;
            }

            keep[idx] = !remove;
         }
      }

      T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), len));
      len = 0;

      for (int i = 0; i < this.size; i++) {
         if (keep[i]) {
            temp[len++] = this.array[i];
         }
      }

      return new Array<>(temp);
   }

   public final <E extends T> void push(E item) {
      this.add(item);
   }

   public final T pop() {
      return this.remove(this.size - 1);
   }

   public final T peek() {
      return this.get(this.size - 1);
   }

   public final T first() {
      return this.size == 0 ? null : this.get(0);
   }

   public final T last() {
      return this.size == 0 ? null : this.get(this.size - 1);
   }

   public final void grow(int length) {
      if (this.array.length < length) {
         int len = Math.max(this.array.length * 2, length);
         if (len < 10) {
            len = 10;
         }

         T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), len));
         System.arraycopy(this.array, 0, temp, 0, this.size);
         this.array = temp;
      }
   }

   public final T[] array() {
      return this.array;
   }

   public final T[] trim() {
      if (this.array.length != this.size) {
         T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), this.size));
         System.arraycopy(this.array, 0, temp, 0, this.size);
         this.array = temp;
      }

      return this.array;
   }

   public final void clear() {
      this.array = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), this.array.length));
      this.size = 0;
   }

   public final ListIterator<T> iterator() {
      return new ArrayIterator<>(this.array, 0, this.size);
   }

   public final List<T> list() {
      return new Array.AsList(this);
   }

   public void swap(int i1, int i2) {
      T temp = this.get(i1);
      this.set(i1, this.get(i2));
      this.set(i2, temp);
   }

   public Array<T> copy() {
      T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), this.size));
      System.arraycopy(this.array, 0, temp, 0, this.size);
      return new Array<>(temp, this.size);
   }

   public Array<T> copy(int beginIndex) {
      return this.copy(beginIndex, this.size);
   }

   public Array<T> copy(int beginIndex, int endIndex) {
      if (beginIndex < 0) {
         throw new ArrayIndexOutOfBoundsException(beginIndex);
      }

      if (endIndex > this.size) {
         throw new ArrayIndexOutOfBoundsException(endIndex);
      }

      int len = endIndex - beginIndex;
      if (len < 0) {
         throw new ArrayIndexOutOfBoundsException(len);
      }

      T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), len));
      System.arraycopy(this.array, beginIndex, temp, 0, len);
      return new Array<>(temp, len);
   }

   public Array<T> filter(IFilter filter) {
      Array<T> result = new Array<>(this.ofClass());

      for (int i = 0; i < this.size; i++) {
         if (filter.accept(this.array[i])) {
            result.add(this.array[i]);
         }
      }

      return result;
   }

   public Array<T> filterNull() {
      return this.filter(obj -> obj != null);
   }

   public <R> Array<R> apply(Class<R> resultOf, ILambda lambda) {
      if (resultOf == null) {
         resultOf = (Class<R>)this.ofClass();
      }

      R[] temp = (R[])java.lang.reflect.Array.newInstance(resultOf, this.size);

      for (int i = 0; i < this.size; i++) {
         temp[i] = (R)lambda.eval(this.array[i]);
      }

      return new Array<>(temp, this.size);
   }

   public Array<T> apply(ILambda lambda) {
      return this.apply(null, lambda);
   }

   public Array<T> sort(Comparator<? super T> comparator) {
      Array<T> result = this.copy();
      SortUtil.sort(result.array, result.array, comparator);
      return result;
   }

   public Array<T> sort() {
      Array<T> result = this.copy();
      SortUtil.sort(result.array, result.array);
      return result;
   }

   public Array<T> rsort() {
      Array<T> result = this.copy();
      SortUtil.rsort(result.array, result.array);
      return result;
   }

   public Array<T> reverse() {
      T[] temp = (T[])((Object[])java.lang.reflect.Array.newInstance(this.ofClass(), this.size));

      for (int i = 0; i < this.size; i++) {
         temp[i] = this.array[this.size - i - 1];
      }

      return new Array<>(temp, this.size);
   }

   class AsList extends AbstractList<T> {
      Array<T> arr;

      AsList(Array<T> arr) {
         this.arr = arr;
      }

      @Override
      public T get(int index) {
         return this.arr.get(index);
      }

      @Override
      public int size() {
         return this.arr.size();
      }

      @Override
      public T set(int index, T element) {
         return this.arr.set(index, element);
      }

      @Override
      public void add(int index, T element) {
         this.arr.add(index, element);
      }

      @Override
      public T remove(int index) {
         return this.arr.remove(index);
      }
   }
}
