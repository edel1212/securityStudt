# Spring Security Study

### Security Depenency

- 의존성을 추가하는 순간부터 모든 요청은 Scurity의 Filter를 거치게 된다.
  - 따라서 모든 요청은 Security에서 기본적으로 제공되는 LoginForm으로 이동된다.
    - 계정 및 비밀번호는 로그에 써 있다.

```groovy
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-security'
	testImplementation 'org.springframework.security:spring-security-test'
}
```

### 기본 Security 설정

- SpringBoot 버전이 올라가면서 Security 설정 방법이 변경되었다.
  - 작성일 기준 버전 `3.2.3`버전
- 지정 클래스는 Bean Scan 대상에 추가 해줘야한다.
  - `@Component` 어노테이션 사용
- `SecurityFilterChain`를 구현하는 메서드를 생성한 후 Bean에 추가해준다.
  - 생성 이후 부터는 모든 요청에 대한 접근이 **허용**으로 변경된다.
- 함수형 인터페이스를 사용하여 옵션을 적용해준다.
  - 이전 `체이닝 -> 함수형`으로 변경되었다.
-  `SecurityFilterChain`를 구현한 메서드내의 매개변수인  HttpSecurity 객체에 옵션을 더하는 식으로 설정을 한다.
-  `WebSecurityCustomizer`를 구현한 메서드내에서 Security 필터에서 제외할 요청을 지정 가능하다
   - 정적파일을 사용하는 경우에는 꼭 해당 설정해주자.
- 예시 코드 	
  ```java
  @Component
  @Log4j2
  public class SecurityConfig {

      /**
      * - SecurityFilterChain << 아무 옵션 없이 적용 시 모든 페이지 접근이 허용된다.
      * */
      @Bean
      public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

          log.info("-------------------------");
          log.info("Filter Chain");
          log.info("-------------------------");

          // 👉 CSRF 사용 ❌
          http.csrf(csrf -> csrf.disable());
          // 👉 CORS 설정
          http.cors(cors->{
              /**
              * 참고 : https://velog.io/@juhyeon1114/Spring-security%EC%97%90%EC%84%9C-CORS%EC%84%A4%EC%A0%95%ED%95%98%EA%B8%B0
              *    - 설정 클래스를 만든 후 주입해주면 Cors 설정이 한번에 가능함
              * */
              // cors.configurationSource(CorsConfigurationSource)
          });

          // 👉  Default Login form 설정 - 사용 할경우
          //http.formLogin(Customizer.withDefaults());

          // 👉 기본 설정 로그인 form 사용 ❌
          http.formLogin(login->login.disable());
          // 👉 Security HTTP Basic 인증 ❌ - 웹 상단 알림창으로 로그인이 뜨는 것 방지
          http.httpBasic(AbstractHttpConfigurer::disable);

          // 👉 모든 접근 제한
          http.authorizeHttpRequests( access ->
                          access.requestMatchers("/**")
                                  .authenticated()
                                  .anyRequest().authenticated()
                  );

          return http.build();
      }


      /**
      * Security - Custom Bean 등록
      * */
      @Bean
      public WebSecurityCustomizer webSecurityCustomizer(){
          return web -> web.ignoring()
                  // Login 접근 허용
                  .requestMatchers(HttpMethod.POST,"/member/login")
                  // Spring Boot의 resources/static 경로의 정적 파일들 접근 허용
                  .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
      }

  }
  ```

### 예외 핸들러 설정

- `AuthenticationEntryPoint` 설정
  - 인증이 실패했을 때 사용자를 리디렉션하거나 에러 메시지를 반환하는 역할을 담당함
    - 인증 실패 처리: 사용자가 인증되지 않았거나, 인증 정보가 잘못되었을 때 호출됩니다.
    - 리디렉션: 웹 애플리케이션에서는 인증되지 않은 사용자를 로그인 페이지로 리디렉션하는 것이 일반적입니다. AuthenticationEntryPoint를 사용하여 이러한 리디렉션을 설정할 수 있습니다
    - 에러 메시지 반환: 인증이 실패하면 사용자에게 에러 메시지나 HTTP 상태 코드를 반환하여 문제의 원인을 알릴 수 있습니다.
  - 사용 방법
    - `AuthenticationEntryPoint`를 구현한 클래스 제작
    - Bean Scan 대상에 올려주기 위해 `@Component`를 추가해주자
      ```java
      @Log4j2
      @Component
      public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
  
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            log.info("- Custom Authentication Entry PointHandler 접근 -");
            var objectMapper = new ObjectMapper();
            int scUnauthorized = HttpServletResponse.SC_UNAUTHORIZED;
            response.setStatus(scUnauthorized);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(scUnauthorized)
                    .message("예외 메세지 등록")
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
      }
      ```
- `AccessDeniedHandler` 설정
  - 인증에 실패했을 경우 처리를 담당한다.
  - 사용 방법
    - `AccessDeniedHandler`를 구현한 클래스 제작
    - Bean Scan 대상에 올려주기 위해 `@Component`를 추가해주자
      ```java
      @Log4j2
      @Component
      public class CustomAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
          log.info("- Custom Access Denied Handler 접근 -");
          var objectMapper = new ObjectMapper();
          int scUnauthorized = HttpServletResponse.SC_UNAUTHORIZED;
          response.setStatus(scUnauthorized);
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.setCharacterEncoding(StandardCharsets.UTF_8.name());
          ErrorResponse errorResponse = ErrorResponse.builder()
                  .code(scUnauthorized)
                  .message("접근 권한이 없습니다.")
                  .build();
          response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
      }
      ```    
- `SecurityConfig` 설정 
  - 의존성 주입 후 `exceptionHandling()`에 등록
    ```java
    @Component
    @RequiredArgsConstructor
    @Log4j2
    public class SecurityConfig {
    
      // 접근 제어 핸들러
      private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    
      @Bean
      public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
    
    
        // Custom Exception Handling
        http.exceptionHandling(handling ->
                handling
                        // ✨ Access Denied Handling
                        .accessDeniedHandler(customAccessDeniedHandler)
                        // ✨ AuthenticationEntryPoint
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
        );
    
        return http.build();
      }
    
    
      /**
       * Security - Custom Bean 등록
       * */
      @Bean
      public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring()
                // Login 접근 허용
                .requestMatchers(HttpMethod.POST,"/member/login")
                // Spring Boot의 resources/static 경로의 정적 파일들 접근 허용
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
      }
    
    }    
    ```  

- `AuthFailureHandler` 설정
  - 해당 핸들러는 로그인 실패 시 핸들링 하는 핸들러이다.
  - 사용하기 위해서는 로그인 form 설정을 해줘야한다.
    - 단 현재 예제에서는 form 을 사용하지 않으니 `loginProcessingUrl()`설정을 해준 후 지정해준다.
  - 사용 방법
    - `SimpleUrlAuthenticationFailureHandler`를 상속한 클래스 제작
    - Bean Scan 대상에 올려주기 위해 `@Component`를 추가해주자
       ```java
       @Log4j2
       @Component
       public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {
         @Override
         public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
           log.info("- Custom Auth Failure Handler 접근 -");
           var objectMapper = new ObjectMapper();
           String errorMessage;
           if (exception instanceof BadCredentialsException) {
             errorMessage = "아이디와 비밀번호를 확인해주세요.";
           } else if (exception instanceof InternalAuthenticationServiceException) {
             errorMessage = "내부 시스템 문제로 로그인할 수 없습니다. 관리자에게 문의하세요.";
           } else if (exception instanceof UsernameNotFoundException) {
             errorMessage = "존재하지 않는 계정입니다.";
           } else {
             errorMessage = "알 수없는 오류입니다.";
           }
           ErrorResponse errorResponse = ErrorResponse.builder()
                   .code(HttpServletResponse.SC_UNAUTHORIZED)
                   .message(errorMessage)
                   .build();
           response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
         }
       }
       ```   
- `SecurityConfig` 설정
  - 의존성 주입 후 `formLogin()`내 함수 등록 `loginProcessingUrl("url"),failureHandler(customAuthFailureHandler)`
    ```java
    @Component
    @RequiredArgsConstructor
    @Log4j2
    public class SecurityConfig {
    
      // 인증 실패 제어 핸들러
      private final CustomAuthFailureHandler customAuthFailureHandler;
    
      @Bean
      public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
    
        // 👉 로그인을 사용할 loginProcessingUrl을 설정해준다.
        http.formLogin(login->login.loginProcessingUrl("/login")
                .failureHandler(customAuthFailureHandler));      
    
        return http.build();
      }
    
    }    
    ```  

### UserDetailService 설정
- 


## TODO List



- DB 계정 관리
  - 권한별 접근
- 커스텀 핸들러 적용
- jwt
  - Refresh token
