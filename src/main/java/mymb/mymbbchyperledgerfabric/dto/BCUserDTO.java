package mymb.mymbbchyperledgerfabric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BCUserDTO {

    private String userId;

    private String nickName;

    private int mymPoint;

    private ArrayList<String> ownedToken;

    private LocalDateTime blockCreatedTime;

    private int delta;
}
