package com.artha.app.services;

import com.artha.app.models.Client;

public interface ClientService {
    void saveClient(Client client);
    Client findClientById(int id);
}
