package com.tappy.pos.repository.tenant;

import com.tappy.pos.model.entity.tenant.ShopConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopConfigRepository extends JpaRepository<ShopConfig, Long> {
    Optional<ShopConfig> findByConfigKey(String configKey);
    List<ShopConfig> findByConfigGroup(String configGroup);
}
