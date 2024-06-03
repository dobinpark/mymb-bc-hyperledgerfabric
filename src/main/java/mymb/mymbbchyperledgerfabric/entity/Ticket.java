package mymb.mymbbchyperledgerfabric.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("ticket")
public class Ticket {

    @Id
    private String ticketId;

    @Field
    private String ticketAmount;

    @Field
    private int ticketMaxCnt;

    @Field
    private LocalDateTime startAt;

    @Field
    private LocalDateTime endAt;

    @Field
    private int useCnt;

    @Field
    private String type;

    @Field
    private String ticketName;

    @Field
    private String fundingId;

    @Field
    private String sellStage;
}
