package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.models.Client;

public interface ClientService {
    void saveClient(Client client);
    Client findClientById(int id);
}
