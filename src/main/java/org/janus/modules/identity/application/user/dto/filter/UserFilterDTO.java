package org.janus.modules.identity.application.user.dto.filter;

import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.filter.FilterBaseDTO;

import java.time.OffsetDateTime;
import java.util.List;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterDTO extends FilterBaseDTO {

    @QueryParam("email")
    private String email;

    @QueryParam("username")
    private String username;

    @QueryParam("fullName")
    private String fullName;

    @QueryParam("isEmailVerified")
    private Boolean isEmailVerified;

    @QueryParam("lastLoginAtFrom")
    private OffsetDateTime lastLoginAtFrom;

    @QueryParam("lastLoginAtTo")
    private OffsetDateTime lastLoginAtTo;

    @QueryParam("isActive")
    @Builder.Default
    private Boolean isActive = true;

    @QueryParam("orders")
    @Builder.Default
    private List<UserOrder> orders = List.of(UserOrder.CREATED_AT);

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public OffsetDateTime getLastLoginAtFrom() {
        return lastLoginAtFrom;
    }

    public void setLastLoginAtFrom(OffsetDateTime lastLoginAtFrom) {
        this.lastLoginAtFrom = lastLoginAtFrom;
    }

    public OffsetDateTime getLastLoginAtTo() {
        return lastLoginAtTo;
    }

    public void setLastLoginAtTo(OffsetDateTime lastLoginAtTo) {
        this.lastLoginAtTo = lastLoginAtTo;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public List<UserOrder> getOrders() {
        return orders;
    }

    public void setOrders(List<UserOrder> orders) {
        this.orders = orders;
    }
}