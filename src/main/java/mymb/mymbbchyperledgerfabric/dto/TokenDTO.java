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

    private String tokenType;

    private String sellStage;

    private LocalDateTime tokenCreatedTime;

    private int ticketCnt;
}
