/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

/**
 *
 * @author dipacs
 */
public final class TdpServerChannel {

    private final HashMap<Long, TdpChannel> sockets = new HashMap<Long, TdpChannel>();
    private final int port;
    private final DatagramSocket datagramSocket;
    private final IServerChannelEventListener serverListener;
    private TdpServerReceiverThread receiverThread;
    private TdpSenderThread senderThread;

    public TdpServerChannel(int port, IServerChannelEventListener serverListener) throws SocketException {
        this.port = port;
        this.datagramSocket = new DatagramSocket(port);
        this.datagramSocket.setSoTimeout(1000);
        this.datagramSocket.setTrafficClass(0x04 | 0x08 | 0x10);
        this.serverListener = serverListener;
        this.senderThread = new TdpSenderThread(datagramSocket, this, null);
        this.receiverThread = new TdpServerReceiverThread(this, senderThread);
        this.senderThread.start();
        this.receiverThread.start();
    }

    public int getPort() {
        return port;
    }

    DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    public synchronized void close() {
        if (this.receiverThread == null) {
            return;
        }
        if (this.receiverThread != null) {
            this.receiverThread.stopThread();
        }
		
		for (TdpChannel channel: sockets.values()) {
			channel.close();
		}
		
		if (this.senderThread != null) {
			this.senderThread.stopThread();
		}
		
        this.receiverThread = null;
		
		datagramSocket.close();
    }

    public TdpChannel getClient(long socketId) {
        return sockets.get(socketId);
    }

    synchronized void addClient(TdpChannel socket) {
        sockets.put(socket.getSocketId(), socket);
    }

    synchronized void removeClient(long id) {
        sockets.remove(id);
    }

    synchronized void removeClient(TdpChannel socket) {
        sockets.remove(socket.getSocketId());
    }

    public IServerChannelEventListener getServerListener() {
        return serverListener;
    }
}
