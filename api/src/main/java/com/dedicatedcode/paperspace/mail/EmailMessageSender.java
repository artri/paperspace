package com.dedicatedcode.paperspace.mail;

import com.dedicatedcode.paperspace.BinaryService;
import com.dedicatedcode.paperspace.StorageService;
import com.dedicatedcode.paperspace.model.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;

@Component
public class EmailMessageSender implements MessageSender {
    private static final Logger log = LoggerFactory.getLogger(EmailMessageSender.class);

    private final BinaryService binaryService;
    private final StorageService storageService;
    private final boolean enabled;
    private final boolean attachDocuments;
    private final String sender;
    private final JavaMailSender mailSender;
    private final String toAddress;


    @Autowired
    public EmailMessageSender(BinaryService binaryService, StorageService storageService, @Value("${email.enabled}") boolean enabled, @Value("${email.attach_documents}") boolean attachDocuments, @Value("${email.sender-address}") String sender, JavaMailSender mailSender, @Value("${email.target-address}") String toAddress) {
        this.binaryService = binaryService;
        this.storageService = storageService;
        this.enabled = enabled;
        this.attachDocuments = attachDocuments;
        this.sender = sender;
        this.mailSender = mailSender;
        this.toAddress = toAddress;
    }

    @Override
    public Message sendMessage(Message message) throws MessagingException {
        if (!enabled) {
            log.debug("sending email is disabled, discarding mail send to [{}]", toAddress);
            return message.withState(MessageState.SENT).withRecipient(toAddress).withSendAt(LocalDateTime.now());
        }
        if (toAddress == null) {
            return message.withState(MessageState.FAILED);
        }
        log.debug("trying to send email to [{}]", toAddress);
        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true);
            helper.setTo(toAddress);
            helper.setSubject(message.getSubject());
            helper.setText(message.getBody(), true);
            helper.setFrom(sender);
            if (this.attachDocuments) {
                for (MessageAttachment attachment : message.getAttachments()) {
                    if (attachment.getType() == MessageAttachment.AttachmentType.BINARY) {
                        Binary binary = this.binaryService.get(attachment.getId());
                        helper.addAttachment(binary.getOriginalFileName(), this.storageService.load(binary.getId()));
                    } else {
                        log.error("Could not add unknown attachment type [{}] to email", attachment.getType());
                        return message.withState(MessageState.FAILED).withRecipient(toAddress).withSendAt(LocalDateTime.now());
                    }
                }
            }
            mailSender.send(mail);
            return message.withSendAt(LocalDateTime.now()).withState(MessageState.SENT).withRecipient(toAddress);
        } catch (MailSendException | javax.mail.MessagingException e) {
            log.error("cold not send email", e);
            return message.withState(MessageState.FAILED).withRecipient(toAddress).withSendAt(LocalDateTime.now());
        }
    }
}
