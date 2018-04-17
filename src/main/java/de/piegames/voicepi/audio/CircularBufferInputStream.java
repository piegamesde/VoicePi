package de.piegames.voicepi.audio;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.greenrobot.essentials.io.CircularByteBuffer;

public class CircularBufferInputStream extends InputStream {

	protected CircularByteBuffer buffer;

	public CircularBufferInputStream(CircularByteBuffer buffer) {
		this.buffer = Objects.requireNonNull(buffer);
	}

	@Override
	public int read() throws IOException {
		if (buffer == null)
			throw new IOException("Stream closed");
		return buffer.get();
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		if (buffer == null)
			throw new IOException("Stream closed");
		return buffer.get(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (buffer == null)
			throw new IOException("Stream closed");
		buffer.free();
		buffer = null;
	}

	/** Returns the {@link CircularByteBuffer} backing this input stream or {@code null} if the stream has been closed */
	public CircularByteBuffer getBuffer() {
		return buffer;
	}
}
