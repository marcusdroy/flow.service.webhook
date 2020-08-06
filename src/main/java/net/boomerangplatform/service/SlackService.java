package net.boomerangplatform.service;

import org.springframework.util.MultiValueMap;

public interface SlackService {
  
  public void acceptSlackEvent(MultiValueMap<String,String> slackEvent);
  
  public boolean validateRequest(String slackSiganture, String timestamp);

}
