package mymb.mymbbchyperledgerfabric.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
@ToString
@Document
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    protected String id;

    @Field
    @Indexed
    protected String email;

    @Field
    protected String password;
    @Field
    protected int ticketCount;
    @Field
    protected int referralCount;
    @Field
    protected String nickName;
    @Field
    protected String inviterEmail;
    @Field
    protected String mainCardId;
    @Field
    protected String mymId;
    @Field
    protected boolean isEnterprise;
    @Field
    protected String callNumber;
    @Field
    protected String countryCode;
    @Field
    protected String businessNumber;
    @Field
    protected String fileName;
    @Field
    protected String uploadUrl;
    @Field
    @DBRef(lazy = true)
    protected Set<User> trustUsers;
    @Field
    @DBRef(lazy = true)
    protected Set<User> trustByUsers;
    @Field
    protected boolean isIdentified;
    @Field
    @JsonFormat(pattern = "yyyy.MM.dd/HH:mm/E")
    protected LocalDateTime createdAt;
    @Field
    @JsonFormat(pattern = "yyyy.MM.dd/HH:mm/E")
    protected LocalDateTime deletedAt;

    @Field
    protected String name;

    @Field
    protected boolean isCertificated;
    @Field
    protected String bankAccount;
    @Field
    protected String bankName;

    @Field
    protected String accountHolderName ;

    @Field
    protected String phoneNum;
}
