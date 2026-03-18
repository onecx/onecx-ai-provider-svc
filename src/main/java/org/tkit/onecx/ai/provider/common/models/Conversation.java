package org.tkit.onecx.ai.provider.common.models;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private List<ChatMessage> history = new ArrayList<>();

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
}
