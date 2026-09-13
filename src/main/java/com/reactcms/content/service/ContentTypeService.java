package com.reactcms.content.service;

import com.reactcms.content.dto.ContentTypeDto;
import com.reactcms.content.entity.ContentTypeEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContentTypeService {

    public List<ContentTypeDto> list() {
        return ContentTypeEntity.<ContentTypeEntity>listAll().stream()
                .map(ct -> new ContentTypeDto(ct.id, ct.name, ct.slug, ct.description))
                .collect(Collectors.toList());
    }
}
