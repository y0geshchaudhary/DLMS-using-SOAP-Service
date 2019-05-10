package server.interfaces;

public enum PublishURLEnum {
	CON("http://localhost:8081/CON"),MON("http://localhost:8083/MON"),MCG("http://localhost:8082/MCG");
	
	public String url;
	private PublishURLEnum(String url) {
		this.url=url;
	}
	
}
