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
public class TicketDTO {

    private String ticketId;

    private String ticketAmount;

    private int ticketMaxCnt;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private int useCnt;

    private String type;

    private String ticketName;

    private String fundingId;
}
