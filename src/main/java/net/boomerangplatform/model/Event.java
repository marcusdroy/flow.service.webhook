package net.boomerangplatform.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Event {

	private Map<String, String> details = new LinkedHashMap<>();

	public Map<String, String> getDetails() {
		return details;
	}

	@JsonAnySetter
	public void setDetail(String key, String value) {
		details.put(key, value);
	}
}
