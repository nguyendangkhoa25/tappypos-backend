package com.tappy.pos.service.modifier;

import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;
import com.tappy.pos.model.entity.modifier.ModifierGroup;
import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.modifier.ModifierGroupRepository;
import com.tappy.pos.repository.modifier.ProductModifierGroupRepository;
import com.tappy.pos.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModifierService Unit Tests")
class ModifierServiceImplTest {

    @Mock private ModifierGroupRepository modifierGroupRepository;
    @Mock private ProductModifierGroupRepository productModifierGroupRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @InjectMocks private ModifierServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        lenient().when(modifierGroupRepository.save(any(ModifierGroup.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("createGroup: builds group with options and tenant id")
    void createGroup_withOptions() {
        SaveModifierGroupRequest req = SaveModifierGroupRequest.builder()
                .name("Size").minSelect(1).maxSelect(1).required(true)
                .options(List.of(
                        SaveModifierGroupRequest.OptionRequest.builder().name("S").priceDelta(BigDecimal.ZERO).build(),
                        SaveModifierGroupRequest.OptionRequest.builder().name("L").priceDelta(new BigDecimal("5000")).build()))
                .build();

        ModifierGroupDTO dto = service.createGroup(req);

        assertThat(dto.getName()).isEqualTo("Size");
        assertThat(dto.getRequired()).isTrue();
        assertThat(dto.getOptions()).hasSize(2);
        assertThat(dto.getOptions().get(1).getPriceDelta()).isEqualByComparingTo("5000");

        ArgumentCaptor<ModifierGroup> captor = ArgumentCaptor.forClass(ModifierGroup.class);
        verify(modifierGroupRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("shop1");
        assertThat(captor.getValue().getOptions()).allMatch(o -> "shop1".equals(o.getTenantId()));
    }

    @Test
    @DisplayName("setProductGroups: clears existing links then creates ordered links for valid groups")
    void setProductGroups_replacesLinks() {
        when(modifierGroupRepository.findByIdAndDeletedFalse(10L)).thenReturn(Optional.of(new ModifierGroup()));
        when(modifierGroupRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(new ModifierGroup()));
        when(modifierGroupRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty()); // skipped

        service.setProductGroups(5L, List.of(10L, 99L, 11L));

        verify(productModifierGroupRepository).deleteByProductId(5L);
        ArgumentCaptor<ProductModifierGroup> captor = ArgumentCaptor.forClass(ProductModifierGroup.class);
        verify(productModifierGroupRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        // Only the two valid groups were linked, in order, with incremental sortOrder.
        assertThat(captor.getAllValues()).extracting(ProductModifierGroup::getModifierGroupId)
                .containsExactly(10L, 11L);
        assertThat(captor.getAllValues()).extracting(ProductModifierGroup::getSortOrder)
                .containsExactly(0, 1);
    }
}
