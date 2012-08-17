/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author dipacs
 */
public final class TdpServerChannel {

    private final HashMap<Long, TdpChannel> channels = new HashMap<Long, TdpChannel>();
    private final int port;
    private final DatagramSocket datagramSocket;
    private final IServerChannelEventListener serverListener;
    private TdpServerReceiverThread receiverThread;
    private TdpSenderThread senderThread;
    private final TdpExecutor executor = new TdpExecutor();

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

        for (TdpChannel channel : channels.values()) {
            channel.close();
        }

        if (this.senderThread != null) {
            this.senderThread.stopThread();
        }

        this.receiverThread = null;

        this.executor.stop();

        datagramSocket.close();
    }

    public TdpChannel getClient(long socketId) {
        return channels.get(socketId);
    }

    synchronized void addClient(TdpChannel socket) {
        channels.put(socket.getChannelId(), socket);
    }

    synchronized void removeClient(long id) {
        channels.remove(id);
    }

    synchronized void removeClient(TdpChannel socket) {
        channels.remove(socket.getChannelId());
    }

    public IServerChannelEventListener getServerListener() {
        return serverListener;
    }

    TdpExecutor getExecutor() {
        return executor;
    }

    void cleanTimedOutChannels() {
        long ss = System.currentTimeMillis();
        ArrayList<TdpChannel> closeChannels = new ArrayList<TdpChannel>();

        for (TdpChannel c : channels.values()) {
            if (c.getLastKeepAlive() < ss - 25000) {
                closeChannels.add(c);
            }
        }

        for (TdpChannel c : closeChannels) {
            c.close();
        }
    }
}
