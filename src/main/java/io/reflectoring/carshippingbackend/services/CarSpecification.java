package io.reflectoring.carshippingbackend.services;

import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarSpecification {
    public static Specification<Car> byFilters(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();

            // equality filters
            if (params.containsKey("brand") && !params.get("brand").isBlank()) {
                preds.add(cb.equal(cb.lower(root.get("brand")), params.get("brand").toLowerCase()));
            }
            if (params.containsKey("model") && !params.get("model").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("model")), "%" + params.get("model").toLowerCase() + "%"));
            }
            if (params.containsKey("fuelType") && !params.get("fuelType").isBlank()) {
                preds.add(cb.equal(root.get("fuelType"), params.get("fuelType")));
            }
            if (params.containsKey("bodyType") && !params.get("bodyType").isBlank()) {
                preds.add(cb.equal(root.get("bodyType"), params.get("bodyType")));
            }
            if (params.containsKey("conditionType") && !params.get("conditionType").isBlank()) {
                preds.add(cb.equal(root.get("conditionType"), params.get("conditionType")));
            }
            if (params.containsKey("location") && !params.get("location").isBlank()) {
                preds.add(cb.like(cb.lower(root.get("location")), "%" + params.get("location").toLowerCase() + "%"));
            }


            if (params.containsKey("price_lte")) {
                preds.add(cb.le(root.get("priceKes"), Long.valueOf(params.get("price_lte"))));
            }
            if (params.containsKey("price_gte")) {
                preds.add(cb.ge(root.get("priceKes"), Long.valueOf(params.get("price_gte"))));
            }
            if (params.containsKey("year_lte")) {
                preds.add(cb.le(root.get("yearOfManufacture"), Integer.valueOf(params.get("year_lte"))));
            }
            if (params.containsKey("year_gte")) {
                preds.add(cb.ge(root.get("yearOfManufacture"), Integer.valueOf(params.get("year_gte"))));
            }
            if (params.containsKey("engine_cc_lte")) {
                preds.add(cb.le(root.get("engineCapacityCc"), Integer.valueOf(params.get("engine_cc_lte"))));
            }
            if (params.containsKey("engine_cc_gte")) {
                preds.add(cb.ge(root.get("engineCapacityCc"), Integer.valueOf(params.get("engine_cc_gte"))));
            }
            if (params.containsKey("mileage_lte")) {
                preds.add(cb.le(root.get("mileageKm"), Integer.valueOf(params.get("mileage_lte"))));
            }
            if (params.containsKey("mileage_gte")) {
                preds.add(cb.ge(root.get("mileageKm"), Integer.valueOf(params.get("mileage_gte"))));
            }

            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}

