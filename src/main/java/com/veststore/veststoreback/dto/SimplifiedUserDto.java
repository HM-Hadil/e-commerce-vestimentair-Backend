package com.veststore.veststoreback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimplifiedUserDto {
    private Long id;
    private String name;
    private String password;
    private String address;
    private String phone;
}