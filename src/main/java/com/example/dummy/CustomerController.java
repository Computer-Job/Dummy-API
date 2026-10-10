package com.example.dummy;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    private final CustomerRepository repository;

    public CustomerController(CustomerRepository repository) {
        this.repository = repository;
    }

    public record CreateCustomerRequest(String name, String email) {}

    @PostMapping 
    public Customer create(@RequestBody CreateCustomerRequest request) {
        Customer customer = new Customer(request.name(), request.email());

        return repository.save(customer);
    }
}
