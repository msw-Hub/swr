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
import org.springframework.web.bind.annotation.CrossOrigin;
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

        // 모든 헤더 로깅
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("Header: " + headerName + " Value: " + request.getHeader(headerName));
        }

        //request에서 Authorization 헤더를 찾음
        String authorization= request.getHeader("Authorization");
        
        //Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("token null");
            filterChain.doFilter(request, response);
            //조건이 해당되면 메소드 종료 (필수)
            return;
        }

        System.out.println("authorization now");
        //Bearer 부분 제거 후 순수 토큰만 획득
        String accessToken = authorization.split(" ")[1];

        //토큰 소멸 시간 검증
        if (jwtUtil.isExpired(accessToken, 1)) {
            //토큰 만료된거임.
            System.out.println("accessToken expired");

            //엑세스 토큰의 만료 시간이 지났을 경우 리프레시 토큰을 확인
            String refreshToken = request.getHeader("X-Refresh-Token");
            log.info("refreshToken: " + refreshToken);

            if(refreshToken == null) {
                // 리프레시 토큰이 없는 경우 클라이언트로부터 새로운 토큰 요청
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken not found");
                return;
            }
            //리프레시토큰이 만료된 경우
            if(jwtUtil.isExpired(refreshToken, 2)) {
                System.out.println("RefreshToken expired");
                // 리프레시 토큰이 만료된 경우 클라이언트로부터 새로운 토큰 요청
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken expired");
                return;
            }

            //리프레시토큰이 유효한 경우
            //리프레시토큰에서 유저이름을 가져와서 데이터베이스에서 리프레시토큰을 비교
            String username = jwtUtil.getUsername(refreshToken,2);
            UserEntity user = userRepository.findByUsername(username);
            String refreshTokenDB = user.getRefreshToken();

            // 가져온 리프레시 토큰과 클라이언트가 보낸 리프레시 토큰을 비교하여 일치하는지 확인
            if (!refreshToken.equals(refreshTokenDB)) {
                // 리프레시 토큰이 일치하지 않는 경우 처리
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RefreshToken 불일치");
                log.info("dbRefreshToken 불일치");
                return;
            }


            // 리프레시 토큰이 일치하는 경우, 새로운 엑세스 토큰과 리프레시 토큰 발급 및 클라이언트에게 전달
            String newAccessToken = jwtUtil.createAccessToken(username, user.getRole(), 60 * 60 * 10L);

            // 새로운 엑세스 토큰을 클라이언트에게 전달
            response.setHeader("Authorization", "Bearer " + newAccessToken);
            response.setHeader("X-Refresh-Token", refreshToken);

            log.info("newAccessToken: " + newAccessToken);
            log.info("refreshToken: " + refreshToken);

            // 새로운 엑세스 토큰과 리프레시 토큰을 클라이언트에게 전달
            // 클라이언트에게 현재 요청을 재시도하도록 지시
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"New access token issued. Please retry the request.\"}");

            // 필터 체인 진행
            filterChain.doFilter(request, response);
            return;
        }

        //엑세스 토큰이 유효한 경우
        //토큰에서 username과 role 획득
        String username = jwtUtil.getUsername(accessToken,1);
        String role = jwtUtil.getRole(accessToken,1);
        String password = jwtUtil.getPassword(accessToken);

        //userEntity를 생성하여 값 set
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(password);
        userEntity.setRole(role);

        //UserDetails에 회원 정보 객체 담기
        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // 엑세스 토큰이 유효한 경우에는 그대로 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
}