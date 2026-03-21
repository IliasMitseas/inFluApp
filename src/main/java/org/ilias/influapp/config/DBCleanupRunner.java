package org.ilias.influapp.config;

import org.ilias.influapp.repository.ReactionRepository;
import org.ilias.influapp.repository.SentimentAnalysisRepository;
import org.ilias.influapp.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DBCleanupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DBCleanupRunner.class);

    private final SentimentAnalysisRepository sentimentAnalysisRepository;
    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;

    public DBCleanupRunner(SentimentAnalysisRepository sentimentAnalysisRepository,
                           ReactionRepository reactionRepository,
                           PostRepository postRepository) {
        this.sentimentAnalysisRepository = sentimentAnalysisRepository;
        this.reactionRepository = reactionRepository;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) {
        log.info("DBCleanupRunner starting: scanning for orphan rows...");

        long orphansSa = sentimentAnalysisRepository.findAll().stream()
                .filter(sa -> sa.getPost() == null || !postRepository.existsById(sa.getPost().getId()))
                .count();
        log.info("SentimentAnalysis orphan count: {}", orphansSa);
        if (orphansSa > 0) {
            sentimentAnalysisRepository.findAll().stream()
                    .filter(sa -> sa.getPost() == null || !postRepository.existsById(sa.getPost().getId()))
                    .forEach(sa -> {
                        log.info("Deleting orphan SentimentAnalysis id={}", sa.getId());
                        sentimentAnalysisRepository.delete(sa);
                    });
        }

        long orphansReactions = reactionRepository.findAll().stream()
                .filter(r -> r.getPost() == null || !postRepository.existsById(r.getPost().getId()))
                .count();
        log.info("Reactions orphan count: {}", orphansReactions);
        if (orphansReactions > 0) {
            reactionRepository.findAll().stream()
                    .filter(r -> r.getPost() == null || !postRepository.existsById(r.getPost().getId()))
                    .forEach(r -> {
                        log.info("Deleting orphan Reaction id={}", r.getId());
                        reactionRepository.delete(r);
                    });
        }

        log.info("DBCleanupRunner finished.");
    }
}

