package com.bidmyhobby.hobbyaibackend.profiles;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Service
public class SimpleProfileService {
    
    private final ProfileRepository profileRepository;
    private static final String PROFILES_FILE_PATH = "profiles.json";
    
    public SimpleProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }
    
    public void saveProfilesToDB() {
        try {
            File profilesFile = new File(PROFILES_FILE_PATH);
            if (profilesFile.exists()) {
                Gson gson = new Gson();
                java.util.List<Profile> profiles = gson.fromJson(
                    new FileReader(profilesFile), 
                    new TypeToken<java.util.List<Profile>>() {}.getType()
                );
                
                if (profiles != null && !profiles.isEmpty()) {
                    profileRepository.saveAll(profiles);
                    System.out.println("Loaded " + profiles.size() + " profiles from file");
                }
            } else {
                System.out.println("No profiles file found at " + PROFILES_FILE_PATH);
            }
        } catch (IOException e) {
            System.out.println("Error loading profiles: " + e.getMessage());
        }
    }
}