package com.beno.summaryspherebackend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentListDTO {
    private String id;
    private String fileName;
    private String fileType;
    private String status;
}
