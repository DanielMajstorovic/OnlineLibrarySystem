package org.unibl.etf.mdp.models;

import java.util.Objects;

public class Clan {
	private String firstName;
	private String lastName;
	private String address;
	private String email;
	private String username;
	private String password;
	private boolean approved;
	
	public Clan() {
		// TODO Auto-generated constructor stub
	}
	
	public Clan(String firstName, String lastName, String address, String email, String username, String password,
			boolean approved) {
		super();
		this.firstName = firstName;
		this.lastName = lastName;
		this.address = address;
		this.email = email;
		this.username = username;
		this.password = password;
		this.approved = approved;
	}
	
	
	@Override
	public String toString() {
		return "Clan [firstName=" + firstName + ", lastName=" + lastName + ", address=" + address + ", email=" + email
				+ ", username=" + username + ", password=" + password + ", approved=" + approved
				+ "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, approved, email, firstName, lastName, password, username);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Clan other = (Clan) obj;
		return Objects.equals(address, other.address) && approved == other.approved
				&& Objects.equals(email, other.email) && Objects.equals(firstName, other.firstName)
				&& Objects.equals(lastName, other.lastName) && Objects.equals(password, other.password)
				&& Objects.equals(username, other.username);
	}

	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isApproved() {
		return approved;
	}
	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	
}
