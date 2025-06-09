package com.bidmyhobby.hobbyaibackend.profiles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.bidmyhobby.hobbyaibackend.Utils.generateMyersBriggsTypes;
import static com.bidmyhobby.hobbyaibackend.Utils.selfieTypes;

@Service
public class ProfileCreationService {

    private static final String STABLE_DIFFUSION_URL = "https://fe97a6a77c5448ab52.gradio.live/sdapi/v1/txt2img";

    private final OpenAiChatClient chatClient;
    private final ProfileRepository profileRepository;
    private final HttpClient httpClient;
    private final HttpRequest.Builder stableDiffusionRequestBuilder;
    private final List<Profile> generatedProfiles = new ArrayList<>();

    private static final String PROFILES_FILE_PATH = "profiles.json";

    @Value("${startup-actions.initializeProfiles:false}")
    private Boolean initializeProfiles;

    @Value("${hobbyai.lookingForGender:Female}")
    private String lookingForGender;

    @Value("#{${hobbyai.character.user}}")
    private Map<String, String> userProfileProperties;

    public ProfileCreationService(OpenAiChatClient chatClient, ProfileRepository profileRepository) {
        this.chatClient = chatClient;
        this.profileRepository = profileRepository;
        this.httpClient = HttpClient.newHttpClient();
        this.stableDiffusionRequestBuilder = HttpRequest.newBuilder()
                .setHeader("Content-type", "application/json")
                .uri(URI.create(STABLE_DIFFUSION_URL));
    }

    private static <T> T getRandomElement(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public void createProfiles(int numberOfProfiles) {
        if (!Boolean.TRUE.equals(this.initializeProfiles)) {
            System.out.println("Profile creation is disabled. Set startup-actions.initializeProfiles=true to enable.");
            return;
        }

        System.out.println("Starting profile creation. Target: " + numberOfProfiles + " profiles");
        
        // Create sample profiles for testing when AI is not available
        if (Boolean.TRUE.equals(initializeProfiles) && "sk-dummy-key".equals(System.getenv("SPRING_AI_OPENAI_API_KEY"))) {
            System.out.println("Using dummy API key. Creating sample profiles instead of calling OpenAI.");
            createSampleProfiles(numberOfProfiles);
            saveProfilesToJson(this.generatedProfiles);
            return;
        }

        List<Integer> ages = new ArrayList<>();
        for (int i = 20; i <= 35; i++) {
            ages.add(i);
        }
        List<String> ethnicities = List.of("White", "Black", "Asian", "Indian", "Hispanic");
        List<String> hobbies = List.of(
            "Painting", "Dance", "Coin Collection", "Jewelry Design", 
            "Yoga", "Cooking", "Photography", "Pottery", 
            "Fashion", "Music", "Writing", "Gardening"
        );
        String gender = this.lookingForGender;

        int attempts = 0;
        int maxAttempts = numberOfProfiles * 2; // Limit attempts to avoid infinite loop
        
        while (this.generatedProfiles.size() < numberOfProfiles && attempts < maxAttempts) {
            attempts++;
            int age = getRandomElement(ages);
            String ethnicity = getRandomElement(ethnicities);
            String mainHobby = getRandomElement(hobbies);

            String promptText = "Create a profile for a " + age + " year old " + ethnicity + " " + gender + 
                " who is passionate about " + mainHobby + " for a hobby-sharing platform. " +
                "Include first name, last name, age, ethnicity, gender, mainHobby (which is " + mainHobby + "), " +
                "and a brief bio that highlights this hobby. " +
                "Save the profile using the saveProfile function.";
            
            System.out.println("Attempt " + attempts + ": Creating profile with prompt: " + promptText);

            try {
                ChatResponse response = chatClient.call(new Prompt(promptText,
                        OpenAiChatOptions.builder().withFunction("saveProfile").build()));
                
                System.out.println("AI response received: " + response.getResult().getOutput().getContent());
                
                // If no profile was added by the function call, create a sample one
                if (this.generatedProfiles.size() == 0 || 
                    this.generatedProfiles.size() == attempts - numberOfProfiles) {
                    System.out.println("Function call didn't add a profile. Creating sample profile.");
                    createSampleProfile(age, ethnicity, gender, mainHobby);
                }
            } catch (Exception e) {
                System.out.println("Error calling AI: " + e.getMessage());
                createSampleProfile(age, ethnicity, gender, mainHobby);
            }
            
            System.out.println("Current profile count: " + this.generatedProfiles.size());
        }

        System.out.println("Profile creation completed. Created " + this.generatedProfiles.size() + " profiles.");
        saveProfilesToJson(this.generatedProfiles);
    }
    
    private void createSampleProfiles(int count) {
        List<String> firstNames = List.of("Emma", "Olivia", "Ava", "Sophia", "Isabella", "Mia", "Charlotte", "Amelia", "Harper", "Evelyn");
        List<String> lastNames = List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez");
        List<Integer> ages = List.of(24, 26, 28, 30, 32, 34, 36, 38);
        List<String> ethnicities = List.of("White", "Black", "Asian", "Indian", "Hispanic");
        List<String> hobbies = List.of(
            "Painting", "Dance", "Coin Collection", "Jewelry Design", 
            "Yoga", "Cooking", "Photography", "Pottery", 
            "Fashion", "Music", "Writing", "Gardening"
        );
        
        for (int i = 0; i < count; i++) {
            String firstName = getRandomElement(firstNames);
            String lastName = getRandomElement(lastNames);
            int age = getRandomElement(ages);
            String ethnicity = getRandomElement(ethnicities);
            String hobby = getRandomElement(hobbies);
            
            String uuid = UUID.randomUUID().toString();
            Profile profile = new Profile(
                uuid,
                firstName,
                lastName,
                age,
                ethnicity,
                Gender.FEMALE,
                "I love " + hobby + " and enjoy sharing my passion with others.",
                uuid + ".jpg",
                hobby
            );
            
            this.generatedProfiles.add(profile);
            System.out.println("Created sample profile: " + firstName + " " + lastName);
        }
    }
    
    private void createSampleProfile(int age, String ethnicity, String gender, String hobby) {
        String uuid = UUID.randomUUID().toString();
        String firstName = getRandomElement(List.of("Emma", "Olivia", "Ava", "Sophia", "Isabella", "Mia", "Charlotte"));
        String lastName = getRandomElement(List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller"));
        
        Profile profile = new Profile(
            uuid,
            firstName,
            lastName,
            age,
            ethnicity,
            Gender.FEMALE,
            "I love " + hobby + " and enjoy sharing my passion with others.",
            uuid + ".jpg",
            hobby
        );
        this.generatedProfiles.add(profile);
        System.out.println("Created sample profile: " + firstName + " " + lastName);
    }

    private void saveProfilesToJson(List<Profile> profiles) {
        try {
            Gson gson = new Gson();
            List<Profile> existingProfiles = new ArrayList<>();
            File profilesFile = new File(PROFILES_FILE_PATH);
            if (profilesFile.exists()) {
                try {
                    existingProfiles = gson.fromJson(new FileReader(profilesFile), 
                        new TypeToken<ArrayList<Profile>>() {}.getType());
                    if (existingProfiles == null) {
                        existingProfiles = new ArrayList<>();
                    }
                } catch (Exception e) {
                    System.out.println("Error reading existing profiles: " + e.getMessage());
                }
            }

            if (existingProfiles != null) {
                profiles.addAll(existingProfiles);
            }
            
            try (FileWriter writer = new FileWriter(PROFILES_FILE_PATH)) {
                gson.toJson(profiles, writer);
                System.out.println("Saved " + profiles.size() + " profiles to " + PROFILES_FILE_PATH);
            }
        } catch (IOException e) {
            System.out.println("Error saving profiles to JSON: " + e.getMessage());
        }
    }

    @Bean
    @Description("Save the profile information")
    public Function<Profile, Boolean> saveProfile() {
        return profile -> {
            System.out.println("Function called by AI to save profile:");
            System.out.println(profile);
            this.generatedProfiles.add(profile);
            return true;
        };
    }

    public void saveProfilesToDB() {
        // First save the user profile
        try {
            Profile userProfile = new Profile(
                    userProfileProperties.get("id"),
                    userProfileProperties.get("firstName"),
                    userProfileProperties.get("lastName"),
                    Integer.parseInt(userProfileProperties.get("age")),
                    userProfileProperties.get("ethnicity"),
                    Gender.valueOf(userProfileProperties.get("gender")),
                    userProfileProperties.get("bio"),
                    userProfileProperties.get("imageUrl"),
                    userProfileProperties.get("mainHobby")
            );
            profileRepository.save(userProfile);
            System.out.println("Saved user profile to database: " + userProfile.firstName() + " " + userProfile.lastName());
        } catch (Exception e) {
            System.out.println("Error saving user profile: " + e.getMessage());
        }
        
        // Then try to load and save profiles from file
        try {
            File profilesFile = new File(PROFILES_FILE_PATH);
            if (profilesFile.exists()) {
                Gson gson = new Gson();
                List<Profile> profiles = gson.fromJson(
                        new FileReader(profilesFile),
                        new TypeToken<ArrayList<Profile>>() {}.getType()
                );
                
                if (profiles != null && !profiles.isEmpty()) {
                    profileRepository.saveAll(profiles);
                    System.out.println("Saved " + profiles.size() + " profiles from file to database");
                } else {
                    System.out.println("No profiles found in file or file is empty");
                }
            } else {
                System.out.println("Profiles file not found: " + PROFILES_FILE_PATH);
            }
        } catch (Exception e) {
            System.out.println("Error loading profiles from file: " + e.getMessage());
        }
    }
}