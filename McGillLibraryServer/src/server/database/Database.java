package server.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.model.Book;

public class Database {
	private static final Logger log = LogManager.getLogger(Database.class);
	// map with key as bookId and value as Book object
	private static final Map<String, Book> bookDB = Collections.synchronizedMap(new LinkedHashMap<>());
	// Set of String as userId
	private static final Set<String> users = Collections.synchronizedSet(new LinkedHashSet<>());
	// map with key as bookId and value as list of userId
	private static final Map<String, List<String>> waitingList = Collections.synchronizedMap(new LinkedHashMap<>());
	// map with key as bookId and value as list of userId
	private static final Map<String, List<String>> borrowedBooks = Collections.synchronizedMap(new LinkedHashMap<>());
	private static Database db;
	private String serverId;

	private Database() {
		createDatabaseEntries();
	}

	public static Database getDatabase() {
		if (db == null)
			db = new Database();

		return db;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	private void createDatabaseEntries() {
		log.debug("Inside createDatabaseEntries() method.");
		synchronized (users) {
			/*
			 * Collections.addAll(users, "MCGM1111", "MCGM1112", "MCGM1113", "MCGM1114",
			 * "MCGM1115", "MCGU1111", "MCGU1112", "MCGU1113", "MCGU1114", "MCGU1115",
			 * "MCGU1116", "MCGU1117", "MCGU1118", "MCGU1119", "MCGU1120");
			 */
			Collections.addAll(users, "MCGM1111", "MCGU1111");
		}
		log.debug("No. of users added to DB are " + users.size());
		List<String> bookIds = new LinkedList<>();
		List<String> bookNames = new LinkedList<>();
		int numberOfCopies = 5;
		/*
		 * Collections.addAll(bookIds, "MCG6231", "MCG6641", "MCG6491", "MCG6651",
		 * "MCG6481", "MCG6501", "MCG6411", "MCG6180", "MCG6461", "MCG6521");
		 * Collections.addAll(bookNames, "Distributed Systems", "Advanced Programming",
		 * "Systems Software", "Algorithm Design", "System Requirements Spec",
		 * "Programming Competency", "Comparative Studies", "Data Mining",
		 * "Software Design", "Advance Database");
		 */

		Collections.addAll(bookIds, "MCG1012");
		Collections.addAll(bookNames, "Distributed");

		synchronized (bookDB) {
			Book book;
			for (int i = 0; i < bookIds.size(); i++) {
				book = new Book(bookIds.get(i), bookNames.get(i), numberOfCopies);
				bookDB.put(book.getId(), book);
			}
			log.debug("No. of books added to DB are " + bookDB.size());
		}
	}

	public String userExists(String userID) {
		log.debug("Inside userExists(String userID) method.");
		log.debug("call parameters: userID-" + userID);
		synchronized (users) {
			log.debug("return value: " + users.contains(userID));
			String returnString = "N";
			if (users.contains(userID)) {
				if (userID.charAt(3) == 'M')
					returnString = "M";
				else if (userID.charAt(3) == 'U')
					returnString = "U";
			}
			return returnString;
		}
	}

	public boolean addBookToLibrary(String itemID, Book book)/* throws RemoteException */ {
		log.debug("Inside addBookToLibrary(String itemID, Book book) method.");
		log.debug("call parameters: itemID-" + itemID + " , book-" + book);
		Book b;
		synchronized (bookDB) {
			synchronized (waitingList) {
				synchronized (borrowedBooks) {
					// try {
					if (bookDB.containsKey(itemID)) {
						b = bookDB.get(itemID);
						b.setNumberOfCopies(b.getNumberOfCopies() + book.getNumberOfCopies());
						log.debug("Book is already in DB, incremented it's quantity.");
					} else {
						bookDB.put(itemID, book);
						log.debug("Book is not in DB, adding book to DB.");
					}
					sweepWaitingList();
					return true;
				}
			}
		}
	}

	// return 0 if operation is failed, 1 if operation is successful and all the
	// records are deleted, 2
	// quantity is decresed and operation is succesfulland and 0 if
	// there is no item to delete.
	public int removeBooksFromLibrary(String itemID, int quantity)/* throws RemoteException */ {
		log.debug("Inside removeBooksFromLibrary(String itemID, int quantity) method.");
		log.debug("call parameters: itemID-" + itemID + " , quantity-" + quantity);
		Book b;
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				// try {
				if (bookDB.containsKey(itemID)) {
					b = bookDB.get(itemID);
					if (quantity == -1) {
						bookDB.remove(b.getId());
						borrowedBooks.remove(b.getId());
						log.debug("Book is completely removed from DB.");
						return 1;
					} else if (quantity <= b.getNumberOfCopies()) {
						b.setNumberOfCopies(b.getNumberOfCopies() - quantity);
						log.debug("No. of copies decremented by quantity.");
						return 2;
					} else if (quantity > b.getNumberOfCopies()) {
						log.debug("Quantity to decrease is higher than available books in library. So doing nothing.");
						return 0;
					} else {
						log.debug("Returning without changing DB.");
						return 0;
					}
				} else {
					log.debug("There is no item found in library to remove it.");
					return 0;
				}
			}
		}
	}

	public List<Book> getAllBooks() {
		log.debug("Inside getAllBooks() method.");
		synchronized (bookDB) {
			List<Book> list = new ArrayList<>(bookDB.values());
			log.debug("Returning " + list.size() + " books.");
			return list;
		}
	}

	public boolean returnBook(String userID, String itemID)/* throws RemoteException */ {
		log.debug("Inside returnBook(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " , itemID-" + itemID);
		// return the book to library and assign it to user if there is any in waiting
		// list for that book.
		synchronized (borrowedBooks) {
			synchronized (bookDB) {
				synchronized (waitingList) {
					if (borrowedBooks.containsKey(itemID) && borrowedBooks.get(itemID).contains(userID)) {
						if (borrowedBooks.get(itemID).remove(userID)) {
							Book book = bookDB.get(itemID);
							book.setNumberOfCopies(book.getNumberOfCopies() + 1);
							log.debug("Book returned to library.");
							sweepWaitingList();
							return true;
						} else
							return false;
					} else
						return false;
				}
			}
		}
	}

	public List<Book> findItem(String itemName)/* throws RemoteException */ {
		log.debug("Inside findItem(String itemName) method.");
		log.debug("call parameters: itemName-" + itemName);
		// look in map and return it.
		List<Book> books;
		synchronized (bookDB) {
			books = new ArrayList<>();
			Book book;
			for (Iterator<String> iterator = bookDB.keySet().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				book = bookDB.get(string);
				if (book.getName().equalsIgnoreCase(itemName))
					books.add(book);
			}
		}
		log.debug("no of books found with itemName are " + books.size());
		return books;

	}

	// 0 if user is not from this Library else 1
	public int borrowBook(String userID, String itemID, int thisLibraryUser)/* throws RemoteException */ {
		log.debug("Inside borrowBook(String userID, String itemID, int thisLibraryUser) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID + " ,thisLibraryUser-" + thisLibraryUser);
		// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
		// book is borrowed and 2 if user can't
		// borrow more items from this library.
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				if (!bookDB.containsKey(itemID))
					return -1;
				else {
					// updated as per the requirements that -
					// a user can borrow multiple items in their own library, but only 1 item from
					// each of the other libraries.
					if (thisLibraryUser == 0 && userAlreadyHaveBook(userID)) {
						log.debug(
								"This user belongs to different university and already borrowed a book from this library.");
						return 2;
					} else {
						Book book = bookDB.get(itemID);
						if (book.getNumberOfCopies() > 0) {
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
							if (borrowedBooks.containsKey(itemID)) {
								borrowedBooks.get(itemID).add(userID);
							} else {
								List<String> tempList = new LinkedList<>();
								tempList.add(userID);
								borrowedBooks.put(itemID, tempList);
							}
							log.debug("Assigned requested book to user.");
							return 1;
						} else {
							log.debug("Unable to assign requested book to user.");
							return 0;
						}
					}
				}
			}
		}
	}

	public boolean addUserToWaitingList(String userID, String itemID) {
		log.debug("Inside addUserToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		synchronized (waitingList) {
			if (waitingList.containsKey(itemID) && waitingList.get(itemID) != null) {
				waitingList.get(itemID).add(userID);
			} else {
				List<String> userList = new ArrayList<>();
				userList.add(userID);
				waitingList.put(itemID, userList);
			}
			log.debug("Added userId to waiting list.");
			return true;
		}
	}

	private boolean userAlreadyHaveBook(String userID) {
		boolean haveBook = false;
		for (String user : borrowedBooks.keySet()) {
			if (borrowedBooks.get(user).contains(userID))
				haveBook = true;
		}

		return haveBook;
	}

	public boolean bookBorrowed(String userID, String itemID) {
		log.debug("Inside bookBorrowed(String userID, String itemID) method.");
		log.debug("call parameters: userID-" + userID + " ,itemID-" + itemID);
		synchronized (borrowedBooks) {
			if (borrowedBooks.containsKey(itemID) && borrowedBooks.get(itemID) != null
					&& borrowedBooks.get(itemID).contains(userID)) {
				log.debug("requested user has borrowed this item.");
				return true;
			} else {
				log.debug("requested user has not borrowed this item.");
				return false;
			}
		}
	}

	public boolean bookAvailable(String itemID) {
		log.debug("Inside bookAvailable(String itemID) method.");
		log.debug("call parameters: itemID-" + itemID);
		synchronized (bookDB) {
			if (bookDB.containsKey(itemID) && bookDB.get(itemID) != null
					&& bookDB.get(itemID).getNumberOfCopies() > 0) {
				log.debug("requested item is available.");
				return true;
			} else {
				log.debug("requested item is not available.");
				return false;
			}
		}
	}

	private void sweepWaitingList() {
		log.debug("Running sweep for waiting users.");
		Set<String> otherLibUsers = new HashSet<>();
		for (List<String> borrowList : borrowedBooks.values()) {
			for (String user : borrowList) {
				if (!user.startsWith(serverId))
					otherLibUsers.add(user);
			}
		}
		int userWaiting, bookCopies, iterations = 0;
		List<String> userList = null;
		List<String> assignBooksTo = new ArrayList<>();
		Book book = null;
		String userId = null;
		List<String> borrowedBookList = null;
		for (String bookId : waitingList.keySet()) {
			userList = new ArrayList<>(waitingList.get(bookId));
			book = bookDB.get(bookId);
			if (userList != null && userList.size() > 0 && book != null && book.getNumberOfCopies() > 0) {
				bookCopies = book.getNumberOfCopies();
				userWaiting = userList.size();
				iterations = userWaiting <= bookCopies ? userWaiting : bookCopies;
				borrowedBookList = borrowedBooks.get(bookId);

				for (int i = 0; i < iterations && userList.size() > i; i++) {
					userId = userList.get(i);
					log.debug("User " + userId + " is waiting for " + book.getId() + ".");
					if (userId.startsWith(serverId) || !otherLibUsers.contains(userId)) {
						if (borrowedBookList != null) {
							borrowedBookList.add(userId);
							// userList.remove(i);
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
						} else {
							List<String> tempUserList = new LinkedList<>();
							tempUserList.add(userId);
							// userList.remove(i);
							book.setNumberOfCopies(book.getNumberOfCopies() - 1);
							borrowedBooks.put(bookId, tempUserList);
						}
						if (!userId.startsWith(serverId))
							otherLibUsers.add(userId);
						assignBooksTo.add(userId);
						log.debug("Assigned " + book.getId() + " to " + userId + ".");
					} else {
						i--;
						userList.remove(userId);
						log.debug(userId + " already borrowed a book from this library.");
					}
				}
			}
			for (String user : assignBooksTo) {
				waitingList.get(bookId).remove(user);
			}
		}
		log.debug("waiting list sweep complete.");
	}

	/*public boolean updateDatabase(String host1, String host2) {
		synchronized (this) {
			log.debug("inside updateDatabase() method.");
			log.debug("call parameters: host1-" + host1 + " host2-" + host2);
			String[] details = host1.split("#");
			String ip = details[1];
			int port = Integer.parseInt(details[2]);
			byte[] replyBytes = new byte[5000];
			String book_database_string = null, waiting_list_string = null, borrowed_books_string = null,
					seuqncerNumber = null;
			try (DatagramSocket socket = new DatagramSocket()) {
				log.debug("Invoking method on " + details[0] + " McGill server.");
				String request = OperationsEnum.UPDATE_DB.name();
				DatagramPacket packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
						InetAddress.getByName(ip), port);
				socket.send(packet);
				packet = new DatagramPacket(replyBytes, replyBytes.length);
				socket.receive(packet);
				ip = packet.getAddress().getHostAddress();
				port = packet.getPort();
				log.debug("temporary connection stablished " + details[0] + " McGill server.");
				boolean serverReply = new String(replyBytes).trim().equalsIgnoreCase("TRUE") ? true : false;

				if (serverReply) {

					// get book db values
					log.debug("getting book database details.");
					request = DBTypeEnum.BOOK_DATABASE.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					socket.send(packet);
					replyBytes = new byte[5000];
					socket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					book_database_string = new String(replyBytes).trim();
					log.debug("server reply - " + book_database_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.BOOK_DATABASE, book_database_string);

					// get borrow list values
					log.debug("getting borrowed books database details.");
					request = DBTypeEnum.BORROW_LIST.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					socket.send(packet);
					replyBytes = new byte[5000];
					socket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					borrowed_books_string = new String(replyBytes).trim();
					log.debug("server reply - " + borrowed_books_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.BORROW_LIST, borrowed_books_string);

					// get waiting values
					log.debug("getting waiting list database details.");
					request = DBTypeEnum.WAITING_LIST.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					socket.send(packet);
					replyBytes = new byte[5000];
					socket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					waiting_list_string = new String(replyBytes).trim();
					log.debug("server reply - " + waiting_list_string);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.WAITING_LIST, waiting_list_string);

					// get sequencer number values
					log.debug("getting sequencer number.");
					request = DBTypeEnum.SEQUENCER_NUMBER.name();
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					socket.send(packet);
					replyBytes = new byte[5000];
					socket.receive(new DatagramPacket(replyBytes, replyBytes.length));
					seuqncerNumber = new String(replyBytes).trim();
					log.debug("server reply - " + seuqncerNumber);
					// unmarshall it and update DB
					unmarshallData(DBTypeEnum.SEQUENCER_NUMBER, seuqncerNumber);

					log.debug("Server DB is updated.");
					// final reply to free host1 system out of synchronization
					request = "TRUE";
					packet = new DatagramPacket(request.getBytes(), request.getBytes().length,
							InetAddress.getByName(ip), port);
					socket.send(packet);
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

		return true;
	}

	private void unmarshallData(DBTypeEnum dbTypeEnum, String data) {
		log.debug("inside unmarshallData() method.");
		log.debug("method params are: dbTypeEnum- " + dbTypeEnum.name() + " data- " + data);
		String[] dataArray = data.split("@");
		switch (dbTypeEnum) {

		case BOOK_DATABASE:
			String[] bookDetails;
			Book tempBook;
			bookDB.clear();
			if (dataArray.length != 0) {
				for (String record : dataArray) {
					bookDetails = record.split("#");
					tempBook = new Book(bookDetails[0], bookDetails[1], Integer.parseInt(bookDetails[2]));
					bookDB.put(tempBook.getId(), tempBook);
				}
			}
			log.debug("Updated Books database.");

			break;

		case BORROW_LIST:
			String[] borrowedDetails;
			List<String> userList;
			borrowedBooks.clear();
			for (String record : dataArray) {
				userList = new ArrayList<>();
				borrowedDetails = record.split("#");
				for (int i = 1; i < borrowedDetails.length; i++) {
					userList.add(borrowedDetails[i]);
				}
				borrowedBooks.put(borrowedDetails[0], userList);
			}
			log.debug("Updated borrowed books details.");

			break;

		case WAITING_LIST:
			String[] waitingDetails;
			List<String> waitingUserList;
			waitingList.clear();
			for (String record : dataArray) {
				waitingUserList = new ArrayList<>();
				waitingDetails = record.split("#");
				for (int i = 1; i < waitingDetails.length; i++) {
					waitingUserList.add(waitingDetails[i]);
				}
				waitingList.put(waitingDetails[0], waitingUserList);
			}
			log.debug("Updated borrowed books details.");

			break;

		case SEQUENCER_NUMBER:
			UDPCSListener.setSequencer(Integer.parseInt(data.trim()));
			break;

		}

	}

	public void getData(InetAddress address, int port) {
		synchronized (this) {
			log.debug("inside getData() method.");
			log.debug("reqest params: address-" + address.getHostAddress() + " port-" + port);
			try (DatagramSocket socket = new DatagramSocket()) {
				byte[] byteBuffer = new byte[5000];
				String replyString = "TRUE";
				DatagramPacket replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length,
						address, port);
				socket.send(replyPacket);
				log.debug("sent reply as " + replyString);

				byteBuffer = new byte[5000];
				DatagramPacket requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				log.debug("received request for " + new String(requestPacket.getData()).trim() + " database.");
				DBTypeEnum dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData()).trim());
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				log.debug("replying as - " + replyString);
				socket.send(replyPacket);

				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				log.debug("received request for " + new String(requestPacket.getData()).trim() + " database.");
				dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData()).trim());
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				log.debug("replying as - " + replyString);
				socket.send(replyPacket);

				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				log.debug("received request for " + new String(requestPacket.getData()).trim() + " database.");
				dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData()).trim());
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				log.debug("replying as - " + replyString);
				socket.send(replyPacket);

				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				log.debug("received request for " + new String(requestPacket.getData()).trim() + " database.");
				dbTypeEnum = DBTypeEnum.valueOf(new String(requestPacket.getData()).trim());
				replyString = marshallData(dbTypeEnum);
				replyPacket = new DatagramPacket(replyString.getBytes(), replyString.getBytes().length, address, port);
				log.debug("replying as - " + replyString);
				socket.send(replyPacket);

				// waiting for final receive
				byteBuffer = new byte[5000];
				requestPacket = new DatagramPacket(byteBuffer, byteBuffer.length);
				socket.receive(requestPacket);
				if (new String(requestPacket.getData()).trim().equals("TRUE")) {
					log.debug("request server database is updated.");
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

	private String marshallData(DBTypeEnum dbTypeEnum) {
		log.debug("inside marshallData() method.");
		log.debug("method params are: dbTypeEnum- " + dbTypeEnum.name());
		StringBuilder returnString = new StringBuilder();
		switch (dbTypeEnum) {
		case BOOK_DATABASE:
			for (Book book : bookDB.values()) {
				returnString.append(book.getId() + "#").append(book.getName() + "#")
						.append(book.getNumberOfCopies() + "@");
			}
			break;

		case BORROW_LIST:
			for (String bookId : borrowedBooks.keySet()) {
				returnString.append(bookId + "#");
				StringBuilder temp = new StringBuilder();
				for (String userId : borrowedBooks.get(bookId)) {
					temp.append(userId + "#");
				}
				returnString .append(temp.substring(0,temp.length()-1)) .append("@");
			}
			break;

		case WAITING_LIST:
			for (String bookId : waitingList.keySet()) {
				returnString.append(bookId + "#");
				StringBuilder temp = new StringBuilder();
				for (String userId : waitingList.get(bookId)) {
					temp.append(userId + "#");
				}
				returnString .append(temp.substring(0,temp.length()-1)) .append("@");
			}
			break;

		case SEQUENCER_NUMBER:
			returnString.append(UDPCSListener.getSequencer());
			break;

		}

		if (dbTypeEnum.equals(DBTypeEnum.SEQUENCER_NUMBER)) {
			log.debug("returning result as " + returnString);
			return returnString.toString();
		} else {
			log.debug("returning result as " + (returnString.toString().trim().length() > 0
					? returnString.substring(0, returnString.length() - 1).toString()
					: ""));
			return returnString.toString().trim().length() > 0
					? returnString.substring(0, returnString.length() - 1).toString()
					: "";
		}
	}*/
}
