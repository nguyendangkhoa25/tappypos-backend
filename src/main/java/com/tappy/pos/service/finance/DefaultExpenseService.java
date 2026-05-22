package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.finance.DefaultExpenseDTO;
import com.tappy.pos.model.dto.finance.DefaultExpenseRequest;
import com.tappy.pos.model.dto.finance.ShopExpenseDTO;

import java.util.List;

public interface DefaultExpenseService {

    List<DefaultExpenseDTO> findAll();

    DefaultExpenseDTO create(DefaultExpenseRequest request);

    DefaultExpenseDTO update(Long id, DefaultExpenseRequest request);

    void delete(Long id);

    /**
     * Clones default expenses into the given month.
     * If {@code ids} is non-null and non-empty, only those defaults are cloned; otherwise all active defaults are cloned.
     * Skips any expense whose description already exists in that month.
     *
     * @param month "YYYY-MM"
     * @param ids   optional list of default-expense IDs to clone (null = clone all)
     * @return list of newly created shop expenses
     */
    List<ShopExpenseDTO> cloneToMonth(String month, List<Long> ids);
}
