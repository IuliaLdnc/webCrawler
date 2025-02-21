package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Long> {
    List<Page> findBySiteId(Integer siteId);
    int countBySite(Site site);
    Optional<Page> findByPath(String path);
    @Query("SELECT p FROM Page p " +
            "JOIN Index i ON p.id = i.page.id " +
            "JOIN Lemma l ON i.lemma.id = l.id " +
            "WHERE l.lemma IN :lemmas " +
            "AND (:siteUrl IS NULL OR p.site.url = :siteUrl) " +
            "GROUP BY p.id " +
            "HAVING COUNT(DISTINCT l.id) = :lemmaCount")
    List<Page> findPagesByLemmas(@Param("lemmas") List<String> lemmas,
                                 @Param("siteUrl") String siteUrl,
                                 @Param("lemmaCount") long lemmaCount);

}
