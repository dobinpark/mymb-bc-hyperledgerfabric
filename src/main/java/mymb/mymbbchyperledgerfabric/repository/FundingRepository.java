package mymb.mymbbchyperledgerfabric.repository;

import mymb.mymbbchyperledgerfabric.entity.Funding;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundingRepository extends MongoRepository<Funding, String> {

    List<Funding> findAllById(String id);
}
