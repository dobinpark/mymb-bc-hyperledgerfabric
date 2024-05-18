package mymb.mymbbchyperledgerfabric.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document("pay")
public class Pay {

    @Id
    private String payId;

    @Field
    private String fundingId;

    @Field
    private int totalAmount;

    @Field
    private int ticketAmount;

    @Field
    private int ticketCount;

    @Field
    private String ticketId;

    @Field
    private String memberId;

    @Field
    private PayStatusEnum status;

    @Field
    private LocalDateTime createdAt;

    @Field
    private LocalDateTime updatedAt;

    @Field
    private LocalDateTime confirmationAt;

    @Field
    private LocalDateTime deletedAt;

    @Field
    private boolean isPrivate;

    @Field
    private String payType;

    @Field
    private String PayTypeDetail;
}
