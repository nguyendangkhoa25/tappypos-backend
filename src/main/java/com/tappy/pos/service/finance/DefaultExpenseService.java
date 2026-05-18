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
     * Clones all default expenses into the given month.
     * Skips any expense whose description already exists in that month.
     *
     * @param month "YYYY-MM"
     * @return list of newly created shop expenses
     */
    List<ShopExpenseDTO> cloneToMonth(String month);
}
