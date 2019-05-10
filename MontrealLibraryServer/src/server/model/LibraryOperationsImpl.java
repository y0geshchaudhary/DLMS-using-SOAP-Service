package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.database.Database;
import server.interfaces.LibraryOperations;
import server.interfaces.OperationsEnum;
import server.interfaces.UDPPortEnum;

@WebService(endpointInterface = "server.interfaces.LibraryOperations")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class LibraryOperationsImpl implements LibraryOperations{
	private static final Logger log = LogManager.getLogger(LibraryOperationsImpl.class);

	private Database database;
//	private Repository centralRepository;
	private String serverId;
	String[] otherServerIds = { "CON", "MCG" };

	public LibraryOperationsImpl(String serverId) {
		database = Database.getDatabase();
		database.setServerId(serverId);
		this.serverId = serverId;
	}
	
	public String userExists(String userId) {
		log.debug("Inside userExists(String userId) method.");
		log.debug("call parameters: userId-" + userId);
		String result = database.userExists(userId);
		log.debug("method call result: " + result);
		return result;
	}

	/**
	 * Manager roles
	 */
	
	public boolean addItem(String managerID, String itemID, String itemName, int quantity) throws GeneralException {
		log.debug("Inside addItem(String managerID, String itemID, String itemName, int quantity) method.");
		log.debug("call parameters: managerID-" + managerID + " ,itemID-" + itemID + " ,itemName-" + itemName
				+ " ,quantity-" + quantity);
		boolean result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.addBookToLibrary(itemID, new Book(itemID, itemName, quantity));
			log.debug("method call result: " + result);
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	
	public int removeItem(String managerID, String itemID, int quantity) throws GeneralException {
		log.debug("Inside removeItem(String managerID, String itemID, int quantity) method.");
		log.debug("call parameters: managerID-" + managerID + " ,itemID-" + itemID + " ,quantity-" + quantity);
		int result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.removeBooksFromLibrary(itemID, quantity);
			log.debug("method call result: " + result);
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	
	public Book[] listAvailableItems(String managerID) throws GeneralException {
		log.debug("Inside listAvailableItems(String managerID) method.");
		log.debug("call parameters: managerID-" + managerID);
		List<Book> bookList;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			bookList = database.getAllBooks();
			log.debug("method returning "+bookList.size()+" books.");
			Book[] books = new Book[bookList.size()];
			return bookList.toArray(books);
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	/**
	 * User roles
	 */

	public int borrowItem(String userID, String itemID) throws GeneralException {
		log.debug("Inside borrowItem(String userID, String itemID, String numberOfDays) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID, false)) {
			if (itemID.startsWith(serverId)) {
				int result = database.borrowBook(userID, itemID, 1);
				log.debug("request belong to this library. method call returns: " + result);
				return result;
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data =OperationsEnum.BORROW_ITEM.name().concat("#").concat(userID).concat("#")
						.concat(itemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					log.debug("response from remote library: " + data);
					return Integer.parseInt(data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.");
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails");
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.");
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}

		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	
	public Book[] findItem(String userID, String itemName) throws GeneralException {
		log.debug("Inside findItem(String userID, String itemName) method.");
		log.debug("call parameters: userID-" + userID + " ,itemName-" + itemName);
		userID = userID.toUpperCase();
		itemName = itemName.toUpperCase();
		List<Book> bookList = new ArrayList<>();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			// query local DB
			List<Book> localDBBooksList = database.findItem(itemName);
			log.debug("no. of related books in local library are " + localDBBooksList.size());
			bookList.addAll(localDBBooksList);

			// query other server DB
			String data = OperationsEnum.FIND_ITEM.name().concat("#").concat(itemName);
			byte[] dataBytes = data.getBytes();
			log.debug("request data to be send to other libraries is " + data);
			try (DatagramSocket socket = new DatagramSocket();) {
				// get details from Concordia university from central repo
				int udpServerPort = getServerDetails(otherServerIds[0]);
				DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getLocalHost(), udpServerPort);
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				String[] bookDetails = data.split("#");
				log.debug("no. of related books received from " + otherServerIds[0] + " are " + bookDetails.length / 2);

				if(bookDetails.length>1) {
					for (int i = 0; i < bookDetails.length; i += 2) {
						bookList.add(new Book(bookDetails[i], itemName, Integer.parseInt(bookDetails[i + 1])));
					}
				}

				data = OperationsEnum.FIND_ITEM.name().concat("#").concat(itemName);
				dataBytes = data.getBytes();
				// get details from Montreal university from central repo
				udpServerPort = getServerDetails(otherServerIds[1]);
				packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getLocalHost(), udpServerPort);
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				bookDetails = data.split("#");
				log.debug("no. of related books received from " + otherServerIds[1] + " are " + bookDetails.length / 2);

				if(bookDetails.length>1) {
					for (int i = 0; i < bookDetails.length; i += 2) {
						bookList.add(new Book(bookDetails[i], itemName, Integer.parseInt(bookDetails[i + 1])));
					}
				}

				log.debug("Total books to be returned are "+bookList.size());
				Book[] books = new Book[bookList.size()];
				return bookList.toArray(books);
			} catch (SocketException e) {
				log.error("Unable to open socket connection.", e);
				e.printStackTrace();
				throw new GeneralException("Unable to open socket connection.");
			} catch (UnknownHostException e) {
				log.error("Unable to identify host given by udpServerDetails", e);
				e.printStackTrace();
				throw new GeneralException("Unable to identify host given by udpServerDetails.");
			} catch (IOException e) {
				log.error("Issue with sending or receiving data packet.", e);
				e.printStackTrace();
				throw new GeneralException("Issue with sending or receiving data packet.");
			}
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	
	public boolean returnItem(String userID, String itemID) throws GeneralException {
		log.debug("Inside returnItem(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.returnBook(userID, itemID);
				log.debug("request belong to this library. method call returns: " + result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = OperationsEnum.RETURN_ITEM.name().concat("#").concat(userID).concat("#")
						.concat(itemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				// get details of server from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			return result;

		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	
	public boolean addToWaitingList(String userID, String itemID) throws GeneralException {
		log.debug("Inside addToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		userID = userID.trim();
		itemID = itemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.addUserToWaitingList(userID, itemID);
				log.debug("request belong to this library. method call returns: " + result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = OperationsEnum.ADD_TO_WAITING_LIST.name().concat("#").concat(userID)
						.concat("#").concat(itemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			return result;
		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing GeneralException.", e);
			throw e;
		}
	}

	private boolean operationIsAllowed(String userId, boolean managerOperation) {
		log.debug("Inside operationIsAllowed(String userId, boolean managerOperation) method.");
		log.debug("call parameters: userId-" + userId + " ,managerOperation-" + managerOperation);
		boolean result;
		if (managerOperation)
			result = userId.charAt(3) == 'M' ? true : false;
		else
			result = userId.charAt(3) == 'U' ? true : false;

		log.debug("method call returns: " + result);
		return result;
	}

	
	public boolean addToWaitingListOverloaded(String userID, String itemID, String oldItemID) throws GeneralException {
		log.debug("Inside addToWaitingListOverloaded(String userID, String newItemID, String oldItemID) method.");
		log.debug("call parameters: userID-" + userID + " ,newItemID-" + itemID + " ,oldItemID-" + oldItemID);
		userID = userID.trim();
		itemID = itemID.trim();
		oldItemID = oldItemID.trim();
		boolean addToWaitingListResult = this.addToWaitingList(userID, itemID);
		if (addToWaitingListResult) {
			log.debug("added user to waiting list of newItemID.");
			boolean returnResult = this.returnItem(userID, oldItemID);
			if (returnResult) {
				log.debug("returned oldItem to its library.");
				return true;
			} else
				return false;

		} else {
			log.debug("Unable to add to waiting list so returning false.");
			return false;
		}
	}

	/**
	 * return 0 if book wasn't available or user doesn't belong to new book library
	 * and want to exchange book from same library or wasn't able to exchange book
	 * due to any reason, 1 if exchange was successful and -1 if user didn't
	 * borrowed the old item.
	 */
	public int exchangeItem(String userID, String newItemID, String oldItemID) throws GeneralException {
		log.debug("Inside exchangeItem(String userID, String newItemID, String oldItemID.");
		log.debug("call parameters: userID-" + userID + " ,newItemID-" + newItemID + " ,oldItemID-" + oldItemID);
		userID = userID.trim();
		newItemID = newItemID.trim();
		oldItemID = oldItemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean bookBorrowed = false;
			boolean bookAvailable = false;
			if (oldItemID.startsWith(serverId)) {
				bookBorrowed = database.bookBorrowed(userID, oldItemID);
			} else {
				log.debug("oldItem belongs to " + oldItemID.substring(0, 3));
				String data = OperationsEnum.BOOK_BORROWED.name().concat("#").concat(userID)
						.concat("#").concat(oldItemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = oldItemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookBorrowed = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("oldItem was borrowed by this user?: " + bookBorrowed);

			if (!bookBorrowed) {
				log.debug("Book " + oldItemID + " is not borrowed by user. So returning response as -1");
				return -1;
			}

			if (newItemID.startsWith(serverId)) {
				bookAvailable = database.bookAvailable(newItemID);
			} else {
				log.debug("newItem belongs to " + newItemID.substring(0, 3));
				String data = OperationsEnum.BOOK_AVAILABLE.name().concat("#").concat(newItemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = newItemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookAvailable = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("newItem is avialable?: " + bookAvailable);

			if (!bookAvailable) {
				log.debug("Book is not available in library. User should be put in waiting list.");
				return 0;
			} else {
				int borrowBookResult = -2;
				boolean returnBookResult = false;
				if (bookAvailable && bookBorrowed) {
					log.debug("user borrowed the oldItem and newItem is also available.");
					borrowBookResult = this.borrowItem(userID, newItemID);
					log.debug("result of invoking borrowBook() on newItem server is :" + borrowBookResult);
					// if book is borrowed successfully.
					if (borrowBookResult == 1) {
						returnBookResult = this.returnItem(userID, oldItemID);
						log.debug("borrowed newItem and result of returning oldItem :" + returnBookResult);
						// if book was returned successfully then return 1.
						if (returnBookResult) {
							log.debug("Items were exchanged successfully.");
							return 1;
						}
						// if not then return the newly borrowed book.
						else {
							log.debug("Unable to return oldItem so returning newItem.");
							this.returnItem(userID, newItemID);
							return 0;
						}
					}
					// if book is not borrowed and borrowBookResult = 1 or use doesn't belong to
					// newItem library but still want to exchange book against the book which belong
					// to same library or he already have a different book from that library and and
					// borrowBookResult = 2
					// so return 0 so that user can add himself to waiting list and return oldItem.
					else
						return 0;
				}
				// not able to exchange items so by default just enter in waiting list by
				// returning the book.
				else
					return 0;
			}

		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.", e);
			throw e;
		}
	}
	
	
	/**
	 * ------------------------------------------------------------------------------------------------------
	 */
	
	/**
	 * Yash and Pulkit based implementation
	 * 
	 * return 0 if book is not exchanged otherwise return 1 if it is.
	 * 
	 * check if oldbook is borrowed or not 
	 * check if newbook is available or not 
	 * if !oldbook return false 
	 * if !newbook return false 
	 * case1: if both books are from same lib then exchange it and return true 
	 * case 2: if they are from different lib 
	 * 		if( newbook is from different lib from which user belongs) 
	 * 			check if user already have book from newbook lib 
	 * 				if true return false 
	 * 				else exchange and return true
	 * 
	 */
	public int exchangeItemDuplicate(String userID, String oldItemID, String newItemID) throws GeneralException {
		log.debug("Inside exchangeItemDuplicate(String userID, String newItemID, String oldItemID.");
		log.debug("call parameters: userID-" + userID + " ,newItemID-" + newItemID + " ,oldItemID-" + oldItemID);
		userID = userID.trim();
		newItemID = newItemID.trim();
		oldItemID = oldItemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean bookBorrowed = false;
			boolean bookAvailable = false;
			if (oldItemID.startsWith(serverId)) {
				bookBorrowed = database.bookBorrowed(userID, oldItemID);
			} else {
				log.debug("oldItem belongs to " + oldItemID.substring(0, 3));
				String data = OperationsEnum.BOOK_BORROWED.name().concat("#").concat(userID)
						.concat("#").concat(oldItemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = oldItemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookBorrowed = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("oldItem was borrowed by this user?: " + bookBorrowed);

			if (!bookBorrowed) {
				log.debug("Book " + oldItemID + " is not borrowed by user. So returning response as 0");
				return 0;
			}

			if (newItemID.startsWith(serverId)) {
				bookAvailable = database.bookAvailable(newItemID);
			} else {
				log.debug("newItem belongs to " + newItemID.substring(0, 3));
				String data = OperationsEnum.BOOK_AVAILABLE.name().concat("#").concat(newItemID);
				log.debug("Data to send over UDP socket: " + data);
				byte[] dataBytes = data.getBytes();
				String server = newItemID.substring(0, 3);
				//get server details from central repo
				int udpServerPort = getServerDetails(server);
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getLocalHost(), udpServerPort);
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					bookAvailable = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: " + data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.", e);
					e.printStackTrace();
					throw new GeneralException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails", e);
					e.printStackTrace();
					throw new GeneralException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.", e);
					e.printStackTrace();
					throw new GeneralException("Issue with sending or receiving data packet.");
				}
			}
			log.debug("newItem is avialable?: " + bookAvailable);

			if (!bookAvailable) {
				log.debug("Book is not available in library. Returning 0.");
				return 0;
			} else if(oldItemID.substring(0, 3).equals(newItemID.substring(0, 3))){
				/*if(this.borrowItem(userID, newItemID)==1) {
				this.returnItem(userID, oldItemID);*/
			if(this.returnItem(userID, oldItemID)) {
				this.borrowItem(userID, newItemID);
				return 1;
			}else return 0;
				
			} else if(!oldItemID.substring(0, 3).equals(newItemID.substring(0, 3))) {
				int borrowBook = this.borrowItem(userID, newItemID);
				if(borrowBook==1) {
					this.returnItem(userID, oldItemID);
					return 1;
				}else return 0;
			}else return 0;

		} else {
			GeneralException e = new GeneralException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.", e);
			throw e;
		}
	}
	
	/**
	 * ------------------------------------------------------------------------------------------------------
	 */
	
	private int getServerDetails(String server) {
		return UDPPortEnum.valueOf(server+"UDP").value;
	}
	
/*	private String getServerDetails(String server) {
		String returnString = null;
		String centralRepoResponse=null;
		try(DatagramSocket socket = new DatagramSocket();){
			byte[] dataByte =  new byte[5000];
			log.debug("getting "+server+" details from central repo.");
			String dataString = RepositoryOperationEnum.GET_LIBRARY_SERVER_DETAILS.name()+"#"+HostEnum.HOST_1.name();
			DatagramPacket packet = new DatagramPacket(dataString.getBytes(), dataString.getBytes().length, InetAddress.getByName(Repository.CENTRAL_REPOSITORY_HOSTNAME), Repository.CENTRAL_REPOSITORY_PORT);
			socket.send(packet);
			packet = new DatagramPacket(dataByte, dataByte.length);
			socket.receive(packet);
			centralRepoResponse = new String(packet.getData()).trim();
			log.debug("Central repository response is "+centralRepoResponse);
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
		String[] dataArray = centralRepoResponse.split("@");
		for(String s : dataArray) {
			if(s.startsWith(server))
				//CONSS#<ip>#<port>
				returnString = s.substring(6,s.length());
		}
		return returnString;
	}*/
}