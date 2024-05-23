package mymb.mymbbchyperledgerfabric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollingResultDTO {

    private String pollingResultId;

    private String synopsisTitle;

    private String genres;

    private String contestantId;

    private String description;

    private String writerName;

    private String categoryCode;
}
