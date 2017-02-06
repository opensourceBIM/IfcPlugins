package org.bimserver.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileName {
	private String name;
	private Date timestamp;
	private List<String> authors = new ArrayList<>();
	private List<String> organizations = new ArrayList<>();
	private List<String> preprocessors = new ArrayList<>();
	private String preprocessorVersion;
	private String originatingSystem;
	private String authorization;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public List<String> getAuthors() {
		return authors;
	}

	public void setAuthors(List<String> authors) {
		this.authors = authors;
	}

	public List<String> getOrganization() {
		return organizations;
	}

	public void setOrganization(List<String> organization) {
		this.organizations = organization;
	}

	public List<String> getPreprocessors() {
		return preprocessors;
	}

	public void setPreprocessors(List<String> preprocessors) {
		this.preprocessors = preprocessors;
	}

	public String getPreprocessorVersion() {
		return preprocessorVersion;
	}

	public void setPreprocessorVersion(String preprocessorVersion) {
		this.preprocessorVersion = preprocessorVersion;
	}

	public String getOriginatingSystem() {
		return originatingSystem;
	}

	public void setOriginatingSystem(String originatingSystem) {
		this.originatingSystem = originatingSystem;
	}

	public String getAuthorization() {
		return authorization;
	}

	public void setAuthorization(String authorization) {
		this.authorization = authorization;
	}

	public void addAuthor(String author) {
		authors.add(author);
	}

	public void addOrganization(String organization) {
		this.organizations.add(organization);
	}
}
