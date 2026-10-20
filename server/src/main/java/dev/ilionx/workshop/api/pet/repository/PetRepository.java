package dev.ilionx.workshop.api.pet.repository;

import dev.ilionx.workshop.api.pet.model.Pet;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link Pet} entities.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {

    /**
     * Finds all pets belonging to the specified owner.
     *
     * @param ownerId the owner's ID
     * @return list of pets for the given owner
     */
    List<Pet> findByOwnerId(Integer ownerId);

    /**
     * Finds pets whose name, owner name, or type name contains the given literal text.
     * The caller must escape {@code \}, {@code %} and {@code _} so they are not treated as LIKE wildcards.
     *
     * @param query    escaped, case-insensitive contains term
     * @param pageable page request; sort is applied by the caller
     * @return the matching page of pets
     */
    @Query(
        value = """
            SELECT pet FROM Pet pet
            WHERE LOWER(pet.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.owner.firstName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.owner.lastName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.type.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
            """,
        countQuery = """
            SELECT COUNT(pet) FROM Pet pet
            WHERE LOWER(pet.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.owner.firstName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.owner.lastName) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
                OR LOWER(pet.type.name) LIKE LOWER(CONCAT('%', :query, '%')) ESCAPE '\\'
            """
    )
    Page<Pet> search(@Param("query") String query, Pageable pageable);
}
