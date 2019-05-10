package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.database.Database;
import server.interfaces.OperationsEnum;

public class UDPSSRequestHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPSSRequestHandler.class);
	private DatagramPacket packet;

	public UDPSSRequestHandler(DatagramPacket packet) {
		super();
		this.packet = packet;
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		byte[] data = packet.getData();
		if (data == null || data.length == 0)
			return;
		else {
			String dataString = new String(data);
			String[] dataArray = dataString.trim().split("#");
			OperationsEnum operation = OperationsEnum.valueOf(dataArray[0]);
			Database database = Database.getDatabase();
			log.debug("Data string received for processing is - " + dataString);

			DatagramPacket replyPacket;
			boolean resultBool;
			String resultString = null;
			int resultInt;
			try (DatagramSocket socket = new DatagramSocket();) {
				switch (operation) {

				// request = dataString format OperationsEnumNo#userId#itemId
				// response = dataString format int
				case BORROW_ITEM:
					log.debug("Operation reuqested: BORROW_ITEM");
					resultInt = database.borrowBook(dataArray[1], dataArray[2], 0);
					resultString = String.valueOf(resultInt);
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format OperationsEnumNo#userId#itemId
				// response = dataString format string as TRUE/FALSE
				case ADD_TO_WAITING_LIST:
					log.debug("Operation reuqested: ADD_TO_WAITING_LIST");
					resultBool = database.addUserToWaitingList(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format OperationsEnumNo#itemName
				// response = dataString format bookId#numberOfCopies#bookId#numberOfCopies
				case FIND_ITEM:
					log.debug("Operation reuqested: FIND_ITEM");
					List<Book> books = database.findItem(dataArray[1]);
					if (books != null && books.size() > 0) {
						for (Iterator<Book> iterator = books.iterator(); iterator.hasNext();) {
							Book bookImpl = (Book) iterator.next();
							resultString = bookImpl.getId().concat("#")
									.concat(String.valueOf(bookImpl.getNumberOfCopies())).concat("#");
						}
						resultString = resultString.substring(0, resultString.length() - 1);

					} else
						resultString = "";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format OperationsEnumNo#userId#itemId
				// response = dataString format string as TRUE/FALSE
				case RETURN_ITEM:
					log.debug("Operation reuqested: RETURN_ITEM");
					resultBool = database.returnBook(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format OperationsEnumValue#userId#oldItemId
				// response = dataString format string as TRUE/FALSE
				case BOOK_BORROWED:
					log.debug("Operation reuqested: BOOK_BORROWED");
					resultBool = database.bookBorrowed(dataArray[1], dataArray[2]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				// request = dataString format OperationsEnumValue#newItemId
				// response = dataString format string as TRUE/FALSE
				case BOOK_AVAILABLE:
					log.debug("Operation reuqested: BOOK_AVAILABLE");
					resultBool = database.bookAvailable(dataArray[1]);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				/*// request = dataString format OperationsEnumValue
				// response = nothing
				case UPDATE_DB:
					log.debug("Operation reuqested: UPDATE_DB");
					database.getData(packet.getAddress(), packet.getPort());
					break;

				// request = dataString format OperationsEnumValue
				// response = dataString format string as TRUE/FALSE
				case UPDATE_SEQUENCE_NUMBER:
					log.debug("Operation reuqested: UPDATE_SEQUENCE_NUMBER");
					resultBool = UDPCSListener.updateSequenceNumber();
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				case RECOVER_SERVER:
					log.debug("Operation reuqested: RECOVER_SERVER");
					resultBool = this.serverHandler(false);
					resultString = resultBool ? "TRUE" : "FALSE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;

				case HEARTBEAT:
					log.debug("Operation reuqested: HEARTBEAT");
					resultString = "TRUE";
					replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
							packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Result of operation : " + resultString);
					break;*/

				default:
					log.debug("Default Operation.");
					replyPacket = new DatagramPacket(new byte[0], 0, packet.getAddress(), packet.getPort());
					socket.send(replyPacket);
					log.debug("Returning empty byte array.");
				}
			} catch (SocketException e) {
				log.error("Issue with opening socket connection.", e);
				e.printStackTrace();
			} catch (IOException e) {
				log.error("Issue with sending data packet.", e);
				e.printStackTrace();
			}
		}
	}
	
	/*private boolean serverHandler(boolean restartServer) {
		log.debug("inside serverHandler() method.");
		log.debug("parameter value is - restartServer: "+restartServer);
		
		log.debug("Updating database.");
		List<String> serverDetails = null;
		try(DatagramSocket socket = new DatagramSocket()){
//			String request = RepositoryOperationEnum.GET_LIBRARY_SERVER_DETAILS.name()+"#CONCS";
			String request = RepositoryOperationEnum.GET_LIBRARY_SERVER_DETAILS.name()+"#"+HostEnum.HOST_3;
			DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length, InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			socket.send(packet);
			byte[] replybuffer = new byte[5000];
			socket.receive(new DatagramPacket(replybuffer, replybuffer.length));
			String reply = new String(replybuffer);
			serverDetails = new ArrayList<>(Arrays.asList(reply.split("@")));
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
		int thisHostDetailsIndex = -1;
		for (int i = 0; i < serverDetails.size(); i++) {
			if(serverDetails.get(i).contains(host.name()))
				thisHostDetailsIndex = i;
		}
		serverDetails.remove(thisHostDetailsIndex);
		
		String serverDetail = null;
		for(String s: serverDetails) {
			if(s.startsWith("CONSS"))
				serverDetail = s.trim();
		}
//		Database.getDatabase().updateDatabase(serverDetails.get(0).trim(), serverDetails.get(1).trim());
		Database.getDatabase().updateDatabase(serverDetail, null);
		log.debug("Database updated.");
		if(restartServer) {
			log.debug("Restarting server and registering details with central repo.");
			main(null);
		}
		return true;
	}*/
}