/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;

/**
 *
 * @author David
 */
public final class TdpSocket {
	
	private final TdpChannel channel;
	private volatile ISocketEventListener eventListener;
	private final TdpInputStream inputStream;
	private final TdpOutputStream outputStream;
	
	public TdpSocket(String host, int port, ISocketEventListener eventListener) throws IOException {
		this.channel = initChannel(host, port);
		this.eventListener = eventListener;
		this.inputStream = new TdpInputStream();
		this.outputStream = new TdpOutputStream(channel);
	}

	TdpSocket(TdpChannel channel) {
		this.channel = channel;
		this.inputStream = new TdpInputStream();
		this.outputStream = new TdpOutputStream(channel);
	}
	
	private TdpChannel initChannel(String host, int port) throws IOException {
		return new TdpChannel(host, port, new IClientChannelEventListener() {

			@Override
			public void onDataReceived(TdpChannel socket, byte[] data) {
				inputStream.addData(data);
				if (TdpSocket.this.eventListener != null) {
					TdpSocket.this.eventListener.onDataReceived(TdpSocket.this);
				}
			}

			@Override
			public void onClientDisconnected(TdpChannel socket, EDisconnectReason reason) {
				if (TdpSocket.this.eventListener != null) {
					TdpSocket.this.eventListener.onDisconnected(TdpSocket.this, reason);
				}
			}
		});
	}

	public ISocketEventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(ISocketEventListener eventListener) {
		this.eventListener = eventListener;
	}

	public TdpInputStream getInputStream() {
		return inputStream;
	}

	public TdpOutputStream getOutputStream() {
		return outputStream;
	}

	public String getHost() {
		return channel.getHost();
	}

	public int getPort() {
		return channel.getPort();
	}

	public long getSocketId() {
		return channel.getChannelId();
	}

	public void close() {
		channel.close();
	}
	
}
