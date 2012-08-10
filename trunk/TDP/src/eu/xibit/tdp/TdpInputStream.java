/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 *
 * @author David
 */
public final class TdpInputStream extends InputStream {

	private final LinkedList<byte[]> data = new LinkedList<byte[]>();
	private int available = 0;
	private int fragmentOffset = 0;

	TdpInputStream() {
	}

	@Override
	public int read() throws IOException {
		while (true) {
			if (available > 0) {
				synchronized (this) {
					if (available > 0) {
						int res = data.peekFirst()[fragmentOffset];
						fragmentOffset++;
						available--;
						dropFragmentIfNeeded();
						return res;
					}
				}
			}
			Thread.yield();
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len < 1) {
			return 0;
		}
		while (true) {
			if (available > 0) {
				synchronized (this) {
					if (available > 0) {
						int readed = 0;
						int readLen = Math.min(len, available);
						while (readed < readLen) {
							byte[] f = data.peekFirst();
							int count = Math.min(f.length - fragmentOffset, len - readed);
							System.arraycopy(f, fragmentOffset, b, off + readed, count);
							readed += count;
							fragmentOffset += count;
							available -= count;
							dropFragmentIfNeeded();
						}
						return readed;
					}
				}
			}
			Thread.yield();
		}
	}

	@Override
	public synchronized long skip(long n) throws IOException {
		if (n < 1) {
			return 0;
		}
		synchronized (this) {
			if (available > 0) {
				long readed = 0;
				long readLen = Math.min(n, available);
				while (readed < readLen) {
					byte[] f = data.peekFirst();
					long count = Math.min(f.length - fragmentOffset, (int)(n - readed));
					readed += count;
					fragmentOffset += count;
					available -= count;
					dropFragmentIfNeeded();
				}
				return readed;
			}
			return 0;
		}
	}

	private synchronized void dropFragmentIfNeeded() {
		byte[] d = data.peekFirst();
		if (d == null) {
			return;
		}
		if (fragmentOffset >= d.length) {
			data.pollFirst();
			fragmentOffset = 0;
		}
	}

	@Override
	public int available() throws IOException {
		return this.available;
	}

	@Override
	public void close() throws IOException {
		// nothing to do here
	}

	@Override
	public synchronized void mark(int readlimit) {
		throw new UnsupportedOperationException("This operation isn't supported in TdpInputStream.");
	}

	@Override
	public synchronized void reset() throws IOException {
		throw new UnsupportedOperationException("This operation isn't supported in TdpInputStream.");
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	synchronized void addData(byte[] d) {
		if (d.length < 1) {
			return;
		}

		data.add(d);
		available += d.length;
	}
}
