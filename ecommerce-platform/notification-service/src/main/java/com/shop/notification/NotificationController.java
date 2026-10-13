package com.shop.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Internal only. Currently "sends" by logging; plug in SendGrid (email) or Twilio (SMS) here.
 */
@RestController
@RequestMapping("/internal/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    public record NotificationRequest(Long userId, String email, @NotBlank String subject, @NotBlank String message) {}

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void send(@Valid @RequestBody NotificationRequest r) {
        log.info("NOTIFY user={} email={} subject='{}' message='{}'", r.userId(), r.email(), r.subject(), r.message());
    }
}
