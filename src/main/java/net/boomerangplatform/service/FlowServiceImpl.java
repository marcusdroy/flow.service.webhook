package net.boomerangplatform.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.boomerangplatform.model.EventPayload;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.security.service.ApiTokenService;

@Service
public class FlowServiceImpl implements FlowService {

	@Value("${flow.executeworkflow.url}")
	private String submitWorkflow;
	
	@Value("${flow.status.url}")
	private String activityStatus;

	@Autowired
	private FlowWorkflowService flowWorkflowService;
	
	@Autowired
	private FlowWorkflowActivityService flowActivtyService;

	@Autowired
	private ApiTokenService apiTokenService;
	
	@Autowired
	@Qualifier("internalRestTemplate")
	private RestTemplate restTemplate;

	@Override
	public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request) {
		String tokenId = request.getToken();

		FlowWorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);

		String workflowId = entity.getId();

		String url = submitWorkflow.replace("{workflow.id}", workflowId) + "?trigger=webhook";

		FlowExecutionRequest executionRequest = new FlowExecutionRequest();

		executionRequest.setProperties(request.getProperties());

		final HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + apiTokenService.createJWTToken());
		final HttpEntity<FlowExecutionRequest> req = new HttpEntity<>(executionRequest, headers);
	
		ResponseEntity<FlowActivity> responseEntity = restTemplate.exchange(url, HttpMethod.POST, req,
				FlowActivity.class);
		
		FlowActivity activity = responseEntity.getBody();
		FlowWebhookResponse response = new FlowWebhookResponse();
		if(activity != null) {
		  response.setActivityId(activity.getId());
		}
		return response;
	}

	@Override
	public void submitListenerEvent(String topic, EventPayload eventPayload) {

		List<FlowWorkflowEntity> workflows = flowWorkflowService.getEventWorkflowsForTopic(topic);

		for (FlowWorkflowEntity entity : workflows) {
			String workflowId = entity.getId();

			String url = submitWorkflow.replace("{workflow.id}", workflowId) + "?trigger=action";

			FlowExecutionRequest executionRequest = new FlowExecutionRequest();

			executionRequest.setProperties(eventPayload.getEvent().getDetails());
			final HttpHeaders headers = new HttpHeaders();
			headers.add("Authorization", "Bearer " + apiTokenService.createJWTToken());
			final HttpEntity<FlowExecutionRequest> req = new HttpEntity<>(executionRequest, headers);
			
			ResponseEntity<FlowActivity> responseEntity = restTemplate.exchange(url, HttpMethod.POST, req,
					FlowActivity.class);
			responseEntity.getBody();
		}

	}

	@Override
	public FlowActivity getFlowActivity(String token, String activityId) {
		String tokenId = token;
		FlowWorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);
		
		if (entity == null) {
			return null;
		}
		
		return getActivityById(activityId);
	}

  private FlowActivity getActivityById(String activityId) {
    String url = activityStatus.replace("{activity.id}", activityId);
		FlowExecutionRequest executionRequest = new FlowExecutionRequest();
		
		final HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + apiTokenService.createJWTToken());
		final HttpEntity<FlowExecutionRequest> req = new HttpEntity<>(executionRequest, headers);
		ResponseEntity<FlowActivity> responseEntity = restTemplate.exchange(url, HttpMethod.GET, req,
				FlowActivity.class);
		FlowActivity activity = responseEntity.getBody();
		return activity;
  }

  @Override
  public FlowActivity getFlowActivityViaProperty(String token, String key, String value) {
    String tokenId = token;
    FlowWorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);
    
    if (entity == null) {
        return null;
    }
    
    FlowWorkflowActivityEntity activity = flowActivtyService.findByWorkflowAndProperty(entity.getId(), key, value);
    
    if (activity == null) 
    {
      return null;
    }
    return getActivityById(activity.getId());
  }
}
