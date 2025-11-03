package lut.cn.c2cplatform.config;

import lut.cn.c2cplatform.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.err.println("[WS] Missing or invalid Authorization header on CONNECT");
                throw new IllegalArgumentException("Unauthorized WebSocket CONNECT: missing Bearer token");
            }

            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.extractUsername(token);
                System.out.println("[WS] User authenticated: " + username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                accessor.setUser(authentication);

                // 确保Principal被保存到WebSocket会话中
                accessor.setLeaveMutable(true);

                System.out.println("[WS] User principal set for: " + username);
            } catch (Exception e) {
                System.err.println("[WS] Authentication failed: " + e.getMessage());
                e.printStackTrace();
                throw new IllegalArgumentException("Unauthorized WebSocket CONNECT: invalid token");
            }
        }

        return message;
    }
}
