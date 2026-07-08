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

    @Transient
    private String source = "TENANT";
}
