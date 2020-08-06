package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.service.FlowService;
import net.boomerangplatform.service.SlackService;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

  @Autowired
  private FlowService flowService;

  @Autowired
  private SlackService slackService;
  
  @Value("${slack.webhook.response}")
  private String slackResponse;

  @PostMapping(value = "/payload", consumes = "application/json; charset=utf-8")
  public FlowWebhookResponse submitWebhookEvent(@RequestBody RequestFlowExecution request) {
    return flowService.submitWebhookEvent(request);
  }

  @GetMapping(value = "/status/{activityId}", consumes = "application/json; charset=utf-8")
  public FlowActivity getWebhookStatus(@PathVariable String activityId,
      @RequestParam String token) {
    return flowService.getFlowActivity(token, activityId);
  }
  
  @GetMapping(value = "/status/search", consumes = "application/json; charset=utf-8")
  public FlowActivity getWebhookStatus(@RequestParam String key,
      @RequestParam String value,  @RequestParam String token) {
    return flowService.getFlowActivityViaProperty(token, key, value);
  }

  @PostMapping(path = "/slack", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public ResponseEntity<String> submitSlackEvent(
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) {
    if (slackService.validateRequest(signature,timestamp)) {
      slackService.acceptSlackEvent(slackEvent);
      return new ResponseEntity<>(slackResponse, HttpStatus.OK); 
    }
    return new ResponseEntity<>("OK", HttpStatus.UNAUTHORIZED);
  }

}
