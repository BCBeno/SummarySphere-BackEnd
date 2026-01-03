package com.beno.summaryspherebackend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentDTO {
    private String id;
    private String title;      // <--- Added by Rbt-Ghost
    private String fileName;
    private String fileType;
    private String status;
    private String uploadedAt;
}