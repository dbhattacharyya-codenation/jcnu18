package simplefixer.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import simplefixer.model.Minifix;

import java.util.List;

public interface MinifixRepository extends CrudRepository<simplefixer.model.Minifix, Integer> {
    List<Minifix> findAllByFixId(Integer fixId);

    @Modifying
    @Query("update Minifix minifix set minifix.s3Link = ?1 where minifix.fixId = ?2")
    @Transactional
    void updateS3LinkByFixId(String s3Link, Integer fixId);
}
