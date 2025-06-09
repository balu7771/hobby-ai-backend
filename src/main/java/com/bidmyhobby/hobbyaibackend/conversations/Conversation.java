package com.bidmyhobby.hobbyaibackend.conversations;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public record Conversation(
        @Id
        String id,
        String profileId,
        List<ChatMessage> messages
) {
}