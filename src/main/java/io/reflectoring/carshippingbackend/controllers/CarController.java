package io.reflectoring.carshippingbackend.controllers;

import io.reflectoring.carshippingbackend.repository.CarRepository;
import io.reflectoring.carshippingbackend.services.CarService;
import io.reflectoring.carshippingbackend.tables.Car;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin("http://localhost:3000/")
@RequestMapping("/api/cars")
public class CarController {
    @Autowired
    private CarRepository carRepo;
    private final CarService service;

    public CarController(CarService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> search(@RequestParam Map<String,String> allParams,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "12") int size,
                                    @RequestParam(defaultValue = "priceKes,desc") String sort) {
        String[] sortParts = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(sortParts.length>1?sortParts[1]:"desc"), sortParts[0]);
        var result = service.search(allParams, page, size, s);
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @RequestPart("car") Car car,
            @RequestPart(value = "images", required = false) MultipartFile[] images
    ) throws IOException {
        Car created = service.create(car, images);
        return ResponseEntity.ok(created);
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getcars(@PathVariable Long id){
       Optional<Car>  car=carRepo.findById(id);

        return   ResponseEntity.ok(car);
    }
    @GetMapping("/similar")
    public ResponseEntity<List<Car>> getSimilarVehicles(
            @RequestParam String brand,
            @RequestParam String model,
            @RequestParam(required = false) Long exclude) {

        // Create query to find similar vehicles
        List<Car> similarCars = carRepo.findByMakeAndModelAndIdNot(brand, model, exclude);

        return ResponseEntity.ok(similarCars);
    }
    @GetMapping("/latest")
    public ResponseEntity<List<Car>> getLatestArrivals() {
        List<Car> cars = carRepo.findAll(
                PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();
        return ResponseEntity.ok(cars);
    }
}

