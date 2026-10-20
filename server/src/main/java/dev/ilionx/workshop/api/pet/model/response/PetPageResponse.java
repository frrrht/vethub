package dev.ilionx.workshop.api.pet.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * One page of the pet directory.
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Schema(description = "A page of pets from the directory search")
public class PetPageResponse {

    @Schema(description = "Pets on this page")
    private List<PetResponse> content;

    @Schema(
        description = "Zero-based page index",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer page;

    @Schema(
        description = "Page size. The server always uses 20.",
        example = "20",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer size;

    @Schema(
        description = "Total number of matching pets",
        example = "42",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long totalElements;

    @Schema(
        description = "Total number of pages",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer totalPages;
}
