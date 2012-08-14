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
class TdpClientReceiverThread extends Thread {
    
    private final class DataHandler implements Runnable {

        private final byte[] data;
        private final int length;

        public DataHandler(byte[] data, int length) {
            this.data = data;
            this.length = length;
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
            if (socketId != socket.getSocketId()) {
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
        }

        private void processAck() {
            long socketId = readLong(data, 1);
            if (socketId != socket.getSocketId()) {
                return;
            }

            long messageId = readLong(data, 9);
            socket.acknowledgeMessage(messageId);
        }

        private void processKeepAlive() {
            // server does not send keep alive
        }

        private void processLogin() {
            // servere does notz login to client
        }

        private void processLoginResponse() {
            // login response is handled in the client constructor
        }

        private void processDisconnect() {
            long socketId = readLong(data, 1);
            if (socketId != socket.getSocketId()) {
                return;
            }

            socket.closeFromOtherSide();
            if (socket.getListener() != null) {
                socket.getListener().onClientDisconnected(socket, EDisconnectReason.SERVER);
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
    private final TdpChannel socket;
    private final TdpSenderThread senderThread;
	private volatile boolean stopped = false;

    public TdpClientReceiverThread(TdpChannel socket, TdpSenderThread senderThread) {
        super("TTdpClientReceiverThread");
        this.socket = socket;
        this.senderThread = senderThread;
    }

    @Override
    public void run() {
        DatagramPacket packet = null;
        DatagramSocket datagramSocket;
        try {
            while (!stopped && !isInterrupted()) {
                try {
                    datagramSocket = socket.getSocket();
                    if (datagramSocket == null || datagramSocket.isClosed()) {
                        break;
                    }
                    byte[] data = new byte[512];
                    packet = new DatagramPacket(data, data.length);
                    try {
                        datagramSocket.receive(packet);
                    } catch (SocketTimeoutException ex) {
                        continue;
                    }
                    if (packet.getLength() > 0) {
//                        TdpExecutor.execute(new DataHandler(packet.getData(), packet.getLength()));
                        new DataHandler(packet.getData(), packet.getLength()).run();
                    }
                } catch (IOException ex) {
					if (!stopped && !isInterrupted()) {
						Logger.getLogger(TdpServerReceiverThread.class.getName()).log(Level.SEVERE, null, ex);
					}
                    break;
                }
            }
        } finally {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ClientReceiverThread stopped.");
        }
    }
	
	public void stopThread() {
		this.stopped = true;
		this.interrupt();
	}
	
}
