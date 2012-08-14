/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 *
 * @author David
 */
final class TdpInputSorter {

	private final TdpChannel socket;
	private final Object readLock = new Object();
	private TreeSet<Fragment> fragments = new TreeSet<Fragment>(new Comparator<Fragment>() {
		@Override
		public int compare(Fragment o1, Fragment o2) {
			if (o1.getMsgId() == o2.getMsgId()) {
				return 0;
			} else if (o1.getMsgId() < o2.getMsgId()) {
				return -1;
			} else {
				return 1;
			}
		}
	});
	private long lastInOrderMsgId = -1;

	TdpInputSorter(TdpChannel socket) {
		this.socket = socket;
	}

	public synchronized boolean available() throws IOException {
		return ((fragments.size() > 0) && (fragments.first().getMsgId() == lastInOrderMsgId));
	}

	synchronized void addFragment(final Fragment fragment) {
		if (lastInOrderMsgId + 1 == fragment.getMsgId()) {
			lastInOrderMsgId++;
			if (socket.getListener() != null) {
				socket.getListener().onDataReceived(socket, fragment.getData());
			}
			if (socket.getServerListener() != null) {
				socket.getServerSocket().getExecutor().executeData(new Runnable() {
					@Override
					public void run() {
						if (socket.getServerListener() != null) {
							socket.getServerListener().onDataReceived(socket.getServerSocket(), socket, fragment.getData());
						}
					}
				});
			}
			Iterator<Fragment> iterator = fragments.iterator();
			while (iterator.hasNext()) {
				final Fragment f = iterator.next();
				if (f.getMsgId() != lastInOrderMsgId + 1) {
					break;
				}
				if (socket.getListener() != null) {
					socket.getListener().onDataReceived(socket, f.getData());
				}
				if (socket.getServerListener() != null) {
					socket.getServerSocket().getExecutor().executeData(new Runnable() {
						@Override
						public void run() {
							if (socket.getServerListener() != null) {
								socket.getServerListener().onDataReceived(socket.getServerSocket(), socket, f.getData());
							}
						}
					});
				}
				lastInOrderMsgId++;
			}
		} else {
			fragments.add(fragment);
		}
	}
}
