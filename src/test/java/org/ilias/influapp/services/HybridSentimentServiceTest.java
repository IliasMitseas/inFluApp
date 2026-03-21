package org.ilias.influapp.services;

import org.ilias.influapp.entities.Enums.PostSentiment;
import org.ilias.influapp.entities.Post;
import org.ilias.influapp.entities.Reaction;
import org.ilias.influapp.entities.SentimentAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridSentimentServiceTest {

    @Mock
    private CoreNlpSentimentService coreNlpService;

    @Mock
    private EmojiSentimentAnalyzer emojiSentimentAnalyzer;

    private HybridSentimentService hybridSentimentService;

    @BeforeEach
    void setUp() {
        hybridSentimentService = new HybridSentimentService(coreNlpService, emojiSentimentAnalyzer);
    }

    @Test
    void shouldDefaultToNeutralWhenNoTextAndNoReactions() {
        Post post = new Post();
        post.setId(1L);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, null, null);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.NEUTRAL, sentiment);
        assertEquals(0.0, analysis.getPolarity());
        assertEquals(0.5, analysis.getConfidence());
        assertEquals("Neutral", analysis.getLabel());
        assertEquals("No data — defaulted to Neutral", analysis.getMethod());

        verify(coreNlpService, never()).analyze(any());
        verify(coreNlpService, never()).analyzeComments(any());
    }

    @Test
    void shouldUseReactionOnlyWhenTextMissing() {
        Post post = new Post();
        post.setId(2L);
        post.setContent("   ");

        List<Reaction> reactions = List.of(new Reaction());
        when(emojiSentimentAnalyzer.calculateReactionScore(reactions)).thenReturn(-0.6);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, reactions, null);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.TERRIBLE, sentiment);
        assertEquals(-0.6, analysis.getPolarity());
        assertEquals(0.5, analysis.getConfidence());
        assertEquals("Reactions only (100%)", analysis.getMethod());

        verify(coreNlpService, never()).analyze(any());
        verify(coreNlpService, never()).analyzeComments(any());
    }

    @Test
    void shouldCombineCoreNlpAndEmojiWhenNoReactions() {
        Post post = new Post();
        post.setId(3L);
        post.setContent("Great launch");

        when(coreNlpService.analyze("Great launch"))
                .thenReturn(new SentimentResult(0.5, "Positive", 0.8, 3));
        when(emojiSentimentAnalyzer.calculateEmojiScore(null)).thenReturn(0.2);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, List.of(), null);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.LOVE, sentiment);
        assertEquals(0.44, analysis.getPolarity());
        assertEquals(0.8, analysis.getConfidence());
        assertEquals("CoreNLP (80%) + Emoji (20%)", analysis.getMethod());
    }

    @Test
    void shouldCombineAllSignalsWhenTextAndReactionsExist() {
        Post post = new Post();
        post.setId(4L);
        post.setContent("Campaign update");

        List<String> comments = List.of("Looks good");
        List<Reaction> reactions = List.of(new Reaction());

        when(coreNlpService.analyzeComments(comments))
                .thenReturn(new SentimentResult(0.1, "Neutral", 0.6, 2));
        when(coreNlpService.analyze("Campaign update"))
                .thenReturn(new SentimentResult(0.6, "Positive", 0.9, 3));
        when(emojiSentimentAnalyzer.calculateEmojiScore(comments)).thenReturn(0.2);
        when(emojiSentimentAnalyzer.calculateReactionScore(reactions)).thenReturn(0.4);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, reactions, comments);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.LIKE, sentiment);
        assertEquals(0.23, analysis.getPolarity());
        assertEquals(0.66, analysis.getConfidence());
        assertEquals("CoreNLP (70%) + Emoji (15%) + Reactions (15%)", analysis.getMethod());
    }

    @Test
    void shouldWeightCommentsMoreThanContentWhenBothExist() {
        Post post = new Post();
        post.setId(5L);
        post.setContent("I love this product");

        List<String> comments = List.of("Bad quality");

        when(coreNlpService.analyzeComments(comments))
                .thenReturn(new SentimentResult(-0.5, "Negative", 0.9, 1));
        when(coreNlpService.analyze("I love this product"))
                .thenReturn(new SentimentResult(0.5, "Positive", 0.9, 3));
        when(emojiSentimentAnalyzer.calculateEmojiScore(comments)).thenReturn(0.0);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, List.of(), comments);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.DISLIKE, sentiment);
        assertEquals(-0.24, analysis.getPolarity());
        assertEquals(0.9, analysis.getConfidence());
        assertEquals("CoreNLP (80%) + Emoji (20%)", analysis.getMethod());
    }

    @Test
    void shouldClampFinalPolarityToRangeMinusOneToOne() {
        Post post = new Post();
        post.setId(6L);
        post.setContent("Extreme text");

        when(coreNlpService.analyze("Extreme text"))
                .thenReturn(new SentimentResult(2.0, "Positive", 0.7, 4));
        when(emojiSentimentAnalyzer.calculateEmojiScore(null)).thenReturn(2.0);

        PostSentiment sentiment = hybridSentimentService.analyzeAndSave(post, List.of(), null);

        SentimentAnalysis analysis = post.getSentimentAnalysis();
        assertNotNull(analysis);
        assertEquals(PostSentiment.LOVE, sentiment);
        assertEquals(1.0, analysis.getPolarity());
    }
}

