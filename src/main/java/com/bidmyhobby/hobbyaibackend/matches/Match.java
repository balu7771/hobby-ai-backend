package com.bidmyhobby.hobbyaibackend.matches;

import com.bidmyhobby.hobbyaibackend.profiles.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Match(
        @Id
        String id,
        Profile profile,
        String conversationId
) {
}