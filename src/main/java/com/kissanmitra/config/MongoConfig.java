package com.kissanmitra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MongoDB configuration.
 *
 * <p>Enables MongoDB auditing for automatic createdAt and updatedAt timestamp management.
 * All entities extending BaseEntity will automatically have these fields populated.
 * 
 * <p>Also ensures all indexes defined with @Indexed annotations are created automatically
 * when the application starts.
 */
@Slf4j
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    /**
     * Automatically creates all indexes defined in entity classes.
     * This ensures indexes are created even if collections don't exist yet.
     * Only processes entities annotated with @Document (skips DTOs).
     */
    @PostConstruct
    public void initIndexes() {
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        
        // Only process entities that are MongoDB documents (have @Document annotation)
        mongoMappingContext.getPersistentEntities().forEach(entity -> {
            // Check if this entity is a MongoDB document (collection root)
            if (entity.getType().isAnnotationPresent(Document.class)) {
                try {
                    IndexOperations indexOps = mongoTemplate.indexOps(entity.getType());
                    resolver.resolveIndexFor(entity.getTypeInformation()).forEach(indexOps::ensureIndex);
                    log.debug("Created indexes for entity: {}", entity.getType().getSimpleName());
                } catch (Exception e) {
                    // Log and continue if index creation fails for a specific entity
                    // This allows the application to start even if some indexes fail
                    log.warn("Failed to create indexes for {}: {}", entity.getType().getSimpleName(), e.getMessage());
                }
            }
        });
    }
}

