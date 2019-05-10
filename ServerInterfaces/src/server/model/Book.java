package server.model;

public class Book {

	private String name;
	private String id;
	private int numberOfCopies;

	public Book() {};
	public Book(String id, String name, int numberOfCopies) {
		this.name = name;
		this.id = id;
		this.numberOfCopies = numberOfCopies;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getNumberOfCopies() {
		return numberOfCopies;
	}

	public void setNumberOfCopies(int numberOfCopies) {
		this.numberOfCopies = numberOfCopies;
	}

}
