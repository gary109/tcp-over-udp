/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

/**
 *
 * @author David
 */
public interface ISocketEventListener {
	
	public void onDataReceived(TdpSocket socket);
	public void onDisconnected(TdpSocket socket, EDisconnectReason reason);
	
}
