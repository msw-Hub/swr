package io.cloudtype.Demo.location;

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
public class LocationWebSocketHandler  extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdToUserIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userLocationMap = new ConcurrentHashMap<>(); // 사용자의 위치정보를 저장하기 위함
    private ScheduledExecutorService heartBeatExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JWTUtil jwtUtil;
    private final LocationService locationService;
    private ScheduledExecutorService executorService;
    public LocationWebSocketHandler(JWTUtil jwtUtil, LocationService locationService) {
        this.locationService = locationService;
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

        System.out.println("Location WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        Map<String, String> messagePayload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = messagePayload.get("type");

        if ("Authorization".equals(type)) {
            String token = messagePayload.get("token");
            if (token != null && !token.isEmpty()) {
                token = token.split(" ")[1];
                String userName = jwtUtil.getUsername(token, 1);
                if (userName != null) {
                    // userName과 WebSocket Session을 매핑
                    userSessions.put(userName, session);
                    // Session ID와 userName을 매핑
                    sessionIdToUserIdMap.put(session.getId(), userName);
                    log.info("Location socket connected userId : " + userName);
                    startPeriodicTask(userName);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "success"))));
                    return;
                }
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "fail"))));
            session.close(CloseStatus.BAD_DATA);
        } else if ("location".equals(type)) {
            // 보낸 사람의 userName을 조회 후 senderId에 저장
            String senderId = sessionIdToUserIdMap.get(session.getId());
            if (senderId != null) {
                log.info("Received location message from senderId: {}", senderId);
                // (보낸사람, {위도: , 경도 : }) 형식으로 LocationMap에 저장
                // 동일한 senderId로 put 요청이 들어오면 덮어씌워짐
                userLocationMap.put(senderId, message.getPayload());
            } else {
                log.warn("Received location message from unknown session: {}", session.getId());
            }
        } else {
            System.out.println("Received unknown message: " + message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            userLocationMap.remove(userId);
        }
        System.out.println("Location WebSocket connection closed: " + session.getId() + " - " + status.getReason());
        stopPeriodicTask();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            userLocationMap.remove(userId);
        }
        System.err.println("WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
        stopPeriodicTask();
    }

    private void startPeriodicTask(String userName) {
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            try {
                // userName으로 DB조회를 통해 상대편(산책을 맡긴 사람)의 username 불러옴
                String receiver = locationService.checkCondition(userName);
                if (receiver != null) {
                    // 상대측 username이 존재(= 산책에 위치정보가 필요한 status)
                    sendLatestLocation(userName, receiver);
                }
            } catch (Exception e) {
                log.error("Error checking condition or sending location: {}", e.getMessage(), e);
            }
        }, 0, 5, TimeUnit.SECONDS); // 주기적으로 5초마다 실행
    }


    private void stopPeriodicTask() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    private void sendLatestLocation(String senderId, String receiverId) {
        // senderId로 저장된 location 정보 get
        String location = userLocationMap.get(senderId);
        if (location != null) {
            // 수신자의 세션 get
            WebSocketSession session = userSessions.get(receiverId);
            if (session != null && session.isOpen()) { // 현재 수신자가 접속중이면
                try {
                    Map<String, String> locationPayload = Map.of(
                            "type", "location",
                            "location", location
                    );
                    String jsonResponse = objectMapper.writeValueAsString(locationPayload);
                    session.sendMessage(new TextMessage(jsonResponse));
                    log.info("Sent location message to user: {} - {}", receiverId, jsonResponse);
                } catch (Exception e) {
                    log.error("Error sending location message to user: {} - {}", receiverId, e.getMessage(), e);
                }
            }
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

}
