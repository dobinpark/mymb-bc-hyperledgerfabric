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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("bcUser")
public class BCUser {

    @Id
    private String userId;

    @Field
    private String userNumber;

    @Field
    private String nickName;

    @Field
    private int mymPoint;

    @Field
    private List<String> ownedToken;

    @Field
    @JsonFormat(pattern = "yyyy.MM.dd/HH:mm/E")
    private LocalDateTime blockCreatedTime;
}
