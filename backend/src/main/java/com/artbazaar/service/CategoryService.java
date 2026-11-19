package com.artbazaar.service;

import com.artbazaar.dto.CategoryResponse;
import com.artbazaar.exception.ResourceNotFoundException;
import com.artbazaar.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByIdAsc().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse findBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Category '" + slug + "' not found"));
    }
}
