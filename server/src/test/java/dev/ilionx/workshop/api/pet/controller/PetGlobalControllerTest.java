package dev.ilionx.workshop.api.pet.controller;

import dev.ilionx.workshop.api.owner.model.Owner;
import dev.ilionx.workshop.api.pet.model.Pet;
import dev.ilionx.workshop.api.pet.model.PetType;
import dev.ilionx.workshop.api.pet.model.request.CreatePetRequest;
import dev.ilionx.workshop.api.pet.model.request.UpdatePetRequest;
import dev.ilionx.workshop.api.pet.model.response.PetResponse;
import dev.ilionx.workshop.support.IntegrationTest;
import io.github.jframe.exception.resource.ErrorResponseResource;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static dev.ilionx.workshop.api.Paths.PETS;
import static dev.ilionx.workshop.api.Paths.PET_BY_ID;
import static dev.ilionx.workshop.common.exception.ApiErrorCode.OWNER_NOT_FOUND;
import static dev.ilionx.workshop.common.exception.ApiErrorCode.PET_NOT_FOUND;
import static io.github.jframe.util.mapper.ObjectMappers.fromJson;
import static io.github.jframe.util.mapper.ObjectMappers.toJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration Test - Pet Global Controller")
class PetGlobalControllerTest extends IntegrationTest {

    private static final int NON_EXISTENT_PET_ID = 999;
    private static final int NON_EXISTENT_OWNER_ID = 999;

    private static final String PET_NAME = "Leo";
    private static final LocalDate PET_BIRTH_DATE = LocalDate.of(2020, 9, 7);
    private static final String UPDATED_PET_NAME = "Max";

    // ========================= LIST =========================
    @Test
    @DisplayName("Should return paged pets when pets exist")
    void shouldReturnPagedPetsWhenPetsExist() throws Exception {
        // Given: An owner with a pet exists in the database
        final Owner owner = aSavedOwner();
        final Pet pet = aSavedPet(owner);

        // When: Requesting the pet directory
        // Then: Should return 200 with page metadata and the owner summary
        mockMvc.perform(get(PETS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.page", is(equalTo(0))))
            .andExpect(jsonPath("$.size", is(equalTo(20))))
            .andExpect(jsonPath("$.totalElements", is(equalTo(1))))
            .andExpect(jsonPath("$.totalPages", is(equalTo(1))))
            .andExpect(jsonPath("$.content[0].id", is(equalTo(pet.getId()))))
            .andExpect(jsonPath("$.content[0].name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.content[0].ownerId", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.content[0].owner.id", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.content[0].owner.firstName", is(equalTo("George"))))
            .andExpect(jsonPath("$.content[0].owner.lastName", is(equalTo("Franklin"))));
    }

    @Test
    @DisplayName("Should return empty page when no pets exist")
    void shouldReturnEmptyPageWhenNoPetsExist() throws Exception {
        // Given: No pets exist in the database

        // When: Requesting the pet directory
        // Then: Should return 200 with an empty page
        mockMvc.perform(get(PETS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(equalTo(0))))
            .andExpect(jsonPath("$.totalPages", is(equalTo(0))));
    }

    @Test
    @DisplayName("Should return matching pet when query matches name owner or type")
    void shouldReturnMatchingPetWhenQueryMatchesNameOwnerOrType() throws Exception {
        // Given: Pets that can be found by name, owner name, or type
        final Owner george = aSavedOwner();
        aSavedPet(george);
        final Owner zoe = aSavedOwner();
        zoe.setFirstName("Zoe");
        zoe.setLastName("Quinn");
        ownerRepository.save(zoe);
        aSavedPet(zoe, "Milo", 2);

        // When & Then: Partial, case-insensitive matches on each field
        mockMvc.perform(get(PETS).param("q", "eo").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name", is(equalTo(PET_NAME))));
        mockMvc.perform(get(PETS).param("q", "ZOE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name", is(equalTo("Milo"))));
        mockMvc.perform(get(PETS).param("q", "frank"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].owner.lastName", is(equalTo("Franklin"))));
        mockMvc.perform(get(PETS).param("q", "dog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type.name", is(equalTo("Dog"))))
            .andExpect(jsonPath("$.page", is(equalTo(0))));
    }

    @Test
    @DisplayName("Should treat query as literal text when query contains wildcard")
    void shouldTreatQueryAsLiteralTextWhenQueryContainsWildcard() throws Exception {
        // Given: One pet whose name contains a percent sign, and one that does not
        final Owner owner = aSavedOwner();
        aSavedPet(owner);
        aSavedPet(owner, "100%", 1);

        // When: Searching for the literal percent sign
        // Then: Only the pet whose name contains that character matches
        mockMvc.perform(get(PETS).param("q", "%"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name", is(equalTo("100%"))));
    }

    @Test
    @DisplayName("Should return all pets when query is blank")
    void shouldReturnAllPetsWhenQueryIsBlank() throws Exception {
        // Given: Two pets exist
        final Owner owner = aSavedOwner();
        aSavedPet(owner);
        aSavedPet(owner, "Milo", 2);

        // When: Submitting a blank query
        // Then: Both pets are returned
        mockMvc.perform(get(PETS).param("q", "   "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.totalElements", is(equalTo(2))));
    }

    @Test
    @DisplayName("Should return empty page when no pets match")
    void shouldReturnEmptyPageWhenNoPetsMatch() throws Exception {
        // Given: A pet exists that does not match the query
        aSavedPet(aSavedOwner());

        // When: Searching for a missing name
        // Then: The page is empty and the total is zero
        mockMvc.perform(get(PETS).param("q", "missing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(equalTo(0))))
            .andExpect(jsonPath("$.totalPages", is(equalTo(0))));
    }

    @Test
    @DisplayName("Should return empty content when page is past the end")
    void shouldReturnEmptyContentWhenPageIsPastTheEnd() throws Exception {
        // Given: One pet exists
        aSavedPet(aSavedOwner());

        // When: Requesting a page past the end
        // Then: Content is empty and the real total is kept
        mockMvc.perform(get(PETS).param("page", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements", is(equalTo(1))))
            .andExpect(jsonPath("$.totalPages", is(equalTo(1))));
    }

    @Test
    @DisplayName("Should return second page when more than twenty pets exist")
    void shouldReturnSecondPageWhenMoreThanTwentyPetsExist() throws Exception {
        // Given: Twenty-one pets for one owner
        final Owner owner = aSavedOwner();
        for (int index = 0; index < 21; index++) {
            aSavedPet(owner, String.format("Pet%02d", index), 1);
        }

        // When: Requesting the second page
        // Then: Only the last pet is returned and the client page size is ignored
        mockMvc.perform(get(PETS).param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name", is(equalTo("Pet20"))))
            .andExpect(jsonPath("$.page", is(equalTo(1))))
            .andExpect(jsonPath("$.size", is(equalTo(20))))
            .andExpect(jsonPath("$.totalElements", is(equalTo(21))))
            .andExpect(jsonPath("$.totalPages", is(equalTo(2))));
    }

    @Test
    @DisplayName("Should return bad request when page is negative")
    void shouldReturnBadRequestWhenPageIsNegative() throws Exception {
        // When: Requesting a negative page
        // Then: The existing bad-request response is returned
        mockMvc.perform(get(PETS).param("page", "-1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request when page is not an integer")
    void shouldReturnBadRequestWhenPageIsNotAnInteger() throws Exception {
        // When: Requesting a page that is not an integer
        // Then: The existing bad-request response is returned
        mockMvc.perform(get(PETS).param("page", "nope"))
            .andExpect(status().isBadRequest());
    }

    private Pet aSavedPet(final Owner owner, final String name, final Integer typeId) {
        final PetType petType = petTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalStateException("Pet type with ID " + typeId + " not found in seed data"));
        final Pet pet = new Pet();
        pet.setName(name);
        pet.setBirthDate(LocalDate.of(2020, 1, 1));
        pet.setType(petType);
        pet.setOwner(owner);
        return petRepository.save(pet);
    }

    // ========================= GET BY ID =========================
    @Test
    @DisplayName("Should return pet when valid pet ID exists")
    void shouldReturnPetWhenValidPetIdExists() throws Exception {
        // Given: An owner with a pet exists in the database
        final Owner owner = aSavedOwner();
        final Pet pet = aSavedPet(owner);

        // When: Requesting pet by ID
        // Then: Should return 200 with pet details
        mockMvc.perform(get(PET_BY_ID, pet.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(equalTo(pet.getId()))))
            .andExpect(jsonPath("$.name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.birthDate", is(notNullValue())))
            .andExpect(jsonPath("$.type.id", is(equalTo(1))))
            .andExpect(jsonPath("$.type.name", is(notNullValue())))
            .andExpect(jsonPath("$.ownerId", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.id", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.firstName", is(equalTo("George"))))
            .andExpect(jsonPath("$.owner.lastName", is(equalTo("Franklin"))))
            .andExpect(jsonPath("$.visits", is(notNullValue())));
    }

    @Test
    @DisplayName("Should return not found when pet ID does not exist")
    void shouldReturnNotFoundWhenPetIdDoesNotExist() throws Exception {
        // Given: No pet exists with ID 999

        // When: Requesting non-existent pet
        final ResultActions response = mockMvc.perform(get(PET_BY_ID, NON_EXISTENT_PET_ID));

        // Then: Should return 404 with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    // ========================= CREATE =========================
    @Test
    @DisplayName("Should create pet when valid data provided")
    void shouldCreatePetWhenValidDataProvided() throws Exception {
        // Given: An owner exists and a valid create pet request with ownerId in body
        final Owner owner = aSavedOwner();
        final CreatePetRequest request = aCreatePetRequestWithOwner(owner.getId());

        // When: Creating the pet
        final String responseBody = mockMvc.perform(
            post(PETS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            // Then: Should return 201 with pet response
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(notNullValue())))
            .andExpect(jsonPath("$.name", is(equalTo(PET_NAME))))
            .andExpect(jsonPath("$.ownerId", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.id", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.firstName", is(equalTo("George"))))
            .andExpect(jsonPath("$.owner.lastName", is(equalTo("Franklin"))))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // And: The returned pet should be valid
        final PetResponse petResponse = fromJson(responseBody, PetResponse.class);
        assertThat(petResponse.getId(), is(notNullValue()));
        assertThat(petResponse.getName(), is(equalTo(PET_NAME)));
        assertThat(petResponse.getOwnerId(), is(equalTo(owner.getId())));
        assertThat(petResponse.getOwner(), is(notNullValue()));
        assertThat(petResponse.getOwner().getId(), is(equalTo(owner.getId())));
        assertThat(petResponse.getOwner().getFirstName(), is(equalTo("George")));
        assertThat(petResponse.getOwner().getLastName(), is(equalTo("Franklin")));
    }

    @Test
    @DisplayName("Should return not found when owner does not exist for create pet")
    void shouldReturnNotFoundWhenOwnerDoesNotExistForCreatePet() throws Exception {
        // Given: A create pet request with non-existent owner ID
        final CreatePetRequest request = aCreatePetRequestWithOwner(NON_EXISTENT_OWNER_ID);

        // When: Creating pet for non-existent owner
        final ResultActions response = mockMvc.perform(
            post(PETS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        );

        // Then: Should return 404 with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(OWNER_NOT_FOUND.getReason())));
    }

    // ========================= UPDATE =========================
    @Test
    @DisplayName("Should update pet when valid data provided")
    void shouldUpdatePetWhenValidDataProvided() throws Exception {
        // Given: An existing pet in the database
        final Owner owner = aSavedOwner();
        final Pet pet = aSavedPet(owner);
        final UpdatePetRequest request = anUpdatePetRequest();

        // When: Updating the pet
        // Then: Should return 200 OK with updated pet
        mockMvc.perform(
            put(PET_BY_ID, pet.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(equalTo(pet.getId()))))
            .andExpect(jsonPath("$.name", is(equalTo(UPDATED_PET_NAME))))
            .andExpect(jsonPath("$.ownerId", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.id", is(equalTo(owner.getId()))))
            .andExpect(jsonPath("$.owner.firstName", is(equalTo("George"))))
            .andExpect(jsonPath("$.owner.lastName", is(equalTo("Franklin"))));
    }

    @Test
    @DisplayName("Should return not found when pet does not exist for update")
    void shouldReturnNotFoundWhenPetDoesNotExistForUpdate() throws Exception {
        // Given: An update request for non-existent pet
        final UpdatePetRequest request = anUpdatePetRequest();

        // When: Updating non-existent pet
        final ResultActions response = mockMvc.perform(
            put(PET_BY_ID, NON_EXISTENT_PET_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        );

        // Then: Should return 404 with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }

    // ========================= DELETE =========================
    @Test
    @DisplayName("Should delete pet when pet exists")
    void shouldDeletePetWhenPetExists() throws Exception {
        // Given: An existing pet in the database
        final Owner owner = aSavedOwner();
        final Pet pet = aSavedPet(owner);

        // When: Deleting the pet
        // Then: Should return 204 no content
        mockMvc.perform(delete(PET_BY_ID, pet.getId()))
            .andExpect(status().isNoContent());

        // And: The pet should no longer exist
        mockMvc.perform(get(PET_BY_ID, pet.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return not found when pet does not exist for delete")
    void shouldReturnNotFoundWhenPetDoesNotExistForDelete() throws Exception {
        // Given: No pet exists with ID 999

        // When: Deleting non-existent pet
        final ResultActions response = mockMvc.perform(delete(PET_BY_ID, NON_EXISTENT_PET_ID));

        // Then: Should return 404 with error message
        response.andExpect(status().isNotFound());

        final String content = response.andReturn().getResponse().getContentAsString();
        final ErrorResponseResource error = fromJson(content, ErrorResponseResource.class);
        assertThat(error.getErrorMessage(), is(equalTo(PET_NOT_FOUND.getReason())));
    }
}
