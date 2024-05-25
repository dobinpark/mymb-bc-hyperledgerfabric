package mymb.mymbbchyperledgerfabric.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TransferRequestOne {

    private String from;
    private String to;
    private String tokenNumber;
}
