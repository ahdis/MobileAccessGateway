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

import java.util.ArrayList;
import java.util.List;

/**
 * Information for a Get-X-User Assertion Request
 *
 * @author alexander kreutz
 *
 */
public class AssertionRequest {

    private String purposeOfUse;
    private String role;
    private String resourceId;
    private String principalID;
    private String principalName;
    private List<String> organizationID;
    private List<String> organizationName;
    private Object samlToken;

    public void addOrganizationID(String orgId) {
        if (organizationID == null) organizationID = new ArrayList<>(4);
        organizationID.add(orgId);
    }

    public void addOrganizationName(String orgName) {
        if (organizationName == null) organizationName = new ArrayList<>(4);
        organizationName.add(orgName);
    }

    public String getPurposeOfUse() {
        return this.purposeOfUse;
    }

    public void setPurposeOfUse(final String purposeOfUse) {
        this.purposeOfUse = purposeOfUse;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public String getResourceId() {
        return this.resourceId;
    }

    public void setResourceId(final String resourceId) {
        this.resourceId = resourceId;
    }

    public String getPrincipalID() {
        return this.principalID;
    }

    public void setPrincipalID(final String principalID) {
        this.principalID = principalID;
    }

    public String getPrincipalName() {
        return this.principalName;
    }

    public void setPrincipalName(final String principalName) {
        this.principalName = principalName;
    }

    public List<String> getOrganizationID() {
        return this.organizationID;
    }

    public void setOrganizationID(final List<String> organizationID) {
        this.organizationID = organizationID;
    }

    public List<String> getOrganizationName() {
        return this.organizationName;
    }

    public void setOrganizationName(final List<String> organizationName) {
        this.organizationName = organizationName;
    }

    public Object getSamlToken() {
        return this.samlToken;
    }

    public void setSamlToken(final Object samlToken) {
        this.samlToken = samlToken;
    }
}
