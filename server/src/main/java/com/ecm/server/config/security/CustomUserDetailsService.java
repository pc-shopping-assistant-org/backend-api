package com.ecm.server.config.security;

import com.ecm.server.common.StatusCode;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Account;
import com.ecm.server.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(StatusCode.INVALID_CREDENTIALS));

        return UserPrincipal.create(account);
    }
}
