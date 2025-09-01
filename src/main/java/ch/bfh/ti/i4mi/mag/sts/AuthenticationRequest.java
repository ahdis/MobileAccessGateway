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
 * Data required during OAuth2 authentication
 *
 * @author alexander kreutz
 *
 */
public class AuthenticationRequest {

    private String scope;
    private String redirect_uri;
    private String client_id;
    private String state;
    private String token_type;
    private String assertion;
    private String idpAssertion;
    private String code_challenge;

    public AuthenticationRequest() {
    }

    public String getScope() {
        return this.scope;
    }

    public String getRedirect_uri() {
        return this.redirect_uri;
    }

    public String getClient_id() {
        return this.client_id;
    }

    public String getState() {
        return this.state;
    }

    public String getToken_type() {
        return this.token_type;
    }

    public String getAssertion() {
        return this.assertion;
    }

    public String getIdpAssertion() {
        return this.idpAssertion;
    }

    public String getCode_challenge() {
        return this.code_challenge;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setRedirect_uri(String redirect_uri) {
        this.redirect_uri = redirect_uri;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public void setIdpAssertion(String idpAssertion) {
        this.idpAssertion = idpAssertion;
    }

    public void setCode_challenge(String code_challenge) {
        this.code_challenge = code_challenge;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof AuthenticationRequest)) return false;
        final AuthenticationRequest other = (AuthenticationRequest) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$scope = this.getScope();
        final Object other$scope = other.getScope();
        if (this$scope == null ? other$scope != null : !this$scope.equals(other$scope)) return false;
        final Object this$redirect_uri = this.getRedirect_uri();
        final Object other$redirect_uri = other.getRedirect_uri();
        if (this$redirect_uri == null ? other$redirect_uri != null : !this$redirect_uri.equals(other$redirect_uri))
            return false;
        final Object this$client_id = this.getClient_id();
        final Object other$client_id = other.getClient_id();
        if (this$client_id == null ? other$client_id != null : !this$client_id.equals(other$client_id)) return false;
        final Object this$state = this.getState();
        final Object other$state = other.getState();
        if (this$state == null ? other$state != null : !this$state.equals(other$state)) return false;
        final Object this$token_type = this.getToken_type();
        final Object other$token_type = other.getToken_type();
        if (this$token_type == null ? other$token_type != null : !this$token_type.equals(other$token_type))
            return false;
        final Object this$assertion = this.getAssertion();
        final Object other$assertion = other.getAssertion();
        if (this$assertion == null ? other$assertion != null : !this$assertion.equals(other$assertion)) return false;
        final Object this$idpAssertion = this.getIdpAssertion();
        final Object other$idpAssertion = other.getIdpAssertion();
        if (this$idpAssertion == null ? other$idpAssertion != null : !this$idpAssertion.equals(other$idpAssertion))
            return false;
        final Object this$code_challenge = this.getCode_challenge();
        final Object other$code_challenge = other.getCode_challenge();
        if (this$code_challenge == null ? other$code_challenge != null : !this$code_challenge.equals(
                other$code_challenge)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof AuthenticationRequest;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $scope = this.getScope();
        result = result * PRIME + ($scope == null ? 43 : $scope.hashCode());
        final Object $redirect_uri = this.getRedirect_uri();
        result = result * PRIME + ($redirect_uri == null ? 43 : $redirect_uri.hashCode());
        final Object $client_id = this.getClient_id();
        result = result * PRIME + ($client_id == null ? 43 : $client_id.hashCode());
        final Object $state = this.getState();
        result = result * PRIME + ($state == null ? 43 : $state.hashCode());
        final Object $token_type = this.getToken_type();
        result = result * PRIME + ($token_type == null ? 43 : $token_type.hashCode());
        final Object $assertion = this.getAssertion();
        result = result * PRIME + ($assertion == null ? 43 : $assertion.hashCode());
        final Object $idpAssertion = this.getIdpAssertion();
        result = result * PRIME + ($idpAssertion == null ? 43 : $idpAssertion.hashCode());
        final Object $code_challenge = this.getCode_challenge();
        result = result * PRIME + ($code_challenge == null ? 43 : $code_challenge.hashCode());
        return result;
    }

    public String toString() {
        return "AuthenticationRequest(scope=" + this.getScope() + ", redirect_uri=" + this.getRedirect_uri() + ", client_id=" + this.getClient_id() + ", state=" + this.getState() + ", token_type=" + this.getToken_type() + ", assertion=" + this.getAssertion() + ", idpAssertion=" + this.getIdpAssertion() + ", code_challenge=" + this.getCode_challenge() + ")";
    }
}
