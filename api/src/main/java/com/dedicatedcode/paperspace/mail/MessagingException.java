package com.dedicatedcode.paperspace.mail;

/**
 * Created by Daniel Wasilew on 21.07.17
 * (c) 2017 Daniel Wasilew <daniel@dedicatedcode.com>
 */
public class MessagingException extends Exception {
    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }

}
