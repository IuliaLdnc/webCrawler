package searchengine.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemma(String lemma);
    int countBySite(Site site);

    @Modifying
    @Query("DELETE FROM Lemma l WHERE l.frequency = 0")
    void deleteUnusedLemmas();
}
