package com.matheusgn.ecommerce.customer;

import com.matheusgn.ecommerce.common.code.CodeSequence;
import com.matheusgn.ecommerce.common.code.CodeSequenceRepository;
import com.matheusgn.ecommerce.common.code.CodeSequenceScope;
import com.matheusgn.ecommerce.customer.service.CustomerCodeGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCodeGeneratorServiceTest {

    @Mock
    private CodeSequenceRepository codeSequenceRepository;

    @InjectMocks
    private CustomerCodeGeneratorService customerCodeGeneratorService;

    @Test
    void nextCode_usesCustPrefix() {
        CodeSequence seq = CodeSequence.builder()
                .scope(CodeSequenceScope.CUSTOMER)
                .lastValue(0L)
                .build();
        when(codeSequenceRepository.findByScopeForUpdate(CodeSequenceScope.CUSTOMER)).thenReturn(Optional.of(seq));
        when(codeSequenceRepository.save(any(CodeSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(customerCodeGeneratorService.nextCode()).isEqualTo("CUST-000001");
    }
}
