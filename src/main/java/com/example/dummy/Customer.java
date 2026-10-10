package com.example.dummy;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity 
public class Customer {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;

    //Customer constructor
    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    //No-arg constructor for JPA
    protected Customer() {}

    //Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

     public String getEmail() {
        return email;
    }
}
