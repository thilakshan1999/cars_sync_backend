package com.hinetics.caresync.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenUtil {
    private final SecretKey jwtSecret = Keys.hmacShaKeyFor(
            "Lx7@J2w!qP9#Tk8$Rm5&Ve1*Zb6@Yi3^".getBytes()
    );

    public String generateToken(String email, String username, String role) {
        Date now = new Date();
        // 24 hours
        long jwtExpirationMs = 1000 * 60 * 60 * 24;
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("username", username)
                .claim("role", role)
                .signWith(jwtSecret)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token expired: " + e.getMessage());
        } catch (JwtException e) {
            System.out.println("Token invalid: " + e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return  Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token).getPayload();
    }
}
