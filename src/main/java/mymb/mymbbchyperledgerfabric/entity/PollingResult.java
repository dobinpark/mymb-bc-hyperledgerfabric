package mymb.mymbbchyperledgerfabric.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Document("pollingResult")
public class PollingResult {

    @Id
    private String pollingResultId;

    @Field
    private String synopsisTitle;

    @Field
    private String genres;

    @Field
    private String contestantId;

    @Field
    private String description;

    @Field
    private String writerName;

    @Field
    private String categoryCode;

    @Field
    private int rank;
}
