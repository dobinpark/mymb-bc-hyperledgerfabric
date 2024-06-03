package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.Pay;
import mymb.mymbbchyperledgerfabric.entity.PayStatusEnum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayRepository extends MongoRepository<Pay, String> {

    List<Pay> findAllByMemberId(String memberId);
    List<Pay> findByMemberIdAndStatus(String memberId, String status);
}
