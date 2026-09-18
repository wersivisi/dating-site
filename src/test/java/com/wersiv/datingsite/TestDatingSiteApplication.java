package com.wersiv.datingsite;

import com.wersiv.datingsite.backend.DatingSiteApplication;
import org.springframework.boot.SpringApplication;

public class TestDatingSiteApplication {

    public static void main(String[] args) {
        SpringApplication.from(DatingSiteApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
