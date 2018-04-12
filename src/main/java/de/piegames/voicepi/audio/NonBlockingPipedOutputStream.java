package de.piegames.voicepi.audio;
/* Copyright (c) 1995, 2006, Oracle and/or its affiliates. All rights reserved. DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License version 2 only, as published by the
 * Free Software Foundation. Oracle designates this particular file as subject to the "Classpath" exception as provided by Oracle in the LICENSE file that
 * accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License version 2 for more details (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version 2 along with this work; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA or visit www.oracle.com if you need additional information or have any questions. */

import java.io.IOException;
import java.io.OutputStream;

/**
 * A NonBlockingPiped output stream can be connected to a NonBlockingPiped input stream to create a communications pipe. The NonBlockingPiped output stream is
 * the sending end of the pipe. Typically, data is written to a <code>NonBlockingPipedOutputStream</code> object by one thread and data is read from the
 * connected <code>NonBlockingPipedInputStream</code> by some other thread. Attempting to use both objects from a single thread is not recommended as it may
 * deadlock the thread. The pipe is said to be <a name=BROKEN> <i>broken</i> </a> if a thread that was reading data bytes from the connected NonBlockingPiped
 * input stream is no longer alive.
 *
 * @author James Gosling
 * @see java.io.NonBlockingPipedInputStream
 * @since JDK1.0
 */
public class NonBlockingPipedOutputStream extends OutputStream {

	/* REMIND: identification of the read and write sides needs to be more sophisticated. Either using thread groups (but what about pipes within a thread?) or
	 * using finalization (but it may be a long time until the next GC). */
	private NonBlockingPipedInputStream sink;

	/**
	 * Creates a NonBlockingPiped output stream connected to the specified NonBlockingPiped input stream. Data bytes written to this stream will then be
	 * available as input from <code>snk</code>.
	 *
	 * @param snk The NonBlockingPiped input stream to connect to.
	 * @exception IOException if an I/O error occurs.
	 */
	public NonBlockingPipedOutputStream(NonBlockingPipedInputStream snk) throws IOException {
		connect(snk);
	}

	/**
	 * Creates a NonBlockingPiped output stream that is not yet connected to a NonBlockingPiped input stream. It must be connected to a NonBlockingPiped input
	 * stream, either by the receiver or the sender, before being used.
	 *
	 * @see java.io.NonBlockingPipedInputStream#connect(java.io.NonBlockingPipedOutputStream)
	 * @see java.io.NonBlockingPipedOutputStream#connect(java.io.NonBlockingPipedInputStream)
	 */
	public NonBlockingPipedOutputStream() {
	}

	/**
	 * Connects this NonBlockingPiped output stream to a receiver. If this object is already connected to some other NonBlockingPiped input stream, an
	 * <code>IOException</code> is thrown.
	 * <p>
	 * If <code>snk</code> is an unconnected NonBlockingPiped input stream and <code>src</code> is an unconnected NonBlockingPiped output stream, they may be
	 * connected by either the call: <blockquote>
	 *
	 * <pre>
	 * src.connect(snk)
	 * </pre>
	 *
	 * </blockquote> or the call: <blockquote>
	 *
	 * <pre>
	 * snk.connect(src)
	 * </pre>
	 *
	 * </blockquote> The two calls have the same effect.
	 *
	 * @param snk the NonBlockingPiped input stream to connect to.
	 * @exception IOException if an I/O error occurs.
	 */
	public synchronized void connect(NonBlockingPipedInputStream snk) throws IOException {
		if (snk == null) {
			throw new NullPointerException();
		} else if (sink != null || snk.connected) {
			throw new IOException("Already connected");
		}
		sink = snk;
		snk.in = -1;
		snk.out = 0;
		snk.connected = true;
	}

	/**
	 * Writes the specified <code>byte</code> to the NonBlockingPiped output stream.
	 * <p>
	 * Implements the <code>write</code> method of <code>OutputStream</code>.
	 *
	 * @param b the <code>byte</code> to be written.
	 * @exception IOException if the pipe is <a href=#BROKEN> broken</a>, {@link #connect(java.io.NonBlockingPipedInputStream) unconnected}, closed, or if an
	 *                I/O error occurs.
	 */
	@Override
	public void write(int b) throws IOException {
		if (sink == null) {
			throw new IOException("Pipe not connected");
		}
		sink.receive(b);
	}

	/**
	 * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to this NonBlockingPiped output stream. This method
	 * blocks until all the bytes are written to the output stream.
	 *
	 * @param b the data.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 * @exception IOException if the pipe is <a href=#BROKEN> broken</a>, {@link #connect(java.io.NonBlockingPipedInputStream) unconnected}, closed, or if an
	 *                I/O error occurs.
	 */
	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if (sink == null) {
			throw new IOException("Pipe not connected");
		} else if (b == null) {
			throw new NullPointerException();
		} else if ((off < 0) || (off > b.length) || (len < 0) ||
				((off + len) > b.length) || ((off + len) < 0)) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		sink.receive(b, off, len);
	}

	/**
	 * Flushes this output stream and forces any buffered output bytes to be written out. This will notify any readers that bytes are waiting in the pipe.
	 *
	 * @exception IOException if an I/O error occurs.
	 */
	@Override
	public synchronized void flush() throws IOException {
		if (sink != null) {
			synchronized (sink) {
				sink.notifyAll();
			}
		}
	}

	/**
	 * Closes this NonBlockingPiped output stream and releases any system resources associated with this stream. This stream may no longer be used for writing
	 * bytes.
	 *
	 * @exception IOException if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		if (sink != null) {
			sink.receivedLast();
		}
	}
}
