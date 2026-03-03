package com.team8.damo.cache.store;

import com.team8.damo.cache.CacheSpec;
import com.team8.damo.repository.LightningParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LightningCacheService {

    private final LightningParticipantRepository lightningParticipantRepository;

    @Cacheable(
        cacheNames = CacheSpec.lightningParticipantCount,
        key = "#lightningId"
    )
    public long getLightningParticipantCount(Long lightningId) {
        return lightningParticipantRepository.countByLightningId(lightningId);
    }
}
