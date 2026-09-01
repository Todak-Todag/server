package com.todak_todag.api_gateway.token;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.todak_todag.api_gateway.config.AuthenticationProperties;
import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenParser {

	private static final String EXPECTED_ALGORITHM = "HS256";
	
	private final JwtParser jwtParser;
	
	private final AuthenticationProperties authenticationProperties;
	
	public JwtTokenParser(
			@Value("${jwt.secret}") String jwtSecret,
			AuthenticationProperties authenticationProperties
	) {
		this.jwtParser = createJwtParser(jwtSecret);
		this.authenticationProperties = authenticationProperties;
	}
	
	public AccessTokenClaims parse(String token) {
		if (token == null || token.isBlank()) {
			throw new TokenException(TokenErrorCode.UNAUTHORIZED);
		}
		
		try {
			Jws<Claims> signedClaims = jwtParser.parseSignedClaims(token);
			
			validateAlgorithm(signedClaims);
			
			Claims claims = signedClaims.getPayload();
			
			validateExpirationClaim(claims);
			
			String userId = claims.get(
					authenticationProperties.userIdClaim(),
					String.class
			);
			
			String role = claims.get(
					authenticationProperties.roleClaim(),
					String.class
			);
			
			return new AccessTokenClaims(userId, role);
		} catch (JwtException | IllegalArgumentException e) {
			throw new TokenException(TokenErrorCode.UNAUTHORIZED);
		}
	}
	
	private JwtParser createJwtParser(String jwtSecret) {
		if(jwtSecret == null || jwtSecret.isBlank()) {
			throw new IllegalStateException("jwt.secret 설정 정보는 필수입니다.");
		}
		
		try {
			byte[] decodeSecret = Decoders.BASE64.decode(jwtSecret);
			SecretKey key = Keys.hmacShaKeyFor(decodeSecret);
			
			return Jwts.parser()
					.verifyWith(key)
					.build();
		} catch (RuntimeException e) {
			throw new IllegalStateException(
					"jwt.secret 설정 정보는 base64 인코딩되어 있어야 합니다.",
					e
			);
		}
	}
	
	private void validateAlgorithm(Jws<Claims> signedClaims) {
		String algorithm = signedClaims.getHeader().getAlgorithm();
		
		if(!EXPECTED_ALGORITHM.equals(algorithm)) {
			throw new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN);
		}
	}
	
	private void validateExpirationClaim(Claims claims) {
		Date expiration = claims.getExpiration();
		
		if(expiration == null) {
			throw new TokenException(TokenErrorCode.INVALID_ACCESS_TOKEN);
		}
	}
	
}
