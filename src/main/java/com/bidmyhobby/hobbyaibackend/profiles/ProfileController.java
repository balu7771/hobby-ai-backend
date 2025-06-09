package com.bidmyhobby.hobbyaibackend.profiles;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileRepository profileRepository;

    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    @GetMapping("/{id}")
    public Profile getProfileById(@PathVariable String id) {
        return profileRepository.findById(id).orElse(null);
    }
    
    @GetMapping("/random")
    public Profile getRandomProfile() {
        return profileRepository.getRandomProfile();
    }
}