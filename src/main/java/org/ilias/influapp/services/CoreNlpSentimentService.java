package org.ilias.influapp.services;

import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.Tree;
import jakarta.annotation.PostConstruct;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;


@Service
public class CoreNlpSentimentService {

    private static final Logger log = LoggerFactory.getLogger(CoreNlpSentimentService.class);
    public static final String MODEL_VERSION = "Stanford-CoreNLP-4.5.7-RNTN";
    public static final String METHOD = "Recursive Neural Tensor Network (RNTN)";

    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init() {
        log.info("Initializing Stanford CoreNLP Sentiment pipeline...");
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        pipeline = new StanfordCoreNLP(props);
        log.info("Stanford CoreNLP Sentiment pipeline initialized successfully.");
    }


    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new SentimentResult(0.0, "Neutral", 1.0, 2);
        }

        CoreDocument doc = new CoreDocument(text);
        pipeline.annotate(doc);

        List<CoreSentence> sentences = doc.sentences();
        if (sentences.isEmpty()) {
            return new SentimentResult(0.0, "Neutral", 1.0, 2);
        }

        double totalPolarity = 0.0;
        double totalConfidence = 0.0;
        int totalWeight = 0;

        for (CoreSentence sentence : sentences) {
            Tree tree = sentence.coreMap().get(edu.stanford.nlp.sentiment.SentimentCoreAnnotations.SentimentAnnotatedTree.class);
            if (tree == null) continue;

            int sentimentClass = RNNCoreAnnotations.getPredictedClass(tree);
            SimpleMatrix probs = RNNCoreAnnotations.getPredictions(tree);

            // Convert 0-4 class to -1 to +1 polarity
            double polarity = (sentimentClass - 2.0) / 2.0;

            // Confidence = the probability of the predicted class
            double confidence = probs.get(sentimentClass, 0);

            // Weight by sentence length (longer sentences contribute more)
            int weight = sentence.tokens().size();
            totalPolarity += polarity * weight;
            totalConfidence += confidence * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return new SentimentResult(0.0, "Neutral", 1.0, 2);
        }

        double avgPolarity = totalPolarity / totalWeight;
        double avgConfidence = totalConfidence / totalWeight;
        int finalClass = polarityToClass(avgPolarity);
        String label = classToLabel(finalClass);

        return new SentimentResult(
                Math.round(avgPolarity * 1000.0) / 1000.0,
                label,
                Math.round(avgConfidence * 1000.0) / 1000.0,
                finalClass
        );
    }


    public SentimentResult analyzeComments(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return new SentimentResult(0.0, "Neutral", 1.0, 2);
        }

        double totalPolarity = 0.0;
        double totalConfidence = 0.0;
        int count = 0;

        for (String comment : comments) {
            if (comment == null || comment.isBlank()) continue;
            SentimentResult result = analyze(comment);
            totalPolarity += result.polarity();
            totalConfidence += result.confidence();
            count++;
        }

        if (count == 0) {
            return new SentimentResult(0.0, "Neutral", 1.0, 2);
        }

        double avgPolarity = totalPolarity / count;
        double avgConfidence = totalConfidence / count;
        int finalClass = polarityToClass(avgPolarity);

        return new SentimentResult(
                Math.round(avgPolarity * 1000.0) / 1000.0,
                classToLabel(finalClass),
                Math.round(avgConfidence * 1000.0) / 1000.0,
                finalClass
        );
    }

    private int polarityToClass(double polarity) {
        if (polarity >= 0.5) return 4;       // Very Positive
        if (polarity >= 0.1) return 3;       // Positive
        if (polarity >= -0.1) return 2;      // Neutral
        if (polarity >= -0.5) return 1;      // Negative
        return 0;                            // Very Negative
    }

    private String classToLabel(int sentimentClass) {
        return switch (sentimentClass) {
            case 0 -> "Very Negative";
            case 1 -> "Negative";
            case 2 -> "Neutral";
            case 3 -> "Positive";
            case 4 -> "Very Positive";
            default -> "Neutral";
        };
    }
}

