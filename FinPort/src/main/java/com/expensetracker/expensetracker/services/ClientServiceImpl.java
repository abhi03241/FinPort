package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.models.Client;
import com.expensetracker.expensetracker.repository.ClientRepository;
import com.expensetracker.expensetracker.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientServiceImpl implements ClientService {

    ClientRepository clientRepository;
    @Autowired
    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public void saveClient(Client client) {
        clientRepository.save(client);
    }

    @Override
    public Client findClientById(int id) {
        return clientRepository.findById(id).orElse(null);
    }
}
