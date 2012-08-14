/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David
 */
class TdpServerReceiverThread extends Thread {

	private final class DataHandler implements Runnable {

		private final byte[] data;
		private final int length;
		private final SocketAddress remoteAddress;
		private final TdpSenderThread senderThread;

		public DataHandler(byte[] data, int length, SocketAddress remoteAddress, TdpSenderThread senderThread) {
			this.data = data;
			this.length = length;
			this.remoteAddress = remoteAddress;
			this.senderThread = senderThread;
		}

		@Override
		public void run() {
			if (data.length < 1) {
				return;
			}

			int header = data[0];
			switch (header) {
				case 0:
					processData();
					break;
				case 1:
					processAck();
					break;
				case 2:
					processKeepAlive();
					break;
				case 3:
					processLogin();
					break;
				case 4:
					processLoginResponse();
					break;
				case 5:
					processDisconnect();
					break;
				default:
					break;
			}
		}

		private void processData() {
			long socketId = readLong(data, 1);
			TdpChannel socket = serverSocket.getClient(socketId);
			if (socket == null) {
				return;
			}

			long messageId = readLong(data, 9);
			int dataLen = length - 17;
			byte[] pureData = new byte[dataLen];
			System.arraycopy(data, 17, pureData, 0, dataLen);
			try {
				socket.sendAck(messageId);
			} catch (IOException ex) {
				// nothing to do here
				return;
			}
			((TdpInputSorter) socket.getInputStream()).addFragment(new Fragment(messageId, pureData));
			socket.touch();
		}

		private void processAck() {
			long socketId = readLong(data, 1);
			TdpChannel socket = serverSocket.getClient(socketId);
			if (socket == null) {
				return;
			}

			long messageId = readLong(data, 9);
			socket.acknowledgeMessage(messageId);
			socket.touch();
		}

		private void processKeepAlive() {
			long socketId = readLong(data, 1);
			TdpChannel socket = serverSocket.getClient(socketId);
			if (socket == null) {
				return;
			}

			socket.touch();
		}

		private void processLogin() {
			TdpChannel socket = new TdpChannel(serverSocket.getDatagramSocket(), remoteAddress, serverSocket, senderThread, serverSocket.getServerListener());
			socket.setSocketId(TdpChannel.getNextId());
			try {
				socket.sendLoginResponse();
				socket.sendLoginResponse();
				socket.sendLoginResponse();
			} catch (IOException ex) {
				// nothing to do here
				return;
			}
			serverSocket.addClient(socket);

			if (serverSocket.getServerListener() != null) {
				serverSocket.getServerListener().onClientConnected(serverSocket, socket);
			}
		}

		private void processLoginResponse() {
			// server can't get login response. Simple ignore it
		}

		private void processDisconnect() {
			long socketId = readLong(data, 1);
			TdpChannel socket = serverSocket.getClient(socketId);
			if (socket == null) {
				return;
			}

			socket.closeFromOtherSide();

			if (serverSocket.getServerListener() != null) {
				serverSocket.getServerListener().onClientDisconnected(serverSocket, socket, EDisconnectReason.CLIENT);
			}
		}

		private long readLong(byte[] data, int offset) {
			long res = data[offset] & 0xff;
			res |= (data[offset + 1] & 0xffl) << 8;
			res |= (data[offset + 2] & 0xffl) << 16;
			res |= (data[offset + 3] & 0xffl) << 24;
			res |= (data[offset + 4] & 0xffl) << 32;
			res |= (data[offset + 5] & 0xffl) << 40;
			res |= (data[offset + 6] & 0xffl) << 48;
			res |= (data[offset + 7] & 0xffl) << 56;
			return res;
		}
	}
	private final TdpServerChannel serverSocket;
	private final TdpSenderThread senderThread;
	private volatile boolean stopped = false;
	private long lastCleanStamp = System.currentTimeMillis();

	public TdpServerReceiverThread(TdpServerChannel serverSocket, TdpSenderThread senderThread) {
		super("TTdpServerReceiverThread");
		this.serverSocket = serverSocket;
		this.senderThread = senderThread;
	}

	@Override
	public void run() {
		DatagramPacket packet = null;
		DatagramSocket datagramSocket;
		try {
			while (!stopped && !isInterrupted()) {
				try {
					datagramSocket = serverSocket.getDatagramSocket();
					if (datagramSocket == null || datagramSocket.isClosed()) {
						break;
					}
					byte[] data = new byte[512];
					packet = new DatagramPacket(data, data.length);
					try {
						datagramSocket.receive(packet);
					} catch (SocketTimeoutException ex) {
						// clean old clients
						if (lastCleanStamp < System.currentTimeMillis() - 10000) {
							serverSocket.cleanTimedOutChannels();
							lastCleanStamp = System.currentTimeMillis();
						}
						continue;
					}
					if (packet.getLength() > 0) {
						serverSocket.getExecutor().execute(new DataHandler(packet.getData(), packet.getLength(), packet.getSocketAddress(), senderThread));
						//TdpExecutor.execute(new DataHandler(packet.getData(), packet.getLength(), packet.getSocketAddress(), senderThread));
//                        new DataHandler(packet.getData(), packet.getLength(), packet.getSocketAddress(), senderThread).run();
					}
				} catch (IOException ex) {
					if (!stopped && !isInterrupted()) {
						Logger.getLogger(TdpServerReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
					}
					break;
				}
			}
		} finally {
			serverSocket.close();
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ServerReceiverThread stopped.");
		}
	}

	public void stopThread() {
		this.stopped = true;
		this.interrupt();
	}
}
