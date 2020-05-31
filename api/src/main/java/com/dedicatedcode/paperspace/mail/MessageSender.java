package com.dedicatedcode.paperspace.mail;

/**
 * Created by Daniel Wasilew on 21.07.17
 * (c) 2017 Daniel Wasilew <daniel@dedicatedcode.com>
 */
public interface MessageSender {
    Message sendMessage(Message message) throws MessagingException;
}
