package mymb.mymbbchyperledgerfabric.entity;

import lombok.Data;

import java.util.ArrayList;

@Data
public class TransferRequest {

    private String from;
    private String to;
    private ArrayList<String> tokenNumbers;
}
