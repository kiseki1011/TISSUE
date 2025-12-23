package com.tissue.security.authentication;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.tissue.member.application.port.out.MemberQueryRepository;

import lombok.RequiredArgsConstructor;

// TODO: is the javadoc i wrote below correct?

/**
 * Spring security uses this service to search UserDetails
 * <p>
 * Gets Member from the DB using the email extracted from the JWT.
 * Then converts the Member to MemberUserDetails.
 * </p>
 * <p> Why is this needed? </p>
 * <li> Spring security cannot know the user's current information only with the JWT </li>
 * <li> A user's role or status can be changed anytime </li>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

	private final MemberQueryRepository memberRepository;

	/**
	 * Find by username(in this case email) extracted from the JWT token
	 */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return memberRepository.findByEmail(email)
			.map(MemberUserDetails::new)
			// TODO: custom exception needed?
			.orElseThrow(() -> new RuntimeException("Member not found for email: " + email));
	}
}
