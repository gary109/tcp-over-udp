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
    
    public void onClientConnected(TdpServerChannel serverSocket, TdpChannel socket);
    public void onDataReceived(TdpServerChannel serverSocket, TdpChannel socket, byte[] data);
    public void onClientDisconnected(TdpServerChannel serverSocket, TdpChannel socket, EDisconnectReason reason);
    public void onServerSocketClosed(TdpServerChannel serverSocket);
    
}
