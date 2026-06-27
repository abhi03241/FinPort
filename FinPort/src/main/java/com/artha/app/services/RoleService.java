package com.artha.app.services;

import com.artha.app.models.Role;

public interface RoleService{
    Role findRoleByName(String name);
}
