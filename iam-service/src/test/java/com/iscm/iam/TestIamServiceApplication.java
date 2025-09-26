package com.iscm.iam;

import org.springframework.boot.SpringApplication;

public class TestIamServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(IamServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
