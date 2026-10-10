package com.example.dummy;

import org.springframework.boot.SpringApplication;

import com.example.dummy.DummyApplication;

public class TestDummyApplication {

	public static void main(String[] args) {
		SpringApplication.from(DummyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
