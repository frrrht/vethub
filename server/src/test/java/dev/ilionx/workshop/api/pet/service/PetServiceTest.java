package dev.ilionx.workshop.api.pet.service;

import dev.ilionx.workshop.api.owner.model.Owner;
import dev.ilionx.workshop.api.owner.repository.OwnerRepository;
import dev.ilionx.workshop.api.pet.model.Pet;
import dev.ilionx.workshop.api.pet.model.PetType;
import dev.ilionx.workshop.api.pet.model.request.CreatePetRequest;
import dev.ilionx.workshop.api.pet.model.request.UpdatePetRequest;
import dev.ilionx.workshop.api.pet.repository.PetRepository;
import dev.ilionx.workshop.api.pet.repository.PetTypeRepository;
import dev.ilionx.workshop.support.UnitTest;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.DataNotFoundException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("Unit Test - Pet Service")
class PetServiceTest extends UnitTest {

    private static final Integer VALID_OWNER_ID = 1;
    private static final Integer VALID_PET_ID = 1;
    private static final String VALID_PET_NAME = "Leo";
    private static final LocalDate VALID_BIRTH_DATE = LocalDate.of(2020, 9, 7);
    private static final Integer VALID_PET_TYPE_ID = 1;
    private static final String VALID_PET_TYPE_NAME = "Cat";
    private static final Integer NON_EXISTENT_PET_ID = 999;
    private static final Integer NON_EXISTENT_OWNER_ID = 999;
    private static final Integer NON_EXISTENT_PET_TYPE_ID = 999;

    private PetRepository petRepository;
    private PetTypeRepository petTypeRepository;
    private OwnerRepository ownerRepository;
    private PetService petService;

    @BeforeEach
    void setUp() {
        petRepository = mock(PetRepository.class);
        petTypeRepository = mock(PetTypeRepository.class);
        ownerRepository = mock(OwnerRepository.class);
        petService = new PetService(petRepository, petTypeRepository, ownerRepository);
    }

    @Test
    @DisplayName("Should return pets when owner has pets")
    void shouldReturnPetsWhenOwnerHasPets() {
        // Given: An owner exists with 2 pets in the repository
        final Owner owner = aValidOwner();
        final Pet firstPet = aValidPet();
        final Pet secondPet = aValidPet();
        secondPet.setId(2);
        secondPet.setName("Bella");
        final List<Pet> expectedPets = List.of(firstPet, secondPet);
        given(ownerRepository.findById(VALID_OWNER_ID)).willReturn(Optional.of(owner));
        given(petRepository.findByOwnerId(VALID_OWNER_ID)).willReturn(expectedPets);

        // When: Finding pets by owner ID
        final List<Pet> actualPets = petService.findByOwnerId(VALID_OWNER_ID);

        // Then: Both pets should be returned
        assertThat(actualPets, is(notNullValue()));
        assertThat(actualPets, hasSize(2));
        assertThat(actualPets.get(0).getId(), is(equalTo(VALID_PET_ID)));
        assertThat(actualPets.get(0).getName(), is(equalTo(VALID_PET_NAME)));
        assertThat(actualPets.get(1).getId(), is(equalTo(2)));
        assertThat(actualPets.get(1).getName(), is(equalTo("Bella")));
    }

    @Test
    @DisplayName("Should return empty list when owner has no pets")
    void shouldReturnEmptyListWhenOwnerHasNoPets() {
        // Given: An owner exists with no pets
        final Owner owner = aValidOwner();
        given(ownerRepository.findById(VALID_OWNER_ID)).willReturn(Optional.of(owner));
        given(petRepository.findByOwnerId(VALID_OWNER_ID)).willReturn(Collections.emptyList());

        // When: Finding pets by owner ID
        final List<Pet> actualPets = petService.findByOwnerId(VALID_OWNER_ID);

        // Then: An empty list should be returned
        assertThat(actualPets, is(notNullValue()));
        assertThat(actualPets, is(empty()));
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when owner does not exist for findByOwnerId")
    void shouldThrowDataNotFoundExceptionWhenOwnerDoesNotExistForFindByOwnerId() {
        // Given: No owner exists with the given ID
        given(ownerRepository.findById(NON_EXISTENT_OWNER_ID)).willReturn(Optional.empty());

        // When & Then: Finding pets for non-existent owner should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.findByOwnerId(NON_EXISTENT_OWNER_ID)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested owner does not exist.")));
    }

    @Test
    @DisplayName("Should return pet when valid pet ID exists")
    void shouldReturnPetWhenValidPetIdExists() {
        // Given: A pet with valid ID exists in the repository
        final Pet expectedPet = aValidPet();
        given(petRepository.findById(VALID_PET_ID)).willReturn(Optional.of(expectedPet));

        // When: Finding pet by ID
        final Pet actualPet = petService.findById(VALID_PET_ID);

        // Then: The pet should be found with all fields
        assertThat(actualPet, is(notNullValue()));
        assertThat(actualPet.getId(), is(equalTo(VALID_PET_ID)));
        assertThat(actualPet.getName(), is(equalTo(VALID_PET_NAME)));
        assertThat(actualPet.getBirthDate(), is(equalTo(VALID_BIRTH_DATE)));
        assertThat(actualPet.getType().getId(), is(equalTo(VALID_PET_TYPE_ID)));
        assertThat(actualPet.getType().getName(), is(equalTo(VALID_PET_TYPE_NAME)));
        assertThat(actualPet.getOwner().getId(), is(equalTo(VALID_OWNER_ID)));
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when pet ID does not exist")
    void shouldThrowDataNotFoundExceptionWhenPetIdDoesNotExist() {
        // Given: No pet exists with the given ID
        given(petRepository.findById(NON_EXISTENT_PET_ID)).willReturn(Optional.empty());

        // When & Then: Finding by non-existent ID should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.findById(NON_EXISTENT_PET_ID)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested pet does not exist.")));
    }

    @Test
    @DisplayName("Should create pet when valid data provided")
    void shouldCreatePetWhenValidDataProvided() {
        // Given: A valid create pet request with existing owner and pet type
        final CreatePetRequest request = new CreatePetRequest();
        request.setName(VALID_PET_NAME);
        request.setBirthDate(VALID_BIRTH_DATE);
        request.setTypeId(VALID_PET_TYPE_ID);
        final Owner owner = aValidOwner();
        final PetType petType = aValidPetType();
        final Pet savedPet = aValidPet();
        given(ownerRepository.findById(VALID_OWNER_ID)).willReturn(Optional.of(owner));
        given(petTypeRepository.findById(VALID_PET_TYPE_ID)).willReturn(Optional.of(petType));
        given(petRepository.save(any(Pet.class))).willReturn(savedPet);

        // When: Creating the pet
        final Pet actualPet = petService.create(VALID_OWNER_ID, request);

        // Then: The pet should be saved and returned with correct owner, type, name, and birthDate
        verify(petRepository).save(any(Pet.class));
        assertThat(actualPet, is(notNullValue()));
        assertThat(actualPet.getId(), is(equalTo(VALID_PET_ID)));
        assertThat(actualPet.getName(), is(equalTo(VALID_PET_NAME)));
        assertThat(actualPet.getBirthDate(), is(equalTo(VALID_BIRTH_DATE)));
        assertThat(actualPet.getType().getId(), is(equalTo(VALID_PET_TYPE_ID)));
        assertThat(actualPet.getType().getName(), is(equalTo(VALID_PET_TYPE_NAME)));
        assertThat(actualPet.getOwner().getId(), is(equalTo(VALID_OWNER_ID)));
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when owner does not exist for create")
    void shouldThrowDataNotFoundExceptionWhenOwnerDoesNotExistForCreate() {
        // Given: No owner exists with the given ID
        final CreatePetRequest request = new CreatePetRequest();
        request.setName(VALID_PET_NAME);
        request.setBirthDate(VALID_BIRTH_DATE);
        request.setTypeId(VALID_PET_TYPE_ID);
        given(ownerRepository.findById(NON_EXISTENT_OWNER_ID)).willReturn(Optional.empty());

        // When & Then: Creating pet for non-existent owner should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.create(NON_EXISTENT_OWNER_ID, request)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested owner does not exist.")));
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when pet type does not exist for create")
    void shouldThrowDataNotFoundExceptionWhenPetTypeDoesNotExistForCreate() {
        // Given: Owner exists but pet type does not exist
        final CreatePetRequest request = new CreatePetRequest();
        request.setName(VALID_PET_NAME);
        request.setBirthDate(VALID_BIRTH_DATE);
        request.setTypeId(NON_EXISTENT_PET_TYPE_ID);
        final Owner owner = aValidOwner();
        given(ownerRepository.findById(VALID_OWNER_ID)).willReturn(Optional.of(owner));
        given(petTypeRepository.findById(NON_EXISTENT_PET_TYPE_ID)).willReturn(Optional.empty());

        // When & Then: Creating pet with non-existent pet type should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.create(VALID_OWNER_ID, request)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested pet type does not exist.")));
    }

    @Test
    @DisplayName("Should update pet when valid data provided")
    void shouldUpdatePetWhenValidDataProvided() {
        // Given: An existing pet and update request with new pet type
        final Pet existingPet = aValidPet();
        final UpdatePetRequest request = new UpdatePetRequest();
        request.setName("Bella");
        request.setBirthDate(VALID_BIRTH_DATE);
        request.setTypeId(2);
        final PetType newPetType = aValidPetType();
        newPetType.setId(2);
        newPetType.setName("Dog");
        final Pet updatedPet = aValidPet();
        updatedPet.setName("Bella");
        updatedPet.setType(newPetType);
        given(petRepository.findById(VALID_PET_ID)).willReturn(Optional.of(existingPet));
        given(petTypeRepository.findById(2)).willReturn(Optional.of(newPetType));
        given(petRepository.save(any(Pet.class))).willReturn(updatedPet);

        // When: Updating the pet
        final Pet actualPet = petService.update(VALID_PET_ID, request);

        // Then: The pet should be updated and saved with new name, birthDate, and type
        verify(petRepository).save(any(Pet.class));
        assertThat(actualPet, is(notNullValue()));
        assertThat(actualPet.getId(), is(equalTo(VALID_PET_ID)));
        assertThat(actualPet.getName(), is(equalTo("Bella")));
        assertThat(actualPet.getBirthDate(), is(equalTo(VALID_BIRTH_DATE)));
        assertThat(actualPet.getType().getId(), is(equalTo(2)));
        assertThat(actualPet.getType().getName(), is(equalTo("Dog")));
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when pet does not exist for update")
    void shouldThrowDataNotFoundExceptionWhenPetDoesNotExistForUpdate() {
        // Given: No pet exists with the given ID
        final UpdatePetRequest request = new UpdatePetRequest();
        request.setName("Bella");
        request.setBirthDate(VALID_BIRTH_DATE);
        request.setTypeId(VALID_PET_TYPE_ID);
        given(petRepository.findById(NON_EXISTENT_PET_ID)).willReturn(Optional.empty());

        // When & Then: Updating non-existent pet should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.update(NON_EXISTENT_PET_ID, request)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested pet does not exist.")));
    }

    @Test
    @DisplayName("Should return a page of 20 ordered by name then id when query is blank")
    void shouldReturnPageOfTwentyOrderedByNameThenIdWhenQueryIsBlank() {
        // Given: Pets exist and the query is blank
        final Pet pet = aValidPet();
        given(petRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(pet)));

        // When: Searching with a blank query
        final Page<Pet> actualPets = petService.search("   ", 0);

        // Then: The unfiltered page is requested with a fixed size and sort
        assertThat(actualPets.getContent(), hasSize(1));
        assertThat(actualPets.getContent().get(0).getName(), is(equalTo(VALID_PET_NAME)));
        final Pageable pageable = capturedPageable(true);
        assertThat(pageable.getPageNumber(), is(equalTo(0)));
        assertThat(pageable.getPageSize(), is(equalTo(20)));
        final List<Sort.Order> orders = pageable.getSort().toList();
        assertThat(orders.get(0).getProperty(), is(equalTo("name")));
        assertThat(orders.get(0).getDirection(), is(equalTo(Sort.Direction.ASC)));
        assertThat(orders.get(1).getProperty(), is(equalTo("id")));
        assertThat(orders.get(1).getDirection(), is(equalTo(Sort.Direction.ASC)));
        verify(petRepository, never()).search(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should search by literal text when query is present")
    void shouldSearchByLiteralTextWhenQueryIsPresent() {
        // Given: A query containing LIKE metacharacters
        given(petRepository.search(eq("50\\%\\_a\\\\b"), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(aValidPet())));

        // When: Searching with that query
        final Page<Pet> actualPets = petService.search("  50%_a\\b  ", 2);

        // Then: The repository receives the trimmed, escaped term and the requested page
        assertThat(actualPets.getContent(), hasSize(1));
        final Pageable pageable = capturedPageable(false);
        assertThat(pageable.getPageNumber(), is(equalTo(2)));
        assertThat(pageable.getPageSize(), is(equalTo(20)));
        verify(petRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should return unfiltered page when query is null")
    void shouldReturnUnfilteredPageWhenQueryIsNull() {
        // Given: Pets exist and no query was supplied
        given(petRepository.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(aValidPet())));

        // When: Searching with a null query
        final Page<Pet> actualPets = petService.search(null, 0);

        // Then: The unfiltered page is returned
        assertThat(actualPets.getContent(), hasSize(1));
        verify(petRepository).findAll(any(Pageable.class));
        verify(petRepository, never()).search(any(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no pets match")
    void shouldReturnEmptyPageWhenNoPetsMatch() {
        // Given: The repository has no match for the query
        given(petRepository.search(eq("missing"), any(Pageable.class)))
            .willReturn(new PageImpl<>(Collections.emptyList()));

        // When: Searching
        final Page<Pet> actualPets = petService.search("missing", 0);

        // Then: An empty page with total 0 is returned
        assertThat(actualPets.getContent(), is(empty()));
        assertThat(actualPets.getTotalElements(), is(equalTo(0L)));
    }

    @Test
    @DisplayName("Should return empty content when page is past the end")
    void shouldReturnEmptyContentWhenPageIsPastTheEnd() {
        // Given: A later page has no rows but the total is known
        final Pageable pastTheEnd = PageRequest.of(4, 20);
        given(petRepository.findAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(Collections.emptyList(), pastTheEnd, 3));

        // When: Requesting that page
        final Page<Pet> actualPets = petService.search(null, 4);

        // Then: Content is empty and the total is preserved
        assertThat(actualPets.getContent(), is(empty()));
        assertThat(actualPets.getTotalElements(), is(equalTo(3L)));
        assertThat(actualPets.getTotalPages(), is(equalTo(1)));
    }

    @Test
    @DisplayName("Should reject search when page is negative")
    void shouldRejectSearchWhenPageIsNegative() {
        // When & Then: A negative page is a bad request and does not touch the repository
        final BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> petService.search(null, -1)
        );

        assertThat(exception.getMessage(), is(equalTo("page must be greater than or equal to 0")));
        verify(petRepository, never()).findAll(any(Pageable.class));
        verify(petRepository, never()).search(any(), any(Pageable.class));
    }

    private Pageable capturedPageable(final boolean unfiltered) {
        final ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        if (unfiltered) {
            verify(petRepository).findAll(captor.capture());
        } else {
            verify(petRepository).search(any(), captor.capture());
        }
        return captor.getValue();
    }

    @Test
    @DisplayName("Should delete pet when pet exists")
    void shouldDeletePetWhenPetExists() {
        // Given: A pet exists in the repository
        final Pet existingPet = aValidPet();
        given(petRepository.findById(VALID_PET_ID)).willReturn(Optional.of(existingPet));

        // When: Deleting the pet
        petService.delete(VALID_PET_ID);

        // Then: The pet should be deleted from the repository
        verify(petRepository).delete(existingPet);
    }

    @Test
    @DisplayName("Should throw DataNotFoundException when pet does not exist for delete")
    void shouldThrowDataNotFoundExceptionWhenPetDoesNotExistForDelete() {
        // Given: No pet exists with the given ID
        given(petRepository.findById(NON_EXISTENT_PET_ID)).willReturn(Optional.empty());

        // When & Then: Deleting non-existent pet should throw DataNotFoundException
        final DataNotFoundException exception = assertThrows(
            DataNotFoundException.class,
            () -> petService.delete(NON_EXISTENT_PET_ID)
        );

        assertThat(exception.getMessage(), is(equalTo("The requested pet does not exist.")));
    }

}
