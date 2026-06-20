package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.models.Role;

public interface RoleService{
    Role findRoleByName(String name);
}
