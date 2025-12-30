package com.beno.summaryspherebackend.ModelMappers;

import com.beno.summaryspherebackend.dtos.DocumentDTO;
import com.beno.summaryspherebackend.dtos.DocumentListDTO;
import com.beno.summaryspherebackend.entities.Document;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class ConvertToDto {

    private final ModelMapper modelMapper;

    public ConvertToDto(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public DocumentListDTO convertDocumentListToDto(Document doc) {
        DocumentListDTO dto = new DocumentListDTO();
        dto.setId(doc.getDocumentId());
        dto.setTitle(doc.getTitle());
        dto.setFileName(doc.getOriginalFilename());
        dto.setFileType(doc.getFileType());
        dto.setStatus(doc.getStatus());
        dto.setUploadedAt(doc.getUploadedAt() != null ? doc.getUploadedAt().toString() : null); 
        return dto;
    }

    public DocumentDTO convertDocumentToDto(Document doc) {
        return modelMapper.map(doc, DocumentDTO.class);
    }
}