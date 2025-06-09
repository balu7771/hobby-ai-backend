package com.bidmyhobby.hobbyaibackend.profiles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
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
        
        // Always create sample profiles to avoid null values
        createSampleProfiles(numberOfProfiles);
        saveProfilesToJson(this.generatedProfiles);
        System.out.println("Created " + this.generatedProfiles.size() + " sample profiles");
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
        List<String> bios = List.of(
            "I love sharing my passion for %s with others. It brings me joy to connect with people who appreciate the art.",
            "As a dedicated %s enthusiast, I find it's a great way to express myself and connect with like-minded people.",
            "My journey with %s started 5 years ago, and it has transformed my life in amazing ways.",
            "%s has been my passion since childhood. I love discussing techniques and ideas with fellow enthusiasts.",
            "When I'm not busy with work, you'll find me immersed in %s. It's my way of unwinding and finding joy."
        );
        
        for (int i = 0; i < count; i++) {
            String firstName = getRandomElement(firstNames);
            String lastName = getRandomElement(lastNames);
            int age = getRandomElement(ages);
            String ethnicity = getRandomElement(ethnicities);
            String hobby = getRandomElement(hobbies);
            String bioTemplate = getRandomElement(bios);
            String bio = String.format(bioTemplate, hobby);
            
            String uuid = UUID.randomUUID().toString();
            Profile profile = new Profile(
                uuid,
                firstName,
                lastName,
                age,
                ethnicity,
                Gender.FEMALE,
                bio,
                uuid + ".jpg",
                hobby
            );
            
            this.generatedProfiles.add(profile);
            System.out.println("Created sample profile: " + firstName + " " + lastName);
        }
    }

    private void saveProfilesToJson(List<Profile> profiles) {
        try {
            // Filter out any profiles with null values
            List<Profile> validProfiles = profiles.stream()
                .filter(p -> p.firstName() != null && p.lastName() != null && p.bio() != null && p.mainHobby() != null)
                .toList();
            
            Gson gson = new Gson();
            try (FileWriter writer = new FileWriter(PROFILES_FILE_PATH)) {
                gson.toJson(validProfiles, writer);
                System.out.println("Saved " + validProfiles.size() + " profiles to " + PROFILES_FILE_PATH);
            }
        } catch (IOException e) {
            System.out.println("Error saving profiles to JSON: " + e.getMessage());
        }
    }

    @Bean
    @Description("Save the profile information")
    public Function<Profile, Boolean> saveProfile() {
        return profile -> {
            // Validate profile before adding
            if (profile.firstName() == null || profile.lastName() == null || 
                profile.bio() == null || profile.mainHobby() == null) {
                System.out.println("Rejected invalid profile with null values");
                return false;
            }
            
            System.out.println("Function called by AI to save profile: " + profile.firstName() + " " + profile.lastName());
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
                    // Filter out any profiles with null values
                    List<Profile> validProfiles = profiles.stream()
                        .filter(p -> p.firstName() != null && p.lastName() != null && 
                                    p.bio() != null && p.mainHobby() != null)
                        .toList();
                    
                    profileRepository.saveAll(validProfiles);
                    System.out.println("Saved " + validProfiles.size() + " profiles from file to database");
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