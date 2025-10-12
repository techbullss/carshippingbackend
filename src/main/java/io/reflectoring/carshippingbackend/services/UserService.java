package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.DTO.SignupRequest;
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
}
