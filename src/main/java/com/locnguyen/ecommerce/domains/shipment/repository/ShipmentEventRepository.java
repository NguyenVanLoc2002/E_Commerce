package com.locnguyen.ecommerce.domains.shipment.repository;

import com.locnguyen.ecommerce.domains.shipment.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;
public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, UUID> {

    List<ShipmentEvent> findByShipmentIdOrderByEventTimeAsc(UUID shipmentId);
}
