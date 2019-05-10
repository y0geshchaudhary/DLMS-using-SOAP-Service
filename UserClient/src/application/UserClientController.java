package application;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server.interfaces.LibraryOperations;
import server.interfaces.PublishURLEnum;
import server.model.Book;
import server.model.GeneralException;

public class UserClientController {

	@FXML
	private TextField userIdTF;

	@FXML
	private ChoiceBox<String> operationDD;

	@FXML
	private TextField itemIdTF;

	@FXML
	private TextField itemNameTF;

	@FXML
	private Button goButton;

	@FXML
	private Button quitButton;

	@FXML
	private Label errorLabel;

	@FXML
	private TextArea outputTA;

	@FXML
	private TextField newItemIdTF;

	private List<String> operations = new ArrayList<>();
	private static final Logger logger = Logger.getLogger(UserClientController.class.getName());
	private SimpleFormatter logFormatter = new SimpleFormatter();

	public void setup() {
		Collections.addAll(operations, "Borrow Item", "Find Item", "Return Item", "Exchange Items");
		operationDD.setItems(FXCollections.observableList(operations));
		operationDD.setValue(operations.get(0));
		logger.setLevel(Level.ALL);

	}

	@FXML
	void quit(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	void performAction(ActionEvent event) {
		FileHandler fileHanlder = null;
		errorLabel.setText("");
		outputTA.setText("");
		if (validate()) {
			String action = operationDD.getValue().trim();
			String userId = userIdTF.getText().trim();
			String itemId = itemIdTF.getText().trim();
			String itemName = itemNameTF.getText().trim();
			String newItemId = newItemIdTF.getText().trim();
			LibraryOperations libraryOperations = null;
			
			URL url = null;
			try {
				url = new URL(PublishURLEnum.valueOf(userId.substring(0, 3)).url);
			} catch (MalformedURLException e1) {
				System.out.println("MalformedURLException exception occured.");
				e1.printStackTrace();
			}
			QName qName = new QName("http://model.server/", "LibraryOperationsImplService");
			Service service = Service.create(url, qName);
			
			libraryOperations = service.getPort(LibraryOperations.class);

			int intResult = -2;
			boolean boolResult;
			String stringResult = "";

			String userExists = libraryOperations.userExists(userId);

			if (userExists.equalsIgnoreCase("U")) {
				String filename = "Logs/" + userId;
				// configure logger
				try {
					fileHanlder = new FileHandler(filename, true);
					fileHanlder.setLevel(Level.ALL);
					fileHanlder.setFormatter(logFormatter);
					logger.addHandler(fileHanlder);
				} catch (Exception e) {
					errorLabel.setText("Issue with log file.");
					e.printStackTrace();
				}
				logger.info("NEW REQUEST");
				logger.info("Request type:" + action);

				/*switch (action) {
				case "Borrow Item":
					logger.info("Item requested: " + itemId);
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
						// book is borrowed and 2 if user can't
						// borrow more items from this library.
						try {
							intResult = libraryOperations.borrowItem(userId, itemId);
						} catch (GeneralException e) {
							outputTA.setText(e.reason);
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
						if (intResult == -1) {
							outputTA.setText("The book doesn't exist in library.");
							logger.info("Received response as -1 which indicate that the book doesn't exist.");
						} else if (intResult == 0) {
							logger.info("Received response as 0 which indicate that the book is out of stock.");
							// popup for user to let decide if he want his name to be added to waiting list.
							Alert alert = new Alert(AlertType.CONFIRMATION);
							// alert.setTitle("Confirmation Dialog");
							alert.setHeaderText("This Book is not available this time.");
							alert.setContentText("Do you want to be added to waiting list?");

							Optional<ButtonType> result = alert.showAndWait();
							try {
								if (result.get() == ButtonType.OK) {
									boolResult = libraryOperations.addToWaitingList(userId, itemId);
									if (boolResult) {
										logger.info("user opt to get enrolled into waiting list for item" + itemId);
										outputTA.setText("Added " + userId + " to waiting list for this book.");
									} else {
										logger.info("Unable to add " + userId + " to waiting list.");
										outputTA.setText("Unable to add " + userId + " to waiting list.");
									}
								} else {
									logger.info("user didn't want to be added to waiting list.");
									outputTA.setText(
											"This book can't be borrowed this time and userId is not added to waiting list.");
								}

							} catch (GeneralException e) {
								outputTA.setText(e.reason);
								logger.info(e.reason + "\n" + e.getLocalizedMessage());
								e.printStackTrace();
								return;
							}
						} else if (intResult == 1) {
							logger.info("Received response as 1 which indicate that the book is issues to user.");
							outputTA.setText("Book is issued to " + userId);
						} else if (intResult == 2) {
							logger.info("Received response as 2 which indicate that " + userId
									+ " has already borrowed a book from " + itemId.substring(0, 3) + "server.");
							outputTA.setText("This user already borrowed a book from this library.");
						}
					} else {
						errorLabel.setText("Enter valid itemId.");
						logger.info("Item requested is invalid.");
					}
					break;

				case "Find Item":
					logger.info("Item Requested: " + itemName);
					try {
						Book[] books = libraryOperations.findItem(userId, itemName);
						for (Book book : books) {
							stringResult = stringResult.concat(book.getId().concat(" ")
									.concat(String.valueOf(book.getNumberOfCopies()).concat(", ")));
						}
						stringResult = stringResult.substring(0, stringResult.length() - 2);
						logger.info("Details recieved : " + stringResult);
						outputTA.setText(stringResult);
					} catch (GeneralException e) {
						outputTA.setText(e.reason);
						logger.info(e.reason + "\n" + e.getLocalizedMessage());
						e.printStackTrace();
						return;
					}
					break;

				case "Return Item":
					logger.info("Item to return : " + itemId);
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						try {
							boolResult = libraryOperations.returnItem(userId, itemId);
							logger.info("Response received from server : " + stringResult);
							stringResult = boolResult ? "Book returned to library successfully."
									: "Unable to return book to library";
							outputTA.setText(stringResult);
						} catch (GeneralException e) {
							outputTA.setText(e.reason);
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
					} else {
						logger.info(itemId + " is not a valid itemId.");
						errorLabel.setText("Enter valid itemId.");
					}
					break;

				case "Exchange Items":
					logger.info("Items requested to exchange: " + oldItemId + " with " + itemId);
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						*//**
						 * return 0 if book wasn't available or user doesn't belong to new book library
						 * and want to exchange book from same library or wasn't able to exchange book
						 * due to any reason, 1 if exchange was successful and -1 if user didn't
						 * borrowed the old item.
						 *//*
						try {
							intResult = libraryOperations.exchangeItem(userId, itemId, oldItemId);
						} catch (GeneralException e) {
							outputTA.setText(e.reason);
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
						if (intResult == -1) {
							outputTA.setText(oldItemId + " book is not borrowed by this user.");
							logger.info("Received response as -1 which indicate that " + oldItemId
									+ " book is not borrowed by this user.");
						} else if (intResult == 0) {
							logger.info(
									"Received response as 0 which indicate due to some reason book exchange wasn't successful.");
							// popup for user to let decide if he want his name to be added to waiting list.
							Alert alert = new Alert(AlertType.CONFIRMATION);
							alert.setHeaderText("This Book exchange was not successful at this moment.");
							alert.setContentText(
									"Do you want to return old book and be added to waiting list for new book?");

							Optional<ButtonType> result = alert.showAndWait();
							try {
								if (result.get() == ButtonType.OK) {
									boolResult = libraryOperations.addToWaitingListOverloaded(userId, itemId,
											oldItemId);
									if (boolResult) {
										logger.info("user opt to get enrolled into waiting list for item" + itemId);
										outputTA.setText("Returned " + oldItemId + "and added " + userId
												+ " to waiting list of " + itemId + " book.");
									} else {
										logger.info("Unable to perform exchange succesfully.");
										outputTA.setText("Unable to perform exchange succesfully.");
									}
								} else {
									logger.info("user didn't want to be added to waiting list.");
									outputTA.setText(
											"Unable to do exchange this time and user didn't want to be added to new book wait list.");
								}

							} catch (GeneralException e) {
								outputTA.setText(e.reason);
								logger.info(e.reason + "\n" + e.getLocalizedMessage());
								e.printStackTrace();
								return;
							}
						} else if (intResult == 1) {
							logger.info("Received response as 1 which indicate that the book is issues to user.");
							outputTA.setText("Book exchange is successful." + userId);
						} else if (intResult == 2) {
							logger.info("Received response as 2 which indicate that " + userId
									+ " has already borrowed a book from " + itemId.substring(0, 3) + "server.");
							outputTA.setText("This user already borrowed a book from this library.");
						}
					} else {
						errorLabel.setText("Enter valid itemId.");
						logger.info("Item requested is invalid.");
					}
					break;
				}*/
				
				switch (action) {
				case "Borrow Item":
					logger.info("Item requested: " + itemId);
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
						// book is borrowed and 2 if user can't
						// borrow more items from this library.
						try {
							intResult = libraryOperations.borrowItem(userId, itemId);
						} catch (GeneralException e) {
							outputTA.setText("Exception occured.");
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
						if (intResult == -1) {
							outputTA.setText("The operation is not succesfull.");
							logger.info("Received response as -1 which indicate that the operation is not succesfull.");
						} else if (intResult == 0) {
							logger.info("Received response as 0 which indicate that the book is out of stock.");
							// popup for user to let decide if he want his name to be added to waiting list.
							Alert alert = new Alert(AlertType.CONFIRMATION);
							// alert.setTitle("Confirmation Dialog");
							alert.setHeaderText("This Book is not available this time.");
							alert.setContentText("Do you want to be added to waiting list?");

							Optional<ButtonType> result = alert.showAndWait();
							try {
								if (result.get() == ButtonType.OK) {
									boolResult = libraryOperations.addToWaitingList(userId, itemId);
									if (boolResult) {
										logger.info("user opt to get enrolled into waiting list for item" + itemId);
										outputTA.setText("Added " + userId + " to waiting list for this book.");
									} else {
										logger.info("Unable to add " + userId + " to waiting list.");
										outputTA.setText("Unable to add " + userId + " to waiting list.");
									}
								} else {
									logger.info("user didn't want to be added to waiting list.");
									outputTA.setText(
											"This book can't be borrowed this time and userId is not added to waiting list.");
								}

							} catch (GeneralException e) {
								outputTA.setText(e.reason);
								logger.info(e.reason + "\n" + e.getLocalizedMessage());
								e.printStackTrace();
								return;
							}
						} else if (intResult == 1) {
							logger.info("Received response as 1 which indicate that the book is issues to user.");
							outputTA.setText("Book is issued to " + userId);
						} else if (intResult == 2) {
							logger.info("Received response as 2 which indicate that " + userId
									+ " has already borrowed a book from " + itemId.substring(0, 3) + "server.");
							outputTA.setText("This user already borrowed a book from this library.");
						}
					} else {
						errorLabel.setText("Enter valid itemId.");
						logger.info("Item requested is invalid.");
					}
					break;

				case "Find Item":
					logger.info("Item Requested: " + itemName);
					try {
						Book[] books = libraryOperations.findItem(userId, itemName);
						for (Book book : books) {
							stringResult = stringResult.concat(book.getId().concat(" ")
									.concat(String.valueOf(book.getNumberOfCopies()).concat(", ")));
						}
						stringResult = books.length>0?stringResult.substring(0, stringResult.length() - 2): "No book found.";
						logger.info("Details recieved : " + stringResult);
						outputTA.setText("Books found:\n"+stringResult);
					} catch (GeneralException e) {
						outputTA.setText(e.reason);
						logger.info(e.reason + "\n" + e.getLocalizedMessage());
						e.printStackTrace();
						return;
					}
					break;

				case "Return Item":
					logger.info("Item to return : " + itemId);
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						try {
							boolResult = libraryOperations.returnItem(userId, itemId);
							logger.info("Response received from server : " + boolResult);
							stringResult = boolResult ? "Book returned to library successfully."
									: "Unable to return book to library";
							outputTA.setText(stringResult);
						} catch (GeneralException e) {
							outputTA.setText("Exception occured.");
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
					} else {
						logger.info(itemId + " is not a valid itemId.");
						errorLabel.setText("Enter valid itemId.");
					}
					break;

				case "Exchange Items":
					logger.info("Items requested to exchange: " + itemId + " with " + newItemId);
						/**
						 * return 0 if book wasn't available or user doesn't belong to new book library
						 * and want to exchange book from same library or wasn't able to exchange book
						 * due to any reason, 1 if exchange was successful and -1 if user didn't
						 * borrowed the old item.
						 */
					if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
						try {
							intResult = libraryOperations.exchangeItemDuplicate(userId, itemId, newItemId);
							logger.info("Response received from server : " + intResult);
							if(intResult == 1) {
								stringResult = "Book exchanged successfully.";
							}else if(intResult == 0) {
								stringResult = "Unable to exchange book.";
							}else {
								stringResult = "NA";
							}
								
							outputTA.setText(stringResult);
						} catch (GeneralException e) {
							outputTA.setText(e.reason);
							logger.info(e.reason + "\n" + e.getLocalizedMessage());
							e.printStackTrace();
							return;
						}
					}
					else {
						errorLabel.setText("Enter valid itemId.");
						logger.info("Item requested is invalid.");
					}
					break;
				}
			} else
				errorLabel.setText("This userId is not found in Library database.");
		}
		if (fileHanlder != null)
			fileHanlder.close();
	}

	private boolean validate() {
		errorLabel.setText("");
		boolean result = false;
		String userId = userIdTF.getText().trim();
		if (userId.length() > 0 && (userId.startsWith("CON") || userId.startsWith("MCG") || userId.startsWith("MON"))) {
			if (operationDD.getValue().equals(operations.get(0)) || operationDD.getValue().equals(operations.get(2))) {
				if (itemIdTF.getText().trim().length() > 0)
					result = true;
				else
					errorLabel.setText("Enter Item Id.");
			} else if (operationDD.getValue().equals(operations.get(1))) {
				if (itemNameTF.getText().trim().length() > 0)
					result = true;
				else
					errorLabel.setText("Enter Item Name.");
			} else if (operationDD.getValue().equals(operations.get(3))) {
				if (itemIdTF.getText().trim().length() > 0 && newItemIdTF.getText().trim().length() > 0)
					result = true;
				else
					errorLabel.setText("ItemId or oldItemId is not valid.");
			}
		} else {
			errorLabel.setText("Enter valid user Id.");
		}

		return result;
	}
}
