package org.tkit.onecx.ai.provider.domain.models;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.tkit.onecx.ai.provider.domain.models.enums.FilterKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class Filter implements Serializable {

    @Column(name = "FILTER_KEY")
    @Enumerated(EnumType.STRING)
    private FilterKey key;

    @Column(name = "FILTER_VALUE")
    private String value;
}
