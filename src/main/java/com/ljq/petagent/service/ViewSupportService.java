package com.ljq.petagent.service;

import com.ljq.petagent.entity.AppUser;
import com.ljq.petagent.entity.PetProfile;
import com.ljq.petagent.repository.PetProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ViewSupportService {

    private final PetProfileRepository petProfileRepository;

    public ViewSupportService(PetProfileRepository petProfileRepository) {
        this.petProfileRepository = petProfileRepository;
    }

    public long activePetCount(AppUser user) {
        return user == null ? 0 : petProfileRepository.countByOwnerIdAndActiveTrue(user.getId());
    }

    public PetProfile requireOwnedPet(Long petId, AppUser user) {
        return petProfileRepository.findByIdAndOwnerId(petId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("这不是你的宠物"));
    }
}
