package com.smartdelivery.dispatcher.controller;

import com.smartdelivery.dispatcher.model.DeliveryAssignment;
import com.smartdelivery.dispatcher.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatcher")
@RequiredArgsConstructor
@Slf4j
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @PostMapping("/assign")
    public ResponseEntity<DeliveryAssignment> assignDelivery(
            @RequestParam UUID deliveryId,
            @RequestParam Double pickupLatitude,
            @RequestParam Double pickupLongitude) {
        log.info("Assigning delivery {} to nearest available courier", deliveryId);
        DeliveryAssignment assignment = dispatcherService.assignDelivery(
                deliveryId, pickupLatitude, pickupLongitude);
        return ResponseEntity.ok(assignment);
    }

    @PutMapping("/assignments/{assignmentId}/status")
    public ResponseEntity<DeliveryAssignment> updateAssignmentStatus(
            @PathVariable UUID assignmentId,
            @RequestParam DeliveryAssignment.AssignmentStatus status) {
        log.info("Updating assignment status to {} for assignment ID: {}", status, assignmentId);
        DeliveryAssignment assignment = dispatcherService.updateAssignmentStatus(assignmentId, status);
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/couriers/{courierId}/assignments")
    public ResponseEntity<List<DeliveryAssignment>> getAssignmentsByCourierId(@PathVariable UUID courierId) {
        log.info("Getting assignments for courier ID: {}", courierId);
        List<DeliveryAssignment> assignments = dispatcherService.getAssignmentsByCourierId(courierId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/couriers/{courierId}/assignments/active")
    public ResponseEntity<List<DeliveryAssignment>> getActiveAssignmentsByCourierId(@PathVariable UUID courierId) {
        log.info("Getting active assignments for courier ID: {}", courierId);
        List<DeliveryAssignment> assignments = dispatcherService.getActiveAssignmentsByCourierId(courierId);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/couriers/{courierId}/assignments/cancel")
    public ResponseEntity<List<DeliveryAssignment>> cancelAllActiveAssignmentsByCourierId(@PathVariable UUID courierId) {
        log.info("Cancelling all active assignments for courier ID: {}", courierId);
        List<DeliveryAssignment> assignments = dispatcherService.cancelAllActiveAssignmentsByCourierId(courierId);
        return ResponseEntity.ok(assignments);
    }
}
