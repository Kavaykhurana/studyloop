package com.studyloop.notification;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.common.web.PageResponse;
import com.studyloop.notification.dto.NotificationDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationDto> list(CurrentUser me, @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.list(me.userId(), page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(CurrentUser me) {
        return Map.of("count", service.unreadCount(me.userId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(CurrentUser me, @PathVariable long id) {
        service.markRead(me.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(CurrentUser me) {
        service.markAllRead(me.userId());
        return ResponseEntity.noContent().build();
    }
}
