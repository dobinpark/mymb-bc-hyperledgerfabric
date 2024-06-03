package mymb.mymbbchyperledgerfabric.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Document("token")
public class Token {

    @Id
    private String tokenId;

    @Field
    private String tokenNumber;

    @Field
    private String categoryCode;

    @Field
    private String pollingResultId;

    @Field
    private String fundingId;

    @Field
    private String ticketId;

    @Field
    private String tokenType;

    @Field
    private String sellStage;

    @Field
    private String imageUrl;

    @Field
    @JsonFormat(pattern = "yyyy.MM.dd/HH:mm/E")
    private LocalDateTime tokenCreatedTime;
}
