package server.model;

import java.io.Serializable;

public class GeneralException extends Exception/* implements Serializable*/ {

	/**
	 * default serial id
	 */
	private static final long serialVersionUID = 1L;
	public String reason;
	public GeneralException(String reason) {
		super();
		this.reason = reason;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
}
