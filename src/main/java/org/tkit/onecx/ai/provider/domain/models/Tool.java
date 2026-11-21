package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.TenantId;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "TOOL")
public class Tool extends AbstractTool {

    @TenantId
    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "EXTERNAL", nullable = false)
    private boolean external;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "APP_ID")
    private String appId;

    @Transient
    private String source = "TENANT";
}
