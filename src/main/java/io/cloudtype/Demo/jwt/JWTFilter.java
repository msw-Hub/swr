package io.cloudtype.Demo.jwt;

import io.cloudtype.Demo.Dto.CustomUserDetails;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    public JWTFilter(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorization.split(" ")[1];

        if (jwtUtil.isExpired(accessToken, 1)) {
            String refreshToken = request.getHeader("X-Refresh-Token");

            if(refreshToken == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken not found");
                return;
            }

            if(jwtUtil.isExpired(refreshToken, 2)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken expired");
                return;
            }

            String username = jwtUtil.getUsername(refreshToken,2);
            UserEntity user = userRepository.findByUsername(username);
            String refreshTokenDB = user.getRefreshToken();

            if (!refreshToken.equals(refreshTokenDB)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken mismatch");
                return;
            }

            String newAccessToken = jwtUtil.createAccessToken(username, user.getRole(), 60 * 60 * 10L);

            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.setHeader("X-Refresh-Token", refreshToken);
            response.setHeader("Access-Control-Expose-Headers", "Authorization, X-Refresh-Token");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"New access token issued. Retry with the new token.\"}");
            response.getWriter().flush();
            return;
        }

        String username = jwtUtil.getUsername(accessToken,1);
        String role = jwtUtil.getRole(accessToken,1);
        String password = jwtUtil.getPassword(accessToken);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userEntity.setRole(role);

        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
