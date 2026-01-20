package com.team8.damo.kakao;

import lombok.Getter;

@Getter
public class KakaoResponse {
    @Getter
    public static class OAuthToken {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private int expires_in;
        private String scope;
        private int refresh_token_expires_in;
    }

    @Getter
    public static class UserInfo {
        private Long id;
        private String connected_at;
        private KakaoAccount kakao_account;
        @Getter
        public class KakaoAccount {
            private Boolean has_email;
            private Boolean email_needs_agreement;
            private Boolean is_email_valid;
            private Boolean is_email_verified;
            private String email;
        }
    }

    @Getter
    public static class UnlinkInfo {
        private Long id;
    }
}
