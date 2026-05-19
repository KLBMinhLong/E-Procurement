package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.application.port.out.PrNumberSequencePort;
import com.eprocure.pr.infrastructure.persistence.mapper.PurchaseRequestMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PrNumberSequenceAdapter implements PrNumberSequencePort {
    private final PurchaseRequestMapper mapper;

    public PrNumberSequenceAdapter(PurchaseRequestMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public long next() {
        return mapper.nextPrNumberSequence();
    }
}
