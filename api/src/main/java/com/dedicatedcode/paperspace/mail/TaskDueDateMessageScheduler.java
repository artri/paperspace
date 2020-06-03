package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.model.TaskDocumentListener;
import com.dedicatedcode.paperspace.web.TaskDocumentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TaskDueDateMessageScheduler implements TaskDocumentListener {
    private final JdbcMessageService messageService;
    private final TemplateEngine textTemplateEngine;
    private final String recipient;
    private final String appHost;

    public TaskDueDateMessageScheduler(JdbcMessageService messageService, TemplateEngine textTemplateEngine, @Value("${email.target-address}") String recipient, @Value("${app.host}") String appHost) {
        this.messageService = messageService;
        this.textTemplateEngine = textTemplateEngine;
        this.recipient = recipient;
        this.appHost = appHost;
    }

    @Override
    public void changed(TaskDocument oldVersion, TaskDocument newVersion) {
        String messageIdentifier = "TASK_DUE_" + newVersion.getId();
        if (newVersion.getState() == State.DONE) {
            this.messageService.getScheduledMessageBy(messageIdentifier).forEach(this.messageService::deleteMessage);
        } else if (!oldVersion.getDueAt().equals(newVersion.getDueAt())) {
            this.messageService.getScheduledMessageBy(messageIdentifier).forEach(this.messageService::deleteMessage);
            created(newVersion);
        }
    }

    @Override
    public void created(TaskDocument document) {
        String messageIdentifier = "TASK_DUE_" + document.getId();
        List<MessageAttachment> attachments = new ArrayList<>();
        attachments.add(new MessageAttachment(MessageAttachment.AttachmentType.BINARY, document.getFile().getId()));

        Context context = new Context(Locale.GERMAN);
        context.setVariable("document", new TaskDocumentResponse(document));
        context.setVariable("appHost", this.appHost);

        String body = textTemplateEngine.process("task_due", context);
        String subject = "Task needs attention";

        this.messageService.storeMessage(
                new Message(UUID.randomUUID(),
                        MessageType.EMAIL,
                        MessageState.SCHEDULED,
                        subject,
                        body,
                        document.getDueAt().minusDays(1).withHour(12).withMinute(0),
                        recipient,
                        attachments), messageIdentifier);
    }

    @Override
    public void deleted(TaskDocument document) {
        String messageIdentifier = "TASK_DUE_" + document.getId();
        this.messageService.getScheduledMessageBy(messageIdentifier).forEach(messageService::deleteMessage);
    }
}
