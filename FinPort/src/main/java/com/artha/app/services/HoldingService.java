package com.artha.app.services;

import com.artha.app.models.Holding;
import com.artha.app.models.User;
import com.artha.app.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HoldingService {

    private final HoldingRepository holdingRepository;

    public HoldingService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    public List<Holding> findByUser(User user) {
        return holdingRepository.findByUserId(user.getId());
    }

    public List<Holding> findByUserId(Integer userId) {
        return holdingRepository.findByUserId(userId);
    }

    @Transactional
    public Holding save(Holding holding) {
        return holdingRepository.save(holding);
    }

    @Transactional
    public void deleteById(Long id) {
        holdingRepository.deleteById(id);
    }
}