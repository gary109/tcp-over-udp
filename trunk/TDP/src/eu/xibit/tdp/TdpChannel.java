/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class can be used to estabilish a connaction with a server. Using this class you can read the data throught events.
 * If you want to read and write data throught InputStream and OutputStream, you need to use the TdpSocket.
 * 
 * Also this class is created on the server side, when a client is connected to a TdpServerSocket.
 * 
 * @author dipacs
 */
public final class TdpChannel {

    private static long nextSocketId = 1;

    public static synchronized long getNextId() {
        return nextSocketId++;
    }
    private final DatagramSocket datagramSocket;
    private final SocketAddress remoteAddress;
    private final TdpServerChannel serverChannel;
    private final String host;
    private final int port;
    private long channelId = -1;
    private final TdpInputSorter inputStream;
    private final TdpSenderThread senderThread;
    private TdpClientReceiverThread clientReceiverThread;
    private long nextMessageId = 0;
    private long lastKeepAlive = System.currentTimeMillis();
    private volatile IClientChannelEventListener listener;
    private final IServerChannelEventListener serverListener;
	private boolean serverSide;

    public TdpChannel(String host, int port, IClientChannelEventListener listener) throws SocketException, IOException {
        this(new DatagramSocket(), new InetSocketAddress(host, port), null, null, null);
		this.serverSide = false;
        this.datagramSocket.setSoTimeout(200);
        this.datagramSocket.setTrafficClass(0x04 | 0x08 | 0x10);

        // trying to login
        byte[] data = new byte[]{3};
        DatagramPacket loginPacket = new DatagramPacket(data, data.length, new InetSocketAddress(host, port));
        byte[] inData = new byte[512];
        DatagramPacket inPacket = new DatagramPacket(inData, inData.length);
        for (int i = 0; i < 10; i++) {
            this.datagramSocket.send(loginPacket);
            try {
                this.datagramSocket.receive(inPacket);
            } catch (SocketTimeoutException ex) {
                continue;
            }
            if (inPacket.getLength() != 9) {
                throw new IOException("Protocol error. Invalid response length.");
            }

            byte[] lrData = inPacket.getData();
            if (lrData[0] != 4) {
                throw new IOException("Protocol error. Invalid response header.");
            }
            
            long sockId = readLong(lrData, 1);
            if (sockId == -1) {
                throw new IOException("Server does not want you to connect.");
            }
            
            this.channelId = sockId;
            break;
        }
        
        if (this.channelId == -1) {
            throw new IOException("Can't connect to server.");
        }
        
        this.listener = listener;

        this.clientReceiverThread = new TdpClientReceiverThread(this, senderThread);
        this.senderThread.start();
        this.clientReceiverThread.start();
    }

    TdpChannel(DatagramSocket datagramSocket, SocketAddress remoteAddress, TdpServerChannel serverChannel, TdpSenderThread senderThread, IServerChannelEventListener listener) {
		this.serverSide = true;
        this.datagramSocket = datagramSocket;
        this.remoteAddress = remoteAddress;
        this.serverChannel = serverChannel;
        this.inputStream = new TdpInputSorter(this);
        if (remoteAddress instanceof InetSocketAddress) {
            this.host = ((InetSocketAddress) remoteAddress).getHostName();
            this.port = ((InetSocketAddress) remoteAddress).getPort();
        } else {
            this.host = null;
            this.port = 0;
        }
        if (senderThread == null) {
            this.senderThread = new TdpSenderThread(datagramSocket, null, this);
        } else {
            this.senderThread = senderThread;
        }
        serverListener = listener;
    }

    DatagramSocket getDatagramSocket() {
        return datagramSocket;
    }

    SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    TdpInputSorter getInputStream() {
        return inputStream;
    }

    public long getChannelId() {
        return channelId;
    }

    void setChannelId(long channelId) {
        this.channelId = channelId;
    }

    void sendData(byte[] data) throws IOException {
        byte[] outData = new byte[data.length + 17];
        System.arraycopy(data, 0, outData, 17, data.length);

        // protocol command
        outData[0] = 0;

        // socket id
        insertLong(outData, channelId, 1);

        // message id
        long messageId = getNextMessageId();
        insertLong(outData, messageId, 9);

        senderThread.addDataPacket(new DatagramPacket(outData, outData.length, remoteAddress), messageId, channelId);
    }

    void sendAck(long msgId) throws IOException {
        byte[] outData = new byte[17];

        // protocol command
        outData[0] = 1;

        // socket id
        insertLong(outData, channelId, 1);

        // message id
        long messageId = msgId;
        insertLong(outData, messageId, 9);
        datagramSocket.send(new DatagramPacket(outData, outData.length, remoteAddress));
        //senderThread.addAckPacket(new DatagramPacket(outData, outData.length, remoteAddress));
    }

    void sendKeepAlive() throws IOException {
        byte[] outData = new byte[9];

        // protocol command
        outData[0] = 2;

        // socket id
        insertLong(outData, channelId, 1);

        senderThread.addSimplePacket(new DatagramPacket(outData, outData.length, remoteAddress));
    }

    void sendLogin() throws IOException {
        byte[] outData = new byte[1];

        // protocol command
        outData[0] = 3;

        senderThread.addSimplePacket(new DatagramPacket(outData, outData.length, remoteAddress));
        senderThread.addSimplePacket(new DatagramPacket(outData, outData.length, remoteAddress));
        senderThread.addSimplePacket(new DatagramPacket(outData, outData.length, remoteAddress));
    }

    void sendLoginResponse() throws IOException {
        byte[] outData = new byte[9];

        // protocol command
        outData[0] = 4;

        // socket id
        insertLong(outData, channelId, 1);

        datagramSocket.send(new DatagramPacket(outData, outData.length, remoteAddress));
        datagramSocket.send(new DatagramPacket(outData, outData.length, remoteAddress));
        datagramSocket.send(new DatagramPacket(outData, outData.length, remoteAddress));
//        senderThread.addSimplePacket(new DatagramPacket(outData, outData.length, remoteAddress));
    }

    void sendDisconnect() throws IOException {
        byte[] outData = new byte[1];

        // protocol command
        outData[0] = 5;

        datagramSocket.send(new DatagramPacket(outData, outData.length, remoteAddress));
    }

    void acknowledgeMessage(long messageId) {
        senderThread.acknowledgePacket(messageId, channelId);
    }

    private void insertLong(byte[] data, long value, int pos) {
        data[pos] = (byte) (value & 0xff);
        data[pos + 1] = (byte) ((value >>> 8) & 0xff);
        data[pos + 2] = (byte) ((value >>> 16) & 0xff);
        data[pos + 3] = (byte) ((value >>> 24) & 0xff);
        data[pos + 4] = (byte) ((value >>> 32) & 0xff);
        data[pos + 5] = (byte) ((value >>> 40) & 0xff);
        data[pos + 6] = (byte) ((value >>> 48) & 0xff);
        data[pos + 7] = (byte) ((value >>> 56) & 0xff);
    }

    private synchronized long getNextMessageId() {
        return nextMessageId++;
    }

    long getLastKeepAlive() {
        return lastKeepAlive;
    }

    void setLastKeepAlive(long lastKeepAlive) {
        this.lastKeepAlive = lastKeepAlive;
    }

    void touch() {
        setLastKeepAlive(System.currentTimeMillis());
    }

    TdpServerChannel getServerChannel() {
        return serverChannel;
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

    public IClientChannelEventListener getListener() {
        return listener;
    }
	
	public void close() {
		closeFromReceiver();
		if (serverChannel == null) {
            if (listener != null) {
				listener.onClientDisconnected(this, EDisconnectReason.CLIENT);
			}
        } else {
            if (serverChannel.getServerListener() != null) {
				serverChannel.getServerListener().onClientDisconnected(serverChannel, this, EDisconnectReason.SERVER);
			}
        }
	}
    
    void closeFromReceiver() {
        try {
            sendDisconnect();
            sendDisconnect();
            sendDisconnect();
        } catch (IOException ex) {
            Logger.getLogger(TdpChannel.class.getName()).log(Level.SEVERE, null, ex);
        }
        closeFromOtherSide();
    }

    IServerChannelEventListener getServerListener() {
        return serverListener;
    }
    
    public synchronized void send(byte[] data) throws IOException {
        if (data.length < 1) {
            return;
        } 
        ArrayList<byte[]> packetDatas = new ArrayList<byte[]>();
        while (data.length > 490) {
            byte[] newData = new byte[data.length - 490];
            byte[] pData = new byte[490];
            System.arraycopy(data, 0, pData, 0, 490);
            System.arraycopy(data, 490, newData, 0, newData.length);
            data = newData;
            packetDatas.add(pData);
        }
        packetDatas.add(data);
        
        for (byte[] b : packetDatas) {
            sendData(b);
        }
    }
    
    void closeFromOtherSide() {
        if (serverChannel == null) {
            // clientSide
            clientReceiverThread.stopThread();
            
            if (senderThread != null) {
                senderThread.stopThread();
            }
			
			datagramSocket.close();
        } else {
            // serverSide
            serverChannel.removeClient(this);
        }
    }

	public boolean isServerSide() {
		return serverSide;
	}
}
