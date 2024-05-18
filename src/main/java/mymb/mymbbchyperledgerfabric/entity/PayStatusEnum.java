package mymb.mymbbchyperledgerfabric.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum PayStatusEnum {

    CD("CD","구매취소"),O1("O1","구매신청"),
    O2("O2","승인보류"),OD("OD","승인완료"),
    R1("R1","환불신청"),R2("R2","환불보류"),RD("RD","환불완료");

    private String code;

    private String codeText;

    PayStatusEnum(String code, String codeText) {
        this.code = code;
        this.codeText = codeText;
    }

    @JsonCreator
    public static PayStatusEnum from(String value) {
        for (PayStatusEnum status : PayStatusEnum.values()) {
            if (status.getCode().equals(value)) {
                return status;
            }
        }
        return null;
    }

    @JsonValue
    public String getCode() {
        return code;
    }
}
