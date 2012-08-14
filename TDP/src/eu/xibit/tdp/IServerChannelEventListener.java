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
    
    public void onClientConnected(TdpServerChannel serverChannel, TdpChannel channel);
    public void onDataReceived(TdpServerChannel serverChannel, TdpChannel channel, byte[] data);
    public void onClientDisconnected(TdpServerChannel serverChannel, TdpChannel channel, EDisconnectReason reason);
    public void onServerSocketClosed(TdpServerChannel serverChannel);
    
}
