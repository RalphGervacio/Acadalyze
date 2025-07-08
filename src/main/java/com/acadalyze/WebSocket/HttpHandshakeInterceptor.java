package com.acadalyze.WebSocket;

import com.acadalyze.beans.admin.manage_users.UsersBean;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 *
 * @author Ralph Gervacio
 */
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                Object user = session.getAttribute("user");
                if (user != null && user instanceof UsersBean authUser) {
                    attributes.put("authUserId", authUser.getAuthUserId());
                    System.out.println("Auth User ID from HttpHandshakeInterceptor: " + authUser.getAuthUserId());
                }
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception ex) {
        // No-op
    }
}
