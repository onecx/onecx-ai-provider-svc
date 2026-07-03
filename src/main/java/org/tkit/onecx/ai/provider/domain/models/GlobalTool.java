package org.tkit.onecx.ai.provider.domain.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "GLOBAL_TOOL")
public class GlobalTool extends AbstractTool {
}
