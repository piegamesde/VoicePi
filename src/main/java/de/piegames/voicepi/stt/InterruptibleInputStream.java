package de.piegames.voicepi.stt;

import java.io.IOException;
import java.io.InputStream;

public class InterruptibleInputStream extends InputStream {

	protected final InputStream in;

	public InterruptibleInputStream(InputStream in) {
		this.in = in;
	}

	/** This will read one char, blocking if needed. If the thread is interrupted while reading, it will stop and throw an {@link IOException}. */
	@Override
	public int read() throws IOException {
		while (!Thread.interrupted())
			if (in.available() > 0)
				return in.read();
			else
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
		throw new IOException("Thread interrupted while reading");
	}

	/**
	 * This will read multiple chars into a buffer. While reading the first char it will block and wait in an interruptible way until one is available. For the
	 * remaining chars, it will stop reading when none are available anymore. If the thread is interrupted, it will return -1
	 */
	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}
		int c = -1;
		while (!Thread.interrupted())
			if (in.available() > 0) {
				c = in.read();
				break;
			} else
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
		if (c == -1) {
			return -1;
		}
		b[off] = (byte) c;

		int i = 1;
		try {
			for (; i < len; i++) {
				c = -1;
				if (in.available() > 0)
					c = in.read();
				if (c == -1) {
					break;
				}
				b[off + i] = (byte) c;
			}
		} catch (IOException ee) {
		}
		return i;
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
}