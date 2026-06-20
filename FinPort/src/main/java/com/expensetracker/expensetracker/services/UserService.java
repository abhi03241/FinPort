package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.DTO.WebUser;
import com.expensetracker.expensetracker.models.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User findUserByUserName(String username);

    void save(WebUser webUser);
}
