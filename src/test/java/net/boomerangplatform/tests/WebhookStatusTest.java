package net.boomerangplatform.tests;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import net.boomerangplatform.Application;
import net.boomerangplatform.MongoConfig;
import net.boomerangplatform.controller.WebhookController;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {Application.class, MongoConfig.class})
@SpringBootTest
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class WebhookStatusTest extends FlowWebhookTests {

  @Autowired
  private WebhookController webhookController;

  private String token = "A5DF2F840C0DFF496D516B4F75BD947C9BC44756A8AE8571FC45FCB064323641";

  @Before
  public void setUp() {
    super.setUp();
    mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();
    try {
      mockServer
          .expect(times(1), requestTo(containsString("flow/activity/5de7f314e638b70001fa9c84")))
          .andExpect(method(HttpMethod.GET)).andRespond(
              withSuccess(getMockFile("mock/flow/executeflow.json"), MediaType.APPLICATION_JSON));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testPayload() {

    FlowActivity flowActivity =
        webhookController.getWebhookStatus("5de7f314e638b70001fa9c84", token);
    assertNotNull(flowActivity);
    assertNotNull(flowActivity.getId());
  }

}
