/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

/**
 *
 * @author dipacs
 */
public interface IClientChannelEventListener {
    
    public void onDataReceived(TdpChannel channel, byte[] data);
    public void onClientDisconnected(TdpChannel channel, EDisconnectReason reason);
    
}
