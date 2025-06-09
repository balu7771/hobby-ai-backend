package com.bidmyhobby.hobbyaibackend;

import com.bidmyhobby.hobbyaibackend.conversations.ConversationRepository;
import com.bidmyhobby.hobbyaibackend.matches.MatchRepository;
import com.bidmyhobby.hobbyaibackend.profiles.ProfileCreationService;
import com.bidmyhobby.hobbyaibackend.profiles.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HobbyAiBackendApplication implements CommandLineRunner {

	@Autowired
	private ProfileRepository profileRepository;
	
	@Autowired
	private ConversationRepository conversationRepository;
	
	@Autowired
	private MatchRepository matchRepository;

	@Autowired
	private ProfileCreationService profileCreationService;
	
	@Value("${startup-actions.initializeProfiles:false}")
	private Boolean initializeProfiles;

	public static void main(String[] args) {
		SpringApplication.run(HobbyAiBackendApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("Application starting...");
		
		if (Boolean.TRUE.equals(initializeProfiles)) {
			System.out.println("Initializing profiles (startup-actions.initializeProfiles=true)");
			clearAllData();
			profileCreationService.createProfiles(5); // Reduced to 5 profiles to avoid long startup times
		} else {
			System.out.println("Skipping profile initialization (startup-actions.initializeProfiles=false)");
		}
		
		profileCreationService.saveProfilesToDB();
		System.out.println("Application startup completed");
	}

	private void clearAllData() {
		System.out.println("Clearing existing data...");
		conversationRepository.deleteAll();
		matchRepository.deleteAll();
		profileRepository.deleteAll();
		System.out.println("Data cleared");
	}
}