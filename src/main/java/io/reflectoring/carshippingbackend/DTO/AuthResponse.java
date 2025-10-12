package io.reflectoring.carshippingbackend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}
