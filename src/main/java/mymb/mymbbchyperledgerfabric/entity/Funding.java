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
@Document("funding")
public class Funding {

    @Id
    private String fundingId;

    @Field
    private String pollingResultId;

    @Field
    private String contestantId;

    @Field
    private String categoryCode;

    @Field
    private String writerName;

    @Field
    private String description;

    @Field
    private String genres;

    @Field
    private String synopsisTitle;

    @Field
    private LocalDateTime startAy;

    @Field
    private LocalDateTime endAt;

    @Field
    private String thumbnailLink;

    @Field
    private String introduce;

    @Field
    private String characterIntro;

    @Field
    private String ticketId;

    @Field
    private String bankName;

    @Field
    private String bankAccount;

    @Field
    private String detailImageLink;

    @Field
    private String detailMobileImageLink;

    @Field
    private String introImageLink;

    @Field
    private String introMobImageLink;

    @Field
    private String introImageLink1;

    @Field
    private String introMobImageLink1;

    @Field
    private String introImageLink2;

    @Field
    private String introMobImageLink2;

    @Field
    private String introImageLink3;

    @Field
    private String introMobImageLink3;

    @Field
    private String introImageLink4;

    @Field
    private String introMobImageLink4;
}
