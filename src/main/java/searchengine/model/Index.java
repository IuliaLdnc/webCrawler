package searchengine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "word_index")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT", nullable = false)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "page_id", columnDefinition = "INT", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", columnDefinition = "INT", nullable = false)
    private Lemma lemma;

    @Column(name = "word_rank", columnDefinition = "INT", nullable = false)
    private Integer rank;
}

