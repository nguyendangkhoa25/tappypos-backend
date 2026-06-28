package com.tappy.pos.repository.tax;

import com.tappy.pos.model.entity.finance.TaxDeclarationLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxDeclarationLineRepository extends JpaRepository<TaxDeclarationLine, Long> {
}
