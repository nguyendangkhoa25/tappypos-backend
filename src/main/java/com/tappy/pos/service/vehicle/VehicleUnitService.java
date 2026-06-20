package com.tappy.pos.service.vehicle;

import com.tappy.pos.model.dto.vehicle.CreateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.SellVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.UpdateVehicleUnitRequest;
import com.tappy.pos.model.dto.vehicle.VehicleUnitDTO;
import com.tappy.pos.model.enums.VehicleUnitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VehicleUnitService {
    VehicleUnitDTO create(CreateVehicleUnitRequest request);
    VehicleUnitDTO update(Long id, UpdateVehicleUnitRequest request);
    VehicleUnitDTO getById(Long id);
    Page<VehicleUnitDTO> search(VehicleUnitStatus status, Long productId, Pageable pageable);
    /** "Tra cứu xe theo số khung / số máy / biển số" + bảo hành lookup. */
    List<VehicleUnitDTO> lookup(String keyword);
    VehicleUnitDTO markSold(Long id, SellVehicleUnitRequest request);
    void delete(Long id);
}
