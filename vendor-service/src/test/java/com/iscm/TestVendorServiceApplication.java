package com.iscm;

import org.springframework.boot.SpringApplication;

public class TestVendorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(VendorServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
