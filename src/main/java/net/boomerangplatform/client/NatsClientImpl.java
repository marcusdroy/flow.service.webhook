package net.boomerangplatform.client;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;
import net.boomerangplatform.model.EventPayload;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.model.Event;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.service.FlowService;

@Component
public class NatsClientImpl implements NatsClient {

	@Value("${actionlistener.nats.url}")
	private String natsUrl;

	@Value("${actionlistener.nats.cluster}")
	private String natsCluster;

	private StreamingConnection streamingConnection;

	private final Logger logger = LogManager.getLogger();

	private List<Subscription> currentSubscriptions = new LinkedList<>();

	@Autowired
	private FlowService flowService;

	public void processMessage(Message msg) {

		byte[] msgBytes = msg.getData();
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			EventPayload eventPayload = objectMapper.readValue(msgBytes, EventPayload.class);
			logger.info("Recieved Payload: ");
			String properties = objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(eventPayload.getEvent());
			logger.info(properties);

			flowService.submitListenerEvent(msg.getSubject(), eventPayload);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws TimeoutException {

		logger.info("Initializng subscriptions to NATS");

		int random = (int) (Math.random() * 10000 + 1); // NOSONAR

		StreamingConnectionFactory cf = new StreamingConnectionFactory(natsCluster, "flow-" + random);
		cf.setNatsUrl(natsUrl);
		try {
			this.streamingConnection = cf.createConnection();

			List<String> subjects = getCurrentTriggers();
			for (String item : subjects) {
				logger.info("Found subject: " + item);

				Subscription subscription = streamingConnection.subscribe(item, "flow", new MessageHandler() { // NOSONAR
					@Override
					public void onMessage(Message m) {
						processMessage(m);
					}
				}, new SubscriptionOptions.Builder().durableName("durable").build());

				currentSubscriptions.add(subscription);
			}
		} catch (IOException ex) {
			logger.error(ex);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Autowired
	private FlowWorkflowService workflowService;

	@Scheduled(fixedRateString = "${trigger.search.in.milliseconds}")
	public void lookupTriggers() {

		if (this.streamingConnection == null) {
			logger.info("Skipping schedule as unable to connect to NATS");

			return;
		}

		logger.info("Looking up workflows for latest triggers.");

		List<String> workflows = getCurrentTriggers();
		logger.info("Workflow count: " + workflows.size());

		List<String> subscriptions = currentSubscriptions.stream().map(x -> x.getSubject())
				.collect(Collectors.toList()); // NOSONAR

		List<Subscription> invalidSubscriptions = new LinkedList<>();

		for (Subscription sub : currentSubscriptions) {
			String subject = sub.getSubject();
			if (!workflows.contains(subject)) {
				try {
					logger.info("Removing subscription: " + subject);

					sub.close(true);
					invalidSubscriptions.add(sub);
				} catch (IOException e) {
					logger.error(e);
				}
			}
		}
		for (Subscription sub : invalidSubscriptions) {
			this.currentSubscriptions.remove(sub);
		}

		for (String newWorkflow : workflows) {
			if (!subscriptions.contains(newWorkflow)) {
				try {
					logger.info("Removing subscription: " + newWorkflow);

					Subscription newSubscription = streamingConnection.subscribe(newWorkflow, "flow",
							new MessageHandler() { // NOSONAR
								@Override
								public void onMessage(Message m) {
									processMessage(m);
								}
							}, new SubscriptionOptions.Builder().durableName("durable").build());
					this.currentSubscriptions.add(newSubscription);
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
					ExceptionUtils.getStackTrace(e);
				}
			}
		}
	}

	private List<String> getCurrentTriggers() {

		List<String> newSubjects = new LinkedList<>();
		List<FlowWorkflowEntity> eventWorkflows = workflowService.getEventWorkflows();
		for (FlowWorkflowEntity workflow : eventWorkflows) {
			if (workflow.getTriggers().getEvent() != null) {
				Event event = workflow.getTriggers().getEvent();
				if (event.getTopic() != null && !"".equals(event.getTopic())) {
					if (!newSubjects.contains(event.getTopic())) { // NOSONAR
						newSubjects.add(event.getTopic());
					}
				}
			}
		}
		return newSubjects;
	}

}
