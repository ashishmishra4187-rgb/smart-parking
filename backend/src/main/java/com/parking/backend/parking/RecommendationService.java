package com.parking.backend.parking;

import com.parking.backend.common.NotFoundException;
import com.parking.backend.vehicle.Vehicle;
import com.parking.backend.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final SlotQueryRepository slots;
    private final VehicleRepository vehicles;
    private final double wCloseness;
    private final double wCongestion;
    private final double wPredicted;
    private final double wTypeMatch;
    private final double wRoute;

    public RecommendationService(
            SlotQueryRepository slots,
            VehicleRepository vehicles,
            @Value("${app.recommend.closeness:0.30}") double wCloseness,
            @Value("${app.recommend.congestion:0.25}") double wCongestion,
            @Value("${app.recommend.predicted:0.20}") double wPredicted,
            @Value("${app.recommend.type-match:0.15}") double wTypeMatch,
            @Value("${app.recommend.route:0.10}") double wRoute) {
        this.slots = slots;
        this.vehicles = vehicles;
        this.wCloseness = wCloseness;
        this.wCongestion = wCongestion;
        this.wPredicted = wPredicted;
        this.wTypeMatch = wTypeMatch;
        this.wRoute = wRoute;
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> recommend(Long lotId, Long vehicleId, String email, int limit) {
        // identity comes from the token; the vehicle must be the caller's own
        Vehicle vehicle = vehicles.findByIdAndUserEmail(vehicleId, email)
                .orElseThrow(() -> new NotFoundException("Vehicle not found"));
        String vehicleType = vehicle.getType().name();

        List<ParkingSlot> all = slots.findAllInLot(lotId);
        if (all.isEmpty()) {
            throw new NotFoundException("Lot not found");
        }

        ParkingLot lot = all.get(0).getSection().getLot();
        double ex = lot.getEntranceX();
        double ey = lot.getEntranceY();

        // pass 1: free/total per section, and the farthest distance in the lot
        Map<Long, long[]> sectionCounts = new HashMap<>(); // [free, total]
        double maxDist = 0;
        for (ParkingSlot s : all) {
            long[] c = sectionCounts.computeIfAbsent(s.getSection().getId(), k -> new long[2]);
            c[1]++;
            if (s.getStatus() == SlotStatus.AVAILABLE) {
                c[0]++;
            }
            maxDist = Math.max(maxDist, distance(s, ex, ey));
        }

        // pass 2: filter, then score
        List<RecommendationDto> result = new ArrayList<>();
        for (ParkingSlot s : all) {
            if (s.getStatus() != SlotStatus.AVAILABLE) {
                continue;
            }
            String slotType = s.getSlotType().name();
            if (!fits(vehicleType, slotType)) {
                continue;
            }

            List<String> reasons = new ArrayList<>();

            // closeness
            double d = distance(s, ex, ey);
            double closeScore = maxDist == 0 ? 100 : 100 * (1 - d / maxDist);
            reasons.add(String.format("Closeness %.0f/100 (weight %.2f): %.0f units from the entrance",
                    closeScore, wCloseness, d));

            // congestion
            long[] c = sectionCounts.get(s.getSection().getId());
            double congestionScore = 100.0 * c[0] / c[1];
            reasons.add(String.format("Congestion %.0f/100 (weight %.2f): section %s has %d of %d slots free",
                    congestionScore, wCongestion, s.getSection().getCode(), c[0], c[1]));

            // predicted availability (placeholder until the ML job)
            double predictedScore = 50;
            reasons.add(String.format("Predicted availability %.0f/100 (weight %.2f): no forecast yet, neutral value",
                    predictedScore, wPredicted));

            // type match
            double matchScore = slotType.equals(vehicleType) ? 100 : 50;
            reasons.add(String.format("Type match %.0f/100 (weight %.2f): %s vehicle in a %s slot",
                    matchScore, wTypeMatch, vehicleType, slotType));

            // route simplicity: straight line from the entrance is easiest
            boolean straight = s.getX() == ex || s.getY() == ey;
            double routeScore = straight ? 100 : 60;
            reasons.add(String.format("Route %.0f/100 (weight %.2f): %s",
                    routeScore, wRoute, straight ? "straight from the entrance" : "needs a turn"));

            double total = wCloseness * closeScore
                    + wCongestion * congestionScore
                    + wPredicted * predictedScore
                    + wTypeMatch * matchScore
                    + wRoute * routeScore;

            result.add(new RecommendationDto(
                    s.getId(), s.getCode(), s.getSection().getCode(), slotType,
                    (int) Math.round(total), reasons));
        }

        result.sort(Comparator.comparingInt(RecommendationDto::score).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    private double distance(ParkingSlot s, double ex, double ey) {
        return Math.abs(s.getX() - ex) + Math.abs(s.getY() - ey);
    }

    // hard rules: same as reservations. Disabled-access slots never match.
    private boolean fits(String vehicleType, String slotType) {
        return switch (vehicleType) {
            case "BIKE" -> slotType.equals("BIKE");
            case "EV" -> slotType.equals("EV") || slotType.equals("CAR") || slotType.equals("VISITOR");
            default -> slotType.equals("CAR") || slotType.equals("VISITOR");
        };
    }
}