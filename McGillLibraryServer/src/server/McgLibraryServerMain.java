package server;

import javax.xml.ws.Endpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.interfaces.PublishURLEnum;
import server.interfaces.UDPPortEnum;
import server.model.LibraryOperationsImpl;
import server.model.UDPSSListener;

public class McgLibraryServerMain {
	private static final Logger log = LogManager.getLogger(McgLibraryServerMain.class);
	// SERVER and UDP details.
	private static String library = "MCG";
	private static int portUDP = UDPPortEnum.MCGUDP.value;
	static LibraryOperationsImpl libraryOperationsImpl;
	
	public static void main(String[] args) {
		log.debug("Inside main() method.");

		// setting up UDP listener thread for server-server communication.
		Thread udpSSThread = new Thread(new UDPSSListener(portUDP));
		udpSSThread.start();
		log.debug("Starting UDP thread for server-server comm.");
		
		// setting up UDP listener thread for client-server communication.
		libraryOperationsImpl = new LibraryOperationsImpl(library);
		Endpoint endpoint = Endpoint.publish(PublishURLEnum.MCG.url, libraryOperationsImpl);
		log.debug("Publishing endpoint for client-server comm.");
		
		log.debug("McGill server is up.");
	}
	
}
