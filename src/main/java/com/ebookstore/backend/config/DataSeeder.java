package com.ebookstore.backend.config;

import com.ebookstore.backend.entity.Category;
import com.ebookstore.backend.entity.Product;
import com.ebookstore.backend.repository.CategoryRepository;
import com.ebookstore.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        // Only seed if the DB is empty - keeps this safe to run on every startup
        if (categoryRepository.count() > 0) {
            return;
        }

        Category fiction = new Category();
        fiction.setName("Fiction");
        fiction = categoryRepository.save(fiction);

        Category nonFiction = new Category();
        nonFiction.setName("Non-Fiction");
        nonFiction = categoryRepository.save(nonFiction);

        Category scienceFiction = new Category();
        scienceFiction.setName("Science Fiction");
        scienceFiction = categoryRepository.save(scienceFiction);

        createProduct("The Hobbit", "A fantasy adventure novel by J.R.R. Tolkien.", 15.99, fiction, 20);
        createProduct("Pride and Prejudice", "A classic romance novel by Jane Austen.", 9.99, fiction, 15);
        createProduct("Sapiens", "A brief history of humankind.", 18.50, nonFiction, 25);
        createProduct("Atomic Habits", "An easy way to build good habits.", 14.99, nonFiction, 30);
        createProduct("Dune", "Epic science fiction novel by Frank Herbert.", 16.75, scienceFiction, 10);
        createProduct("The Martian", "A stranded astronaut fights to survive on Mars.", 12.99, scienceFiction, 18);

        System.out.println("Sample data seeded: 3 categories, 6 products.");
    }

    private void createProduct(String name, String description, double price, Category category, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setStock(stock);
        productRepository.save(product);
    }
}
