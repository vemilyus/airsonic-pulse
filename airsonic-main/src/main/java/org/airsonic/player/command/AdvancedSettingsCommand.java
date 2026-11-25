/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.command;

import org.airsonic.player.controller.AdvancedSettingsController;

/**
 * Command used in {@link AdvancedSettingsController}.
 *
 * @author Sindre Mehus
 */
public class AdvancedSettingsCommand {

    private String downloadLimit;
    private String uploadLimit;
    private boolean ldapEnabled;
    private String ldapUrl;
    private String ldapSearchFilter;
    private String ldapManagerDn;
    private String ldapManagerPassword;
    private boolean ldapAutoShadowing;
    private String brand;

    private boolean oidcEnabled;
    private String oidcIssuerUri;
    private String oidcClientId;
    private String oidcClientSecret;
    private String oidcScopes;
    private String oidcUsernameClaim;
    private String oidcButtonLabel;
    private boolean oidcAutoCreateUsers;
    private boolean oidcLinkByEmail;
    private String oidcRequiredClaim;
    private String oidcRequiredClaimValue;
    private String oidcGroupsClaim;
    private String oidcAdminGroup;
    private String oidcRedirectUri;

    private String smtpServer;
    private String smtpEncryption;
    private String smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String smtpFrom;

    private boolean captchaEnabled;
    private String recaptchaSiteKey;
    private String recaptchaSecretKey;

    public String getDownloadLimit() {
        return downloadLimit;
    }

    public void setDownloadLimit(String downloadLimit) {
        this.downloadLimit = downloadLimit;
    }

    public String getUploadLimit() {
        return uploadLimit;
    }

    public void setUploadLimit(String uploadLimit) {
        this.uploadLimit = uploadLimit;
    }

    public boolean isLdapEnabled() {
        return ldapEnabled;
    }

    public void setLdapEnabled(boolean ldapEnabled) {
        this.ldapEnabled = ldapEnabled;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapSearchFilter() {
        return ldapSearchFilter;
    }

    public void setLdapSearchFilter(String ldapSearchFilter) {
        this.ldapSearchFilter = ldapSearchFilter;
    }

    public String getLdapManagerDn() {
        return ldapManagerDn;
    }

    public void setLdapManagerDn(String ldapManagerDn) {
        this.ldapManagerDn = ldapManagerDn;
    }

    public String getLdapManagerPassword() {
        return ldapManagerPassword;
    }

    public void setLdapManagerPassword(String ldapManagerPassword) {
        this.ldapManagerPassword = ldapManagerPassword;
    }

    public boolean isLdapAutoShadowing() {
        return ldapAutoShadowing;
    }

    public void setLdapAutoShadowing(boolean ldapAutoShadowing) {
        this.ldapAutoShadowing = ldapAutoShadowing;
    }

    public boolean isOidcEnabled() {
        return oidcEnabled;
    }

    public void setOidcEnabled(boolean oidcEnabled) {
        this.oidcEnabled = oidcEnabled;
    }

    public String getOidcIssuerUri() {
        return oidcIssuerUri;
    }

    public void setOidcIssuerUri(String oidcIssuerUri) {
        this.oidcIssuerUri = oidcIssuerUri;
    }

    public String getOidcClientId() {
        return oidcClientId;
    }

    public void setOidcClientId(String oidcClientId) {
        this.oidcClientId = oidcClientId;
    }

    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    public void setOidcClientSecret(String oidcClientSecret) {
        this.oidcClientSecret = oidcClientSecret;
    }

    public String getOidcScopes() {
        return oidcScopes;
    }

    public void setOidcScopes(String oidcScopes) {
        this.oidcScopes = oidcScopes;
    }

    public String getOidcUsernameClaim() {
        return oidcUsernameClaim;
    }

    public void setOidcUsernameClaim(String oidcUsernameClaim) {
        this.oidcUsernameClaim = oidcUsernameClaim;
    }

    public String getOidcButtonLabel() {
        return oidcButtonLabel;
    }

    public void setOidcButtonLabel(String oidcButtonLabel) {
        this.oidcButtonLabel = oidcButtonLabel;
    }

    public boolean isOidcAutoCreateUsers() {
        return oidcAutoCreateUsers;
    }

    public void setOidcAutoCreateUsers(boolean oidcAutoCreateUsers) {
        this.oidcAutoCreateUsers = oidcAutoCreateUsers;
    }

    public boolean isOidcLinkByEmail() {
        return oidcLinkByEmail;
    }

    public void setOidcLinkByEmail(boolean oidcLinkByEmail) {
        this.oidcLinkByEmail = oidcLinkByEmail;
    }

    public String getOidcRequiredClaim() {
        return oidcRequiredClaim;
    }

    public void setOidcRequiredClaim(String oidcRequiredClaim) {
        this.oidcRequiredClaim = oidcRequiredClaim;
    }

    public String getOidcRequiredClaimValue() {
        return oidcRequiredClaimValue;
    }

    public void setOidcRequiredClaimValue(String oidcRequiredClaimValue) {
        this.oidcRequiredClaimValue = oidcRequiredClaimValue;
    }

    public String getOidcGroupsClaim() {
        return oidcGroupsClaim;
    }

    public void setOidcGroupsClaim(String oidcGroupsClaim) {
        this.oidcGroupsClaim = oidcGroupsClaim;
    }

    public String getOidcAdminGroup() {
        return oidcAdminGroup;
    }

    public void setOidcAdminGroup(String oidcAdminGroup) {
        this.oidcAdminGroup = oidcAdminGroup;
    }

    public String getOidcRedirectUri() {
        return oidcRedirectUri;
    }

    public void setOidcRedirectUri(String oidcRedirectUri) {
        this.oidcRedirectUri = oidcRedirectUri;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setReloadNeeded(boolean reloadNeeded) {
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getSmtpEncryption() {
        return smtpEncryption;
    }

    public void setSmtpEncryption(String smtpEncryption) {
        this.smtpEncryption = smtpEncryption;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpFrom() {
        return smtpFrom;
    }

    public void setSmtpFrom(String smtpFrom) {
        this.smtpFrom = smtpFrom;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public String getRecaptchaSiteKey() {
        return recaptchaSiteKey;
    }

    public void setRecaptchaSiteKey(String recaptchaSiteKey) {
        this.recaptchaSiteKey = recaptchaSiteKey;
    }

    public String getRecaptchaSecretKey() {
        return recaptchaSecretKey;
    }

    public void setRecaptchaSecretKey(String recaptchaSecretKey) {
        this.recaptchaSecretKey = recaptchaSecretKey;
    }
}
