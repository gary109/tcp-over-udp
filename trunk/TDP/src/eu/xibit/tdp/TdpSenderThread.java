/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David
 */
class TdpSenderThread extends Thread {

    private long resendedPackets = 1;
    private final Object dataSync = new Object();
    private final Object otherSync = new Object();

    private static class TdpPacket {

        private final DatagramPacket packet;
        private final long messageId;
        private final long socketId;
        private volatile long lastSent = 0;
        private volatile long resendCount = 0;

        public TdpPacket(DatagramPacket packet, long socketId, long messageId) {
            this.packet = packet;
            this.messageId = messageId;
            this.socketId = socketId;
        }

        public DatagramPacket getPacket() {
            return packet;
        }

        public long getLastSent() {
            return lastSent;
        }

        public void setLastSent(long lastSent) {
            this.lastSent = lastSent;
        }

        public long getResendCount() {
            return resendCount;
        }

        public void setResendCount(long resendCount) {
            this.resendCount = resendCount;
        }

        public long getMessageId() {
            return messageId;
        }

        public long getSocketId() {
            return socketId;
        }
    }
	private static final long KEEP_ALIVE_INTERVAL = 10000;
    private final LinkedList<TdpPacket> packets = new LinkedList<TdpPacket>();
    private final LinkedList<DatagramPacket> otherPackets = new LinkedList<DatagramPacket>();
    private final DatagramSocket datagramSocket;
    private final TdpServerChannel serverSocket;
    private final TdpChannel socket;
	private volatile boolean stopped = false;
	private long lastSendTime = 0;

    public TdpSenderThread(DatagramSocket datagramSocket, TdpServerChannel serverSocket, TdpChannel socket) {
        super("TdpSenderThread");
        this.datagramSocket = datagramSocket;
        this.serverSocket = serverSocket;
        this.socket = socket;
    }

    @Override
    public void run() {
        DatagramPacket packet = null;
        
        long actStamp = 0;
        while (!stopped && !isInterrupted()) {
            actStamp = System.currentTimeMillis();
//            try {
//                packet = pollOtherPacket();
//                if (packet != null) {
//                    datagramSocket.send(packet);
//                    // sending all the ack packets first
//                    continue;
//                }
//            } catch (IOException ex) {
//                Logger.getLogger(TdpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (Throwable ex) {
//                Logger.getLogger(TdpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
//            }

            try {
                synchronized (dataSync) {
                    Iterator<TdpPacket> iterator = packets.iterator();
                    while (iterator.hasNext()) {
                        final TdpPacket tdpPacket = iterator.next();
                        if (tdpPacket != null) {
                            if (tdpPacket.getResendCount() > 10) {
                                if (serverSocket != null) {
                                    TdpChannel s = serverSocket.getClient(tdpPacket.getSocketId());
                                    if (s != null) {
                                        s.closeFromReceiver();
                                    }
                                    serverSocket.getServerListener().onClientDisconnected(serverSocket, s, EDisconnectReason.TIMED_OUT);
                                }
                                if (socket != null) {
                                    socket.closeFromReceiver();
                                    if (socket.getListener() != null) {
                                        socket.getListener().onClientDisconnected(socket, EDisconnectReason.TIMED_OUT);
                                    }
                                }
                                iterator.remove();
                                System.out.println("Closed by resend count");
                                continue;
                            }
                            if (tdpPacket.getLastSent() < actStamp - 500) {
                                tdpPacket.setResendCount(tdpPacket.getResendCount() + 1);
                                datagramSocket.send(tdpPacket.getPacket());
                                tdpPacket.setLastSent(System.currentTimeMillis());
                                continue;
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(TdpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
                pollTdpPacket();
            } catch (Throwable ex) {
                Logger.getLogger(TdpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
                pollTdpPacket();
            }
			
			// sending keep alive if needed
			if (serverSocket == null) {
				if (lastSendTime < System.currentTimeMillis() - KEEP_ALIVE_INTERVAL) {
					touch();
					try {
						sendKeepAlive();
					} catch (IOException ex) {
						// TODO ???
					}
				}
			}
			
            try {
                // nothing to send, wait a bit
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                return;
            }
        }
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "SenderThread stopped.");
    }
	
	public synchronized void touch() {
		this.lastSendTime = System.currentTimeMillis();
	}

    public void addDataPacket(DatagramPacket packet, long messageId, long socketId) {
        while (packets.size() > 1000) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(TdpSenderThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        synchronized (dataSync) {
            TdpPacket p = new TdpPacket(packet, socketId, messageId);
            packets.addLast(p);
        }
    }

    public void addSimplePacket(DatagramPacket packet) {
        synchronized (otherSync) {
            otherPackets.addLast(packet);
        }
    }

    public void addAckPacket(DatagramPacket packet) {
        synchronized (otherSync) {
            otherPackets.addFirst(packet);
        }
    }

    public void acknowledgePacket(long messageId, long socketId) {
        synchronized (dataSync) {
            Iterator<TdpPacket> iterator = packets.iterator();
            TdpPacket packet = null;
            while (iterator.hasNext()) {
                packet = iterator.next();
                if (packet.getMessageId() == messageId && packet.getSocketId() == socketId) {
                    iterator.remove();
                    return;
                }
            }
        }
    }

    private TdpPacket peekTdpPacket() {
        synchronized (dataSync) {
            return packets.peekFirst();
        }
    }

    private synchronized TdpPacket pollTdpPacket() {
        synchronized (dataSync) {
            return packets.pollFirst();
        }
    }

    private synchronized DatagramPacket pollOtherPacket() {
        synchronized (otherSync) {
            return otherPackets.pollFirst();
        }
    }
	
	public void stopThread() {
		this.stopped = true;
		this.interrupt();
	}
	
	void sendKeepAlive() throws IOException {
        byte[] outData = new byte[9];

        // protocol command
        outData[0] = 2;

        // socket id
        insertLong(outData, socket.getSocketId(), 1);

		DatagramPacket dp = new DatagramPacket(outData, outData.length, socket.getRemoteAddress());
		datagramSocket.send(dp);
		datagramSocket.send(dp);
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
}
