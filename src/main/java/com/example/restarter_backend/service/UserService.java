package com.example.restarter_backend.service;

import com.example.restarter_backend.entity.Role;
import com.example.restarter_backend.entity.User;
import com.example.restarter_backend.dto.RegisterRequest;
import com.example.restarter_backend.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // Import Optional

@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    /**
     * Finds a user by their username.
     * This method is called by AuthController to check for existing users.
     * @param username The username to search for.
     * @return An Optional containing the User if found, empty otherwise.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean isUserOwner(Long userId, String username) {
        return userRepository.findById(userId)
                .map(user -> user.getUsername().equals(username))
                .orElse(false);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(RegisterRequest request) {
        // The AuthController now calls findByUsername before this, but
        // this check is good for robustness if createUser is called directly elsewhere.
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MEMBER);
        user.setRegistrationDate(LocalDate.now());
        user.setMembershipExpiryDate(LocalDate.now().plusYears(1));

        return userRepository.save(user);
    }

    public User updateUser(Long id, User updates) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getContactInfo() != null) existing.setContactInfo(updates.getContactInfo());

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    public List<User> searchUsersByName(String name) {
        return userRepository.findAll().stream()
                .filter(user -> user.getName() != null &&
                             user.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }
}


//Jun.6 need to add a public method findByUsername(String username) 
//to your UserService that simply delegates the call to your userRepository.