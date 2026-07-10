package com.investment.tracker.config;

import com.investment.tracker.model.*;
import com.investment.tracker.repository.SymbolMappingRepository;
import com.investment.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Initializes the database with demo users and symbol mappings on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SymbolMappingRepository symbolMappingRepository;

    @Override
    public void run(String... args) {
        // Check if users already exist
        if (userRepository.count() == 0) {
            log.info("Initializing database with demo users...");

            // Create admin user
            User admin = User.builder()
                    .username("admin")
                    .email("admin@investmenttracker.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Created admin user: username='admin', password='admin123'");

            // Create demo regular user
            User demoUser = User.builder()
                    .username("demo")
                    .email("demo@investmenttracker.com")
                    .password(passwordEncoder.encode("demo123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(demoUser);
            log.info("Created demo user: username='demo', password='demo123'");

            log.info("Database initialization complete. {} users created.", userRepository.count());
        } else {
            log.info("Database already contains {} users. Skipping initialization.", userRepository.count());
        }
        
        // Initialize symbol mappings
        initializeSymbolMappings();
    }
    
    /**
     * Load symbol mappings from CSV seed file
     */
    private void initializeSymbolMappings() {
        // Check if symbol mappings already exist
        if (symbolMappingRepository.count() > 0) {
            log.info("Database already contains {} symbol mappings. Skipping initialization.", 
                     symbolMappingRepository.count());
            return;
        }
        
        log.info("Loading symbol mappings from seed file...");
        
        try {
            ClassPathResource resource = new ClassPathResource("data/symbol-mappings-seed.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            String line;
            int count = 0;
            int skipped = 0;
            
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    continue;
                }
                
                try {
                    // Parse CSV line: userSymbol,yahooSymbol,googleSymbol,market,assetType,name,isin
                    String[] parts = line.split(",", -1); // -1 to include empty trailing fields
                    if (parts.length < 7) {
                        log.warn("Invalid CSV line (expected 7 fields): {}", line);
                        skipped++;
                        continue;
                    }
                    
                    String userSymbol = parts[0].trim();
                    String yahooSymbol = parts[1].trim();
                    String googleSymbol = parts[2].trim();
                    String marketStr = parts[3].trim();
                    String assetTypeStr = parts[4].trim();
                    String name = parts[5].trim();
                    String isin = parts[6].trim();
                    
                    // Skip if user symbol already exists
                    if (symbolMappingRepository.existsByUserSymbol(userSymbol)) {
                        skipped++;
                        continue;
                    }
                    
                    // Create symbol mapping
                    SymbolMapping mapping = SymbolMapping.builder()
                        .userSymbol(userSymbol)
                        .yahooSymbol(yahooSymbol.isEmpty() ? null : yahooSymbol)
                        .googleSymbol(googleSymbol.isEmpty() ? null : googleSymbol)
                        .market(Market.valueOf(marketStr))
                        .assetType(InvestmentType.valueOf(assetTypeStr))
                        .name(name.isEmpty() ? null : name)
                        .isin(isin.isEmpty() ? null : isin)
                        .isVerified(false) // Will be verified on first API call
                        .build();
                    
                    symbolMappingRepository.save(mapping);
                    count++;
                    
                } catch (Exception e) {
                    log.warn("Error parsing CSV line: {} - {}", line, e.getMessage());
                    skipped++;
                }
            }
            
            reader.close();
            log.info("Symbol mappings initialization complete. {} mappings loaded, {} skipped.", 
                     count, skipped);
            
        } catch (Exception e) {
            log.error("Error loading symbol mappings seed file: {}", e.getMessage(), e);
        }
    }
}
