package com.tappy.pos.service.modifier;

import com.tappy.pos.model.dto.modifier.ChosenModifierDTO;
import com.tappy.pos.model.dto.modifier.ModifierGroupDTO;
import com.tappy.pos.model.dto.modifier.SaveModifierGroupRequest;

import java.util.List;

public interface ModifierService {

    List<ModifierGroupDTO> listGroups();

    /** Resolve selected option ids into chosen-modifier rows (group/option label + price delta), in id order. */
    List<ChosenModifierDTO> resolveOptions(List<Long> optionIds);

    ModifierGroupDTO createGroup(SaveModifierGroupRequest req);

    ModifierGroupDTO updateGroup(Long id, SaveModifierGroupRequest req);

    void deleteGroup(Long id);

    /** Modifier groups attached to a product, in display order. */
    List<ModifierGroupDTO> getGroupsForProduct(Long productId);

    /** Replace the modifier groups attached to a product. */
    void setProductGroups(Long productId, List<Long> groupIds);
}
