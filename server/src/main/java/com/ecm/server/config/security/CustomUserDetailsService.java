package com.ecm.server.config.security;

import com.ecm.server.common.StatusCode;
import com.ecm.server.exception.BusinessException;
import com.ecm.server.model.Account;
import com.ecm.server.model.Employee;
import com.ecm.server.repository.AccountRepository;
import com.ecm.server.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(StatusCode.INVALID_CREDENTIALS));

        UUID employeeId = employeeRepository.findByAccountId(account.getId())
                .map(Employee::getId)
                .orElse(null);

        return UserPrincipal.create(account, employeeId);
    }
}
