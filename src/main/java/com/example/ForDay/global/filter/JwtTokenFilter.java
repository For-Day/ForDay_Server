package com.example.ForDay.global.filter;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public JwtTokenFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.split(" ")[1];

        try {
            // 1. 토큰 만료 및 유효성(서명) 검증
            // getUsername 등 Claims를 꺼내는 로직이 실행될 때 서명 예외가 발생하므로 모두 try 안에 넣어야 합니다.
            if (jwtUtil.isExpired(token)) {
                throw new CustomException(ErrorCode.TOKEN_EXPIRED);
            }

            // 2. 토큰에서 사용자 정보 꺼내기 (여기서 SignatureException 발생 가능)
            String socialId = jwtUtil.getUsername(token);

            // 3. DB 조회
            User user = userRepository.findBySocialId(socialId);

            if (user == null) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            // Authentication 생성 후 SecurityContext 저장
            Authentication authToken =
                    new UsernamePasswordAuthenticationToken(
                            customUserDetails,
                            null,
                            customUserDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authToken);

            // 정상 로직 수행 후 필터 체인 이동
            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.security.SignatureException | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.security.WeakKeyException e) {
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            setErrorResponse(response, ErrorCode.TOKEN_EXPIRED);
        } catch (CustomException e) {
            setErrorResponse(response, e.getErrorCode());
        } catch (Exception e) {
            // 예상치 못한 에러 로그 출력
            log.error("JWT Filter Unexpected Error: {}", e.getMessage());
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        // 공통 응답 객체 구조에 맞게 Map 생성
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorClassName", errorCode.name());
        errorDetails.put("message", errorCode.getMessage());

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("status", errorCode.getStatus().value());
        responseMap.put("success", false);
        responseMap.put("data", errorDetails);

        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}