package io.cloudtype.Demo.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.jwt.JWTUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdToUserIdMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartBeatExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JWTUtil jwtUtil;

    public NotificationWebSocketHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        heartBeatExecutor = Executors.newScheduledThreadPool(1);
        heartBeatExecutor.scheduleAtFixedRate(this::sendHeartBeat, 0, 55, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartBeatExecutor.shutdown();
    }

    private void sendHeartBeat() {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error sending ping message to session " + userId + ": " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            sessionIdToUserIdMap.put(session.getId(), userId);
            log.info("socket connected userId : " + userId);
            log.info("userSessions : " + userSessions);
            log.info("sessionIdToUserIdMap : " + sessionIdToUserIdMap);
        } else {
            session.close(CloseStatus.BAD_DATA);
            log.info("WebSocket connection closed due to missing userId");
        }
        System.out.println("Notification WebSocket connection established: " + session.getId());
    }
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        System.out.println("Received notification message: " + message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        System.out.println("Notification WebSocket connection closed: " + session.getId() + " - " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        System.err.println("WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void sendNotificationToUser(String userId, String notificationMessage) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, String> notificationPayload = Map.of(
                        "type", "notification",
                        "message", notificationMessage
                );
                String jsonResponse = objectMapper.writeValueAsString(notificationPayload);
                session.sendMessage(new TextMessage(jsonResponse));
                System.out.println("Sent notification message to: " + session.getId() + " - " + jsonResponse);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error sending notification message to: " + session.getId() + " - " + e.getMessage());
            }
        } else {
            System.err.println("No active session found for user: " + userId);
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String token = authHeaders.get(0).split(" ")[1];
            System.out.println(token);
            return jwtUtil.getUsername(token, 1); // JWT 토큰에서 사용자 ID 추출
        }
        return null;
    }
    public boolean isUserConnected(String userId) {
        return userSessions.containsKey(userId);
    }
}