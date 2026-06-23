package com.tappy.pos.service.modifier;

import com.tappy.pos.service.audit.ActivityLogService;

import com.tappy.pos.config.AuthContext;

import com.tappy.pos.model.dto.modifier.ChosenModifierDTO;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.entity.modifier.ModifierGroup;
import com.tappy.pos.model.entity.modifier.ModifierOption;
import com.tappy.pos.model.entity.modifier.ProductModifierGroup;
import com.tappy.pos.model.enums.ActivityAction;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModifierService Unit Tests")
class ModifierServiceImplTest {

    @Mock private ModifierGroupRepository modifierGroupRepository;
    @Mock private com.tappy.pos.repository.modifier.ModifierOptionRepository modifierOptionRepository;
    @Mock private ProductModifierGroupRepository productModifierGroupRepository;
    @Mock private TenantContext tenantContext;
    @Mock private MessageService messageService;

    @Mock
    private AuthContext authContext;

    @Mock
    private ActivityLogService activityLogService;

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

    // ── helpers ────────────────────────────────────────────────────────────────

    private ModifierGroup groupWithOptions(Long id, String name, ModifierOption... opts) {
        ModifierGroup g = new ModifierGroup();
        g.setId(id);
        g.setName(name);
        g.setMinSelect(0);
        g.setMaxSelect(2);
        g.setRequired(false);
        g.setSortOrder(0);
        for (ModifierOption o : opts) g.getOptions().add(o);
        return g;
    }

    private ModifierOption option(Long id, String name, BigDecimal delta, ModifierGroup group) {
        ModifierOption o = ModifierOption.builder()
                .name(name).priceDelta(delta).sortOrder(0).modifierGroup(group).build();
        o.setId(id);
        return o;
    }

    // ── listGroups ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("listGroups: maps all non-deleted groups to DTOs")
    void listGroups_mapsAll() {
        ModifierGroup g = groupWithOptions(1L, "Topping",
                option(100L, "Trân châu", new BigDecimal("5000"), null));
        when(modifierGroupRepository.findByDeletedFalseOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(g));

        List<ModifierGroupDTO> result = service.listGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Topping");
        assertThat(result.get(0).getOptions()).hasSize(1);
        assertThat(result.get(0).getOptions().get(0).getName()).isEqualTo("Trân châu");
    }

    // ── resolveOptions ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolveOptions: returns empty list for null or empty ids")
    void resolveOptions_emptyInput() {
        assertThat(service.resolveOptions(null)).isEmpty();
        assertThat(service.resolveOptions(List.of())).isEmpty();
        verify(modifierOptionRepository, never()).findAllByIdInWithGroup(any());
    }

    @Test
    @DisplayName("resolveOptions: preserves caller order, skips unresolved ids, folds null group/delta")
    void resolveOptions_orderingAndSkips() {
        ModifierGroup grp = groupWithOptions(1L, "Đường");
        ModifierOption o1 = option(10L, "100%", new BigDecimal("0"), grp);
        ModifierOption o2 = option(20L, "Ít đường", null, null); // null group + null delta → ZERO

        when(modifierOptionRepository.findAllByIdInWithGroup(any()))
                .thenReturn(List.of(o1, o2));

        // request order: 20, 99 (missing), 10 → expect [Ít đường, 100%]
        List<ChosenModifierDTO> result = service.resolveOptions(List.of(20L, 99L, 10L));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ChosenModifierDTO::getOptionName)
                .containsExactly("Ít đường", "100%");
        assertThat(result.get(0).getGroupName()).isNull();
        assertThat(result.get(0).getPriceDelta()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.get(1).getGroupName()).isEqualTo("Đường");
    }

    // ── updateGroup ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateGroup: clears old options then re-applies fields")
    void updateGroup_replacesOptions() {
        ModifierGroup existing = groupWithOptions(7L, "Old",
                option(1L, "X", BigDecimal.ZERO, null));
        when(modifierGroupRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(existing));

        SaveModifierGroupRequest req = SaveModifierGroupRequest.builder()
                .name("New").minSelect(0).maxSelect(1)
                .options(List.of(SaveModifierGroupRequest.OptionRequest.builder()
                        .name("Y").priceDelta(new BigDecimal("1000")).build()))
                .build();

        ModifierGroupDTO dto = service.updateGroup(7L, req);

        assertThat(dto.getName()).isEqualTo("New");
        assertThat(dto.getOptions()).extracting(ModifierGroupDTO.OptionDTO::getName)
                .containsExactly("Y");
        verify(activityLogService).logAsync(eq("shop1"), any(), any(),
                eq(ActivityAction.MODIFIER_GROUP_UPDATED), any(), eq("7"), any(), any());
    }

    @Test
    @DisplayName("updateGroup: throws ResourceNotFoundException when group missing")
    void updateGroup_notFound() {
        when(modifierGroupRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateGroup(404L,
                SaveModifierGroupRequest.builder().name("z").build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteGroup ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteGroup: soft-deletes group and logs activity")
    void deleteGroup_softDeletes() {
        ModifierGroup g = groupWithOptions(9L, " To delete");
        when(modifierGroupRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(g));

        service.deleteGroup(9L);

        assertThat(g.isDeleted()).isTrue();
        verify(modifierGroupRepository).save(g);
        verify(activityLogService).logAsync(eq("shop1"), any(), any(),
                eq(ActivityAction.MODIFIER_GROUP_DELETED), any(), eq("9"), any(), any());
    }

    // ── getGroupsForProduct ────────────────────────────────────────────────────

    @Test
    @DisplayName("getGroupsForProduct: returns DTOs in link order, skips deleted groups")
    void getGroupsForProduct_ordersAndSkips() {
        ProductModifierGroup link1 = ProductModifierGroup.builder()
                .productId(5L).modifierGroupId(1L).sortOrder(0).build();
        ProductModifierGroup link2 = ProductModifierGroup.builder()
                .productId(5L).modifierGroupId(2L).sortOrder(1).build();
        when(productModifierGroupRepository.findByProductIdOrderBySortOrderAscIdAsc(5L))
                .thenReturn(List.of(link1, link2));
        when(modifierGroupRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(groupWithOptions(1L, "G1")));
        when(modifierGroupRepository.findByIdAndDeletedFalse(2L))
                .thenReturn(Optional.empty()); // deleted → skipped

        List<ModifierGroupDTO> result = service.getGroupsForProduct(5L);

        assertThat(result).extracting(ModifierGroupDTO::getName).containsExactly("G1");
    }

    // ── getGroupsForProducts (batch) ───────────────────────────────────────────

    @Test
    @DisplayName("getGroupsForProducts: returns empty map for null/empty/no-links input")
    void getGroupsForProducts_emptyCases() {
        assertThat(service.getGroupsForProducts(null)).isEmpty();
        assertThat(service.getGroupsForProducts(List.of())).isEmpty();

        when(productModifierGroupRepository
                .findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(any()))
                .thenReturn(List.of());
        assertThat(service.getGroupsForProducts(List.of(1L, 2L))).isEmpty();
    }

    @Test
    @DisplayName("getGroupsForProducts: fans groups out per product preserving order")
    void getGroupsForProducts_batch() {
        ProductModifierGroup l1 = ProductModifierGroup.builder()
                .productId(5L).modifierGroupId(1L).sortOrder(0).build();
        ProductModifierGroup l2 = ProductModifierGroup.builder()
                .productId(5L).modifierGroupId(2L).sortOrder(1).build();
        ProductModifierGroup l3 = ProductModifierGroup.builder()
                .productId(6L).modifierGroupId(1L).sortOrder(0).build();
        ProductModifierGroup l4 = ProductModifierGroup.builder()
                .productId(6L).modifierGroupId(99L).sortOrder(1).build(); // missing group → skipped

        when(productModifierGroupRepository
                .findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(any()))
                .thenReturn(List.of(l1, l2, l3, l4));
        when(modifierGroupRepository.findByIdInAndDeletedFalse(any()))
                .thenReturn(List.of(groupWithOptions(1L, "G1"), groupWithOptions(2L, "G2")));

        var result = service.getGroupsForProducts(List.of(5L, 6L));

        assertThat(result).containsKeys(5L, 6L);
        assertThat(result.get(5L)).extracting(ModifierGroupDTO::getName).containsExactly("G1", "G2");
        assertThat(result.get(6L)).extracting(ModifierGroupDTO::getName).containsExactly("G1");
    }

    // ── setProductGroups null ──────────────────────────────────────────────────

    @Test
    @DisplayName("setProductGroups: null groupIds clears links and returns early (no save, no log)")
    void setProductGroups_nullClearsOnly() {
        service.setProductGroups(5L, null);

        verify(productModifierGroupRepository).deleteByProductId(5L);
        verify(productModifierGroupRepository, never()).save(any());
        verify(modifierGroupRepository, never()).findByIdAndDeletedFalse(anyLong());
        verify(activityLogService, never()).logAsync(any(), any(), any(), any(), any(), any(), any(), any());
    }
}
