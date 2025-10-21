package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.SignupRequest;
import io.reflectoring.carshippingbackend.DTO.UpdateUserRequest;
import io.reflectoring.carshippingbackend.Enum.Role;
import io.reflectoring.carshippingbackend.configaration.CustomUserDetails;
import io.reflectoring.carshippingbackend.repository.UserRepository;
import io.reflectoring.carshippingbackend.tables.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(SignupRequest signupRequest, Set<Role> roles) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPhone(signupRequest.getPhone());
        user.setDateOfBirth(signupRequest.getDateOfBirth());
        user.setGender(signupRequest.getGender());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setStreetAddress(signupRequest.getStreetAddress());
        user.setCity(signupRequest.getCity());
        user.setState(signupRequest.getState());
        user.setPostalCode(signupRequest.getPostalCode());
        user.setCountry(signupRequest.getCountry());
        user.setPreferredCommunication(signupRequest.getPreferredCommunication());
        user.setNewsletter(signupRequest.isNewsletter());
        user.setShippingFrequency(signupRequest.getShippingFrequency());
        user.setVehicleType(signupRequest.getVehicleType());
        user.setEstimatedShippingDate(signupRequest.getEstimatedShippingDate());
        user.setSourceCountry(signupRequest.getSourceCountry());
        user.setDestinationCountry(signupRequest.getDestinationCountry());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user); // wrap entity inside CustomUserDetails
    }
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }



    public User updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update fields if provided in request
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getStreetAddress() != null) user.setStreetAddress(request.getStreetAddress());
        if (request.getCity() != null) user.setCity(request.getCity());
        if (request.getState() != null) user.setState(request.getState());
        if (request.getPostalCode() != null) user.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getNewsletter() != null) user.setNewsletter(request.getNewsletter());
        if (request.getShippingFrequency() != null) user.setShippingFrequency(request.getShippingFrequency());
        if (request.getVehicleType() != null) user.setVehicleType(request.getVehicleType());
        if (request.getEstimatedShippingDate() != null) user.setEstimatedShippingDate(request.getEstimatedShippingDate());

        return userRepository.save(user);
    }

    public User updateUserRoles(Long id, String role, String action) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());

            if ("ADD".equalsIgnoreCase(action)) {
                user.getRoles().add(roleEnum);
            } else if ("REMOVE".equalsIgnoreCase(action)) {
                user.getRoles().remove(roleEnum);
            } else {
                throw new RuntimeException("Invalid action. Use 'ADD' or 'REMOVE'");
            }

            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + role);
        }
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }


}
