package fansirsqi.xposed.sesame.util;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * 源码来自：Apache Commons Collections 4.4
 * 少量定制
 */
public class CircularFifoQueue<E> extends AbstractCollection<E> implements Queue<E>, Serializable {
    /**
     * Serialization version.
     */
    @Serial
    private static final long serialVersionUID = -8423413834657610406L;
    /**
     * Underlying storage array.
     */
    private transient E[] elements;
    /**
     * Array index of first (oldest) queue element.
     */
    private transient int start = 0;
    /**
     * Index mod maxElements of the array position following the last queue
     * element.  Queue elements start at elements[start] and "wrap around"
     * elements[maxElements-1], ending at elements[decrement(end)].
     * For example, elements = {c,a,b}, start=1, end=1 corresponds to
     * the queue [a,b,c].
     */
    private transient int end = 0;
    /**
     * Flag to indicate if the queue is currently full.
     */
    private transient boolean full = false;
    /**
     * Capacity of the queue.
     */
    private final int maxElements;

    /**
     * Constructor that creates a queue with the specified size.
     *
     * @param size the size of the queue (cannot be changed)
     * @throws IllegalArgumentException if the size is &lt; 1
     */
    @SuppressWarnings("unchecked")
    public CircularFifoQueue(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        elements = (E[]) new Object[size];
        maxElements = elements.length;
    }

    //-----------------------------------------------------------------------

    /**
     * Write the queue out using a custom routine.
     *
     * @param out the output stream
     * @throws IOException if an I/O error occurs while writing to the output stream
     */
    @Serial
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size());
        for (final E e : this) {
            out.writeObject(e);
        }
    }

    /**
     * Read the queue in using a custom routine.
     *
     * @param in the input stream
     * @throws IOException            if an I/O error occurs while writing to the output stream
     * @throws ClassNotFoundException if the class of a serialized object can not be found
     */
    @Serial
    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        elements = (E[]) new Object[maxElements];
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            elements[i] = (E) in.readObject();
        }
        start = 0;
        full = size == maxElements;
        if (full) {
            end = 0;
        } else {
            end = size;
        }
    }
    //-----------------------------------------------------------------------

    /**
     * Returns the number of elements stored in the queue.
     *
     * @return this queue's size
     */
    @Override
    public int size() {
        int size;
        if (end < start) {
            size = maxElements - start + end;
        } else if (end == start) {
            size = full ? maxElements : 0;
        } else {
            size = end - start;
        }
        return size;
    }

    /**
     * Returns true if this queue is empty; false otherwise.
     *
     * @return true if this queue is empty
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A {@code CircularFifoQueue} can never be full, thus this returns always
     * {@code false}.
     *
     * @return always returns {@code false}
     */
    public boolean isFull() {
        return false;
    }

    /**
     * Returns {@code true} if the capacity limit of this queue has been reached,
     * i.e. the number of elements stored in the queue equals its maximum size.
     *
     * @return {@code true} if the capacity limit has been reached, {@code false} otherwise
     * @since 4.1
     */
    public boolean isAtFullCapacity() {
        return size() == maxElements;
    }

    /**
     * Clears this queue.
     */
    @Override
    public void clear() {
        full = false;
        start = 0;
        end = 0;
        Arrays.fill(elements, null);
    }

    public E push(final E element) {
        if (null == element) {
            throw new NullPointerException("Attempted to add null object to queue");
        }
        E oldElement;
        if (isAtFullCapacity()) {
            oldElement = remove();
        } else {
            oldElement = null;
        }
        elements[end++] = element;
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
        return oldElement;
    }

    /**
     * Adds the given element to this queue. If the queue is full, the least recently added
     * element is discarded so that a new element can be inserted.
     *
     * @param element the element to add
     * @return true, always
     * @throws NullPointerException if the given element is null
     */
    @Override
    public boolean add(final E element) {
        if (null == element) {
            throw new NullPointerException("Attempted to add null object to queue");
        }
        if (isAtFullCapacity()) {
            remove();
        }
        elements[end++] = element;
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
        return true;
    }

    /**
     * Returns the element at the specified position in this queue.
     *
     * @param index the position of the element in the queue
     * @return the element at position {@code index}
     * @throws NoSuchElementException if the requested position is outside the range [0, size)
     */
    public E get(final int index) {
        final int sz = size();
        if (index < 0 || index >= sz) {
            throw new NoSuchElementException(
                    String.format("The specified index (%1$d) is outside the available range [0, %2$d)",
                            index, sz));
        }
        final int idx = (start + index) % maxElements;
        return elements[idx];
    }
    //-----------------------------------------------------------------------

    /**
     * Adds the given element to this queue. If the queue is full, the least recently added
     * element is discarded so that a new element can be inserted.
     *
     * @param element the element to add
     * @return true, always
     * @throws NullPointerException if the given element is null
     */
    @Override
    public boolean offer(final E element) {
        return add(element);
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        return remove();
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new NoSuchElementException("queue is empty");
        }
        return peek();
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return elements[start];
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException("queue is empty");
        }
        final E element = elements[start];
        if (null != element) {
            elements[start++] = null;
            if (start >= maxElements) {
                start = 0;
            }
            full = false;
        }
        return element;
    }
    //-----------------------------------------------------------------------

    /**
     * Increments the internal index.
     *
     * @param index the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= maxElements) {
            index = 0;
        }
        return index;
    }

    /**
     * Decrements the internal index.
     *
     * @param index the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = maxElements - 1;
        }
        return index;
    }

    /**
     * Returns an iterator over this queue's elements.
     *
     * @return an iterator over this queue's elements
     */
    @NonNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private int index = start;
            private int lastReturnedIndex = -1;
            private boolean isFirst = full;

            @Override
            public boolean hasNext() {
                return isFirst || index != end;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                isFirst = false;
                lastReturnedIndex = index;
                index = increment(index);
                return elements[lastReturnedIndex];
            }

            @Override
            public void remove() {
                if (lastReturnedIndex == -1) {
                    throw new IllegalStateException();
                }
                // First element can be removed quickly
                if (lastReturnedIndex == start) {
                    CircularFifoQueue.this.remove();
                    lastReturnedIndex = -1;
                    return;
                }
                int pos = lastReturnedIndex + 1;
                if (start < lastReturnedIndex && pos < end) {
                    // shift in one part
                    System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos);
                } else {
                    // Other elements require us to shift the subsequent elements
                    while (pos != end) {
                        if (pos >= maxElements) {
                            elements[pos - 1] = elements[0];
                            pos = 0;
                        } else {
                            elements[decrement(pos)] = elements[pos];
                            pos = increment(pos);
                        }
                    }
                }
                lastReturnedIndex = -1;
                end = decrement(end);
                elements[end] = null;
                full = false;
                index = decrement(index);
            }
        };
    }
}