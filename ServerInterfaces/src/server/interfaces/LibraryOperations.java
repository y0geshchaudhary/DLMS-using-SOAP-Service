package server.interfaces;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import server.model.Book;
import server.model.GeneralException;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface LibraryOperations {
	
	public String userExists(String userId);
	public boolean addItem(String managerID, String itemID, String itemName, int quantity) throws GeneralException;
	public int removeItem(String managerID, String itemID, int quantity) throws GeneralException;
	public Book[] listAvailableItems(String managerID) throws GeneralException;
	public int borrowItem(String userID, String itemID) throws GeneralException;
	public Book[] findItem(String userID, String itemName) throws GeneralException;
	public boolean returnItem(String userID, String itemID) throws GeneralException;
	public boolean addToWaitingList(String userID, String itemID) throws GeneralException;
	public int exchangeItemDuplicate(String userID, String oldItemID, String newItemID) throws GeneralException;
	

}
