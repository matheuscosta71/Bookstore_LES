package com.matheusgn.ecommerce.book.service;

import com.matheusgn.ecommerce.common.code.CodeSequence;
import com.matheusgn.ecommerce.common.code.CodeSequenceRepository;
import com.matheusgn.ecommerce.common.code.CodeSequenceScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookCodeGeneratorService {

    private final CodeSequenceRepository codeSequenceRepository;

    @Transactional
    public String nextCode() {
        CodeSequence seq = codeSequenceRepository.findByScopeForUpdate(CodeSequenceScope.BOOK)
                .orElseGet(() -> codeSequenceRepository.save(CodeSequence.builder()
                        .scope(CodeSequenceScope.BOOK)
                        .lastValue(0L)
                        .build()));
        seq.setLastValue(seq.getLastValue() + 1);
        codeSequenceRepository.save(seq);
        long n = seq.getLastValue();
        int checksum = (int) (n % 97);
        return String.format("LIVR-%06d-%02d", n, checksum);
    }
}
