package com.investment.tracker.util;

import com.investment.tracker.exception.UnauthorizedAccessException;
import com.investment.tracker.model.User;
import com.investment.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility class for retrieving the currently authenticated user.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user's ID.
     * @return the user ID
     * @throws UnauthorizedAccessException if no user is authenticated
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user.getId();
    }

    /**
     * Get the currently authenticated user.
     * @return the User object
     * @throws UnauthorizedAccessException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));
    }

    /**
     * Get the username of the currently authenticated user.
     * @return the username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
}
