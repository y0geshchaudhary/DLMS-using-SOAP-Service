/*package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.HostEnum;
import server.interfaces.LibraryOperationsEnum;

public class UDPCSRequestHandler implements Runnable {
	private static final Logger log = LogManager.getLogger(UDPCSRequestHandler.class);
	private DatagramPacket packet;
	private LibraryOperations libraryOperations;
	private HostEnum host = HostEnum.HOST_1;

	public UDPCSRequestHandler(DatagramPacket packet, String serverId) {
		super();
		this.packet = packet;
		this.libraryOperations = new LibraryOperations(serverId);
	}

	@Override
	public void run() {
		log.debug("Inside run() method.");
		byte[] data = packet.getData();
		String dataString = new String(data);
		log.debug("Data string received for processing is - " + dataString);
		
		String[] dataArray = dataString.trim().split("#");
		String feIP = dataArray[0];
		int fePort = Integer.parseInt(dataArray[1]);
//		int sequenceNumber =  Integer.parseInt(dataArray[2]);
		LibraryOperationsEnum operation = LibraryOperationsEnum.valueOf(dataArray[3]);
		

		DatagramPacket replyPacket;
		boolean resultBool;
		String resultString = "";
		int resultInt;
		try (DatagramSocket socket = new DatagramSocket();) {
			switch (operation) {

			case USER_EXISTS:
				log.debug("Operation reuqested: USER_EXISTS");
				resultString = libraryOperations.userExists(dataArray[4]);
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;

			case ADD_ITEM:
				log.debug("Operation reuqested: ADD_ITEM");
				resultBool = libraryOperations.addItem(dataArray[4], dataArray[5], dataArray[6], Integer.parseInt(dataArray[7]));
				resultString = resultBool?"TRUE":"FALSE";
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case REMOVE_ITEM:
				log.debug("Operation reuqested: REMOVE_ITEM");
				resultInt = libraryOperations.removeItem(dataArray[4], dataArray[5], Integer.parseInt(dataArray[6]));
				resultString = String.valueOf(resultInt);
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case LIST_AVAILABLE_ITEM:
				log.debug("Operation reuqested: LIST_AVAILABLE_ITEM");
				List<Book> books = libraryOperations.listAvailableItems(dataArray[4]);
				StringBuilder stringBuilder = new StringBuilder();
				for(Book b : books) {
					stringBuilder.append(b.getId()).append("#").append(b.getName()).append("#").append(b.getNumberOfCopies()).append("@");
				}
				resultString = stringBuilder.substring(0, stringBuilder.length()-1);
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case BORROW_ITEM:
				log.debug("Operation reuqested: BORROW_ITEM");
				resultInt = libraryOperations.borrowItem(dataArray[4], dataArray[5]);
				resultString = String.valueOf(resultInt);
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;	
				
			case FIND_ITEM:
				log.debug("Operation reuqested: FIND_ITEM");
				List<Book> books1 = libraryOperations.findItem(dataArray[4], dataArray[5]);
				StringBuilder stringBuilder1 = new StringBuilder();
				for(Book b : books1) {
					stringBuilder1.append(b.getId()).append("#").append(b.getName()).append("#").append(b.getNumberOfCopies()).append("@");
				}
				if(stringBuilder1.length()>0)
					resultString = stringBuilder1.substring(0, stringBuilder1.length()-1);
				
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case RETURN_ITEM:
				log.debug("Operation reuqested: RETURN_ITEM");
				resultBool = libraryOperations.returnItem(dataArray[4], dataArray[5]);
				resultString = resultBool?"TRUE":"FALSE";
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case ADD_TO_WAITING_LIST:
				log.debug("Operation reuqested: ADD_TO_WAITING_LIST");
				resultBool = libraryOperations.addToWaitingList(dataArray[4], dataArray[5]);
				resultString = resultBool?"TRUE":"FALSE";
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;

			case ADD_TO_WAITING_LIST_OVERLOADED:
				log.debug("Operation reuqested: ADD_TO_WAITING_LIST_OVERLOADED");
				resultBool = libraryOperations.addToWaitingListOverloaded(dataArray[4], dataArray[5], dataArray[6]);
				resultString = resultBool?"TRUE":"FALSE";
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;
				
			case EXCHANGE_ITEM:
				log.debug("Operation reuqested: EXCHANGE_ITEM");
				resultInt = libraryOperations.exchangeItemDuplicate(dataArray[4], dataArray[5], dataArray[6]);
				resultString = String.valueOf(resultInt);
				resultString = host+"#"+resultString;
				replyPacket = new DatagramPacket(resultString.getBytes(), resultString.getBytes().length,
						InetAddress.getByName(feIP), fePort);
				sendReliableMessage(socket, replyPacket);
				log.debug("Result of operation : " + resultString);
				break;	

			default:
				log.debug("Default Operation.");
				replyPacket = new DatagramPacket(new byte[0], 0, packet.getAddress(), packet.getPort());
				sendReliableMessage(socket, replyPacket);
				log.debug("Returning empty byte array.");
			}
			
			//UDPCSListener.updateSequenceNumber();
		} catch (IOException | GeneralException e) {
			GeneralException exception;
			if(!(e instanceof GeneralException)) {
				exception = new GeneralException("Issue opening socket connection or sending data packet.");
			}else exception = (GeneralException) e;
			log.error("General exception - "+exception.reason,e);
			try(DatagramSocket sock =new DatagramSocket();) {
				String exceptionString = host+"#"+"EXCEPTION#"+exception.reason;
				DatagramPacket packet = new DatagramPacket(exceptionString.getBytes(),exceptionString.getBytes().length, InetAddress.getByName(feIP), fePort);
				sock.send(packet);
			} catch (IOException e2) {
				log.debug("Exception message IOException exception.");
				e2.printStackTrace();
			}
			e.printStackTrace();
		}		
	}
	
	private void sendReliableMessage(DatagramSocket socket, DatagramPacket packet) {

		int packetSentCounter = 0;
		try {
			socket.send(packet);
			socket.setSoTimeout(5 * 1000);

			for (int i = 0; i < 3; i++) {
				log.debug("Sending message " + i + " times.");
				packetSentCounter++;
				byte[] dataByte = new byte[5000];
				DatagramPacket replyPacket = new DatagramPacket(dataByte, dataByte.length);
				try {
					socket.receive(replyPacket);
					log.debug("FE received message succesfully.");
					break;
				} catch (SocketTimeoutException e) {
					if (packetSentCounter == 3) {
						log.debug("Already sent packet 3 time but didn't received any acknowledgement from FE.");
						break;
					} else {
						socket.send(packet);
						continue;
					}
				}
			}
		} catch (IOException e) {
			log.error("IO Exception "+e.getMessage());
			e.printStackTrace();
		}

	}

}
*/