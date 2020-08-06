package net.boomerangplatform.tests;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import net.boomerangplatform.AbstractBoomerangTest;

public class FlowWebhookTests extends AbstractBoomerangTest  {

	protected MockRestServiceServer mockServer;

	@Autowired
	@Qualifier("internalRestTemplate")
	protected RestTemplate restTemplate;

	@Override
	protected String[] getCollections() {
		return new String[] { "flow_workflows" };
	}
	
	@Before
	public void setUp() {
		super.setUp();
		mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();
	}

	@Override
	protected Map<String, List<String>> getData() {
		LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
		
		data.put("flow_workflows",
				Arrays.asList("db/flow_workflows/workflow1.json"));
		return data;
	}
}
