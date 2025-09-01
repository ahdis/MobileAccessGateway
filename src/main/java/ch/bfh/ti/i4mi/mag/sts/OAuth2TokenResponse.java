/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ch.bfh.ti.i4mi.mag.sts;

/**
 * Data used as response for OAuth2 token exchange
 *
 * @author alexander kreutz
 *
 */
public class OAuth2TokenResponse {

    private String access_token;
    private String refresh_token;
    private String token_type;
    private long expires_in;
    private String scope;

    public OAuth2TokenResponse() {
    }

    public String getAccess_token() {
        return this.access_token;
    }

    public String getRefresh_token() {
        return this.refresh_token;
    }

    public String getToken_type() {
        return this.token_type;
    }

    public long getExpires_in() {
        return this.expires_in;
    }

    public String getScope() {
        return this.scope;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof OAuth2TokenResponse)) return false;
        final OAuth2TokenResponse other = (OAuth2TokenResponse) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$access_token = this.getAccess_token();
        final Object other$access_token = other.getAccess_token();
        if (this$access_token == null ? other$access_token != null : !this$access_token.equals(other$access_token))
            return false;
        final Object this$refresh_token = this.getRefresh_token();
        final Object other$refresh_token = other.getRefresh_token();
        if (this$refresh_token == null ? other$refresh_token != null : !this$refresh_token.equals(other$refresh_token))
            return false;
        final Object this$token_type = this.getToken_type();
        final Object other$token_type = other.getToken_type();
        if (this$token_type == null ? other$token_type != null : !this$token_type.equals(other$token_type))
            return false;
        if (this.getExpires_in() != other.getExpires_in()) return false;
        final Object this$scope = this.getScope();
        final Object other$scope = other.getScope();
        if (this$scope == null ? other$scope != null : !this$scope.equals(other$scope)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof OAuth2TokenResponse;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $access_token = this.getAccess_token();
        result = result * PRIME + ($access_token == null ? 43 : $access_token.hashCode());
        final Object $refresh_token = this.getRefresh_token();
        result = result * PRIME + ($refresh_token == null ? 43 : $refresh_token.hashCode());
        final Object $token_type = this.getToken_type();
        result = result * PRIME + ($token_type == null ? 43 : $token_type.hashCode());
        final long $expires_in = this.getExpires_in();
        result = result * PRIME + (int) ($expires_in >>> 32 ^ $expires_in);
        final Object $scope = this.getScope();
        result = result * PRIME + ($scope == null ? 43 : $scope.hashCode());
        return result;
    }

    public String toString() {
        return "OAuth2TokenResponse(access_token=" + this.getAccess_token() + ", refresh_token=" + this.getRefresh_token() + ", token_type=" + this.getToken_type() + ", expires_in=" + this.getExpires_in() + ", scope=" + this.getScope() + ")";
    }
}
