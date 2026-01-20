package com.team8.damo.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import static com.team8.damo.exception.errorcode.GlobalErrorCode.*;

@Slf4j
@Component
public class KakaoUtil {
    @Value("${kakao.oauth.admin}")
    private String admin;

    @Value("${kakao.oauth.client-id}")
    private String clientId;

    @Value("${kakao.oauth.redirect-uri")
    private String redirectUri;

    private final String CONTENT_TYPE = "application/x-www-form-urlencoded;charset=utf-8";
    private final String GRANT_TYPE = "authorization_code";

    // 받은 인가코드를 사용하여 액세스 토큰 발급
    public KakaoResponse.OAuthToken getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", CONTENT_TYPE);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", GRANT_TYPE);
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<LinkedMultiValueMap<String, String>> kakaoRequest = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://kauth.kakao.com/oauth/token",
            HttpMethod.POST,
            kakaoRequest,
            String.class);

        KakaoResponse.OAuthToken oAuthToken = null;
        ObjectMapper objectMapper = new ObjectMapper();
        log.info(response.getBody());

        try {
            oAuthToken = objectMapper.readValue(response.getBody(), KakaoResponse.OAuthToken.class);
            log.info("oAuthToken : {}", oAuthToken.getAccess_token());
        } catch (Exception e) {
            throw new CustomException(JSON_PARSING_ERROR);
        }

        return oAuthToken;
    }

    // 발급 받은 액세스 토큰을 사용하여 카카오에서 사용자 정보 가져오기
    public KakaoResponse.UserInfo getUserInfo(String token) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", CONTENT_TYPE);
        headers.add("Authorization", "Bearer " + token);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        HttpEntity<LinkedMultiValueMap<String, String>> kakaoRequest = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://kapi.kakao.com/v2/user/me",
            HttpMethod.POST,
            kakaoRequest,
            String.class);
        System.out.println("response = " + response.getBody());

        KakaoResponse.UserInfo userInfo = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            userInfo = objectMapper.readValue(response.getBody(), KakaoResponse.UserInfo.class);
            log.info(userInfo.getKakao_account().getEmail());
        } catch (Exception e) {
            throw new CustomException(JSON_PARSING_ERROR);
        }

        return userInfo;
    }

    public void kakaoUnlink(Long targetId) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", CONTENT_TYPE);
        headers.add("Authorization", "KakaoAK " + admin);

        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", targetId.toString());

        HttpEntity<LinkedMultiValueMap<String, String>> kakaoRequest = new HttpEntity<>(params, headers);
        ResponseEntity<String> response = restTemplate.exchange(
            "https://kapi.kakao.com/v1/user/unlink",
            HttpMethod.POST,
            kakaoRequest,
            String.class);
        System.out.println("response = " + response.getBody());

        KakaoResponse.UnlinkInfo unlinkInfo = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            unlinkInfo = objectMapper.readValue(response.getBody(), KakaoResponse.UnlinkInfo.class);
            log.info(unlinkInfo.getId().toString());
        } catch (Exception e) {
            throw new CustomException(JSON_PARSING_ERROR);
        }

        if(!unlinkInfo.getId().equals(targetId)) {
            // throw new CustomException(KAKAO_UNLINK_ERROR);
        }
    }
}
