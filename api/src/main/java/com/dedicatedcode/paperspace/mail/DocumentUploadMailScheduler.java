package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.model.Document;
import com.dedicatedcode.paperspace.model.DocumentListener;
import com.dedicatedcode.paperspace.model.TaskDocument;
import com.dedicatedcode.paperspace.web.DocumentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentUploadMailScheduler implements DocumentListener {
    private final JdbcMessageService messageService;
    private final TemplateEngine textTemplateEngine;
    private final String recipient;
    private final String appHost;

    @Autowired
    public DocumentUploadMailScheduler(JdbcMessageService messageService, TemplateEngine textTemplateEngine, @Value("${email.target-address}") String recipient, @Value("${app.host}") String appHost) {
        this.messageService = messageService;
        this.textTemplateEngine = textTemplateEngine;
        this.recipient = recipient;
        this.appHost = appHost;
    }

    @Override
    public void changed(Document oldVersion, Document newVersion) {

    }

    @Override
    public void created(Document document) {
        String messageIdentifier = "DOCUMENT_CREATED_" + document.getId();
        List<MessageAttachment> attachments = new ArrayList<>();
        attachments.add(new MessageAttachment(MessageAttachment.AttachmentType.BINARY, document.getFile().getId()));

        Context context = new Context(Locale.GERMAN);
        context.setVariable("document", new DocumentResponse(document));
        context.setVariable("appHost",this.appHost);

        String body = textTemplateEngine.process("document", context);
        String subject = "New document uploaded";

        this.messageService.storeMessage(
                new Message(UUID.randomUUID(),
                        MessageType.EMAIL,
                        MessageState.SCHEDULED,
                        subject,
                        body,
                        LocalDateTime.now(),
                        recipient,
                        attachments), messageIdentifier);
    }

    @Override
    public void deleted(Document document) {
        String messageIdentifier = "DOCUMENT_CREATED_" + document.getId();
        this.messageService.getScheduledMessageBy(messageIdentifier).forEach(messageService::deleteMessage);
    }
}
