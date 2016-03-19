package com.renhua.logAnalyze.email;

/**
 * @author renhua
 * 报警者的bean
 * 2016年3月19日
 */
public class Sender {
	private String address;
	private String username;
	private String password;
	private String domain;
	
	public Sender(String address, String password) {
		if(address == null || address.isEmpty() ||
			password == null || password.isEmpty()) {
			return;
		}else{
			this.address = address;
			String[] s = address.split("@");
			this.username = s[0];
			this.password = password;
			this.domain = s[1];
		}
	}

	public String getAddress() {
		return address;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getDomain() {
		return domain;
	}
	
	
}
