/**
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royaltee free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package com.noelios.restlet.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream to write data in the HTTP chunked encoding format to a
 * destination OutputStream.<br>
 * <br>
 * See section 3.6.1 of HTTP Protocol for more information on chunked encoding.
 * 
 * @author <a href="mailto:kevin.a.conaway@gmail.com">Kevin Conaway</a>
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html">HTTP/1.1
 *      Protocol< /a>
 */
public class ChunkedOutputStream extends OutputStream {

    private static final int DEFAULT_CHUNK_SIZE = 2048;

    /** The byte buffer. */
    private final byte[] buffer;

    /** The number of bytes written. */
    private volatile int bytesWritten;

    /** Indicate if the stream is closed. */
    private volatile boolean closed;

    /** The destination output stream. */
    private final OutputStream destination;

    /**
     * Convenience constructor to use a default chunk size size of 2048.
     * 
     * @param destination
     * @see #ChunkedOutputStream(OutputStream, int)
     */
    public ChunkedOutputStream(OutputStream destination) {
        this(destination, DEFAULT_CHUNK_SIZE);
    }

    /**
     * @param destination
     *            Outputstream to write chunked data to
     * @param chunkSize
     *            Chunk size
     */
    public ChunkedOutputStream(OutputStream destination, int chunkSize) {
        this.destination = destination;
        this.buffer = new byte[chunkSize];
        this.bytesWritten = 0;
        this.closed = false;
    }

    /**
     * @return True if the current chunk is full.
     */
    private boolean chunkFull() {
        return this.bytesWritten == this.buffer.length;
    }

    /**
     * Closes this output stream for writing but does not close the wrapped.
     * stream
     */
    @Override
    public void close() throws IOException {
        if (!this.closed) {
            writeChunk();
            writeFinalChunk();
            super.close();
            this.closed = true;
            this.destination.flush();
        }
    }

    /**
     * Writes the current chunk and flushes the wrapped stream.
     */
    @Override
    public void flush() throws IOException {
        writeChunk();
        this.destination.flush();
    }

    /**
     * Reset the internal buffer.
     */
    private void reset() {
        this.bytesWritten = 0;
    }

    @Override
    public void write(int b) throws IOException {
        if (chunkFull()) {
            writeChunk();
        }
        this.buffer[this.bytesWritten++] = (byte) b;
    }

    /**
     * Write the buffer contents.
     * 
     * @throws IOException
     */
    private void writeBuffer() throws IOException {
        this.destination.write(this.buffer, 0, this.bytesWritten);
        HttpUtils.writeCRLF(this.destination);
    }

    /**
     * Write a chunk.
     * 
     * @throws IOException
     */
    private void writeChunk() throws IOException {
        if (this.bytesWritten > 0) {
            writePosition();
            writeBuffer();
            reset();
        }
    }

    /**
     * Write the closing chunk: A zero followed by crlf and another crlf.
     * 
     * @throws IOException
     */
    private void writeFinalChunk() throws IOException {
        this.destination.write((byte) '0');
        HttpUtils.writeCRLF(this.destination);
        HttpUtils.writeCRLF(this.destination);
    }

    /**
     * Write the current position in hexadecimal format followed by CRLF.
     * 
     * @throws IOException
     */
    private void writePosition() throws IOException {
        this.destination.write(Integer.toHexString(this.bytesWritten)
                .getBytes());
        HttpUtils.writeCRLF(this.destination);
    }
}
