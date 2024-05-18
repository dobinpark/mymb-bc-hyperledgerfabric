package mymb.mymbbchyperledgerfabric.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import mymb.mymbbchyperledgerfabric.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    @NotNull @NotBlank
    private String email;

    @NotNull @NotBlank
    private String password;

    @NotNull @NotBlank
    private String nickName;

    private String inviterEmail;

    private int ticketCount;

    private int referralCount;

    private String mainCardId;

    private String mymId;

    private boolean isEnterprise;

    private String callNumber;

    private String countryCode;

    private String businessNumber;

    private String fileName;

    private String uploadUrl;

    private Set<User> trustUsers;

    private Set<User> trustByUsers;

    private boolean isIdentified;

    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    private String name;

    private boolean isCertificated;

    private String bankAccount;

    private String bankName;

    private String accountHolderName;

    private String phoneNum;
}
