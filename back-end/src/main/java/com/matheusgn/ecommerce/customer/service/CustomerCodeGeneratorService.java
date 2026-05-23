package com.matheusgn.ecommerce.customer.service;

import com.matheusgn.ecommerce.common.code.CodeSequence;
import com.matheusgn.ecommerce.common.code.CodeSequenceRepository;
import com.matheusgn.ecommerce.common.code.CodeSequenceScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerCodeGeneratorService {

    private final CodeSequenceRepository codeSequenceRepository;

    @Transactional
    public String nextCode() {
        CodeSequence seq = codeSequenceRepository.findByScopeForUpdate(CodeSequenceScope.CUSTOMER)
                .orElseGet(() -> codeSequenceRepository.save(CodeSequence.builder()
                        .scope(CodeSequenceScope.CUSTOMER)
                        .lastValue(0L)
                        .build()));
        seq.setLastValue(seq.getLastValue() + 1);
        codeSequenceRepository.save(seq);
        return String.format("CUST-%06d", seq.getLastValue());
    }
}
