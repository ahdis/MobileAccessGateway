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
 * OAuth2 error response (JSON)
 *
 * @author alexander kreutz
 *
 */
public class ErrorResponse {

    private String error;
    private String error_description;

    public ErrorResponse() {
    }

    public String getError() {
        return this.error;
    }

    public String getError_description() {
        return this.error_description;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setError_description(String error_description) {
        this.error_description = error_description;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ErrorResponse)) return false;
        final ErrorResponse other = (ErrorResponse) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$error = this.getError();
        final Object other$error = other.getError();
        if (this$error == null ? other$error != null : !this$error.equals(other$error)) return false;
        final Object this$error_description = this.getError_description();
        final Object other$error_description = other.getError_description();
        if (this$error_description == null ? other$error_description != null : !this$error_description.equals(
                other$error_description)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ErrorResponse;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $error = this.getError();
        result = result * PRIME + ($error == null ? 43 : $error.hashCode());
        final Object $error_description = this.getError_description();
        result = result * PRIME + ($error_description == null ? 43 : $error_description.hashCode());
        return result;
    }

    public String toString() {
        return "ErrorResponse(error=" + this.getError() + ", error_description=" + this.getError_description() + ")";
    }
}
