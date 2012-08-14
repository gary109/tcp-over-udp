/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author David
 */
public final class TdpServerSocket {

	private final TdpServerChannel serverChannel;
	private final LinkedBlockingQueue<TdpChannel> pendingConnects = new LinkedBlockingQueue<TdpChannel>();
	private final HashMap<Long, TdpSocket> sockets = new HashMap<Long, TdpSocket>();

	public TdpServerSocket(int port) throws SocketException {
		serverChannel = new TdpServerChannel(port, new IServerChannelEventListener() {
			@Override
			public void onClientConnected(TdpServerChannel serverSocket, TdpChannel socket) {
				pendingConnects.add(socket);
			}

			@Override
			public void onDataReceived(TdpServerChannel serverSocket, TdpChannel socket, byte[] data) {
				TdpSocket sock = sockets.get(socket.getChannelId());
				if (sock != null) {
					sock.getInputStream().addData(data);
					if (sock.getEventListener() != null) {
						sock.getEventListener().onDataReceived(sock);
					}
				}
			}

			@Override
			public void onClientDisconnected(TdpServerChannel serverSocket, TdpChannel socket, EDisconnectReason reason) {
				sockets.remove(socket.getChannelId());
				TdpSocket sock = sockets.get(socket.getChannelId());
				if (sock != null) {
					if (sock.getEventListener() != null) {
						sock.getEventListener().onDisconnected(sock, reason);
					}
				}
			}

			@Override
			public void onServerSocketClosed(TdpServerChannel serverSocket) {
				sockets.clear();
			}
		});
	}

	public synchronized TdpSocket accept() {
		TdpChannel channel = pendingConnects.poll();
		TdpSocket socket = new TdpSocket(channel);
		sockets.put(channel.getChannelId(), socket);
		return socket;
	}

	public TdpSocket getSocket(long socketId) {
		return sockets.get(socketId);
	}
	
	public void close() {
		serverChannel.close();
		sockets.clear();
	}
}
