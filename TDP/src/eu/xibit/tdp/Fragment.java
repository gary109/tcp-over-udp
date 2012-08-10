/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.xibit.tdp;

/**
 *
 * @author David
 */
class Fragment {
	
	private long msgId;
	private byte[] data;

	public Fragment(long msgId, byte[] data) {
		this.msgId = msgId;
		this.data = data;
	}

	public long getMsgId() {
		return msgId;
	}

	public byte[] getData() {
		return data;
	}

	public void setMsgId(long msgId) {
		this.msgId = msgId;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (int) (this.msgId ^ (this.msgId >>> 32));
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Fragment other = (Fragment) obj;
		if (this.msgId != other.msgId) {
			return false;
		}
		return true;
	}
	
}
