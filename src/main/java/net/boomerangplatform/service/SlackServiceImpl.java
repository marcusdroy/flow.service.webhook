package net.boomerangplatform.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import net.boomerangplatform.model.Event;
import net.boomerangplatform.model.EventPayload;

@Service
public class SlackServiceImpl implements SlackService {

  private final Logger logger = LogManager.getLogger();

  @Autowired
  private FlowService flowService;

  @Override
  @Async
  public void acceptSlackEvent(MultiValueMap<String, String> slackEvent) {
    logger.info("Accepting slack event:");


    String command = getValue("command", slackEvent);

    if (command != null) {
      logger.info("Submitting command: " + command);

      EventPayload eventPayload = new EventPayload();
      Event event = new Event();

      eventPayload.setEvent(event);

      for (Entry<String, List<String>> map : slackEvent.entrySet()) {
   
        String key = map.getKey();
        String cleanValue = getValue(key, slackEvent);
        
        logger.info("Key: " + map.getKey());
        logger.info("Value: " + cleanValue);
        
        event.setDetail(map.getKey(), cleanValue);
      }

      flowService.submitListenerEvent(command, eventPayload);
    }
  }

  private String getValue(String key, MultiValueMap<String, String> slackEvent) {
    String decodedCommand =
        java.net.URLDecoder.decode(slackEvent.get(key).get(0), StandardCharsets.UTF_8);
    
    if (decodedCommand.startsWith("/")) {
      return decodedCommand.replaceAll(Pattern.quote("/"), "");
    }
    
    return decodedCommand;
  }

  @Override
  public boolean validateRequest(String slackSiganture, String timestamp) {
    logger.info(
        "Validating slack siganture payload: " + slackSiganture + " - timestamp: " + timestamp);
    return (slackSiganture != null);
  }
}
