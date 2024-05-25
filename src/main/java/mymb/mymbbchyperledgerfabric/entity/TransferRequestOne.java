package mymb.mymbbchyperledgerfabric.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TransferRequestOne {

    private String from;
    private String to;
    private List<String> tokenNumber;
}
