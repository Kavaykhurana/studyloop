package com.studyloop.notification;

import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.web.PageResponse;
import com.studyloop.notification.dto.NotificationDto;
import com.studyloop.user.UserLookup;
import com.studyloop.user.dto.UserBrief;
import java.time.Clock;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserLookup userLookup;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, UserLookup userLookup, Clock clock) {
        this.notifications = notifications;
        this.userLookup = userLookup;
        this.clock = clock;
    }

    @Transactional
    public void create(long userId, NotificationType type, long doubtId, Long actorId, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setDoubtId(doubtId);
        n.setActorId(actorId);
        n.setMessage(message.length() <= 200 ? message : message.substring(0, 199) + "…");
        n.setCreatedAt(clock.instant());
        notifications.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(long userId, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 50));
        Page<Notification> result = notifications.findByUserId(userId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        Map<Long, UserBrief> actors = userLookup.briefs(
                result.getContent().stream().map(Notification::getActorId).toList());
        return PageResponse.from(result, n -> new NotificationDto(n.getId(), n.getType().name(), n.getMessage(),
                n.getDoubtId(), actors.get(n.getActorId()), n.getReadAt() != null, n.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(long userId, long id) {
        Notification n = notifications.findById(id).filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (n.getReadAt() == null) {
            n.setReadAt(clock.instant());
        }
    }

    @Transactional
    public void markAllRead(long userId) {
        notifications.markAllRead(userId, clock.instant());
    }
}
