package com.bidmyhobby.hobbyaibackend.conversations;

import com.bidmyhobby.hobbyaibackend.profiles.Profile;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationService {

    private final ChatClient chatClient;

    public ConversationService(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public Conversation generateProfileResponse(Conversation conversation, Profile profile, Profile user) {
        // System message
        String systemMessageStr = 
                "You are a " + profile.age() + " year old " + profile.ethnicity() + " " + profile.gender() + " called " + profile.firstName() + " " + profile.lastName() + " matched " +
                "with a " + user.age() + " year old " + user.ethnicity() + " " + user.gender() + " called " + user.firstName() + " " + user.lastName() + " on Hobby AI platform.\n" +
                "This is an in-app text conversation between you two.\n" +
                "Pretend to be the provided person and respond to the conversation as if writing on a dating app.\n" +
                "Your bio is: " + profile.bio() + " and your main hobby is " + profile.mainHobby() + ". Respond in the role of this person only.\n" +
                "Keep your responses brief, friendly, and engaging.";
                
        SystemMessage systemMessage = new SystemMessage(systemMessageStr);

        List<Message> allMessages = new ArrayList<>();
        allMessages.add(systemMessage);
        
        // Add conversation history
        for (ChatMessage msg : conversation.messages()) {
            allMessages.add(new UserMessage(msg.messageText()));
        }

        try {
            Prompt prompt = new Prompt(allMessages);
            ChatResponse response = chatClient.call(prompt);
            conversation.messages().add(new ChatMessage(
                    response.getResult().getOutput().getContent(),
                    profile.id(),
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            // Fallback response if AI fails
            conversation.messages().add(new ChatMessage(
                    "Hi there! I'm " + profile.firstName() + ". I love " + profile.mainHobby() + ". What about you?",
                    profile.id(),
                    LocalDateTime.now()
            ));
        }
        return conversation;
    }
}