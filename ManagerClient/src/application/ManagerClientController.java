package application;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server.interfaces.LibraryOperations;
import server.interfaces.PublishURLEnum;
import server.model.Book;
import server.model.GeneralException;

public class ManagerClientController {

	@FXML
	private TextField managerIdTF;

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
	private TextField quantityTF;

	private List<String> operations = new ArrayList<>();
	private static final Logger logger = Logger.getLogger(ManagerClientController.class.getName());
	private SimpleFormatter logFormatter = new SimpleFormatter();

	public void setup() {
		Collections.addAll(operations, "Add Item", "Remove Item", "List Item");
		operationDD.setItems(FXCollections.observableList(operations));
		operationDD.setValue(operations.get(0));
		logger.setLevel(Level.ALL);
	}

	@FXML
	void performAction(ActionEvent event) {
		FileHandler fileHanlder = null;
		errorLabel.setText("");
		outputTA.setText("");
		if (validate()) {
			String action = operationDD.getValue().trim();
			String managerId = managerIdTF.getText().trim();
			String itemId = itemIdTF.getText().trim();
			String itemName = itemNameTF.getText().trim();
			LibraryOperations libraryOperations = null;
			
			URL url = null;
			try {
				url = new URL(PublishURLEnum.valueOf(managerId.substring(0, 3)).url);
			} catch (MalformedURLException e1) {
				System.out.println("MalformedURLException exception occured.");
				e1.printStackTrace();
			}
			QName qName = new QName("http://model.server/", "LibraryOperationsImplService");
			Service service = Service.create(url, qName);
			
			libraryOperations = service.getPort(LibraryOperations.class);

			int intResult = -2;
			boolean boolResult;
			String stringResult = null;
			
			String userExists = libraryOperations.userExists(managerId); 

			if (userExists.equalsIgnoreCase("M")) {
				String filename = "Logs/" + managerId;
				// configure logger

				try {
					fileHanlder = new FileHandler(filename, true);
					fileHanlder.setLevel(Level.ALL);
					fileHanlder.setFormatter(logFormatter);
					logger.addHandler(fileHanlder);
				} catch (SecurityException | IOException e1) {
					errorLabel.setText("Issue with log file.");
					e1.printStackTrace();
				}

				logger.info("NEW REQUEST");
				logger.info("Request type:" + action);

				switch (action) {
				case "Add Item":
					int quantity = Integer.parseInt(quantityTF.getText().trim());
					logger.info(
							"Book details :\n ItemId:" + itemId + " ItemName:" + itemName + " Quantity:" + quantity);
					// either receive true when book is added or an exception is thrown by server.
					try {
						boolResult = libraryOperations.addItem(managerId, itemId, itemName, quantity);
					} catch (GeneralException e) {
						outputTA.setText(e.reason);
						logger.info(e.reason + "\n" + e.getLocalizedMessage());
						e.printStackTrace();
						return;
					}

					logger.info("Received response from server: " + boolResult);
					if (boolResult)
						outputTA.setText("Book is added to library.");
					else
						outputTA.setText("Book can't be added to library.");
					break;

				case "Remove Item":
					
					quantity = Integer.parseInt(quantityTF.getText().trim());
					logger.info("Item requested to remove: " + itemId + "\n quantity to remove: " + quantity);
					try {
						// return 0 if operation is failed, 1 if operation is successful, 2 when
						// quantity is greater than the number of copies which library have.
						intResult = libraryOperations.removeItem(managerId, itemId, quantity);
						logger.info("Response recived from server : " + intResult);
						if (intResult == 0) {
							outputTA.setText("Unable to remove item from library.");
							logger.info("Unable to remove item from library");
						} else if (intResult == 1) {
//							if (quantity == -1) {
								outputTA.setText("Item is completely deleted from library.");
								logger.info("Item is completely deleted from library.");
							/*} else if (quantity > -1) {
								outputTA.setText(
										quantity + " books related to " + itemId + " are removed from library.");
								logger.info(quantity + " books related to " + itemId + " are removed from library.");
							}*/
						} else if (intResult == 2) {
							outputTA.setText(
									quantity + " books related to " + itemId + " are removed from library.");
							logger.info(quantity + " books related to " + itemId + " are removed from library.");
						} /*else if (intResult == 3) {
							outputTA.setText(itemId + " is not found in database to delete.");
							logger.info(itemId + " is not found in database to delete.");
						}*/

					} catch (GeneralException e) {
						logger.info(e.reason + "\n" + e.getLocalizedMessage());
						outputTA.setText(e.reason);
						e.printStackTrace();
					}
					break;

				case "List Item":
					try {
						Book[] books = libraryOperations.listAvailableItems(managerId);
						StringBuilder stringBuilder = new StringBuilder();
						for (Book b : books) {
							stringBuilder.append(b.getId() + " " + b.getName() + " " + b.getNumberOfCopies() + ",\n");
						}
						stringResult = books.length>0?stringBuilder.substring(0, stringBuilder.length() - 2):"No books found.";
						logger.info("Books returned from server : " + stringResult);
						outputTA.setText("Books found:\n"+stringResult);
					} catch (GeneralException e) {
						outputTA.setText(e.reason);
						e.printStackTrace();
						logger.info(e.reason + "\n" + e.getLocalizedMessage());
					}
				}
			} else
				errorLabel.setText("This ManagerId is not valid for this operation.");
		}
		if (fileHanlder != null)
			fileHanlder.close();
	}

	@FXML
	void quit(ActionEvent event) {
		Platform.exit();
	}

	private boolean validate() {
		errorLabel.setText("");
		boolean result = false;
		String managerId = managerIdTF.getText().trim();
		if (managerId.length() > 0
				&& (managerId.startsWith("CON") || managerId.startsWith("MCG") || managerId.startsWith("MON"))) {
			if (operationDD.getValue().equals(operations.get(0))) {
				if (itemIdTF.getText().trim().startsWith(managerId.substring(0, 3))) {
					try {
						if (quantityTF.getText().trim().length() > 0
								&& Integer.parseInt(quantityTF.getText().trim()) > 0) {
							result = true;
						} else
							errorLabel.setText("Invalid quantity.");
					} catch (NumberFormatException e) {
						errorLabel.setText("Invalid quantity.");
						return false;
					}
				} else
					errorLabel.setText("Item id should start with " + managerId.substring(0, 3));

			} else if (operationDD.getValue().equals(operations.get(1))) {
				if (itemIdTF.getText().trim().startsWith(managerId.substring(0, 3))) {
					try {
						if (quantityTF.getText().trim().length() > 0) {
							int val = Integer.parseInt(quantityTF.getText().trim());
							if (val >= -1)
								result = true;
							else
								errorLabel.setText("Quantity value can't be less than -1.");
						} else
							errorLabel.setText("Enter quantity.");
					} catch (NumberFormatException e) {
						errorLabel.setText("Invalid value for quantity.");
						return false;
					}
				} else
					errorLabel.setText("Item id should start with " + managerId.substring(0, 3));
			} else if (operationDD.getValue().equals(operations.get(2)))
				result = true;
		} else {
			errorLabel.setText("Enter valid manager id.");
		}
		return result;
	}
}
