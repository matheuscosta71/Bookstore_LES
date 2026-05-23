package com.matheusgn.ecommerce.book;

import com.matheusgn.ecommerce.book.service.BookCodeGeneratorService;
import com.matheusgn.ecommerce.common.code.CodeSequence;
import com.matheusgn.ecommerce.common.code.CodeSequenceRepository;
import com.matheusgn.ecommerce.common.code.CodeSequenceScope;
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
class BookCodeGeneratorServiceTest {

    @Mock
    private CodeSequenceRepository codeSequenceRepository;

    @InjectMocks
    private BookCodeGeneratorService bookCodeGeneratorService;

    @Test
    void nextCode_incrementsAndFormatsBookPrefix() {
        CodeSequence seq = CodeSequence.builder()
                .scope(CodeSequenceScope.BOOK)
                .lastValue(0L)
                .build();
        when(codeSequenceRepository.findByScopeForUpdate(CodeSequenceScope.BOOK)).thenReturn(Optional.of(seq));
        when(codeSequenceRepository.save(any(CodeSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String c1 = bookCodeGeneratorService.nextCode();
        String c2 = bookCodeGeneratorService.nextCode();

        assertThat(c1).isEqualTo("LIVR-000001-01");
        assertThat(c2).isEqualTo("LIVR-000002-02");
    }
}
