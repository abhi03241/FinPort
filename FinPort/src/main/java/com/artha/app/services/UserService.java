package com.artha.app.services;

import com.artha.app.DTO.WebUser;
import com.artha.app.models.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User findUserByUserName(String username);

    void save(WebUser webUser);
}
