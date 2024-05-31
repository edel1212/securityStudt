package com.yoo.securityStudy.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoo.securityStudy.dto.google.GoogleOAuthToken;
import com.yoo.securityStudy.dto.google.GoogleUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class GoogleOauth implements SocialOAuth{

    @Value("${spring.OAuth2.google.url}")
    private String GOOGLE_SNS_LOGIN_URL;

    @Value("${spring.OAuth2.google.client-id}")
    private String GOOGLE_SNS_CLIENT_ID;

    @Value("${spring.OAuth2.google.callback-url}")
    private String GOOGLE_SNS_CALLBACK_URL;

    @Value("${spring.OAuth2.google.client-secret}")
    private String GOOGLE_SNS_CLIENT_SECRET;

    @Value("${spring.OAuth2.google.scope}")
    private String GOOGLE_DATA_ACCESS_SCOPE;

    private final ObjectMapper objectMapper;

    @Override
    public String getOauthRedirectURL() {
        // 👉 파라미터 정의
        Map<String, String> params = new HashMap<>();
        params.put("scope"          , GOOGLE_DATA_ACCESS_SCOPE);
        params.put("response_type"  , "code");
        params.put("client_id"      , GOOGLE_SNS_CLIENT_ID);
        params.put("redirect_uri"   , GOOGLE_SNS_CALLBACK_URL);

        // 👉 파라미터를 URL 형식으로 변경
        String parameterString = params.entrySet()
                .stream()
                .map(x->x.getKey()+"="+x.getValue())
                .collect(Collectors.joining("&"));

        // 👉 리디렉션시킬 URL에 파라미터 추가
        String redirectURL = GOOGLE_SNS_LOGIN_URL + "?" + parameterString;
        /***
         * https://accounts.google.com/o/oauth2/v2/auth
         * ?scope=https://www.googleapis.com/auth/userinfo.email
         * %20https://www.googleapis.com/auth/userinfo.profile&response_type=code
         * &redirect_uri=http://localhost:8080/app/accounts/auth/google/callback
         * &client_id=824915807954-ba1vkfj4aec6bgiestgnc0lqrbo0rgg3.apps.googleusercontent.com
         * **/
        log.info("-------------------");
        log.info("redirectURL = " + redirectURL);
        log.info("-------------------");
        return redirectURL;
    }

    /**
     * Google에서 인증받은 일회성 코드을 연계에 사용하여 인증 jwt 토큰을 받아옴
     *
     * @param code the code
     * @return the response entity
     */
    public GoogleOAuthToken requestAccessToken(String code) throws JsonProcessingException{
        // ℹ️ 토큰 요청 URL - 공식문서 확인
        String GOOGLE_TOKEN_REQUEST_URL = "https://oauth2.googleapis.com/token";
        RestTemplate restTemplate       = new RestTemplate();
        Map<String, Object> params      = new HashMap<>();
        params.put("code", code);
        params.put("client_id"      , GOOGLE_SNS_CLIENT_ID);
        params.put("client_secret"  , GOOGLE_SNS_CLIENT_SECRET);
        params.put("redirect_uri"   , GOOGLE_SNS_CALLBACK_URL);
        params.put("grant_type"     , "authorization_code");

        // 👉 Google 연계 시작
        ResponseEntity<String> responseEntity =
                restTemplate.postForEntity(GOOGLE_TOKEN_REQUEST_URL, params, String.class);
        // ℹ️ 2xx가 아니면 null 반환
        if(responseEntity.getStatusCode() != HttpStatus.OK) return null;
        
        // Google에서 받아온 Response Body 데이터
        log.info("response.getBody() = " + responseEntity.getBody());
        /***
         * {
         *   "access_token": "~",
         *   "expires_in": 3598,
         *   "scope": "openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile",
         *   "token_type": "Bearer",
         *   "id_token": "~"
         * }
         *
         * **/
        // 자바 객체로 변환
        return objectMapper.readValue(responseEntity.getBody(), GoogleOAuthToken.class);

    }

    /**
     * Google에서 발행한 jwt 토큰을 사용해서 회원 정보를 받아옴
     *
     * @param oAuthToken the o auth token
     * @return the google user
     * @throws JsonProcessingException the json processing exception
     */
    public GoogleUser requestUserInfo(GoogleOAuthToken oAuthToken)  throws JsonProcessingException{
        // ℹ️ 회원정보 요청 URL - 공식문서 확인 [ AccessToken 필요 ]
        String GOOGLE_USERINFO_REQUEST_URL = "https://www.googleapis.com/oauth2/v1/userinfo";

        // 👉 Header에 jwt 토큰을 담음
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION,"Bearer " + oAuthToken.getAccess_token());

        // 👉 Google과 연계
        RestTemplate restTemplate       = new RestTemplate();
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(GOOGLE_USERINFO_REQUEST_URL, HttpMethod.GET,request,String.class);
        log.info("response.getBody() = " + response.getBody());
        /**
         * {
         *   "id": "~~~",
         *   "email": "~",
         *   "verified_email": true,
         *   "name": "유정호",
         *   "given_name": "정호",
         *   "family_name": "유",
         *   "picture": "~",
         *   "locale": "ko"
         * }
         * **/
        return objectMapper.readValue(response.getBody(), GoogleUser.class);
    }

}
