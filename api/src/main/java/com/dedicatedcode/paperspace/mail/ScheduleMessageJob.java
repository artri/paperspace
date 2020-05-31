package com.dedicatedcode.paperspace.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleMessageJob {
    private static final Logger log = LoggerFactory.getLogger(ScheduleMessageJob.class);

    private final JdbcMessageService messageService;
    private final EmailMessageSender emailMessageSender;

    @Autowired
    public ScheduleMessageJob(JdbcMessageService messageService, EmailMessageSender emailMessageSender) {
        this.messageService = messageService;
        this.emailMessageSender = emailMessageSender;
    }

    @Scheduled(initialDelay = 0, fixedDelay = 15000)
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        List<Message> messages = this.messageService.getAllScheduled();
        log.debug("found [{}] unsent messages. [{}] which should be send.", messages.size(), messages.stream().filter(message -> message.getSendAt().isBefore(now)).count());
        for (Message message : messages) {
            if (message.getSendAt().isBefore(now)) {

                if (message.getMessageType() == MessageType.EMAIL) {
                    log.debug("message [{}] will be send now with [{}]", message.getSendAt(), emailMessageSender.getClass());
                    try {
                        Message response = emailMessageSender.sendMessage(message);
                        this.messageService.moveToProcessedMessages(response);
                    } catch (MessagingException e) {
                        this.messageService.moveToProcessedMessages(message.withState(MessageState.FAILED).withRecipient(""));
                        log.error("message [{}] could not be sent", message.getId(), e);
                    }
                } else {
                    log.warn("no sender found for message type [{}]", message.getMessageType());
                }
            }
        }
    }
}
