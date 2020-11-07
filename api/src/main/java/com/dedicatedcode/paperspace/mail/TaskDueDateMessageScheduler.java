package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.DocumentListener;
import com.dedicatedcode.paperspace.model.State;
import com.dedicatedcode.paperspace.model.TaskDocument;
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
public class TaskDueDateMessageScheduler implements DocumentListener {
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
    public void changed(Document oldVersion, Document newVersion) {
        if (oldVersion instanceof TaskDocument && newVersion instanceof TaskDocument) {
            String messageIdentifier = "TASK_DUE_" + newVersion.getId();
            if (((TaskDocument) newVersion).getState() == State.DONE) {
                this.messageService.getScheduledMessageBy(messageIdentifier).forEach(this.messageService::deleteMessage);
            } else if (!((TaskDocument) oldVersion).getDueAt().equals(((TaskDocument) newVersion).getDueAt())) {
                this.messageService.getScheduledMessageBy(messageIdentifier).forEach(this.messageService::deleteMessage);
                created(newVersion);
            }
        } else if (!(oldVersion instanceof TaskDocument) && newVersion instanceof TaskDocument) {
            created(newVersion);
        } else if (oldVersion instanceof TaskDocument) {
            deleted(oldVersion);
        }
    }

    @Override
    public void created(Document document) {
        if (document instanceof TaskDocument) {
            TaskDocument task = (TaskDocument) document;
            String messageIdentifier = "TASK_DUE_" + document.getId();
            List<MessageAttachment> attachments = new ArrayList<>();
            attachments.add(new MessageAttachment(MessageAttachment.AttachmentType.BINARY, task.getFile().getId()));

            Context context = new Context(Locale.GERMAN);
            context.setVariable("document", new TaskDocumentResponse(task));
            context.setVariable("appHost", this.appHost);

            String body = textTemplateEngine.process("task_due", context);
            String subject = "Task needs attention";

            this.messageService.storeMessage(
                    new Message(UUID.randomUUID(),
                            MessageType.EMAIL,
                            MessageState.SCHEDULED,
                            subject,
                            body,
                            task.getDueAt().minusDays(1).withHour(12).withMinute(0),
                            recipient,
                            attachments), messageIdentifier);
        }
    }

    @Override
    public void deleted(Document document) {
        String messageIdentifier = "TASK_DUE_" + document.getId();
        this.messageService.getScheduledMessageBy(messageIdentifier).forEach(messageService::deleteMessage);
    }
}
