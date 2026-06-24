package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SCAFFOLD")
@SQLRestriction("TENANT_ID is null")
public class GlobalScaffold extends AbstractScaffold {
}
