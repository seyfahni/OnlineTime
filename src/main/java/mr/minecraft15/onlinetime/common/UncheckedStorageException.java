/*
 * MIT License
 *
 * Copyright (c) 2019 Niklas Seyfarth
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package mr.minecraft15.onlinetime.common;

import mr.minecraft15.onlinetime.api.StorageException;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Objects;

public class UncheckedStorageException extends RuntimeException {

    private static final long serialVersionUID = 8740150807803129490L;

    /**
     * Constructs an instance of this class.
     *
     * @param cause the {@code StorageException}
     * @throws NullPointerException if the cause is {@code null}
     */
    public UncheckedStorageException(StorageException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message, can be null
     * @param cause the {@code StorageException}
     * @throws NullPointerException if the cause is {@code null}
     */
    public UncheckedStorageException(String message, StorageException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the {@code StorageException} which is the cause of this exception.
     */
    @Override
    public StorageException getCause() {
        return (StorageException) super.getCause();
    }

    /**
     * Called to read the object from a stream.
     *
     * @throws InvalidObjectException if the object is invalid or has a cause that is not an {@code StorageException}
     */
    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof StorageException)) {
            throw new InvalidObjectException("Cause must be an StorageException");
        }
    }
}
