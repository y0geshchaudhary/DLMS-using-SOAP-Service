/*package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.HostEnum;
import centralRepo.interfaces.Repository;
import centralRepo.interfaces.RepositoryOperationEnum;
import server.interfaces.OperationsEnum;

public class UDPCSListener implements Runnable {

	private static final Logger log = LogManager.getLogger(UDPSSListener.class);
	private int port;
	private HostEnum host = HostEnum.HOST_1;
	private static String serverId;
	private static int expectedSequence = 1;
	static TreeMap<Integer,DatagramPacket> requestQueue=new TreeMap<>();

	public UDPCSListener(int port, String serverId) {
		super();
		this.port = port;
		UDPCSListener.serverId = serverId;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		try (MulticastSocket socket = new MulticastSocket(port)) {
			socket.joinGroup(InetAddress.getByName(Repository.CONCS_GROUP));
			log.debug("UDP sockket is open at " + port + " port to listen for request.");
			String requestString;
			int requestSequenceNumber;
			while (true) {
				byte[] data = new byte[5000];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				socket.receive(packet);
				//extract sequence number
				requestString = new String(packet.getData()).trim();
				requestSequenceNumber = Integer.parseInt(requestString.split("#")[2].trim());
				
				log.debug(requestSequenceNumber+" - fetch a reuqest with requestString as- "+requestString);
				if(requestSequenceNumber == expectedSequence) {
					log.debug("requestSequenceNumber matched expected sequence number so processing request.");
					Thread reqestHandler = new Thread(new UDPCSRequestHandler(packet, serverId));
					reqestHandler.start();
					expectedSequence++;
					informOtherServers();
					
				}else if(requestSequenceNumber > expectedSequence) {
					log.debug("requestSequenceNumber is greater than expected sequence number so putting it in waiting queue.");
					requestQueue.put(requestSequenceNumber, packet);
				} else {
					log.debug("already processed this requestSequenceNumber.");
				}
			}
		} catch (SocketException e) {
			log.error("Issue with opening socket connection over " + port, e);
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Issue with creating packet from data received on socket.", e);
			e.printStackTrace();
		}
	}
	
	private void informOtherServers() {
		List<String> serverDetails = new ArrayList<>();
		String replyString = "";
		try(DatagramSocket socket = new DatagramSocket()){
			String request = RepositoryOperationEnum.GET_LIBRARY_SERVER_DETAILS.name()+"#"+host;
			DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length, InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			socket.send(packet);
			byte[] replybuffer = new byte[5000];
			socket.receive(new DatagramPacket(replybuffer, replybuffer.length));
			replyString = new String(replybuffer).trim();
		} catch (SocketException e) {
			log.debug("There is an error creating or accessing a Socket.");
			e.printStackTrace();
		} catch (UnknownHostException e) {
			log.debug("The IP address of a host could not be determined.");
			e.printStackTrace();
		} catch (IOException e) {
			log.debug("Issue with I/O over socket connection.");
			e.printStackTrace();
		}
		
		for(String s : replyString.split("@")) {
			if(!s.contains(serverId))
				serverDetails.add(s);
		}
		
		if (serverDetails.size() > 0) {
			String[] details;
			String requestString = OperationsEnum.UPDATE_SEQUENCE_NUMBER.name();
			try (DatagramSocket socket = new DatagramSocket()) {

				for (String s : serverDetails) {
					details = s.split("#");
					log.debug("sending request to " + details[0] + " server to update it's sequence number.");

					DatagramPacket packet = new DatagramPacket(requestString.getBytes(),
							requestString.getBytes().length, InetAddress.getByName(details[1]),
							Integer.parseInt(details[2].trim()));
					socket.send(packet);
					byte[] replybuffer = new byte[5000];
					socket.receive(new DatagramPacket(replybuffer, replybuffer.length));
					if( new String(replybuffer).trim().equals("TRUE"))
						System.out.println(details[0]+" server sequence number is updated.");
					else 
						System.out.println(details[0]+" server sequence number is not updated.");
				}
			} catch (SocketException e) {
				log.debug("There is an error creating or accessing a Socket.");
				e.printStackTrace();
			} catch (UnknownHostException e) {
				log.debug("The IP address of a host could not be determined.");
				e.printStackTrace();
			} catch (IOException e) {
				log.debug("Issue with I/O over socket connection.");
				e.printStackTrace();
			}

		}
	}

	protected static boolean updateSequenceNumber() {
		log.debug("inside updateSequenceNumber() method.");
		expectedSequence++;
		log.debug("expectedSequenceNumber is incremented to "+ expectedSequence);
		if(!requestQueue.keySet().isEmpty() && requestQueue.firstKey()==expectedSequence) {
			log.debug("expectedSequenceNumber matched with first request in queue.");
			DatagramPacket packet = requestQueue.firstEntry().getValue();
			Thread reqestHandler = new Thread(new UDPCSRequestHandler(packet, serverId));
			reqestHandler.start();
		}
		return true;
	}
	
	public static void setSequencer(int v) {
		expectedSequence = v;
		log.debug("Resetting sequence number "+ expectedSequence);
	}
	
	public static int getSequencer() {
		log.debug("Getting sequence number "+ expectedSequence);
		return expectedSequence;
		
	}

}
*/