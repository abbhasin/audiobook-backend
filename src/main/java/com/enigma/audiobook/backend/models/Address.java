package com.enigma.audiobook.backend.models;

import lombok.Data;

@Data
public class Address {
    String street;
    String locality;
    String city;
    String state;
    String pincode;
    String country;
}
