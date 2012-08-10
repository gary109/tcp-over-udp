/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author David
 */
public final class TdpOutputStream extends OutputStream {
	
	private final TdpChannel channel;
	private ByteArrayOutputStream baos;

	TdpOutputStream(TdpChannel channel) {
		this.channel = channel;
		this.baos = new ByteArrayOutputStream();
	}

	@Override
	public synchronized void write(int b) throws IOException {
		baos.write(b);
		flushFullFrames();
	}
	
	@Override
	public synchronized void write(byte[] b) throws IOException {
		baos.write(b);
		flushFullFrames();
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		baos.write(b, off, len);
		flushFullFrames();
	}

	@Override
	public synchronized void flush() throws IOException {
		byte[] data = baos.toByteArray();
		baos = new ByteArrayOutputStream();
		channel.send(data);
	}

	@Override
	public synchronized void close() throws IOException {
		// nothing to do here
	}
	
	private void flushFullFrames() {
		byte[] data = baos.toByteArray();
		if (data.length < 490) {
			return;
		}
		int fCount = data.length / 490;
		byte[] flushData = new byte[fCount * 490];
		System.arraycopy(data, 0, flushData, 0, flushData.length);
		baos = new ByteArrayOutputStream();
		if (data.length - (fCount * 490) > 0) {
			baos.write(data, fCount * 490, data.length - (fCount * 490));
		}
	}

	
}
