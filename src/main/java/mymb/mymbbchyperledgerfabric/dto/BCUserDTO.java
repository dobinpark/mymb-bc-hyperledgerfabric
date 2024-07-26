package mymb.mymbbchyperledgerfabric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BCUserDTO {

    private String userId;

    private String userNumber;

    private String nickName;

    private int mymPoint;

    private List<String> ownedToken;

    private LocalDateTime blockCreatedTime;

    private int delta;

    private int count;
}
