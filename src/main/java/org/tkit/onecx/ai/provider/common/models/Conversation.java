package org.tkit.onecx.ai.provider.common.models;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Conversation {

    private List<ChatMessage> history = new ArrayList<>();

}
