package server.interfaces;

public enum UDPPortEnum {
	CONUDP(2001), MONUDP(2002), MCGUDP(2003);
	
	public int value;
	private UDPPortEnum(int value) {
		this.value = value;
	}
}
