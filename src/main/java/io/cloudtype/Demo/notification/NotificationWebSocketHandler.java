package io.cloudtype.Demo.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userSessionMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartBeatExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        sessions.forEach((id, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error sending ping message to session " + id + ": " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        String userId = getUserIdFromSession(session); // 사용자 식별자 추출 로직 추가 필요
        if (userId != null) {
            userSessionMap.put(userId, session.getId());
        }
        System.out.println("Notification WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        System.out.println("Received notification message: " + message.getPayload());
        // 알림 메시지 처리 로직
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        userSessionMap.values().remove(session.getId());
        System.out.println("Notification WebSocket connection closed: " + session.getId() + " - " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session.getId());
        userSessionMap.values().remove(session.getId());
        System.err.println("WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void sendNotificationToUser(String userId, String notificationMessage) {
        String sessionId = userSessionMap.get(userId);
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                // Construct the notification payload
                Map<String, String> notificationPayload = Map.of(
                        "type", "notification",
                        "message", notificationMessage
                );

                // Convert the payload to a JSON string
                String jsonResponse = objectMapper.writeValueAsString(notificationPayload);

                // Send the JSON message
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
        return Optional.ofNullable(session.getUri())
                .map(uri -> uri.getQuery())
                .flatMap(query -> {
                    for (String param : query.split("&")) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2 && keyValue[0].equals("userId")) {
                            return Optional.of(keyValue[1]);
                        }
                    }
                    return Optional.empty();
                }).orElse(null);
    }
}