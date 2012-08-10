/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

/**
 *
 * @author dipacs
 */
public interface IServerChannelEventListener {
    
    public void onClientConnected(TdpServerSocket serverSocket, TdpChannel socket);
    public void onDataReceived(TdpServerSocket serverSocket, TdpChannel socket, byte[] data);
    public void onClientDisconnected(TdpServerSocket serverSocket, TdpChannel socket, EDisconnectReason reason);
    public void onServerSocketClosed(TdpServerSocket serverSocket);
    
}
