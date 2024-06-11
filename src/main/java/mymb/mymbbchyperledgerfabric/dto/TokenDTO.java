package mymb.mymbbchyperledgerfabric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenDTO {

    private String tokenId;

    private String tokenNumber;

    private String categoryCode;

    private String pollingResultId;

    private String fundingId;

    private String ticketId;

    private String tokenType;

    private String sellStage;

    private String imageUrl;

    private int ticketCnt;

    private LocalDateTime tokenCreatedTime;
}
