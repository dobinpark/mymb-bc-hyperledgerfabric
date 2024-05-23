package mymb.mymbbchyperledgerfabric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundingDTO {

    private String fundingId;

    private String contestantId;

    private String categoryCode;

    private String writerName;

    private String description;

    private String genres;

    private String synopsisTitle;

    private LocalDateTime startAy;

    private LocalDateTime endAt;

    private String thumbnailLink;

    private String introduce;

    private String characterIntro;

    private String ticketId;

    private String bankName;

    private String bankAccount;

    private String detailImageLink;

    private String detailMobileImageLink;

    private String introImageLink;

    private String introMobImageLink;

    private String introImageLink1;

    private String introMobImageLink1;

    private String introImageLink2;

    private String introMobImageLink2;

    private String introImageLink3;

    private String introMobImageLink3;

    private String introImageLink4;

    private String introMobImageLink4;
}
